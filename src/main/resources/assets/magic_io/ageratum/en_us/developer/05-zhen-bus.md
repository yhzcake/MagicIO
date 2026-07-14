---
title: "Zhen Bus"
navigation:
  title: "Chapter 5"
---

# Chapter 05: Zhen Bus

## 1. Design Goals

The zhen bus turns one block position into six faces that can independently host zhen functions. Each direction stores at most one `SideProcessor`; each processor owns its zhen type, inventory, fluids, energy, recipe progress, thin collision shape, and external Capability. The bus itself handles containment, interaction, saving, synchronization, and rendering, but does not directly implement specific recipes.

## 2. Class Responsibilities

| Class | Responsibility |
|---|---|
| `ZhenBusBlock` | Placement interaction, face hit detection, ticker, shape, drops, and processor installation entry point |
| `ZhenBusBlockEntity` | Host implementation, persistence, deferred restoration, update packets, and container delegation |
| `ZhenBusHost` | Minimal interface abstraction for host capabilities |
| `ZhenBusContainer` | Add/remove/tick all processors, combine shapes, and collect drops |
| `ZhenBusStorage` | `EnumMap<Direction, SideProcessor>` and client direction markers |
| `SideProcessor` | Contract for side-module lifecycle, IO, interaction, serialization, and recipe state |
| `AbstractSideProcessor` | Generic processor implementation for ordinary zhen types |
| `GridCellSideProcessor` | Specialized processor implementation for grid_cell |
| `VirtualPort` | Scan adjacent bus faces and transfer resources between processors |
| `PortBinding` | Binding description for host direction, target direction, and input/output zones |

## 3. Installation and Replacement Flow

The core bus installation flow is in `ZhenBusContainer.add`:

1. Read the old processor on the target direction.
2. If one exists, collect its drops, spawn them in the world, and call `onRemove`.
3. If the type string is `magic_io:grid_cell`, create `GridCellSideProcessor`; otherwise create a generic `AbstractSideProcessor`.
4. Call `onAdd`, bind the host change callback, and mark the inputs as changed.
5. Store it in direction-indexed Storage.
6. Invalidate the Capability cache at the block position.
7. Run the host change callback to trigger saving and synchronization.

`canAdd` currently checks only whether the direction is empty, while `add` itself supports replacement and drops the old contents. Callers must explicitly define whether their interaction forbids replacement or permits it; method names alone are insufficient.

## 4. SideProcessor Lifecycle

The interface requires processors to implement:

- Identity and context: direction, zhen type, position, and world;
- Work loop: `tick`, `hasWork`, progress, and input-change state;
- IO: `IOProcessor`, face access, and items/fluids required for serialization;
- Player interaction: normal activation and sneaking activation;
- Lifecycle: `onAdd`, `onRemove`, and drop collection;
- Presentation: collision shape and client buffer reading/writing.

A new specialized processor must implement the complete contract. Implementing only tick while ignoring drops, NBT, or network state creates inconsistencies during removal, reload, or client observation.

## 5. Generic Side Processor

`AbstractSideProcessor` initializes from `ZhenType`:

1. Calculate item and fluid container lengths from `SlotPartition`.
2. Register Item, Fluid, and optional Energy IOComponents.
3. Install change callbacks that mark the host dirty and invalidate recipe input caches.
4. Cache the input/output slot sets for each resource type.
5. Copy the type's face access rules into `FaceAccessController`.
6. Create the `self` port and six directional virtual ports.

It calls the shared `RecipeProcessor` while working, so standalone zhen and bus sides follow identical recipe semantics. Idle processors wake less frequently; virtual ports also reduce scan frequency when they have no connections, preventing large bus networks from scanning neighbors every tick.

## 6. Tick and Shape

`ZhenBusContainer.tickAll` iterates over every processor in server Storage and calls `tick`. The bus combines fixed 1/16-thick face plates for every installed direction: up, down, east, west, south, and north each have a fixed VoxelShape.

The client does not need complete executable processors. `ZhenBusStorage.clientMarkers` may record only which directions contain modules, allowing collision and selection shapes to be rebuilt. This separation of “complete server state” from “client presentation markers” reduces synchronization payload, but all client interaction checks must tolerate incomplete state.

## 7. Capability Routing

When an external system queries the bus Capability from a direction, that world direction serves two purposes:

1. Locate the SideProcessor installed on that direction;
2. Query the IO slots that processor exposes on that world direction.

Item and fluid slots are then intersected with the processor's input/output slots to construct Linked Handlers. If there is no processor, no corresponding IO component, no exposed slots, or no valid permission, the query returns `null`. Adding or removing a processor must invalidate the Capability cache.

## 8. Persistence and Deferred Restoration

`ZhenBusBlockEntity` creates a child tag for each of the six directions. Every processor stores at least its type ID and processing_time; non-fluid components call their own `saveNBT`, while fluids use a list containing tank indices.

Loading has two paths:

- If `level != null`, a processor can be created from the type registry immediately and its components restored.
- If `level == null`, required data is copied into `pendingNbt` first and deserialized after `onLoad` obtains the world.

Deferred restoration matters because constructing a processor requires a Level and type lookup requires the registry to be available. Preserve this restoration order: “create processor → restore progress and IO → onAdd → bind callback → write to Storage.” A client update tag may rebuild only direction markers and rendering state.

## 9. Virtual Ports

Every generic processor creates seven ports by default: `self` points to the relative face of the block adjacent to the installation face, plus one port for each of the six world directions. `PortBinding` describes which host face to search from and which target face to read on the neighbor.

`VirtualPort.scanNeighbors` currently connects only adjacent `ZhenBusBlockEntity` instances and retrieves a processor from the bound targetFace. Connection results are cached and refresh every 200 ticks by default; forced scanning updates immediately after structural changes.

Resource transfer proceeds as follows:

1. Filter connected processors that have a component for the target IOType.
2. Divide the target quantity evenly among them, assigning remainders to the first targets.
3. Call the target component's generic `insert`.
4. Accumulate and return all rejected amounts.
5. Let the source processor update its output slot from the returned remainder.

Current quantity dispatch recognizes only `ItemStack`, `FluidStack`, and `Integer`. A new IO type must extend quantity reading and reconstruction, or its transfer amount will be interpreted as 0.

## 10. Output Pushing

`AbstractSideProcessor.pushOutputsThroughPorts` iterates over IO components: items push only item output slots, fluids use fluid slot sets, and energy follows a dedicated path. For each non-empty value, it tries connected ports in order; once a port accepts part of the resource, it updates source storage and stops trying ports for that round.

Development considerations:

- A VirtualPort return value must be the unaccepted remainder;
- Target insertion must obey simulation/actual semantics;
- The host change callback must run after pushing;
- Port cache invalidation and Capability cache invalidation are separate mechanisms;
- `PortBinding`'s inputZones/outputZones are currently structural fields, while actual transfer mainly follows component insertion rules; zone routing extensions must connect these fields to filtering logic.

## 11. Client Rendering

`MagicIOClient` registers `ZhenBusBlockEntityRenderer` for the zhen bus block entity. The renderer submits face models for each direction according to synchronized state. Server logic must not directly reference the renderer or Minecraft client classes. When adding visual state:

1. Mark an update when server state changes;
2. Write the minimum presentation data to an update tag or packet;
3. Rebuild `ZhenBusRenderState` on the client;
4. Let the renderer only read client state, never execute recipes or modify authoritative inventory.

## 12. Adding a Specialized Bus-Side Module

1. Register a stable `ZhenType` and decide whether it belongs to the level expansion system.
2. Implement `SideProcessor`, or extend the generic processor and override only differing behavior.
3. Dispatch the specialized implementation by type ID in processor factories; currently this check exists in both container addition and block entity restoration paths, and the two must remain consistent.
4. Implement NBT, update data, drops, interaction, shape, and lifecycle.
5. If it exposes external resources, ensure `getIOProcessor` and `getFaceAccess` are compatible with Capability registration.
6. After structural changes, handle saving, client synchronization, Capability invalidation, and virtual-port rescanning together.
7. Validate placement, replacement, removal, chunk unload/reload, and dedicated server behavior.

## 13. Debugging Checklist

- One face does not work: verify the direction in Storage, processor type, and server ticker.
- Collision shape exists but processor is empty: distinguish client markers from complete server processors.
- Inventory is lost after reload: verify the pendingNbt path and component loading order.
- Pipe connects to the wrong face: verify the relationship among world direction, installation direction, and targetFace.
- Adjacent buses do not transfer: force a port scan and verify that the target really has the corresponding processor and IO component.
- Removal destroys items: confirm that both `addDrops` and `onRemove` are called.
- Old inventory remains accessible after replacement: confirm that `invalidateCapabilities` runs.
- Client appearance does not refresh: confirm that structural changes call `markForUpdate` and send an update tag.

## 14. Summary

The zhen bus is a directional SideProcessor container. Storage indexes state, Container manages lifecycle, BlockEntity manages persistence and synchronization, Capability manages external access, and VirtualPort manages internal adjacent transfer. When extending the bus, validate these five chains as one whole rather than focusing only on processor tick logic.

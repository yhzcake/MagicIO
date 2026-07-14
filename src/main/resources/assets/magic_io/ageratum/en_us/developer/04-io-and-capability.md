# Chapter 04: IO and Capability

## 1. Two Abstraction Layers

MagicIO divides resource interaction into an internal IO layer and a NeoForge Capability layer:

- The internal layer uses `IOType`, `IOComponent`, and `IOProcessor` to unify resource operations for recipes and the zhen bus.
- The external layer exposes restricted views to pipes or other mods through NeoForge Item, Fluid, and Energy Block Capabilities.

A Capability is not the storage itself. Actual state is kept in `ItemIOComponent`, `FluidIOComponent`, and `EnergyIOComponent`; the Handler returned by an external query is only an access window linked to that state.

## 2. IOType and IOProcessor

`IOType` is a custom registry object, currently including item, fluid, and energy. `IOProcessor` aggregates the components owned by one zhen instance in a `Map<IOType, IOComponent<?, ?>>`, and provides typed lookup, enumeration, and bulk installation of change callbacks.

Registering a new component for the same IOType replaces the old component, so one processor has only one authoritative component of each type. After callers retrieve a generic interface from `get`, they usually still need a concrete type check because Java type erasure cannot prove the one-to-one relationship between a registry object and a component implementation.

## 3. IOComponent Contract

`IOComponent<R,T>` abstracts resource requirement `R` and actual value `T`, defining:

- `canSupply`, `hasSupply`: check whether a requirement can be met;
- `canFit`: check whether an output fits in target slots;
- `extract`, `insert`: support simulated and actual operations;
- `consume`, `produce`: apply changes during recipe commit;
- `setChangeCallback`: notify the host to mark itself dirty and refresh inputs;
- `saveNBT`, `loadNBT`: persist the component.

Every new implementation must guarantee that simulation has no side effects. “Simulate all inputs and outputs first, then commit the actual operations” is the critical transaction boundary that prevents a recipe from consuming only half of its resources or producing only part of its results.

## 4. Three Built-In Components

### 4.1 ItemIOComponent

Uses `NonNullList<ItemStack>` to store items and `SlotPartition` to distinguish input and output zones. It also provides NeoForge `ResourceHandler<ItemResource>`. Item comparison, Ingredient requirements, stack limits, and container serialization are centralized here; business code should not duplicate these rules.

### 4.2 FluidIOComponent

Uses `NonNullList<FluidStack>` and nullable tankCapacity. Fluid slots are also divided into input and output zones by the partition. When the bus block entity persists fluids, it additionally stores each tank index so filtering out empty slots does not lose the original slot positions.

### 4.3 EnergyIOComponent

Internally wraps `SimpleEnergyHandler`; capacity, input rate, output rate, and initial energy are determined by constructor parameters. Energy changes trigger the component callback. A standalone zhen currently creates a zero-capacity component even when its type has no energy capacity, while the bus `AbstractSideProcessor` registers an energy component only when the capacity is non-null. Extension code should account for this difference.

## 5. Face Access Control

`ZhenType` first resolves “direction → IO type → zone name” into slot indices. Runtime instances then write the result into `FaceAccessController`. External Capability queries are constrained by three layers:

1. The queried direction must be non-null;
2. That direction must expose the corresponding IOType in the type's face access table;
3. Exposed slots must also intersect with the input or output slot sets.

Thus, face access determines “which slots are visible from which face,” while the partition determines “whether those slots accept insertion or allow extraction.” Neither can replace the other.

## 6. Linked Handlers

`LinkedItemHandler` and `LinkedFluidHandler` do not copy resources. They link to the host's lists and retain sets of slots that permit insertion and extraction. Their change callbacks:

- Call the component's `notifyChanged`;
- Mark the block entity as changed;
- Consequently invalidate the recipe input cache.

Wrapping the underlying list in an unrestricted Handler would bypass face and input/output permissions. Returning a resource copy would mean pipe writes never reach the real storage.

## 7. Capability Registration

All block Capabilities are registered centrally in `MagicIO.registerCapabilities`.

### 7.1 Ordinary Zhen

- Item: read the insertable and extractable slots for the specified direction; return `null` if both are empty.
- Fluid: additionally require the type to have tankCapacity, then create a LinkedFluidHandler with capacity and slot restrictions.
- Energy: return its Handler when an energy component exists; the current path does not filter by direction, so direction rules must be updated here as well if they are added.

### 7.2 Zhen Bus

First retrieve the `SideProcessor` on the queried direction, then read that processor's access table for the world direction. Item and fluid slots are intersected with the input/output slot caches in `AbstractSideProcessor`. Energy uses the generic `registerZhenBusCap` helper to retrieve the corresponding component and convert it to an external Handler.

After installing, removing, or replacing a bus processor, call `level.invalidateCapabilities(pos)` because NeoForge may cache Capability query results; `setChanged` alone does not invalidate an old Handler.

## 8. Changes, Persistence, and Synchronization

Resource changes involve at least three concepts:

- **Marking persistence dirty**: `setChanged` or a host callback ensures chunk state is saved.
- **Recipe cache invalidation**: `inputsChanged = true` causes the processor to rematch recipes.
- **Client synchronization**: `markForUpdate`, update packets, or update tags refresh collision shapes, rendering, and observation information.

These serve different purposes. Marking dirty alone does not immediately update the client, while sending a packet does not guarantee saving. Component callbacks handle the first two; structural changes usually also require explicit client synchronization and Capability invalidation.

## 9. Complete Path for Adding an IO Type

1. Register a new `IOType` entry and define a stable ID and localization key.
2. Design an `IOComponent<R,T>` implementation with explicit semantics for empty values, quantities, equality, simulation, and serialization.
3. Register the component according to type requirements during component initialization for both standalone zhen and `AbstractSideProcessor`.
4. Extend `SlotZone` or reuse zones that can express input/output for the resource, and declare continuous slot indices in `SlotPartition`.
5. Add handling for the resource type to recipe input/output parsing and `RecipeProcessor`.
6. If NeoForge has a corresponding Capability, register an external adapter constrained by direction and slots.
7. Extend `VirtualPort.getAmount`, `withAmount`, and output-pushing logic; currently they recognize only ItemStack, FluidStack, and Integer.
8. Complete NBT, update packets, Jade/JEI display, and save migration strategy.

Completing only steps 1 and 2 does not make a new resource automatically flow through the system; in particular, check runtime type dispatch in virtual ports.

## 10. Debugging Methods

- Pipes cannot see a capability: check the query direction, face access zone names, and slot intersections.
- Output slots accept insertion: check `SlotZone` naming and the insertSlots used to construct the Linked Handler.
- Recipes do not refresh: verify that actual writes trigger the component callback and `inputsChanged`.
- Fluids return to wrong slots after reload: verify that serialization preserves tank indices.
- Old inventory remains accessible after replacing a bus side: verify that `invalidateCapabilities` is called.
- Simulation reduces resources: verify that the `simulate=true` branch is completely side-effect free.
- Automatic output loses resources: verify that the VirtualPort return value strictly represents the unaccepted remainder.

## 11. Validation Matrix

For every IO type, validate at least: queries from all six directions, null-direction queries, allowed/forbidden insertion, allowed/forbidden extraction, simulation, full capacity, partial acceptance, save/reload, cache invalidation after processor replacement, and consistency between standalone zhen and the zhen bus.

## 12. Summary

Internal `IOComponent` implementations provide reliable resource semantics, while Capability only exposes restricted views for specific directions and slots. Extensions must maintain partitions, face access, transactional simulation, change callbacks, persistence, Capability caches, and virtual ports together; adding only a Handler is insufficient.

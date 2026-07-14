# Chapter 03: Extending Zhen

## 1. Core Model

MagicIO separates “zhen function” and “zhen level” into orthogonal concepts: `ZhenFunction` describes a function family, `ZhenLevel` describes a level, and the two expand into the final registered object, `ZhenType`. The same `ZhenType` can drive either a standalone `ZhenBlockEntity` or be wrapped by the zhen bus as a `SideProcessor`.

```text
ZhenFunction + ZhenLevel
          ↓ register
       ZhenType
       ↙      ↘
独立阵方块实体   阵总线面处理器
          ↘   ↙
       RecipeProcessor
```

## 2. ZhenLevel: Levels and Recipe Multipliers

The current levels are unstable 0, stable 10, sturdy 20, infused 30, ancient 40, and primeval 50. Each level contains an ID prefix, a level value, and speed and output multipliers relative to the preceding level.

`totalSpeedMultiplier` and `totalOutputMultiplier` accumulate relative multipliers along the level sequence. For example, if a function is introduced at level 10, calculating its multiplier from level 10 to 30 accumulates the factors for levels 20 and 30. When adding a level, also consider:

- Its order in `ALL`;
- Its ID prefix and language key;
- Recipe expansion logic;
- Models and item resources;
- Stability of existing save IDs.

## 3. ZhenFunction: Declaring a Function Family

`ZhenFunction` stores seven kinds of information: base name, element supplier, first available level, slot partition supplier, face access rules, optional TickFactory, and optional fluid capacity. `register` iterates over all levels, skips entries below the introductory level, and generates:

```text
<level_prefix><base_name>_zhen
```

For example, `sieve`, introduced at level 0, produces `unstable_sieve_zhen` through `primeval_sieve_zhen`; a function introduced at level 30 produces only the final three levels.

Existing convenience factories include:

- `simpleItem`: item input slot 0 and output slot 1, with no custom tick;
- `simpleItemWithTick`: the same slots with an attached tick;
- `tickOnly`: no storage, suitable for world behavior such as portals;
- `fluidItem`: item input/output plus one fluid output slot and capacity.

## 4. ZhenType: Runtime Definition

The final `ZhenType` is an immutable type object containing:

- Element type and full type string;
- `SlotPartition`;
- Level value;
- Optional tick factory;
- Optional fluid and energy capacities;
- A face access table of `Direction → IOType → set of slot indices`;
- A mapping from virtual ports to slot zone names.

During construction, face access configuration still uses zone names. `buildFaceAccess` resolves those names to integer slots according to the `SlotPartition`. This keeps business declarations independent from concrete slot numbers, but every zone name must exist in the partition. A missing zone produces an empty slot set rather than an automatic error, so extensions should verify this explicitly.

## 5. Slot Partitions

`SlotPartition` uses `SlotZone → IOType → Set<Integer>` to represent slot semantics. Standard zones include item input/output, fluid input/output, energy input/output, and world-drop output.

Slot numbers are interpreted independently by each IO component. Item slot 0 and fluid slot 0 can coexist because different `IOType` values distinguish them. Slot indices should be continuous from 0; `getTotalSlots(IOType)` returns the number of unique indices, so declaring only slot 3 without slots 0–2 creates a container of length 1 and then causes out-of-bounds access to slot 3.

## 6. Standalone Zhen Lifecycle

`ZhenBlock` creates the shared `ZhenBlockEntity`, and its server ticker calls `AbstractZhenBlockEntity.serverTick`. An ordinary zhen follows this work path:

1. The block entity resolves its `ZhenType` from the block ID.
2. It creates item, fluid, and energy IO components according to the partition.
3. IO change callbacks call `setChanged` and mark recipe inputs as changed.
4. Each server tick calls the shared `RecipeProcessor.processTick`.
5. When there is no current recipe, checks occur at intervals to avoid scanning every recipe every tick.
6. After matching, processing time advances; on completion, input consumption and output insertion are simulated and then committed.
7. Block entity state is synchronized when necessary.

When a `ZhenBlock` is broken, container items are dropped and cleared. Fluids, energy, or custom components that need recovery require explicit lifecycle design; they are not handled by `Container` automatically.

## 7. Recipe-Driven Logic and Custom Ticks

Standard processing zhen should prefer zhen recipes and `RecipeProcessor` so they share input matching, progress, multipliers, output, reload, and network synchronization. `TickFactory` is better suited to world behavior that is not purely recipe processing, such as portals.

A custom tick must:

- Change the world and persistent state only on the server;
- Use the supplied `Level`, `BlockPos`, `BlockState`, and block entity without retaining invalid world objects long-term;
- Trigger the component change callback after modifying IO;
- Limit scan range and execution frequency rather than performing broad scans every tick;
- Handle bus reuse scenarios when the block entity parameter is nullable.

## 8. Steps for Adding a Standard Zhen Function

1. **Define semantics**: determine the base name, element, minimum level, input/output types, capacities, and whether world ticking is needed.
2. **Design the partition**: assign continuous slot numbers for every IOType and express input/output responsibilities with `SlotZone`.
3. **Design face access**: decide which zones are visible from each of the six world directions; default access exposes all standard item and fluid input/output zones on all six faces.
4. **Declare the function family**: use a convenience factory in `ZhenFunctions`, or construct a complete `ZhenFunction` for complex functions.
5. **Connect centralized registration**: ensure `ZhenFunctions.register` calls the function's `register`; if another module needs a reference to the lowest-level type, retain the returned Supplier.
6. **Add recipe data**: the recipe type ID must correspond to the expanded zhen type; server resource reload is responsible for loading it.
7. **Complete resources**: languages, item models, and required recipe directories must cover the levels that are actually generated.
8. **Validate both forms**: test a standalone zhen and the same type installed on a zhen bus side, confirming that both use the same partition and recipe rules.

## 9. When to Register ZhenType Directly

`GRID_CELL` is a special example: it has zero slots and its behavior is handled by `GridCellSideProcessor`, so it registers `ZhenType` directly instead of using function × level expansion. This is recommended only when all the following apply:

- It does not belong to a leveled function family;
- It requires a specialized SideProcessor;
- It should not automatically generate six level variants;
- Its lifecycle and serialization differ significantly from ordinary processing zhen.

After direct registration, also decide whether a standalone zhen block should be generated. Currently `ModBlocks` iterates over every `ZhenType` entry, so special types may also enter the automatic block registration set. Extension authors must decide whether that is intended or requires filtering.

## 10. Common Issues

- Type ID omits `_zhen`: block-name resolution and recipe mapping may fail.
- Wrong introductory level: creates low-level variants that should not exist or leaves higher-level variants without resources.
- Zone name mismatch: the Capability exists but exposes an empty set.
- Output slot treated as input: insertion and extraction permissions are jointly determined by partition semantics and face access.
- Custom tick bypasses callbacks: saves are not marked dirty, recipe caches do not refresh, or clients do not update.
- Testing only standalone zhen: the bus uses `AbstractSideProcessor`, so its serialization and face-direction behavior must also be tested.

## 11. Validation Checklist

- Every expected level has the exact ID in the registry.
- Every type has a same-named block, BlockItem, and localized display name.
- Input changes trigger recipe rematching, while idle machines do not perform a full scan every tick.
- Face Capabilities can insert only into input slots and extract only from output slots.
- Inventory, fluids, and progress survive saving and reloading.
- After server resource reload, the client recipe display updates through network synchronization.

## 12. Summary

The preferred path for adding a zhen is to declare a `ZhenFunction`, let the level system expand it into `ZhenType` objects, and then let the existing registration, block entity, IO, and recipe systems take over. Only functions with a fundamentally different lifecycle, such as grid cells, should use specialized types and processors.

# Chapter 06: Data-Driven Recipes

This chapter explains the zhen recipe system in the current MagicIO 26.1 source code. Here, “data-driven” does not mean delegating all work directly to the vanilla `RecipeManager`. Instead, the server scans data-pack resources, parses them into `ZhenRecipe` objects, stores them in a separate cache, and synchronizes them to clients. Extensions must follow this actual pipeline.

## 1. Recipe Processing Pipeline

A zhen recipe passes through these stages:

1. Its JSON resides under `data/<namespace>/recipe/**/*.json`.
2. `MagicIO.loadRecipesToManager` enumerates resources from the server `ResourceManager`'s `recipe` directory.
3. The current implementation accepts only resources in the `magic_io` namespace.
4. `ZhenRecipeLoader.loadRecipeFromJson` manually parses the JSON and validates the zhen type, items, fluids, and loot tables.
5. Valid recipes are added to `ZhenRecipeManager` and grouped by the zhen function's base name.
6. `RecipeProcessor` finds and executes recipes according to the machine's zhen type, slot partition, and current resources.
7. The server sends the complete cache to clients through `ZhenRecipeSyncPayload` for JEI display.

Key source code:

- `MagicIO#loadRecipesToManager`
- `ZhenRecipeLoader`
- `ZhenRecipe`
- `ZhenRecipeSerializer`
- `ZhenRecipeManager`
- `RecipeProcessor`

## 2. Minimal Recipe Example

The project's unstable sieve zhen recipe is a complete and straightforward reference:

```json
{
  "type": "magic_io:zhen_block",
  "zhen_type": "magic_io:unstable_sieve_zhen",
  "inputs": {
    "item_input_all": [
      {"item": "minecraft:coarse_dirt", "count": 1}
    ]
  },
  "outputs": {
    "item_output_all": [
      {"item": "minecraft:dirt", "count": 1}
    ],
    "drop_output": [
      {"loot_table": "magic_io:sift_metal_drop"}
    ]
  },
  "processing_time": 100
}
```

Field meanings:

| Field | Current Behavior |
| --- | --- |
| `type` | Recipe type identifier retained in data; the manual loader currently does not use it for dispatch |
| `zhen_type` | Required; must resolve in the custom `ZhenType.ZHEN_TYPES` registry |
| `inputs` | Item inputs organized by slot partition; each `item` contains a standard Ingredient and quantity |
| `outputs` | Item or loot outputs; keys are slot zone names |
| `fluid_inputs` | Fluid inputs grouped by zone |
| `fluid_outputs` | Fluid outputs grouped by zone |
| `processing_time` | Base processing ticks; 20 ticks are usually about 1 second |

## 3. Zoned Inputs and Outputs

Recipes do not directly store physical slot numbers. They store `SlotZone` names, which `SlotPartition` maps to concrete slots during execution. Common zones include:

- `item_input_all`: general item input zone.
- `item_output_all`: general item output zone.
- `fluid_input_all`: general fluid input zone.
- `fluid_output_all`: general fluid output zone.
- `drop_output`: output produced directly in the world rather than inserted into a container.

This design allows multiple levels of one function to share recipes. `ZhenRecipeManager` removes the level prefix through `ZhenLevel.baseName`, then removes the `_zhen` suffix. For example, `unstable_sieve_zhen` and higher-level `sieve_zhen` variants all end up in the `sieve` group.

Before adding a recipe, verify that the target `ZhenType`'s `SlotPartition` actually declares the corresponding zone. Writing a zone that the machine does not have in JSON does not automatically grant the machine that slot.

## 4. Item Inputs

The recommended format is an object grouped by zone:

```json
"inputs": {
  "item_input_all": [
    {"item": "minecraft:iron_ingot", "count": 2},
    {"item": "#c:dusts/redstone", "count": 1}
  ]
}
```

Each input stores a complete `Ingredient` and a separate positive integer `count`. If `count` is omitted, it defaults to 1; values below 1 cause the recipe data to be rejected. `item` is parsed by the standard Codec and can express a fixed item, a tag, or a NeoForge-registered custom Ingredient.

## 5. Item and Fluid Outputs

Fixed item output syntax:

```json
"outputs": {
  "item_output_all": [
    {"item": "minecraft:copper_ingot", "count": 3}
  ]
}
```

Fluid input supports either a concrete fluid or a fluid tag prefixed with `#`:

```json
"fluid_inputs": {
  "fluid_input_all": [
    {"fluid": "#minecraft:water", "amount": 1000}
  ]
}
```

Fluid output currently requires a concrete fluid ID:

```json
"fluid_outputs": {
  "fluid_output_all": [
    {"fluid": "minecraft:water", "amount": 250}
  ]
}
```

`amount` uses the integer capacity unit of NeoForge `FluidStack`. Whether a recipe can execute also depends on the machine's fluid zones, capacity, and `RecipeProcessor` output preflight, not merely whether the JSON parses.

## 6. In-Memory Model and Serialization

`ZhenRecipe` stores four core categories: zhen type ID, a list of `RecipeInput<?>`, a list of `RecipeOutput<?>`, and base processing time. Inputs and outputs are distinguished by `IOType`, allowing the processor to iterate over different resources through a unified structure.

`ZhenRecipeSerializer` defines all of the following:

- `MapCodec<ZhenRecipe>`: structured data encoding and decoding.
- `StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe>`: server-to-client network encoding and decoding.
- `RecipeSerializer<ZhenRecipe>`: wraps both codecs as a Minecraft recipe serializer.

The network format writes the recipe ID, zhen type, item input requirements and quantities, item outputs, fluid inputs, fluid outputs, and processing time in a fixed order. Any data-model change must update both encoding and decoding, or the client will read misaligned data or disconnect.

## 7. Matching, Caching, and Multipliers

`RecipeProcessor` does not scan every recipe unconditionally each tick. It stores the last valid recipe and an input hash, preferring to reuse the cache while inputs remain unchanged. When no recipe matches, it also checks at intervals to reduce the cost of idle machines.

Before completing a recipe, it:

1. Confirms that the current world is a `ServerLevel`.
2. Rolls fixed and loot-table outputs.
3. Applies output multipliers supplied by the level or plugins.
4. Checks item and fluid output space.
5. Consumes inputs.
6. Writes to output zones or spawns world drops.

`RecipeModifiers` allows multiple `RecipeModifier` instances to multiply processing-time and output multipliers together. Extensions should avoid negative, zero, or non-finite values and define the balance implications of stacked modifiers.

## 8. Debugging Steps

1. Confirm the path is `data/magic_io/recipe/...json`; the current scanner does not load other namespaces.
2. Confirm that `zhen_type` is registered and includes the correct level prefix and `_zhen` suffix.
3. Confirm that zone names match the target zhen's `SlotPartition`.
4. Confirm that all item and fluid IDs exist and that input tags have members bound in the registry.
5. Run `/reload` and observe the `Loaded ... recipes into ZhenRecipeManager` log.
6. On parsing failure, inspect `Failed to load recipe from JSON` or the specific resource-path error log.
7. If the recipe loads but does not run, check input quantities, fluid capacity, level restrictions, and output space.
8. If server execution works but JEI does not update, consult Chapters 08 and 09 for synchronization and runtime refresh checks.

## 9. Current Limitations

- The scanner hard-codes the `magic_io` namespace, so third-party data packs cannot inject zhen recipes solely through their own namespace.
- The top-level JSON structure is read manually with Gson, while item requirement fields are delegated uniformly to the `ItemRequirement` Codec and standard `Ingredient.CODEC`.
- Loot tables must be declared in an explicit item output zone.
- `ZhenRecipeManager` is a process-local singleton cache maintained separately on the server and client.

When extending these capabilities, align all four layers—disk format, in-memory model, network format, and JEI display—rather than changing only one.

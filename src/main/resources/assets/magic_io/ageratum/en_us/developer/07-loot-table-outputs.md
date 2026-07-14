---
title: "Loot Table Outputs"
navigation:
  title: "Chapter 7"
---

# Chapter 07: Loot Table Outputs

MagicIO allows zhen recipes to mix fixed items and random loot tables as outputs. Random results are generated only on the server when a recipe actually completes; probability information displayed in JEI is estimated by a lightweight client-side parser. These serve different purposes and have different capabilities.

## 1. Runtime Structure

The smallest unit of item output is `OutputEntry`:

- `OutputEntry.item(ItemStack)` represents a fixed item.
- `OutputEntry.lootTable(Identifier)` represents one evaluation of the specified loot table.
- `isLootTable()` distinguishes the two forms.
- `roll(ServerLevel)` returns the list of items actually produced by this roll.

`ZhenRecipe.rollOutput` iterates over these entries by output zone. Fixed items are copied into the result, while loot entries call `OutputEntry.roll`. Consequently, one output zone can contain both fixed items and multiple loot tables.

## 2. Recommended Recipe Syntax

```json
"outputs": {
  "item_output_all": [
    {"item": "minecraft:dirt", "count": 1}
  ],
  "drop_output": [
    {"loot_table": "magic_io:sift_metal_drop"}
  ]
}
```

Here, the fixed product enters the machine output zone, while random products enter `drop_output`. `RecipeProcessor.handleDropOutput` locates the face that exposes this zone and spawns an `ItemEntity` in that direction; if the caller requests a centered drop, it instead spawns near the block center.

## 3. Loot Table Files

The project's resource directory uses the current version's singular path:

```text
src/main/resources/data/magic_io/loot_table/random_treasure.json
```

Example structure:

```json
{
  "type": "minecraft:empty",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {"type": "minecraft:item", "weight": 10, "name": "minecraft:diamond"},
        {"type": "minecraft:item", "weight": 20, "name": "minecraft:iron_ingot"}
      ]
    }
  ]
}
```

Weights are not percentages. The two entries above have a total weight of 30, so the diamond has a `10 / 30` chance per draw and the iron ingot has a `20 / 30` chance. Adding or removing an entry from the same pool changes the actual probability of every entry.

## 4. Server-Side Evaluation

Zoned `outputs[].loot_table` entries are constructed as loot outputs during loading; actual table resolution and drawing occur when the recipe completes. Invalid references are usually exposed at roll time, so data-pack testing must cover the referenced loot table.

`OutputEntry.roll` uses:

```java
new LootParams.Builder(level).create(LootContextParamSets.EMPTY)
```

This means the loot table currently runs with an empty context, without player, tool, block position, entity, or luck parameters. Conditions and functions that depend on those context parameters are unsuitable for direct use in current zhen outputs.

Safe design principles:

- Use entries, weights, and quantity functions that do not depend on external context.
- Do not assume access to the killer, tool enchantments, or block state.
- If conditions produce an empty result, the recipe may still complete and consume inputs.
- Roll random outputs during output-space preflight so preflight and actual production do not use two different random results.

## 5. JEI Probability Display

`LootTableParser` does not call the server loot system. It reads the following from the classpath:

```text
data/<namespace>/loot_table/<path>.json
```

It can currently expand:

- `minecraft:item` entries.
- `minecraft:tag` entries.
- Integer or parseable `rolls`.
- Entry `weight`.

It calculates expected quantity per item and generates tooltip text for “expected value per operation” and “number of draws × probability.” `ZhenRecipeCategory` collects all loot outputs, merges them by item type, and passes them to JEI's cycling grid.

This parser is a display approximation, not a complete loot interpreter. It does not fully handle conditions, quality, complex entries, nested tables, function-modified quantities, or runtime context. A complex table may execute correctly in game while appearing incomplete in JEI.

## 6. Data Pack and Resource Pack Differences

The server reads loot tables through reloadable registries, so server data-pack overrides can change actual output. The client JEI parser reads packaged resources through the class loader and does not automatically see JSON from temporary server data packs.

This can produce the following situation:

- The server's random output has been modified by a data pack.
- The recipe received by the client contains only the loot table ID.
- JEI still displays the old table from the mod jar, or cannot find the external table at all.

This is a boundary of the current architecture; JEI display values must not be treated as server-authoritative results. If exact display is required in the future, parse a dedicated display snapshot during server reload and synchronize it over the network instead of having the client guess server resources.

## 7. Debugging Steps

1. Verify the path is `data/<namespace>/loot_table/<path>.json`.
2. Verify that the recipe reference omits `.json` and uses the correct namespace.
3. Run `/reload` and first confirm that top-level validation did not discard the recipe.
4. With relevant logging enabled, check whether `OutputEntry.roll` finds the target table and how many items it rolls.
5. If no actual output appears, reduce the table to one unconditional pool with a plain item entry.
6. If actual output works but JEI is empty, check whether the entry exceeds `LootTableParser` support or exists only in a server data pack.
7. If the drop direction is wrong, check whether `drop_output` is exposed on the expected face and inspect `zoneFaceAccess` configuration.

## 8. Compatibility Recommendations

- Keep public loot table IDs stable so data packs can override them without replacing recipes.
- Refer to third-party mod items by registry ID rather than creating a hard dependency through Java classes.
- When referencing items from an optional mod, account for parsing and recipe-loading behavior when that mod is absent.
- Test complex conditional tables both for actual dedicated-server evaluation and degraded JEI display.
- Do not roll real rewards on the client; authoritative random results must be produced by the server.

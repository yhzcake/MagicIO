# Chapter 6: Developing Zhen Recipes with Datapacks

## 1. File Location and Loading

Built-in recipes are located under `data/magic_io/recipe/<tier_directory>/`. The loader reads JSON from the resource manager, parses it into a standalone Zhen recipe manager, and synchronizes it to clients through a network payload. Recipes are rebuilt during data reloads.

The `type` field in current example recipes is consistently written as `magic_io:zhen_block`, but the core fields used by the custom loader are `zhen_type`, inputs, outputs, and `processing_time`.

## 2. Minimal Item Recipe

```json
{
  "type": "magic_io:zhen_block",
  "zhen_type": "magic_io:unstable_cinder_zhen",
  "inputs": {
    "item_input_all": [
      {"item": "minecraft:coal", "count": 1}
    ]
  },
  "outputs": {
    "item_output_all": [
      {"item": "magic_io:coal_coke", "count": 1}
    ]
  },
  "processing_time": 100
}
```

`count` defaults to 1 when omitted. `item` uses the standard Ingredient format of the current Minecraft/NeoForge version. The amount is stored independently by the outer requirement and is not expanded into repeated entries.

## 3. Region Names

Ordinary Zhen types should use the existing region names:

- `item_input_all`: item input.
- `item_output_all`: item output.
- `fluid_input_all`: fluid input, provided that the Zhen type has a corresponding fluid tank.
- `fluid_output_all`: fluid output.
- `drop_output`: world-drop output.

Region names must match the slot partitions of the target Zhen type. Successfully parsing JSON does not mean that the target Zhen has usable slots. For example, adding fluid output to an item-only Zhen does not automatically create a fluid tank.

## 4. Fluid Format

```json
{
  "type": "magic_io:zhen_block",
  "zhen_type": "magic_io:unstable_dew_zhen",
  "processing_time": 10,
  "fluid_outputs": {
    "fluid_output_all": [
      {"fluid": "minecraft:water", "amount": 20}
    ]
  }
}
```

Fluid inputs are written under `fluid_inputs`, and fluid outputs under `fluid_outputs`. An input fluid string supports either a direct ID or a fluid tag beginning with `#`; an output must resolve to a concrete fluid. Amount units follow NeoForge fluid stack semantics.

**Limitation:** The current fluid consumption path contains a generic `consume` method that is not fully implemented. Before adding production recipes with fluid inputs, actual consumption must be tested. Successful loading alone cannot establish that no duplication issue exists.

## 5. Loot Table Output

The following can be written inside an item output region:

```json
{"loot_table": "magic_io:sift_metal_drop"}
```

Loot-table output must be placed in an explicit output region. Results are rolled only when the server actually executes the recipe. JEI can only attempt to display candidate results by parsing local resources and cannot replace the results actually rolled by the server.

## 6. Tier Inheritance Design

A recipe's `zhen_type` points to its minimum target tier. When the actual Zhen has a higher tier and the same base function, the manager can find the lower-tier recipe and apply cumulative multipliers. Avoid copying identical JSON for every higher tier. Add a recipe at a new tier only when its ingredients, results, or base duration truly change.

## 7. Error Handling and Verification

- An unknown Zhen type causes that JSON file to fail loading and records a warning.
- A missing `processing_time`, an invalid ID, or an incorrect JSON structure causes the recipe to return empty and be skipped.
- A nonexistent item may be skipped, causing the input or output list to differ from the author's expectation.
- A recipe does not complete normally when its output region is full, so full-storage behavior should also be verified.
- After a data reload, verify the server recipe, client JEI display, and actual processing. Success in any one of the three does not independently prove complete synchronization.

Recommended verification order: first copy a currently valid minimal recipe and change only the file ID and target Zhen; then change the input; then change the output; finally add fluid or a loot table. Test every step with both a single set of ingredients and full storage.

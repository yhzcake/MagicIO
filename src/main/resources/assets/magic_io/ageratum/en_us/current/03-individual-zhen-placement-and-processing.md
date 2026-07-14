---
title: "Placing and Processing with an Individual Zhen"
navigation:
  title: "Chapter 3"
---

# Chapter 3: Placing and Processing with an Individual Zhen

## 1. Basic Workflow

1. Obtain a Zhen block from the Creative Mode tab.
2. Place the Zhen block in a loaded chunk.
3. Send recipe ingredients into its item input slot or fluid input tank from any surface.
4. Wait for the number of ticks required by the recipe.
5. Extract products from the item output slot or fluid output tank. `drop_output` generates item entities in the world.

An individual Zhen has no container menu that players can open directly. Adjacent devices with NeoForge item or fluid transfer capabilities are recommended for input and output. The default item layout is fixed: slot 0 is input and slot 1 is output. Input and output access are exposed on all six directions.

## 2. Minimal Verification Case: Unstable Cinder Zhen

The Zhen type is `magic_io:unstable_cinder_zhen`. The current built-in recipe consumes 1 coal and outputs 1 `magic_io:coal_coke` after 100 ticks.

During verification, confirm that:

- The coal actually enters the input slot rather than being rejected by the target device.
- The output side has room for the coal coke.
- The chunk remains loaded and server ticks advance normally.
- The Zhen uses the "Cinder" function rather than another function that also belongs to the Fire element.

## 3. Input-Free and Fluid Cases

The built-in recipe for the Unstable Zephyr Zhen requires no input and generates 1 wind charge every 100 ticks. As long as the Zhen remains within the server's loaded area, it continuously attempts to produce. It cannot complete another cycle when the output slot is full.

The Unstable Dew Zhen generates 20 mB of water every 10 ticks, and its built-in fluid tank has a capacity of 1000 mB. Its fluid partitions include only an output region and no fluid input region. Once the tank is full, an external device must extract the water or processing will stop because output is blocked.

## 4. World Drop Case

The Unstable Sieve Zhen consumes 1 coarse dirt, always outputs 1 dirt, and additionally resolves the `magic_io:sift_metal_drop` loot table. Loot-table products use `drop_output`; they do not enter the normal output slot and instead spawn as item entities outside the Zhen in its designated direction.

Therefore, automated sieving must handle two output paths at the same time:

- Dirt in the normal output slot.
- Random drop entities in the world.

Using only a hopper or pipe to extract the normal output slot may allow random products to scatter or accumulate.

## 5. Processing State Rules

- When there is no current recipe, the processor searches again at intervals. Input changes trigger another check.
- If the input changes during processing and no longer matches, the current recipe is terminated and progress resets to zero.
- If the output cannot accept the result upon completion, the processor does not consume the input and lose the result. Instead, it rolls progress back slightly and retries.
- After successful completion, the current recipe cache is retained and the next cycle starts from zero, which suits continuous feeding.
- Input matching is based only on current Ingredient semantics. The internal input hash does not track NBT, so do not assume that custom data differences always trigger an immediate rescan.

## 6. Higher-Tier Substitution

When a higher-tier Zhen of the same function processes a lower-tier recipe, speed and output multipliers accumulate. The function name must be identical: a Sturdy Sieve Zhen can process an Unstable Sieve recipe, while a Sturdy Gem Zhen cannot process a Sieve recipe.

## 7. Current Limitations

1. Most functions are not bridged to vanilla furnace, brewing stand, or similar recipes. Current actual content is determined by standalone Zhen JSON.
2. Energy components generally exist in the internal IO framework, but current type definitions for ordinary functions do not set effective energy capacities or recipe energy-consumption fields. This should not be understood as a complete FE-powered system.
3. When an individual Zhen is broken, items in its container are dropped. It should not be assumed that fluids and progress can be retained in item form.
4. The Primeval Creative Zhen attempts to generate a command block without input every tick. It is development placeholder content and is unsuitable for Survival balance.

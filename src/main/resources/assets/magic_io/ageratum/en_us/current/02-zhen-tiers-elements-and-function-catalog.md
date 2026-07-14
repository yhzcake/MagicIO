---
title: "Zhen Tiers, Elements, and Function Catalog"
navigation:
  title: "Chapter 2"
---

# Chapter 2: Zhen Tiers, Elements, and Function Catalog

## 1. Tier Mechanics

| Tier | Value | Relative Speed Factor | Relative Output Factor |
| --- | ---: | ---: | ---: |
| Unstable | 0 | 1.00 | 1.00 |
| Stable | 10 | 0.75 | 1.00 |
| Sturdy | 20 | 0.75 | 1.10 |
| Abundant | 30 | 0.75 | 1.10 |
| Archaic | 40 | 0.75 | 1.20 |
| Primeval | 50 | 0.75 | 1.20 |

Speed and output factors are multiplied cumulatively for each tier crossed. For example, when an Unstable recipe is placed in a Sturdy Zhen with the same function, its duration is multiplied by `0.75 × 0.75`, while its output is multiplied by `1.00 × 1.10`. The final processing duration is at least 1 tick. The exact rounding of item output multipliers should be determined from runtime logic rather than promised through mental calculation from the table alone.

A higher-tier Zhen can process lower-tier recipes of the same base function, but a lower-tier Zhen cannot process higher-tier recipes in reverse. Compatibility checking depends on the tier prefix in the Zhen ID. Extension IDs whose tier cannot be recognized follow a conservative compatibility path.

## 2. Automatic Function Expansion

Each function declares its "minimum introduction tier" only once. During registration, Zhen types are automatically generated for that tier and every tier after it. The ID format is:

```text
magic_io:<tier_prefix><function_name>_zhen
```

For example, Sieve is introduced at the Unstable tier, so `unstable_sieve_zhen`, `stable_sieve_zhen`, and all variants through `primeval_sieve_zhen` exist. Creative is introduced only at the Primeval tier, so only `primeval_creative_zhen` exists.

## 3. Current Function Table

| Minimum Tier | Element | Function ID | Name | Current Characteristics |
| --- | --- | --- | --- | --- |
| Unstable | Fire | cinder | Cinder | Item processing |
| Unstable | Earth | sieve | Sieve | Item processing, may include world drops |
| Unstable | Water | dew | Dew | Item slot plus 1000 mB output fluid tank |
| Unstable | Wind | zephyr | Zephyr | Item processing |
| Stable | Fire/Earth/Water/Wind | forge/compact/potion/carve | Forge/Compact/Potion/Carve | Item processing |
| Stable | Wood/Metal/Frost/Lightning | sprout/grind/frost/voltaic | Sprout/Grind/Frost/Voltaic | Item processing |
| Sturdy | Eight basic elements | blaze/gem/spring/whirl/ferment/mold/shift/thunder | Eight Sturdy functions | Item processing |
| Abundant | Various basic and composite elements | engrave/quake/symbiosis/conflux/synthesis/distill/gate/divine | Eight Abundant functions | Item processing |
| Archaic | Order/Chaos/Space/Time/Energy/Description/Soul | weave/fate/void/haste/transmute/foresight/summon | Seven item-processing functions | fate uses a random loot table |
| Archaic | Space | portal | Portal | Tick-only, no recipe slots |
| Primeval | Creation | creative | Creative | Input-free placeholder recipe |

## 4. Actual Role of Elements

Element types currently describe and categorize Zhen functions. The source code registers sixteen elements: Earth, Water, Wind, Fire, Wood, Ice, Metal, Lightning, Order, Space, Time, Chaos, Description, Energy, Soul, and Creative.

**Limitation:** No unified physical rules can currently be inferred from element names. For example, "Fire" does not automatically support all furnace recipes, and "Metal" does not automatically perform all metal processing. What can actually be processed is determined by Zhen recipe JSON.

## 5. Current Recipe Coverage

The resource directory provides 37 Zhen recipe files: 4 Unstable, 8 Stable, 8 Sturdy, 8 Abundant, 8 Archaic, and 1 Primeval. A Zhen automatically expanded to a higher tier does not necessarily have a dedicated recipe at that tier, but it can still process lower-tier recipes of the same function.

This means that "the block exists" does not guarantee that "a new recipe exists at that tier." To determine whether a Zhen has content, check both the registered type and the data under `data/*/recipe`.

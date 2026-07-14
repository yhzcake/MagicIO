---
title: "LV-0: Post-Apocalyptic Survival"
navigation:
  title: "Chapter 2"
---

# LV-0: Post-Apocalyptic Survival

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** All listed items, interactions, probabilities, recipes, and world-generation parameters are target designs and must not be treated as current-version functionality.

## 1. Stage Role

LV-0 establishes the most basic survival loop: dirt, clay, stone, fire, drinking water, and the first array. Without conventional ores or mature tools, players must gradually establish the conditions for crafting through direct interaction with the environment.

## 2. Polluted Dirt Interactions

Polluted dirt is planned as the early resource entry point. Left-clicking focuses on separating loose materials, while right-clicking focuses on risky searches for stones.

| Action | Planned Result | Cost and Purpose |
| --- | --- | --- |
| Left-click to gather | 2–4 dirt piles | Provides a stable basic product |
| Left-click to gather | 10% chance to obtain a clay pellet | Advances toward the clay furnace |
| Left-click to gather | 1% chance to obtain talc | Unlocks the array path |
| Right-click to search | 20% chance to obtain a stone | Provides material for a stone blade |
| Right-click to search | Consumes 1 health point | Prevents cost-free repeated attempts |

Dirt piles are planned to craft into dirt at a 4:1 ratio, while clay pellets craft into clay at a 4:1 ratio. The formal implementation must define interaction cooldowns, client animations, server-side drop determination, and a probability pity system to prevent repeated resolution caused by rapid clicking and network latency.

## 3. Crafting a Stone Blade

While holding a stone, the player continuously left-clicks a stone block to grind it. This action does not break the target block and is planned to take longer than mining a beacon by hand, emphasizing the primitive tool-making process. Completion produces a stone blade.

The stone blade has two functions:

- Slowly mines polluted dirt and obtains the block item.
- Scrapes charcoal to produce charcoal dust.

During implementation, the "grinding progress" should be bound to the player, held item, and target position. Any change in these conditions interrupts progress. Resolution must occur only on the server to avoid generating an item once on each side.

## 4. Fire-Starting Process

The planned sequence is:

1. Spread charcoal dust on the ground.
2. Hold a stone in the off hand and a stone blade in the main hand.
3. Hold right-click on the charcoal-dust position to start a fire.
4. Fire on a charcoal block serves as a stable heat source that does not extinguish naturally.

The fire-starting mechanic should distinguish ordinary fire, permanent carriers, and intentional player destruction. Permanent fire does not mean irremovable fire: it should still be cleared when the block is destroyed, waterlogged, or its rule conditions become invalid, preventing ownerless fire from remaining behind.

## 5. Clay Furnace

### 5.1 Construction

The player holds clay and places it repeatedly on the ground to form an unfired furnace body. A continuous heat source below the body slowly fires it, after which it becomes a hardened, functional structure.

### 5.2 Functions

The clay furnace is the only planned processing device in LV-0. It is slower than a vanilla furnace, but supports:

- Processing polluted water into purified water.
- Cooking basic food.
- Processing sifted metal forms in LV-1.
- Melting alloys and casting pickaxe heads with molds.

### 5.3 Key States

The furnace must record at least construction completion, firing progress, heat-source validity, processing progress, and input/output. If a multiblock structure is damaged, it should pause rather than continue offline processing; it can resume only after the structure is validated again.

## 6. Starting the Small Sifting Array

After obtaining talc, the player holds clay in the off hand and talc in the main hand, then draws a small sifting array on the ground. LV-0 covers only its discovery and drawing; actual metal sifting belongs to LV-1.

The first array must provide clear feedback: drawing trails, closure validation, invalid-position prompts, required-material prompts, and activation results. If it relies only on invisible rules, early-game players will struggle to understand why it failed.

## 7. Stage Completion Checklist

- Dirt piles can be obtained reliably and crafted into dirt.
- Clay pellets can be collected and a clay furnace can be fired.
- A stone blade can be crafted, charcoal dust can be scraped, and a fire can be started.
- Polluted water can be purified.
- Talc can be obtained and a small sifting array can be drawn.
- Conventional ore veins or mature automation still cannot be used to skip progression.

## 8. Balance and Debugging

- Record how many interactions players need to obtain their first piece of talc and determine whether 1% creates an excessively long gap.
- Check whether right-click searching can accidentally kill players at low health and whether minimum-health protection is needed.
- Verify clay furnace state consistency after heat-source removal, chunk unloading, and structural damage.
- Check compatibility between permanent heat sources and fire-spread game rules, rain, and fluid updates.

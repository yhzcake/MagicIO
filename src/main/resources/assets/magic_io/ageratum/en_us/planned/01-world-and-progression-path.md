---
title: "World and Progression Path"
navigation:
  title: "Chapter 1"
---

# World and Progression Path

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** It describes the intended gameplay and does not mean that the corresponding dimensions, resources, items, blocks, recipes, or interactions exist in the current build. For runnable content, refer to the status documentation under `../current/` and the source code.

## 1. Design Goals

MagicIO plans to provide a complete technology path from manual survival in a post-apocalyptic world to an elemental endgame. Instead of directly copying vanilla mining progression, its core progression asks players to separate resources from pollutants, gradually discover elements, establish arrays, connect power, and automate production, ultimately gaining control over probability, space, time, and matter.

The planned core loop is:

1. Obtain low-purity, highly random basic resources from the environment.
2. Complete the earliest processing with hand tools and a clay furnace.
3. Discover talc and draw the first array.
4. Use elemental arrays to expand processing options and improve efficiency and resource purity.
5. Establish an Array Energy network, storage, and logistics to reduce manual work.
6. Eliminate randomness through composite elements and build deterministic production lines.
7. Enter the stage of advanced composite arrays and autonomous networks.

## 2. World Entry

### 2.1 Polluted World Preset

The polluted world is planned as an optional starting world: it is cold overall, the sky is covered by gray-yellow haze, bodies of water are polluted, and conventional ore generation is disabled. Its purpose is not merely to increase survival difficulty, but to ensure through world rules that players must use MagicIO's resource separation system.

| World Feature | Planned Behavior | Effect on Progression |
| --- | --- | --- |
| Biome temperature | Approximately -2 | Reinforces the cold post-apocalyptic atmosphere |
| Conventional ores | Do not generate | Blocks the vanilla mining shortcut |
| Polluted water | Cannot be drunk directly and does not freeze | Forces players to learn water purification |
| Surface resources | Polluted dirt, charred wood remains, etc. | Supports the LV-0 start |
| Visual palette | Dark gray, dark brown, and haze | Establishes a clear world theme |

### 2.2 Normal Overworld

The normal world preset is planned to retain vanilla resource generation while only adding talc ore. Players in the polluted world can enter a normal world in the mid-to-late game through space-related mechanics. This preserves a conventional compatibility path for modpacks while also allowing challenge gameplay built specifically around the post-apocalyptic preset.

## 3. Stage Overview

| Stage | Tier Range | Core Problem | Key Breakthrough |
| --- | --- | --- | --- |
| Post-apocalyptic survival | LV-0 | How to obtain water, dirt, fire, and tools | Clay furnace, stone blade, small sifting array |
| Array initiation | LV-1–LV-2 | How to obtain metals from polluted resources | Talc, metal sifting, elemental primers |
| Array power | MV–HV | How to run arrays continuously and efficiently | Array Energy, stable arrays, upgrade modules |
| Advanced era | HV–EV | How to expand production-line throughput | Storage, parallel processing, filters, and channels |
| Eliminating randomness | EV–IV | How to precisely control products | Chaos arrays and deterministic recipes |
| Elemental endgame | IV–MAX | How to make networks autonomous and manipulate the world | Description, Energy, Consciousness, and Creation |

## 4. Progression Gates

Each progression tier should be driven by an observable and verifiable change in capability rather than merely replacing material names:

- The LV-0 gate is obtaining talc and completing the first array pattern.
- The LV-1 gate is gathering enough random metals to make a usable alloy pickaxe.
- The LV-2 gate is completing the discovery of basic elements and primer purification.
- The MV gate is obtaining a stable primer and connecting an array to Array Energy.
- The HV gate is establishing batch processing and preliminary automatic input and output.
- The EV gate is establishing storage, channel-based logistics, and composite elemental arrays.
- The IV gate is converting probabilistic production into deterministic production.
- The MAX gate is completing advanced composite elements and establishing an autonomous network.

## 5. Path Constraints

- The polluted world must not allow conventional ore veins to bypass the resource separation stage.
- Key random products need a pity or accumulation mechanism so players are not permanently unable to obtain gate materials such as ZA-27.
- Higher-tier arrays should be backward-compatible with lower-tier recipes, while speed increases and energy consumption remain balanced.
- Automation should unlock progressively: manual, semi-automatic, directed automatic, network automatic, and autonomous.
- Cross-dimensional travel and matter creation are endgame capabilities and should not prematurely remove resource pressure.

## 6. Reading Order

Read this chapter first, followed by [LV-0 Post-Apocalyptic Survival](02-lv-0-post-apocalyptic-survival.md), [Array System](03-zhen-system.md), [Element System](04-element-system.md), [Power and Upgrades](05-power-and-upgrades.md), [Automation](06-automation-storage-and-logistics.md), and [Advanced Era and Endgame](07-advanced-era-and-elemental-endgame.md). Finally, use the [Status Index](08-planned-status-index.md) to distinguish plans from the current state.

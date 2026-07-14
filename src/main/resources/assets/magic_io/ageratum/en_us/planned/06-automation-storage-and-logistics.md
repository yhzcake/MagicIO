---
title: "Automation, Storage, and Logistics"
navigation:
  title: "Chapter 6"
---

# Automation, Storage, and Logistics

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** The storage blocks, transmission nodes, channels, batch processing, and autonomous capabilities described here are target designs.

## 1. Automation Progression

| Stage | Input | Output | Control Capabilities |
| --- | --- | --- | --- |
| LV | Manual player insertion | Ejected dropped items | None |
| MV | Hoppers, pipes, or adjacent faces | Directed output | Basic direction configuration |
| HV | Active extraction | Automatic ejection | Filtering and batching |
| EV | Network scheduling | Network delivery | Channels, targets, and recipe memory |
| IV | Automatic recipe-chain supply | Deterministic output | Orchestration and autonomy |

Progressive unlocking lets players first understand the input and output of a single array before handling network-scale problems. If full remote logistics is available at LV, spatial layout and directional design of arrays lose their meaning.

## 2. Internal Buffers

Planned capacities are 1 stack per input slot at MV, 3 stacks at HV, and 9 stacks at EV. Buffers absorb logistical fluctuations and are not long-term storage. Input filtering and recipe locking must work together, or recipes sharing a buffer may occupy one another's slots.

Item and fluid operations should use a simulate-then-execute transactional model. All inputs for one recipe must be extractable at once, and all outputs must be receivable at once, preventing duplication or item loss from partial success.

## 3. Independent Storage

| Device | Planned Capacity | Purpose |
| --- | --- | --- |
| Basic storage box | 1 stack of each type | Single-material buffer |
| Expanded storage box | 9 stacks of each type | Multi-slot categorized buffer |
| Network storage core | Limited by total quantity | Centralized storage and remote access |

Network storage requires stable item-identity rules. In addition to item ID, data components, durability, enchantments, and data attached by external mods must be considered; items with different components must not be merged incorrectly.

## 4. Parallel and Batch Processing

HV is planned to unlock 2× parallel processing, while EV unlocks 4×. Parallel processing means handling multiple independent sets of inputs simultaneously; energy consumption and space requirements should scale with the number actually executed.

Batch-processing modes include:

- Single batch: start only one recipe set at a time.
- Fixed batch: schedule N sets of identical inputs at once.
- Loop: continue starting recipes while inputs, outputs, and energy permit.

Loop mode must have stopping conditions and pause automatically when output is blocked. It must not continue extracting inputs and then drop products into the world.

## 5. Transmission Tiers

| Method | Planned Range and Rate | Role |
| --- | --- | --- |
| Direct transfer through touching faces | Adjacent, approximately 1 stack per transfer | Most basic deviceless transfer |
| AE transmission node | Approximately 32 blocks, approximately 1 stack/2 seconds | HV remote connection |
| AE transmission bus | High speed and cross-dimensional | EV advanced logistics backbone |

Cross-dimensional transport must not force-load every target chunk by default. It may operate only while the destination is online, use restricted chunk tickets, or follow server configuration. Whichever approach is used, offline status must be displayed explicitly.

## 6. Filters and Channels

Filter nodes are planned to support allowlists and blocklists. Channel nodes let different resources share a physical network while remaining logically isolated, and directed transfer specifies a target inventory.

Example:

```text
Channel 1: Metal materials → Metal Array → Alloy output
Channel 2: Combustibles → Fire Array → Thermal-processing output
Channel 3: Fluids → Water Array → Purified-fluid output
```

In addition to exact items, filters should support tag matching, ignoring quantity, and data-component matching modes. Default rules must be conservative: when configuration is incomplete, transport should stop rather than send items to the wrong target.

## 7. Recipe Memory and Orchestration

EV arrays are planned to remember successfully executed recipes for quick player selection. IV Order capabilities further combine the steps of multiple arrays into recipe chains. Recipe chains must handle branches, byproducts, cyclic dependencies, timeouts, and insufficient intermediate storage.

The orchestrator should generate an execution plan first, then reserve inputs and outputs incrementally. If execution fails partway through, it should release reserved resources and retain a diagnosable failing node rather than silently resubmitting the task.

## 8. Automated Primer Production

| Primer | Planned Production Line |
| --- | --- |
| Stable primer | Wood Array + basic primer |
| Reinforced primer | Metal Array + stable primer + fine metal powder |
| Infused primer | Space Array + reinforced primer + space fragment |

Primer production lines serve as stage acceptance tests for automation: they simultaneously require working raw-material supply, array processing, energy, logistics, and output storage.

## 9. Performance Boundaries

- Network scheduling should be event-driven or run at a limited frequency rather than scan every container each tick.
- Transfer tasks need a per-network budget to prevent large factories from consuming all server tick time.
- Dropped-item output should be a last resort; automation stages should prefer retaining products in internal buffers.
- Cross-chunk connections, dimension unloading, and data pack reloading must all preserve transactional consistency.

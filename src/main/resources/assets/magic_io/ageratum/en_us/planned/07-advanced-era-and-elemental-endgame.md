---
title: "Advanced Era and Elemental Endgame"
navigation:
  title: "Chapter 7"
---

# Advanced Era and Elemental Endgame

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** In particular, IV–MAX remains only a preliminary framework in the root plan, and its names, combination relationships, capabilities, and costs may all be reworked.

## 1. HV–EV: Advanced Era

The focus of this stage is not adding more manual work, but increasing the throughput, controllability, and observability of existing production lines. Players should complete centralized storage, parallel processing, filtering channels, and automated primer production, providing infrastructure for composite elements.

Stage acceptance criteria include:

- Intermediate and finished products enter stable storage rather than becoming large numbers of dropped entities.
- A single array can process inputs in batches or in parallel.
- The network can route resources to correct targets by channel.
- Basic and reinforced primers can be produced continuously and automatically.
- When a production line is blocked, it can identify the specific node.

## 2. EV–IV: Eliminating Randomness

Early sifting and byproducts depend on probability. The core EV–IV goal is to let players exchange substantial technological cost for determinism.

### 2.1 Chaos Array Modes

| Mode | Effect | Planned Cost |
| --- | --- | --- |
| Random mode | Resolves according to original probability | Base AE |
| Probability lock | Selects one specific result | Approximately 5× AE |
| Deterministic conversion | Registers the target random process as a fixed process | Large one-time AE cost |

Determinism must not preserve the original average cost of rare products, or it would flatten progression value. A reasonable approach is to derive energy and input multipliers from the target product's original probability and apply a recipe allowlist.

### 2.2 Other Composite Arrays

- Order Arrays handle conditional routing, path switching, and recipe orchestration.
- Space Arrays handle cross-dimensional networks and pocket storage.
- Time Arrays handle controlled acceleration and scheduled execution.

Together, all four form fully deterministic production lines: Order selects the process, Space connects resources, Time adjusts progress, and Chaos fixes the result.

## 3. EV and IV Arrays

EV Infused Arrays are planned to include automatic input/output, 4× parallel processing, filtering, and recipe memory. IV Ancient Arrays are planned to support deterministic processing, composite inputs and outputs, and cross-array recipe chains.

After reaching IV, performance limits should not rely only on increasing per-tick execution counts. Batched mathematical resolution is more suitable: calculate total inputs, outputs, and energy costs for N recipe executions at once, reducing the server overhead of large factories.

## 4. Spirit Pearls and Orbs

Spirit Pearls for the eight elements enhance basic elemental arrays, while Orbs for the four elements provide network-level composite capabilities. Their crafting paths should require the corresponding elemental production lines to reach EV/IV rather than merely collecting a one-time rare material.

Orbs may involve network binding, so they need to record owner, network identifier, and security permissions. Item duplication, death drops, or cross-server migration must not allow one Orb to control multiple networks simultaneously.

## 5. IV–MAX Advanced Composite Arrays

| Array | Planned Combination | Core Role |
| --- | --- | --- |
| Description Array | Order + Chaos | Information reading, pattern saving, restricted data modification |
| Energy Array | Space + Time | Energy conversion, wireless transmission, and centralized storage |
| Consciousness Array | Lightning + Earth + Energy | Experience, souls, entity control, and binding |
| Creation Array | Description + Energy + Consciousness | Ultimate conversion from energy to matter |

These combinations are currently only a framework. Consciousness and Creation capabilities in particular require server permissions, blocklists, resource-value models, and cross-mod security boundaries to be established before implementation begins.

## 6. Autonomous Networks

The endgame network is planned to schedule production automatically according to inventory targets: read deficits, select recipe chains, reserve resources, schedule arrays, process byproducts, and replenish energy. Autonomy does not mean unlimited self-replication; players must still configure goals and resource budgets.

A task state machine is recommended: awaiting planning, reserved, executing, outputting, complete, blocked, and canceled. Every task retains source and consumption records, helping players locate cyclic production or resource anomalies.

## 7. Creation Capability Boundaries

- Only items with explicitly registered values may be created as matter.
- Items with complex data, container contents, or external capabilities are prohibited from duplication by default.
- Energy costs should account for raw-material rarity and processing chains rather than item count alone.
- Creation processes must be restricted by server configuration, permissions, and rate limits.
- Task cancellation, power loss, or chunk unloading must not return more resources than have been consumed.

## 8. Endgame Completion Criteria

- The path from the polluted-world start to IV has no unrecoverable random progression locks.
- All automated production can trace inputs, outputs, energy, and task origins.
- Advanced capabilities do not bypass server permissions or duplicate external mod data.
- Large networks run stably within a reasonable server tick budget.
- MAX provides a long-term goal without making all previous elements and production lines obsolete.

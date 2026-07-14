# Planned Status Index

> [!CAUTION]
> **This entire directory contains planned-version tutorials, and its gameplay descriptions are not evidence of current implementation.** Refer to `../current/` for currently runnable mechanics. This index tracks the target scope and does not indicate that any entry is complete.

## 1. Documentation Layers

| Location | Meaning | Usage |
| --- | --- | --- |
| `docs/tutorial/current/` | Current-state version | Describes available capabilities according to current source code and resources |
| `docs/tutorial/planned/` | Planned version | Describes target capabilities according to the complete development plan in the root directory |
| `MagicIO_完整开发规划.md` | Planning source | Provides the overall technology tree, system tables, and priorities |

When the current-state and planned versions conflict, use the current-state version and source code to determine current functionality. When the planned version conflicts with the root plan, use the updated root plan as the design baseline.

## 2. Eight-Chapter Index

| Chapter | Scope | Overall Status |
| --- | --- | --- |
| [World Progression](01-world-and-progression-path.md) | World presets, stage path, progression gates | Planned, not implemented |
| [LV-0](02-lv-0-post-apocalyptic-survival.md) | Polluted dirt, stone blade, fire starting, clay furnace | Planned, not implemented |
| [Arrays](03-array-system.md) | Drawing, activation, tiers, recipe execution | Planned, not implemented |
| [Elements](04-element-system.md) | Basic, composite, and advanced elements | Planned, not implemented |
| [Power and Upgrades](05-power-and-upgrades.md) | AE, charcoal power, tiers, and modules | Planned, not implemented |
| [Automation](06-automation-storage-and-logistics.md) | Storage, parallel processing, logistics, and orchestration | Planned, not implemented |
| [Advanced Endgame](07-advanced-era-and-elemental-endgame.md) | Deterministic processing, autonomy, and creation | Planned, not implemented |
| Status Index | Documentation boundaries and implementation path | This index |

## 3. Feature Status Matrix

| System | Target Stage | Planning Status | Acceptance Focus |
| --- | --- | --- | --- |
| Polluted world preset | LV-0 | Not implemented | Terrain, water, ore replacement, spawn flow |
| Polluted dirt interactions | LV-0 | Not implemented | Dual actions, probability, health cost, server-side resolution |
| Stone blade and fire starting | LV-0 | Not implemented | Hold progress, heat-source state, item consumption |
| Clay furnace | LV-0 | Not implemented | Structure, firing, heat source, recipes, and unload recovery |
| Small sifting array | LV-1 | Not implemented | Drawing, activation, random metals, and pity system |
| Metal forms and casting | LV-1 | Not implemented | Form conversion, alloys, molds, and tool gates |
| Element discovery | LV-2 | Not implemented | Player progression, combination experiments, unlock feedback |
| Array upgrades | MV–IV | Not implemented | Data migration, tier compatibility, visual changes |
| Array Energy network | MV–HV | Not implemented | Network topology, supply and demand, storage, and performance |
| Upgrade modules | MV–EV | Not implemented | Numerical order, slots, and energy balance |
| Automatic input/output | MV–HV | Not implemented | Face configuration, transactions, and blockage recovery |
| Storage and channel logistics | HV–EV | Not implemented | Item identity, filtering, scheduling, and cross-chunk operation |
| Composite elements | EV–IV | Not implemented | Order, Chaos, Space, and Time |
| Deterministic processing | EV–IV | Not implemented | Probability cost, allowlists, and recipe registration |
| Advanced composite elements | IV–MAX | Framework needs refinement | Data security, permissions, and value model |
| Autonomous network | MAX | Framework needs refinement | Task states, resource reservation, cycle detection |

## 4. Implementation Order

### Milestone 1: Minimal LV-0 Loop

First complete the polluted-world resource entry point, dirt interactions, stone blade, fire starting, clay furnace, and first array pattern. The acceptance goal is for players to survive reliably and reach the array system without using vanilla ores.

### Milestone 2: LV-1–LV-2

Complete the sifting array, random metal forms, ZA-27 or a silver-copper pickaxe, basic primers, the eight elements, and element discovery. The acceptance goal is a closed loop from polluted dirt to metal tools and basic array processing.

### Milestone 3: MV–HV

Complete Array Energy, charcoal power, Stable and Reinforced tiers, upgrade modules, and automatic input/output. The acceptance goal is for production lines to run continuously with clear feedback for blockage and insufficient energy.

### Milestone 4: HV–EV

Complete storage, parallel processing, batching, filtering, channels, and automated primer production. The acceptance goal is for medium-sized factories to operate without players moving materials between every machine.

### Milestone 5: EV–IV

Complete the four composite elements, Infused and Ancient tiers, Spirit Pearls, Orbs, and deterministic processing. The acceptance goal is for early random production lines to convert into specified products at a reasonable cost.

### Milestone 6: IV–MAX

After completing the design, implement Description, Energy, Consciousness, Creation, and autonomous networks. The acceptance goal is not only powerful functionality, but also permissions, data security, resource conservation, and server performance.

## 5. Status Maintenance Rules

- After a planned feature is completed, first add a verifiable description to the current-state version according to the source code.
- Only capabilities verified through actual execution or testing may change from "not implemented."
- Partial implementations must list the completed scope and missing boundaries rather than being marked complete directly.
- Parameter changes should update the corresponding chapter and this index together.
- Endgame content still marked "to be supplemented" in the root plan must not be presented as finalized in the tutorials.

## 6. Current Conclusion

The planned directory is a design-path description, not a current user manual. Always read it under the premise that the plans are not implemented. When building, debugging, or developing existing mechanics, return to the current-state tutorials and verify the source-code facts.

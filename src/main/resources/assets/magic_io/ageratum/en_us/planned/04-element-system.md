# Element System

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** Element discovery relationships, processing capabilities, and endgame effects may all change during formal implementation and balancing.

## 1. Element Design Principles

Elements are not merely recipe categories; they abstract processing mechanics. Each element governs a class of physical or logical changes, while composite elements govern higher-level process, probability, and information control.

## 2. Eight Basic Elements

| Element | Domain | Representative Capabilities | Machine Analogy |
| --- | --- | --- | --- |
| Earth | Inorganic solids | Sifting, centrifuging, pressing, sedimentation | Sifter, compressor |
| Water | Fluids | Purification, transport, dissolution, distillation | Pump, refinery |
| Fire | Internal energy and heat | Heating, smelting, heat treatment | Furnace, induction furnace |
| Air | Mechanical energy | Crushing, grinding, transport, vacuum | Grinder, piping |
| Wood | Organic matter | Cultivation, fermentation, composting, extraction | Growth chamber, fermentation tank |
| Ice | Phase changes | Refrigeration, condensation, liquefaction, temperature control | Freezer |
| Metal | Chemistry and metallurgy | Purification, alloying, displacement, catalysis | Reactor, alloy furnace |
| Lightning | Electromagnetism | Power generation, electrolysis, electroplating, magnetization | Generator, magnetizer |

## 3. Initial Primers

The earliest planned primers include charcoal dust as the Fire primer, clay pellets as the Earth primer, purified water as the Water primer, and the empty-hand state as the Air primer. Primers connect materials players understand with abstract elements.

"Empty hand" is not an item, so recipe and interaction APIs must express it as a contextual condition rather than fabricating an invisible item. Other primers should explicitly define whether they are consumed, whether containers are returned, and how automation devices provide them.

## 4. Basic Discovery Chain

```text
Earth + Water → Wood
Water + Air → Ice
Fire + Earth → Metal
Air + Fire → Lightning
```

This discovery chain lets players first master four intuitive elements, then understand complex processing through combinations. Discovery should require two arrays to reach specified states, have a valid structural relationship, and complete an experiment, rather than simply crafting a new item at a crafting table.

## 5. Four Composite Elements

| Element | Combination | Planned Capabilities |
| --- | --- | --- |
| Order | Wood + Metal | Conditional routing, sequential execution, load balancing, recipe orchestration |
| Chaos | Ice + Lightning | Probability enhancement, probability locking, deterministic conversion |
| Space | Ice + Wood | Remote transport, spatial expansion, cross-dimensional connections |
| Time | Metal + Lightning | Acceleration, deceleration, pausing, scheduled execution |

These capabilities change the entire production system, so tier, Array Energy, and network-size gates are necessary. For example, time acceleration must not recursively affect another Time Array without limit; spatial transport must handle unloaded destination chunks and cross-dimensional permissions.

## 6. Advanced Elements

| Element | Combination Requirement | Role |
| --- | --- | --- |
| Description | Order + Chaos | Read and modify item data, save patterns |
| Energy | Space + Time | AE/FE conversion, wireless power transmission, energy-quality improvement |
| Consciousness | Lightning + Earth + Energy | Experience, souls, entity detection, and binding |
| Creation | Description + Energy + Consciousness | Generate matter from energy and reach rule-level capabilities |

Advanced capabilities require strict data allowlists. If a Description Array can arbitrarily write components or NBT, it will cause duplication, unauthorized items, and cross-mod crashes. A Creation Array should likewise use explicitly registered matter costs rather than automatically assigning values to every item.

## 7. Primer Tiers

| Primer | Corresponding Stage | Purpose |
| --- | --- | --- |
| Basic primer | LV | Activates basic elements |
| Purified primer | LV-2 | Supports stable processing experiments |
| Stable primer | MV | Upgrades an array to the Stable tier |
| Reinforced primer | HV | Unlocks batching and greater capacity |
| Infused primer | EV | Unlocks full automation and composite elements |
| Ancient primer | IV | Unlocks deterministic processing and complex orchestration |

Mass-producing primers is a mid-to-late-game automation goal. Each primer tier should depend on the previous tier's production line, maintaining a closed technology tree and preventing high-tier materials from easily bypassing progression through ordinary loot.

## 8. Spirit Pearls and Orbs

The EV stage plans to provide Spirit Pearls for the eight basic elements as portable enhancement cores for their corresponding arrays. At IV, Orbs for Order, Chaos, Space, and Time provide network-level capabilities. They should be functional carriers rather than mere numerical materials, with clear insertion, removal, binding, and duplication restrictions.

## 9. Compatibility Principles

- Elemental recipes should prefer item and fluid tags, reducing hard-coded dependencies on specific mod IDs.
- Energy conversion must declare conversion ratios, losses, and per-tick limits.
- Data modification should expose only safe fields and prohibit arbitrary copying of external mods' internal state.
- Entity and soul capabilities should respect blocklists, Boss checks, and server permissions.
- Time acceleration should advance through controlled interfaces rather than unconditionally adding random ticks or directly repeating block-entity ticks.

# Power and Upgrades

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** Array Energy units, power-supply devices, upgrade values, and tier capabilities are all planned targets.

## 1. Array Energy System

Arrays are planned to operate using Array Energy (AE). AE is internal energy for array networks and should not be confused with the common abbreviation for storage networks. After reaching MV, most continuous processing requires stable power, while LV retains inefficient, manual operation or operation assisted by environmental heat sources.

## 2. Charcoal Power

Charcoal is the planned core energy source from MV to HV, continuing the charred-wood and fire-starting path from LV-0.

| Component | Planned Responsibility |
| --- | --- |
| Charcoal power base | Consumes charcoal and supplies AE to adjacent arrays |
| Charcoal power core | Serves multiple arrays simultaneously and improves fuel efficiency |
| AE transmission node | Extends the power network within a certain range |
| Energy storage core | Buffers generation fluctuations and handles peak loads |

Fuel resolution should first convert items into remaining burn time, then output energy per tick. At full device load, fuel consumption should pause or waste should be limited, while remaining energy remains consistent across chunk unloading.

## 3. Network Rules

- Adjacent arrays share AE through touching faces.
- Transmission nodes extend network range and establish independent connections.
- Consumers draw from the network according to demand and must not create negative energy.
- Energy storage units absorb surplus and supplement the network during insufficient generation.
- After networks split or merge, recalculate node sets, capacity, and load.

The network implementation should avoid traversing the entire connected graph from every node each tick. A more robust approach is to rebuild the network when topology changes and process only cached producer, consumer, and storage lists during normal ticks.

## 4. Tiers and Slots

| Tier | Base Efficiency | Slots | Planned Capabilities |
| --- | --- | --- | --- |
| LV Unstable | 1× | 0 | Manual processing |
| MV Stable | Approximately 1.5× | 1 | Starting point for automatic input/output |
| HV Reinforced | Approximately 2× | 3 | Buffering and batch processing |
| EV Infused | Advanced | To be balanced | 4× parallel processing, built-in filtering |
| IV Ancient | Final | To be balanced | Deterministic processing and recipe chains |

Tier multipliers must not unconditionally multiply speed, parallelism, and output at the same time, or total throughput will grow exponentially. Final values should jointly balance AE cost per unit of product, occupied space, and server tick cost.

## 5. Upgrade Modules

| Module | Planned Effect | Cost or Limitation |
| --- | --- | --- |
| Speed upgrade | Increases processing speed by approximately 50% | Energy consumption increases accordingly |
| Enhanced speed upgrade | Increases speed by approximately 100% | Greater energy consumption and a tier requirement |
| Efficiency upgrade | Reduces energy consumption by approximately 25% | Requires a stacking floor |
| Output upgrade | Increases byproduct probability | Should not create additional primary products from nothing |
| Capacity upgrade | Increases input/output buffers | Excess contents must be cleared before structural downgrading |

Upgrade calculations should process base values, tier multipliers, parallelism, speed, and efficiency in a fixed order so installation order does not change the result. The configuration interface should display final speed, per-tick energy consumption, parallel count, and estimated completion time.

## 6. Higher-Tier Compatibility

Higher-tier arrays are planned to execute all lower-tier recipes and gain speed based on tier difference. Compatibility does not mean free operation: advanced arrays must still satisfy input, element, and power conditions. If a recipe depends on LV environmental behavior, an advanced executor needs an explicit simulation method to prevent inconsistent results caused by missing environmental conditions.

## 7. Behavior During Insufficient Energy

When energy is insufficient, processing should pause rather than regress or consume materials. Processing state should record at least the recipe identifier, processed progress, locked inputs, and required output space. Processing resumes when power returns. If a data pack reload removes the recipe, inputs should be returned safely and the invalidation reason reported.

## 8. Balance Metrics

- Number of baseline recipes completed per unit of charcoal.
- Time and total AE required by different tiers to complete the same recipe.
- Peak network load caused by throughput increases from speed modules.
- Space and cost benefits of parallel arrays compared with multiple lower-tier arrays.
- Large-network share of the server's per-tick budget.

## 9. Debugging Methods

The planned implementation should provide read-only diagnostic information: network identifier, current generation, consumption, stored energy, node count, array demand, and blockage reasons. Diagnostics are for observation only and should not rely on clearing caches or changing the environment to conceal network-state errors.

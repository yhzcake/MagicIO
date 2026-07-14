# Array System

> [!CAUTION]
> **This entire chapter is planned content and has not yet been implemented.** It describes the intended array experience. The presence of identically named tiers, types, or array blocks in the current code does not mean that the drawing, upgrading, and network gameplay planned here is complete.

## 1. Array Structure

Planned arrays consist of four concepts:

| Part | Responsibility |
| --- | --- |
| Array base | Talc array patterns attached to solid block surfaces |
| Array focus | The position where primers are placed, activation occurs, and primary interactions take place |
| Core | Holds tier and elemental capabilities |
| Auxiliary slots | Install speed, efficiency, output, capacity, and other upgrades |

An array pattern is not an ordinary full block, but a surface structure with an attachment face and orientation. Implementation requires unified coordinate transformations so drawing, collision, rendering, and output direction on all six faces use the same local-coordinate rules.

## 2. Drawing Process

1. The player holds talc in the main hand and places the target elemental primer in the off hand.
2. The player drags across a solid block surface to form an array-pattern trail.
3. The system determines whether the trail is closed and matches a valid pattern.
4. The player right-clicks the array focus, consuming the primer to complete activation.
5. The activated array receives an element, tier, orientation, and set of available recipes.

Drawing validation should tolerate minor input errors, but must not rely only on bounding-box approximation, which could easily misidentify different patterns. The recommended approach is to discretize the trail onto a regular grid, then perform rotation, mirroring, and template matching.

## 3. Basic Operations

| Action | Planned Behavior |
| --- | --- |
| Normal right-click on the array focus | Activate, open interaction, or insert materials |
| Talc + sneak-right-click | Rotate the output face |
| Talc + sneak-left-click | Remove the array pattern and return some materials |
| Manual insertion | Primary input method during the LV stage |
| Ejected output | Primary output method during the LV stage |

All operations that change block states, inventories, or dropped items should be confirmed by the server; the client handles only gesture prediction and visual feedback.

## 4. Tier Sequence

| Tier | Name | Planned Characteristics | Pattern Appearance |
| --- | --- | --- | --- |
| LV | Unstable | Fully manual, basic recipes, 1× efficiency | Dark gray, sparse particles |
| MV | Stable | 1 upgrade slot, semi-automatic input/output | Silver-white, continuous glow |
| HV | Reinforced | 3 upgrade slots, batch processing | Gold, pulsing halo |
| EV | Infused | 4× parallel processing, built-in filtering | Multicolored, encircling halo |
| IV | Ancient | Deterministic processing, complex recipe chains | Deep purple with gold trim, dual rotating rings |

The planned upgrade path uses a stable primer to upgrade an Unstable Array to a Stable Array, followed in order by reinforced, infused, and ancient primers. Upgrading should preserve valid inventory contents and orientation. If the new tier changes capacity, lossless migration must be validated first.

## 5. Recipe Execution

Array recipes need to express item inputs, fluid inputs, outputs, processing time, Array Energy consumption, elemental function, minimum tier, and probabilistic products. Higher-tier arrays are backward-compatible with lower-tier recipes and may gain speed from tier differences.

A recommended execution sequence is:

1. Match recipes permitted by the current tier and element.
2. Simulate extraction of all inputs to ensure the transaction can complete.
3. Check output space and energy supply.
4. Lock the recipe and advance processing progress.
5. On completion, consume inputs and generate outputs transactionally.
6. If output is blocked, preserve the completed state instead of discarding products.

## 6. Composite Array Discovery

Basic elements combine to reveal later elements: Earth and Water lead to Wood, Water and Air lead to Ice, Fire and Earth lead to Metal, and Air and Fire lead to Lightning. MV elements further combine into Order, Chaos, Space, and Time; the endgame then combines them into Description, Energy, Consciousness, and Creation.

"Discovery" should be player knowledge progression rather than merely a hidden recipe. The server needs to record discovery state, and the client should use it to display tutorials, recipes, and patterns, preventing players from bypassing exploration simply by inspecting data files.

## 7. Size and Attachment

Planned visual sizes progress from 1×1 at LV to 2×2 at MV/HV and 3×3 at EV/IV. Larger sizes introduce cross-block structures, chunk boundaries, arbitrary surface orientations, and partial structural destruction. There should be exactly one controller position, while auxiliary positions store only a reference to the controller, preventing multiple block entities from executing the same recipe repeatedly.

## 8. Failure Feedback

- Incomplete pattern: highlight missing or incorrect nodes.
- Wrong primer: display the required element without consuming the current item.
- Insufficient tier: display the minimum array tier.
- Insufficient energy: pause progress and preserve inserted materials.
- Blocked output: stop new recipes and preserve pending results.
- Damaged structure: stop working immediately and validate again after repair.

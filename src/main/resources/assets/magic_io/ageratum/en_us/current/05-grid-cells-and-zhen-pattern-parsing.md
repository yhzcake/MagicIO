---
title: "Grid Cells and Zhen Pattern Parsing"
navigation:
  title: "Chapter 5"
---

# Chapter 5: Grid Cells and Zhen Pattern Parsing

## 1. Two Grid Carriers

There are currently two carriers: the standalone Grid Panel and the Zhen Bus grid surface processor. Both use 4×4 cell storage, but neither is a complete, closed-loop formal Zhen-drawing system.

The standalone Grid Panel is a horizontal sheet 1/16 of a block thick. A grid processor in the Zhen Bus can be located on any surface and converts click coordinates into two-dimensional grid coordinates according to the surface direction.

## 2. Editing Operations

The main hand must hold a stick while editing:

- Hold the item corresponding to a valid action in the offhand and click the target cell with the stick in the main hand to write that action into the cell.
- With the offhand empty, click the target cell with the stick in the main hand to clear that cell.

The current action mappings are:

| Offhand Item | Action | Element |
| --- | --- | --- |
| Blaze powder | fire | Fire |
| Water bucket | water | Water |
| Dirt | earth | Earth |
| Feather | wind | Wind |

The `empty` action is registered using air, but an empty offhand clears the cell rather than actively writing that action.

## 3. Parsing Trigger Conditions

The Zhen Bus grid processor runs rules only when all 4×4 cells are filled and parsing has not yet been triggered in the current cycle. It iterates through registered rules and executes the first match. The grid snapshot and trigger state are persisted.

The only built-in rule is currently named `unstable_sift_zhen`. Its matcher is constructed from a 2×2 all-Earth pattern in `SHARP` mode, with direction parameter 5. A discrepancy exists between the source comment and the actual constructor arguments, so the constructor code takes precedence. Do not copy the "4×4 all-Fire" description from the old comment.

## 4. Actual Result of the Current Rule

After this rule matches, the processor only records a log entry reporting that the rule matched at a certain position. The rule handler does not replace a block, install a Sieve Zhen, consume materials, or grant an item.

Therefore, the current grid system should be labeled as a **limited implementation/development skeleton**:

1. Cell editing and saving exist.
2. Triggering when the 4×4 grid is full exists.
3. Registry-driven pattern matching exists.
4. The business result of the only built-in rule remains a placeholder log.

## 5. Verification Path

1. Place a standalone Grid Panel and confirm that the main-hand stick and the four offhand materials can edit the corresponding cells.
2. Empty the offhand and click to confirm that a single cell is cleared.
3. To verify the Zhen Bus grid processor, first ensure that the target surface actually has the `grid_cell` type installed. The current ordinary player entry points do not provide a complete and explicit installation workflow.
4. After filling all 4×4 cells, observe the server log and confirm that parsing triggers only once.
5. Modify the grid and verify again whether the trigger flag resets as expected. Do not assume that filling it repeatedly always causes repeated execution.

## 6. Usage Limitations

- The server tick of the standalone Grid Panel and that of the Zhen Bus grid processor follow different implementation paths. Do not assume they have identical rule-triggering behavior.
- There is currently no complete Chinese prompt telling the player a cell's coordinates, action, or matching result.
- There is currently only one pattern rule, and its processing logic is a placeholder.
- This tutorial does not describe the grid as a formal Survival gameplay system for crafting Zhen blocks.
- When adding rules later, explicitly define the pattern size, direction, matching mode, material mappings, and consequences of triggering. Avoid expressing the protocol through comments alone.

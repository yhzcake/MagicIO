# Chapter 1: Project Status and Reading Scope

## 1. Basis of This Tutorial

This tutorial describes only behavior that is already registered, loaded, or executed by the source code in the current workspace. Lore, technology trees, and future items in design documents are not automatically considered available content. The order of precedence is as follows:

1. Java registration and runtime logic.
2. Data loaded with the mod from `src/main/resources`.
3. Localization text and optional compatibility code.
4. Planning documents are used only to explain design intent, not to prove that a feature has been implemented.

Therefore, "a name exists," "a plan exists," and "a class exists" do not mean that the gameplay loop is complete. Three types of labels are used in this tutorial:

- **Implemented**: The current source code has a complete registration or execution entry point.
- **Limited implementation**: The main functionality works, but interaction, data, or presentation has explicit boundaries.
- **Placeholder/debug**: The source code retains examples, tests, or incomplete entry points and should not be treated as a promise of formal gameplay.

## 2. Current Technical Baseline

| Project | Current Value | Impact |
| --- | --- | --- |
| Minecraft | 26.1.2 | Class names and resource formats in this tutorial apply only to this version |
| NeoForge | 26.1.2.41-beta | Uses NeoForge registries, capabilities, and event buses |
| Java | 25 | Building and running require a Java 25 toolchain |
| Mod ID | `magic_io` | All mod resources use this namespace |
| Mod version | 1.0.0 | The version number comes from the current Gradle properties |
| JEI | Optional at compile time only | The default build does not declare a complete JEI runtime dependency |
| Jade | Optional at compile time only | Without Jade, core processing is unaffected, but the corresponding information overlay is unavailable |

## 3. Current Content Boundaries

The only independently registered items that can currently be confirmed are coal coke and the example item. The core content mainly consists of dynamically registered Zhen blocks, the Zhen Bus, and the Grid Panel. Zhen functions are automatically expanded to higher tiers according to their "introduction tier," so the number of registered blocks is greater than the number of function definitions.

The six tiers are, in order: Unstable 0, Stable 10, Sturdy 20, Abundant 30, Archaic 40, and Primeval 50. The current code does not use the LV/MV/HV/EV/IV/MAX naming system. This tutorial follows the actual localized names.

## 4. Player-Visible Entry Points

- The Creative Mode tab lists the example item, all dynamically registered Zhen blocks, the Zhen Bus, and the Grid Panel.
- After an individual Zhen block is placed, its server-side block entity processes recipes.
- After a Zhen Bus is placed, an Unstable Sieve Zhen processor is installed on its bottom surface by default.
- Right-clicking the Zhen Bus while holding another Zhen block installs the corresponding processor on the targeted surface.
- When JEI is present, it can display Zhen recipes. When Jade is present, it can display the Zhen name, items, fluids, and energy information for a surface of the Zhen Bus.

## 5. Limitations You Must Know in Advance

1. There is currently no Survival Mode crafting chain guaranteed to provide all core blocks. The full experience described in this tutorial is better suited to Creative Mode or datapack development verification.
2. `example_item`, the example block name, example configuration options, and some English system messages remain development artifacts.
3. Individual Zhen blocks do not have a conventional menu screen. They exchange resources primarily through block capabilities and adjacent automation devices.
4. Most Zhen functions only have generic recipe processing and no dedicated tick mechanism. Their functional differences mainly come from the Zhen type and JSON recipes.
5. The Primeval Creative recipe can produce command blocks without input. This is clearly development/balance placeholder content.
6. The pollution world, primers, spirit pearls, orbs, complete energy technology tree, and other content from planning documents cannot be assumed to be implemented in the current code.

## 6. Recommended Reading Order

First understand Zhen tiers and types, then learn how an individual Zhen processes recipes. Next, read about the Zhen Bus and IO, and finally cover grids, datapacks, and compatibility presentation. When the tutorial and in-game behavior differ, first check whether another datapack overrides the current resource pack, then verify the corresponding Zhen type and input region instead of applying an old workflow from planning documents.

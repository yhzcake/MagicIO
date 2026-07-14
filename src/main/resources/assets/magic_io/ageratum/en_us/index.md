---
title: "MagicIO Tutorial Documentation"
navigation:
  title: "MagicIO Tutorial Documentation"
---

# MagicIO Tutorial Documentation

This directory contains tutorials for the current version, planned gameplay tutorials, and professional development tutorials. These three sections have different factual boundaries. Do not treat planned content as functionality already implemented in the current version.

## Reading Guide

### Current Version

For players, testers, and modpack authors who want to experience existing functionality. The content is written strictly according to the current source code and resources.

1. [Chapter 1: Project Status and Reading Boundaries](current/01-project-status-and-reading-scope.md)
2. [Chapter 2: Array Tiers, Elements, and Feature Directory](current/02-zhen-tiers-elements-and-function-catalog.md)
3. [Chapter 3: Placing and Processing with Standalone Arrays](current/03-individual-zhen-placement-and-processing.md)
4. [Chapter 4: Array Bus and Six-Sided Processors](current/04-zhen-bus-and-six-sided-processors.md)
5. [Chapter 5: Grid Cells and Array Pattern Parsing](current/05-grid-cells-and-zhen-pattern-parsing.md)
6. [Chapter 6: Developing Data Pack Array Recipes](current/06-zhen-recipes-with-datapacks.md)
7. [Chapter 7: Compatibility Displays, Debugging, and Known Limitations](current/07-compatibility-debugging-and-known-limitations.md)

### Planned Gameplay

For readers who want to understand the ultimate gameplay vision of MagicIO. This section describes planned content and does not mean that it has already been implemented in the current version.

1. [Chapter 1: World Progression](planned/01-world-and-progression-path.md)
2. [Chapter 2: LV0](planned/02-lv-0-post-apocalyptic-survival.md)
3. [Chapter 3: Arrays](planned/03-zhen-system.md)
4. [Chapter 4: Elements](planned/04-element-system.md)
5. [Chapter 5: Power and Upgrades](planned/05-power-and-upgrades.md)
6. [Chapter 6: Automation](planned/06-automation-storage-and-logistics.md)
7. [Chapter 7: Advanced Endgame](planned/07-advanced-era-and-elemental-endgame.md)
8. [Chapter 8: Status Index](planned/08-planned-status-index.md)

### Developers

For add-on mod authors, modpack authors, and data pack authors. The content covers Java extensions, data-driven systems, resource reloading, and compatibility integration.

1. [Chapter 1: Environment and Project Structure](developer/01-development-environment-and-project-structure.md)
2. [Chapter 2: Java Registries](developer/02-java-registry-system.md)
3. [Chapter 3: Array Extensions](developer/03-extending-zhen.md)
4. [Chapter 4: IO and Capabilities](developer/04-io-and-capability.md)
5. [Chapter 5: Array Bus](developer/05-zhen-bus.md)
6. [Chapter 6: Data-Driven Recipes](developer/06-data-driven-recipes.md)
7. [Chapter 7: Loot Table Outputs](developer/07-loot-table-outputs.md)
8. [Chapter 8: Resource Reloading and Network Synchronization](developer/08-resource-reload-and-network-synchronization.md)
9. [Chapter 9: JEI and Jade Integration](developer/09-jei-and-jade-integration.md)
10. [Chapter 10: API Design and Mod Compatibility](developer/10-api-design-and-mod-compatibility.md)

## Version Boundaries

- The current project targets Minecraft 26.1.2, NeoForge 26.1.2.41-beta, and Java 25.
- JEI and Jade are optional dependencies. MagicIO's basic loading is unaffected when they are not installed.
- The current version does not yet provide a complete survival acquisition path. Some content requires Creative mode, commands, or external automation devices for testing.
- The currently public Java types do not yet form an independent API module with a promise of stable compatibility. Add-on mods should target exact versions and perform compatibility testing.
- The worlds, materials, energy, and endgame systems in the planned tutorials are design goals. Actual release content may change during implementation and balance testing.

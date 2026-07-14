---
title: "Development Environment and Project Structure"
navigation:
  title: "Chapter 1"
---

# Chapter 01: Development Environment and Project Structure

## 1. Chapter Goals

Based on the current repository, this chapter explains MagicIO's runtime baseline, Gradle configuration, source directories, loading entry points, and server/client boundaries. After reading it, you should be able to determine which package a feature belongs in, which event bus should initialize it, and whether its resources should go into the handwritten or generated resource directory.

## 2. Current Technical Baseline

| Item | Current Value | Source Reference |
|---|---:|---|
| Minecraft | 26.1.2 | `minecraft_version` in `gradle.properties` |
| NeoForge | 26.1.2.41-beta | `neo_version` in `gradle.properties` |
| Java | 25 | Java Toolchain in `build.gradle` |
| Mod ID | `magic_io` | `gradle.properties` and `MagicIO.MOD_ID` |
| Java package | `cn.yhzcake.magicio` | `mod_group_id` and the source package path |
| Build plugin | NeoForge ModDevGradle 2.0.141 | `build.gradle` |

The project requires Java 25. Changing only the IDE project SDK is insufficient; the Gradle Toolchain determines the actual version used by compilation tasks. The mod version, name, license, and Minecraft/NeoForge versions are all injected into templates from `gradle.properties`, rather than maintained directly in the final `neoforge.mods.toml`.

## 3. Gradle and Run Configurations

`build.gradle` should be understood in the following layers:

1. `java-library`, `maven-publish`, and `net.neoforged.moddev` provide Java compilation, publishing, and NeoForge development run configurations.
2. `sourceSets.main.resources` merges `src/generated/resources` into the main resource set, so data-generation results are packaged with the mod.
3. `neoForge.runs` defines four run targets: `client`, `server`, `gameTestServer`, and `data`.
4. The `data` run writes generated content to `src/generated/resources` and uses `src/main/resources` as existing resource input.
5. `generateModMetadata` expands `src/main/templates` into the build directory; the template files are the metadata source files.
6. JEI and Jade currently use `compileOnly`. Their compatibility modules can be compiled, but these dependencies are not guaranteed to exist at development runtime.

### 3.1 Resource Override Risks

Main and generated resources are merged. If the same resource location appears in both directories, maintainers must explicitly decide which file is authoritative. Many zhen item models currently reside under `src/generated/resources/assets/magic_io`, while languages, block states, textures, recipes, and loot tables mainly reside under `src/main/resources`.

### 3.2 Optional Compatibility Modules

`compat/jei` and `compat/jade` depend on compile-time APIs. They declare plugin entry points through `META-INF/services`. This does not mean JEI/Jade can be assumed to exist in the runtime environment; shared core logic must not depend on compatibility plugins for initialization either.

## 4. Source Directory Responsibilities

```text
src/main/java/cn/yhzcake/magicio/
├─ MagicIO.java                 Common entry point, registration orchestration, Capability, and network events
├─ MagicIOClient.java           Client-only configuration screen and block entity renderer
├─ block/
│  ├─ entity/                   Standalone Zhen block entities and their execution methods
│  ├─ inventory/                Slot partitions, zones, and face access control
│  ├─ zhen/                     Zhen tiers, functions, types, and blocks
│  ├─ zhenbus/                  Zhen Bus, side processor containers, and virtual ports
│  └─ gridcell/                 Grid cell parsing and specialized side processors
├─ io/                          Unified IO abstractions and item/fluid/energy implementations
├─ item/crafting/               Zhen recipe loading, expansion, execution, and synchronization
├─ network/                     Custom network payloads
├─ compat/                      Optional JEI and Jade compatibility
├─ datagen/                     Data generation entry points and model generators
├─ config/                      NeoForge COMMON configuration
└─ utils/                       Element types and the element registry
```

## 5. Two Event Buses

MagicIO uses two kinds of event buses. Confusing them causes listeners to never execute.

### 5.1 Mod Event Bus

The constructor parameter `IEventBus modEventBus` registers lifecycle content:

- `DeferredRegister`: element types, IO types, zhen types, blocks, items, block entities, creative mode tabs, and recipe types.
- `FMLCommonSetupEvent`: common initialization.
- `RegisterCapabilitiesEvent`: block Capability providers.
- Network payload registration events.
- Mod lifecycle events such as `BuildCreativeModeTabContentsEvent`.

### 5.2 NeoForge Game Event Bus

`NeoForge.EVENT_BUS.register(this)` registers runtime game events such as player interaction, server startup, and server resource reloads. Zhen recipe resource reloads and server lifecycle events belong here, not on the registry lifecycle side.

## 6. Common-Side and Client Boundaries

`MagicIO` is the common entry point, and its class loading must be safe on a dedicated server. `MagicIOClient` uses `@EventBusSubscriber(..., value = Dist.CLIENT)` and is responsible only for:

- Registering the NeoForge configuration screen;
- Registering `ZhenBusBlockEntityRenderer`;
- Handling client lifecycle logic.

Recipes are server-authoritative: after the server loads zhen recipes, it synchronizes them to clients through `ZhenRecipeSyncPayload`; clients do not preload recipes. This avoids creating invalid caches before client registries stabilize and ensures that client displays match server rules in multiplayer.

## 7. Initialization Order

The current `MagicIO` constructor reflects a clear dependency chain:

1. Register data components.
2. Attach the static event subscriber classes of `ElementType`, `ZhenType`, and `IOType` to the Mod bus to create custom registries.
3. Register element and IO type entries.
4. Register zhen types and the recipe system.
5. Generate zhen blocks from the zhen type DeferredRegister entries, then generate the corresponding BlockItems.
6. Attach blocks, items, zhen bus blocks/block entities, and ordinary zhen block entities to the event bus.
7. Register common initialization, network, creative mode tab, and Capability listeners.
8. Register game events and the COMMON configuration.

The key is separating “declaration” from “value retrieval.” `Supplier#get` or `DeferredHolder#get` may only be called when the corresponding registry is available. Therefore, `ZhenFunction` places `ElementTypes.*.get()`, `ModIOTypes.*.get()`, and slot partition construction inside registration suppliers instead of evaluating them immediately during static initialization.

## 8. Development Navigation Path

Before adding a feature, locate it in this order:

1. Determine whether it is a registered object, runtime object, resource data, or client presentation.
2. Registered objects go into the corresponding `DeferredRegister`; runtime objects are owned by block entities or processors.
3. Processing that spans items, fluids, and energy should preferably go into `io` and `item/crafting`; do not reimplement it in block interactions.
4. For standalone block forms, read `ZhenBlock` and `AbstractZhenBlockEntity`; for bus-side forms, read `SideProcessor` and `ZhenBusBlockEntity`.
5. Put client display code in `MagicIOClient`, a renderer, or a compat package, and do not let the common entry point reference client classes.

## 9. Debugging Checklist

- Registration failure: verify that the listener is attached to the Mod bus and that the custom registry is created before its DeferredRegister entries.
- Server crash: check whether common classes directly reference `net.minecraft.client` types.
- Missing resource: verify that the namespace is `magic_io` and that the resource is at the correct relative path in either main or generated resources.
- Missing client recipes: check server reload and network synchronization first; do not load the resources again on the client.
- Block entity not created: verify that the valid block set of the `BlockEntityType` contains the target block.
- Capability still points to an old object: after changing a processor, call `Level#invalidateCapabilities`.

## 10. Summary

MagicIO is a NeoForge 26.1.2 and Java 25 project. Its core structure uses custom registries to describe “types,” block entities or side processors to store “instance state,” a unified recipe processor to execute logic, and server resource reload plus network synchronization to maintain data authority. The next four chapters cover the registration system, zhen extensions, IO/Capability, and the zhen bus.

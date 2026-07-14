---
title: "Java Registry System"
navigation:
  title: "Chapter 2"
---

# Chapter 02: Java Registry System

## 1. Registry Overview

MagicIO uses both built-in Minecraft registries and three custom registries. Registries hold stable “type definitions”; instance state such as items, fluids, and progress in block entities must not be stored in registries.

| Category | Registry/Entry Point | Main Content |
|---|---|---|
| Built-in | `MagicIO.BLOCKS` | Zhen blocks and grid panels |
| Built-in | `MagicIO.ITEMS` | Regular items, zhen BlockItems, and bus BlockItems |
| Built-in | `ModBlockEntities.BLOCK_ENTITIES` | Ordinary zhen and grid panel block entity types |
| Built-in | `ModZhenBusBlocks` | Zhen bus block and block entity types |
| Built-in | `CREATIVE_MODE_TABS` | MagicIO creative mode tab |
| Custom | `ElementType.ELEMENT_TYPE_REGISTRY_KEY` | Earth, water, air, fire, and other elements |
| Custom | `IOType.IO_TYPE_REGISTRY_KEY` | item, fluid, energy |
| Custom | `ZhenType.ZHEN_TYPE_REGISTRY_KEY` | Zhen types expanded across all levels |

## 2. Two-Phase Creation of Custom Registries

All three type classes follow the same pattern:

1. Declare the registry key with `ResourceKey.createRegistryKey`.
2. Listen for `NewRegistryEvent` with a static `@SubscribeEvent` method.
3. Create the registry with `RegistryBuilder` and set a default key.
4. Call `event.register` to hand the registry over to NeoForge.
5. Use another `DeferredRegister<T>` to register entries into the newly created registry.

Therefore, `modEventBus.register(ElementType.class)`, `modEventBus.register(ZhenType.class)`, and `modEventBus.register(IOType.class)` do not register entries. They activate the static listeners that create the registries; `ElementTypes.register`, `ModIOTypes.register`, and `ZhenTypes.register` register the actual entries.

## 3. Correct Semantics of DeferredRegister

During static declaration, `DeferredRegister` only records registration intent. The actual objects are created collectively during registration events. Development code should retain `Supplier<T>`, `DeferredHolder`, `DeferredBlock`, or `DeferredItem`, rather than a raw object forcibly retrieved during static initialization.

A typical form in the current code is:

```java
public static final DeferredRegister<IOType> IO_TYPES =
        DeferredRegister.create(IOType.IO_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
```

The `register(IEventBus)` method then declares `item`, `fluid`, and `energy`, and finally calls `IO_TYPES.register(eventBus)`. This order ensures that suppliers are evaluated only after the registry has been created.

## 4. Zhen-Type-Driven Block Registration

Zhen blocks are not registered one by one. `ZhenFunctions.register` first expands each function into multiple `ZhenType` entries according to its available levels. `ModBlocks.registerZhenBlocks` then iterates over `ZhenTypes.ZHEN_TYPES.getEntries()` and creates a same-named `ZhenBlock` for every entry. Finally, `registerZhenBlockItems` generates same-named BlockItems for those blocks.

The data flow is:

```text
ZhenFunction
  → 等级展开后的 ZhenType DeferredHolder
  → 同路径 ZhenBlock DeferredBlock
  → 同路径 BlockItem DeferredItem
  → 创造模式页遍历 ZHEN_BLOCK_ITEMS
```

This explains why zhen type registration must precede zhen block declaration, and why adding a standard zhen function usually does not require adding individual Java fields for every block.

## 5. Block Entity Types

All ordinary zhen blocks share `ModBlockEntities.ZHEN_BLOCK`. Its valid block set comes from `ModBlocks.getZhenBlockList()`, and its instance factory is uniformly `ZhenBlockEntity::new`. After placement, the block entity resolves the corresponding `ZhenType` from the block's registry ID in the world, rather than creating a separate block entity class for every zhen.

The zhen bus uses a separate `ModZhenBusBlocks.ZHEN_BUS_BE` because its storage model is “one SideProcessor for each of six directions,” unlike the single-processor model of an ordinary zhen. The grid panel likewise has a separate block entity type.

## 6. Lookup and Fallback

`ZhenTypes.getType` supports both full IDs and paths without namespaces. A failed lookup first falls back to `magic_io:unstable_sieve_zhen`, then to `GRID_CELL`. `ModIOTypes.getType` falls back to the item type.

Fallbacks improve tolerance for old saves or incomplete input, but extension code must not rely on them to hide spelling mistakes. During post-registration validation, compare the actual registry ID. Persistent data should store full type IDs to avoid cross-namespace ambiguity.

## 7. Adding a Regular Item or Block

### 7.1 Regular Item

1. Declare a `DeferredItem` on the unified `MagicIO.ITEMS`.
2. If it should appear in creative mode, add its supplier to the tab's `displayItems`.
3. Add the language key, item model, and any required data components.
4. Do not split registration into a second DeferredRegister in `ModItems`; currently `ModItems` is only a reference entry point, while `MagicIO.ITEMS` is authoritative.

### 7.2 Regular Block

1. Register the block on `MagicIO.BLOCKS`.
2. Register its corresponding BlockItem on `MagicIO.ITEMS`.
3. If it has a block entity, register its type in `ModBlockEntities` and include the block in the valid set.
4. Register the block entity's capability provider in `RegisterCapabilitiesEvent`.
5. Add block states, models, loot tables, and language resources.

## 8. Adding a Custom Type Registry

If a new extensible category is needed in the future, follow the structure of the existing three registries:

1. Define an immutable type object and Registry Key.
2. Listen for `NewRegistryEvent` to create the registry and choose a reasonable default key.
3. Create a dedicated `DeferredRegister<NewType>`.
4. In the common entry point, register the type-class listener first, then declare entries and attach the DeferredRegister.
5. Store `Identifier` values during serialization, not Java class names or localized names.
6. Define behavior for unknown IDs—error, skip, or fallback—to avoid silently producing the wrong type.

## 9. Common Pitfalls

- Calling `get()` too early: the registry is not ready during static initialization.
- Duplicate registrars: scattering one object across multiple DeferredRegisters makes initialization order difficult to maintain.
- Inconsistent IDs: zhen types, blocks, and BlockItems are associated by the same path; an extra suffix anywhere breaks the mapping.
- Registering zhen types too late: adding a type after the zhen block list has already been generated from DeferredRegister entries may not create its corresponding block.
- Missing valid block entity entry: the block exists but cannot create the target BlockEntity.
- Storing state in registry objects: types are global singletons and must not hold one block's progress, inventory, or world reference.

## 10. Validation Path

1. At startup, inspect `REGISTRIES` debug logs for duplicate keys or missing registries.
2. Verify that the number of `ZhenType` entries corresponds to `ModBlocks.ZHEN_BLOCKS` and `ZHEN_BLOCK_ITEMS`.
3. Place the new block and confirm that its block entity type matches.
4. Save and reload the world, then confirm that the persistent ID resolves from the registry.
5. Use a dedicated server to verify that common registration code has no client class-loading issues.

## 11. Summary

MagicIO's registration core is “declare custom types first, expand concrete zhen across levels, then derive blocks and items from the type collection.” Understanding DeferredRegister's deferred evaluation and registration order is fundamental to extending zhen, IO types, and block entities.

---
title: "JEI and Jade Integration"
navigation:
  title: "Chapter 9"
---

# Chapter 09: JEI and Jade Integration

MagicIO declares both JEI and Jade as optional dependencies. JEI displays zhen recipes, while Jade observes the state, items, and fluids on each zhen bus side. Both must follow the rule “enable when present, do not affect core mod loading when absent.”

## 1. Dependencies and Service Discovery

`build.gradle` uses `compileOnly`:

```gradle
compileOnly("mezz.jei:jei-26.1.2-neoforge-api:29.5.0.28")
compileOnly("curse.maven:jade-324717:8281991")
```

This makes the APIs visible only during compilation; it does not package the complete mods into MagicIO or add them to the runtime environment by default. The metadata template marks `jei` and `jade` as `optional` and specifies the currently expected version ranges.

The service files are:

```text
META-INF/services/mezz.jei.api.IModPlugin
META-INF/services/snownee.jade.api.IWailaPlugin
```

They point to `MagicIOJeiPlugin` and `MagicIOJadePlugin`. Renaming a plugin class or package requires updating its service file, or the code will compile but the corresponding mod will not discover it.

## 2. JEI Plugin Lifecycle

`MagicIOJeiPlugin` uses `@JeiPlugin` and implements `IModPlugin`. Its main phases are:

1. `registerCategories`: create a `ZhenRecipeCategory` for every zhen function.
2. `registerRecipes`: preferentially register current recipes from the client recipe cache.
3. `registerRecipeCatalysts`: register each level's zhen block as a workstation.
4. `onRuntimeAvailable`: retain `IJeiRuntime` and handle uncertain ordering between network and JEI initialization.

The plugin creates an `IRecipeType<ZhenRecipe>` for each function base name. Different levels of the same function share one JEI category, matching the grouping rule of the server `ZhenRecipeManager`.

The furnace zhen `forge` is special: it does not create a separate zhen category, but registers its blocks as workstations for the vanilla smelting category. Other functions map to MagicIO's own recipe types.

## 3. Categories and Layout

`ZhenRecipeCategory` is responsible for:

- Finding the corresponding zhen block to use as the function icon.
- Constructing the category title from a translation key.
- Placing item Ingredients in input slots.
- Placing fluids and capacities in fluid slots.
- Displaying fixed item outputs.
- Parsing loot tables and placing random candidates in a cycling grid.

Fluid slots call `setFluidRenderer`, with capacity taken from the fluid amount in the recipe. When changing layouts, inspect slot coordinates, category width and height, cycling regions, and tooltip bounds together to avoid mismatches between clickable and rendered positions.

## 4. Dynamic JEI Refresh

After server synchronization, `refreshFromCache`:

1. Retrieves the JEI RecipeManager.
2. Queries existing recipes for each MagicIO type.
3. Hides old recipes.
4. Adds recipes from the latest cache.

`registerFrom` currently deduplicates by base name and retains only the first matching recipe in each category, rather than adding all recipes in that category to JEI. It also skips `forge` and filters out recipes without fixed-item, loot, or fluid outputs. Therefore, “the server has multiple recipes in a category but JEI displays only one” is current source behavior and not necessarily a synchronization failure.

Another boundary is that refresh “hides old entries, then adds new ones” rather than removing them. With frequent reloads, verify whether JEI accumulates hidden objects internally and whether focus queries remain correct.

## 5. Loot Display Limitations

`LootTableParser` reads JSON from the client classpath and supports only common item/tag entries and weighted expected values. It is not equivalent to the server LootTable evaluator. Server data-pack overrides, complex conditions, functions, and nested tables may display inaccurately.

Tutorials and user interfaces should describe these numbers as indicative expected values rather than guaranteed drop rates. Actual results are always determined by server-side `OutputEntry.roll`.

## 6. Jade Common-Side Registration

`MagicIOJadePlugin.register` registers three server-side providers:

- `ZhenBusServerProvider`: sends each side's zhen type, processing time, and item summary.
- `ZhenBusItemProvider`: provides an item storage view to Jade.
- `ZhenBusFluidProvider`: provides a fluid storage view to Jade.

`ZhenBusServerProvider.shouldRequestData` requests data only when the target block entity is a `ZhenBusBlockEntity`. `appendServerData` iterates over the six directions and encodes every existing `SideProcessor` into a `CompoundTag`.

The server should send only data that the interface actually needs. A zhen bus may connect large amounts of resources; sending complete storage every frame would create substantial network traffic and NBT allocation pressure.

## 7. Jade Client Registration

`registerClient` registers:

- `ZhenBusClientProvider`: renders primary block information.
- `ZhenBusItemClientProvider`: renders item storage.
- `ZhenBusFluidClientProvider`: renders fluid storage.

This is a typical “server gathers authoritative data, client formats and renders it” architecture. Client providers cannot directly read the complete block entity state on a remote server and should not infer machine recipe progress independently.

## 8. Optional Dependency Isolation

Compatibility code should remain concentrated in `compat.jei` and `compat.jade`. Core logic must not directly reference optional-mod types in static fields, public signatures, or class initialization; otherwise the JVM may fail to resolve classes when the dependency is missing, even if the plugin itself is never discovered.

Recommended scenarios to test:

| Environment | Expected Result |
| --- | --- |
| Neither JEI nor Jade installed | MagicIO core functions normally; compatibility plugins do not load |
| Only JEI installed | Recipe categories and synchronized refresh work; Jade classes do not participate |
| Only Jade installed | Zhen bus observation works; JEI classes do not participate |
| Both installed | Both plugins work independently |
| Dedicated server | Pure client rendering classes are not loaded; common-side providers work according to Jade's mechanism |

## 9. Local Validation Steps

1. Confirm that API versions in the build file match the runtime mod versions.
2. Confirm that both `META-INF/services` files contain the plugins' fully qualified class names.
3. After startup, confirm the logged number of JEI categories.
4. After entering a world, confirm that the client receives the server recipe cache.
5. Run `/reload` and confirm that JEI content refreshes with the cache.
6. Inspect all six zhen bus sides and confirm that Jade display matches the actual SideProcessor.
7. Test core startup separately with JEI absent, Jade absent, and both absent.
8. Use a dedicated server to confirm there are no client class-loading exceptions.

## 10. Common Issues

- **Plugin never appears**: check the service file, optional mod version, and plugin annotation first.
- **JEI has a category but no recipes**: check whether the client cache was received and whether `registerFrom` filtered the recipe.
- **Only one recipe appears in a category**: current code deduplicates by base name with `putIfAbsent`.
- **Random products are incomplete**: check whether the loot table exceeds the lightweight parser's capabilities.
- **Jade data is empty**: check whether the target is a `ZhenBusBlockEntity` and whether a processor exists in that direction.
- **Crash only on server**: check whether core classes directly reference JEI/Jade client APIs.

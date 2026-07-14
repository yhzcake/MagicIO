---
title: "API Design and Mod Compatibility"
navigation:
  title: "Chapter 10"
---

# Chapter 10: API Design and Mod Compatibility

MagicIO currently has no separately published `api` source set or stable API artifact, but the source already provides several extension points: custom registries, zhen function descriptions, IO abstractions, recipe modifiers, grid actions, and parsing rules. This chapter distinguishes “currently usable extension points” from “internal implementations not yet promised to be stable.”

## 1. Compatibility Levels

The recommended extension priority is:

1. **Data packs**: recipes, loot tables, and tags, with no hard Java dependency.
2. **Registry extensions**: register objects such as `ZhenType` and `IOType` through the NeoForge mod event bus.
3. **Behavior extensions**: register `RecipeModifier`, `GridParseRule`, or custom processing logic.
4. **Optional mod adapters**: call third-party APIs from independent compat packages.
5. **Mixin or internal reflection**: use only when no public extension point exists, accepting the highest maintenance cost.

Do not prioritize a Java compatibility layer for a problem that data can solve. Data IDs are more stable than internal class names and easier for modpacks to override.

## 2. Custom Registries

The project creates several custom registries through `NewRegistryEvent`:

- `magic_io:element_type`: element types.
- `magic_io:zhen_type`: concrete zhen types.
- `magic_io:io_type`: resource IO types.
- `magic_io:cell_action`: grid cell actions.
- `magic_io:grid_parse_rule`: grid parsing rules.

Built-in objects use `DeferredRegister`. Third-party extensions should likewise bind their DeferredRegister to their own mod event bus during mod construction, rather than directly modifying a registry or writing after registry freeze.

Registered objects should use the extension mod's namespace. For example, a third-party zhen type should use `other_mod:...`, not occupy the `magic_io` namespace. Cross-mod references should use `Identifier` or registry Holders rather than concrete static fields.

## 3. Zhen Functions and Zhen Types

`ZhenFunction` is an internal convenience description for generating leveled zhen types in bulk. It records the base name, element, first available level, slot partition, face access, tick factory, and fluid capacity. Calling `register` iterates over `ZhenLevel.ALL` and generates the available levels as:

```text
<level_prefix><base_name>_zhen
```

`ZhenType` is the final object entered into the registry. Designing a third-party zhen requires determining all of the following:

- A stable base name and full registry ID.
- Its element type.
- Its first available level.
- Slot zones for each `IOType`.
- Zones exposed on each of the six directions.
- Whether it has per-tick behavior.
- Fluid or energy capacity.
- Whether it uses the data-recipe flow of `RecipeProcessor`.

The base name participates in recipe cache grouping and JEI category grouping. Arbitrary renaming breaks recipe lookup, existing data packs, and display integration together.

## 4. IO Extensions

Built-in `IOType` values currently include items, fluids, and energy. `IOProcessor` stores `IOComponent<R,T>` objects by type, while recipe inputs and outputs use `RecipeInput<?>` and `RecipeOutput<?>` to carry their types and zones.

Adding a resource type requires more than registering one `IOType`; it also requires a complete implementation of:

1. Data models for resource requirements and output values.
2. Query, simulation, consumption, production, and persistence in the corresponding `IOComponent`.
3. Zone layout for the type in `SlotPartition`.
4. Directional access mapping.
5. Recipe JSON parsing.
6. MapCodec and StreamCodec.
7. Matching, preflight, consumption, and production in `RecipeProcessor`.
8. JEI/Jade or other UI display.

Registering only the `IOType` without completing the pipeline produces a shell type that registers successfully but cannot participate in recipes.

The current NeoForge 26.1 source uses the Transfer API's `ResourceHandler`, resource objects, and transaction contexts. Do not directly apply the old Forge Capability architecture based on `IItemHandler`/`LazyOptional` unless writing a dedicated bridge layer.

## 5. Recipe Modifiers

`RecipeModifiers.register(RecipeModifier)` is a lightweight behavioral extension point. All modifiers are traversed in registration order, with time and output multipliers accumulated multiplicatively.

Compatibility implementations should:

- Check the target zhen type or recipe before modifying it rather than affecting every recipe unconditionally.
- Return predictable values with reasonable bounds.
- Avoid expensive registry scans during every-tick calculations.
- Avoid depending on a fixed order when multiple mods register modifiers unless the API explicitly provides priority.
- Let the server determine final multipliers; client display can only be advisory.

`RecipeModifiers` is currently an ordinary static list, not a NeoForge registry, and has no duplicate-registration protection or stable ordering protocol. It is useful as a source-level extension point but should not yet be advertised as a cross-version stable API.

## 6. Grid Actions and Parsing Rules

`CellAction` describes actions recognized by grid cells, while `GridParseRule` combines matching conditions, target zhen types, and processors. Rules are traversed in registration order, and the first match executes.

Third-party rules must account for conflicts:

- Broader match conditions are more likely to intercept other rules first.
- Cross-mod registration order must not be treated as stable business priority.
- Rule processors should validate input context rather than assuming the grid is complete or the object belongs to this mod.
- If two mods declare the same pattern, resolve it through configuration, an explicit priority API, or a more specific pattern—not incidental load order.

## 7. Current Data Compatibility Limitations

Although data-driven extension is preferred, the current zhen recipe scanner restricts resource namespaces to `magic_io`. This means a third-party mod cannot add zhen recipes solely under its own `data/other_mod/recipe` directory and have the existing loader discover them.

The available paths currently involve explicit tradeoffs:

- A modpack places compatibility recipes in the `magic_io` namespace: simple to implement, but risks resource ID overrides and unclear ownership.
- Extend the loader to accept multiple namespaces: architecturally cleaner, but requires core code evolution.
- Provide a formal recipe registration event or API: appropriate for Java extensions, but must handle reload lifecycle and network synchronization.

Until a formal API exists, do not promise native support for zhen recipes in third-party namespaces.

## 8. Optional Dependency Pattern

JEI/Jade demonstrate the recommended pattern:

- Use `compileOnly` APIs at build time.
- Use `type="optional"` in mod metadata.
- Place adapter code in separate compat packages.
- Load it through the service discovery or plugin entry point specified by the other mod.
- Keep optional dependency types out of core class signatures.

For compatibility with other mods, also check that the mod is loaded before entering compatibility initialization. Do not hide structural class-loading issues by catching `ClassNotFoundException`.

## 9. API Stability Recommendations

If MagicIO is intended to become a development dependency for other mods, establish explicit boundaries:

1. Create a separate API package exposing only interfaces, registry keys, events, and immutable data objects.
2. Record breaking changes with semantic versioning.
3. Avoid returning internal mutable collections or singleton caches from the API.
4. Provide explicit events for registration, server reload, and client synchronization stages.
5. Provide a unified Codec for recipe extensions, eliminating capability differences between manual Gson and MapCodec.
6. Establish a network protocol version policy or use NeoForge version negotiation to reject incompatible connections.
7. Build JEI/Jade adapters that depend only on public view models.

## 10. Compatibility Test Matrix

Every extension should validate at least:

| Scenario | Key Checks |
| --- | --- |
| MagicIO only | Core registration, recipe execution, and reload work |
| MagicIO + extension mod | Registry IDs, recipes, multipliers, and rules work |
| Optional dependency absent | Core mod has no class-loading errors |
| Single-player | Integrated server synchronizes to the local client |
| Dedicated server | Client APIs are not loaded; server executes authoritatively |
| `/reload` | Cache replacement, loot table updates, and client refresh work |
| Player joins midway | Receives the complete recipe snapshot |
| Multiple extensions coexist | Registries, rule order, and multiplier stacking do not conflict |

## 11. Troubleshooting

- **Registered object is absent**: check whether DeferredRegister is bound to the correct event bus and whether registry keys match.
- **Recipe cannot find a third-party zhen**: check `zhen_type` registration timing, base-name rules, and the scanner's namespace restriction.
- **Custom IO does not execute**: verify parsing, networking, partitioning, components, and processor flow one by one.
- **Failure only after reload**: check whether static extension lists are cleared and re-registered, and inspect cache replacement timing.
- **Client display differs from server**: treat the server as authoritative and check complete snapshot synchronization plus display-parser limitations.
- **Crash when an optional mod is absent**: inspect static references in core classes, public method signatures, and service-entry isolation.

## 12. Development Principles Summary

MagicIO compatibility development should center on stable resource IDs, server authority, safe registration stages, and optional dependency isolation. Existing custom registries provide a strong extension foundation, but static recipe caches, the manual JSON loader, and some internal convenience classes do not yet form a formal stable API. Third-party extensions should bind themselves to a specific MagicIO version and rerun the complete test matrix after upgrading NeoForge, Minecraft, or MagicIO.

# Chapter 7: Compatibility Presentation, Debugging, and Known Limitations

## 1. JEI Compatibility

The project currently provides a JEI mod plugin service entry point and a Zhen recipe category. JEI displays Zhen recipes and adds a "± Random" note to candidate loot-table products.

The build script declares only the JEI API as `compileOnly`, while the complete JEI runtime dependency is commented out. Therefore:

- The released mod does not require JEI.
- If JEI is not installed locally, recipe pages should not be expected to appear.
- This tutorial does not recommend modifying build dependencies automatically. When JEI debugging is needed, the developer should explicitly decide how it will be installed at runtime.
- Loot-table parsing is a presentation aid and does not guarantee a complete reproduction of all complex vanilla loot conditions and functions.

## 2. Jade Compatibility

Jade is likewise an optional compile-time dependency. When installed, it can display the following for the Zhen Bus:

- The Zhen name on the currently targeted surface.
- Item information for that surface's processor.
- Fluid information.
- Current energy and capacity.

Jade's surface identification reuses the Zhen Bus ray detection. If aiming near an edge displays the wrong processor, retest from an angle closer to the center of the surface before deciding that the data is incorrect.

## 3. Client Tag Export Command

After entering a world, execute:

```text
/magicio tags
```

This client command iterates over nonempty tags in the current item registry, sorts them by namespace and tag path, and writes them under the game directory to:

```text
magicio/tags-<timestamp>.json
```

It exports only item tags, not fluid tags, block tags, or Zhen recipes. The command must be executed after entering a world, when the client registry is available.

## 4. Portal Function

The Pristine Portal Zhen is one of the few functions that currently has dedicated tick behavior. Every tick, it checks server-side players within the Zhen block's bounds. A player must hold the example item in the main hand, its custom data must contain a valid `position` string, and the player must be sneaking.

The accepted formats are:

```text
x,y,z
x,y,z yaw pitch
```

yaw must be strictly between -180 and 180, and pitch must be strictly between -90 and 90. The current implementation teleports within the same `ServerLevel` and has no dimension ID parameter.

**Limitation:** No normal player interaction currently writes `position` to the example item. The existing right-click-air logic rebuilds the custom data and saves only `side`, which may overwrite other fields. Therefore, the Portal should be treated as a development-level interface rather than a complete Survival feature.

## 5. Configuration and Example Artifacts

The current common configuration only records a dirt block, a magic number, introductory text for the magic number, and a list of items to record. All retain the nature of template examples. They do not control Zhen speed, output, energy consumption, or bus capacity. Do not claim to modpack authors that these configuration options can balance the core system.

The Chinese language file still contains names for the example block and example item, and the Creative tab icon is also the example item. The project contains `ExampleMixin` and other development naming, which must not be used to infer formal gameplay.

## 6. Systematic Troubleshooting Path

1. **Confirm the type**: Record the full registry ID of the actual Zhen block or the target surface of the bus.
2. **Confirm the data**: Check the recipe's `zhen_type`, region names, input IDs, output IDs, and processing duration.
3. **Confirm storage**: Ensure that input enters the correct slot and that the output slot or fluid tank has space.
4. **Confirm the tier**: Higher tiers are backward-compatible; a lower tier with the same function cannot process a higher-tier recipe.
5. **Confirm loading**: Keep the chunk loaded and observe server warnings after reloading data.
6. **Confirm presentation boundaries**: Missing JEI/Jade affects only auxiliary information and is not equivalent to missing core recipes.
7. **Confirm random output**: Check both the normal output slot and item entities near the Zhen.

## 7. Summary of Current Known Limitations

| Module | Limitation |
| --- | --- |
| Survival progression | No complete crafting chain for obtaining every Zhen and the Zhen Bus |
| Zhen functions | Most are distinguished only by JSON and have no dedicated mechanics |
| Energy | An IO framework exists, but current core recipes do not form an explicit closed loop of FE consumption |
| Fluid input | The consumption path requires additional runtime verification |
| Grid | The only built-in parsing rule merely records a log entry |
| Portal | Depends on example item data that cannot be configured reliably through the normal workflow |
| Creative Zhen | Generates command blocks without input and is placeholder content |
| Configuration | Remains a template example and does not control core balance |
| Optional compatibility | JEI/Jade are not forcibly provided as core dependencies |

## 8. Conclusion

The current source code already forms an extensible skeleton for Zhen type registration, tier inheritance, item/fluid IO, data-driven recipes, multi-surface Zhen Bus processing, and client recipe synchronization. It is best suited to Creative Mode mechanics verification and datapack prototype development. Before release as a formal Survival mod, it still needs an acquisition chain, formal interactions, grid business results, energy semantics, fluid-input verification, configuration options, and overall balance.

# Chapter 08: Resource Reload and Network Synchronization

MagicIO's zhen recipes use the server as their authoritative source. Clients do not scan local recipes themselves; they wait for the server to send an already parsed list of `ZhenRecipe` objects. This chapter follows the source code to explain the complete startup, login, `/reload`, and JEI refresh sequence.

## 1. Server-Authoritative Model

Server authority has three direct consequences:

- Even single-player recipes are loaded by the integrated server.
- A multiplayer server's data packs determine the actual recipe content.
- The client cache exists only for display and client-side queries; it cannot determine machine execution results.

`MagicIOClient` explicitly does not preload recipes, avoiding invalid caches before client registries are ready. Recipe loading is centralized in server events and reload listeners in `MagicIO`.

## 2. Startup Sequence

When the server starts:

1. `ServerStartingEvent` triggers `onServerStarting`.
2. `loadRecipesToManager` clears `ZhenRecipeManager`.
3. `ResourceManager.listResources("recipe", ...)` enumerates JSON in the `magic_io` namespace.
4. Each resource is parsed by `ZhenRecipeLoader`.
5. `ForgeRecipeBridge.injectFurnaceRecipes(server)` injects furnace bridge recipes.
6. The final cache size is logged.
7. `syncRecipesToAll` broadcasts a snapshot to all currently online players.

When a player logs in, `PlayerLoggedInEvent` submits the synchronization task to the server's main thread. The current implementation calls the broadcast method, so one player joining resends the complete list to every online player, not just the new player.

## 3. `/reload` Sequence

`AddServerReloadListenersEvent` registers `ZhenRecipeReloadListener` under the ID `magic_io:zhen_recipes`. The listener extends `SimplePreparableReloadListener<Void>`:

- `prepare` currently performs no parsing and returns `null`.
- `apply` obtains the current server during the application phase, reloads the cache, and broadcasts it.
- A physical client environment skips server application logic directly.

Therefore, JSON opening, Gson parsing, and registry validation all currently occur in the apply phase. This is simple while recipe counts remain small. If they increase significantly, immutable intermediate data could be read during prepare, leaving only registry-dependent parsing and cache replacement for apply.

## 4. Network Registration

`RegisterPayloadHandlersEvent` registers the payload through the mod registrar:

```java
registrar.playToClient(
    ZhenRecipeSyncPayload.TYPE,
    ZhenRecipeSyncPayload.STREAM_CODEC,
    ZhenRecipeSyncPayload::handle
);
```

The payload type ID is `magic_io:zhen_recipe_sync`, and its direction is restricted to server-to-client. `ZhenRecipeSyncPayload.STREAM_CODEC` writes the recipe count first, then delegates each recipe to `ZhenRecipeSerializer.STREAM_CODEC`.

Using `RegistryFriendlyByteBuf` is important because Ingredient, HolderSet, FluidStack, and related data depend on registries. It must not be replaced with an ordinary byte buffer without verifying the semantics.

## 5. Client Handling

After receiving the payload, the client uses `context.enqueueWork` to:

1. Clear the local `ZhenRecipeManager`.
2. Add every received recipe in order.
3. Log the received count.
4. Call `MagicIOJeiPlugin.refreshFromCache()`.

Enqueuing work avoids modifying shared client state directly on the network thread. Replacing the complete cache before refreshing JEI ensures that JEI observes one consistent snapshot.

## 6. Codec Consistency

Network data order is defined by `ZhenRecipeSerializer.STREAM_CODEC` and mainly includes:

1. Zhen type `Identifier`.
2. Item input zone count, zone names, and Ingredient lists.
3. Item output zone count and fixed-item/loot-table discriminator flags.
4. Fluid input zones and `HolderSet + amount`.
5. Fluid output zones and `FluidStack`.
6. `processing_time`.

When adding a field, update both encoding and decoding and decide whether old clients may connect to new servers. Changing only the JSON Codec does not automatically change the network Codec.

## 7. Empty Snapshot Issue

The current `syncRecipesToAll` returns immediately when the cache is empty. This means that if a server reload reduces the number of valid recipes to 0, clients do not receive an empty list instructing them to clear their caches and may continue displaying old recipes.

Treat this as a known boundary during development and testing:

- “The server loaded 0 recipes” does not mean “the client cache was cleared.”
- When testing deletion of all recipes, explicitly check for stale client cache entries.
- A future fix should allow sending an authoritative snapshot of length 0 and make JEI hide old entries.

## 8. JEI Initialization Race

The network payload and JEI Runtime can become ready in either order, so the source handles both directions:

- Payload first: the cache updates first; refresh returns while JEI Runtime is absent, then `onRuntimeAvailable` refreshes from the cache later.
- JEI first: initial registration finds an empty cache and waits; payload arrival then triggers a refresh.

This “two triggers, one cache source” pattern suits optional client integrations. REI, EMI, or other viewer integrations should likewise avoid assuming a fixed initialization order.

## 9. Performance and Safety

- Synchronization sends the complete recipe list, not incremental differences; monitor packet size with many recipes.
- `Ingredient`, fluid HolderSets, and output lists can all grow large, so collection sizes from untrusted data should be bounded.
- The network receiver must not execute recipes; it updates only the client display cache.
- Server reload should build a new result before deciding whether to replace the old cache; the current implementation clears first, so an exception midway may leave a partial list.
- Broadcasting to all players on login is correct but creates duplicate traffic at scale; it can be changed to send only to the joining player.

## 10. Debugging Checklist

1. Check startup logs for the total number of loaded recipes.
2. Confirm that the network payload is registered.
3. After player login, inspect the server synchronization count log.
4. On the client, check `[Network] Received ... recipes from server`.
5. Run `/reload` and confirm that reload logs precede synchronization logs.
6. If the client disconnects, focus on `STREAM_CODEC` encoding/decoding order and registry objects.
7. If the client cache is correct but JEI is unchanged, check whether JEI Runtime is available and whether refresh logic ran.
8. If failure occurs only on a dedicated server, check whether server code accidentally loads JEI/Jade client classes.

## 11. Version Notes

This project targets NeoForge 26.1 and uses `CustomPacketPayload`, `RegisterPayloadHandlersEvent`, and `playToClient`. SimpleChannel, numeric message IDs, and custom `encode/decode/handle` registration templates from old Forge tutorials cannot be copied directly. Always use the current dependency source and the registration pattern already established in this project.

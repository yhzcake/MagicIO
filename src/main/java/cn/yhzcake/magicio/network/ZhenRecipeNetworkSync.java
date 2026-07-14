package cn.yhzcake.magicio.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ZhenRecipeNetworkSync {
    public static final int PROTOCOL_VERSION = 1;
    private static volatile CatalogSnapshot previousServerCatalog = CatalogSnapshot.empty();
    private static volatile CatalogSnapshot currentServerCatalog = CatalogSnapshot.empty();
    private static final Map<Identifier, ZhenRecipeCatalogPayload.Entry> clientCatalog = new LinkedHashMap<>();
    private static final ArrayDeque<Identifier> pendingDetails = new ArrayDeque<>();
    private static Map<Identifier, ZhenRecipe> stagingRecipes = new LinkedHashMap<>();
    private static long targetRevision;
    private static boolean detailRequestInFlight;
    private static final Map<UUID, RequestWindow> catalogRequestWindows = new ConcurrentHashMap<>();
    private static final Map<UUID, DetailSession> detailSessions = new ConcurrentHashMap<>();

    private ZhenRecipeNetworkSync() {
    }

    public static synchronized void publishServerSnapshot() {
        previousServerCatalog = currentServerCatalog;
        Map<Identifier, ZhenRecipeCatalogPayload.Entry> entries = new LinkedHashMap<>();
        for (ZhenRecipe recipe : ZhenRecipeManager.getInstance().getAllRecipes()) {
            if (isForge(recipe)) continue;
            entries.put(recipe.getRecipeId(), new ZhenRecipeCatalogPayload.Entry(recipe.getRecipeId(), recipe.getProcessingTime()));
        }
        currentServerCatalog = new CatalogSnapshot(ZhenRecipeManager.getInstance().getRevision(), Map.copyOf(entries));
    }

    public static void handleCatalogRequest(ZhenRecipeCatalogRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            if (!allowRequest(catalogRequestWindows, player.getUUID(), 8)) return;
            CatalogSnapshot current = currentServerCatalog;
            detailSessions.put(player.getUUID(), new DetailSession(current.revision()));
            ZhenRecipeCatalogPayload response;
            if (payload.protocolVersion() != PROTOCOL_VERSION) {
                response = fullCatalog(current);
            } else if (payload.knownRevision() == current.revision()) {
                response = new ZhenRecipeCatalogPayload(PROTOCOL_VERSION, current.revision(), current.revision(),
                        ZhenRecipeCatalogPayload.NOT_MODIFIED, List.of(), List.of());
            } else if (payload.knownRevision() == previousServerCatalog.revision()) {
                response = deltaCatalog(previousServerCatalog, current);
            } else {
                response = fullCatalog(current);
            }
            PacketDistributor.sendToPlayer(player, response);
        });
    }

    public static void handleCatalog(ZhenRecipeCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyCatalog(payload, context));
    }

    public static void handleDetailRequest(ZhenRecipeDetailRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.enqueueWork(() -> {
            ZhenRecipe recipe = null;
            if (payload.protocolVersion() == PROTOCOL_VERSION && payload.revision() == currentServerCatalog.revision()
                    && currentServerCatalog.entries().containsKey(payload.recipeId())
                    && allowDetail(player.getUUID(), payload.revision(), payload.recipeId())) {
                recipe = ZhenRecipeManager.getInstance().getRecipe(payload.recipeId());
            }
            PacketDistributor.sendToPlayer(player, new ZhenRecipeDetailPayload(PROTOCOL_VERSION,
                    currentServerCatalog.revision(), payload.recipeId(), recipe));
        });
    }

    public static void handleDetail(ZhenRecipeDetailPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyDetail(payload, context));
    }

    public static synchronized void resetClient() {
        clientCatalog.clear();
        pendingDetails.clear();
        stagingRecipes = new LinkedHashMap<>();
        targetRevision = 0;
        detailRequestInFlight = false;
    }

    public static void clearServerRequestState(UUID playerId) {
        catalogRequestWindows.remove(playerId);
        detailSessions.remove(playerId);
    }

    private static synchronized void applyCatalog(ZhenRecipeCatalogPayload payload, IPayloadContext context) {
        if (payload.protocolVersion() != PROTOCOL_VERSION) {
            resetClient();
            return;
        }
        if (payload.mode() == ZhenRecipeCatalogPayload.NOT_MODIFIED) return;
        if (payload.mode() != ZhenRecipeCatalogPayload.FULL && payload.mode() != ZhenRecipeCatalogPayload.DELTA) {
            requestFullCatalog(context);
            return;
        }
        long localRevision = ZhenRecipeManager.getClientInstance().getRevision();
        if (payload.mode() == ZhenRecipeCatalogPayload.DELTA && payload.baseRevision() != localRevision) {
            requestFullCatalog(context);
            return;
        }
        Map<Identifier, ZhenRecipeCatalogPayload.Entry> nextCatalog = payload.mode() == ZhenRecipeCatalogPayload.FULL
                ? new LinkedHashMap<>() : new LinkedHashMap<>(clientCatalog);
        for (Identifier removed : payload.removed()) nextCatalog.remove(removed);
        for (ZhenRecipeCatalogPayload.Entry entry : payload.entries()) nextCatalog.put(entry.recipeId(), entry);
        clientCatalog.clear();
        clientCatalog.putAll(nextCatalog);
        targetRevision = payload.revision();
        stagingRecipes = new LinkedHashMap<>();
        Map<Identifier, ZhenRecipe> cached = new LinkedHashMap<>();
        for (ZhenRecipe recipe : ZhenRecipeManager.getClientInstance().getAllRecipes()) cached.put(recipe.getRecipeId(), recipe);
        Set<Identifier> changed = new LinkedHashSet<>();
        for (ZhenRecipeCatalogPayload.Entry entry : payload.entries()) changed.add(entry.recipeId());
        pendingDetails.clear();
        for (var entry : clientCatalog.values()) {
            ZhenRecipe recipe = cached.get(entry.recipeId());
            if (!changed.contains(entry.recipeId()) && recipe != null && recipe.getProcessingTime() == entry.processTime()) {
                stagingRecipes.put(entry.recipeId(), recipe);
            }
            else pendingDetails.add(entry.recipeId());
        }
        detailRequestInFlight = false;
        requestNextDetailOrCommit(context);
    }

    private static synchronized void applyDetail(ZhenRecipeDetailPayload payload, IPayloadContext context) {
        detailRequestInFlight = false;
        if (payload.protocolVersion() != PROTOCOL_VERSION || payload.revision() != targetRevision || payload.recipe() == null
                || !payload.recipeId().equals(payload.recipe().getRecipeId())) {
            requestFullCatalog(context);
            return;
        }
        stagingRecipes.put(payload.recipeId(), payload.recipe());
        requestNextDetailOrCommit(context);
    }

    private static void requestNextDetailOrCommit(IPayloadContext context) {
        if (detailRequestInFlight) return;
        Identifier next = pendingDetails.poll();
        if (next != null) {
            detailRequestInFlight = true;
            context.reply(new ZhenRecipeDetailRequestPayload(PROTOCOL_VERSION, targetRevision, next));
            return;
        }
        List<ZhenRecipe> recipes = new ArrayList<>();
        for (Identifier id : clientCatalog.keySet()) {
            ZhenRecipe recipe = stagingRecipes.get(id);
            if (recipe == null) return;
            recipes.add(recipe);
        }
        ZhenRecipeManager.getClientInstance().replaceClientSnapshot(targetRevision, recipes);
        refreshOptionalJei();
    }

    private static void requestFullCatalog(IPayloadContext context) {
        context.reply(new ZhenRecipeCatalogRequestPayload(PROTOCOL_VERSION, -1));
    }

    private static boolean allowRequest(Map<UUID, RequestWindow> windows, UUID playerId, int limit) {
        long now = System.nanoTime();
        RequestWindow window = windows.computeIfAbsent(playerId, ignored -> new RequestWindow(now));
        synchronized (window) {
            if (now - window.startedAt >= 5_000_000_000L) {
                window.startedAt = now;
                window.count = 0;
            }
            if (window.count >= limit) return false;
            window.count++;
            return true;
        }
    }

    private static boolean allowDetail(UUID playerId, long revision, Identifier recipeId) {
        DetailSession session = detailSessions.get(playerId);
        if (session == null || session.revision != revision) return false;
        synchronized (session) {
            return session.served.add(recipeId);
        }
    }

    private static ZhenRecipeCatalogPayload fullCatalog(CatalogSnapshot current) {
        return new ZhenRecipeCatalogPayload(PROTOCOL_VERSION, 0, current.revision(), ZhenRecipeCatalogPayload.FULL,
                List.copyOf(current.entries().values()), List.of());
    }

    private static ZhenRecipeCatalogPayload deltaCatalog(CatalogSnapshot previous, CatalogSnapshot current) {
        List<ZhenRecipeCatalogPayload.Entry> changed = new ArrayList<>(current.entries().values());
        Set<Identifier> removed = new LinkedHashSet<>(previous.entries().keySet());
        removed.removeAll(current.entries().keySet());
        return new ZhenRecipeCatalogPayload(PROTOCOL_VERSION, previous.revision(), current.revision(),
                ZhenRecipeCatalogPayload.DELTA, List.copyOf(changed), List.copyOf(removed));
    }

    private static boolean isForge(ZhenRecipe recipe) {
        String name = ZhenLevel.baseName(recipe.getZhenTypeId().getPath());
        if (name.endsWith("_zhen")) name = name.substring(0, name.length() - 5);
        return "forge".equals(name);
    }

    private static void refreshOptionalJei() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return;
        try {
            Class<?> pluginClass = Class.forName("cn.yhzcake.magicio.compat.jei.MagicIOJeiPlugin");
            pluginClass.getMethod("refreshFromCache").invoke(null);
        } catch (ReflectiveOperationException e) {
            MagicIO.LOGGER.error("Failed to refresh optional JEI integration", e);
        }
    }

    private record CatalogSnapshot(long revision, Map<Identifier, ZhenRecipeCatalogPayload.Entry> entries) {
        private static CatalogSnapshot empty() {
            return new CatalogSnapshot(0, Map.of());
        }
    }

    private static final class RequestWindow {
        private long startedAt;
        private int count;

        private RequestWindow(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private static final class DetailSession {
        private final long revision;
        private final Set<Identifier> served = new LinkedHashSet<>();

        private DetailSession(long revision) {
            this.revision = revision;
        }
    }
}

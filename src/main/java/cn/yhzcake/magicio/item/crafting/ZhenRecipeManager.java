package cn.yhzcake.magicio.item.crafting;

import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.io.IOProcessor;

/**
 * 配方管理器。
 * <p>配方以功能基础名（如 "cinder""forge"）为 key 存储，与具体等级解耦。
 * 一个配方可被同一功能的所有等级复用，处理时根据实际等级动态计算倍率。</p>
 */
public class ZhenRecipeManager {
    private static final ZhenRecipeManager SERVER_INSTANCE = new ZhenRecipeManager();
    private static final ZhenRecipeManager CLIENT_INSTANCE = new ZhenRecipeManager();
    private volatile Snapshot snapshot = Snapshot.empty();
    private Map<String, List<ZhenRecipe>> reloadRecipes;
    private boolean loaded;

    private ZhenRecipeManager() {
    }

    public static ZhenRecipeManager getInstance() {
        return SERVER_INSTANCE;
    }

    public static ZhenRecipeManager getClientInstance() {
        return CLIENT_INSTANCE;
    }

    /** 提取配方对应的基础名（去掉等级前缀和 _zhen 后缀） */
    private static String baseName(ZhenRecipe recipe) {
        String path = recipe.getZhenTypeId().getPath();
        return stripSuffix(ZhenLevel.baseName(path));
    }

    /** 从完整的 zhenType 字符串中提取基础名 */
    private static String baseName(String zhenType) {
        String path;
        if (zhenType.contains(":")) {
            path = zhenType.substring(zhenType.indexOf(':') + 1);
        } else {
            path = zhenType;
        }
        return stripSuffix(ZhenLevel.baseName(path));
    }

    /** 去掉尾部的 _zhen 后缀 */
    private static String stripSuffix(String name) {
        if (name.endsWith("_zhen")) {
            return name.substring(0, name.length() - "_zhen".length());
        }
        return name;
    }

    /** 添加配方，自动按基础名归类 */
    public synchronized void addRecipe(ZhenRecipe recipe) {
        String key = baseName(recipe);
        Map<String, List<ZhenRecipe>> target = reloadRecipes;
        if (target == null) {
            target = mutableCopy(snapshot.recipesByBaseName());
            target.computeIfAbsent(key, ignored -> new ArrayList<>()).add(recipe);
            snapshot = Snapshot.create(snapshot.revision(), target);
            return;
        }
        target.computeIfAbsent(key, ignored -> new ArrayList<>()).add(recipe);
    }

    public int getRecipeCount() {
        return snapshot.allRecipes().size();
    }

    /** 按基础名查找配方 */
    public List<ZhenRecipe> getRecipes(String zhenType) {
        return snapshot.recipesByBaseName().getOrDefault(baseName(zhenType), List.of());
    }

    /** 获取所有已注册的配方 */
    public List<ZhenRecipe> getAllRecipes() {
        return snapshot.allRecipes();
    }

    public ZhenRecipe getRecipe(net.minecraft.resources.Identifier recipeId) {
        return snapshot.recipesById().get(recipeId);
    }

    /** 查找匹配的配方（在遍历候选时同步检查等级，避免高等级配方遮挡低等级配方） */
    public ZhenRecipe findRecipe(String zhenType, IOProcessor ioProcessor, SlotPartition partition, Level level) {
        List<ZhenRecipe> candidates = snapshot.recipesByBaseName().get(baseName(zhenType));
        if (candidates == null) return null;
        for (ZhenRecipe recipe : candidates) {
            if (recipe.matchesAll(ioProcessor, partition, level)
                    && RecipeProcessor.isRecipeLevelAllowed(recipe, zhenType)) {
                return recipe;
            }
        }
        return null;
    }

    public synchronized void clearRecipes() {
        snapshot = Snapshot.create(snapshot.revision() + 1, Map.of());
        reloadRecipes = null;
    }

    public synchronized void beginReload() {
        loaded = false;
        reloadRecipes = new LinkedHashMap<>();
    }

    public synchronized void finishReload() {
        if (reloadRecipes != null) {
            snapshot = Snapshot.create(snapshot.revision() + 1, reloadRecipes);
            reloadRecipes = null;
        }
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 仅清空配方缓存但不递增版本号（用于客户端断开连接时清理展示数据）。
     * 避免在客户端侧修改全局版本号影响服务端判断。
     */
    public synchronized void clearClientCache() {
        snapshot = Snapshot.empty();
    }

    public static int getRecipeGeneration() {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, SERVER_INSTANCE.snapshot.revision()));
    }

    public long getRevision() {
        return snapshot.revision();
    }

    public synchronized void replaceClientSnapshot(long revision, List<ZhenRecipe> recipes) {
        Map<String, List<ZhenRecipe>> grouped = new LinkedHashMap<>();
        for (ZhenRecipe recipe : recipes) {
            grouped.computeIfAbsent(baseName(recipe), ignored -> new ArrayList<>()).add(recipe);
        }
        snapshot = Snapshot.create(revision, grouped);
    }

    private static Map<String, List<ZhenRecipe>> mutableCopy(Map<String, List<ZhenRecipe>> source) {
        Map<String, List<ZhenRecipe>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, new ArrayList<>(value)));
        return copy;
    }

    private record Snapshot(long revision, Map<String, List<ZhenRecipe>> recipesByBaseName,
            Map<net.minecraft.resources.Identifier, ZhenRecipe> recipesById, List<ZhenRecipe> allRecipes) {

        private static Snapshot empty() {
            return new Snapshot(0, Map.of(), Map.of(), List.of());
        }

        private static Snapshot create(long revision, Map<String, List<ZhenRecipe>> source) {
            Map<String, List<ZhenRecipe>> grouped = new LinkedHashMap<>();
            Map<net.minecraft.resources.Identifier, ZhenRecipe> byId = new LinkedHashMap<>();
            List<ZhenRecipe> all = new ArrayList<>();
            source.forEach((key, value) -> {
                List<ZhenRecipe> immutable = List.copyOf(value);
                grouped.put(key, immutable);
                for (ZhenRecipe recipe : immutable) {
                    byId.put(recipe.getRecipeId(), recipe);
                    all.add(recipe);
                }
            });
            return new Snapshot(revision, Map.copyOf(grouped), Map.copyOf(byId), List.copyOf(all));
        }
    }
}

package cn.yhzcake.magicio.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;

/**
 * 配方管理器。
 * <p>配方以功能基础名（如 "cinder""forge"）为 key 存储，与具体等级解耦。
 * 一个配方可被同一功能的所有等级复用，处理时根据实际等级动态计算倍率。</p>
 */
public class ZhenRecipeManager {
    private static ZhenRecipeManager INSTANCE;
    /** key = 功能基础名（如 "cinder"），value = 该功能的所有配方 */
    private final Map<String, List<ZhenRecipe>> recipesByBaseName;

    private ZhenRecipeManager() {
        this.recipesByBaseName = new HashMap<>();
    }

    public static ZhenRecipeManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZhenRecipeManager();
        }
        return INSTANCE;
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
    public void addRecipe(ZhenRecipe recipe) {
        String key = baseName(recipe);
        recipesByBaseName.computeIfAbsent(key, k -> new ArrayList<>()).add(recipe);
    }

    public int getRecipeCount() {
        return recipesByBaseName.values().stream().mapToInt(List::size).sum();
    }

    /** 按基础名查找配方 */
    public List<ZhenRecipe> getRecipes(String zhenType) {
        return recipesByBaseName.getOrDefault(baseName(zhenType), List.of());
    }

    /** 获取所有已注册的配方 */
    public List<ZhenRecipe> getAllRecipes() {
        return recipesByBaseName.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    /** 查找匹配的配方 */
    public ZhenRecipe findRecipe(String zhenType, NonNullList<ItemStack> allItems, SlotPartition partition, Level level) {
        List<ZhenRecipe> candidates = recipesByBaseName.get(baseName(zhenType));
        if (candidates == null) return null;
        for (ZhenRecipe recipe : candidates) {
            if (recipe.matches(allItems, partition, level)) {
                return recipe;
            }
        }
        return null;
    }

    public void clearRecipes() {
        recipesByBaseName.clear();
    }
}

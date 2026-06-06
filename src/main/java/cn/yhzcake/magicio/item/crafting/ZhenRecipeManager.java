package cn.yhzcake.magicio.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.yhzcake.magicio.block.inventory.SlotPartition;

public class ZhenRecipeManager {
    private static ZhenRecipeManager INSTANCE;
    private final Map<String, List<ZhenRecipe>> recipesByType;

    private ZhenRecipeManager() {
        this.recipesByType = new HashMap<>();
    }

    public static ZhenRecipeManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZhenRecipeManager();
        }
        return INSTANCE;
    }

    public void addRecipe(ZhenRecipe recipe) {
        recipesByType.computeIfAbsent(recipe.getTypeStr(), k -> new ArrayList<>()).add(recipe);
    }

    public int getRecipeCount() {
        return recipesByType.values().stream().mapToInt(List::size).sum();
    }

    public List<ZhenRecipe> getRecipes(String type) {
        return recipesByType.getOrDefault(type, List.of());
    }

    public ZhenRecipe findRecipe(String type, NonNullList<ItemStack> allItems, SlotPartition partition, Level level) {
        List<ZhenRecipe> candidates = recipesByType.get(type);
        if (candidates == null) return null;
        for (ZhenRecipe recipe : candidates) {
            if (recipe.matches(allItems, partition, level)) {
                return recipe;
            }
        }
        return null;
    }

    public void clearRecipes() {
        recipesByType.clear();
    }
}

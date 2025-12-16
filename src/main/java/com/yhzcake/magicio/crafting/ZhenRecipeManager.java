package com.yhzcake.magicio.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;

import java.util.ArrayList;
import java.util.List;

public class ZhenRecipeManager {
    private static ZhenRecipeManager INSTANCE;
    private final List<ZhenRecipe> recipes;

    private ZhenRecipeManager() {
        this.recipes = new ArrayList<>();
        // 不再添加默认配方
    }

    public static ZhenRecipeManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZhenRecipeManager();
        }
        return INSTANCE;
    }

    public void addRecipe(ZhenRecipe recipe) {
        recipes.add(recipe);
    }

    public List<ZhenRecipe> getRecipes() {
        return recipes;
    }

    /**
     * 根据ZhenBlock类型和输入物品查找匹配的配方
     */
    public ZhenRecipe findRecipe(String type, NonNullList<ItemStack> inputItems) {
        for (ZhenRecipe recipe : recipes) {
            // 检查类型是否匹配
            if (recipe.getType().equals(type) && recipe.matches(inputItems)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * 清除所有配方（用于重新加载或测试）
     */
    public void clearRecipes() {
        recipes.clear();
    }
}
package com.yhzcake.magicio.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;

import com.yhzcake.magicio.block.inventory.SlotPartition;

import java.util.ArrayList;
import java.util.List;

public class ZhenRecipeManager {
    private static ZhenRecipeManager INSTANCE;
    private final List<ZhenRecipe> recipes;

    private ZhenRecipeManager() {
        this.recipes = new ArrayList<>();
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

    public ZhenRecipe findRecipe(String type, NonNullList<ItemStack> allItems, SlotPartition partition, Level level) {
        for (ZhenRecipe recipe : recipes) {
            if (recipe.getTypeStr().equals(type) && recipe.matches(allItems, partition, level)) {
                return recipe;
            }
        }
        return null;
    }

    public void clearRecipes() {
        recipes.clear();
    }
}

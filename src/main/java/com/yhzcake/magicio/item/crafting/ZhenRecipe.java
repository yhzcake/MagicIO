package com.yhzcake.magicio.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public interface ZhenRecipe extends Recipe<ZhenRecipeInput> {
    String getZhenType();
    NonNullList<Ingredient> getInputs();
    NonNullList<ItemStack> getOutputs();
    NonNullList<ResourceLocation> getLootTables();
    int getProcessingTime();
    
    default boolean matchesInputCount(int inputCount) {
        return getInputs().size() <= inputCount;
    }
}
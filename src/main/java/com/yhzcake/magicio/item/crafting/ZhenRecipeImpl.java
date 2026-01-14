package com.yhzcake.magicio.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

@SuppressWarnings("null")
public class ZhenRecipeImpl implements ZhenRecipe{
    private final String zhenType;
    private final NonNullList<Ingredient> inputs;
    private final NonNullList<ItemStack> outputs;
    private final int processingTime;
    private final String group;
    private final NonNullList<ResourceLocation> lootTables;
    
    public ZhenRecipeImpl(String zhenType, NonNullList<Ingredient> inputs, NonNullList<ItemStack> outputs, 
                         int processingTime, String group, NonNullList<ResourceLocation> lootTables) {
        this.zhenType = zhenType;
        this.inputs = inputs;
        this.outputs = outputs;
        this.processingTime = processingTime;
        this.group = group;
        this.lootTables = lootTables;
    }

    @Override
    public String getZhenType() {
        return zhenType;
    }
    
    @Override
    public NonNullList<Ingredient> getInputs() {
        return inputs;
    }

    @Override
    public NonNullList<ItemStack> getOutputs() {
        return outputs;
    }
    
    public NonNullList<ResourceLocation> getLootTables() {
        return lootTables;
    }

    @Override
    public int getProcessingTime() {
        return processingTime;
    }
    
    public  String getGroup() {
        return group;
    }
    
    @Override
    public  NonNullList<Ingredient> getIngredients() {
        return inputs;
    }

    @Override
    public boolean matches(ZhenRecipeInput input,  Level level) {
        if (input.size() < inputs.size()) {
            return false;
        }
        
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).test(input.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public  ItemStack assemble( ZhenRecipeInput input, HolderLookup. Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public  ItemStack getResultItem(HolderLookup. Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
    }

    @Override
    public  RecipeSerializer<?> getSerializer() {
        return ZhenRecipeSerializer.INSTANCE;
    }

    @Override
    public  RecipeType<?> getType() {
        return ModRecipeManager.ZHEN_RECIPE.get();
    }
}
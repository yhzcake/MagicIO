package com.yhzcake.magicio.item.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;
import net.minecraft.core.NonNullList;

@SuppressWarnings("null")
public class ZhenRecipe implements Recipe<ZhenRecipeInput> {
    private final String type; // 关联到 ZhenBlock 类型
    private final NonNullList<Ingredient> inputs;
    private final NonNullList<ItemStack> outputs;
    private final int processingTime; // 处理时间（ticks）

    public ZhenRecipe(String type, NonNullList<Ingredient> inputs, NonNullList<ItemStack> outputs, int processingTime) {
        this.type = type;
        this.inputs = inputs;
        this.outputs = outputs;
        this.processingTime = processingTime;
    }

    public String getRecipeType() {
        return type;
    }
    
    public NonNullList<Ingredient> getInputs() {
        return inputs;
    }

    public NonNullList<ItemStack> getOutputs() {
        return outputs;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    /**
     * 检查给定的输入是否匹配此配方
     */
    public boolean matches(NonNullList<ItemStack> inputItems) {
        if (inputItems.size() < inputs.size()) {
            return false;
        }

        // 检查每个输入槽位是否匹配配方要求
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).test(inputItems.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean matches(ZhenRecipeInput input, Level level) {
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
    public ItemStack assemble(ZhenRecipeInput input, HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ZhenRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeManager.ZHEN_RECIPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputs;
    }
}
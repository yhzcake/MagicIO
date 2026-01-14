package com.yhzcake.magicio.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

@SuppressWarnings("null")
public class ZhenRecipeInput implements RecipeInput {
    private final NonNullList<ItemStack> inputs;

    public ZhenRecipeInput(NonNullList<ItemStack> inputs) {
        this.inputs = inputs;
    }

    public ZhenRecipeInput(ItemStack input) {
        this.inputs = NonNullList.withSize(1,input);
    }

    @Override
    public  ItemStack getItem(int index) {
        return inputs.size() > index ? inputs.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return inputs.size();
    }

    @Override
    public boolean isEmpty() {
        return inputs.stream().allMatch(ItemStack::isEmpty);
    }

    public NonNullList<ItemStack> getInputs() {
        return inputs;
    }
}

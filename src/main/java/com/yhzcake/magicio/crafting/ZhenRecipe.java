package com.yhzcake.magicio.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;

public class ZhenRecipe {
    private final String type; // 关联的 ZhenBlock 类型
    private final NonNullList<Ingredient> inputs;
    private final NonNullList<ItemStack> outputs;
    private final int processingTime; // 处理时间（tick）

    public ZhenRecipe(String type, NonNullList<Ingredient> inputs, NonNullList<ItemStack> outputs, int processingTime) {
        this.type = type;
        this.inputs = inputs;
        this.outputs = outputs;
        this.processingTime = processingTime;
    }

    public String getType() {
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
}
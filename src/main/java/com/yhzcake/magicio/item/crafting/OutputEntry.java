package com.yhzcake.magicio.item.crafting;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public record OutputEntry(@Nullable ItemStack stack, @Nullable Identifier lootTableId) {

    public static OutputEntry item(ItemStack stack) {
        return new OutputEntry(stack, null);
    }

    public static OutputEntry lootTable(Identifier lootTableId) {
        return new OutputEntry(null, lootTableId);
    }

    public boolean isLootTable() {
        return lootTableId != null;
    }

    public NonNullList<ItemStack> roll(ServerLevel level) {
        NonNullList<ItemStack> result = NonNullList.create();
        if (stack != null && !stack.isEmpty()) {
            result.add(stack.copy());
        }
        if (lootTableId != null) {
            LootTable table = ZhenRecipeLoader.getCachedExpandedTable(lootTableId);
            if (table == null) {
                table = ZhenRecipeLoader.getLootTable(level.getServer(), lootTableId);
            }
            LootParams params = new LootParams.Builder(level)
                    .create(LootContextParamSets.EMPTY);
            result.addAll(table.getRandomItems(params));
        }
        return result;
    }

    public boolean isEmpty() {
        return (stack == null || stack.isEmpty()) && lootTableId == null;
    }
}

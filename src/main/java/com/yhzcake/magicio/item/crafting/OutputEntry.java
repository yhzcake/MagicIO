package com.yhzcake.magicio.item.crafting;

import org.jspecify.annotations.Nullable;

import com.yhzcake.magicio.MagicIO;

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
            MagicIO.LOGGER.trace("OutputEntry.roll: looking up loot table {}", lootTableId);
            LootTable table = ZhenRecipeLoader.getCachedExpandedTable(lootTableId);
            if (table == null) {
                MagicIO.LOGGER.trace("OutputEntry.roll: cached expanded table NOT FOUND for {}, falling back to vanilla", lootTableId);
                table = ZhenRecipeLoader.getLootTable(level.getServer(), lootTableId);
            } else {
                MagicIO.LOGGER.trace("OutputEntry.roll: using cached expanded table for {}", lootTableId);
            }
            if (table == null || table == LootTable.EMPTY) {
                MagicIO.LOGGER.warn("OutputEntry.roll: loot table {} is null or EMPTY, no items will be dropped!", lootTableId);
                return result;
            }
            LootParams params = new LootParams.Builder(level)
                    .create(LootContextParamSets.EMPTY);
            var items = table.getRandomItems(params);
            MagicIO.LOGGER.trace("OutputEntry.roll: loot table {} rolled {} items: {}", lootTableId, items.size(), items);
            result.addAll(items);
        }
        return result;
    }

    public boolean isEmpty() {
        return (stack == null || stack.isEmpty()) && lootTableId == null;
    }
}

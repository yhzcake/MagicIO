package cn.yhzcake.magicio.item.crafting;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public record OutputEntry(@Nullable ItemStack rawStack, @Nullable Identifier itemId, int count, @Nullable Identifier lootTableId) {

    public static OutputEntry item(ItemStack stack) {
        return new OutputEntry(stack, null, stack.getCount(), null);
    }

    public static OutputEntry item(Identifier itemId, int count) {
        return new OutputEntry(null, itemId, Math.max(1, count), null);
    }

    public static OutputEntry lootTable(Identifier lootTableId) {
        return new OutputEntry(null, null, 0, lootTableId);
    }

    public @Nullable ItemStack stack() {
        if (rawStack != null) return rawStack;
        if (itemId == null) return null;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId)
                .map(holder -> new ItemStack(holder.value(), count))
                .orElse(ItemStack.EMPTY);
    }

    public boolean isLootTable() {
        return lootTableId != null;
    }

    public NonNullList<ItemStack> roll(ServerLevel level) {
        NonNullList<ItemStack> result = NonNullList.create();
        ItemStack stack = stack();
        if (stack != null && !stack.isEmpty()) {
            result.add(stack.copy());
        }
        if (lootTableId != null) {
            MagicIO.LOGGER.trace("OutputEntry.roll: looking up loot table {}", lootTableId);
            LootTable table = ZhenRecipeLoader.getLootTable(level.getServer(), lootTableId);
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
        ItemStack stack = stack();
        return (stack == null || stack.isEmpty()) && lootTableId == null;
    }
}

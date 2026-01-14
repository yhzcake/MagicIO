package com.yhzcake.magicio.crafting;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.mixin.LootTableMixin;
import com.yhzcake.magicio.utils.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("null")
public class LootTableHelper {
    
    /**
     * 从战利品表生成单个随机物品
     * @param level 服务器世界
     * @param lootTableResource 战利品表资源位置
     * @return 生成的单个物品
     */
    public static List<ItemStack> generateRandomLootItem(ServerLevel level, ResourceLocation lootTableResource) {
        try {
            List<ItemStack> list = new ArrayList<>();
            if (!level.isClientSide()) {
                MagicIO.LOGGER.info("Attempting to generate loot from table: {}", lootTableResource);
                ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableResource);
                LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
                MagicIO.LOGGER.info("Successfully loaded loot table: {}", lootTableResource);
                LootParams.Builder builder = new LootParams.Builder(level);
                LootParams lootParams = builder.create(LootContextParamSets.EMPTY);
                list = lootTable.getRandomItems(lootParams);
                MagicIO.LOGGER.info("Generated {} items from loot table: {}", list.size(), lootTableResource);
            }
            return list;
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to generate loot from table: {}", lootTableResource, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 从标记物品中提取战利品表资源位置
     * @param marker 标记物品
     * @return 战利品表资源位置
     */
    public static ResourceLocation getLootTableFromMarker(ItemStack marker) {
        if (marker.has(ModDataComponents.MARKER)) {
            String markerStr = marker.get(ModDataComponents.MARKER);
            if (markerStr != null) {
                ResourceLocation location = ResourceLocation.parse(markerStr);
                MagicIO.LOGGER.info("Extracted loot table location from marker: {}", location);
                return location;
            }
        }
        MagicIO.LOGGER.warn("Failed to extract loot table location from marker, using default: magicio:zhen_loot");
        return ResourceLocation.parse("magicio:zhen_loot"); // 默认战利品表
    }
    
    /**
     * 创建战利品表标记物品
     * @param lootTableResource 战利品表资源位置
     * @return 标记物品
     */
    public static ItemStack createLootTableMarker(ResourceLocation lootTableResource) {
        ItemStack marker = new ItemStack(com.yhzcake.magicio.item.ModItems.TEST_ITEM.get());
        marker.set(ModDataComponents.MARKER, lootTableResource.toString());
        MagicIO.LOGGER.info("Created loot table marker for: {}", lootTableResource);
        return marker;
    }
    
    /**
     * 检查物品是否是战利品表标记
     * @param stack 物品堆
     * @return 是否是战利品表标记
     */
    public static boolean isLootTableMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean hasMarker = stack.has(ModDataComponents.MARKER);
        boolean isTestItem = stack.is(com.yhzcake.magicio.item.ModItems.TEST_ITEM.get());
        MagicIO.LOGGER.debug("Checking if item is loot table marker: hasMarker={}, isTestItem={}", hasMarker, isTestItem);
        if (!hasMarker){
            return false;
        }
        return isTestItem;
    }
    
    /**
     * 检查战利品表是否有效且只有一个池子
     * @param level 服务器世界
     * @param lootTableResource 战利品表资源位置
     * @return 是否是有效的单池战利品表
     */
    public static boolean isValidSinglePoolLootTable(ServerLevel level, ResourceLocation lootTableResource) {
        try {
            ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableResource);
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
            int poolCount = ((LootTableMixin) lootTable).getPools().size();
            MagicIO.LOGGER.info("Loot table {} has {} pools", lootTableResource, poolCount);
            return poolCount <= 1;
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to validate loot table: {}", lootTableResource, e);
            return false;
        }
    }
}
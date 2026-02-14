package com.yhzcake.magicio.item.crafting;

import com.yhzcake.magicio.MagicIO;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.List;

@SuppressWarnings("null")
public class ZhenRecipeLoader {
    
    /**
     * 验证战利品表是否存在
     * @param server Minecraft服务器实例
     * @param lootTableId 战利品表资源位置
     * @return 如果存在返回true，否则返回false
     */
    public static boolean validateLootTable(MinecraftServer server, ResourceLocation lootTableId) {
        try {
            // 使用正确的Minecraft API验证战利品表存在性
            net.minecraft.resources.ResourceKey<LootTable> lootTableKey = 
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, lootTableId);
            
            // 通过注册表访问器获取战利品表
            LootTable lootTable = server.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE)
                .get(lootTableKey);
            
            // 检查是否是有效的战利品表（不是默认的空表）
            return lootTable != null && lootTable != LootTable.EMPTY;
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to validate loot table {}: {}", lootTableId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 从战利品表获取物品
     * @param server Minecraft服务器实例
     * @param level 服务器世界
     * @param lootTableId 战利品表资源位置
     * @return 生成的物品列表
     */
    public static List<ItemStack> getItemsFromLootTable(MinecraftServer server, ServerLevel level, ResourceLocation lootTableId) {
        try {
            // 获取战利品表
            net.minecraft.resources.ResourceKey<LootTable> lootTableKey = 
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, lootTableId);
            
            LootTable lootTable = server.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE)
                .get(lootTableKey);
            
            if (lootTable == null || lootTable == LootTable.EMPTY) {
                return List.of();
            }
            
            // 生成战利品
            return lootTable.getRandomItems(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY)
            );
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to get items from loot table {}: {}", lootTableId, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * 从JSON输入流加载配方
     * @param inputStream JSON输入流
     * @param server Minecraft服务器实例（用于验证战利品表）
     * @return 解析的配方对象，如果解析失败或战利品表不存在则返回null
     */
    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream, MinecraftServer server) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            
            // 解析基本字段
            String type = json.get("type").getAsString();
            int processingTime = json.get("processing_time").getAsInt();
            
            // 解析输入
            NonNullList<Ingredient> inputs = NonNullList.create();
            if (json.has("inputs")) {
                JsonArray inputsArray = json.getAsJsonArray("inputs");
                for (JsonElement element : inputsArray) {
                    JsonObject inputObj = element.getAsJsonObject();
                    String item = inputObj.get("item").getAsString();
                    int count = inputObj.has("count") ? inputObj.get("count").getAsInt() : 1;
                    
                    ItemStack stack = new ItemStack(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)), 
                        count
                    );
                    inputs.add(Ingredient.of(stack));
                }
            }
            
            // 解析输出 - 添加战利品表验证
            NonNullList<ItemStack> outputs = NonNullList.create();
            if (json.has("outputs")) {
                JsonArray outputsArray = json.getAsJsonArray("outputs");
                for (JsonElement element : outputsArray) {
                    JsonObject outputObj = element.getAsJsonObject();
                    
                    // 检查是否是战利品表输出
                    if (outputObj.has("loot_table")) {
                        String lootTableStr = outputObj.get("loot_table").getAsString();
                        ResourceLocation lootTableId = ResourceLocation.parse(lootTableStr);
                        
                        // 验证战利品表是否存在
                        if (!validateLootTable(server, lootTableId)) {
                            MagicIO.LOGGER.warn("Loot table {} not found, recipe will be discarded", lootTableId);
                            return null; // 战利品表不存在，配方作废
                        }
                        
                        // 获取战利品表物品
                        List<ItemStack> lootItems = getItemsFromLootTable(server, null, lootTableId);
                        outputs.addAll(lootItems);
                    } else if (outputObj.has("item")) {
                        // 普通物品输出
                        String item = outputObj.get("item").getAsString();
                        int count = outputObj.has("count") ? outputObj.get("count").getAsInt() : 1;
                        
                        ItemStack stack = new ItemStack(
                            BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)), 
                            count
                        );
                        outputs.add(stack);
                    }
                }
            }
            
            return new ZhenRecipe(type, inputs, outputs, processingTime);
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to load recipe from JSON: {}", e.getMessage());
            return null;
        }
    }
    
    // 保持向后兼容的旧方法
    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream) {
        // 这个方法需要MinecraftServer实例才能验证战利品表
        // 在实际使用中应该调用带server参数的版本
        MagicIO.LOGGER.warn("Using deprecated loadRecipeFromJson method without server validation");
        return null;
    }
}
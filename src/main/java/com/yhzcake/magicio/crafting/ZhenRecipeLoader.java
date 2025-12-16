package com.yhzcake.magicio.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yhzcake.magicio.MagicIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.NonNullList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ZhenRecipeLoader {
    private static final Gson GSON = new Gson();

    /**
     * 从JSON文件加载配方
     * @param inputStream JSON文件输入流
     * @return ZhenRecipe对象
     */
    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return parseRecipe(json);
        } catch (Exception e) {
            MagicIO.LOGGER.warn(String.valueOf(e));
            return null;
        }
    }

    /**
     * 解析JSON对象为ZhenRecipe
     */
    private static ZhenRecipe parseRecipe(JsonObject json) {
        // 解析类型
        String type = json.get("type").getAsString();

        // 解析输入
        JsonArray inputArray = json.getAsJsonArray("input");
        NonNullList<Ingredient> inputs = NonNullList.create();
        
        for (JsonElement element : inputArray) {
            JsonObject inputObj = element.getAsJsonObject();
            String itemName = inputObj.get("item").getAsString();
            int count = inputObj.get("count").getAsInt();
            
            if ("minecraft:air".equals(itemName) || count <= 0) {
                inputs.add(Ingredient.of(ItemStack.EMPTY));
            } else {
                ResourceLocation itemId = ResourceLocation.parse(itemName);
                Item item = BuiltInRegistries.ITEM.get(itemId);
                ItemStack stack = new ItemStack(item, count);
                inputs.add(Ingredient.of(stack));
            }
        }

        // 解析输出
        JsonArray outputArray = json.getAsJsonArray("output");
        NonNullList<ItemStack> outputs = NonNullList.create();
        
        for (JsonElement element : outputArray) {
            JsonObject outputObj = element.getAsJsonObject();
            
            // 检查是否是战利品表
            if (outputObj.has("loot_table")) {
                // 对于战利品表，我们创建一个特殊的标记物品
                // 在实际应用中，这个标记会被替换为从战利品表生成的实际物品
                String lootTable = outputObj.get("loot_table").getAsString();
                // 创建一个标记物品来表示战利品表输出
                ItemStack lootMarker = LootTableHelper.createLootTableMarker(ResourceLocation.parse(lootTable));
                outputs.add(lootMarker);
            } else {
                // 普通物品输出
                String outputItemName = outputObj.get("item").getAsString();
                int outputCount = outputObj.get("count").getAsInt();
                
                ResourceLocation outputItemId = ResourceLocation.parse(outputItemName);
                Item outputItem = BuiltInRegistries.ITEM.get(outputItemId);
                ItemStack output = new ItemStack(outputItem, outputCount);
                outputs.add(output);
            }
        }

        // 解析处理时间
        int processingTime = json.get("processing_time").getAsInt();

        return new ZhenRecipe(type, inputs, outputs, processingTime);
    }
}
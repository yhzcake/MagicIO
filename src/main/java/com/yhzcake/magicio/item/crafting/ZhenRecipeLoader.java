package com.yhzcake.magicio.item.crafting;

import com.yhzcake.magicio.MagicIO;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZhenRecipeLoader {

    private static LootTable getLootTable(MinecraftServer server, Identifier lootTableId) {
        var lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);
        return server.reloadableRegistries().getLootTable(lootTableKey);
    }

    public static boolean validateLootTable(MinecraftServer server, Identifier lootTableId) {
        try {
            LootTable lootTable = getLootTable(server, lootTableId);
            return lootTable != null && lootTable != LootTable.EMPTY;
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to validate loot table {}: {}", lootTableId, e.getMessage());
            return false;
        }
    }

    public static List<ItemStack> getItemsFromLootTable(MinecraftServer server, ServerLevel level, Identifier lootTableId) {
        try {
            LootTable lootTable = getLootTable(server, lootTableId);
            if (lootTable == null || lootTable == LootTable.EMPTY) {
                return List.of();
            }

            return lootTable.getRandomItems(
                    new LootParams.Builder(level)
                            .create(LootContextParamSets.EMPTY)
            );
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to get items from loot table {}: {}", lootTableId, e.getMessage());
            return List.of();
        }
    }

    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream, MinecraftServer server) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            String type = json.get("type").getAsString();
            int processingTime = json.get("processing_time").getAsInt();

            Map<String, NonNullList<Ingredient>> zoneInputs = new LinkedHashMap<>();
            if (json.has("inputs")) {
                JsonElement inputsElement = json.get("inputs");
                if (inputsElement.isJsonArray()) {
                    NonNullList<Ingredient> flat = NonNullList.create();
                    for (JsonElement element : inputsElement.getAsJsonArray()) {
                        JsonObject inputObj = element.getAsJsonObject();
                        String item = inputObj.get("item").getAsString();
                        Identifier itemId = Identifier.parse(item);
                        BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> flat.add(Ingredient.of(holder.value())));
                    }
                    zoneInputs.put("input_all", flat);
                } else if (inputsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : inputsElement.getAsJsonObject().entrySet()) {
                        NonNullList<Ingredient> ingredients = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            JsonObject inputObj = element.getAsJsonObject();
                            String item = inputObj.get("item").getAsString();
                            Identifier itemId = Identifier.parse(item);
                            BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> ingredients.add(Ingredient.of(holder.value())));
                        }
                        zoneInputs.put(entry.getKey(), ingredients);
                    }
                }
            }

            Map<String, NonNullList<OutputEntry>> zoneOutputs = new LinkedHashMap<>();
            if (json.has("outputs")) {
                JsonElement outputsElement = json.get("outputs");
                if (outputsElement.isJsonArray()) {
                    NonNullList<OutputEntry> entries = NonNullList.create();
                    for (JsonElement element : outputsElement.getAsJsonArray()) {
                        JsonObject outputObj = element.getAsJsonObject();
                        if (outputObj.has("loot_table")) {
                            Identifier lootId = Identifier.parse(outputObj.get("loot_table").getAsString());
                            entries.add(OutputEntry.lootTable(lootId));
                        } else {
                            String item = outputObj.get("item").getAsString();
                            int count = outputObj.has("count") ? outputObj.get("count").getAsInt() : 1;
                            Identifier itemId = Identifier.parse(item);
                            BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> entries.add(OutputEntry.item(new ItemStack(holder.value(), count))));
                        }
                    }
                    zoneOutputs.put("output_all", entries);
                } else if (outputsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : outputsElement.getAsJsonObject().entrySet()) {
                        NonNullList<OutputEntry> entries = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            JsonObject outputObj = element.getAsJsonObject();
                            if (outputObj.has("loot_table")) {
                                Identifier lootId = Identifier.parse(outputObj.get("loot_table").getAsString());
                                entries.add(OutputEntry.lootTable(lootId));
                            } else {
                                String item = outputObj.get("item").getAsString();
                                int count = outputObj.has("count") ? outputObj.get("count").getAsInt() : 1;
                                Identifier itemId = Identifier.parse(item);
                                BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> entries.add(OutputEntry.item(new ItemStack(holder.value(), count))));
                            }
                        }
                        zoneOutputs.put(entry.getKey(), entries);
                    }
                }
            }

            Identifier lootTableId = null;
            if (json.has("loot_table")) {
                String lootTableStr = json.get("loot_table").getAsString();
                lootTableId = Identifier.parse(lootTableStr);
                if (!validateLootTable(server, lootTableId)) {
                    MagicIO.LOGGER.warn("Loot table {} not found, recipe will be discarded", lootTableId);
                    return null;
                }
            }

            return new ZhenRecipe(type, zoneInputs, zoneOutputs, lootTableId, processingTime);
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to load recipe from JSON: {}", e.getMessage());
            return null;
        }
    }

    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream) {
        MagicIO.LOGGER.warn("Using deprecated loadRecipeFromJson method without server validation");
        return null;
    }
}

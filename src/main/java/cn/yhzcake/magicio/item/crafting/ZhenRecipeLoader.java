package cn.yhzcake.magicio.item.crafting;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.io.ModIOTypes;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZhenRecipeLoader {

    public static LootTable getLootTable(MinecraftServer server, Identifier lootTableId) {
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

    public static ZhenRecipe loadRecipeFromJson(Identifier recipeId, InputStream inputStream, MinecraftServer server) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            Identifier typeId = Identifier.parse(json.get("zhen_type").getAsString());
            if (ZhenType.ZHEN_TYPES != null) {
                ZhenType.ZHEN_TYPES.get(typeId).orElseThrow(() -> new IllegalArgumentException("Unknown ZhenType: " + typeId));
            }
            int processingTime = json.get("processing_time").getAsInt();

            List<RecipeInput<?>> inputs = new ArrayList<>();
            List<RecipeOutput<?>> outputs = new ArrayList<>();
            var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

            if (json.has("inputs")) {
                JsonObject inputZones = json.getAsJsonObject("inputs");
                for (Map.Entry<String, JsonElement> entry : inputZones.entrySet()) {
                    NonNullList<ItemRequirement> requirements = NonNullList.create();
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        requirements.add(ZhenRecipeSerializer.ITEM_REQUIREMENT_CODEC.parse(ops, element).getOrThrow(JsonParseException::new));
                    }
                    inputs.add(new RecipeInput<>(ModIOTypes.ITEM.get(), entry.getKey(), requirements));
                }
            }

            if (json.has("outputs")) {
                JsonElement outputsElement = json.get("outputs");
                if (outputsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : outputsElement.getAsJsonObject().entrySet()) {
                        NonNullList<OutputEntry> entries = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            JsonObject outputObj = element.getAsJsonObject();
                            if (outputObj.has("loot_table")) {
                                Identifier lootId = Identifier.parse(outputObj.get("loot_table").getAsString());
                                entries.add(OutputEntry.lootTable(lootId));
                            } else if (outputObj.has("item")) {
                                String item = outputObj.get("item").getAsString();
                                int count = outputObj.has("count") ? outputObj.get("count").getAsInt() : 1;
                                Identifier itemId = Identifier.parse(item);
                                BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> {
                                    try {
                                        entries.add(OutputEntry.item(new ItemStack(holder.value(), count)));
                                    } catch (Exception e) {
                                        MagicIO.LOGGER.debug("Skipping item {} - registries not ready: {}", itemId, e.getMessage());
                                    }
                                });
                            }
                        }
                        outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), entry.getKey(), entries));
                    }
                }
            }

            if (json.has("fluid_inputs")) {
                JsonElement fluidsElement = json.get("fluid_inputs");
                if (fluidsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : fluidsElement.getAsJsonObject().entrySet()) {
                        NonNullList<FluidRequirement> fluids = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            fluids.add(FluidRequirement.CODEC.parse(ops, element).getOrThrow(JsonParseException::new));
                        }
                        inputs.add(new RecipeInput<>(ModIOTypes.FLUID.get(), entry.getKey(), fluids));
                    }
                }
            }

            if (json.has("fluid_outputs")) {
                JsonElement fluidsElement = json.get("fluid_outputs");
                if (fluidsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : fluidsElement.getAsJsonObject().entrySet()) {
                        NonNullList<FluidStack> fluids = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            fluids.add(ZhenRecipeSerializer.FLUID_OUTPUT_CODEC.parse(ops, element).getOrThrow(JsonParseException::new));
                        }
                        outputs.add(new RecipeOutput<>(ModIOTypes.FLUID.get(), entry.getKey(), fluids));
                    }
                }
            }

            return new ZhenRecipe(recipeId, typeId, inputs, outputs, processingTime);
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to load recipe from JSON: {}", e.getMessage());
            return null;
        }
    }

}

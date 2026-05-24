package com.yhzcake.magicio.item.crafting;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.io.ModIOTypes;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
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

    @SuppressWarnings("unchecked")
    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream, MinecraftServer server) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            String type = json.get("type").getAsString();
            int processingTime = json.get("processing_time").getAsInt();

            List<RecipeInput<?>> inputs = new ArrayList<>();
            List<RecipeOutput<?>> outputs = new ArrayList<>();

            if (json.has("inputs")) {
                JsonElement inputsElement = json.get("inputs");
                if (inputsElement.isJsonArray()) {
                    NonNullList<Ingredient> flat = NonNullList.create();
                    for (JsonElement element : inputsElement.getAsJsonArray()) {
                        JsonObject inputObj = element.getAsJsonObject();
                        String item = inputObj.get("item").getAsString();
                        int count = Math.max(1, inputObj.has("count") ? inputObj.get("count").getAsInt() : 1);
                        Identifier itemId = Identifier.parse(item);
                        BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> {
                            Ingredient ing = Ingredient.of(holder.value());
                            for (int i = 0; i < count; i++) flat.add(ing);
                        });
                    }
                    inputs.add(new RecipeInput<>(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL.getName(), flat));
                } else if (inputsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : inputsElement.getAsJsonObject().entrySet()) {
                        NonNullList<Ingredient> ingredients = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            JsonObject inputObj = element.getAsJsonObject();
                            String item = inputObj.get("item").getAsString();
                            int count = Math.max(1, inputObj.has("count") ? inputObj.get("count").getAsInt() : 1);
                            Identifier itemId = Identifier.parse(item);
                            BuiltInRegistries.ITEM.get(itemId).ifPresent(holder -> {
                                Ingredient ing = Ingredient.of(holder.value());
                                for (int i = 0; i < count; i++) ingredients.add(ing);
                            });
                        }
                        inputs.add(new RecipeInput<>(ModIOTypes.ITEM.get(), entry.getKey(), ingredients));
                    }
                }
            }

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
                    outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL.getName(), entries));
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
                        outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), entry.getKey(), entries));
                    }
                }
            }

            if (json.has("fluid_inputs")) {
                JsonElement fluidsElement = json.get("fluid_inputs");
                if (fluidsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : fluidsElement.getAsJsonObject().entrySet()) {
                        NonNullList<ZhenRecipe.FluidIngredient> fluids = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            JsonObject fluidObj = element.getAsJsonObject();
                            String fluidStr = fluidObj.get("fluid").getAsString();
                            int amount = fluidObj.get("amount").getAsInt();
                            fluids.add(parseFluidIngredient(fluidStr, amount));
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
                            JsonObject fluidObj = element.getAsJsonObject();
                            String fluidStr = fluidObj.get("fluid").getAsString();
                            int amount = fluidObj.get("amount").getAsInt();
                            fluids.add(new FluidStack(BuiltInRegistries.FLUID.get(Identifier.parse(fluidStr)).orElseThrow(), amount));
                        }
                        outputs.add(new RecipeOutput<>(ModIOTypes.FLUID.get(), entry.getKey(), fluids));
                    }
                }
            }

            if (json.has("loot_table")) {
                String lootTableStr = json.get("loot_table").getAsString();
                Identifier lootTableId = Identifier.parse(lootTableStr);
                if (server != null && !validateLootTable(server, lootTableId)) {
                    MagicIO.LOGGER.warn("Loot table {} not found, recipe will be discarded", lootTableId);
                    return null;
                }
                if (!outputs.isEmpty()) {
                    RecipeOutput<?> first = outputs.get(0);
                    NonNullList<OutputEntry> augmented = NonNullList.create();
                    augmented.addAll((NonNullList<OutputEntry>) first.specification());
                    augmented.add(OutputEntry.lootTable(lootTableId));
                    outputs.set(0, new RecipeOutput<>(ModIOTypes.ITEM.get(), first.zoneName(), augmented));
                } else {
                    outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL.getName(),
                            NonNullList.of(OutputEntry.lootTable(lootTableId))));
                }
            }

            return new ZhenRecipe(type, inputs, outputs, processingTime);
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to load recipe from JSON: {}", e.getMessage());
            return null;
        }
    }

    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream) {
        MagicIO.LOGGER.warn("Using deprecated loadRecipeFromJson method without server validation");
        return null;
    }

    private static ZhenRecipe.FluidIngredient parseFluidIngredient(String fluidStr, int amount) {
        if (fluidStr.startsWith("#")) {
            Identifier tagId = Identifier.parse(fluidStr.substring(1));
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            java.util.Optional<HolderSet.Named<Fluid>> named = BuiltInRegistries.FLUID.get(tagKey);
            HolderSet<Fluid> holders = named.isPresent() ? named.get() : HolderSet.empty();
            return new ZhenRecipe.FluidIngredient(holders, amount);
        } else {
            Identifier fluidId = Identifier.parse(fluidStr);
            Holder<Fluid> holder = BuiltInRegistries.FLUID.get(fluidId).orElseThrow();
            return new ZhenRecipe.FluidIngredient(HolderSet.direct(holder), amount);
        }
    }
}

package cn.yhzcake.magicio.item.crafting;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.io.ModIOTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
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
import java.util.concurrent.ConcurrentHashMap;

public class ZhenRecipeLoader {

    private static final Map<Identifier, LootTable> EXPANDED_CACHE = new ConcurrentHashMap<>();

    public static LootTable getLootTable(MinecraftServer server, Identifier lootTableId) {
        var lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, lootTableId);
        return server.reloadableRegistries().getLootTable(lootTableKey);
    }

    public static LootTable getCachedExpandedTable(Identifier lootTableId) {
        return EXPANDED_CACHE.get(lootTableId);
    }

    public static void clearCache() {
        EXPANDED_CACHE.clear();
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

    private static void expandAndCache(Identifier lootTableId, MinecraftServer server) {
        if (server == null || EXPANDED_CACHE.containsKey(lootTableId)) return;
        try {
            Identifier filePath = Identifier.parse(lootTableId.getNamespace() + ":" + "loot_table/" + lootTableId.getPath() + ".json");
            var resourceOpt = server.getResourceManager().getResource(filePath);
            if (resourceOpt.isEmpty()) return;

            Gson gson = new Gson();
            JsonObject root;
            try (Reader reader = new InputStreamReader(resourceOpt.get().open())) {
                root = gson.fromJson(reader, JsonObject.class);
            }
            if (root == null) return;

            JsonArray pools = root.getAsJsonArray("pools");
            if (pools == null) return;

            boolean modified = false;
            for (int pi = 0; pi < pools.size(); pi++) {
                JsonObject poolObj = pools.get(pi).getAsJsonObject();

                // 修正 binomial rolls：补上 type 字段
                if (poolObj.has("rolls")) {
                    JsonElement rollsElem = poolObj.get("rolls");
                    if (rollsElem.isJsonObject()) {
                        JsonObject rollsObj = rollsElem.getAsJsonObject();
                        if (rollsObj.has("n") && !rollsObj.has("type")) {
                            rollsObj.addProperty("type", "minecraft:binomial");
                        }
                    }
                }

                JsonArray entries = poolObj.getAsJsonArray("entries");
                if (entries == null) continue;

                JsonArray newEntries = new JsonArray();
                for (int ei = 0; ei < entries.size(); ei++) {
                    JsonObject entryObj = entries.get(ei).getAsJsonObject();
                    if (!entryObj.has("name")) { newEntries.add(entryObj); continue; }
                    String name = entryObj.get("name").getAsString();
                    if (!name.startsWith("#")) { newEntries.add(entryObj); continue; }

                    modified = true;
                    List<String> itemIds = resolveItemIds(name.substring(1), server, gson);
                    MagicIO.LOGGER.trace("expandAndCache: resolved '{}' -> {} items: {}", name, itemIds.size(), itemIds);
                    if (itemIds.isEmpty()) continue;

                    for (String itemId : itemIds) {
                        JsonObject copy = entryObj.deepCopy();
                        copy.addProperty("name", itemId);
                        newEntries.add(copy);
                    }
                }
                if (modified) {
                    poolObj.add("entries", newEntries);
                }
            }

            if (!modified) return;

            var result = LootTable.DIRECT_CODEC.parse(JsonOps.INSTANCE, root);
            result.result().ifPresent(table -> {
                EXPANDED_CACHE.put(lootTableId, table);
                int entryCount = 0;
                for (int i = 0; i < pools.size(); i++) {
                    entryCount += pools.get(i).getAsJsonObject().getAsJsonArray("entries").size();
                }
                MagicIO.LOGGER.info("Expanded loot table {} with #tag entries ({} pools, {} total entries)", lootTableId, pools.size(), entryCount);
            });
            result.error().ifPresent(err ->
                MagicIO.LOGGER.warn("Failed to parse expanded loot table {}: {}", lootTableId, err.message())
            );
        } catch (Exception e) {
            MagicIO.LOGGER.warn("Failed to expand loot table {}: {}", lootTableId, e.getMessage());
        }
    }

    private static List<String> resolveItemIds(String tagStr, MinecraftServer server, Gson gson) {
        // 直接从服务端的注册表标签系统解析，不依赖手动读 tag JSON 文件
        HolderLookup.RegistryLookup<Item> itemRegistry = server.reloadableRegistries().lookup().lookupOrThrow(Registries.ITEM);
        Identifier tagId = Identifier.parse(tagStr);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

        List<String> results = new ArrayList<>();
        itemRegistry.get(tagKey).ifPresent(named -> {
            for (Holder<Item> holder : named) {
                holder.unwrapKey().ifPresent(key -> results.add(key.identifier().toString()));
            }
        });
        MagicIO.LOGGER.trace("resolveItemIds: registry lookup '{}' -> {} items", tagStr, results.size());
        return results;
    }

    @SuppressWarnings("unchecked")
    public static ZhenRecipe loadRecipeFromJson(InputStream inputStream, MinecraftServer server) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            Identifier typeId = Identifier.parse(json.get("type").getAsString());
            if (ZhenType.ZHEN_TYPES != null) {
                ZhenType.ZHEN_TYPES.get(typeId).orElseThrow(() -> new IllegalArgumentException("Unknown ZhenType: " + typeId));
            }
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
                if (outputsElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : outputsElement.getAsJsonObject().entrySet()) {
                        NonNullList<OutputEntry> entries = NonNullList.create();
                        for (JsonElement element : entry.getValue().getAsJsonArray()) {
                            JsonObject outputObj = element.getAsJsonObject();
                            if (outputObj.has("loot_table")) {
                                Identifier lootId = Identifier.parse(outputObj.get("loot_table").getAsString());
                                expandAndCache(lootId, server);
                                entries.add(OutputEntry.lootTable(lootId));
                            } else if (outputObj.has("item")) {
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

            return new ZhenRecipe(typeId, inputs, outputs, processingTime);
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

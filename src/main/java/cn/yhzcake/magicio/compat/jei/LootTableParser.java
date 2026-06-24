package cn.yhzcake.magicio.compat.jei;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LootTableParser {

    public static record LootDisplayItem(ItemStack stack, double expectedCount, List<Component> tooltipLines) {}

    private static final Gson GSON = new Gson();

    public static List<LootDisplayItem> parse(Identifier lootTableId) {
        try {
            String classpathPath = "data/" + lootTableId.getNamespace() + "/loot_table/" + lootTableId.getPath() + ".json";
            var inStream = LootTableParser.class.getClassLoader().getResourceAsStream(classpathPath);
            if (inStream == null) return List.of();

            JsonObject json;
            try (var in = inStream) {
                json = GSON.fromJson(new String(in.readAllBytes()), JsonObject.class);
            }
            if (json == null || !json.has("pools")) return List.of();

            JsonArray pools = json.getAsJsonArray("pools");
            List<PoolInfo> poolInfos = new ArrayList<>();

            for (var poolElem : pools) {
                if (!poolElem.isJsonObject()) continue;
                JsonObject pool = poolElem.getAsJsonObject();
                if (!pool.has("entries")) continue;

                double poolRolls = parseRolls(pool);
                JsonArray entries = pool.getAsJsonArray("entries");
                List<ExpandedEntry> expanded = new ArrayList<>();
                int totalEffectiveWeight = 0;

                for (var entryElem : entries) {
                    if (!entryElem.isJsonObject()) continue;
                    JsonObject entry = entryElem.getAsJsonObject();
                    int weight = entry.has("weight") ? entry.get("weight").getAsInt() : 1;
                    String type = entry.get("type").getAsString();

                    if ("minecraft:item".equals(type)) {
                        String name = entry.get("name").getAsString();
                        var opt = BuiltInRegistries.ITEM.get(Identifier.parse(name));
                        if (opt.isPresent()) {
                            expanded.add(new ExpandedEntry(weight, opt.get().value(), 1));
                            totalEffectiveWeight += weight;
                        }
                    } else if ("minecraft:tag".equals(type)) {
                        String tagName = entry.get("name").getAsString();
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(tagName));
                        var tag = BuiltInRegistries.ITEM.get(tagKey);
                        if (tag.isPresent()) {
                            int count = 0;
                            for (var h : tag.get()) count++;
                            for (var h : tag.get()) {
                                expanded.add(new ExpandedEntry(weight, h.value(), count));
                            }
                            totalEffectiveWeight += weight * count;
                        }
                    }
                }

                if (totalEffectiveWeight > 0) {
                    poolInfos.add(new PoolInfo(poolRolls, totalEffectiveWeight, expanded));
                }
            }

            Map<Item, MergedInfo> mergedMap = new LinkedHashMap<>();

            for (PoolInfo pi : poolInfos) {
                for (ExpandedEntry ee : pi.expanded) {
                    double perRollProb = (double) ee.weight / pi.totalEffectiveWeight;
                    double expectedFromThisPool = pi.rolls * perRollProb;

                    MergedInfo mi = mergedMap.computeIfAbsent(ee.item, k -> new MergedInfo(0, new ArrayList<>()));

                    mi.totalExpected += expectedFromThisPool;

                    String rollStr = pi.rolls == (long) pi.rolls
                            ? String.valueOf((long) pi.rolls)
                            : String.valueOf(pi.rolls);
                    double pct = perRollProb * 100;

                    String itemNote = ee.groupSize > 1 ? "/" + ee.groupSize + "种" : "";
                    String poolPart = String.format("%s×%.1f%%%s", rollStr, pct, itemNote);
                    mi.poolLines.add(poolPart);
                }
            }

            List<LootDisplayItem> result = new ArrayList<>();
            for (var entry : mergedMap.entrySet()) {
                ItemStack stack = new ItemStack(entry.getKey());
                MergedInfo mi = entry.getValue();

                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(String.format("(%.2f/次)", mi.totalExpected)).withStyle(ChatFormatting.GOLD));
                for (String line : mi.poolLines) {
                    lines.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
                }

                result.add(new LootDisplayItem(stack, mi.totalExpected, lines));
            }

            return result;

        } catch (Exception e) {
            MagicIO.LOGGER.warn("[JEI] Failed to parse loot table {}: {}", lootTableId, e.getMessage());
            return List.of();
        }
    }

    private static double parseRolls(JsonObject pool) {
        JsonElement rollsElem = pool.get("rolls");
        if (rollsElem == null) return 1;
        if (rollsElem.isJsonPrimitive() && rollsElem.getAsJsonPrimitive().isNumber()) {
            return rollsElem.getAsInt();
        }
        if (rollsElem.isJsonObject()) {
            JsonObject rollsObj = rollsElem.getAsJsonObject();
            String type = rollsObj.get("type").getAsString();
            if ("minecraft:binomial".equals(type)) {
                double n = rollsObj.get("n").getAsDouble();
                double p = rollsObj.get("p").getAsDouble();
                return n * p;
            }
        }
        return 1;
    }

    private record ExpandedEntry(int weight, Item item, int groupSize) {}
    private record PoolInfo(double rolls, int totalEffectiveWeight, List<ExpandedEntry> expanded) {}

    private static class MergedInfo {
        double totalExpected;
        List<String> poolLines;
        MergedInfo(double totalExpected, List<String> poolLines) {
            this.totalExpected = totalExpected;
            this.poolLines = poolLines;
        }
    }
}

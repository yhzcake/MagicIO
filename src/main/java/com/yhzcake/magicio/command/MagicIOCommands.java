package com.yhzcake.magicio.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.yhzcake.magicio.MagicIO;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@EventBusSubscriber(modid = MagicIO.MOD_ID, value = Dist.CLIENT)
public class MagicIOCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("magicio")
                .then(Commands.literal("tags")
                    .executes(MagicIOCommands::exportTags)
                )
        );
    }

    private static int exportTags(CommandContext<CommandSourceStack> ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ctx.getSource().sendFailure(Component.literal("请先进入世界再执行此指令"));
            return 0;
        }

        try {
            Registry<Item> itemRegistry = mc.level.registryAccess().lookupOrThrow(Registries.ITEM);

            // 按命名空间分组收集：namespace -> (tagPath -> itemsArray)
            Map<String, Map<String, JsonArray>> grouped = new TreeMap<>();
            for (HolderSet.Named<Item> named : itemRegistry.getTags().toList()) {
                TagKey<Item> tagKey = named.key();
                String fullPath = tagKey.location().toString();     // e.g. "minecraft:pig_food"
                int colonIdx = fullPath.indexOf(':');
                String namespace = colonIdx >= 0 ? fullPath.substring(0, colonIdx) : "";
                String tagPath = colonIdx >= 0 ? fullPath.substring(colonIdx + 1) : fullPath;

                JsonArray itemsArray = new JsonArray();
                for (Holder<Item> holder : named) {
                    itemsArray.add(holder.unwrapKey().orElseThrow().identifier().toString());
                }
                if (itemsArray.isEmpty()) {
                    continue;
                }

                grouped.computeIfAbsent(namespace, k -> new TreeMap<>()).put(tagPath, itemsArray);
            }

            // 构建嵌套 JSON，命名空间和标签路径均已按字典序排序
            JsonObject tagsJson = new JsonObject();
            for (Map.Entry<String, Map<String, JsonArray>> nsEntry : grouped.entrySet()) {
                JsonObject nsObj = new JsonObject();
                for (Map.Entry<String, JsonArray> tagEntry : nsEntry.getValue().entrySet()) {
                    JsonObject tagObj = new JsonObject();
                    tagObj.add("items", tagEntry.getValue());
                    nsObj.add(tagEntry.getKey(), tagObj);
                }
                tagsJson.add(nsEntry.getKey(), nsObj);
            }

            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

            JsonObject root = new JsonObject();
            int totalTags = grouped.values().stream().mapToInt(Map::size).sum();
            root.addProperty("generated_at", now.toString());
            root.addProperty("total_tags", totalTags);
            root.add("tags", tagsJson);

            Path gameDir = mc.gameDirectory.toPath();
            Path magicIODir = gameDir.resolve("magicio");
            Files.createDirectories(magicIODir);
            Path tagsFile = magicIODir.resolve("tags-" + timestamp + ".json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(tagsFile, gson.toJson(root), StandardCharsets.UTF_8);

            MagicIO.LOGGER.info("已导出 {} 个物品标签到 {}", totalTags, tagsFile);
            ctx.getSource().sendSuccess(
                () -> Component.literal("已导出 " + totalTags + " 个物品标签到 magicio/tags-" + timestamp + ".json"),
                false
            );

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            MagicIO.LOGGER.error("导出标签失败", e);
            ctx.getSource().sendFailure(Component.literal("导出失败: " + e.getMessage()));
            return 0;
        }
    }
}

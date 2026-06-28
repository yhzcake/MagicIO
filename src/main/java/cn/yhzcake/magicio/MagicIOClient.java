package cn.yhzcake.magicio;

import cn.yhzcake.magicio.item.crafting.ZhenRecipeLoader;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = MagicIO.MOD_ID, value = Dist.CLIENT)
public class MagicIOClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.getContainer().registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        MagicIO.LOGGER.info("HELLO FROM CLIENT SETUP");
        MagicIO.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // 在客户端初始化时提前加载配方，供 JEI 注册时读取
        loadRecipes();
    }

    /** 从 classpath 加载配方到 ZhenRecipeManager 缓存 */
    private static void loadRecipes() {
        if (ZhenRecipeManager.getInstance().getRecipeCount() > 0) return;

        try {
            var url = MagicIOClient.class.getResource("/data/magic_io/recipe/unstable/cinder.json");
            if (url == null) {
                MagicIO.LOGGER.warn("[Client] Cannot find recipe files in classpath");
                return;
            }

            URI uri = url.toURI();
            List<java.nio.file.Path> recipeFiles;

            if ("file".equals(uri.getScheme())) {
                var recipeDir = java.nio.file.Paths.get(uri).getParent().getParent();
                try (var stream = Files.walk(recipeDir)) {
                    recipeFiles = stream.filter(f -> f.toString().endsWith(".json")).toList();
                }
            } else if ("jar".equals(uri.getScheme())) {
                String spec = uri.getSchemeSpecificPart();
                int sep = spec.indexOf("!/");
                String jarFile = spec.substring(0, sep);
                URI jarUri = URI.create("jar:file:" + jarFile + "!/data/magic_io/recipe");
                try (var fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                    var jarRecipeDir = fs.getPath("/data/magic_io/recipe");
                    if (Files.exists(jarRecipeDir)) {
                        try (var stream = Files.walk(jarRecipeDir)) {
                            recipeFiles = stream.filter(f -> f.toString().endsWith(".json")).toList();
                        }
                    } else {
                        recipeFiles = List.of();
                    }
                }
            } else {
                return;
            }

            for (var path : recipeFiles) {
                try (var in = Files.newInputStream(path)) {
                    var recipe = ZhenRecipeLoader.loadRecipeFromJson(in, null);
                    if (recipe != null) ZhenRecipeManager.getInstance().addRecipe(recipe);
                } catch (Exception ignored) {}
            }
            MagicIO.LOGGER.info("[Client] Loaded {} recipes for client display", ZhenRecipeManager.getInstance().getRecipeCount());
        } catch (Exception e) {
            MagicIO.LOGGER.error("[Client] Failed to load recipes", e);
        }
    }
}

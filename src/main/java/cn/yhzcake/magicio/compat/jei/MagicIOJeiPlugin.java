package cn.yhzcake.magicio.compat.jei;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.ModBlocks;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeLoader;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.nio.file.*;
import java.util.*;

@JeiPlugin
public class MagicIOJeiPlugin implements IModPlugin {

    public static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "jei_plugin");

    private static final Map<String, IRecipeType<ZhenRecipe>> CATEGORIES = new LinkedHashMap<>();
    /** 每个 baseName 对应的展示配方（最低等级的） */
    private static final Map<String, ZhenRecipe> DISPLAY_RECIPES = new LinkedHashMap<>();

    private static IRecipeType<ZhenRecipe> type(String baseName) {
        return IRecipeType.create(MagicIO.MOD_ID, baseName, ZhenRecipe.class);
    }

    static String baseName(ZhenRecipe recipe) {
        return stripSuffix(ZhenLevel.baseName(recipe.getZhenTypeId().getPath()));
    }

    private static String stripSuffix(String name) {
        if (name.endsWith("_zhen")) return name.substring(0, name.length() - "_zhen".length());
        return name;
    }

    static String blockBaseName(String blockName) {
        return stripSuffix(ZhenLevel.baseName(blockName));
    }

    @Override
    public Identifier getPluginUid() { return PLUGIN_ID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 从 classpath 加载配方用于 JEI 展示（网络同步不可靠，classpath 确保始终有数据）
        ensureDisplayRecipes();

        if (CATEGORIES.isEmpty()) return;

        var helper = registration.getJeiHelpers().getGuiHelper();
        for (var entry : CATEGORIES.entrySet()) {
            registration.addRecipeCategories(
                    new ZhenRecipeCategory(helper, entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ensureDisplayRecipes();

        for (var entry : DISPLAY_RECIPES.entrySet()) {
            IRecipeType<ZhenRecipe> type = CATEGORIES.get(entry.getKey());
            if (type != null) {
                registration.addRecipes(type, List.of(entry.getValue()));
            }
        }
        MagicIO.LOGGER.info("[JEI] Registered {} zhen recipes", DISPLAY_RECIPES.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ensureDisplayRecipes();

        for (var blockEntry : ModBlocks.ZHEN_BLOCKS.entrySet()) {
            String blockName = blockEntry.getKey();
            String name = blockBaseName(blockName);

            if ("forge".equals(name)) {
                registration.addCraftingStation(RecipeTypes.SMELTING, blockEntry.getValue().get());
            } else {
                IRecipeType<ZhenRecipe> type = CATEGORIES.get(name);
                if (type != null) {
                    registration.addCraftingStation(type, blockEntry.getValue().get());
                }
            }
        }
    }

    // ====== 从 classpath 加载展示配方 ======

    private static void ensureDisplayRecipes() {
        if (!DISPLAY_RECIPES.isEmpty()) return;
        loadFromClasspath();
    }

    private static void loadFromClasspath() {
        try {
            var url = MagicIOJeiPlugin.class.getResource("/data/magic_io/recipe/unstable/cinder.json");
            if (url == null) return;

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
                    if (recipe != null) addDisplayRecipe(recipe);
                } catch (Exception ignored) {}
            }
            MagicIO.LOGGER.info("[JEI] Loaded {} display recipes from classpath", DISPLAY_RECIPES.size());
        } catch (Exception e) {
            MagicIO.LOGGER.error("[JEI] Failed to load display recipes", e);
        }
    }

    private static void addDisplayRecipe(ZhenRecipe recipe) {
        String name = baseName(recipe);
        if ("forge".equals(name)) return;
        CATEGORIES.putIfAbsent(name, type(name));
        DISPLAY_RECIPES.putIfAbsent(name, recipe);
    }
}

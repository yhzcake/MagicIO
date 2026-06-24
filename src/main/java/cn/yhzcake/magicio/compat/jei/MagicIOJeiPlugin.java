package cn.yhzcake.magicio.compat.jei;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.ModBlocks;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeLoader;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.*;

@JeiPlugin
public class MagicIOJeiPlugin implements IModPlugin {

    public static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "jei_plugin");

    /** baseName → IRecipeType */
    private static final Map<String, IRecipeType<ZhenRecipe>> CATEGORIES = new LinkedHashMap<>();

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
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ensureRecipesLoaded();
        buildCategories();

        var helper = registration.getJeiHelpers().getGuiHelper();
        for (var entry : CATEGORIES.entrySet()) {
            registration.addRecipeCategories(
                    new ZhenRecipeCategory(helper, entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ensureRecipesLoaded();
        buildCategories();

        // 按 baseName 分组，每个 baseName 只用一个配方展示
        Map<String, ZhenRecipe> unique = new LinkedHashMap<>();
        for (var recipe : ZhenRecipeManager.getInstance().getAllRecipes()) {
            String name = baseName(recipe);
            if ("forge".equals(name)) continue;
            unique.putIfAbsent(name, recipe);
        }

        for (var entry : unique.entrySet()) {
            IRecipeType<ZhenRecipe> type = CATEGORIES.get(entry.getKey());
            if (type != null) {
                registration.addRecipes(type, List.of(entry.getValue()));
            }
        }
        MagicIO.LOGGER.info("[JEI] Displayed {} zhen categories", unique.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ensureRecipesLoaded();
        buildCategories();

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

    // ====== 内部 ======

    private static boolean categoriesBuilt = false;

    private static void buildCategories() {
        if (categoriesBuilt) return;
        categoriesBuilt = true;

        for (var recipe : ZhenRecipeManager.getInstance().getAllRecipes()) {
            String name = baseName(recipe);
            if ("forge".equals(name)) continue;
            CATEGORIES.putIfAbsent(name, type(name));
        }
    }

    private static void ensureRecipesLoaded() {
        if (ZhenRecipeManager.getInstance().getRecipeCount() > 0) return;

        var mc = Minecraft.getInstance();
        if (mc == null) return;

        try {
            var resources = mc.getResourceManager().listResources(
                    "recipe",
                    path -> path.getPath().endsWith(".json") && path.getNamespace().equals(MagicIO.MOD_ID));
            for (var entry : resources.entrySet()) {
                try (var in = entry.getValue().open()) {
                    var recipe = ZhenRecipeLoader.loadRecipeFromJson(in, null);
                    if (recipe != null) ZhenRecipeManager.getInstance().addRecipe(recipe);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            MagicIO.LOGGER.error("[JEI] Failed to load recipes", e);
        }
    }
}

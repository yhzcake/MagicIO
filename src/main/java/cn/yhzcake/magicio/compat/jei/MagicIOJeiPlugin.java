package cn.yhzcake.magicio.compat.jei;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.ModBlocks;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.item.crafting.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;

import java.util.*;

@JeiPlugin
public class MagicIOJeiPlugin implements IModPlugin {

    public static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "jei_plugin");

    private static final Map<String, IRecipeType<ZhenRecipe>> CATEGORIES = new LinkedHashMap<>();
    private static boolean categoriesBuilt = false;
    private static IJeiRuntime jeiRuntime;

    /** classpath 上的配方路径（仅在 registerRecipes 时缓存为空时使用） */
    /* TEMP: 暂时注释，用于测试网络同步
    private static final String[] RECIPE_PATHS = {
        "data/magic_io/recipe/unstable/cinder.json",
        "data/magic_io/recipe/unstable/dew.json",
        "data/magic_io/recipe/unstable/sieve.json",
        "data/magic_io/recipe/unstable/zephyr.json",
        "data/magic_io/recipe/stable/carve.json",
        "data/magic_io/recipe/stable/compact.json",
        "data/magic_io/recipe/stable/frost.json",
        "data/magic_io/recipe/stable/grind.json",
        "data/magic_io/recipe/stable/potion.json",
        "data/magic_io/recipe/stable/sprout.json",
        "data/magic_io/recipe/stable/voltaic.json",
        "data/magic_io/recipe/sturdy/blaze.json",
        "data/magic_io/recipe/sturdy/ferment.json",
        "data/magic_io/recipe/sturdy/gem.json",
        "data/magic_io/recipe/sturdy/mold.json",
        "data/magic_io/recipe/sturdy/shift.json",
        "data/magic_io/recipe/sturdy/spring.json",
        "data/magic_io/recipe/sturdy/thunder.json",
        "data/magic_io/recipe/sturdy/whirl.json",
        "data/magic_io/recipe/abundant/conflux.json",
        "data/magic_io/recipe/abundant/distill.json",
        "data/magic_io/recipe/abundant/divine.json",
        "data/magic_io/recipe/abundant/engrave.json",
        "data/magic_io/recipe/abundant/gate.json",
        "data/magic_io/recipe/abundant/quake.json",
        "data/magic_io/recipe/abundant/symbiosis.json",
        "data/magic_io/recipe/abundant/synthesis.json",
        "data/magic_io/recipe/archaic/fate.json",
        "data/magic_io/recipe/archaic/foresight.json",
        "data/magic_io/recipe/archaic/haste.json",
        "data/magic_io/recipe/archaic/summon.json",
        "data/magic_io/recipe/archaic/transmute.json",
        "data/magic_io/recipe/archaic/void.json",
        "data/magic_io/recipe/archaic/weave.json",
        "data/magic_io/recipe/primeval/creative.json"
    };
    */

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

    private static void ensureCategories() {
        if (categoriesBuilt) return;
        categoriesBuilt = true;
        for (var blockEntry : ModBlocks.ZHEN_BLOCKS.entrySet()) {
            String name = blockBaseName(blockEntry.getKey());
            if ("forge".equals(name)) continue;
            CATEGORIES.putIfAbsent(name, type(name));
        }
    }

    @Override
    public Identifier getPluginUid() { return PLUGIN_ID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ensureCategories();
        if (CATEGORIES.isEmpty()) return;
        var helper = registration.getJeiHelpers().getGuiHelper();
        for (var entry : CATEGORIES.entrySet()) {
            registration.addRecipeCategories(new ZhenRecipeCategory(helper, entry.getKey(), entry.getValue()));
        }
        MagicIO.LOGGER.info("[JEI] Registered {} zhen categories", CATEGORIES.size());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ensureCategories();

        // 优先从缓存取（单机集成服务器已有数据）
        var cached = ZhenRecipeManager.getInstance().getAllRecipes();
        if (!cached.isEmpty()) {
            int count = registerFrom(cached, registration::addRecipes);
            if (count > 0) {
                MagicIO.LOGGER.info("[JEI] Registered {} zhen recipes from cache", count);
                return;
            }
        }

        // 缓存为空时不注册任何配方（测试网络同步用）
        MagicIO.LOGGER.info("[JEI] No cached recipes, waiting for server sync");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ensureCategories();
        for (var blockEntry : ModBlocks.ZHEN_BLOCKS.entrySet()) {
            String name = blockBaseName(blockEntry.getKey());
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

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        // 运行时就绪后也尝试刷新（覆盖网络包到达较早的情况）
        var all = ZhenRecipeManager.getInstance().getAllRecipes();
        if (!all.isEmpty()) refreshFromCache();
    }

    // ====== 运行时刷新 ======

    /** 服务端同步后调用：用服务端配方刷新 JEI 显示 */
    public static void refreshFromCache() {
        if (jeiRuntime == null) return;
        var all = ZhenRecipeManager.getInstance().getAllRecipes();
        if (all.isEmpty()) return;

        var recipeManager = jeiRuntime.getRecipeManager();
        // 先隐藏该类型下所有现有配方，再添加新的
        for (var type : CATEGORIES.values()) {
            var existing = recipeManager.createRecipeLookup(type).get().toList();
            recipeManager.hideRecipes(type, existing);
        }

        int count = registerFrom(all, recipeManager::addRecipes);
        MagicIO.LOGGER.info("[JEI] Refreshed {} zhen recipes from server sync", count);
    }

    // ====== 内部 ======

    @FunctionalInterface
    private interface RecipeAdder {
        void add(IRecipeType<ZhenRecipe> type, List<ZhenRecipe> recipes);
    }

    /** 从配方列表注册到目标接收器 */
    private static int registerFrom(List<ZhenRecipe> recipes, RecipeAdder adder) {
        Map<String, ZhenRecipe> unique = new LinkedHashMap<>();
        for (var recipe : recipes) {
            String name = baseName(recipe);
            if ("forge".equals(name)) continue;
            if (recipe.getFixedOutputs().isEmpty() && !hasLootOutput(recipe) && !hasFluidOutput(recipe)) continue;
            unique.putIfAbsent(name, recipe);
        }

        int count = 0;
        for (var entry : unique.entrySet()) {
            IRecipeType<ZhenRecipe> type = CATEGORIES.get(entry.getKey());
            if (type != null) {
                adder.add(type, List.of(entry.getValue()));
                count++;
            }
        }
        return count;
    }

    /** 从 classpath 加载并注册 */
    /* TEMP: 暂时注释，用于测试网络同步
    private static int loadAndRegisterFromClasspath(IRecipeRegistration registration) {
        Set<String> seenNames = new HashSet<>();
        int count = 0;

        for (String path : RECIPE_PATHS) {
            try (var in = MagicIOJeiPlugin.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) continue;
                var recipe = ZhenRecipeLoader.loadRecipeFromJson(in, null);
                if (recipe == null) continue;
                String name = baseName(recipe);
                if ("forge".equals(name) || !seenNames.add(name)) continue;
                IRecipeType<ZhenRecipe> type = CATEGORIES.get(name);
                if (type != null) {
                    registration.addRecipes(type, List.of(recipe));
                    count++;
                }
            } catch (Exception ignored) {}
        }
        return count;
    }
    */

    @SuppressWarnings("unchecked")
    private static boolean hasLootOutput(ZhenRecipe recipe) {
        for (var output : recipe.getOutputs()) {
            if (output.type() != ModIOTypes.ITEM.get()) continue;
            var entries = (NonNullList<OutputEntry>) output.specification();
            for (var entry : entries) if (entry.isLootTable()) return true;
        }
        return false;
    }

    private static boolean hasFluidOutput(ZhenRecipe recipe) {
        for (var output : recipe.getOutputs()) {
            if (output.type() == ModIOTypes.FLUID.get()) return true;
        }
        return false;
    }
}

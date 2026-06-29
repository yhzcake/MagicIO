package cn.yhzcake.magicio.compat.jei;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.ModBlocks;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

import java.util.*;

@JeiPlugin
public class MagicIOJeiPlugin implements IModPlugin {

    public static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "jei_plugin");

    /** baseName → IRecipeType */
    private static final Map<String, IRecipeType<ZhenRecipe>> CATEGORIES = new LinkedHashMap<>();
    private static boolean categoriesBuilt = false;
    private static IJeiRuntime jeiRuntime;

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

    /** 从方块列表推断所有分类，无需读取配方文件 */
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
            registration.addRecipeCategories(
                    new ZhenRecipeCategory(helper, entry.getKey(), entry.getValue()));
        }
        MagicIO.LOGGER.info("[JEI] Registered {} zhen categories (from blocks)", CATEGORIES.size());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ensureCategories();
        // 从缓存读取（单机时集成服务器可能已加载完毕）
        tryRegisterFromCache(registration);
        MagicIO.LOGGER.info("[JEI] Recipe registration complete (deferred to network sync)");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ensureCategories();

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

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        // 运行时就绪后立即从缓存拉取（覆盖网络包到达较晚的情况）
        var all = ZhenRecipeManager.getInstance().getAllRecipes();
        if (!all.isEmpty()) {
            refreshRecipes();
        }
    }

    // ====== 运行时刷新方法 ======

    /**
     * 从 ZhenRecipeManager 缓存中读取配方并动态注入 JEI。
     * 在网络同步数据包到达后调用。
     */
    public static void refreshRecipes() {
        if (jeiRuntime == null) {
            MagicIO.LOGGER.warn("[JEI] Runtime not available, cannot refresh");
            return;
        }
        var recipeManager = jeiRuntime.getRecipeManager();
        injectRecipes(recipeManager::addRecipes);
    }

    /** 在 registerRecipes 时从缓存读取 */
    private static void tryRegisterFromCache(IRecipeRegistration registration) {
        injectRecipes(registration::addRecipes);
    }

    /** 从缓存提取配方并注入目标接收器 */
    private static void injectRecipes(RecipeConsumer consumer) {
        var all = ZhenRecipeManager.getInstance().getAllRecipes();
        if (all.isEmpty()) return;

        Map<String, ZhenRecipe> unique = new LinkedHashMap<>();
        for (var recipe : all) {
            String name = baseName(recipe);
            if ("forge".equals(name)) continue;
            // 过滤全空的坏数据（注册表未就绪时加载的配方，所有输出都为空）
            if (recipe.getFixedOutputs().isEmpty() && !hasLootOutput(recipe) && !hasFluidOutput(recipe)) continue;
            unique.putIfAbsent(name, recipe);
        }
        if (unique.isEmpty()) return;

        int count = 0;
        for (var entry : unique.entrySet()) {
            IRecipeType<ZhenRecipe> type = CATEGORIES.get(entry.getKey());
            if (type != null) {
                consumer.accept(type, List.of(entry.getValue()));
                count++;
            }
        }
        MagicIO.LOGGER.info("[JEI] Injected {} zhen recipes from cache", count);
    }

    /** 检查配方是否有战利品表输出 */
    @SuppressWarnings("unchecked")
    private static boolean hasLootOutput(ZhenRecipe recipe) {
        for (var output : recipe.getOutputs()) {
            if (output.type() != cn.yhzcake.magicio.io.ModIOTypes.ITEM.get()) continue;
            var entries = (net.minecraft.core.NonNullList<cn.yhzcake.magicio.item.crafting.OutputEntry>) output.specification();
            for (var entry : entries) {
                if (entry.isLootTable()) return true;
            }
        }
        return false;
    }

    /** 检查配方是否有流体输出 */
    private static boolean hasFluidOutput(ZhenRecipe recipe) {
        for (var output : recipe.getOutputs()) {
            if (output.type() == cn.yhzcake.magicio.io.ModIOTypes.FLUID.get()) return true;
        }
        return false;
    }

    @FunctionalInterface
    private interface RecipeConsumer {
        void accept(IRecipeType<ZhenRecipe> type, List<ZhenRecipe> recipes);
    }
}

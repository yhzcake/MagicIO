package cn.yhzcake.magicio.item.crafting;

import java.util.ArrayList;
import java.util.List;

import cn.yhzcake.magicio.MagicIO;

/**
 * 配方倍率修饰器的集中注册表和计算入口。
 * <p>所有修饰器通过 {@link #register(RecipeModifier)} 注册，调用
 * {@link #getTimeMultiplier(String, ZhenRecipe)} 和
 * {@link #getOutputMultiplier(String, ZhenRecipe)} 计算叠加后的倍率。</p>
 */
public class RecipeModifiers {

    private static final List<RecipeModifier> MODIFIERS = new ArrayList<>();

    private RecipeModifiers() {}

    /** 注册一个修饰器，可随时调用（如插件初始化时） */
    public static void register(RecipeModifier modifier) {
        MODIFIERS.add(modifier);
        MagicIO.LOGGER.debug("[RecipeModifiers] 已注册修饰器: {}", modifier.getClass().getSimpleName());
    }

    /** 计算给定配方的最终时间倍率（所有修饰器乘法叠加） */
    public static double getTimeMultiplier(String zhenTypeId, ZhenRecipe recipe) {
        double m = 1.0;
        for (RecipeModifier mod : MODIFIERS) {
            m *= mod.timeMultiplier(zhenTypeId, recipe);
        }
        return m;
    }

    /** 计算给定配方的最终产出倍率（所有修饰器乘法叠加） */
    public static double getOutputMultiplier(String zhenTypeId, ZhenRecipe recipe) {
        double m = 1.0;
        for (RecipeModifier mod : MODIFIERS) {
            m *= mod.outputMultiplier(zhenTypeId, recipe);
        }
        return m;
    }

    /** 清空所有修饰器（用于 /reload 重置） */
    public static void clear() {
        MODIFIERS.clear();
    }
}

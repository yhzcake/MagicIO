package cn.yhzcake.magicio.item.crafting;

/**
 * 配方的倍率修饰器接口。
 * <p>每个倍率来源（等级系统、插件等）都实现此接口，通过 {@link RecipeModifiers#register(RecipeModifier)}
 * 注册后自动叠加生效。所有修饰器的倍率通过乘法叠加。</p>
 */
public interface RecipeModifier {

    /** 对 processingTime 的倍率（乘法叠加），1.0 表示无变化 */
    default double timeMultiplier(String zhenTypeId, ZhenRecipe recipe) {
        return 1.0;
    }

    /** 对产出数量的倍率（乘法叠加），1.0 表示无变化 */
    default double outputMultiplier(String zhenTypeId, ZhenRecipe recipe) {
        return 1.0;
    }
}

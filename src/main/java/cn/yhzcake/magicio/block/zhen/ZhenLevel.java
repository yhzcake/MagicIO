package cn.yhzcake.magicio.block.zhen;

import java.util.List;

import cn.yhzcake.magicio.item.crafting.RecipeModifier;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;

/**
 * 阵的等级定义。
 * <p>每个等级包含名称前缀、等级数值、以及<b>相对于前一等级</b>的速度倍率和产出倍率。
 * 等级的初始等级（如 0/10）是绝对数值，倍率是相对乘数。</p>
 */
public record ZhenLevel(String prefix, int level, double speedFactor, double outputFactor) implements RecipeModifier {

    public static final ZhenLevel UNSTABLE = new ZhenLevel("unstable_", 0, 1.0, 1.0);
    public static final ZhenLevel STABLE   = new ZhenLevel("stable_", 10, 0.75, 1.0);
    public static final ZhenLevel STURDY   = new ZhenLevel("sturdy_", 20, 0.75, 1.1);
    public static final ZhenLevel ABUNDANT = new ZhenLevel("abundant_", 30, 0.75, 1.1);
    public static final ZhenLevel ARCHAIC  = new ZhenLevel("archaic_", 40, 0.75, 1.2);
    public static final ZhenLevel PRIMEVAL = new ZhenLevel("primeval_", 50, 0.75, 1.2);

    public static final List<ZhenLevel> ALL = List.of(UNSTABLE, STABLE, STURDY, ABUNDANT, ARCHAIC, PRIMEVAL);

    /** 按等级数值查找 */
    public static ZhenLevel byLevel(int level) {
        for (var l : ALL) if (l.level() == level) return l;
        return null;
    }

    /** 按前缀查找，如 "unstable_" → UNSTABLE */
    public static ZhenLevel byPrefix(String prefix) {
        for (var l : ALL) if (l.prefix().equals(prefix)) return l;
        return null;
    }

    /** 从前缀中提取等级信息，如 "unstable_cinder" → UNSTABLE */
    public static ZhenLevel fromFullId(String fullId) {
        for (var l : ALL) if (fullId.startsWith(l.prefix())) return l;
        return null;
    }

    /** 从完整 ID 中剥离前缀得到基础名，如 "unstable_cinder" → "cinder" */
    public static String baseName(String fullId) {
        for (var l : ALL) {
            if (fullId.startsWith(l.prefix())) {
                return fullId.substring(l.prefix().length());
            }
        }
        return fullId;
    }

    /** 计算从 fromLevel 到 toLevel 的累计速度倍率 */
    public static double totalSpeedMultiplier(int fromLevel, int toLevel) {
        if (toLevel <= fromLevel) return 1.0;
        double m = 1.0;
        boolean started = false;
        for (var l : ALL) {
            if (l.level() == fromLevel) started = true;
            if (started && l.level() > fromLevel && l.level() <= toLevel) {
                m *= l.speedFactor();
            }
        }
        return m;
    }

    /** 计算从 fromLevel 到 toLevel 的累计产出倍率 */
    public static double totalOutputMultiplier(int fromLevel, int toLevel) {
        if (toLevel <= fromLevel) return 1.0;
        double m = 1.0;
        boolean started = false;
        for (var l : ALL) {
            if (l.level() == fromLevel) started = true;
            if (started && l.level() > fromLevel && l.level() <= toLevel) {
                m *= l.outputFactor();
            }
        }
        return m;
    }

    // ===== RecipeModifier 实现 =====
    // 等级系统作为一个整体修饰器注册到 RecipeModifiers，
    // 在配方展开时通过 totalSpeedMultiplier / totalOutputMultiplier 计算倍率。

    @Override
    public double timeMultiplier(String zhenTypeId, ZhenRecipe recipe) {
        // 此方法在 RecipeModifiers 中不会直接使用，
        // 配方展开时直接调用 ZhenLevel.totalSpeedMultiplier()
        return 1.0;
    }

    @Override
    public double outputMultiplier(String zhenTypeId, ZhenRecipe recipe) {
        return 1.0;
    }
}

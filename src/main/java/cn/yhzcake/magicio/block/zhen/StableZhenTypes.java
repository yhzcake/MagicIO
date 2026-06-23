package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.utils.ElementTypes;
import net.neoforged.bus.api.IEventBus;

/**
 * 等级 10 — 稳定（Stable）
 */
public class StableZhenTypes {

    private final static int level = 10;

    public static Supplier<ZhenType> STABLE_FORGE_ZHEN;
    public static Supplier<ZhenType> STABLE_COMPACT_ZHEN;
    public static Supplier<ZhenType> STABLE_POTION_ZHEN;
    public static Supplier<ZhenType> STABLE_CARVE_ZHEN;
    public static Supplier<ZhenType> STABLE_SPROUT_ZHEN;
    public static Supplier<ZhenType> STABLE_GRIND_ZHEN;
    public static Supplier<ZhenType> STABLE_FROST_ZHEN;
    public static Supplier<ZhenType> STABLE_VOLTAIC_ZHEN;

    public static void register(IEventBus eventBus) {
        STABLE_FORGE_ZHEN = ZhenTypes.registerSimple("stable_forge_zhen", ElementTypes.FIRE, level);
        STABLE_COMPACT_ZHEN = ZhenTypes.registerSimple("stable_compact_zhen", ElementTypes.EARTH, level);
        STABLE_POTION_ZHEN = ZhenTypes.registerSimple("stable_potion_zhen", ElementTypes.WATER, level);
        STABLE_CARVE_ZHEN = ZhenTypes.registerSimple("stable_carve_zhen", ElementTypes.WIND, level);
        STABLE_SPROUT_ZHEN = ZhenTypes.registerSimple("stable_sprout_zhen", ElementTypes.WOOD, level);
        STABLE_GRIND_ZHEN = ZhenTypes.registerSimple("stable_grind_zhen", ElementTypes.METAL, level);
        STABLE_FROST_ZHEN = ZhenTypes.registerSimple("stable_frost_zhen", ElementTypes.ICE, level);
        STABLE_VOLTAIC_ZHEN = ZhenTypes.registerSimple("stable_voltaic_zhen", ElementTypes.LIGHTNING, level);
    }
}

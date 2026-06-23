package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.utils.ElementTypes;
import net.neoforged.bus.api.IEventBus;

/**
 * 等级 20 — 稳固（Sturdy）
 */
public class SturdyZhenTypes {

    private final static int level = 20;

    public static Supplier<ZhenType> STURDY_BLAZE_ZHEN;
    public static Supplier<ZhenType> STURDY_GEM_ZHEN;
    public static Supplier<ZhenType> STURDY_SPRING_ZHEN;
    public static Supplier<ZhenType> STURDY_WHIRL_ZHEN;
    public static Supplier<ZhenType> STURDY_FERMENT_ZHEN;
    public static Supplier<ZhenType> STURDY_MOLD_ZHEN;
    public static Supplier<ZhenType> STURDY_SHIFT_ZHEN;
    public static Supplier<ZhenType> STURDY_THUNDER_ZHEN;

    public static void register(IEventBus eventBus) {
        STURDY_BLAZE_ZHEN = ZhenTypes.registerSimple("sturdy_blaze_zhen", ElementTypes.FIRE, level);
        STURDY_GEM_ZHEN = ZhenTypes.registerSimple("sturdy_gem_zhen", ElementTypes.EARTH, level);
        STURDY_SPRING_ZHEN = ZhenTypes.registerSimple("sturdy_spring_zhen", ElementTypes.WATER, level);
        STURDY_WHIRL_ZHEN = ZhenTypes.registerSimple("sturdy_whirl_zhen", ElementTypes.WIND, level);
        STURDY_FERMENT_ZHEN = ZhenTypes.registerSimple("sturdy_ferment_zhen", ElementTypes.WOOD, level);
        STURDY_MOLD_ZHEN = ZhenTypes.registerSimple("sturdy_mold_zhen", ElementTypes.METAL, level);
        STURDY_SHIFT_ZHEN = ZhenTypes.registerSimple("sturdy_shift_zhen", ElementTypes.ICE, level);
        STURDY_THUNDER_ZHEN = ZhenTypes.registerSimple("sturdy_thunder_zhen", ElementTypes.LIGHTNING, level);
    }
}

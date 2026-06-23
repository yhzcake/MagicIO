package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.utils.ElementTypes;
import net.neoforged.bus.api.IEventBus;

/**
 * 等级 30 — 充盈（Abundant）
 */
public class AbundantZhenTypes {

    private final static int level = 30;

    public static Supplier<ZhenType> ABUNDANT_ENGRAVE_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_QUAKE_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_SYMBIOSIS_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_CONFLUX_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_SYNTHESIS_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_DISTILL_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_GATE_ZHEN;
    public static Supplier<ZhenType> ABUNDANT_DIVINE_ZHEN;

    public static void register(IEventBus eventBus) {
        ABUNDANT_ENGRAVE_ZHEN = ZhenTypes.registerSimple("abundant_engrave_zhen", ElementTypes.FIRE, level);
        ABUNDANT_QUAKE_ZHEN = ZhenTypes.registerSimple("abundant_quake_zhen", ElementTypes.EARTH, level);
        ABUNDANT_SYMBIOSIS_ZHEN = ZhenTypes.registerSimple("abundant_symbiosis_zhen", ElementTypes.WOOD, level);
        ABUNDANT_CONFLUX_ZHEN = ZhenTypes.registerSimple("abundant_conflux_zhen", ElementTypes.METAL, level);
        ABUNDANT_SYNTHESIS_ZHEN = ZhenTypes.registerSimple("abundant_synthesis_zhen", ElementTypes.ORDER, level);
        ABUNDANT_DISTILL_ZHEN = ZhenTypes.registerSimple("abundant_distill_zhen", ElementTypes.CHAOS, level);
        ABUNDANT_GATE_ZHEN = ZhenTypes.registerSimple("abundant_gate_zhen", ElementTypes.SPACE, level);
        ABUNDANT_DIVINE_ZHEN = ZhenTypes.registerSimple("abundant_divine_zhen", ElementTypes.DESCRIPTION, level);
    }
}

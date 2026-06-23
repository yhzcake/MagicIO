package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.utils.ElementTypes;
import net.neoforged.bus.api.IEventBus;

/**
 * 等级 40 — 古朴（Archaic）
 */
public class ArchaicZhenTypes {

    private final static int level = 40;

    public static Supplier<ZhenType> ARCHAIC_WEAVE_ZHEN;
    public static Supplier<ZhenType> ARCHAIC_FATE_ZHEN;
    public static Supplier<ZhenType> ARCHAIC_VOID_ZHEN;
    public static Supplier<ZhenType> ARCHAIC_HASTE_ZHEN;
    public static Supplier<ZhenType> ARCHAIC_TRANSMUTE_ZHEN;
    public static Supplier<ZhenType> ARCHAIC_FORESIGHT_ZHEN;
    public static Supplier<ZhenType> ARCHAIC_SUMMON_ZHEN;

    public static void register(IEventBus eventBus) {
        ARCHAIC_WEAVE_ZHEN = ZhenTypes.registerSimple("archaic_weave_zhen", ElementTypes.ORDER, level);
        ARCHAIC_FATE_ZHEN = ZhenTypes.registerSimple("archaic_fate_zhen", ElementTypes.CHAOS, level);
        ARCHAIC_VOID_ZHEN = ZhenTypes.registerSimple("archaic_void_zhen", ElementTypes.SPACE, level);
        ARCHAIC_HASTE_ZHEN = ZhenTypes.registerSimple("archaic_haste_zhen", ElementTypes.TIME, level);
        ARCHAIC_TRANSMUTE_ZHEN = ZhenTypes.registerSimple("archaic_transmute_zhen", ElementTypes.ENERGY, level);
        ARCHAIC_FORESIGHT_ZHEN = ZhenTypes.registerSimple("archaic_foresight_zhen", ElementTypes.DESCRIPTION, level);
        ARCHAIC_SUMMON_ZHEN = ZhenTypes.registerSimple("archaic_summon_zhen", ElementTypes.CONSCIOUSNESS, level);
    }
}

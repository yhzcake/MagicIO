package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.utils.ElementTypes;
import net.neoforged.bus.api.IEventBus;

/**
 * 等级 50 — 远古（Primeval）
 */
public class PrimevalZhenTypes {

    private final static int level = 50;

    public static Supplier<ZhenType> PRIMEVAL_CREATIVE_ZHEN;

    public static void register(IEventBus eventBus) {
        PRIMEVAL_CREATIVE_ZHEN = ZhenTypes.registerSimple("primeval_creative_zhen", ElementTypes.CREATIVE, level);
    }
}

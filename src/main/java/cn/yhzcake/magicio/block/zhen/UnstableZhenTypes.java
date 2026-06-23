package cn.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;

import cn.yhzcake.magicio.block.entity.method.UnstableDewMethod;
import cn.yhzcake.magicio.block.entity.method.UnstableSiftMethod;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.utils.ElementTypes;
import net.neoforged.bus.api.IEventBus;

/**
 * 等级 0 — 不稳定（Unstable）
 */
public class UnstableZhenTypes {

    private final static int level = 0;

    public static Supplier<ZhenType> UNSTABLE_CINDER_ZHEN;
    public static Supplier<ZhenType> UNSTABLE_SIEVE_ZHEN;
    public static Supplier<ZhenType> UNSTABLE_DEW_ZHEN;
    public static Supplier<ZhenType> UNSTABLE_ZEPHYR_ZHEN;

    public static void register(IEventBus eventBus) {
        UNSTABLE_CINDER_ZHEN = ZhenTypes.registerSimple("unstable_cinder_zhen", ElementTypes.FIRE, level);

        UNSTABLE_SIEVE_ZHEN = ZhenTypes.ZHEN_TYPES.register("unstable_sieve_zhen",
                () -> new ZhenType(ElementTypes.EARTH.get(), "magic_io:unstable_sieve_zhen",
                        SlotPartition.of(ModIOTypes.ITEM.get(), ZhenTypes.defaultItemSlots()), level,
                        (ctx) -> () -> {
                            UnstableSiftMethod m = new UnstableSiftMethod(ctx.level(), ctx.pos(), ctx.state(), ctx.blockEntity());
                            m.unstable_sift_tick();
                        }));

        UNSTABLE_DEW_ZHEN = ZhenTypes.ZHEN_TYPES.register("unstable_dew_zhen",
                () -> new ZhenType(ElementTypes.WATER.get(), "magic_io:unstable_dew_zhen",
                        SlotPartition.of(java.util.Map.of(
                            ModIOTypes.ITEM.get(), ZhenTypes.defaultItemSlots(),
                            ModIOTypes.FLUID.get(), java.util.Map.of(SlotZone.FLUID_OUTPUT_ALL, ZhenTypes.range(0, 0))
                        )), level,
                        (ctx) -> () -> {
                            UnstableDewMethod m = new UnstableDewMethod(ctx.level(), ctx.pos(), ctx.state(), ctx.blockEntity());
                            m.unstable_dew_tick();
                        }, 1000));

        UNSTABLE_ZEPHYR_ZHEN = ZhenTypes.registerSimple("unstable_zephyr_zhen", ElementTypes.WIND, level);
    }
}

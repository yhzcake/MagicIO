package com.yhzcake.magicio.block.zhen;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.method.SmallDewMethod;
import com.yhzcake.magicio.block.entity.method.SmallSiftMethod;
import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.io.ModIOTypes;
import com.yhzcake.magicio.utils.ElementTypes;

import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZhenTypes {
    public static final DeferredRegister<ZhenType> ZHEN_TYPES = DeferredRegister.create(ZhenType.ZHEN_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
    public static Supplier<ZhenType> SMALL_SIFT_ZHEN;
    public static Supplier<ZhenType> SMALL_DEW_ZHEN;

    public static Set<Integer> range(int start, int end) {
        return IntStream.range(start, end + 1).boxed().collect(Collectors.toSet());
    }

    public static void register(IEventBus eventBus) {
        SMALL_SIFT_ZHEN = ZHEN_TYPES.register("small_sift_zhen",
                () -> new ZhenType(ElementTypes.EARTH.get(), "magic_io:small_sift_zhen",
                        SlotPartition.of(ModIOTypes.ITEM.get(), Map.of(
                                SlotZone.ITEM_INPUT_ALL, range(0, 0),
                                SlotZone.ITEM_OUTPUT_ALL, range(1, 1)
                        )),
                        0,
                        (ctx) -> () -> {
                            SmallSiftMethod m = new SmallSiftMethod(ctx.level(), ctx.pos(), ctx.state(), ctx.blockEntity());
                            m.small_sift_tick();
                        }));
        
        SMALL_DEW_ZHEN = ZHEN_TYPES.register("small_dew_zhen",
                () -> new ZhenType(ElementTypes.WATER.get(), "magic_io:small_dew_zhen",
                        SlotPartition.of(
                            Map.of(
                                ModIOTypes.ITEM.get(),Map.of(
                                    // for bucket
                                    SlotZone.ITEM_INPUT_ALL, range(0, 0),
                                    SlotZone.ITEM_OUTPUT_ALL, range(1, 1)
                                ),
                                ModIOTypes.FLUID.get(), Map.of(
                                    SlotZone.FLUID_ALL, range(0, 0)
                                )
                            )
                        ),
                        0,
                        (ctx) -> () -> {
                            SmallDewMethod m = new SmallDewMethod(ctx.level(), ctx.pos(), ctx.state(), ctx.blockEntity());
                            m.small_dew_tick();
                        }, 1000));
        
        ZHEN_TYPES.register(eventBus);
    }

    public static ZhenType getType(String name) {
        if (name == null || name.isEmpty()) {
            return SMALL_SIFT_ZHEN.get();
        }

        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }

        try {
            return ZhenType.ZHEN_TYPES.get(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, name))
                    .map(Reference::value).orElse(SMALL_SIFT_ZHEN.get());
        } catch (Exception e) {
            return SMALL_SIFT_ZHEN.get();
        }
    }

    public static ZhenType getType(Identifier location) {
        if (location == null) {
            return SMALL_SIFT_ZHEN.get();
        }

        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }

        try {
            return ZhenType.ZHEN_TYPES.get(location)
                    .map(Reference::value).orElse(SMALL_SIFT_ZHEN.get());
        } catch (Exception e) {
            return SMALL_SIFT_ZHEN.get();
        }
    }

    public static ZhenType getType() {
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }

        try {
            return ZhenType.ZHEN_TYPES.get(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "small_sift_zhen"))
                    .map(Reference::value).orElse(SMALL_SIFT_ZHEN.get());
        } catch (Exception e) {
            return SMALL_SIFT_ZHEN.get();
        }
    }
}

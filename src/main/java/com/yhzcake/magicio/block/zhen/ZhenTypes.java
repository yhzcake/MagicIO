package com.yhzcake.magicio.block.zhen;

import com.yhzcake.magicio.MagicIO;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ZhenTypes {
    public static final DeferredRegister<ZhenType<? extends ElementType>> ZHEN_TYPES = DeferredRegister.create(ZhenType.ZHEN_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
    public static Supplier<ZhenType<ElementType>> SMALL_SIFT_ZHEN;

    public static void register(IEventBus eventBus) {
        // 初始化Supplier，确保在注册表被注册后再创建对象
        SMALL_SIFT_ZHEN = ZHEN_TYPES.register("small_sift_zhen",
                () -> new ZhenType<>(ElementType.EARTH, "sift", 1, 2, 0, 
                    (method) -> () -> method.small_sift_tick()));
        ZHEN_TYPES.register(eventBus);
    }
}
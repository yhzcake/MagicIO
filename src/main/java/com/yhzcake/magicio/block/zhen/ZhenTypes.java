package com.yhzcake.magicio.block.zhen;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.utils.ElementTypes;

import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZhenTypes {
    public static final DeferredRegister<ZhenType> ZHEN_TYPES = DeferredRegister.create(ZhenType.ZHEN_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
    public static Supplier<ZhenType> SMALL_SIFT_ZHEN;

    public static Set<Integer> range (int start, int end) {
        return IntStream.range(start, end+1).boxed().collect(Collectors.toSet());
    }

    public static void register(IEventBus eventBus) {
        SMALL_SIFT_ZHEN = ZHEN_TYPES.register("small_sift_zhen",
                () -> new ZhenType(ElementTypes.EARTH.get(), "small_sift_zhen",
                        new SlotPartition(Map.of(
                                SlotZone.INPUT_ALL, range(0, 0),
                                SlotZone.OUTPUT_ALL, range(1, 1)
                        )),
                        0,
                    (method) -> () -> method.small_sift_tick()));
        ZHEN_TYPES.register(eventBus);
    }

    public static ZhenType getType(String name){
        // 添加空值检查
        if (name == null || name.isEmpty()) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        // 添加注册表空值检查
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        try {
            return ZhenType.ZHEN_TYPES.get(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, name))
                    .map(Reference::value).orElse(SMALL_SIFT_ZHEN.get());
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return SMALL_SIFT_ZHEN.get();
        }
    }

    public static ZhenType getType(Identifier location){
        // 添加空值检查
        if (location == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        // 添加注册表空值检查
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        try {
            return ZhenType.ZHEN_TYPES.get(location)
                    .map(Reference::value).orElse(SMALL_SIFT_ZHEN.get());
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return SMALL_SIFT_ZHEN.get();
        }
    }

    public static ZhenType getType(){
        // 添加注册表空值检查
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        try {
            return ZhenType.ZHEN_TYPES.get(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "small_sift_zhen"))
                    .map(Reference::value).orElse(SMALL_SIFT_ZHEN.get());
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return SMALL_SIFT_ZHEN.get();
        }
    }
}

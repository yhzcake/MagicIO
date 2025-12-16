package com.yhzcake.magicio.block.zhen;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ZhenTypes {
    public static final DeferredRegister<ZhenType<? extends ElementType>> ZHEN_TYPES = DeferredRegister.create(ZhenType.ZHEN_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
    public static Supplier<ZhenType<ElementType>> SMALL_SIFT_ZHEN;

    public static void register(IEventBus eventBus) {
        // 初始化Supplier，确保在注册表被注册后再创建对象
        SMALL_SIFT_ZHEN = ZHEN_TYPES.register("small_sift_zhen",
                () -> new ZhenType<>(ElementType.EARTH, "small_sift_zhen", 1, 2, 0,
                    (method) -> () -> method.small_sift_tick()));
        ZHEN_TYPES.register(eventBus);
    }

    public static ZhenType<?> getType(String name){
        // 添加空值检查
        if (name == null || name.isEmpty()) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        // 添加注册表空值检查
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        try {
            return ZhenType.ZHEN_TYPES.get(ResourceLocation.fromNamespaceAndPath(MagicIO.MOD_ID, name));
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return SMALL_SIFT_ZHEN.get();
        }
    }

    public static ZhenType<?> getType(ResourceLocation location){
        // 添加空值检查
        if (location == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        // 添加注册表空值检查
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        try {
            return ZhenType.ZHEN_TYPES.get(location);
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return SMALL_SIFT_ZHEN.get();
        }
    }

    public static ZhenType<?> getType(){
        // 添加注册表空值检查
        if (ZhenType.ZHEN_TYPES == null) {
            return SMALL_SIFT_ZHEN.get();
        }
        
        try {
            return ZhenType.ZHEN_TYPES.get(ResourceLocation.fromNamespaceAndPath(MagicIO.MOD_ID, "small_sift_zhen"));
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return SMALL_SIFT_ZHEN.get();
        }
    }
}
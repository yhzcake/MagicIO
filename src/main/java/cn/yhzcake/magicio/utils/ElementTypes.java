package cn.yhzcake.magicio.utils;

import java.util.function.Supplier;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ElementTypes {
    public static final DeferredRegister<ElementType> ELEMENT_TYPES = DeferredRegister.create(ElementType.ELEMENT_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);
    public static Supplier<ElementType> EARTH;
    public static Supplier<ElementType> WATER;
    public static Supplier<ElementType> WIND;
    public static Supplier<ElementType> FIRE;
    public static Supplier<ElementType> WOOD;
    public static Supplier<ElementType> ICE;
    public static Supplier<ElementType> METAL;
    public static Supplier<ElementType> LIGHTNING;
    public static Supplier<ElementType> ORDER;
    public static Supplier<ElementType> SPACE;
    public static Supplier<ElementType> TIME;
    public static Supplier<ElementType> CHAOS;
    public static Supplier<ElementType> DESCRIPTION;
    public static Supplier<ElementType> ENERGY;
    public static Supplier<ElementType> CONSCIOUSNESS;
    public static Supplier<ElementType> CREATIVE;

    public static void register(IEventBus eventBus){
        EARTH = ELEMENT_TYPES.register("earth", () -> new ElementType("earth"));
        WATER = ELEMENT_TYPES.register("water", () -> new ElementType("water"));
        WIND = ELEMENT_TYPES.register("wind", () -> new ElementType("wind"));
        FIRE = ELEMENT_TYPES.register("fire", () -> new ElementType("fire"));
        WOOD = ELEMENT_TYPES.register("wood", () -> new ElementType("wood", new ElementType[]{EARTH.get(),WATER.get()}));
        ICE = ELEMENT_TYPES.register("ice", () -> new ElementType("ice", new ElementType[]{WATER.get(),WIND.get()}));
        METAL = ELEMENT_TYPES.register("metal", () -> new ElementType("metal", new ElementType[]{FIRE.get(),EARTH.get()}));
        LIGHTNING = ELEMENT_TYPES.register("lightning", () -> new ElementType("lightning", new ElementType[]{WIND.get(),FIRE.get()}));
        ORDER = ELEMENT_TYPES.register("order", () -> new ElementType("order", new ElementType[]{WOOD.get(),METAL.get()}));
        SPACE = ELEMENT_TYPES.register("space", () -> new ElementType("space", new ElementType[]{ICE.get(),WOOD.get()}));
        TIME = ELEMENT_TYPES.register("time", () -> new ElementType("time", new ElementType[]{METAL.get(),LIGHTNING.get()}));
        CHAOS = ELEMENT_TYPES.register("chaos", () -> new ElementType("chaos", new ElementType[]{LIGHTNING.get(),ICE.get()}));
        DESCRIPTION = ELEMENT_TYPES.register("description", () -> new ElementType("description", new ElementType[]{CHAOS.get(),ORDER.get()}));
        ENERGY = ELEMENT_TYPES.register("energy", () -> new ElementType("energy", new ElementType[]{SPACE.get(),TIME.get()}));
        CONSCIOUSNESS = ELEMENT_TYPES.register("consciousness", () -> new ElementType("consciousness", new ElementType[]{LIGHTNING.get(),EARTH.get(),ENERGY.get()}));
        CREATIVE = ELEMENT_TYPES.register("creative", () -> new ElementType("creative", new ElementType[]{DESCRIPTION.get(),ENERGY.get(),CONSCIOUSNESS.get()}));
        ELEMENT_TYPES.register(eventBus);
    }

    public static ElementType getType(String name){
        // 添加空值检查
        if (name == null || name.isEmpty()) {
            return EARTH.get();
        }
        
        // 添加注册表空值检查
        if (ELEMENT_TYPES == null) {
            return EARTH.get();
        }
        
        try {
            return ElementType.ELEMENT_TYPES.get(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, name))
                    .map(Reference::value).orElse(EARTH.get());
        } catch (Exception e) {
            // 如果出现异常，回退到默认类型
            return EARTH.get();
        }
    }

    public static ElementType getType(Identifier location){
        // 添加空值检查
        if (location == null) {
            return EARTH.get();
        }
        
        // 添加注册表空值检查
        if (ELEMENT_TYPES == null) {
            return EARTH.get();
        }
        
        try {
            return ElementType.ELEMENT_TYPES.get(location)
                    .map(Reference::value).orElse(EARTH.get());
        } catch (Exception e) {
            return EARTH.get();
        }
    }
}

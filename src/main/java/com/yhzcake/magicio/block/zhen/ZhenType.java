package com.yhzcake.magicio.block.zhen;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Objects;
import java.util.function.Function;

public class ZhenType<T extends ElementType> {

    public static final ResourceKey<Registry<ZhenType<? extends ElementType>>> ZHEN_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MagicIO.MOD_ID,"zhen_type"));
    public static Registry<ZhenType<? extends ElementType>> ZHEN_TYPE_REGISTRY;

    private final T element_id;
    private final String type;
    private final int input;
    private final int output;
    private final int level;
    private final Function<ZhenMethod, Runnable> tickFactory;

    public ZhenType(T element_id,String type,int input, int output,int level,Function<ZhenMethod, Runnable> tickFactory){
        this.element_id = Objects.requireNonNull(element_id);
        this.type = Objects.requireNonNull(type);
        this.input = input;
        this.output = output;
        this.level = level;
        this.tickFactory = tickFactory;
    }

    public T getElement_id() {
        return element_id;
    }
    public String getType() {
        return type;
    }
    public int getInput() {
        return input;
    }
    public int getOutput() {
        return output;
    }
    public int getLevel() {
        return level;
    }
    public Function<ZhenMethod, Runnable> getTickFactory() {
        return tickFactory;
    }
    public void executeMethod(ZhenMethod method) {
        Runnable runnable = this.tickFactory.apply(method);
        runnable.run();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ZhenType<?> zhenType = (ZhenType<?>) obj;
        return Objects.equals(type, zhenType.type) && Objects.equals(element_id, zhenType.element_id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(type, element_id);
    }
    @Override
    public String toString() {
        return type;
    }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        ZHEN_TYPE_REGISTRY = new RegistryBuilder<>(ZHEN_TYPE_REGISTRY_KEY)
                .defaultKey(ResourceLocation.fromNamespaceAndPath(MagicIO.MOD_ID, "small_soil_zhen"))
                .create();
        event.register(ZHEN_TYPE_REGISTRY);
    }
}
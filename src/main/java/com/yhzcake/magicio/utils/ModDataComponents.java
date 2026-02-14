package com.yhzcake.magicio.utils;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENT = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MagicIO.MOD_ID);
    
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> ZHEN_CONTENTS =
            COMPONENT.register("zhen_contents",()->
                    DataComponentType.<ItemContainerContents>builder()
                            .persistent(ItemContainerContents.CODEC)
                            .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                            .cacheEncoding()
                            .build());
                            
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> ZHEN_TYPE =
            COMPONENT.register("zhen_type",()->
                    DataComponentType.<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MARKER =
            COMPONENT.register("marker",()->
                    DataComponentType.<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static void register(IEventBus bus){
        COMPONENT.register(bus);
    }

}

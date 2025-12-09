package com.yhzcake.magicio.block.entity;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZhenBlockEntity>> ZHEN_BLOCK =
            BLOCK_ENTITIES.register("zhen_block", new Supplier<BlockEntityType<ZhenBlockEntity>>() {
                @Override
                public BlockEntityType<ZhenBlockEntity> get() {
                    return BlockEntityType.Builder.of(ZhenBlockEntity::new,
                            ModBlocks.ZHEN_BLOCK.get()).build(null);
                }
            });

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
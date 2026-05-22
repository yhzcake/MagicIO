package com.yhzcake.magicio.block.zhenbus;

import java.util.function.Supplier;

import com.yhzcake.magicio.MagicIO;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModZhenBusBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MagicIO.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);

    public static final DeferredBlock<Block> ZHEN_BUS =
            BLOCKS.registerBlock("zhen_bus",
                    p -> new ZhenBusBlock(p.noOcclusion().noCollision()));

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZhenBusBlockEntity>> ZHEN_BUS_BE =
            (DeferredHolder) BLOCK_ENTITIES.register("zhen_bus",
                    (Supplier) () -> new BlockEntityType(ZhenBusBlockEntity::new,
                            java.util.Set.of(ZHEN_BUS.get())));
}

package com.yhzcake.magicio.block.entity;

import java.util.function.Supplier;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.ModBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,MagicIO.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZhenBlockEntity>> ZHEN_BLOCK =
            BLOCK_ENTITIES.register("zhen_block", new Supplier<BlockEntityType<ZhenBlockEntity>>() {
                 @Override
                 public BlockEntityType<ZhenBlockEntity> get() {
                    return new BlockEntityType<>(
                            ZhenBlockEntity::new,
                            ModBlocks.getZhenBlockList().toArray(new Block[0]));
                }
             });
    
}

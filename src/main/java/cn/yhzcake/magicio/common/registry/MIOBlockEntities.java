package cn.yhzcake.magicio.common.registry;

import java.util.function.Supplier;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.block.entity.MatrixBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MIOBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MatrixBlockEntity>> MATRIX_BLOCK = BLOCK_ENTITIES.register("matrix_block", new Supplier<BlockEntityType<MatrixBlockEntity>>() {
        @Override
        public BlockEntityType<MatrixBlockEntity> get() {
            return new BlockEntityType<>(MatrixBlockEntity::new, MIOCommonBlocks.MATRIX_BLOCK.value());
        }
    });

    
}

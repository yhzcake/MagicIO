package cn.yhzcake.magicio.common.registry;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.block.MatrixBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public class MIOCommonBlocks {
    public static void touch() {
        // 子注册表需要touch以防在注册生命周期结束后才被访问
    }
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagicIO.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);


    public static final DeferredBlock<@NotNull MatrixBlock> MATRIX_BLOCK = BLOCKS.registerBlock("matrix_block", MatrixBlock::new);
    public static final DeferredItem<@NotNull BlockItem> MATRIX_BLOCK_ITEM = MIOCommonItems.ITEMS.registerSimpleBlockItem(MATRIX_BLOCK);
}

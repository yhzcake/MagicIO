package asia.yhzcake.magicio.block;

import asia.yhzcake.magicio.MagicIO;
import asia.yhzcake.magicio.block.blocks.ZhenBlock;
import asia.yhzcake.magicio.block.blocks.ZhenBlockEntity;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Blocks {
  // 创建方块注册器
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks(MagicIO.MOD_ID);
  // BLOCKS is a DeferredRegister.Blocks
  // 注册阵法方块
  public static final DeferredBlock<ZhenBlock> ZHEN_BLOCK =
      BLOCKS.register(
          "zhen_block",
          () ->
              new ZhenBlock(
                  BlockBehaviour.Properties.of()
                      .destroyTime(2.0f)
                      .explosionResistance(10.0f)
                      .sound(SoundType.GRAVEL)
                      .lightLevel(state -> 7)));
  // 创造阵法方块实体注册器
  public static final DeferredRegister<BlockEntityType<?>> ZHEN_ENTITY =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);
  // 注册阵法方块实体
  public static final Supplier<BlockEntityType<ZhenBlockEntity>> ZHEN_BLOCK_ENTITY =
      ZHEN_ENTITY.register(
          "zhen_block_entity",
          // The block entity type, created using a builder.
          () ->
              BlockEntityType.Builder.of(
                      // The supplier to use for constructing the block entity instances.
                      ZhenBlockEntity::new, ZHEN_BLOCK.get()
                      // A vararg of blocks that can have this block entity.
                      // This assumes the existence of the referenced blocks as
                      // DeferredBlock<Block>s.
                      )
                  // Build using null; vanilla does some datafixer shenanigans with the parameter
                  // that we don't need.
                  .build(null));
}

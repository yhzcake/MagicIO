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
  public static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks(MagicIO.MOD_ID);
  // BLOCKS is a DeferredRegister.Blocks
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
  public static final DeferredRegister<BlockEntityType<?>> ZHEN_ENTITY =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagicIO.MOD_ID);
  public static final Supplier<BlockEntityType<ZhenBlockEntity>> ZHEN_ENTITY_BLOCK =
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

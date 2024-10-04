package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.ZhenMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ZhenBlockEntity extends BlockEntity {
  private String zhen_type = "wind";

  public ZhenBlockEntity(BlockPos Pos, BlockState BlockState) {
    super(Blocks.ZHEN_ENTITY_BLOCK.get(), Pos, BlockState);
  }

  public static void tick(Level level, BlockPos blockPos, BlockState blockState, BlockEntity be) {
    ZhenBlockEntity blockEntity = (ZhenBlockEntity) be;
    ZhenMethod.ZHENMETHOD.executeMethod(blockEntity.zhen_type);
  }

  @Override
  public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
    super.loadAdditional(tag, registries);
    this.zhen_type = tag.getString("type");
  }

  @Override
  public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putString("type", this.zhen_type);
  }
}

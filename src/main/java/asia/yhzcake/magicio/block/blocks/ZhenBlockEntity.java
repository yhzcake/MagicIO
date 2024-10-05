package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.ZhenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ZhenBlockEntity extends BlockEntity {
  // 初始化阵法类型,默认为风阵
  private String zhen_type = "wind";

  public ZhenBlockEntity(BlockPos Pos, BlockState BlockState) {
    super(Blocks.ZHEN_BLOCK_ENTITY.get(), Pos, BlockState);
  }

  // 阵法tick调用,根据阵法类型调用相应的阵法tick事件
  public static void tick(Level level, BlockPos blockPos, BlockState blockState, BlockEntity be) {
    ZhenBlockEntity blockEntity = (ZhenBlockEntity) be;
    ZhenHash.ZHENMETHOD.executeMethod(blockEntity.zhen_type);
  }

  // 从世界加载方块数据,反正和下面那个只要加nbttag都要加上
  @Override
  public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
    super.loadAdditional(tag, registries);
    this.zhen_type = tag.getString("type");
  }

  // 保存方块数据到世界
  @Override
  public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putString("type", this.zhen_type);
  }
}

package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.ZhenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ZhenBlockEntity extends BlockEntity {
  private static final int[][] CACHED_SLOTS = new int[54][];
  public static int ZHEN_SIZE = 2;
  // 初始化阵法类型,默认为风阵
  private String ZHEN_TYPE = "wind";
  private NonNullList<ItemStack> items = NonNullList.withSize(ZHEN_SIZE, ItemStack.EMPTY);
  private int cooldownTime = -1;
  private long tickedGameTime;
  private Direction facing;
  private boolean hasInner;
  private boolean hasOuter;

  public ZhenBlockEntity(BlockPos Pos, BlockState BlockState) {
    super(Blocks.ZHEN_BLOCK_ENTITY.get(), Pos, BlockState);
    this.facing = BlockState.getValue(ZhenBlock.FACING);
  }

  // 阵法tick调用,根据阵法类型调用相应的阵法tick事件
  public static void tick(Level level, BlockPos blockPos, BlockState blockState, BlockEntity be) {
    ZhenBlockEntity blockEntity = (ZhenBlockEntity) be;
    ZhenHash.ZHENMETHOD.executeMethod(blockEntity.ZHEN_TYPE);
  }

  // 从世界加载方块数据,反正和下面那个只要加nbttag都要加上
  @Override
  public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
    super.loadAdditional(tag, registries);
    this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

    this.ZHEN_TYPE = tag.getString("type");
    this.cooldownTime = tag.getInt("TransferCooldown");
    this.hasInner = tag.getBoolean("HasInner");
    this.hasOuter = tag.getBoolean("HasOuter");
  }

  // 保存方块数据到世界
  @Override
  public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putInt("TransferCooldown", this.cooldownTime);
    tag.putBoolean("HasInner", this.hasInner);
    tag.putBoolean("HasOuter", this.hasOuter);
    tag.putString("type", this.ZHEN_TYPE);
  }

  public int getContainerSize() {
    return this.items.size();
  }

  public Boolean isHasInner() {
    return this.hasInner;
  }

  public Boolean isHasOuter() {
    return this.hasOuter;
  }

  public ItemStack removeItem(int pIndex, int pCount) {
    return ContainerHelper.removeItem(this.getItems(), pIndex, pCount);
  }

  public void setItem(int pIndex, ItemStack pStack) {
    this.getItems().set(pIndex, pStack);
    pStack.limitSize(this.getMaxStackSize(pStack));
  }

  protected NonNullList<ItemStack> getItems() {
    return this.items;
  }

  protected int getMaxStackSize(ItemStack pStack) {
    return Math.min(64, pStack.getMaxStackSize());
  }

  private boolean inventoryFull() {
    for (ItemStack itemstack : this.items) {
      if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
        return false;
      }
    }

    return true;
  }
}

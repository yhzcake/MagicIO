package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.ZhenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nonnull;

public class ZhenBlockEntity extends BlockEntity implements Container {
  @SuppressWarnings("unused")
  private static final int[][] CACHED_SLOTS = new int[54][];
  public static int ZHEN_SIZE = 2;
  // 初始化阵法类型,默认为风阵
  private ZhenType ZHEN_TYPE = ZhenTypeRegistry.getDefaultType();
  private NonNullList<ItemStack> items = NonNullList.withSize(ZHEN_SIZE, ItemStack.EMPTY);
  private int cooldownTime = -1;
  @SuppressWarnings("unused")
  private long tickedGameTime;
  @SuppressWarnings("unused")
  private Direction facing;
  private boolean hasInner;
  private boolean hasOuter;

  public ZhenBlockEntity(BlockPos Pos, BlockState BlockState) {
    super(Blocks.ZHEN_BLOCK_ENTITY.get(), Pos, BlockState);
    this.facing = BlockState.getValue(ZhenBlock.FACING);
  }

  public ZhenBlockEntity(BlockPos Pos, BlockState BlockState, ZhenType zhenType) {
    super(Blocks.ZHEN_BLOCK_ENTITY.get(), Pos, BlockState);
    this.facing = BlockState.getValue(ZhenBlock.FACING);
    this.ZHEN_TYPE = zhenType;
  }

  // 阵法tick调用,根据阵法类型调用相应的阵法tick事件
  public static void tick(@Nonnull Level level, @Nonnull BlockPos blockPos, @Nonnull BlockState blockState, @Nonnull BlockEntity be) {
    ZhenBlockEntity blockEntity = (ZhenBlockEntity) be;
    ZhenHash.ZHENMETHOD.executeMethod(blockEntity.ZHEN_TYPE.getId());
  }

  // 从世界加载方块数据,反正和下面那个只要加nbttag都要加上
  @Override
  public void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    ContainerHelper.loadAllItems(tag, this.items, registries);

    this.ZHEN_TYPE = ZhenTypeRegistry.getById(tag.getString("type"));
    this.cooldownTime = tag.getInt("TransferCooldown");
    this.hasInner = tag.getBoolean("HasInner");
    this.hasOuter = tag.getBoolean("HasOuter");
  }

  // 保存方块数据到世界
  @Override
  public void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    ContainerHelper.saveAllItems(tag, this.items, registries);
    tag.putInt("TransferCooldown", this.cooldownTime);
    tag.putBoolean("HasInner", this.hasInner);
    tag.putBoolean("HasOuter", this.hasOuter);
    tag.putString("type", this.ZHEN_TYPE.getId());
  }

  @Override
  public int getContainerSize() {
    return this.items.size();
  }

  public Boolean isHasInner() {
    return this.hasInner;
  }

  public Boolean isHasOuter() {
    return this.hasOuter;
  }

  public ZhenType getZhenType() {
    return this.ZHEN_TYPE;
  }

  public void setZhenType(ZhenType zhenType) {
    this.ZHEN_TYPE = zhenType;
  }

  @Override
  @Nonnull
  public ItemStack removeItem(int pIndex, int pCount) {
    return ContainerHelper.removeItem(this.items, pIndex, pCount);
  }

  @Override
  @Nonnull
  public ItemStack removeItemNoUpdate(int pIndex) {
    return ContainerHelper.takeItem(this.items, pIndex);
  }

  @Override
  public void setItem(int pIndex, @Nonnull ItemStack pStack) {
    this.items.set(pIndex, pStack);
    pStack.limitSize(this.getMaxStackSize());
    this.setChanged();
  }

  @Override
  @Nonnull
  public ItemStack getItem(int pIndex) {
    return this.items.get(pIndex);
  }

  @Nonnull
  public NonNullList<ItemStack> getItems() {
    return this.items;
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack itemstack : this.items) {
      if (!itemstack.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean stillValid(@Nonnull Player pPlayer) {
    return Container.stillValidBlockEntity(this, pPlayer);
  }

  @Override
  public void clearContent() {
    this.items.clear();
  }

  @Override
  public int getMaxStackSize() {
    return 64;
  }

  public int getMaxStackSize(@Nonnull ItemStack pStack) {
    return Math.min(this.getMaxStackSize(), pStack.getMaxStackSize());
  }

  @SuppressWarnings("unused")
  private boolean inventoryFull() {
    for (ItemStack itemstack : this.items) {
      if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
        return false;
      }
    }

    return true;
  }
}

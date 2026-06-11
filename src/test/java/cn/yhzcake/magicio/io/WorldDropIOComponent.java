package cn.yhzcake.magicio.io;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class WorldDropIOComponent implements IOComponent<Object, ItemStack> {
    private final Level level;
    private final BlockPos pos;
    private final Direction dropDirection;
    private Runnable onChange = () -> {};

    public WorldDropIOComponent(Level level, BlockPos pos, Direction dropDirection) {
        this.level = level;
        this.pos = pos;
        this.dropDirection = dropDirection;
    }

    public WorldDropIOComponent(Level level, BlockPos pos) {
        this(level, pos, Direction.UP);
    }

    @Override
    public IOType type() {
        return ModIOTypes.ITEM.get();
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public void setChangeCallback(Runnable onChanged) {
        this.onChange = onChanged;
    }

    @Override
    public boolean canSupply(int slot, Object requirement) {
        return false;
    }

    @Override
    public boolean hasSupply(Object requirement) {
        return false;
    }

    @Override
    public boolean canFit(int slot, ItemStack value) {
        if (level == null || value.isEmpty()) return false;
        return level.getBlockState(pos.relative(dropDirection)).isAir();
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insert(int slot, ItemStack value, boolean simulate) {
        return value;
    }

    @Override
    public ItemStack insert(ItemStack value, boolean simulate) {
        return value;
    }

    @Override
    public void consume(int slot, Object requirement) {
    }

    @Override
    public void produce(int slot, ItemStack value) {
        produce(value);
    }

    @Override
    public void produce(ItemStack value) {
        if (value.isEmpty()) return;
        BlockPos dropPos = pos.relative(dropDirection);
        Vec3 center = Vec3.atCenterOf(dropPos);
        ItemEntity item = new ItemEntity(level, center.x, center.y - 0.5, center.z, value, 0, 0, 0);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        onChange.run();
    }

    @Override
    public void saveNBT(ValueOutput output) {
    }

    @Override
    public void loadNBT(ValueInput input) {
    }
}

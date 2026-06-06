package cn.yhzcake.magicio.block.zhenbus;

import java.util.List;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ZhenBusBlockEntity extends BlockEntity implements ZhenBusHost {

    private final ZhenBusContainer container;

    public ZhenBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModZhenBusBlocks.ZHEN_BUS_BE.get(), pos, state);
        this.container = new ZhenBusContainer();
        this.container.setChangeCallback(this::markForSave);
    }

    @Override
    public @Nullable SideProcessor getProcessor(Direction direction) {
        return container.get(direction);
    }

    @Override
    public boolean canAddProcessor(Direction direction, ZhenType zhenType) {
        return container.canAdd(direction);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends SideProcessor> T addProcessor(ZhenType zhenType, Direction direction, Player player) {
        return (T) container.add(zhenType, direction, getLevel(), getBlockPos(), player);
    }

    @Override
    public void removeProcessor(Direction direction) {
        container.remove(direction);
        markForUpdate();
        if (container.isEmpty()) {
            destroyBusBlock();
        }
    }

    private void destroyBusBlock() {
        if (level != null && !level.isClientSide()) {
            for (ItemStack drop : container.collectDrops()) {
                Block.popResource(level, worldPosition, drop);
            }
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    public void markForUpdate() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void markForSave() {
        setChanged();
    }

    @Override
    public boolean isEmpty() {
        return container.isEmpty();
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    public ZhenBusContainer getContainer() {
        return container;
    }

    public VoxelShape getCombinedShape() {
        return container.getCombinedShape();
    }

    public List<ItemStack> collectDrops() {
        return container.collectDrops();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && container.isEmpty()) {
            initDefaultProcessors(level);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ZhenBusBlockEntity be) {
        be.container.tickAll();
    }

    private void initDefaultProcessors(Level level) {
        ZhenType defaultType = ZhenTypes.SMALL_SIFT_ZHEN.get();
        container.add(defaultType, Direction.DOWN, level, worldPosition, null);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        container.writeToNBT(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (level != null) {
            container.readFromNBT(input, level, worldPosition);
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

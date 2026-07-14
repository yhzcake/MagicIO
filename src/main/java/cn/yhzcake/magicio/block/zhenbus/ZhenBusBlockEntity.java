package cn.yhzcake.magicio.block.zhenbus;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.gridcell.GridCellSideProcessor;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

public class ZhenBusBlockEntity extends BlockEntity implements ZhenBusHost {

    private final ZhenBusContainer container = new ZhenBusContainer();
    /** 缓存 NBT，来自 loadAdditional（level=null 时） */
    private @Nullable CompoundTag pendingNbt;

    public ZhenBusBlockEntity(BlockPos pos, BlockState state) {
        super(ModZhenBusBlocks.ZHEN_BUS_BE.get(), pos, state);
        this.container.setChangeCallback(this::markForSave);
    }

    @Override public @Nullable SideProcessor getProcessor(Direction d) { return container.get(d); }
    @Override public boolean canAddProcessor(Direction d, ZhenType t) { return container.canAdd(d); }
    @SuppressWarnings("unchecked")
    @Override public <T extends SideProcessor> T addProcessor(ZhenType t, Direction d, Player p) { return (T) container.add(t, d, getLevel(), getBlockPos(), p); }
    @Override public void removeProcessor(Direction d) {
        container.remove(d); markForUpdate(); if (container.isEmpty()) destroyBusBlock();
    }
    private void destroyBusBlock() {
        if (level != null && !level.isClientSide()) {
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            for (ItemStack drop : container.collectDrops()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, drop);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
    @Override public void markForUpdate() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override public void markForSave() { setChanged(); }
    @Override public boolean isEmpty() { return container.isEmpty(); }
    @Override public BlockEntity getBlockEntity() { return this; }
    public ZhenBusContainer getContainer() { return container; }
    public VoxelShape getCombinedShape() { return container.getCombinedShape(); }
    public List<ItemStack> collectDrops() { return container.collectDrops(); }

    // ============ NBT ============

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (Direction d : Direction.values()) {
            SideProcessor p = container.get(d);
            if (p == null) continue;
            ValueOutput child = output.child(d.getName());
            child.putString("type", p.getZhenType().getType());
            if (p instanceof AbstractSideProcessor ap) {
                ap.writeToNBT(child);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 非 pending 路径（level != null）：直接创建处理器并恢复
        if (level != null) {
            if (level.isClientSide()) {
                applyClientUpdate(input);
                return;
            }
            for (Direction d : Direction.values()) {
                input.child(d.getName()).ifPresent(child -> {
                    String type = child.getString("type").orElse("");
                    if (type.isEmpty()) return;
                    ZhenType zhenType = ZhenTypes.getTypeStrict(type);
                    if (zhenType == null) return;
                    SideProcessor p = createProcessorForType(d, zhenType, worldPosition, level);
                    if (p instanceof AbstractSideProcessor ap) {
                        ap.readFromNBT(child);
                    }
                    p.onAdd();
                    p.setChangeCallback(container.getChangeCallback());
                    container.getStorage().set(d, p);
                });
            }
            return;
        }

        // pending 路径（level == null）：缓存到 pendingNbt，onLoad 后再恢复
        CompoundTag tag = new CompoundTag();
        for (Direction d : Direction.values()) {
            input.child(d.getName()).ifPresent(child -> {
                String type = child.getString("type").orElse("");
                if (type.isEmpty()) return;
                CompoundTag sideTag = new CompoundTag();
                sideTag.putString("type", type);
                sideTag.putInt("process_time", child.getIntOr("process_time", 0));
                sideTag.putBoolean("inputs_changed", child.getBooleanOr("inputs_changed", false));
                sideTag.putInt("effective_processing_time", child.getIntOr("effective_processing_time", 0));
                sideTag.putDouble("output_multiplier", child.getDoubleOr("output_multiplier", 1.0));
                sideTag.putInt("last_input_hash", child.getIntOr("last_input_hash", 0));
                sideTag.putInt("recipe_generation", child.getIntOr("recipe_generation", 0));
                sideTag.putInt("recipe_check_timer", child.getIntOr("recipe_check_timer", 0));
                sideTag.putLong("state_revision", child.getLongOr("state_revision", 0));
                sideTag.putLong("cycle_id", child.getLongOr("cycle_id", 0));
                sideTag.putBoolean("output_blocked", child.getBooleanOr("output_blocked", false));
                // Items — ContainerHelper 格式 (ItemStackWithSlot 索引格式)
                child.list("Items", ItemStackWithSlot.CODEC).ifPresent(list -> {
                    List<ItemStackWithSlot> copy = new ArrayList<>();
                    list.forEach(copy::add);
                    sideTag.store("Items", ItemStackWithSlot.CODEC.listOf(), copy);
                });
                // Fluids — FluidTank_N 格式
                for (int i = 0; i < 64; i++) {
                    final int idx = i;
                    child.read("FluidTank_" + idx, FluidStack.CODEC).ifPresent(fs ->
                            sideTag.store("FluidTank_" + idx, FluidStack.CODEC, fs));
                }
                // Energy
                child.getInt("energy").ifPresent(e -> sideTag.putInt("energy", e));
                child.getString("current_recipe_id").ifPresent(cr ->
                        sideTag.putString("current_recipe_id", cr));
                child.getString("last_valid_recipe_id").ifPresent(cr ->
                        sideTag.putString("last_valid_recipe_id", cr));
                tag.put(d.getName(), sideTag);
            });
        }
        if (!tag.isEmpty()) {
            pendingNbt = tag;
        }
    }

    private void applyClientUpdate(ValueInput input) {
        for (Direction d : Direction.values()) {
            var child = input.child(d.getName());
            if (child.isEmpty() || !child.get().getBooleanOr("present", false)) {
                container.getStorage().remove(d);
                continue;
            }
            ValueInput state = child.get();
            String typeName = state.getString("type").orElse("");
            ZhenType zhenType = ZhenTypes.getTypeStrict(typeName);
            if (zhenType == null) {
                container.getStorage().remove(d);
                continue;
            }
            SideProcessor existing = container.get(d);
            if (existing == null || existing.getZhenType() != zhenType) {
                if (existing != null) existing.onRemove();
                existing = createProcessorForType(d, zhenType, worldPosition, level);
                existing.setChangeCallback(() -> {});
                container.getStorage().set(d, existing);
            }
            if (existing instanceof AbstractSideProcessor ap) {
                ap.applyClientState(
                        state.getIntOr("process_time", 0),
                        state.getBooleanOr("has_recipe", false));
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) return;
        if (pendingNbt != null) {
            deserializeProcessors(pendingNbt);
            pendingNbt = null;
        }
    }

    private void deserializeProcessors(CompoundTag tag) {
        for (Direction d : Direction.values()) {
            CompoundTag sideTag = tag.getCompoundOrEmpty(d.getName());
            if (sideTag.isEmpty()) continue;
            String tn = sideTag.getString("type").orElse("");
            if (tn.isEmpty()) continue;
            ZhenType type = ZhenTypes.getTypeStrict(tn);
            if (type == null) continue;

            SideProcessor p = createProcessorForType(d, type, worldPosition, level);
            p.onAdd();
            p.setChangeCallback(container.getChangeCallback());

            if (p instanceof AbstractSideProcessor ap) {
                ValueInput childInput = TagValueInput.create(
                        ProblemReporter.DISCARDING, level.registryAccess(), sideTag);
                ap.readFromNBT(childInput);
            }

            container.getStorage().set(d, p);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ZhenBusBlockEntity be) {
        be.container.tickAll();
    }

    /**
     * 根据 ZhenType 创建对应的 SideProcessor 实例。
     * grid_cell 类型创建 {@link GridCellSideProcessor}，其余创建 {@link AbstractSideProcessor}。
     */
    public static SideProcessor createProcessorForType(Direction dir, ZhenType zhenType, BlockPos pos, Level level) {
        if ("magic_io:grid_cell".equals(zhenType.getType())) {
            return new GridCellSideProcessor(dir, zhenType, pos, level);
        }
        return new AbstractSideProcessor(dir, zhenType, pos, level) {};
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        container.writeToUpdateTag(tag);
        return tag;
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

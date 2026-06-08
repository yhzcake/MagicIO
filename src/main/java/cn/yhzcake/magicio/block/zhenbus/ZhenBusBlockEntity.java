package cn.yhzcake.magicio.block.zhenbus;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.gridcell.GridCellSideProcessor;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import cn.yhzcake.magicio.io.FluidIOComponent;
import cn.yhzcake.magicio.io.FluidStackWithTank;
import cn.yhzcake.magicio.io.IOComponent;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
            for (ItemStack drop : container.collectDrops()) Block.popResource(level, worldPosition, drop);
            level.destroyBlock(worldPosition, false);
        }
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
            child.putInt("processing_time", p.getProcessTime());
            // 持久化所有 IOType 组件（Fluid 单独处理以使用 Fluids 列表格式）
            for (IOComponent<?, ?> component : p.getIOProcessor().getAll()) {
                if (component.type() == ModIOTypes.FLUID.get()) continue;
                component.saveNBT(child);
            }
            // Fluids — 列表格式 [{tank, fluid}]
            Object rawFluid = p.getIOProcessor().get(ModIOTypes.FLUID.get());
            if (rawFluid instanceof FluidIOComponent fluidIO) {
                NonNullList<FluidStack> tanks = fluidIO.getTanks();
                List<FluidStackWithTank> entries = new ArrayList<>();
                for (int i = 0; i < tanks.size(); i++) {
                    if (!tanks.get(i).isEmpty()) {
                        entries.add(new FluidStackWithTank(i, tanks.get(i).copy()));
                    }
                }
                if (!entries.isEmpty()) {
                    child.store("Fluids", FluidStackWithTank.CODEC.listOf(), entries);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 非 pending 路径（level != null）：直接创建处理器并恢复
        if (level != null) {
            for (Direction d : Direction.values()) {
                input.child(d.getName()).ifPresent(child -> {
                    String type = child.getString("type").orElse("");
                    if (type.isEmpty()) return;
                    ZhenType zhenType = ZhenTypes.getType(type);
                    if (zhenType == null) return;
                    SideProcessor p = createProcessorForType(d, zhenType, worldPosition, level);
                    p.setProcessTime(child.getIntOr("processing_time", 0));
                    p.setInputsChanged(true);
                    // 仅 AbstractSideProcessor 子类才有 IOProcessor，跳过 GridCellSideProcessor
                    if (p instanceof AbstractSideProcessor ap) {
                        for (IOComponent<?, ?> component : ap.getIOProcessor().getAll()) {
                            if (component.type() == ModIOTypes.FLUID.get()) continue;
                            component.loadNBT(child);
                        }
                        // Fluids
                        Object rawFluid = ap.getIOProcessor().get(ModIOTypes.FLUID.get());
                        if (rawFluid instanceof FluidIOComponent fluidIO) {
                            NonNullList<FluidStack> tanks = fluidIO.getTanks();
                            child.read("Fluids", FluidStackWithTank.CODEC.listOf()).ifPresent(list -> {
                                for (FluidStackWithTank entry : list) {
                                    if (entry.tank() >= 0 && entry.tank() < tanks.size()) {
                                        tanks.set(entry.tank(), entry.fluid().copy());
                                    }
                                }
                            });
                        }
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
                sideTag.putInt("processing_time", child.getIntOr("processing_time", 0));
                // Items — ContainerHelper 格式 (ItemStackWithSlot)
                child.list("Items", ItemStackWithSlot.CODEC).ifPresent(list -> {
                    List<ItemStackWithSlot> copy = new ArrayList<>();
                    list.forEach(copy::add);
                    sideTag.store("Items", ItemStackWithSlot.CODEC.listOf(), copy);
                });
                // Fluids — 列表格式 [{tank, fluid}]
                child.read("Fluids", FluidStackWithTank.CODEC.listOf()).ifPresent(list -> {
                    sideTag.store("Fluids", FluidStackWithTank.CODEC.listOf(), list);
                });
                // Energy
                int energy = child.getIntOr("energy", 0);
                if (energy > 0) sideTag.putInt("energy", energy);
                tag.put(d.getName(), sideTag);
            });
        }
        if (!tag.isEmpty()) {
            pendingNbt = tag;
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
            ZhenType type = ZhenTypes.getType(tn);
            if (type == null) continue;

            SideProcessor p = createProcessorForType(d, type, worldPosition, level);
            p.setProcessTime(sideTag.getIntOr("processing_time", 0));
            p.setInputsChanged(true);
            p.onAdd();
            p.setChangeCallback(container.getChangeCallback());

            if (p instanceof AbstractSideProcessor ap) {
                // 恢复 IOType 组件
                ValueInput childInput = TagValueInput.create(
                        ProblemReporter.DISCARDING, level.registryAccess(), sideTag);
                for (IOComponent<?, ?> component : ap.getIOProcessor().getAll()) {
                    if (component.type() == ModIOTypes.FLUID.get()) continue;
                    component.loadNBT(childInput);
                }
                // Fluids
                Object rawFluid = ap.getIOProcessor().get(ModIOTypes.FLUID.get());
                if (rawFluid instanceof FluidIOComponent fluidIO) {
                    NonNullList<FluidStack> tanks = fluidIO.getTanks();
                    sideTag.read("Fluids", FluidStackWithTank.CODEC.listOf()).ifPresent(list -> {
                        for (FluidStackWithTank entry : list) {
                            if (entry.tank() >= 0 && entry.tank() < tanks.size()) {
                                tanks.set(entry.tank(), entry.fluid().copy());
                            }
                        }
                    });
                }
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

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return super.getUpdateTag(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

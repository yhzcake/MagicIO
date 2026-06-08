package cn.yhzcake.magicio.block.zhenbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import cn.yhzcake.magicio.block.gridcell.GridCellSideProcessor;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

public class ZhenBusContainer {

    private final ZhenBusStorage storage = new ZhenBusStorage();
    private Runnable onChanged = () -> {};

    public @Nullable SideProcessor get(Direction dir) {
        return storage.get(dir);
    }

    public boolean canAdd(Direction dir) {
        return !storage.has(dir);
    }

    public SideProcessor add(ZhenType type, Direction dir, Level level, BlockPos pos, Player player) {
        // 收集旧处理器的掉落物（覆盖替换时）
        SideProcessor old = storage.get(dir);
        if (old != null) {
            List<ItemStack> drops = new ArrayList<>();
            old.addDrops(drops);
            for (ItemStack drop : drops) {
                Block.popResource(level, pos, drop);
            }
            old.onRemove();
        }

        SideProcessor processor;
        if ("magic_io:grid_cell".equals(type.getType())) {
            processor = new GridCellSideProcessor(dir, type, pos, level);
        } else {
            processor = new AbstractSideProcessor(dir, type, pos, level) {};
        }
        processor.onAdd();
        processor.setChangeCallback(onChanged);
        processor.setInputsChanged(true);
        storage.set(dir, processor);
        level.invalidateCapabilities(pos);
        onChanged.run();
        return processor;
    }

    public void remove(Direction dir) {
        SideProcessor processor = storage.get(dir);
        if (processor != null) {
            Level level = processor.getLevel();
            BlockPos pos = processor.getPos();
            processor.onRemove();
            storage.remove(dir);
            if (level != null) level.invalidateCapabilities(pos);
            onChanged.run();
        }
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public int size() {
        return storage.size();
    }

    public void tickAll() {
        for (SideProcessor processor : storage.all()) {
            processor.tick();
        }
    }

    public List<ItemStack> collectDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (SideProcessor processor : storage.all()) {
            processor.addDrops(drops);
        }
        return drops;
    }

    // ===== 碰撞箱 =====

    public static final VoxelShape DEFAULT_FACE = Shapes.box(0, 0, 0, 1, 1.0/16, 1);

    private static final Map<Direction, VoxelShape> FACE_SHAPES = Map.of(
            Direction.DOWN,  DEFAULT_FACE,
            Direction.UP,    Shapes.box(0, 15.0/16, 0, 1, 1, 1),
            Direction.NORTH, Shapes.box(0, 0, 0, 1, 1, 1.0/16),
            Direction.SOUTH, Shapes.box(0, 0, 15.0/16, 1, 1, 1),
            Direction.WEST,  Shapes.box(0, 0, 0, 1.0/16, 1, 1),
            Direction.EAST,  Shapes.box(15.0/16, 0, 0, 1, 1, 1)
    );

    public VoxelShape getCombinedShape() {
        VoxelShape shape = Shapes.empty();
        for (Direction dir : Direction.values()) {
            if (storage.has(dir)) {
                shape = Shapes.joinUnoptimized(shape, FACE_SHAPES.get(dir), BooleanOp.OR);
            }
        }
        return shape.optimize();
    }

    public static VoxelShape shapeForFace(Direction side) {
        return FACE_SHAPES.get(side);
    }

    // ===== NBT 序列化 =====

    private static final Codec<List<ItemStack>> ITEMS_CODEC = ItemStack.CODEC.listOf();
    private static final Codec<List<FluidStack>> FLUIDS_CODEC = FluidStack.CODEC.listOf();

    public static Codec<List<ItemStack>> getItemsCodec() { return ITEMS_CODEC; }
    public static Codec<List<FluidStack>> getFluidsCodec() { return FLUIDS_CODEC; }

    public void writeToNBT(ValueOutput output) {
        for (Direction dir : Direction.values()) {
            SideProcessor processor = storage.get(dir);
            if (processor == null) continue;
            writeProcessorToNBT(output, dir, processor);
        }
    }

    /** 直接写入 {@link CompoundTag} 供 {@code getUpdateTag} 使用。 */
    public void writeToUpdateTag(net.minecraft.nbt.CompoundTag tag) {
        for (Direction dir : Direction.values()) {
            SideProcessor processor = storage.get(dir);
            if (processor == null) continue;
            net.minecraft.nbt.CompoundTag sideTag = new net.minecraft.nbt.CompoundTag();
            sideTag.putString("type", processor.getZhenType().getType());
            sideTag.putInt("processing_time", processor.getProcessTime());
            sideTag.putBoolean("inputs_changed", processor.isInputsChanged());

            NonNullList<ItemStack> procItems = processor.getItemsForSerialization();
            if (procItems != null) {
                List<ItemStack> nonEmpty = new ArrayList<>();
                for (ItemStack stack : procItems) {
                    if (!stack.isEmpty()) {
                        nonEmpty.add(stack.copy());
                    }
                }
                if (!nonEmpty.isEmpty()) {
                    sideTag.put("items", ITEMS_CODEC.encodeStart(
                            net.minecraft.nbt.NbtOps.INSTANCE, nonEmpty).result().orElse(new net.minecraft.nbt.ListTag()));
                }
            }

            NonNullList<FluidStack> procFluids = processor.getFluidsForSerialization();
            if (procFluids != null) {
                List<FluidStack> nonEmpty = new ArrayList<>();
                for (FluidStack fs : procFluids) {
                    if (!fs.isEmpty()) {
                        nonEmpty.add(fs.copy());
                    }
                }
                if (!nonEmpty.isEmpty()) {
                    sideTag.put("fluids", FLUIDS_CODEC.encodeStart(
                            net.minecraft.nbt.NbtOps.INSTANCE, nonEmpty).result().orElse(new net.minecraft.nbt.ListTag()));
                }
            }

            tag.put(dir.getName(), sideTag);
        }
    }

    private void writeProcessorToNBT(ValueOutput output, Direction dir, SideProcessor processor) {
        ValueOutput child = output.child(dir.getName());
        child.putString("type", processor.getZhenType().getType());
        child.putInt("processing_time", processor.getProcessTime());
        child.putBoolean("inputs_changed", processor.isInputsChanged());

        NonNullList<ItemStack> procItems = processor.getItemsForSerialization();
        if (procItems != null) {
            List<ItemStack> nonEmpty = new ArrayList<>();
            for (ItemStack stack : procItems) {
                if (!stack.isEmpty()) {
                    nonEmpty.add(stack.copy());
                }
            }
            child.store("items", ITEMS_CODEC, nonEmpty);
        }

        NonNullList<FluidStack> procFluids = processor.getFluidsForSerialization();
        if (procFluids != null) {
            List<FluidStack> nonEmpty = new ArrayList<>();
            for (FluidStack fs : procFluids) {
                if (!fs.isEmpty()) {
                    nonEmpty.add(fs.copy());
                }
            }
            child.store("fluids", FLUIDS_CODEC, nonEmpty);
        }
    }

    public void readFromNBT(ValueInput input, Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            String key = dir.getName();
            input.child(key).ifPresent(child -> {
                String typeName = child.getString("type").orElse("");
                if (typeName.isEmpty()) return;

                ZhenType type = ZhenTypes.getType(typeName);
                if (type == null) return;

                AbstractSideProcessor processor = new AbstractSideProcessor(dir, type, pos, level) {};
                processor.setProcessTime(child.getIntOr("processing_time", 0));
                processor.setInputsChanged(true);
                // 自动恢复所有 IOType 组件
                for (cn.yhzcake.magicio.io.IOComponent<?, ?> component : processor.getIOProcessor().getAll()) {
                    component.loadNBT(child);
                }
                processor.setChangeCallback(onChanged);
                storage.set(dir, processor);
            });
        }
    }

    public void setChangeCallback(Runnable onChanged) {
        this.onChanged = onChanged;
        // 传播到已有处理器：确保 NBT 加载后变更能被持久化
        for (SideProcessor p : storage.all()) {
            p.setChangeCallback(onChanged);
        }
    }

    public Runnable getChangeCallback() {
        return onChanged;
    }

    public ZhenBusStorage getStorage() {
        return storage;
    }
}

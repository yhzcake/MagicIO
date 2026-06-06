package cn.yhzcake.magicio.block.zhenbus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
        SideProcessor processor = new AbstractSideProcessor(dir, type, pos, level) {};
        processor.onAdd();
        storage.set(dir, processor);
        onChanged.run();
        return processor;
    }

    public void remove(Direction dir) {
        SideProcessor processor = storage.get(dir);
        if (processor != null) {
            processor.onRemove();
            storage.remove(dir);
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

    public void writeToNBT(ValueOutput output) {
        for (Direction dir : Direction.values()) {
            SideProcessor processor = storage.get(dir);
            if (processor == null) continue;

            String prefix = dir.getName();
            output.putString(prefix + "_type", processor.getZhenType().getType());
            output.putInt(prefix + "_process_time", processor.getProcessTime());
            output.putBoolean(prefix + "_inputs_changed", processor.isInputsChanged());

            NonNullList<ItemStack> procItems = processor.getItemsForSerialization();
            if (procItems != null) {
                List<ItemStack> nonEmpty = new ArrayList<>();
                for (ItemStack stack : procItems) {
                    if (!stack.isEmpty()) {          // 过滤空物品，避免 ItemStack.CODEC 严格验证失败
                        nonEmpty.add(stack.copy());
                    }
                }
                output.store(prefix + "_items", ITEMS_CODEC, nonEmpty);
            }

            NonNullList<FluidStack> procFluids = processor.getFluidsForSerialization();
            if (procFluids != null) {
                List<FluidStack> nonEmpty = new ArrayList<>();
                for (FluidStack fs : procFluids) {
                    if (!fs.isEmpty()) {              // 过滤空流体
                        nonEmpty.add(fs.copy());
                    }
                }
                output.store(prefix + "_fluids", FLUIDS_CODEC, nonEmpty);
            }
        }
    }

    public void readFromNBT(ValueInput input, Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            String prefix = dir.getName();
            String typeName = input.getString(prefix + "_type").orElse("");
            if (typeName.isEmpty()) continue;

            ZhenType type = ZhenTypes.getType(typeName);
            if (type == null) continue;

            AbstractSideProcessor processor = new AbstractSideProcessor(dir, type, pos, level) {};
            processor.setProcessTime(input.getIntOr(prefix + "_process_time", 0));
            // 从 NBT 加载后强制配方重检（防止重开游戏后 inputsChanged=false 导致配方不启动）
            processor.setInputsChanged(input.getBooleanOr(prefix + "_inputs_changed", true));

            NonNullList<ItemStack> procItems = processor.getItemsForSerialization();
            if (procItems != null) {
                List<ItemStack> loaded = input.read(prefix + "_items", ITEMS_CODEC).orElse(List.of());
                for (int i = 0; i < procItems.size(); i++) {
                    if (i < loaded.size()) {
                        procItems.set(i, loaded.get(i).copy());
                    } else {
                        procItems.set(i, ItemStack.EMPTY);  // 缺失的槽位补空
                    }
                }
            }

            NonNullList<FluidStack> procFluids = processor.getFluidsForSerialization();
            if (procFluids != null) {
                List<FluidStack> loaded = input.read(prefix + "_fluids", FLUIDS_CODEC).orElse(List.of());
                for (int i = 0; i < procFluids.size(); i++) {
                    if (i < loaded.size()) {
                        procFluids.set(i, loaded.get(i).copy());
                    } else {
                        procFluids.set(i, FluidStack.EMPTY);  // 缺失的槽位补空
                    }
                }
            }

            storage.set(dir, processor);
        }
    }

    public void setChangeCallback(Runnable onChanged) {
        this.onChanged = onChanged;
    }
}

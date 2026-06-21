package cn.yhzcake.magicio.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.FaceAccessController;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhenbus.PortBinding;
import cn.yhzcake.magicio.block.zhenbus.VirtualPort;
import cn.yhzcake.magicio.item.crafting.RecipeProcessor;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

public abstract class AbstractSideProcessor implements SideProcessor {

    protected final Direction side;
    protected final ZhenType zhenType;
    protected final BlockPos pos;
    protected final Level level;
    protected final SlotPartition partition;
    protected final IOProcessor ioProcessor;
    protected final NonNullList<ItemStack> items;
    protected final NonNullList<FluidStack> tanks;
    protected final @Nullable Integer tankCapacity;
    protected final @Nullable Integer energyCapacity;

    protected int processTime = 0;
    protected boolean inputsChanged = false;
    protected @Nullable ZhenRecipe currentRecipe;
    protected Runnable onChanged = () -> {};
    protected final FaceAccessController faceAccessController = new FaceAccessController();

    private int tickInterval = 1;
    private int tickCounter = 0;

    private final Map<String, VirtualPort> virtualPorts = new HashMap<>();

    // 槽位集合缓存（由 initIOComponents 初始化）
    private Set<Integer> outputItemSlots = Set.of();
    private Set<Integer> inputItemSlots = Set.of();
    private Set<Integer> fluidSlots = Set.of();
    private Set<Integer> fluidInputSlots = Set.of();
    private Set<Integer> fluidOutputSlots = Set.of();

    public Set<Integer> getInputItemSlots() {
        return inputItemSlots;
    }

    public Set<Integer> getOutputItemSlots() {
        return outputItemSlots;
    }

    public Set<Integer> getFluidSlots() {
        return fluidSlots;
    }

    public Set<Integer> getFluidInputSlots() {
        return fluidInputSlots;
    }

    public Set<Integer> getFluidOutputSlots() {
        return fluidOutputSlots;
    }

    public AbstractSideProcessor(Direction side, ZhenType zhenType, BlockPos pos, Level level) {
        this.side = side;
        this.zhenType = zhenType;
        this.pos = pos;
        this.level = level;
        this.partition = zhenType.getPartition();
        this.tankCapacity = zhenType.getTankCapacity();
        this.energyCapacity = zhenType.getEnergyCapacity();

        int itemSlots = partition.getTotalSlots(ModIOTypes.ITEM.get());
        int fluidSlots = partition.getTotalSlots(ModIOTypes.FLUID.get());
        this.items = NonNullList.withSize(itemSlots, ItemStack.EMPTY);
        this.tanks = NonNullList.withSize(fluidSlots, FluidStack.EMPTY);

        this.ioProcessor = new IOProcessor();
        initIOComponents();
        initVirtualPorts();
        initSlotCache();
        initFaceAccess();
    }

    private void initIOComponents() {
        ioProcessor.register(new ItemIOComponent(items, partition));
        ioProcessor.register(new FluidIOComponent(tanks, partition, tankCapacity));
        if (energyCapacity != null) {
            ioProcessor.register(new EnergyIOComponent(energyCapacity, 1));
        }
        ioProcessor.registerChangeCallback(() -> {
            Runnable cb = onChanged;
            if (cb != null) cb.run();
            inputsChanged = true;
        });
    }

    private void initSlotCache() {
        inputItemSlots = Set.copyOf(partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL));
        outputItemSlots = Set.copyOf(partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL));
        fluidInputSlots = Set.copyOf(partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_INPUT_ALL));
        fluidOutputSlots = Set.copyOf(partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_OUTPUT_ALL));
        Set<Integer> combined = new java.util.HashSet<>(fluidInputSlots);
        combined.addAll(fluidOutputSlots);
        fluidSlots = Set.copyOf(combined);
    }

    private void initFaceAccess() {
        Map<Direction, Map<IOType, Set<Integer>>> defined = zhenType.getFaceAccess();
        if (!defined.isEmpty()) {
            for (var dirEntry : defined.entrySet()) {
                Direction dir = dirEntry.getKey();
                for (var typeEntry : dirEntry.getValue().entrySet()) {
                    faceAccessController.setSlotsForFace(dir, typeEntry.getKey(), typeEntry.getValue());
                }
            }
            return;
        }
        if (!inputItemSlots.isEmpty()) {
            faceAccessController.setSlotsForFace(side, ModIOTypes.ITEM.get(), inputItemSlots);
        }
        if (!outputItemSlots.isEmpty()) {
            faceAccessController.addSlotsToFaceAccess(side, ModIOTypes.ITEM.get(), outputItemSlots);
        }
        if (!fluidSlots.isEmpty()) {
            faceAccessController.setSlotsForFace(side, ModIOTypes.FLUID.get(), fluidSlots);
        }
        if (energyCapacity != null) {
            faceAccessController.setSlotsForFace(side, ModIOTypes.ENERGY.get(), Set.of(0));
        }
    }

    // ============ VirtualPort 管理 ============

    private void initVirtualPorts() {
        virtualPorts.clear();
        virtualPorts.put("self", new VirtualPort("self", List.of(
                new PortBinding(side, side.getOpposite())
        )));
        for (Direction dir : Direction.values()) {
            virtualPorts.put(dir.getName(), new VirtualPort(dir.getName(), List.of(
                    new PortBinding(dir, dir.getOpposite())
            )));
        }
    }

    public void scanAllPorts() {
        if (level == null) return;
        long currentTick = level.getGameTime();
        for (VirtualPort port : virtualPorts.values()) {
            port.forceScan(level, pos, currentTick);
        }
    }

    public void invalidatePorts() {
        for (VirtualPort port : virtualPorts.values()) {
            port.invalidate();
        }
    }

    public void tickRefreshPorts() {
        if (level == null) return;
        long currentTick = level.getGameTime();
        for (VirtualPort port : virtualPorts.values()) {
            port.tickRefresh(level, pos, currentTick);
        }
    }

    public @Nullable VirtualPort getPort(String name) {
        return virtualPorts.get(name);
    }

    public Map<String, VirtualPort> getVirtualPorts() {
        return Collections.unmodifiableMap(virtualPorts);
    }

    // ============ 面访问控制 ============

    @Override
    public @Nullable Map<IOType, Set<Integer>> getFaceAccess(Direction worldDirection) {
        Map<Direction, Map<IOType, Set<Integer>>> raw = faceAccessController.getIoFaceAccess();
        return raw.get(worldDirection);
    }

    // ============ VirtualPort 产出推送 ============

    /** 遍历所有已注册的 IO 组件，自动通过 VirtualPort 推送其输出槽。 */
    private void pushOutputsThroughPorts() {
        boolean changed = false;

        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            IOType type = component.type();
            if (type == ModIOTypes.ENERGY.get()) {
                changed = pushEnergyOutput() || changed;
            } else if (type == ModIOTypes.FLUID.get()) {
                changed = pushOutput(fluidSlots, tanks, type) || changed;
            } else {
                changed = pushOutput(outputItemSlots, items, type) || changed;
            }
        }

        if (changed && onChanged != null) {
            onChanged.run();
        }
    }

    private <T> boolean pushOutput(Set<Integer> outputSlots, NonNullList<T> storage, IOType type) {
        boolean changed = false;
        for (int slot : outputSlots) {
            T value = storage.get(slot);
            if (isSlotEmpty(value)) continue;
            for (VirtualPort port : virtualPorts.values()) {
                if (!port.hasDirectConnection()) continue;
                T remaining = port.transfer(value, type, false);
                if (!isSlotSameAmount(remaining, value)) {
                    storage.set(slot, remaining);
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean isSlotEmpty(Object value) {
        if (value instanceof ItemStack is) return is.isEmpty();
        if (value instanceof FluidStack fs) return fs.isEmpty();
        return false;
    }

    private static boolean isSlotSameAmount(Object a, Object b) {
        if (a instanceof ItemStack ia && b instanceof ItemStack ib) return ia.getCount() == ib.getCount();
        if (a instanceof FluidStack fa && b instanceof FluidStack fb) return fa.getAmount() == fb.getAmount();
        return true;
    }

    @SuppressWarnings({"rawtypes"})
    private boolean pushEnergyOutput() {
        if (energyCapacity == null) return false;
        IOComponent rawComp = ioProcessor.get(ModIOTypes.ENERGY.get());
        if (!(rawComp instanceof EnergyIOComponent energyComp)) return false;
        if (energyComp.getEnergy() <= 0) return false;

        for (VirtualPort port : virtualPorts.values()) {
            if (!port.hasDirectConnection() || !port.canAccept(ModIOTypes.ENERGY.get())) continue;
            int available = energyComp.getEnergy();
            Integer remaining = port.transfer(available, ModIOTypes.ENERGY.get(), false);
            if (remaining < available) {
                return true;
            }
        }
        return false;
    }

    // ============ 接口实现 ============

    @Override
    public Direction getSide() {
        return side;
    }

    @Override
    public ZhenType getZhenType() {
        return zhenType;
    }

    @Override
    public IOProcessor getIOProcessor() {
        return ioProcessor;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    public void setTickInterval(int interval) {
        this.tickInterval = Math.max(1, interval);
    }

    @Override
    public boolean hasWork() {
        return currentRecipe != null || inputsChanged;
    }

    @Override
    public void tick() {
        if (++tickCounter % tickInterval != 0) return;

        if (!hasWork() && !zhenType.hasTickFactory()) {
            return;
        }

        int tank0Before = tanks.isEmpty() ? -1 : (tanks.get(0).isEmpty() ? 0 : tanks.get(0).getAmount());

        tickRefreshPorts();

        if (hasWork()) {
            RecipeProcessor.State recipeState = new RecipeProcessor.State();
            recipeState.processTime = this.processTime;
            recipeState.inputsChanged = this.inputsChanged;
            recipeState.currentRecipe = this.currentRecipe;

            boolean needSync = RecipeProcessor.processTick(
                    level, pos, recipeState, zhenType.getType(),
                    partition, items, tanks, ioProcessor, faceAccessController.getZoneFaceAccess(),
                    true,
                    onChanged);

            this.processTime = recipeState.processTime;
            this.inputsChanged = recipeState.inputsChanged;
            this.currentRecipe = recipeState.currentRecipe;

            boolean hasAnyConnected = false;
            for (var port : virtualPorts.values()) {
                if (port.hasDirectConnection()) { hasAnyConnected = true; break; }
            }
            if (hasAnyConnected) {
                pushOutputsThroughPorts();
            }

            if (needSync) {
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }

        int tank0After = tanks.isEmpty() ? -1 : (tanks.get(0).isEmpty() ? 0 : tanks.get(0).getAmount());
        if (tank0Before != tank0After) {
            MagicIO.LOGGER.info("[tick] {} tank[0] {}→{}", pos.toShortString(), tank0Before, tank0After);
        }

        zhenType.execute(level, pos, level.getBlockState(pos), null);
    }

    @Override
    public VoxelShape getShape() {
        return Shapes.block();
    }

    @Override
    public void setChangeCallback(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public boolean onActivate(Player player, InteractionHand hand, Vec3 hitPos) {
        return false;
    }

    @Override
    public boolean onShiftActivate(Player player, InteractionHand hand, Vec3 hitPos) {
        return false;
    }

    @Override
    public void addDrops(List<ItemStack> drops) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Override
    public void onAdd() {
        scanAllPorts();
    }

    @Override
    public void onRemove() {
        invalidatePorts();
    }

    @Override
    public @Nullable NonNullList<ItemStack> getItemsForSerialization() {
        return items;
    }

    @Override
    public @Nullable NonNullList<FluidStack> getFluidsForSerialization() {
        return tanks;
    }

    @Override
    public int getProcessTime() {
        return processTime;
    }

    @Override
    public void setProcessTime(int time) {
        this.processTime = time;
    }

    @Override
    public boolean isInputsChanged() {
        return inputsChanged;
    }

    @Override
    public void setInputsChanged(boolean changed) {
        this.inputsChanged = changed;
    }

    @Override
    public void writeToNBT(ValueOutput output) {
        output.putString("side", side.getName());
        output.putInt("process_time", processTime);
        output.putBoolean("inputs_changed", inputsChanged);
        ContainerHelper.saveAllItems(output, items);
        for (int i = 0; i < tanks.size(); i++) {
            if (!tanks.get(i).isEmpty()) {
                output.store("FluidTank_" + i, FluidStack.CODEC, tanks.get(i));
            }
        }
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            component.saveNBT(output);
        }
        if (currentRecipe != null) {
            output.putString("current_recipe", currentRecipe.getZhenTypeStr());
        }
    }

    @Override
    public void readFromNBT(ValueInput input) {
        processTime = input.getIntOr("process_time", 0);
        inputsChanged = input.getBooleanOr("inputs_changed", false);
        ContainerHelper.loadAllItems(input, items);
        for (int i = 0; i < tanks.size(); i++) {
            tanks.set(i, input.read("FluidTank_" + i, FluidStack.CODEC).orElse(FluidStack.EMPTY));
        }
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            component.loadNBT(input);
        }
    }

    @Override
    public void writeToStream(FriendlyByteBuf buf) {
        buf.writeInt(processTime);
        buf.writeBoolean(currentRecipe != null);
    }

    @Override
    public boolean readFromStream(FriendlyByteBuf buf) {
        boolean changed = false;
        int newProcessTime = buf.readInt();
        if (newProcessTime != processTime) {
            processTime = newProcessTime;
            changed = true;
        }
        boolean hasRecipe = buf.readBoolean();
        if ((currentRecipe != null) != hasRecipe) {
            changed = true;
        }
        return changed;
    }
}

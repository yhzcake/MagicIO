package cn.yhzcake.magicio.io;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.FaceAccessController;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhenbus.VirtualPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
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

    protected Runnable onChanged = new Runnable() {
        @Override
        public void run() {
        }
    };
    protected final FaceAccessController faceAccessController = new FaceAccessController();

    private int tickInterval = 1;
    private int tickCounter = 0;
    private static final int IDLE_TICK_INTERVAL = 20;  // 空闲时每 20 tick（1 秒）唤醒一次

    private final SideRecipeStateAdapter recipeState = new SideRecipeStateAdapter();
    private final SidePortManager portManager;
    private final SideProcessorPersistence persistence;

    // 槽位集合缓存（由 initIOComponents 初始化）
    private Set<Integer> outputItemSlots = Set.of();
    private Set<Integer> inputItemSlots = Set.of();
    private Set<Integer> fluidSlots = Set.of();
    private Set<Integer> fluidInputSlots = Set.of();
    private Set<Integer> fluidOutputSlots = Set.of();
    private Set<Integer> energyOutputSlots = Set.of();

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

    public SlotPartition getPartition() {
        return partition;
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
        initSlotCache();
        initFaceAccess();
        this.portManager = new SidePortManager(level, pos, side, ioProcessor, items, tanks,
                outputItemSlots, fluidOutputSlots, energyOutputSlots, energyCapacity);
        this.persistence = new SideProcessorPersistence(zhenType.getType(), side, ioProcessor, recipeState);
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
            recipeState.markInputsChanged();
        });
    }

    private void initSlotCache() {
        inputItemSlots = Set.copyOf(partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL));
        outputItemSlots = Set.copyOf(partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL));
        fluidInputSlots = Set.copyOf(partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_INPUT_ALL));
        fluidOutputSlots = Set.copyOf(partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_OUTPUT_ALL));
        energyOutputSlots = Set.copyOf(partition.getSlots(ModIOTypes.ENERGY.get(), SlotZone.ENERGY_OUTPUT_ALL));
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

    public void scanAllPorts() {
        portManager.scanAll();
    }

    public void invalidatePorts() {
        portManager.invalidate();
    }

    public void tickRefreshPorts() {
        portManager.tickRefresh();
    }

    public @Nullable VirtualPort getPort(String name) {
        return portManager.getPort(name);
    }

    public Map<String, VirtualPort> getVirtualPorts() {
        return portManager.getPorts();
    }

    // ============ 面访问控制 ============

    @Override
    public @Nullable Map<IOType, Set<Integer>> getFaceAccess(Direction worldDirection) {
        Map<Direction, Map<IOType, Set<Integer>>> raw = faceAccessController.getIoFaceAccess();
        return raw.get(worldDirection);
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
        return recipeState.hasWork();
    }

    @Override
    public void tick() {
        recipeState.resolvePendingRecipes(zhenType.getType());
        tickRefreshPorts();
        if (++tickCounter % tickInterval != 0) {
            // 空闲稀释时，每 tick 检查是否变为活跃，保证唤醒延迟 ≤1 tick
            if (tickInterval > 1 && (hasWork() || hasPendingOutputs())) {
                tickCounter = 0;
                tickInterval = 1;
            } else {
                return;
            }
        }

        boolean isActive = hasWork() || zhenType.hasTickFactory() || hasPendingOutputs();

        // 动态 tickInterval：活跃时每 tick 运行，空闲时稀释频率
        if (isActive) {
            if (tickInterval > 1) tickInterval = 1;
        } else {
            tickInterval = IDLE_TICK_INTERVAL;
            return;  // 完全跳过 tick 本体
        }

        int tank0Before = tanks.isEmpty() ? -1 : (tanks.get(0).isEmpty() ? 0 : tanks.get(0).getAmount());

        if (hasWork()) {
            recipeState.processTick(level, pos, zhenType.getType(), partition,
                    items, tanks, ioProcessor, faceAccessController.getZoneFaceAccess(), onChanged);
        }

        if (portManager.hasConnectedPort()) {
            portManager.pushOutputs(onChanged);
        }

        int tank0After = tanks.isEmpty() ? -1 : (tanks.get(0).isEmpty() ? 0 : tanks.get(0).getAmount());
        if (tank0Before != tank0After) {
            MagicIO.LOGGER.trace("[tick] {} tank[0] {}→{}", pos.toShortString(), tank0Before, tank0After);
        }

        zhenType.execute(level, pos, level.getBlockState(pos), null);
    }

    private boolean hasPendingOutputs() {
        return portManager.hasConnectedPort() && portManager.hasPushableOutput();
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
        return recipeState.getProcessTime();
    }

    public cn.yhzcake.magicio.item.crafting.ProcessingStateSnapshot getProcessingStateSnapshot() {
        return recipeState.snapshot();
    }

    @Override
    public void setProcessTime(int time) {
        recipeState.setProcessTime(time);
    }

    @Override
    public boolean isInputsChanged() {
        return recipeState.isInputsChanged();
    }

    @Override
    public void setInputsChanged(boolean changed) {
        recipeState.setInputsChanged(changed);
    }

    public boolean hasActiveRecipe() {
        return recipeState.hasActiveRecipe();
    }

    public void applyClientState(int processTime, boolean hasRecipe) {
        recipeState.applyClientState(processTime, hasRecipe);
    }

    @Override
    public void writeToNBT(ValueOutput output) {
        persistence.write(output);
    }

    @Override
    public void readFromNBT(ValueInput input) {
        persistence.read(input);
    }

    @Override
    public void writeToStream(FriendlyByteBuf buf) {
        persistence.writeToStream(buf);
    }

    @Override
    public boolean readFromStream(FriendlyByteBuf buf) {
        return persistence.readFromStream(buf);
    }
}

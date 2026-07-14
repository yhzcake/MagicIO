package cn.yhzcake.magicio.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.inventory.FaceAccessController;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.io.EnergyIOComponent;
import cn.yhzcake.magicio.io.FluidIOComponent;
import cn.yhzcake.magicio.io.IOComponent;
import cn.yhzcake.magicio.io.IOProcessor;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ItemIOComponent;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.item.ModDataComponents;
import cn.yhzcake.magicio.item.crafting.RecipeProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class AbstractZhenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {

    // 字段
    private final ZhenType type;
    private SlotPartition partition;
    private RecipeProcessor.State state;
    private @Nullable RecipeHolder<?> lastRecipe;
    private final FaceAccessController faceAccessController = new FaceAccessController();
    private NonNullList<ItemStack> items;
    private NonNullList<FluidStack> tanks;
    private final @Nullable Integer tankCapacity;
    private final IOProcessor ioProcessor;
    private FluidIOComponent fluidIOComponent;
    private EnergyIOComponent energyIOComponent;
    private final @Nullable Integer energyCapacity;
    private @Nullable Identifier pendingCurrentRecipeId;
    private @Nullable Identifier pendingLastValidRecipeId;

    // 构造函数
    protected AbstractZhenBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), worldPosition, blockState);
        this.type = ZhenTypes.getType(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
        this.partition = this.type.getPartition();
        setItems(NonNullList.withSize(this.partition.getTotalSlots(ModIOTypes.ITEM.get()), ItemStack.EMPTY));
        this.tanks = NonNullList.withSize(this.partition.getTotalSlots(ModIOTypes.FLUID.get()), FluidStack.EMPTY);
        this.tankCapacity = this.type.getTankCapacity();
        this.ioProcessor = new IOProcessor();
        energyCapacity = type.getEnergyCapacity();

        initIOComponents();
        this.state = new RecipeProcessor.State();
        initFaceAccess();
    }

    private void initIOComponents() {
        ItemIOComponent itemIO = new ItemIOComponent(items, partition);
        fluidIOComponent = new FluidIOComponent(tanks, partition, tankCapacity);
        if (energyCapacity != null) {
            energyIOComponent = new EnergyIOComponent(energyCapacity, 1);
            ioProcessor.register(energyIOComponent);
        }
        ioProcessor.register(itemIO);
        ioProcessor.register(fluidIOComponent);
        ioProcessor.registerChangeCallback(() -> {
            setChanged();
            state.inputsChanged = true;
        });
    }

    public IOProcessor getIOProcessor() {
        return ioProcessor;
    }

    public ItemIOComponent getItemIOComponent() {
        return (ItemIOComponent) (IOComponent<?, ?>) ioProcessor.get(ModIOTypes.ITEM.get());
    }

    public FluidIOComponent getFluidIOComponent() {
        return fluidIOComponent;
    }

    public cn.yhzcake.magicio.item.crafting.ProcessingStateSnapshot getProcessingStateSnapshot() {
        return RecipeProcessor.snapshot(state);
    }

    // ===== 容器基础方法 =====
    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    private Set<Integer> getIOFaceSlots(Direction direction, IOType type) {
        return faceAccessController.getIOFaceSlots(direction, type);
    }

    // ===== 面访问控制 =====
    public void initFaceAccess() {
        Map<Direction, Map<IOType, Set<Integer>>> defined = type.getFaceAccess();
        if (!defined.isEmpty()) {
            for (var dirEntry : defined.entrySet()) {
                Direction dir = dirEntry.getKey();
                for (var typeEntry : dirEntry.getValue().entrySet()) {
                    faceAccessController.setSlotsForFace(dir, typeEntry.getKey(), typeEntry.getValue());
                }
            }
            return;
        }
        faceAccessController.setSlotsForFace(Direction.UP, ModIOTypes.ITEM.get(), partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.DROP_OUTPUT));
        faceAccessController.setZoneForFace(Direction.UP, SlotZone.DROP_OUTPUT.getName());
        faceAccessController.setSlotsForFace(Direction.DOWN, ModIOTypes.ITEM.get(), partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL));
        faceAccessController.setZoneForFace(Direction.DOWN, SlotZone.ITEM_OUTPUT_ALL.getName());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            faceAccessController.setSlotsForFace(direction, ModIOTypes.ITEM.get(), partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL));
            faceAccessController.setZoneForFace(direction, SlotZone.ITEM_INPUT_ALL.getName());
        }
        faceAccessController.setSlotsForFace(Direction.UP, ModIOTypes.FLUID.get(), partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_INPUT_ALL));
        faceAccessController.setZoneForFace(Direction.UP, SlotZone.FLUID_INPUT_ALL.getName());
        faceAccessController.setSlotsForFace(Direction.DOWN, ModIOTypes.FLUID.get(), partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_OUTPUT_ALL));
        faceAccessController.setZoneForFace(Direction.DOWN, SlotZone.FLUID_OUTPUT_ALL.getName());
        if (energyCapacity != null) {
            for (Direction direction : Direction.values()) {
                faceAccessController.setSlotsForFace(direction, ModIOTypes.ENERGY.get(), partition.getSlots(ModIOTypes.ENERGY.get(), SlotZone.ENERGY_INPUT_ALL));
                faceAccessController.setSlotsForFace(direction, ModIOTypes.ENERGY.get(), partition.getSlots(ModIOTypes.ENERGY.get(), SlotZone.ENERGY_OUTPUT_ALL));
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == null) return new int[0];
        Set<Integer> slots = getIOFaceSlots(direction, ModIOTypes.ITEM.get());
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getTanksForFace(Direction direction) {
        if (direction == null) return new int[0];
        Set<Integer> slots = getIOFaceSlots(direction, ModIOTypes.FLUID.get());
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean isInput(int slot) {
        return partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL).contains(slot);
    }

    public boolean isOutput(int slot) {
        return partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL).contains(slot);
    }

    /** 该方向允许外部输入（插入）的槽位 = 面访问 ∩ 输入区 */
    public Set<Integer> getInsertSlots(Direction direction) {
        Set<Integer> faceSlots = getIOFaceSlots(direction, ModIOTypes.ITEM.get());
        Set<Integer> inputSlots = partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL);
        Set<Integer> result = new java.util.HashSet<>(faceSlots);
        result.retainAll(inputSlots);
        return result;
    }

    /** 该方向允许外部输出（提取）的槽位 = 面访问 ∩ 输出区 */
    public Set<Integer> getExtractSlots(Direction direction) {
        Set<Integer> faceSlots = getIOFaceSlots(direction, ModIOTypes.ITEM.get());
        Set<Integer> outputSlots = partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL);
        Set<Integer> result = new java.util.HashSet<>(faceSlots);
        result.retainAll(outputSlots);
        return result;
    }

    /** 该方向允许外部插入流体的罐位 = 面访问 ∩ 流体输入区 */
    public Set<Integer> getInsertFluidSlots(Direction direction) {
        Set<Integer> faceSlots = getIOFaceSlots(direction, ModIOTypes.FLUID.get());
        Set<Integer> inputSlots = partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_INPUT_ALL);
        Set<Integer> result = new java.util.HashSet<>(faceSlots);
        result.retainAll(inputSlots);
        return result;
    }

    /** 该方向允许外部提取流体的罐位 = 面访问 ∩ 流体输出区 */
    public Set<Integer> getExtractFluidSlots(Direction direction) {
        Set<Integer> faceSlots = getIOFaceSlots(direction, ModIOTypes.FLUID.get());
        Set<Integer> outputSlots = partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_OUTPUT_ALL);
        Set<Integer> result = new java.util.HashSet<>(faceSlots);
        result.retainAll(outputSlots);
        return result;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        if (direction == null) return false;
        Set<Integer> slots = getIOFaceSlots(direction, ModIOTypes.ITEM.get());
        return slots.contains(slot) && isInput(slot);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        if (direction == null) return false;
        Set<Integer> slots = getIOFaceSlots(direction, ModIOTypes.ITEM.get());
        return slots.contains(slot) && isOutput(slot);
    }

    public Map<Direction, Set<Integer>> getItemFaceAccess() {
        return faceAccessController.getFaceSlotsForType(ModIOTypes.ITEM.get());
    }

    public Map<Direction, Set<Integer>> getFluidFaceAccess() {
        return faceAccessController.getFaceSlotsForType(ModIOTypes.FLUID.get());
    }

    public Map<Direction, Set<Integer>> getEnergyFaceAccess() {
        return faceAccessController.getFaceSlotsForType(ModIOTypes.ENERGY.get());
    }

    public void setItemFaceAccess(Map<Direction, Set<Integer>> faceAccess) {
        faceAccessController.setItemFaceAccess(faceAccess);
    }

    public void setFluidFaceAccess(Map<Direction, Set<Integer>> fluidFaceAccess) {
        faceAccessController.setFluidFaceAccess(fluidFaceAccess);
    }

    public void setSlotsForFace(Direction direction, Set<Integer> slots) {
        faceAccessController.setSlotsForFace(direction, slots);
    }

    public void setSlotsForFace(Direction direction) {
        faceAccessController.setSlotsForFace(direction);
    }

    public void addSlotsToFaceAccess(Direction direction, Set<Integer> slots) {
        faceAccessController.addSlotsToFaceAccess(direction, slots);
    }

    public void addSlotToFaceAccess(Direction direction, int slot) {
        faceAccessController.addSlotToFaceAccess(direction, slot);
    }

    public void removeSlotFromFaceAccess(Direction direction, int slot) {
        faceAccessController.removeSlotFromFaceAccess(direction, slot);
    }

    public void removeSlotsFromFaceAccess(Direction direction, Set<Integer> slots) {
        faceAccessController.removeSlotsFromFaceAccess(direction, slots);
    }

    public void clearItemFaceAccess() {
        faceAccessController.clearType(ModIOTypes.ITEM.get());
    }

    public void clearFluidFaceAccess() {
        faceAccessController.clearType(ModIOTypes.FLUID.get());
    }

    public void addFluidSlotsToFaceAccess(Direction direction, Set<Integer> tanks) {
        faceAccessController.addFluidSlotsToFaceAccess(direction, tanks);
    }

    public void removeFluidSlotsFromFaceAccess(Direction direction) {
        faceAccessController.removeFluidSlotsFromFaceAccess(direction);
    }

    public Map<Direction, Set<String>> getZoneFaceAccess() {
        return faceAccessController.getZoneFaceAccess();
    }

    public void setZoneForFace(Direction direction, String zoneName) {
        faceAccessController.setZoneForFace(direction, zoneName);
    }

    public void removeZoneFromFace(Direction direction, String zoneName) {
        faceAccessController.removeZoneFromFace(direction, zoneName);
    }

    // ===== Zone 物品查询 =====
    public List<ItemStack> getItemsInZone(SlotZone zone) {
        Set<Integer> slots = partition.getSlots(ModIOTypes.ITEM.get(), zone);
        List<ItemStack> result = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    public boolean isItemZoneEmpty(SlotZone zone) {
        return getItemsInZone(zone).isEmpty();
    }

    public boolean isItemZoneFull(SlotZone zone) {
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    // ===== 物品插入提取 =====
    public ItemStack insertItem(SlotZone zone, ItemStack stack, boolean simulate) {
        return getItemIOComponent().insertItem(zone, stack, simulate);
    }

    public ItemStack extractItem(SlotZone zone, int slot, int amount, boolean simulate) {
        return getItemIOComponent().extractItem(zone, slot, amount, simulate);
    }

    public ItemStack extractItem(SlotZone zone, int amount, boolean simulate) {
        return getItemIOComponent().extractItem(zone, amount, simulate);
    }

    // ===== 流体 Zone 查询 =====
    public List<FluidStack> getFluidsInZone(SlotZone zone) {
        Set<Integer> tankSlots = partition.getSlots(ModIOTypes.FLUID.get(), zone);
        List<FluidStack> result = new ArrayList<>();
        for (int tank : tankSlots) {
            FluidStack fluid = tanks.get(tank);
            if (!fluid.isEmpty()) {
                result.add(fluid);
            }
        }
        return result;
    }

    public boolean isFluidZoneEmpty(SlotZone zone) {
        return getFluidsInZone(zone).isEmpty();
    }

    // ===== 流体插入提取 =====
    public int insertFluid(SlotZone zone, FluidStack fluid, boolean simulate) {
        if (fluid.isEmpty() || tankCapacity == null) return 0;
        Set<Integer> tankSlots = partition.getSlots(ModIOTypes.FLUID.get(), zone);

        int filled = 0;
        FluidStack toFill = fluid.copy();

        for (int tank : tankSlots) {
            if (toFill.isEmpty()) break;
            FluidStack existing = tanks.get(tank);
            if (existing.isEmpty()) {
                int canInsert = Math.min(toFill.getAmount(), tankCapacity);
                if (!simulate) {
                    tanks.set(tank, toFill.copyWithAmount(canInsert));
                    setChanged();
                }
                filled += canInsert;
                toFill.shrink(canInsert);
            } else if (FluidStack.isSameFluid(existing, toFill)) {
                int canInsert = Math.min(toFill.getAmount(), tankCapacity - existing.getAmount());
                if (canInsert > 0) {
                    if (!simulate) {
                        existing.grow(canInsert);
                        setChanged();
                    }
                    filled += canInsert;
                    toFill.shrink(canInsert);
                }
            }
        }

        return filled;
    }

    public FluidStack extractFluid(SlotZone zone, int slot, int amount, boolean simulate) {
        Set<Integer> fluidSlots = partition.getSlots(ModIOTypes.FLUID.get(), zone);
        if (!fluidSlots.contains(slot) || amount <= 0) return FluidStack.EMPTY;

        FluidStack existing = tanks.get(slot);
        if (existing.isEmpty()) return FluidStack.EMPTY;

        int drained = Math.min(amount, existing.getAmount());
        FluidStack result = existing.copyWithAmount(drained);

        if (!simulate) {
            existing.shrink(drained);
            if (existing.isEmpty()) {
                tanks.set(slot, FluidStack.EMPTY);
            }
            setChanged();
        }

        return result;
    }

    public FluidStack extractFluid(SlotZone zone, int amount, boolean simulate) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
            FluidStack existing = tanks.get(tank);
            if (!existing.isEmpty()) {
                return extractFluid(zone, tank, Math.min(amount, existing.getAmount()), simulate);
            }
        }
        return FluidStack.EMPTY;
    }

    // ===== 流体容量预检（与 canFitItem / canConsumeItem 对称） =====
    public boolean canFitFluid(SlotZone zone, FluidStack fluid) {
        if (fluid.isEmpty() || tankCapacity == null) return false;
        return insertFluid(zone, fluid.copy(), true) >= fluid.getAmount();
    }

    public boolean canFitFluidZoneOutputs(Map<String, NonNullList<FluidStack>> fluidOutputs) {
        for (Map.Entry<String, NonNullList<FluidStack>> entry : fluidOutputs.entrySet()) {
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            for (FluidStack fluid : entry.getValue()) {
                if (!fluid.isEmpty() && !canFitFluid(zone, fluid)) return false;
            }
        }
        return true;
    }

    public void produceFluid(SlotZone zone, NonNullList<FluidStack> fluids) {
        fluidIOComponent.produceFluid(zone, fluids);
    }

    // ===== 配方处理 =====
    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        lastRecipe = recipe;
    }

    @Override
    public @Nullable RecipeHolder<?> getRecipeUsed() {
        return lastRecipe;
    }

    public boolean canFitItem(NonNullList<ItemStack> outputs) {
        return canFitItem(SlotZone.ITEM_OUTPUT_ALL, outputs);
    }

    public boolean canFitItem(SlotZone zone, NonNullList<ItemStack> outputs) {
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !insertItem(zone, stack.copy(), true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void produceItem(NonNullList<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                insertItem(SlotZone.ITEM_OUTPUT_ALL, output.copy(), false);
            }
        }
    }

    public void produceItem(SlotZone zone, NonNullList<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                insertItem(zone, output.copy(), false);
            }
        }
    }

    // ===== Tick =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity be) {
        be.resolvePendingRecipes();
        RecipeProcessor.processTick(
                level, pos, be.state, be.type.getType(),
                be.partition, be.items, be.tanks,
                be.ioProcessor, be.faceAccessController.getZoneFaceAccess(),
                false,
                be::setChanged);

        be.type.execute(level, pos, state, be);
    }

    public NonNullList<FluidStack> getFluidTanks() {
        return fluidIOComponent != null ? fluidIOComponent.getTanks() : tanks;
    }

    public @Nullable Integer getFluidTankCapacity() {
        return fluidIOComponent != null ? fluidIOComponent.getTankCapacity() : tankCapacity;
    }

    public EnergyIOComponent getEnergyIOComponent() {
        return energyIOComponent;
    }

    public @Nullable Integer getEnergyCapacity() {
        return energyCapacity;
    }

    // ===== 数据持久化 & 网络同步 =====
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("process_time", state.processTime);
        output.putInt("effective_processing_time", state.effectiveProcessingTime);
        output.putDouble("output_multiplier", state.outputMultiplier);
        output.putInt("recipe_generation", state.recipeGeneration);
        output.putLong("state_revision", state.stateRevision);
        output.putLong("cycle_id", state.cycleId);
        output.putBoolean("output_blocked", state.outputBlocked);
        if (state.currentRecipe != null) {
            output.putString("current_recipe_id", state.currentRecipe.getRecipeId().toString());
        }
        if (state.lastValidRecipe != null) {
            output.putString("last_valid_recipe_id", state.lastValidRecipe.getRecipeId().toString());
        }
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            component.saveNBT(output);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (level != null && level.isClientSide()) {
            state.processTime = input.getIntOr("process_time", 0);
            return;
        }
        state.processTime = input.getIntOr("process_time", 0);
        state.effectiveProcessingTime = input.getIntOr("effective_processing_time", 0);
        state.outputMultiplier = input.getDoubleOr("output_multiplier", 1.0);
        state.recipeGeneration = input.getIntOr("recipe_generation", 0);
        state.stateRevision = input.getLongOr("state_revision", 0);
        state.cycleId = input.getLongOr("cycle_id", 0);
        state.outputBlocked = input.getBooleanOr("output_blocked", false);
        input.getString("current_recipe_id").ifPresent(idStr -> {
            try {
                pendingCurrentRecipeId = Identifier.parse(idStr);
            } catch (Exception ignored) {}
        });
        input.getString("last_valid_recipe_id").ifPresent(idStr -> {
            try {
                pendingLastValidRecipeId = Identifier.parse(idStr);
            } catch (Exception ignored) {}
        });
        state.inputsChanged = true;
        resolvePendingRecipes();
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            component.loadNBT(input);
        }
    }

    private void resolvePendingRecipes() {
        if (pendingCurrentRecipeId == null && pendingLastValidRecipeId == null) return;
        var manager = cn.yhzcake.magicio.item.crafting.ZhenRecipeManager.getInstance();
        if (!manager.isLoaded()) return;
        if (pendingCurrentRecipeId != null) {
            state.currentRecipe = manager.getRecipe(pendingCurrentRecipeId);
            pendingCurrentRecipeId = null;
        }
        if (pendingLastValidRecipeId != null) {
            state.lastValidRecipe = manager.getRecipe(pendingLastValidRecipeId);
            pendingLastValidRecipeId = null;
        }
        if (state.currentRecipe != null) {
            RecipeProcessor.computeEffectiveValues(state, type.getType());
            if (state.recipeGeneration != cn.yhzcake.magicio.item.crafting.ZhenRecipeManager.getRecipeGeneration()) {
                state.inputsChanged = true;
                state.recipeGeneration = cn.yhzcake.magicio.item.crafting.ZhenRecipeManager.getRecipeGeneration();
            }
        } else if (pendingCurrentRecipeId == null) {
            state.processTime = 0;
            state.effectiveProcessingTime = 0;
            state.outputBlocked = false;
            state.stateRevision++;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("process_time", state.processTime);
        tag.putBoolean("has_recipe", state.currentRecipe != null);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (type != null) {
            Identifier typeId = ZhenType.ZHEN_TYPES.getKey(type);
            if (typeId != null) {
                components.set(ModDataComponents.ZHEN_TYPE, typeId.toString());
            }
        }
    }

    // ===== 接口桩方法 =====
    @Override
    public void fillStackedContents(StackedItemContents contents) {
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL)) {
            contents.accountStack(items.get(slot));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.magic_io." + type.getType());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        // TODO 
        return null;
    }

}

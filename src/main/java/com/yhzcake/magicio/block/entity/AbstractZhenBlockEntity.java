package com.yhzcake.magicio.block.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.block.zhen.ZhenType;
import com.yhzcake.magicio.block.zhen.ZhenTypes;
import com.yhzcake.magicio.item.ModDataComponents;
import com.yhzcake.magicio.item.crafting.ModRecipeManager;
import com.yhzcake.magicio.item.crafting.ZhenRecipe;
import com.yhzcake.magicio.item.crafting.ZhenRecipeInput;
import com.yhzcake.magicio.item.crafting.ZhenRecipeManager;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeManager.CachedCheck;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@SuppressWarnings("unused")
public abstract class AbstractZhenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {

    // 字段
    private final ZhenType type;
    private SlotPartition partition;
    private static final int PROCESS_COLL_SPEED = 2;
    private final RecipeType<? extends ZhenRecipe> recipeType;
    private final CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> quickCheck;
    private ZhenRecipe currentRecipe;
    private int processTime = 0;
    private boolean inputsChanged = false;
    private @Nullable RecipeHolder<?> lastRecipe;
    private boolean autoInput = false;
    private boolean autoOutput = false;
    private Map<Direction, Set<Integer>> itemFaceAccess = new EnumMap<>(Direction.class);
    private Map<Direction, Set<String>> zoneFaceAccess = new EnumMap<>(Direction.class);
    private NonNullList<ItemStack> items;
    private NonNullList<FluidStack> tanks;
    private Map<Direction, Set<Integer>> fluidFaceAccess = new EnumMap<>(Direction.class);
    private final @Nullable Integer tankCapacity;

    // 构造函数
    protected AbstractZhenBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), worldPosition, blockState);
        this.recipeType = ModRecipeManager.ZHEN_RECIPE.get();
        this.quickCheck = RecipeManager.createCheck(this.recipeType);
        this.type = ZhenTypes.getType(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
        this.partition = this.type.getPartition();
        setItems(NonNullList.withSize(this.partition.getTotalSlots(), ItemStack.EMPTY));
        this.tanks = NonNullList.withSize(this.partition.getTotalTanks(), FluidStack.EMPTY);
        this.tankCapacity = this.type.getTankCapacity();
        initFaceAccess();
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

    // ===== 面访问控制 =====
    public void initFaceAccess() {
        addSlotsToFaceAccess(Direction.UP, SlotZone.DROP_OUTPUT);
        addSlotsToFaceAccess(Direction.DOWN, SlotZone.ITEM_OUTPUT_ALL);
        addSlotsToFaceAccess(Direction.Plane.HORIZONTAL, SlotZone.ITEM_INPUT_ALL);
        addFluidSlotsToFaceAccess(Direction.UP, SlotZone.FLUID_INPUT_ALL);
        addFluidSlotsToFaceAccess(Direction.DOWN, SlotZone.FLUID_OUTPUT_ALL);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == null) return new int[0];
        Set<Integer> slots = itemFaceAccess.get(direction);
        if (slots == null || slots.isEmpty()) {
            return new int[0];
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean isInput(int slot) {
        return partition.getSlots(SlotZone.ITEM_INPUT_ALL).contains(slot) || partition.getTanks(SlotZone.FLUID_INPUT_ALL).contains(slot);
    }

    public boolean isOutput(int slot) {
        return partition.getSlots(SlotZone.ITEM_OUTPUT_ALL).contains(slot) || partition.getTanks(SlotZone.FLUID_OUTPUT_ALL).contains(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        if (direction == null) return false;
        Set<Integer> slots = itemFaceAccess.get(direction);
        return slots != null && slots.contains(slot) && isInput(slot);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        if (direction == null) return false;
        Set<Integer> slots = itemFaceAccess.get(direction);
        return slots != null && slots.contains(slot) && isOutput(slot);
    }

    public Map<Direction, Set<Integer>> getItemFaceAccess() {
        return itemFaceAccess;
    }

    public void setItemFaceAccess(Map<Direction, Set<Integer>> faceAccess) {
        this.itemFaceAccess = new EnumMap<>(faceAccess);
        this.zoneFaceAccess.clear();
    }

    public void setSlotsForFace(Direction direction, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            itemFaceAccess.remove(direction);
            zoneFaceAccess.remove(direction);
        } else {
            itemFaceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void setSlotsForFace(Direction direction, SlotZone zone) {
        setSlotsForFace(direction, partition.getSlots(zone));
        setZoneForFace(direction, zone.getName());
    }

    public void setSlotsForFace(Direction direction) {
        itemFaceAccess.remove(direction);
        zoneFaceAccess.remove(direction);
    }

    public void addSlotToFaceAccess(Direction direction, int slot) {
        Set<Integer> slots = new java.util.HashSet<>(itemFaceAccess.getOrDefault(direction, Set.of()));
        slots.add(slot);
        itemFaceAccess.put(direction, Set.copyOf(slots));
    }

    public void addSlotsToFaceAccess(Direction direction, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            itemFaceAccess.remove(direction);
            zoneFaceAccess.remove(direction);
        } else {
            itemFaceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void addSlotsToFaceAccess(Direction.Plane directions, SlotZone zone) {
        for (Direction direction : directions) {
            addSlotsToFaceAccess(direction, zone);
        }
    }

    public void addSlotsToFaceAccess(Direction direction, SlotZone zone) {
        addSlotsToFaceAccess(direction, partition.getSlots(zone));
        setZoneForFace(direction, zone.getName());
    }

    public void removeSlotFromFaceAccess(Direction direction, int slot) {
        Set<Integer> slots = new java.util.HashSet<>(itemFaceAccess.getOrDefault(direction, Set.of()));
        slots.remove(slot);
        if (slots.isEmpty()) {
            itemFaceAccess.remove(direction);
            zoneFaceAccess.remove(direction);
        } else {
            itemFaceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void removeSlotsFromFaceAccess(Direction direction, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            itemFaceAccess.remove(direction);
            zoneFaceAccess.remove(direction);
        } else {
            itemFaceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void removeSlotsFromFaceAccess(Direction direction, SlotZone zone) {
        removeSlotsFromFaceAccess(direction, partition.getSlots(zone));
        removeZoneFromFace(direction, zone.getName());
    }

    public void clearItemFaceAccess() {
        itemFaceAccess.clear();
        zoneFaceAccess.clear();
    }

    public Map<Direction, Set<String>> getZoneFaceAccess() {
        return zoneFaceAccess;
    }

    public void setZoneForFace(Direction direction, String zoneName) {
        Set<String> zones = zoneFaceAccess.getOrDefault(direction, new java.util.HashSet<>());
        zones.add(zoneName);
        zoneFaceAccess.put(direction, Set.copyOf(zones));
    }

    public void removeZoneFromFace(Direction direction, String zoneName) {
        Set<String> zones = zoneFaceAccess.getOrDefault(direction, new java.util.HashSet<>());
        zones.remove(zoneName);
        if (zones.isEmpty()) {
            zoneFaceAccess.remove(direction);
        } else {
            zoneFaceAccess.put(direction, Set.copyOf(zones));
        }
    }

    // ===== 流体面访问控制 =====

    public int[] getTanksForFace(Direction direction) {
        if (direction == null) return new int[0];
        Set<Integer> tanks = fluidFaceAccess.get(direction);
        if (tanks == null || tanks.isEmpty()) {
            return new int[0];
        }
        return tanks.stream().mapToInt(Integer::intValue).toArray();
    }

    public Map<Direction, Set<Integer>> getFluidFaceAccess() {
        return fluidFaceAccess;
    }

    public void setFluidFaceAccess(Map<Direction, Set<Integer>> fluidFaceAccess) {
        this.fluidFaceAccess = new EnumMap<>(fluidFaceAccess);
    }

    public void addFluidSlotsToFaceAccess(Direction direction, Set<Integer> tanks) {
        if (tanks == null || tanks.isEmpty()) {
            fluidFaceAccess.remove(direction);
        } else {
            fluidFaceAccess.put(direction, Set.copyOf(tanks));
        }
    }

    public void addFluidSlotsToFaceAccess(Direction direction, SlotZone zone) {
        addFluidSlotsToFaceAccess(direction, partition.getTanks(zone));
    }

    public void addFluidSlotsToFaceAccess(Direction.Plane directions, SlotZone zone) {
        for (Direction direction : directions) {
            addFluidSlotsToFaceAccess(direction, zone);
        }
    }

    public void removeFluidSlotsFromFaceAccess(Direction direction) {
        fluidFaceAccess.remove(direction);
    }

    public void clearFluidFaceAccess() {
        fluidFaceAccess.clear();
    }

    // ===== Zone 物品查询 =====
    public List<ItemStack> getItemsInZone(SlotZone zone) {
        Set<Integer> slots = partition.getSlots(zone);
        List<ItemStack> result = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    public boolean isZoneEmpty(SlotZone zone) {
        return getItemsInZone(zone).isEmpty();
    }

    public boolean isZoneFull(SlotZone zone) {
        for (int slot : partition.getSlots(zone)) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    // ===== 物品插入提取 =====
    public ItemStack insertItem(SlotZone zone, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack remaining = stack.copy();
        Set<Integer> slots = partition.getSlots(zone);

        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int canInsert = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canInsert > 0) {
                    if (!simulate) {
                        existing.grow(canInsert);
                        setChanged();
                    }
                    remaining.shrink(canInsert);
                }
            }
        }

        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                ItemStack placed = remaining.split(remaining.getCount());
                if (!simulate) {
                    items.set(slot, placed);
                    setChanged();
                }
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    public ItemStack extractItem(SlotZone zone, int slot, int amount, boolean simulate) {
        Set<Integer> slots = partition.getSlots(zone);
        if (!slots.contains(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int extracted = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(extracted);
        if (!simulate) {
            existing.shrink(extracted);
            if (existing.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            setChanged();
        }
        return result;
    }

    public ItemStack extractItem(SlotZone zone, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        for (int slot : partition.getSlots(zone)) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty()) {
                return extractItem(zone, slot, Math.min(amount, existing.getCount()), simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    // ===== 流体 Zone 查询 =====
    public List<FluidStack> getFluidsInZone(SlotZone zone) {
        Set<Integer> tankSlots = partition.getTanks(zone);
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
    public int fillTank(SlotZone zone, FluidStack fluid, boolean simulate) {
        if (fluid.isEmpty() || tankCapacity == null) return 0;
        Set<Integer> tankSlots = partition.getTanks(zone);

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

    public FluidStack drainTank(SlotZone zone, int tank, int amount, boolean simulate) {
        Set<Integer> tankSlots = partition.getTanks(zone);
        if (!tankSlots.contains(tank) || amount <= 0) return FluidStack.EMPTY;

        FluidStack existing = tanks.get(tank);
        if (existing.isEmpty()) return FluidStack.EMPTY;

        int drained = Math.min(amount, existing.getAmount());
        FluidStack result = existing.copyWithAmount(drained);

        if (!simulate) {
            existing.shrink(drained);
            if (existing.isEmpty()) {
                tanks.set(tank, FluidStack.EMPTY);
            }
            setChanged();
        }

        return result;
    }

    public FluidStack drainTank(SlotZone zone, int amount, boolean simulate) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (int tank : partition.getTanks(zone)) {
            FluidStack existing = tanks.get(tank);
            if (!existing.isEmpty()) {
                return drainTank(zone, tank, Math.min(amount, existing.getAmount()), simulate);
            }
        }
        return FluidStack.EMPTY;
    }

    // ===== 流体容量预检（与物品 canFitOutput / canConsume 对称） =====
    public boolean canFillFluidOutput(SlotZone zone, FluidStack fluid) {
        if (fluid.isEmpty() || tankCapacity == null) return false;
        return fillTank(zone, fluid.copy(), true) >= fluid.getAmount();
    }

    public boolean canFitFluidZoneOutputs(Map<String, NonNullList<FluidStack>> fluidOutputs) {
        for (Map.Entry<String, NonNullList<FluidStack>> entry : fluidOutputs.entrySet()) {
            SlotZone zone = partition.getTankZoneByName(entry.getKey());
            if (zone == null) return false;
            for (FluidStack fluid : entry.getValue()) {
                if (!fluid.isEmpty() && !canFillFluidOutput(zone, fluid)) return false;
            }
        }
        return true;
    }

    public boolean canDrainFluidInput(SlotZone zone, ZhenRecipe.FluidIngredient ingredient) {
        if (ingredient == null || ingredient.amount() <= 0) return false;
        for (int tank : partition.getTanks(zone)) {
            if (ingredient.test(tanks.get(tank))) {
                return true;
            }
        }
        return false;
    }

    public boolean canDrainAllFluidInputs(Map<String, NonNullList<ZhenRecipe.FluidIngredient>> fluidInputs) {
        for (Map.Entry<String, NonNullList<ZhenRecipe.FluidIngredient>> entry : fluidInputs.entrySet()) {
            SlotZone zone = partition.getTankZoneByName(entry.getKey());
            if (zone == null) return false;
            for (ZhenRecipe.FluidIngredient fluid : entry.getValue()) {
                if (!canDrainFluidInput(zone, fluid)) return false;
            }
        }
        return true;
    }

    public void consumeFluidInputs(NonNullList<ZhenRecipe.FluidIngredient> ingredients, SlotZone zone) {
        for (ZhenRecipe.FluidIngredient ingredient : ingredients) {
            for (int tank : partition.getTanks(zone)) {
                FluidStack existing = tanks.get(tank);
                if (ingredient.test(existing)) {
                    drainTank(zone, tank, ingredient.amount(), false);
                    break;
                }
            }
        }
    }

    public void produceFluidOutputs(NonNullList<FluidStack> fluids, SlotZone zone) {
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                fillTank(zone, fluid.copy(), false);
            }
        }
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

    public boolean canFitOutput(NonNullList<ItemStack> outputs) {
        return canFitOutput(outputs, SlotZone.ITEM_OUTPUT_ALL);
    }

    public boolean canFitOutput(NonNullList<ItemStack> outputs, SlotZone zone) {
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !insertItem(zone, stack.copy(), true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean canFitOutput(Map<SlotZone, ItemStack> outputs) {
        for (Map.Entry<SlotZone, ItemStack> entry : outputs.entrySet()) {
            if (!entry.getValue().isEmpty() && !insertItem(entry.getKey(), entry.getValue().copy(), true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean canConsume(NonNullList<Ingredient> ingredients) {
        return canConsume(ingredients, SlotZone.ITEM_INPUT_ALL);
    }

    public boolean canConsume(NonNullList<Ingredient> ingredients, SlotZone zone) {
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int slot : partition.getSlots(zone)) {
                ItemStack simulated = extractItem(zone, slot, 1, true);
                if (!simulated.isEmpty() && ingredient.test(simulated)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public void produceOutputs(NonNullList<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                insertItem(SlotZone.ITEM_OUTPUT_ALL, output.copy(), false);
            }
        }
    }

    public void produceOutputs(NonNullList<ItemStack> outputs, SlotZone zone) {
        Map<SlotZone, ItemStack> map = new java.util.HashMap<>();
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                map.put(zone, output.copy());
            }
        }
        produceOutputs(map);
    }

    public void produceOutputs(Map<SlotZone, ItemStack> outputs) {
        for (Map.Entry<SlotZone, ItemStack> entry : outputs.entrySet()) {
            ItemStack stack = entry.getValue();
            if (!stack.isEmpty()) {
                insertItem(entry.getKey(), stack.copy(), false);
            }
        }
    }

    // ===== Tick =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        boolean needSync = false;

        if (blockEntity.currentRecipe == null) {
            if (blockEntity.inputsChanged && blockEntity.lastRecipe != null) {
                ZhenRecipe lastRecipe = (ZhenRecipe) blockEntity.lastRecipe.value();
                if (lastRecipe.matches(blockEntity.items, blockEntity.partition, level)
                        && lastRecipe.matchesFluid(blockEntity.tanks, blockEntity.partition)) {
                    blockEntity.currentRecipe = lastRecipe;
                    blockEntity.processTime = 0;
                    blockEntity.inputsChanged = false;
                    needSync = true;
                }
            }

            if (blockEntity.currentRecipe == null) {
                ZhenRecipe newRecipe = ZhenRecipeManager.getInstance().findRecipe(blockEntity.type.getType(), blockEntity.items, blockEntity.partition, level);
                if (newRecipe != null && newRecipe.matchesFluid(blockEntity.tanks, blockEntity.partition)) {
                    blockEntity.currentRecipe = newRecipe;
                    blockEntity.processTime = 0;
                    blockEntity.inputsChanged = false;
                    needSync = true;
                }
            }
        }

        if (blockEntity.currentRecipe != null) {
            blockEntity.processTime += 1;
            needSync = true;
            if (blockEntity.processTime >= blockEntity.currentRecipe.getProcessingTime()) {
                Map<String, NonNullList<ItemStack>> zoneOutputs = blockEntity.currentRecipe.rollOutput((ServerLevel) level);
                Map<String, NonNullList<FluidStack>> fluidOutputs = blockEntity.currentRecipe.rollFluidOutput();
                if (blockEntity.canFitZoneOutputs(zoneOutputs)
                        && blockEntity.canConsumeAllInputs(blockEntity.currentRecipe)
                        && blockEntity.canFitFluidZoneOutputs(fluidOutputs)
                        && blockEntity.canDrainAllFluidInputs(blockEntity.currentRecipe.getFluidZoneInputs())) {
                    for (Map.Entry<String, NonNullList<Ingredient>> entry : blockEntity.currentRecipe.getZoneInputs().entrySet()) {
                        SlotZone zone = blockEntity.partition.getZoneByName(entry.getKey());
                        if (zone != null) {
                            blockEntity.consumeInputs(entry.getValue(), zone);
                        }
                    }
                    for (Map.Entry<String, NonNullList<ZhenRecipe.FluidIngredient>> entry : blockEntity.currentRecipe.getFluidZoneInputs().entrySet()) {
                        SlotZone zone = blockEntity.partition.getTankZoneByName(entry.getKey());
                        if (zone != null) {
                            blockEntity.consumeFluidInputs(entry.getValue(), zone);
                        }
                    }
                    for (Map.Entry<String, NonNullList<ItemStack>> entry : zoneOutputs.entrySet()) {
                        if (entry.getKey().equals(SlotZone.DROP_OUTPUT.getName())) {
                            Direction dropDir = Direction.DOWN;
                            for (Direction dir : Direction.values()) {
                                if (blockEntity.zoneFaceAccess.getOrDefault(dir, Set.of()).contains(SlotZone.DROP_OUTPUT.getName())) {
                                    dropDir = dir;
                                    break;
                                }
                            }
                            Vec3 dropPos = Vec3.atCenterOf(pos.relative(dropDir));
                            for (ItemStack stack : entry.getValue()) {
                                ItemEntity item = new ItemEntity(level, dropPos.x, dropPos.y - 0.5, dropPos.z, stack, 0, 0, 0);
                                item.setDefaultPickUpDelay();
                                level.addFreshEntity(item);
                            }
                        } else {
                            SlotZone zone = blockEntity.partition.getZoneByName(entry.getKey());
                            if (zone != null) {
                                blockEntity.produceOutputs(entry.getValue(), zone);
                            }
                        }
                    }
                    for (Map.Entry<String, NonNullList<FluidStack>> entry : fluidOutputs.entrySet()) {
                        SlotZone zone = blockEntity.partition.getTankZoneByName(entry.getKey());
                        if (zone != null) {
                            blockEntity.produceFluidOutputs(entry.getValue(), zone);
                        }
                    }
                }
                blockEntity.processTime = 0;
                blockEntity.currentRecipe = null;
            }
        } else if (blockEntity.processTime > 0) {
            blockEntity.processTime = Math.max(0, blockEntity.processTime - PROCESS_COLL_SPEED);
            needSync = true;
        }

        if (needSync) {
            level.sendBlockUpdated(pos, state, state, 3);
        }

        if (blockEntity.type.getTickFactory() != null) {
            blockEntity.type.execute(level, pos, state, blockEntity);
        }
    }

    private boolean canFitZoneOutputs(Map<String, NonNullList<ItemStack>> zoneOutputs) {
        for (Map.Entry<String, NonNullList<ItemStack>> entry : zoneOutputs.entrySet()) {
            if (entry.getKey().equals(SlotZone.DROP_OUTPUT.getName())) continue;
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            if (!canFitOutput(entry.getValue(), zone)) return false;
        }
        return true;
    }

    private boolean canConsumeAllInputs(ZhenRecipe recipe) {
        for (Map.Entry<String, NonNullList<Ingredient>> entry : recipe.getZoneInputs().entrySet()) {
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            if (!canConsume(entry.getValue(), zone)) return false;
        }
        return true;
    }

    public boolean hasIngredients(NonNullList<Ingredient> ingredients) {
        return hasIngredients(ingredients, SlotZone.ITEM_INPUT_ALL);
    }

    public boolean hasIngredients(NonNullList<Ingredient> ingredients, SlotZone zone) {
        if (ingredients.isEmpty()) return true;

        Set<Integer> slots = partition.getSlots(zone);

        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int slot : slots) {
                if (ingredient.test(items.get(slot))) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public boolean consumeInputs(NonNullList<Ingredient> ingredients) {
        return consumeInputs(ingredients, SlotZone.ITEM_INPUT_ALL);
    }

    public boolean consumeInputs(NonNullList<Ingredient> ingredients, SlotZone zone) {
        if (ingredients.isEmpty()) return true;

        Set<Integer> slots = partition.getSlots(zone);

        for (Ingredient ingredient : ingredients) {
            boolean consumed = false;
            for (int slot : slots) {
                ItemStack existing = items.get(slot);
                if (ingredient.test(existing)) {
                    existing.shrink(1);
                    if (existing.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                    consumed = true;
                    break;
                }
            }
            if (!consumed) return false;
        }

        setChanged();
        inputsChanged = true;
        return true;
    }

    public NonNullList<FluidStack> getTanks() {
        return tanks;
    }

    public @Nullable Integer getTankCapacity() {
        return tankCapacity;
    }

    // ===== 数据持久化 & 网络同步 =====
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("process_time", processTime);
        for (int i = 0; i < tanks.size(); i++) {
            if (!tanks.get(i).isEmpty()) {
                output.store("FluidTank_" + i, FluidStack.CODEC, tanks.get(i));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        processTime = input.getIntOr("process_time", 0);
        for (int i = 0; i < tanks.size(); i++) {
            tanks.set(i, input.read("FluidTank_" + i, FluidStack.CODEC).orElse(FluidStack.EMPTY));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("process_time", processTime);
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
        for (ItemStack stack : items) {
            contents.accountStack(stack);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.magic_io." + type.getType());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createMenu'");
    }

}

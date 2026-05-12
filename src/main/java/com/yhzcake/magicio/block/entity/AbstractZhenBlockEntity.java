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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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
    private Map<Direction, Set<Integer>> faceAccess = new EnumMap<>(Direction.class);
    private Map<Direction, Set<String>> zoneFaceAccess = new EnumMap<>(Direction.class);
    private NonNullList<ItemStack> items;

    // 构造函数
    protected AbstractZhenBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), worldPosition, blockState);
        this.recipeType = ModRecipeManager.ZHEN_RECIPE.get();
        this.quickCheck = RecipeManager.createCheck(this.recipeType);
        this.type = ZhenTypes.getType(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
        this.partition = this.type.getPartition();
        setItems(NonNullList.withSize(this.partition.getTotalSlots(), ItemStack.EMPTY));
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
        addSlotsToFaceAccess(Direction.UP, SlotZone.INPUT_ALL);
        addSlotsToFaceAccess(Direction.DOWN, SlotZone.OUTPUT_ALL);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == null) return new int[0];
        Set<Integer> slots = faceAccess.get(direction);
        if (slots == null || slots.isEmpty()) {
            return new int[0];
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean isInput(int slot) {
        return partition.getSlots(SlotZone.INPUT_ALL).contains(slot);
    }

    public boolean isOutput(int slot) {
        return partition.getSlots(SlotZone.OUTPUT_ALL).contains(slot);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        if (direction == null) return false;
        Set<Integer> slots = faceAccess.get(direction);
        return slots != null && slots.contains(slot) && isInput(slot);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        if (direction == null) return false;
        Set<Integer> slots = faceAccess.get(direction);
        return slots != null && slots.contains(slot) && isOutput(slot);
    }

    public Map<Direction, Set<Integer>> getFaceAccess() {
        return faceAccess;
    }

    public void setFaceAccess(Map<Direction, Set<Integer>> faceAccess) {
        this.faceAccess = new EnumMap<>(faceAccess);
    }

    public void setSlotsForFace(Direction direction, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            faceAccess.remove(direction);
        } else {
            faceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void setSlotsForFace(Direction direction, SlotZone zone) {
        setSlotsForFace(direction, partition.getSlots(zone));
    }

    public void setSlotsForFace(Direction direction) {
        faceAccess.remove(direction);
    }

    public void addSlotToFaceAccess(Direction direction, int slot) {
        Set<Integer> slots = new java.util.HashSet<>(faceAccess.getOrDefault(direction, Set.of()));
        slots.add(slot);
        faceAccess.put(direction, Set.copyOf(slots));
    }

    public void addSlotsToFaceAccess(Direction direction, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            faceAccess.remove(direction);
        } else {
            faceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void addSlotsToFaceAccess(Direction direction, SlotZone zone) {
        addSlotsToFaceAccess(direction, partition.getSlots(zone));
    }

    public void removeSlotFromFaceAccess(Direction direction, int slot) {
        Set<Integer> slots = new java.util.HashSet<>(faceAccess.getOrDefault(direction, Set.of()));
        slots.remove(slot);
        if (slots.isEmpty()) {
            faceAccess.remove(direction);
        } else {
            faceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void removeSlotsFromFaceAccess(Direction direction, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            faceAccess.remove(direction);
        } else {
            faceAccess.put(direction, Set.copyOf(slots));
        }
    }

    public void removeSlotsFromFaceAccess(Direction direction, SlotZone zone) {
        removeSlotsFromFaceAccess(direction, partition.getSlots(zone));
    }

    public void clearFaceAccess() {
        faceAccess.clear();
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
    public ItemStack insertItem(SlotZone zone, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack remaining = stack.copy();
        Set<Integer> slots = partition.getSlots(zone);

        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int canInsert = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canInsert > 0) {
                    existing.grow(canInsert);
                    remaining.shrink(canInsert);
                    setChanged();
                }
            }
        }

        for (int slot : slots) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                ItemStack placed = remaining.split(remaining.getCount());
                items.set(slot, placed);
                setChanged();
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    public ItemStack extractItem(SlotZone zone, int slot, int amount) {
        Set<Integer> slots = partition.getSlots(zone);
        if (!slots.contains(slot) || amount <= 0) return ItemStack.EMPTY;
        return removeItem(slot, amount);
    }

    public ItemStack extractItem(SlotZone zone, int amount) {
        if (amount <= 0) return ItemStack.EMPTY;
        for (int slot : partition.getSlots(zone)) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                return removeItem(slot, Math.min(amount, stack.getCount()));
            }
        }
        return ItemStack.EMPTY;
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
        if (outputs.isEmpty()) return true;

        Set<Integer> outputSlots = partition.getSlots(SlotZone.OUTPUT_ALL);

        int availableEmptySlots = 0;
        for (int slot : outputSlots) {
            if (items.get(slot).isEmpty()) {
                availableEmptySlots++;
            }
        }

        int neededNewSlots = 0;
        for (ItemStack output : outputs) {
            if (output.isEmpty()) continue;

            boolean canStack = false;
            for (int slot : outputSlots) {
                ItemStack existing = items.get(slot);
                if (!existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, output)
                        && existing.getCount() + output.getCount() <= existing.getMaxStackSize()) {
                    canStack = true;
                    break;
                }
            }
            if (!canStack) {
                neededNewSlots++;
            }
        }

        return neededNewSlots <= availableEmptySlots;
    }

    public boolean canFitOutput(Map<SlotZone, ItemStack> outputs) {
        if (outputs.isEmpty()) return true;

        for (Map.Entry<SlotZone, ItemStack> entry : outputs.entrySet()) {
            SlotZone zone = entry.getKey();
            ItemStack stack = entry.getValue();
            if (stack.isEmpty()) continue;

            Set<Integer> slots = partition.getSlots(zone);
            int remaining = stack.getCount();

            for (int slot : slots) {
                if (remaining <= 0) break;
                ItemStack existing = items.get(slot);
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    remaining -= existing.getMaxStackSize() - existing.getCount();
                }
            }

            for (int slot : slots) {
                if (remaining <= 0) break;
                if (items.get(slot).isEmpty()) {
                    remaining -= Math.min(remaining, stack.getMaxStackSize());
                }
            }

            if (remaining > 0) return false;
        }
        return true;
    }

    public void produceOutputs(NonNullList<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                insertItem(SlotZone.OUTPUT_ALL, output.copy());
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
                insertItem(entry.getKey(), stack.copy());
            }
        }
    }

    // ===== Tick =====
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        boolean needSync = false;

        if (blockEntity.currentRecipe == null) {
            if (blockEntity.inputsChanged && blockEntity.lastRecipe != null) {
                ZhenRecipe lastRecipe = (ZhenRecipe) blockEntity.lastRecipe.value();
                if (lastRecipe.matches(blockEntity.items, blockEntity.partition, level)) {
                    blockEntity.currentRecipe = lastRecipe;
                    blockEntity.processTime = 0;
                    blockEntity.inputsChanged = false;
                    needSync = true;
                }
            }

            if (blockEntity.currentRecipe == null) {
                ZhenRecipe newRecipe = ZhenRecipeManager.getInstance().findRecipe(blockEntity.type.getType(), blockEntity.items, blockEntity.partition, level);
                if (newRecipe != null) {
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
                if (blockEntity.canFitZoneOutputs(zoneOutputs)) {
                    for (Map.Entry<String, NonNullList<Ingredient>> entry : blockEntity.currentRecipe.getZoneInputs().entrySet()) {
                        SlotZone zone = blockEntity.partition.getZoneByName(entry.getKey());
                        if (zone != null) {
                            blockEntity.consumeInputs(entry.getValue(), zone);
                        }
                    }
                    for (Map.Entry<String, NonNullList<ItemStack>> entry : zoneOutputs.entrySet()) {
                        if (entry.getKey().equals("DROP_OUTPUT")) {
                            Set<String> zones = blockEntity.zoneFaceAccess.getOrDefault(Direction.DOWN, Set.of());
                            if (zones.contains("DROP_OUTPUT")) {
                                for (ItemStack stack : entry.getValue()) {
                                    Block.popResourceFromFace(level, pos, Direction.DOWN, stack);
                                }
                            } else {
                                for (Direction dir : Direction.values()) {
                                    if (blockEntity.zoneFaceAccess.getOrDefault(dir, Set.of()).contains("DROP_OUTPUT")) {
                                        for (ItemStack stack : entry.getValue()) {
                                            Block.popResourceFromFace(level, pos, dir, stack);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            SlotZone zone = blockEntity.partition.getZoneByName(entry.getKey());
                            if (zone != null) {
                                blockEntity.produceOutputs(entry.getValue(), zone);
                            }
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
            if (entry.getKey().equals("DROP_OUTPUT")) continue;
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            for (ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty() && !canFitOutput(Map.of(zone, stack))) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasIngredients(NonNullList<Ingredient> ingredients) {
        return hasIngredients(ingredients, SlotZone.INPUT_ALL);
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
        return consumeInputs(ingredients, SlotZone.INPUT_ALL);
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

    // ===== 数据持久化 & 网络同步 =====
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("process_time", processTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        processTime = input.getIntOr("process_time", 0);
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

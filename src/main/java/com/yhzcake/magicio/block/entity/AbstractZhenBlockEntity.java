package com.yhzcake.magicio.block.entity;

import com.yhzcake.magicio.item.crafting.ZhenRecipe;
import com.yhzcake.magicio.item.crafting.ZhenRecipeInput;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings(value = {"null","unused"})
public abstract class AbstractZhenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {

    public static final int PROCESS_COOL_SPEED = 2;
    private final RecipeType<? extends ZhenRecipe> recipeType;
    protected NonNullList<ItemStack> items;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> quickCheck;
    int processTime;
    int processTimeTotal;
    // 当前正在处理的配方（nullable），由子类/抽象合成逻辑使用
    private ZhenRecipe currentRecipe;
    // 当输入槽位发生变化时设置为 true，合成逻辑读取后应重置为 false
    protected boolean inputsChanged = false;
    // 可选的自定义 inventory handler；子类可通过 installInventoryHandler 安装自定义的 ItemStackHandler
    protected ItemStackHandler inventoryHandler;

    /**
     * 安装自定义的 ItemStackHandler。使用此方法可以避免在父类构造期间调用子类方法带来的时序问题。
     * 子类可在其构造器或初始化方法中调用此方法传入自定义 handler。
     */
    protected void installInventoryHandler(ItemStackHandler handler) {
        this.inventoryHandler = handler;
    }

    /**
     * 标记输入已改变，合成逻辑会在下一次检查时响应此标记。
     */
    protected void markInputsChanged() {
        this.inputsChanged = true;
    }

    /**
     * 返回槽位数量（委托到 handler 或 items 回退）。
     */
    protected int getInventorySize() {
        if (this.inventoryHandler != null) return this.inventoryHandler.getSlots();
        if (this.items != null) return this.items.size();
        return 0;
    }

    /**
     * 读取指定槽位的物品（委托实现）。
     */
    protected ItemStack getStackInSlot(int slot) {
        if (this.inventoryHandler != null) return this.inventoryHandler.getStackInSlot(slot);
        if (this.items == null || slot < 0 || slot >= this.items.size()) return ItemStack.EMPTY;
        return this.items.get(slot);
    }

    /**
     * 写入指定槽位（委托实现）。
     */
    protected void setStackInSlot(int slot, ItemStack stack) {
        if (this.inventoryHandler != null) {
            this.inventoryHandler.setStackInSlot(slot, stack);
            return;
        }
        if (this.items == null || slot < 0 || slot >= this.items.size()) return;
        this.items.set(slot, stack);
    }

    /**
     * 简单插入实现：优先委托 handler，否则在 items 上尝试合并/放置并返回剩余。
     */
    protected ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (this.inventoryHandler != null) return this.inventoryHandler.insertItem(slot, stack, simulate);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (this.items == null || slot < 0 || slot >= this.items.size()) return stack;

        ItemStack existing = this.items.get(slot);
        if (existing.isEmpty()) {
            if (!simulate) this.items.set(slot, stack.copy());
            return ItemStack.EMPTY;
        }
        if (ItemStack.isSameItemSameComponents(existing, stack)) {
            int max = existing.getMaxStackSize();
            int space = max - existing.getCount();
            if (space <= 0) return stack;
            int toAdd = Math.min(space, stack.getCount());
            if (!simulate) existing.grow(toAdd);
            if (toAdd == stack.getCount()) return ItemStack.EMPTY;
            ItemStack rem = stack.copy();
            rem.shrink(toAdd);
            return rem;
        }
        return stack;
    }

    /**
     * 简单提取实现：优先委托 handler，否则在 items 上按数量提取并返回取出的物品。
     */
    protected ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (this.inventoryHandler != null) return this.inventoryHandler.extractItem(slot, amount, simulate);
        if (this.items == null || slot < 0 || slot >= this.items.size() || amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = this.items.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int taken = Math.min(amount, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(taken);
        if (!simulate) {
            existing.shrink(taken);
            if (existing.getCount() <= 0) this.items.set(slot, ItemStack.EMPTY);
        }
        return result;
    }

    /**
     * 子类应实现槽位语义：哪些是输入槽，哪些是输出槽。（抽象契约，由子类定义）
     */
    protected abstract boolean isInputSlot(int slot);

    protected abstract boolean isOutputSlot(int slot);

    @SuppressWarnings("unchecked")
    protected AbstractZhenBlockEntity(BlockPos pos, BlockState blockState, RecipeType<? extends ZhenRecipe> recipeType) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), pos, blockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType<ZhenRecipe>) recipeType);
        this.recipeType = recipeType;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.processTime = tag.getInt("ProcessTime");
        this.processTimeTotal = tag.getInt("ProcessTimeTotal");
        CompoundTag compoundTag = tag.getCompound("RecipesUsed");

        for (String string : compoundTag.getAllKeys()) {
            this.recipesUsed.put(ResourceLocation.parse(string), compoundTag.getInt(string));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ProcessTime", this.processTime);
        tag.putInt("ProcessTimeTotal", this.processTimeTotal);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        CompoundTag compoundTag = new CompoundTag();
        this.recipesUsed.forEach((resourceLocation, integer) -> compoundTag.putInt(resourceLocation.toString(), integer));
        tag.put("RecipesUsed", compoundTag);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    public RecipeManager.CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> getQuickCheck() {
        return quickCheck;
    }

    public RecipeType<? extends ZhenRecipe> getRecipeType() {
        return recipeType;
    }

    // 分区映射由子类在构造时或初始化时一次性安装并保持不可变
    private Map<String, List<Integer>> inputPartitions = Collections.emptyMap();
    private Map<String, List<Integer>> outputPartitions = Collections.emptyMap();

    /**
     * 安装输入分区映射，子类应在构造器中调用一次。安装后映射将不可变，重复调用被忽略。
     */
    protected final void installInputPartitions(Map<String, List<Integer>> parts) {
        if (this.inputPartitions.isEmpty() && parts != null && !parts.isEmpty()) {
            this.inputPartitions = Collections.unmodifiableMap(parts);
        }
    }

    /**
     * 安装输出分区映射，子类应在构造器中调用一次。安装后映射将不可变，重复调用被忽略。
     */
    protected final void installOutputPartitions(Map<String, List<Integer>> parts) {
        if (this.outputPartitions.isEmpty() && parts != null && !parts.isEmpty()) {
            this.outputPartitions = Collections.unmodifiableMap(parts);
        }
    }

    /**
     * 获取输入分区映射（不可变）。
     */
    protected final Map<String, List<Integer>> getInputPartitions() {
        return this.inputPartitions;
    }

    /**
     * 获取输出分区映射（不可变）。
     */
    protected final Map<String, List<Integer>> getOutputPartitions() {
        return this.outputPartitions;
    }

    // WorldlyContainer 的面方法将在子类中实现（由子类根据具体面映射返回槽位数组）
    @Override
    public abstract int[] getSlotsForFace(Direction side);

    @Override
    public abstract boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side);

    @Override
    public abstract boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side);
}

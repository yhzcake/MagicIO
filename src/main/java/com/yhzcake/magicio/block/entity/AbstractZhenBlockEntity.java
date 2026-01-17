package com.yhzcake.magicio.block.entity;

import com.yhzcake.magicio.item.crafting.ZhenRecipe;
import com.yhzcake.magicio.item.crafting.ZhenRecipeInput;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;

@SuppressWarnings(value = {"null","unused"})
public abstract class AbstractZhenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder {

    public static final int PROCESS_COOL_SPEED = 2;
    private final RecipeType<? extends ZhenRecipe> recipeType;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> quickCheck;
    int processTime;
    int processTimeTotal;
    // 当前正在处理的配方（nullable），由子类/抽象合成逻辑使用
    private ZhenRecipe currentRecipe;
    // 当输入槽位发生变化时设置为 true，合成逻辑读取后应重置为 false
    protected boolean inputsChanged = false;
    
    // 统一的 ItemStackHandler 实现
    protected final ItemStackHandler inventoryHandler;
    
    // 输入/输出槽位数量
    protected final int inputSlotCount;
    protected final int outputSlotCount;
    
    /**
     * 获取物品处理器，用于能力系统
     */
    public IItemHandler getInventory() {
        return this.inventoryHandler;
    }

    /**
     * 标记输入已改变，合成逻辑会在下一次检查时响应此标记。
     */
    protected void markInputsChanged() {
        this.inputsChanged = true;
    }

    /**
     * 返回槽位数量（从 handler 获取）。
     */
    protected int getInventorySize() {
        return this.inventoryHandler.getSlots();
    }

    /**
     * 读取指定槽位的物品。
     */
    protected ItemStack getStackInSlot(int slot) {
        return this.inventoryHandler.getStackInSlot(slot);
    }

    /**
     * 写入指定槽位。
     */
    protected void setStackInSlot(int slot, ItemStack stack) {
        this.inventoryHandler.setStackInSlot(slot, stack);
    }

    /**
     * 插入物品到指定槽位。
     */
    protected ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.inventoryHandler.insertItem(slot, stack, simulate);
    }

    /**
     * 从指定槽位提取物品。
     */
    protected ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.inventoryHandler.extractItem(slot, amount, simulate);
    }

    /**
     * 检查指定槽位是否接受物品
     */
    protected boolean isItemValid(int slot, ItemStack stack) {
        return this.inventoryHandler.isItemValid(slot, stack);
    }

    /**
     * 获取输入槽位数量
     */
    public int getInputSlotCount() {
        return inputSlotCount;
    }

    /**
     * 获取输出槽位数量
     */
    public int getOutputSlotCount() {
        return outputSlotCount;
    }

    /**
     * 检查是否为输入槽位
     */
    protected boolean isInputSlot(int slot) {
        return slot < inputSlotCount;
    }

    /**
     * 检查是否为输出槽位
     */
    protected boolean isOutputSlot(int slot) {
        return slot >= inputSlotCount && slot < inputSlotCount + outputSlotCount;
    }

    /**
     * 获取输入物品（用于配方查找）
     */
    protected NonNullList<ItemStack> getInputItems() {
        NonNullList<ItemStack> inputItems = NonNullList.create();
        for (int i = 0; i < inputSlotCount; i++) {
            inputItems.add(inventoryHandler.getStackInSlot(i));
        }
        return inputItems;
    }

    /**
     * 获取输出物品（用于检查空间）
     */
    protected NonNullList<ItemStack> getOutputItems() {
        NonNullList<ItemStack> outputItems = NonNullList.create();
        for (int i = inputSlotCount; i < inputSlotCount + outputSlotCount; i++) {
            outputItems.add(inventoryHandler.getStackInSlot(i));
        }
        return outputItems;
    }

    /**
     * 检查是否可以放入物品到指定输入槽位
     */
    protected boolean canInsertItem(int slot, ItemStack stack) {
        return isInputSlot(slot) && inventoryHandler.isItemValid(slot, stack);
    }

    /**
     * 检查是否可以从指定输出槽位提取物品
     */
    protected boolean canExtractItem(int slot) {
        return isOutputSlot(slot);
    }

    // 这里移除了原来的抽象方法定义，因为现在有具体实现

    protected AbstractZhenBlockEntity(BlockPos pos, BlockState blockState, RecipeType<? extends ZhenRecipe> recipeType, int inputSlotCount, int outputSlotCount) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), pos, blockState);
        this.recipeType = recipeType;
        this.inputSlotCount = inputSlotCount;
        this.outputSlotCount = outputSlotCount;
        this.inventoryHandler = createItemHandler();
        this.quickCheck = RecipeManager.createCheck(recipeType);
    }
    
    // 创建自定义的 ItemStackHandler
    protected ItemStackHandler createItemHandler() {
        return new ItemStackHandler(inputSlotCount + outputSlotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                // 检查是否是输入槽位发生变化
                if (isInputSlot(slot)) {
                    inputsChanged = true;
                }
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isInputSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isInputSlot(slot)) {
                    return stack;
                }
                // 检查是否是输入槽位发生变化
                inputsChanged = true;
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (isInputSlot(slot)) {
                    inputsChanged = true;
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }
        };
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 加载 inventory handler 的数据
        if (tag.contains("Inventory")) {
            inventoryHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
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
        // 保存 inventory handler 的数据
        tag.put("Inventory", inventoryHandler.serializeNBT(registries));
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
    private Map<String, List<Integer>> partitions = Collections.emptyMap();

    /**
     * 安装分区映射，子类应在构造器中调用一次。安装后映射将不可变，重复调用被忽略。
     */
    public final void installPartitions(Map<String, List<Integer>> parts) {
        if (this.partitions.isEmpty() && parts != null && !parts.isEmpty()) {
            this.partitions = Collections.unmodifiableMap(parts);
        }
    }

    /**
     * 获取分区映射（不可变）。
     */
    public final Map<String, List<Integer>> getPartitions() {
        return this.partitions;
    }
    
    /**
     * 检查分区是否为输入分区
     */
    public boolean isInputPartition(String partitionName) {
        if ("__ALL_INPUT__".equals(partitionName)) {
            return true; // 整体输入槽位分区
        }
        List<Integer> partitionSlots = this.partitions.get(partitionName);
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            return false;
        }
        // 检查分区中的槽位是否都属于输入槽位
        return partitionSlots.stream().allMatch(this::isInputSlot);
    }
    
    /**
     * 检查分区是否为输出分区
     */
    public boolean isOutputPartition(String partitionName) {
        if ("__ALL_OUTPUT__".equals(partitionName)) {
            return true; // 整体输出槽位分区
        }
        List<Integer> partitionSlots = this.partitions.get(partitionName);
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            return false;
        }
        // 检查分区中的槽位是否都属于输出槽位
        return partitionSlots.stream().allMatch(this::isOutputSlot);
    }
    
    /**
     * 便捷方法：安装默认的输入和输出分区映射
     */
    public final void installDefaultPartitions() {
        java.util.Map<String, List<Integer>> defaultPartitions = new java.util.HashMap<>();
        
        // 添加输入分区（使用预定义常量）
        java.util.List<Integer> inputSlots = new java.util.ArrayList<>();
        for (int i = 0; i < inputSlotCount; i++) {
            inputSlots.add(i);
        }
        defaultPartitions.put(ALL_INPUT_SLOTS_PARTITION, inputSlots);
        
        // 添加输出分区（使用预定义常量）
        java.util.List<Integer> outputSlots = new java.util.ArrayList<>();
        for (int i = 0; i < outputSlotCount; i++) {
            outputSlots.add(inputSlotCount + i);
        }
        defaultPartitions.put(ALL_OUTPUT_SLOTS_PARTITION, outputSlots);
        
        // 安装分区
        installPartitions(defaultPartitions);
    }

        // 添加槽位映射字段
    private Map<Direction, int[]> slotMappings;
    private boolean mappingsInitialized = false;
    
    // 初始化槽位映射（由子类负责提供默认映射）
    protected void initializeSlotMappings() {
        if (!mappingsInitialized) {
            // 初始时设置为空映射，由子类负责提供映射
            this.slotMappings = new java.util.HashMap<>();
            this.mappingsInitialized = true;
        }
    }
    
    // 常量定义
    public static final String ALL_INPUT_SLOTS_PARTITION = "__ALL_INPUT__";
    public static final String ALL_OUTPUT_SLOTS_PARTITION = "__ALL_OUTPUT__";
    
    // 提供统一的重载方法供子类设置面的槽位映射
    public void setSlotForFace(Direction direction, String partitionName) {
        if (this.slotMappings == null) {
            initializeSlotMappings();
        }
        
        // 获取分区对应的槽位
        Map<String, List<Integer>> allPartitions = getPartitions();
        List<Integer> partitionSlots = allPartitions.get(partitionName);
        
        if (partitionSlots != null && !partitionSlots.isEmpty()) {
            // 将List<Integer>转换为int...并复用setSlotForFace方法
            int[] slotsArray = partitionSlots.stream().mapToInt(Integer::intValue).toArray();
            setSlotForFace(direction, slotsArray);
        } else {
            // 如果分区不存在，检查是否为整体输入或输出槽位分区
            if (ALL_INPUT_SLOTS_PARTITION.equals(partitionName)) {
                int[] inputSlots = new int[inputSlotCount];
                for (int i = 0; i < inputSlotCount; i++) {
                    inputSlots[i] = i;
                }
                setSlotForFace(direction, inputSlots);
            } else if (ALL_OUTPUT_SLOTS_PARTITION.equals(partitionName)) {
                int[] outputSlots = new int[outputSlotCount];
                for (int i = 0; i < outputSlotCount; i++) {
                    outputSlots[i] = inputSlotCount + i;
                }
                setSlotForFace(direction, outputSlots);
            }
        }
    }
    
    public void setSlotForFace(Direction direction, int... slotNumbers) {
        if (this.slotMappings == null) {
            initializeSlotMappings();
        }
        this.slotMappings.put(direction, slotNumbers);
    }
    

    
    // 提供方法供子类添加槽位到特定面的映射
    public void addSlotForFace(Direction direction, int... slots) {
        if (this.slotMappings == null) {
            initializeSlotMappings();
        }
        
        // 获取当前映射并添加新槽位
        int[] currentSlots = this.slotMappings.getOrDefault(direction, new int[0]);
        
        // 创建新的槽位数组，包含原有槽位和新槽位
        int[] newSlots = new int[currentSlots.length + slots.length];
        System.arraycopy(currentSlots, 0, newSlots, 0, currentSlots.length);
        System.arraycopy(slots, 0, newSlots, currentSlots.length, slots.length);
        
        this.slotMappings.put(direction, newSlots);
    }
    
    // 提供方法供子类移除特定面的槽位
    public void removeSlotsFromFace(Direction direction, int... slotsToRemove) {
        if (this.slotMappings == null) {
            initializeSlotMappings();
        }
        
        int[] currentSlots = this.slotMappings.getOrDefault(direction, new int[0]);
        if (currentSlots.length == 0) return;
        
        // 创建一个新的列表，排除要移除的槽位
        java.util.List<Integer> remainingSlots = new java.util.ArrayList<>();
        for (int currentSlot : currentSlots) {
            boolean shouldRemove = false;
            for (int slotToRemove : slotsToRemove) {
                if (currentSlot == slotToRemove) {
                    shouldRemove = true;
                    break;
                }
            }
            if (!shouldRemove) {
                remainingSlots.add(currentSlot);
            }
        }
        
        // 转换回数组并更新映射
        int[] newSlots = remainingSlots.stream().mapToInt(Integer::intValue).toArray();
        this.slotMappings.put(direction, newSlots);
    }
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (!mappingsInitialized) {
            initializeSlotMappings();
        }
        // 默认情况下如果没有为该面设置槽位映射，则返回空数组，由子类负责提供默认映射
        return this.slotMappings.getOrDefault(side, new int[0]);
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side) {
        if (!mappingsInitialized) {
            initializeSlotMappings();
        }
        
        // 检查索引是否在允许的槽位中
        int[] allowedSlots = this.slotMappings.get(side);
        if (allowedSlots == null || allowedSlots.length == 0) {
            // 如果没有为该面定义槽位映射，返回false，由子类负责提供默认映射
            return false;
        }
        
        boolean isAllowedSlot = false;
        for (int slot : allowedSlots) {
            if (slot == index) {
                isAllowedSlot = true;
                break;
            }
        }
        if (!isAllowedSlot) return false;
        
        // 检查是否为输入槽位
        return isInputSlot(index);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
        if (!mappingsInitialized) {
            initializeSlotMappings();
        }
        
        // 检查索引是否在允许的槽位中
        int[] allowedSlots = this.slotMappings.get(side);
        if (allowedSlots == null || allowedSlots.length == 0) {
            // 如果没有为该面定义槽位映射，返回false，由子类负责提供默认映射
            return false;
        }
        
        boolean isAllowedSlot = false;
        for (int slot : allowedSlots) {
            if (slot == index) {
                isAllowedSlot = true;
                break;
            }
        }
        if (!isAllowedSlot) return false;
        
        // 检查是否为输出槽位
        return isOutputSlot(index);
    }
    
    // 物品分配相关方法
    /**
     * 根据面插入物品，允许部分插入
     */
    public ItemStack insertItemByFace(ItemStack stack, Direction side, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        
        // 获取该面允许的槽位
        int[] accessibleSlots = getSlotsForFace(side);
        ItemStack remainingStack = stack;
        
        // 只考虑输入槽位
        for (int slot : accessibleSlots) {
            if (isInputSlot(slot) && !remainingStack.isEmpty()) {  // 确保是输入槽位且还有物品待插入
                ItemStack remaining = insertItem(slot, remainingStack, simulate);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;  // 完全插入
                }
                remainingStack = remaining;  // 更新待插入的物品
            }
        }
        
        return remainingStack;  // 返回未插入的物品
    }
    

    
    /**
     * 按分区名称插入物品，允许部分插入
     */
    public ItemStack insertItemByPartition(ItemStack stack, String partitionName, boolean simulate) {
        if (stack.isEmpty() || partitionName == null) return stack;
        
        // 获取分区对应的槽位
        Map<String, List<Integer>> allPartitions = getPartitions();
        List<Integer> partitionSlots = allPartitions.get(partitionName);
        
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            // 如果指定分区不存在，使用默认输入槽位
            return insertItemToInputSlots(stack, simulate);
        }
        
        // 将分区槽位转换为数组并复用insertItemBySlots方法
        int[] slotsArray = partitionSlots.stream().mapToInt(Integer::intValue).toArray();
        return insertItemBySlots(stack, slotsArray, simulate);
    }
    
    /**
     * 按分区名称插入物品，只有当能完全插入时才执行插入
     */
    public boolean insertItemByPartitionCompletely(ItemStack stack, String partitionName) {
        if (stack.isEmpty() || partitionName == null) return true;
        
        // 检查是否能够完全插入
        if (!canFullyInsertItemByPartition(stack, partitionName)) {
            return false;  // 无法完全插入
        }
        
        // 执行实际插入
        ItemStack remaining = insertItemByPartition(stack, partitionName, false);
        return remaining.isEmpty();  // 应该完全插入，没有剩余
    }
    
    /**
     * 插入物品到所有可用输入槽位
     */
    public ItemStack insertItemToInputSlots(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        
        // 首先尝试合并现有堆叠
        ItemStack remainingStack = stack;
        for (int i = 0; i < inputSlotCount && !remainingStack.isEmpty(); i++) {
            remainingStack = insertItem(i, remainingStack, simulate);
            if (remainingStack.isEmpty()) {
                return ItemStack.EMPTY;  // 完全插入
            }
        }
        
        return remainingStack;  // 返回未插入的物品
    }
    

    
    /**
     * 检查面是否可以接收物品（基于输入槽位是否可用）
     */
    public boolean canAcceptItemsFromFace(Direction side) {
        int[] accessibleSlots = getSlotsForFace(side);
        
        // 检查是否有任何输入槽位可用
        for (int slot : accessibleSlots) {
            if (isInputSlot(slot)) {
                return true; // 至少有一个输入槽位可用
            }
        }
        
        return false; // 没有输入槽位可用
    }
    
    /**
     * 检查是否可以按分区名称完全插入物品
     */
    public boolean canFullyInsertItemByPartition(ItemStack stack, String partitionName) {
        if (stack.isEmpty() || partitionName == null) return true;
        
        // 获取分区对应的槽位
        Map<String, List<Integer>> allPartitions = getPartitions();
        List<Integer> partitionSlots = allPartitions.get(partitionName);
        
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            // 如果指定分区不存在，检查是否可以插入到所有输入槽位
            return canFullyInsertItemToInputSlots(stack);
        }
        
        // 将分区槽位转换为数组并复用canFullyInsertItemBySlots方法
        int[] slotsArray = partitionSlots.stream().mapToInt(Integer::intValue).toArray();
        return canFullyInsertItemBySlots(stack, slotsArray);
    }
    
    /**
     * 检查是否可以插入物品到所有可用输入槽位
     */
    public boolean canFullyInsertItemToInputSlots(ItemStack stack) {
        if (stack.isEmpty()) return true;
        
        // 创建临时的模拟库存来检查是否能完全插入
        ItemStack[] tempInventory = new ItemStack[inputSlotCount + outputSlotCount];
        for (int i = 0; i < tempInventory.length; i++) {
            tempInventory[i] = getStackInSlot(i).copy();
        }
        
        ItemStack tempStack = stack.copy();
        
        // 首先尝试合并现有堆叠
        for (int i = 0; i < inputSlotCount && !tempStack.isEmpty(); i++) {
            ItemStack existingStack = tempInventory[i];
            
            if (existingStack.isEmpty()) {
                // 空槽位，计算能放入多少
                int maxFit = Math.min(tempStack.getMaxStackSize(), inventoryHandler.getSlotLimit(i));
                int toInsert = Math.min(tempStack.getCount(), maxFit);
                tempStack.shrink(toInsert);
            } else if (ItemStack.isSameItemSameComponents(existingStack, tempStack)) {
                // 同类物品，尝试合并
                int spaceAvailable = Math.min(existingStack.getMaxStackSize(), inventoryHandler.getSlotLimit(i)) - existingStack.getCount();
                int toInsert = Math.min(tempStack.getCount(), spaceAvailable);
                tempStack.shrink(toInsert);
            }
        }
        
        return tempStack.isEmpty();
    }
    
    /**
     * 在指定槽位范围内分配物品（考虑同类合并和堆叠限制）
     */
    public AllocationResult allocateItemsToSlots(ItemStack[] itemStacks, int startSlot, int endSlot) {
        java.util.List<ItemStack> remainingItems = new java.util.ArrayList<>();
        int totalAllocated = 0;
        
        for (ItemStack itemStack : itemStacks) {
            if (itemStack.isEmpty()) continue;
            
            ItemStack stackToProcess = itemStack.copy();
            
            // 首先尝试合并到现有的同类物品堆叠
            for (int slot = startSlot; slot < endSlot; slot++) {
                ItemStack existingStack = getStackInSlot(slot);
                
                if (!existingStack.isEmpty() && 
                    ItemStack.isSameItemSameComponents(existingStack, stackToProcess)) {
                    
                    int maxStackSize = Math.min(existingStack.getMaxStackSize(), inventoryHandler.getSlotLimit(slot));
                    int spaceAvailable = maxStackSize - existingStack.getCount();
                    int transferAmount = Math.min(stackToProcess.getCount(), spaceAvailable);
                    
                    if (transferAmount > 0) {
                        existingStack.grow(transferAmount);
                        setStackInSlot(slot, existingStack);
                        stackToProcess.shrink(transferAmount);
                        totalAllocated += transferAmount;
                        
                        if (stackToProcess.isEmpty()) {
                            break; // 当前物品已全部分配
                        }
                    }
                }
            }
            
            // 如果还有剩余物品，尝试放入空槽位
            if (!stackToProcess.isEmpty()) {
                for (int slot = startSlot; slot < endSlot; slot++) {
                    ItemStack existingStack = getStackInSlot(slot);
                    
                    if (existingStack.isEmpty()) {
                        int maxStackSize = Math.min(stackToProcess.getMaxStackSize(), inventoryHandler.getSlotLimit(slot));
                        int transferAmount = Math.min(stackToProcess.getCount(), maxStackSize);
                        ItemStack toInsert = stackToProcess.split(transferAmount);
                        setStackInSlot(slot, toInsert);
                        totalAllocated += toInsert.getCount();
                        
                        if (stackToProcess.isEmpty()) {
                            break; // 当前物品已全部分配
                        }
                    }
                }
            }
            
            // 如果仍有剩余物品，添加到剩余列表
            if (!stackToProcess.isEmpty()) {
                remainingItems.add(stackToProcess);
            }
        }
        
        return new AllocationResult(totalAllocated, 
                                   remainingItems.toArray(new ItemStack[0]));
    }
        
    /**
     * 检查是否可以在指定槽位范围内完全分配物品
     */
    public boolean canFullyAllocateItemsToSlots(ItemStack[] itemStacks, int startSlot, int endSlot) {
        // 创建临时的模拟库存来检查是否能完全分配
        ItemStack[] tempInventory = new ItemStack[inputSlotCount + outputSlotCount];
        for (int i = 0; i < tempInventory.length; i++) {
            tempInventory[i] = getStackInSlot(i).copy();
        }
            
        // 模拟分配过程
        for (ItemStack itemStack : itemStacks) {
            if (itemStack.isEmpty()) continue;
                
            ItemStack stackToProcess = itemStack.copy();
                
            // 首先尝试合并到现有的同类物品堆叠
            for (int slot = startSlot; slot < endSlot; slot++) {
                ItemStack existingStack = tempInventory[slot];
                    
                if (!existingStack.isEmpty() && 
                    ItemStack.isSameItemSameComponents(existingStack, stackToProcess)) {
                        
                    int maxStackSize = Math.min(existingStack.getMaxStackSize(), inventoryHandler.getSlotLimit(slot));
                    int spaceAvailable = maxStackSize - existingStack.getCount();
                    int transferAmount = Math.min(stackToProcess.getCount(), spaceAvailable);
                        
                    if (transferAmount > 0) {
                        ItemStack newStack = existingStack.copy();
                        newStack.grow(transferAmount);
                        tempInventory[slot] = newStack;
                        stackToProcess.shrink(transferAmount);
                            
                        if (stackToProcess.isEmpty()) {
                            break; // 当前物品已全部分配
                        }
                    }
                }
            }
                
            // 如果还有剩余物品，尝试放入空槽位
            if (!stackToProcess.isEmpty()) {
                for (int slot = startSlot; slot < endSlot; slot++) {
                    ItemStack existingStack = tempInventory[slot];
                        
                    if (existingStack.isEmpty()) {
                        int maxStackSize = Math.min(stackToProcess.getMaxStackSize(), inventoryHandler.getSlotLimit(slot));
                        int transferAmount = Math.min(stackToProcess.getCount(), maxStackSize);
                        ItemStack toInsert = stackToProcess.split(transferAmount);
                        tempInventory[slot] = toInsert;
                        stackToProcess.shrink(transferAmount);
                            
                        if (stackToProcess.isEmpty()) {
                            break; // 当前物品已全部分配
                        }
                    }
                }
            }
                
            // 如果仍有剩余物品，说明无法完全分配
            if (!stackToProcess.isEmpty()) {
                return false;
            }
        }
            
        return true;
    }
        
    /**
     * 分配输出物品到输出槽位
     */
    public ItemStack[] distributeOutputItems(ItemStack[] outputStacks) {
        // 从第一个输出槽位开始分配
        int startOutputSlot = inputSlotCount;
        int endOutputSlot = inputSlotCount + outputSlotCount;
        
        return allocateItemsToSlots(outputStacks, startOutputSlot, endOutputSlot)
               .getRemainingItems();
    }
    
    /**
     * 检查是否可以完全分配输出物品到指定分区
     */
    public boolean canFullyDistributeOutputItemsToPartition(ItemStack[] outputStacks, String partitionName) {
        if (partitionName == null) {
            // 计算输出槽位范围
            int startOutputSlot = inputSlotCount;
            int endOutputSlot = inputSlotCount + outputSlotCount;
            return canFullyAllocateItemsToSlots(outputStacks, startOutputSlot, endOutputSlot);
        }
        
        Map<String, List<Integer>> allPartitions = getPartitions();
        List<Integer> partitionSlots = allPartitions.get(partitionName);
        
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            // 计算输出槽位范围
            int startOutputSlot = inputSlotCount;
            int endOutputSlot = inputSlotCount + outputSlotCount;
            return canFullyAllocateItemsToSlots(outputStacks, startOutputSlot, endOutputSlot);
        }
        
        // 将分区 List<Integer> 转换为 int[] 并找到范围
        int minSlot = partitionSlots.stream().mapToInt(Integer::intValue).min().orElse(inputSlotCount + outputSlotCount);
        int maxSlot = partitionSlots.stream().mapToInt(Integer::intValue).max().orElse(inputSlotCount - 1);
        
        // 确保在输出槽位范围内
        minSlot = Math.max(minSlot, inputSlotCount);
        maxSlot = Math.min(maxSlot, inputSlotCount + outputSlotCount - 1);
        
        if (minSlot > maxSlot) {
            // 无效的范围，无法分配
            return false;
        }
        
        return canFullyAllocateItemsToSlots(outputStacks, minSlot, maxSlot + 1);
    }
    
    /**
     * 分配输出物品到指定分区
     */
    public ItemStack[] distributeOutputItemsToPartition(ItemStack[] outputStacks, String partitionName) {
        if (partitionName == null) {
            return distributeOutputItems(outputStacks);
        }
        
        Map<String, List<Integer>> allPartitions = getPartitions();
        List<Integer> partitionSlots = allPartitions.get(partitionName);
        
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            return distributeOutputItems(outputStacks);
        }
        
        // 将分区 List<Integer> 转换为 int[] 并找到范围
        int minSlot = partitionSlots.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxSlot = partitionSlots.stream().mapToInt(Integer::intValue).max().orElse(0);
        
        // 确保在输出槽位范围内
        minSlot = Math.max(minSlot, inputSlotCount);
        maxSlot = Math.min(maxSlot, inputSlotCount + outputSlotCount - 1);
        
        AllocationResult result = allocateItemsToSlots(outputStacks, minSlot, maxSlot + 1);
        return result.getRemainingItems();
    }
    
    /**
     * 按槽位数组插入物品，允许部分插入
     */
    private ItemStack insertItemBySlots(ItemStack stack, int[] slots, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        
        ItemStack remainingStack = stack;
        
        // 遍历指定槽位，尝试插入
        for (int slot : slots) {
            if (isInputSlot(slot) && !remainingStack.isEmpty()) {  // 确保是输入槽位且还有物品待插入
                ItemStack remaining = insertItem(slot, remainingStack, simulate);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;  // 完全插入
                }
                remainingStack = remaining;  // 更新待插入的物品
            }
        }
        
        return remainingStack;  // 返回未插入的物品
    }
    
    /**
     * 检查是否可以按槽位数组完全插入物品
     */
    private boolean canFullyInsertItemBySlots(ItemStack stack, int[] slots) {
        if (stack.isEmpty()) return true;
        
        ItemStack tempStack = stack.copy();
        
        // 创建临时的模拟库存来检查是否能完全插入
        ItemStack[] tempInventory = new ItemStack[inputSlotCount + outputSlotCount];
        for (int i = 0; i < tempInventory.length; i++) {
            tempInventory[i] = getStackInSlot(i).copy();
        }
        
        // 遍历指定槽位，尝试插入
        for (int slot : slots) {
            if (isInputSlot(slot) && !tempStack.isEmpty()) {
                ItemStack existingStack = tempInventory[slot];
                
                if (existingStack.isEmpty()) {
                    // 空槽位，计算能放入多少
                    int maxFit = Math.min(tempStack.getMaxStackSize(), inventoryHandler.getSlotLimit(slot));
                    int toInsert = Math.min(tempStack.getCount(), maxFit);
                    tempStack.shrink(toInsert);
                } else if (ItemStack.isSameItemSameComponents(existingStack, tempStack)) {
                    // 同类物品，尝试合并
                    int spaceAvailable = Math.min(existingStack.getMaxStackSize(), inventoryHandler.getSlotLimit(slot)) - existingStack.getCount();
                    int toInsert = Math.min(tempStack.getCount(), spaceAvailable);
                    tempStack.shrink(toInsert);
                }
            }
        }
        
        return tempStack.isEmpty();
    }
    
    /**
     * 物品分配结果类
     */
    protected static class AllocationResult {
        private final int allocatedCount;
        private final ItemStack[] remainingItems;
        
        public AllocationResult(int allocatedCount, ItemStack[] remainingItems) {
            this.allocatedCount = allocatedCount;
            this.remainingItems = remainingItems;
        }
        
        public int getAllocatedCount() {
            return allocatedCount;
        }
        
        public ItemStack[] getRemainingItems() {
            return remainingItems;
        }
    }
    

    
    // 实现 Container 接口
    @Override
    public int getContainerSize() {
        return this.inventoryHandler.getSlots();
    }
    
    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.inventoryHandler.getSlots(); i++) {
            if (!this.inventoryHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < this.inventoryHandler.getSlots()) {
            return this.inventoryHandler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }
    
    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot >= 0 && slot < this.inventoryHandler.getSlots()) {
            return this.inventoryHandler.extractItem(slot, count, false);
        }
        return ItemStack.EMPTY;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < this.inventoryHandler.getSlots()) {
            return this.inventoryHandler.extractItem(slot, this.inventoryHandler.getStackInSlot(slot).getCount(), false);
        }
        return ItemStack.EMPTY;
    }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.inventoryHandler.getSlots()) {
            this.inventoryHandler.setStackInSlot(slot, stack);
        }
    }
    
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.inventoryHandler.getSlots()) {
            return this.inventoryHandler.isItemValid(slot, stack);
        }
        return false;
    }
    
    // 实现 BaseContainerBlockEntity 抽象方法
    @Override
    protected NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(this.inventoryHandler.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < this.inventoryHandler.getSlots(); i++) {
            items.set(i, this.inventoryHandler.getStackInSlot(i));
        }
        return items;
    }
    
    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < Math.min(items.size(), this.inventoryHandler.getSlots()); i++) {
            this.inventoryHandler.setStackInSlot(i, items.get(i));
        }
    }
    
    @Override
    public Component getDefaultName() {
        return Component.literal("Zhen Block Entity");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null; // 不支持GUI菜单
    }
    
    // 实现 RecipeCraftingHolder 接口
    @Override
    public void setRecipeUsed(@Nullable net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        if (recipe != null) {
            this.recipesUsed.addTo(recipe.id(), 1);
        }
    }
    
    @Nullable
    @Override
    public net.minecraft.world.item.crafting.RecipeHolder<?> getRecipeUsed() {
        if (this.recipesUsed.isEmpty()) {
            return null;
        }
        ResourceLocation resourceLocation = this.recipesUsed.keySet().iterator().next();
        if (level != null && level.getRecipeManager() != null) {
            return level.getRecipeManager().byKey(resourceLocation).orElse(null);
        }
        return null;
    }
    
}
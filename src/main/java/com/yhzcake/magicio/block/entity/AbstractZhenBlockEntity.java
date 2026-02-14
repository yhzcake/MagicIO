package com.yhzcake.magicio.block.entity;

import com.yhzcake.magicio.block.zhen.ZhenBlock;
import com.yhzcake.magicio.block.zhen.ZhenType;
import com.yhzcake.magicio.block.zhen.ZhenTypes;
import com.yhzcake.magicio.item.crafting.ZhenRecipe;
import com.yhzcake.magicio.item.crafting.ZhenRecipeInput;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings(value = {"null","unused"})
public abstract class AbstractZhenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {

    public static final int PROCESS_COOL_SPEED = 2;
    private final RecipeType<? extends ZhenRecipe> recipeType;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> quickCheck;
    int processTime;
    int processTimeTotal;
    // 当前正在处理的配方（nullable），由子类/抽象合成逻辑使用
    private ZhenRecipe currentRecipe;
    // 当输入槽位发生变化时设置为 true，合成逻辑读取后应重置为 false
    private boolean inputsChanged = false;
    
    private final ItemStackHandler inventoryHandler;
    private final int inputSlotCount;
    private final int outputSlotCount;
    public IItemHandler getInventory() {
        return this.inventoryHandler;
    }
    private final ZhenType<?> type;
    /**
     * 标记输入已改变，合成逻辑会在下一次检查时响应此标记。
     */
    protected void markInputsChanged() {
        this.inputsChanged = true;
    }

    /**
     * 返回槽位数量
     */
    public int getInventorySize() {
        return this.inventoryHandler.getSlots();
    }

    /**
     * 读取指定槽位的物品
     */
    public ItemStack getStackInSlot(int slot) {
        if (this.inventoryHandler != null) return this.inventoryHandler.getStackInSlot(slot);
        return ItemStack.EMPTY;
    }

    /**
     * 写入指定槽位
     */
    public void setStackInSlot(int slot, ItemStack stack) {
        if (this.inventoryHandler != null) this.inventoryHandler.setStackInSlot(slot, stack);
    }

    /**
     * 插入物品到指定槽位
     */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (this.inventoryHandler != null) return this.inventoryHandler.insertItem(slot, stack, simulate);
        return stack;
    }
    /**
     * 
     * 从指定槽位提取物品
     * 
     */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (this.inventoryHandler != null) return this.inventoryHandler.extractItem(slot, amount, simulate);
        return ItemStack.EMPTY;
    }
    /**
     * 获取输入槽位数量
     */
    public int getInputSlotCount() {
        return this.inputSlotCount;
    }
    /**
     * 获取输出槽位数量
     */
    public int getOutputSlotCount() {
        return this.outputSlotCount;
    }

    /**
     * 检查是否为输入槽位
     */
    public boolean isInputSlot(int slot) {
        List<Integer> inputSlots = this.partitions.get("input");
        return inputSlots != null && inputSlots.contains(slot);
    }
    /**
     * 检查是否为输出槽位
     */
    public boolean isOutputSlot(int slot) {
        List<Integer> outputSlots = this.partitions.get("output");
        return outputSlots != null && outputSlots.contains(slot);
    }
    /**
     * 获取输入物品（按分区分开的输入物品表）
    */
    public Map<String, NonNullList<ItemStack>> getInputs() {
        Map<String, NonNullList<ItemStack>> inputs = new HashMap<>();
        //遍历分区，为每个key对应的输入槽位创建NonNullList<ItemStack>并添加到inputs中
        for (Map.Entry<String, List<Integer>> entry : this.partitions.entrySet()) {
            String partition = entry.getKey();
            List<Integer> slots = entry.getValue();
            NonNullList<ItemStack> partitionInputs = NonNullList.create();
            for (int i : slots) {
                partitionInputs.add(this.inventoryHandler.getStackInSlot(i));
            }
            inputs.put(partition, partitionInputs);
        }
        return inputs;
    }
    /**
     * 获取输出物品
     */
    public NonNullList<ItemStack> getOutputs() {
        NonNullList<ItemStack> outputs = NonNullList.create();
        List<Integer> outputSlots = this.partitions.get("output");
        if (outputSlots != null) {
            for (int i : outputSlots) {
                outputs.add(this.inventoryHandler.getStackInSlot(i));
            }
        }
        return outputs;
    }

    /**
     * 检测槽位属于哪个分区
     * @param slot 槽位索引
     * @return 槽位所属分区，如果不存在则返回 null
     */
    public String getPartition(int slot) {
        for (Map.Entry<String, List<Integer>> entry : this.partitions.entrySet()) {
            String partition = entry.getKey();
            List<Integer> slots = entry.getValue();
            if (!partition.equals("input") && slots.contains(slot)) {
                return partition;
            }
        }
        return null;
    }
    /**
     * 检查是否可以放入物品（匹配分区和物品标签）
     */
    public boolean canInsertItem(int slot, ItemStack stack) {
        if(isInputSlot(slot)){
            String partition = getPartition(slot);
            if (partition != null) {
                List<TagKey<Item>> tags = this.partitionItemTags.get(partition);
                if (tags != null && !tags.isEmpty()) {
                    for (TagKey<Item> tag : tags) {
                        if (stack.is(tag)) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }        
        return false;
    }
    
    /**
     * 检查是否可以提取物品
     */
    public boolean canExtractItem(int slot, ItemStack stack) {
        return isOutputSlot(slot);
    }
    protected AbstractZhenBlockEntity(BlockPos pos, BlockState blockState, RecipeType<? extends ZhenRecipe> recipeType) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), pos, blockState);
        this.quickCheck = RecipeManager.createCheck(recipeType);
        this.recipeType = recipeType;
        if (blockState.getBlock() instanceof ZhenBlock block) {
            this.type = block.getType();
        } else {
            this.type = ZhenTypes.SMALL_SIFT_ZHEN.get();
        }
        this.inputSlotCount = type.getInput();
        this.outputSlotCount = type.getOutput();
        this.inventoryHandler = createItemHandler();
    }
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
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
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
        if(tag.contains("Inventory")) {
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
    private Map<String, List<Integer>> partitions;

    /**
     * 安装分区映射，子类应在构造器中调用一次。安装后映射将不可变，重复调用被忽略。
     */
    public final void installPartitions(Map<String, List<Integer>> parts) {
        if (this.partitions.isEmpty() && parts != null && !parts.isEmpty()) {
            Map<String, List<Integer>> dfPartition = new HashMap<>();

            List<Integer> inputSlots = new ArrayList<>();
            for (int i = 0; i < inputSlotCount; i++) {
                inputSlots.add(i);
            }
            dfPartition.put("input",inputSlots);

            List<Integer> outputSlots = new ArrayList<>();
            for (int i = 0; i < outputSlotCount; i++) {
                outputSlots.add(inputSlotCount + i);
            }
            dfPartition.put("output",outputSlots);
            
            dfPartition.forEach(
                (key, value) -> parts.merge(key, value, (v1, v2) -> v1)
            );

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
    public boolean isInputPartition(String partition) {
        if ("input".equals(partition)) {
            return true; // 整体输入槽位分区
        }
        List<Integer> partitionSlots = this.partitions.get(partition);
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            return false;
        }
        // 检查分区中的槽位是否都属于输入槽位
        return partitionSlots.stream().allMatch(this::isInputSlot);
    }
    
    /**
     * 检查分区是否为输出分区
     */
    public boolean isOutputPartition(String partition) {
        if ("output".equals(partition)) {
            return true; // 整体输出槽位分区
        }
        List<Integer> partitionSlots = this.partitions.get(partition);
        if (partitionSlots == null || partitionSlots.isEmpty()) {
            return false;
        }
        // 检查分区中的槽位是否都属于输出槽位
        return partitionSlots.stream().allMatch(this::isOutputSlot);
    }

    /**
     * 检查物品能否输入到指定分区，子类override
     */
    public boolean canInsertItemToPartition(ItemStack stack, String partition) {
        // 首先检查是否为输入分区
        if (!isInputPartition(partition)) {
            return false;
        }
        
        // 检查分区的物品标签限制
        List<TagKey<Item>> requiredTags = this.partitionItemTags.get(partition);
        if (requiredTags != null && !requiredTags.isEmpty()) {
            // 检查物品是否至少匹配其中一个标签
            for (TagKey<Item> tag : requiredTags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            // 如果指定了标签但物品不匹配任何标签，则不允许插入
            return false;
        }
        
        // 如果没有标签限制，则允许插入
        return true;
    }
    
    /**
     * 设置分区的物品标签限制
     * @param partition 分区名称
     * @param tags 该分区接受的物品标签列表
     */
    public void setPartitionItemTags(String partition, List<TagKey<Item>> tags) {
        if (tags != null) {
            this.partitionItemTags.put(partition, new ArrayList<>(tags));
        } else {
            this.partitionItemTags.remove(partition);
        }
    }
    
    /**
     * 添加一个物品标签到指定分区
     * @param partition 分区名称
     * @param tag 要添加的物品标签
     */
    public void addPartitionItemTag(String partition, TagKey<Item> tag) {
        this.partitionItemTags.computeIfAbsent(partition, k -> new ArrayList<>()).add(tag);
    }
    
    /**
     * 获取分区的物品标签限制
     * @param partition 分区名称
     * @return 该分区接受的物品标签列表
     */
    public List<TagKey<Item>> getPartitionItemTags(String partition) {
        List<TagKey<Item>> tags = this.partitionItemTags.get(partition);
        return tags != null ? new ArrayList<>(tags) : new ArrayList<TagKey<Item>>();
    }
    
    /**
     * 移除分区的物品标签限制
     * @param partition 分区名称
     */
    public void removePartitionItemTags(String partition) {
        this.partitionItemTags.remove(partition);
    }
    
    // 添加槽位映射字段
    private Map<Direction, int[]> slotMappings = new HashMap<Direction, int[]>();
    
    // 分区对应的物品标签映射
    private Map<String, List<TagKey<Item>>> partitionItemTags = new HashMap<>();
    
    //设定面的槽位映射

    public void setSlotsForFace(Direction side, int[] slots) {
        this.slotMappings.put(side, slots);
    }
    /**
     * 设定面映射分区
     */
    public void setSlotsForFace(Direction side, String partition) {
        List<Integer> slots = this.partitions.get(partition);
        if (slots != null) {
            setSlotsForFace(side, slots.stream().mapToInt(Integer::intValue).toArray());
        }
    }
    /**
     * 添加映射到面的槽位
    */
    public void addSlotsForFace(Direction side, int[] slots) {
        int[] existingSlots = this.slotMappings.get(side);
        if (existingSlots != null) {
            int[] newSlots = Arrays.copyOf(existingSlots, existingSlots.length + slots.length);
            System.arraycopy(slots, 0, newSlots, existingSlots.length, slots.length);
            this.slotMappings.put(side, newSlots);
        } else {
            this.slotMappings.put(side, slots);
        }
    }
    /**
     * 添加映射到面的分区
     */
    public void addSlotsForFace(Direction side, String partition) {
        List<Integer> slots = this.partitions.get(partition);
        if (slots != null) {
            addSlotsForFace(side, slots.stream().mapToInt(Integer::intValue).toArray());
        }
    }
    /**
     * 移除映射到面的槽位
     */

    public void removeSlotsForFace(Direction side, int[] slots) {
        int[] existingSlots = this.slotMappings.get(side);
        if (existingSlots != null) {
            int[] newSlots = Arrays.stream(existingSlots).filter(slot -> !IntStream.of(slots).anyMatch(s -> s == slot)).toArray();
            this.slotMappings.put(side, newSlots);
        }
    }
    /**
     * 移除映射到面的分区
     */
    public void removeSlotsForFace(Direction side, String partition) {
        List<Integer> slots = this.partitions.get(partition);
        if (slots != null) {
            removeSlotsForFace(side, slots.stream().mapToInt(Integer::intValue).toArray());
        }
    }
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (!this.slotMappings.containsKey(side)) {
            return null;
        }
        return this.slotMappings.get(side);
    }
    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side) {
        // 检查索引是否在允许的槽位中
        int[] allowedSlots = this.slotMappings.get(side);
        if (allowedSlots == null || allowedSlots.length == 0) {
            // 如果没有为该面定义槽位映射，返回false，由子类负责提供默认映射
            return false;
        }
        //使用caninsertitem判断物品是否能插入到该槽位
        
        for (int slot : allowedSlots) {
            if (slot == index) {
                return canInsertItem(index, stack);
            }
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
        // 检查索引是否在允许的槽位中
        int[] allowedSlots = this.slotMappings.get(side);
        if (allowedSlots == null || allowedSlots.length == 0) {
            // 如果没有为该面定义槽位映射，返回false，由子类负责提供默认映射
            return false;
        }
        
        for (int slot : allowedSlots) {
            if (slot == index) {
                return canExtractItem(index, stack);
            }
        }
        return false;
    }

    public boolean canFullyInsertItemBySlots(ItemStack stack, int[] slots) {
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
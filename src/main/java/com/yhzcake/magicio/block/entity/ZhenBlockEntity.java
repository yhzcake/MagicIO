package com.yhzcake.magicio.block.entity;

import com.yhzcake.magicio.block.zhen.ZhenBlock;
import com.yhzcake.magicio.block.zhen.ZhenMethod;
import com.yhzcake.magicio.block.zhen.ZhenType;
import com.yhzcake.magicio.block.zhen.ZhenTypes;
import com.yhzcake.magicio.item.crafting.ZhenRecipeLoader;

import com.yhzcake.magicio.item.crafting.ZhenRecipe;
import com.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings(value = {"null","unused"})
public class ZhenBlockEntity extends BlockEntity implements MenuProvider {

    // Fields
    private final ZhenType<?> type;
    private final int inputCount;
    private final int outputCount;
    
    // 在类中添加一个新的字段来跟踪输入是否发生变化
    private boolean inputsChanged = false; // 标记输入是否已改变
    
    private final ItemStackHandler inventory;
    
    // 合成相关字段
    private ZhenRecipe currentRecipe;
    private int processingTime;
    private int maxProcessingTime;
    
    public ZhenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), pos, blockState);
        if (blockState.getBlock() instanceof ZhenBlock block) {
            type = block.getType();
        } else {
            type = ZhenTypes.SMALL_SIFT_ZHEN.get();
        }
        this.inputCount = type.getInput();
        this.outputCount = type.getOutput();
        this.inventory = new ItemStackHandler(inputCount + outputCount) {
            @Override
            protected void onContentsChanged(int slot) {
                // 检查是否是输入槽位发生变化
                if (slot < inputCount) {
                    inputsChanged = true;
                }
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
                }
            }

            @Override
            public void setSize(int size) {
                super.setSize(inputCount + outputCount);
            }

            @Override
            public boolean isItemValid(int slot,   ItemStack stack) {
                return slot < inputCount;
            }

            @Override
            public   ItemStack insertItem(int slot,   ItemStack stack, boolean simulate) {
                if (slot >= inputCount) {
                    return stack;
                }
                // 检查是否是输入槽位发生变化
                inputsChanged = true;
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public   ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot < inputCount) {
                    inputsChanged = true;
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }
        };
        
        // 初始化合成相关字段
        this.currentRecipe = null;
        this.processingTime = 0;
        this.maxProcessingTime = 0;
    }

    // 基本的方块实体方法
    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // 确保在服务器端发送更新到客户端
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // 重写 saveAdditional 方法以确保不保存 components 相关数据到NBT
    @Override
    protected void saveAdditional(  CompoundTag tag, HolderLookup.  Provider provider) {
        super.saveAdditional(tag, provider);
        // 只保存inventory数据，明确删除components标签
        tag.remove("components"); // 确保删除 components 标签
        tag.put("Inventory", inventory.serializeNBT(provider));
        
        // 保存合成进度
        tag.putInt("ProcessingTime", processingTime);
        tag.putInt("MaxProcessingTime", maxProcessingTime);
    }

    // 重写 loadAdditional 方法以确保正确加载并清理 components 标签
    @Override
    public void loadAdditional(  CompoundTag tag, HolderLookup.  Provider provider) {
        super.loadAdditional(tag, provider);
        
        // 删除components标签（如果存在）
        tag.remove("components");
        
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("Inventory"));
        }
        
        // 加载合成进度
        processingTime = tag.getInt("ProcessingTime");
        maxProcessingTime = tag.getInt("MaxProcessingTime");
    }

    // 重写 getUpdateTag 方法以控制发送到客户端的数据
    @Override
    public   CompoundTag getUpdateTag(  HolderLookup.Provider provider) {
        // 获取标准的更新标签
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    // 菜单提供者接口实现
    @Override
    public   Component getDisplayName() {
        return Component.translatable("block.magicio."+type.getType());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId,   Inventory playerInventory,   Player player) {
        return null;
    }

    // 物品操作相关方法
    public void dropContents() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (level != null) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    public ItemContainerContents getContentsComponent() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return ItemContainerContents.fromItems(list);
    }
    
    public void loadFromComponent(ItemContainerContents contents) {
        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getSlots(),ItemStack.EMPTY);
        contents.copyInto(items);

        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, items.get(i));
        }
    }

    // Getter 方法
    public ZhenType<?> getZhenType() {
        return type;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
    
    public ItemStack getInputStack(int index){
        if (index >= inputCount) return ItemStack.EMPTY;
        return inventory.getStackInSlot(index);
    }

    public void setOutputStack(int index, ItemStack stack){
        int realSlot = inputCount + index;
        if(realSlot < inventory.getSlots()){
            inventory.setStackInSlot(realSlot, stack);
        }
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getOutputCount() {
        return outputCount;
    }

    // 合成相关方法
    public int getProcessingTime() {
        return processingTime;
    }
    
    public int getMaxProcessingTime() {
        return maxProcessingTime;
    }
    
    public boolean isProcessing() {
        return currentRecipe != null;
    }

    // 阵法逻辑更新
    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity entity) {
        if (entity instanceof ZhenBlockEntity zhenEntity) {
            // 获取阵法类型
            ZhenType<?> zhenType = zhenEntity.getZhenType();
            
            // 创建 ZhenMethod 实例
            ZhenMethod method = new ZhenMethod(level, pos, state, entity);
            
            // 执行阵法类型指定的方法
            zhenType.executeMethod(method);
            
            // 处理合成逻辑
            zhenEntity.processCrafting();
        }
    }
    
    // 处理合成逻辑
    private void processCrafting() {
        if (level == null || level.isClientSide()) {
            return;
        }
        
        // 如果正在处理配方
        if (currentRecipe != null) {
            // 增加处理时间
            processingTime++;
            
            // 如果处理完成
            if (processingTime >= maxProcessingTime) {
                // 检查最终输出条件
                if (canFitAllOutputs(currentRecipe.getOutputs())) {
                    // 完成合成
                    finishCrafting();
                } else {
                    // 如果没有足够空间，重置进度但保留配方
                    processingTime = 0;
                    setChanged();
                }
            }
            
            setChanged();
        } else {
            // 只有在输入发生变化或首次检测时才查找新的配方
            if (inputsChanged) {
                findAndStartRecipe();
                inputsChanged = false; // 重置标记
            }
        }
    }
    
    // 检查输入物品是否仍然有效
    private boolean areInputsValid() {
        if (currentRecipe == null) {
            return false;
        }
        
        NonNullList<Ingredient> inputs = currentRecipe.getIngredients();
        for (int i = 0; i < Math.min(inputs.size(), inputCount); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !inputs.get(i).test(stack)) {
                return false;
            }
        }
        return true;
    }
    
    // 查找并开始新的配方
    private void findAndStartRecipe() {
        // 收集输入物品
        NonNullList<ItemStack> inputItems = NonNullList.create();
        for (int i = 0; i < inputCount; i++) {
            inputItems.add(inventory.getStackInSlot(i).copy());
        }
        
        // 根据当前 ZhenBlock 的类型查找匹配的配方
        ZhenRecipe recipe = ZhenRecipeManager.getInstance().findRecipe(type.getType(), inputItems);
        if (recipe != null) {
            // 检查是否有足够的空间开始合成
            if (canFitAllOutputs(recipe.getOutputs())) {
                // 开始处理配方
                currentRecipe = recipe;
                processingTime = 0;
                maxProcessingTime = recipe.getProcessingTime();
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    // 完成合成
    private void finishCrafting() {
        if (currentRecipe == null) {
            return;
        }
        
        // 消耗输入物品
        consumeInputs();
        
        // 生成输出物品
        produceOutput();
        
        // 重置合成状态
        currentRecipe = null;
        processingTime = 0;
        maxProcessingTime = 0;
        
        // 标记方块实体已更改
        setChanged();
    }
    
    // 消耗输入物品
    private void consumeInputs() {
        NonNullList<Ingredient> inputs = currentRecipe.getIngredients();
        for (int i = 0; i < Math.min(inputs.size(), inputCount); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && inputs.get(i).test(stack)) {
                stack.shrink(1);
                inventory.setStackInSlot(i, stack);
            }
        }
    }
    
    // 生成输出物品
    private void produceOutput() {
        NonNullList<ItemStack> outputs = currentRecipe.getOutputs();
        
        // 首先检查是否有足够的空间放置所有输出物品
        if (!canFitAllOutputs(outputs)) {
            // 如果没有足够空间，不进行合成，直接返回
            return;
        }
        
        // 检查主输出是否是战利品表
        boolean isPrimaryLootTable = !outputs.isEmpty() && isLootTableMarker(outputs.getFirst());
        
        // 如果主输出是战利品表，检查是否只有一个池子
        if (isPrimaryLootTable && !isValidSinglePoolLootTable(outputs.getFirst())) {
            // 如果有多个池子，配方无效，不进行合成
            return;
        }
        
        // 创建一个跟踪输出槽位状态的数组
        ItemStack[] outputSlots = new ItemStack[outputCount];
        for (int i = 0; i < outputCount; i++) {
            outputSlots[i] = inventory.getStackInSlot(inputCount + i).copy();
        }
        
        // 处理主要输出（第一个输出）
        if (!outputs.isEmpty()) {
            ItemStack primaryOutput = outputs.getFirst().copy();
            // 检查是否是战利品表标记
            if (isLootTableMarker(primaryOutput)) {
                // 从战利品表生成实际的物品
                List<ItemStack> lootItems = generateLootFromTable(primaryOutput);
                if (lootItems != null && !lootItems.isEmpty()) {
                    // 将生成的物品添加到输出槽位
                    for (ItemStack lootItem : lootItems) {
                        if (!lootItem.isEmpty()) {
                            // 尝试在所有输出槽位中找到合适的位置
                            boolean placed = false;
                            for (int j = 0; j < outputCount; j++) {
                                if (addItemToOutputSlotWithTracking(outputSlots, j, lootItem)) {
                                    placed = true;
                                    break;
                                }
                            }
                            // 如果无法放置，理论上不会发生，因为我们已经在canFitAllOutputs中检查过了
                            if (!placed) {
                                // 作为后备，尝试放在第一个可用的槽位
                                for (int j = 0; j < outputCount; j++) {
                                    if (outputSlots[j].isEmpty()) {
                                        inventory.setStackInSlot(inputCount + j, lootItem.copy());
                                        outputSlots[j] = lootItem.copy();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 将普通物品添加到输出槽位
                addItemToOutputSlotWithTracking(outputSlots, 0, primaryOutput);
            }
        }
        
        // 处理其他输出
        for (int i = 1; i < outputs.size() && i < outputCount; i++) {
            ItemStack output = outputs.get(i).copy();
            
            // 检查是否是战利品表标记
            if (isLootTableMarker(output)) {
                // 从战利品表生成实际的物品
                List<ItemStack> lootItems = generateLootFromTable(output);
                if (lootItems != null && !lootItems.isEmpty()) {
                    // 将生成的物品添加到输出槽位
                    for (ItemStack lootItem : lootItems) {
                        if (!lootItem.isEmpty()) {
                            // 尝试在所有输出槽位中找到合适的位置
                            boolean placed = false;
                            for (int j = 0; j < outputCount; j++) {
                                if (addItemToOutputSlotWithTracking(outputSlots, j, lootItem)) {
                                    placed = true;
                                    break;
                                }
                            }
                            // 如果无法放置，理论上不会发生，因为我们已经在canFitAllOutputs中检查过了
                            if (!placed) {
                                // 作为后备，尝试放在第一个可用的槽位
                                for (int j = 0; j < outputCount; j++) {
                                    if (outputSlots[j].isEmpty()) {
                                        inventory.setStackInSlot(inputCount + j, lootItem.copy());
                                        outputSlots[j] = lootItem.copy();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 尝试在所有输出槽位中找到合适的位置
                boolean placed = false;
                for (int j = 0; j < outputCount; j++) {
                    if (addItemToOutputSlotWithTracking(outputSlots, j, output)) {
                        placed = true;
                        break;
                    }
                }
                // 如果无法放置，理论上不会发生，因为我们已经在canFitAllOutputs中检查过了
                if (!placed) {
                    // 作为后备，尝试放在第一个可用的槽位
                    for (int j = 0; j < outputCount; j++) {
                        if (outputSlots[j].isEmpty()) {
                            inventory.setStackInSlot(inputCount + j, output.copy());
                            outputSlots[j] = output.copy();
                            break;
                        }
                    }
                }
            }
        }
    }
    
    // 将物品添加到指定的输出槽位并跟踪状态
    private boolean addItemToOutputSlotWithTracking(ItemStack[] slots, int slotIndex, ItemStack itemToAdd) {
        ItemStack existingStack = slots[slotIndex];
        int inventorySlotIndex = inputCount + slotIndex;
        
        // 如果槽位为空，直接放置物品
        if (existingStack.isEmpty()) {
            ItemStack copy = itemToAdd.copy();
            inventory.setStackInSlot(inventorySlotIndex, copy);
            slots[slotIndex] = copy;
            return true;
        }
        
        // 如果槽位中有相同类型的物品，尝试合并堆叠
        if (ItemStack.isSameItemSameComponents(existingStack, itemToAdd)) {
            int maxStackSize = existingStack.getMaxStackSize();
            int currentCount = existingStack.getCount();
            int addCount = itemToAdd.getCount();
            int totalCount = currentCount + addCount;
            
            // 如果总数量不超过最大堆叠数，合并物品
            if (totalCount <= maxStackSize) {
                existingStack.setCount(totalCount);
                inventory.setStackInSlot(inventorySlotIndex, existingStack);
                slots[slotIndex] = existingStack;
                return true;
            }
        }
        
        // 无法在此槽位放置物品
        return false;
    }
    
    // 检查战利品表是否只有一个池子（有效的单池战利品表）
    private boolean isValidSinglePoolLootTable(ItemStack lootMarker) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return false; // 暂时返回false，因为当前没有实现战利品表验证
        }
        return false;
    }
    
    // 检查物品是否是战利品表标记
    private boolean isLootTableMarker(ItemStack stack) {
        // 检查物品是否包含战利品表标记
        return false; // 暂时返回false，因为当前没有实现战利品表标记检查
    }
    
    // 从战利品表生成实际的物品
    private List<ItemStack> generateLootFromTable(ItemStack marker) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // 实现战利品表物品获取逻辑
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }
    
    // 检查是否有足够的空间放置所有输出物品
    private boolean canFitAllOutputs(NonNullList<ItemStack> outputs) {
        // 创建一个模拟的输出槽位状态数组
        ItemStack[] simulatedSlots = new ItemStack[outputCount];
        for (int i = 0; i < outputCount; i++) {
            simulatedSlots[i] = inventory.getStackInSlot(inputCount + i).copy();
        }
        
        // 检查主要输出（第一个输出）
        if (!outputs.isEmpty()) {
            ItemStack primaryOutput = outputs.getFirst();
            
            // 检查是否是战利品表标记
            if (isLootTableMarker(primaryOutput)) {
                // 对于战利品表，我们需要检查是否有空间放置生成的物品
                List<ItemStack> lootItems = generateLootFromTable(primaryOutput);
                if (lootItems != null && !lootItems.isEmpty()) {
                    // 模拟将每个生成的物品放入输出槽位
                    for (ItemStack lootItem : lootItems) {
                        if (!lootItem.isEmpty()) {
                            // 尝试在所有输出槽位中找到合适的位置
                            boolean placed = false;
                            for (int j = 0; j < outputCount; j++) {
                                if (simulateAddItemToOutputSlot(simulatedSlots, j, lootItem)) {
                                    placed = true;
                                    break;
                                }
                            }
                            if (!placed) {
                                return false; // 没有足够空间
                            }
                        }
                    }
                }
            } else {
                // 检查普通物品是否有空间
                if (!simulateAddItemToOutputSlot(simulatedSlots, 0, primaryOutput)) {
                    return false; // 没有足够空间
                }
            }
        }
        
        // 检查其他输出
        for (int i = 1; i < outputs.size() && i < outputCount; i++) {
            ItemStack output = outputs.get(i);
            
            // 检查是否是战利品表标记
            if (isLootTableMarker(output)) {
                // 对于战利品表，我们需要检查是否有空间放置生成的物品
                List<ItemStack> lootItems = generateLootFromTable(output);
                if (lootItems != null && !lootItems.isEmpty()) {
                    // 模拟将每个生成的物品放入输出槽位
                    for (ItemStack lootItem : lootItems) {
                        if (!lootItem.isEmpty()) {
                            boolean placed = false;
                            // 尝试在所有输出槽位中找到合适的位置
                            for (int j = 0; j < outputCount; j++) {
                                if (simulateAddItemToOutputSlot(simulatedSlots, j, lootItem)) {
                                    placed = true;
                                    break;
                                }
                            }
                            if (!placed) {
                                return false; // 没有足够空间
                            }
                        }
                    }
                }
            } else {
                // 检查普通物品是否有空间
                boolean placed = false;
                // 尝试在所有输出槽位中找到合适的位置
                for (int j = 0; j < outputCount; j++) {
                    if (simulateAddItemToOutputSlot(simulatedSlots, j, output)) {
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    return false; // 没有足够空间
                }
            }
        }
        
        return true; // 所有物品都能放置
    }
    
    // 模拟将物品添加到指定的输出槽位（用于检查是否有足够空间）
    private boolean simulateAddItemToOutputSlot(ItemStack[] slots, int slotIndex, ItemStack itemToAdd) {
        ItemStack existingStack = slots[slotIndex];
        
        // 如果槽位为空，可以直接放置
        if (existingStack.isEmpty()) {
            slots[slotIndex] = itemToAdd.copy();
            return true;
        }
        
        // 如果槽位中有相同类型的物品，检查是否可以合并堆叠
        if (ItemStack.isSameItemSameComponents(existingStack, itemToAdd)) {
            int maxStackSize = existingStack.getMaxStackSize();
            int currentCount = existingStack.getCount();
            int addCount = itemToAdd.getCount();
            int totalCount = currentCount + addCount;
            
            // 如果总数量不超过最大堆叠数，可以合并
            if (totalCount <= maxStackSize) {
                existingStack.setCount(totalCount);
                return true;
            }
        }
        
        // 无法在此槽位放置物品
        return false;
    }
}
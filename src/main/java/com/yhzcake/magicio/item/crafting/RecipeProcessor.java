package com.yhzcake.magicio.item.crafting;

import java.util.Map;
import java.util.Set;

import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.io.IOProcessor;
import com.yhzcake.magicio.io.ModIOTypes;
import com.yhzcake.magicio.io.WorldDropIOComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings("unchecked")
public class RecipeProcessor {

    private static final int PROCESS_COLL_SPEED = 2;
    private static final int RECIPE_RECHECK_INTERVAL = 10;

    public static class State {
        public int processTime = 0;
        public boolean inputsChanged = false;
        public ZhenRecipe currentRecipe = null;
        public int recipeCheckTimer = 0;
    }

    /**
     * 执行一个 tick 的配方处理逻辑。
     * 从 AbstractZhenBlockEntity.serverTick 提取的核心逻辑，无状态、可复用。
     *
     * @param level         世界
     * @param pos           方块位置
     * @param state         配方状态（processTime / inputsChanged / currentRecipe）
     * @param zhenType      Zhen 类型名称（用于配方查找）
     * @param partition     槽位分区
     * @param items         物品槽（可以被直接修改）
     * @param tanks         流体槽（可以被直接修改）
     * @param ioProcessor   IO 处理器
     * @param zoneFaceAccess 区域-面映射（用于 DROP_OUTPUT 方向）
     * @param onChanged     数据变更回调（标记脏数据）
     * @return true 表示需要网络同步（方块外观变更）
     */
    public static boolean processTick(
            Level level,
            BlockPos pos,
            State state,
            String zhenType,
            SlotPartition partition,
            NonNullList<ItemStack> items,
            NonNullList<FluidStack> tanks,
            IOProcessor ioProcessor,
            Map<Direction, Set<String>> zoneFaceAccess,
            Runnable onChanged
    ) {
        boolean needSync = false;

        // 阶段一：没有配方 → 间隔检查（避免每 tick 扫描全部配方）
        if (state.currentRecipe == null) {
            state.recipeCheckTimer++;
            if (state.inputsChanged || state.recipeCheckTimer >= RECIPE_RECHECK_INTERVAL) {
                state.recipeCheckTimer = 0;
                if (state.inputsChanged && findRecipeFromCache(state, zhenType, items, tanks, partition, level)) {
                    needSync = true;
                }
                if (state.currentRecipe == null) {
                    tryFindNewRecipe(state, zhenType, items, tanks, partition, level);
                    if (state.currentRecipe != null) {
                        needSync = true;
                    }
                }
            }
        }

        // 阶段二：有配方 → 推进进度
        if (state.currentRecipe != null) {
            // 检查输入是否仍然匹配
            if (state.inputsChanged) {
                if (!state.currentRecipe.matches(items, partition, level)
                        || !state.currentRecipe.matchesFluid(tanks, partition)) {
                    state.currentRecipe = null;
                    state.processTime = 0;
                    state.inputsChanged = false;
                    needSync = true;
                }
            }

            if (state.currentRecipe != null) {
                state.processTime += 1;
                needSync = true;

                // 配方完成
                if (state.processTime >= state.currentRecipe.getProcessingTime()) {
                    if (tryCompleteRecipe(level, pos, state, partition, items, tanks, ioProcessor, zoneFaceAccess, onChanged)) {
                        state.processTime = 0;
                        state.currentRecipe = null;
                    } else {
                        state.processTime = Math.max(0, state.processTime - PROCESS_COLL_SPEED);
                        state.inputsChanged = true;
                    }
                }
            }
        } else if (state.processTime > 0) {
            // 没有配方时缓慢衰减进度
            state.processTime = Math.max(0, state.processTime - PROCESS_COLL_SPEED);
            needSync = true;
        }

        return needSync;
    }

    // ============ 阶段一辅助 ============

    private static boolean findRecipeFromCache(State state, String zhenType,
            NonNullList<ItemStack> items, NonNullList<FluidStack> tanks,
            SlotPartition partition, Level level) {
        // 当前实现没有缓存 lastRecipe，保留此扩展点
        return false;
    }

    private static void tryFindNewRecipe(State state, String zhenType,
            NonNullList<ItemStack> items, NonNullList<FluidStack> tanks,
            SlotPartition partition, Level level) {
        ZhenRecipe newRecipe = ZhenRecipeManager.getInstance().findRecipe(
                zhenType, items, partition, level);
        if (newRecipe != null && newRecipe.matchesFluid(tanks, partition)) {
            state.currentRecipe = newRecipe;
            state.processTime = 0;
            state.inputsChanged = false;
        }
    }

    // ============ 阶段二配方完成 ============

    /**
     * 尝试完成配方。返回 true 表示成功完成并产出了物品/流体。
     */
    private static boolean tryCompleteRecipe(
            Level level,
            BlockPos pos,
            State state,
            SlotPartition partition,
            NonNullList<ItemStack> items,
            NonNullList<FluidStack> tanks,
            IOProcessor ioProcessor,
            Map<Direction, Set<String>> zoneFaceAccess,
            Runnable onChanged
    ) {
        ZhenRecipe recipe = state.currentRecipe;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        Map<String, NonNullList<ItemStack>> zoneOutputs = recipe.rollOutput(serverLevel);
        Map<String, NonNullList<FluidStack>> fluidOutputs = recipe.rollFluidOutput();

        // 预检：所有输入输出是否可满足
        if (!canProcess(recipe, partition, ioProcessor)) return false;
        if (!canFitAllZoneItems(zoneOutputs, partition, items, ioProcessor)) return false;
        if (!canConsumeAllZoneItems(recipe, partition, items)) return false;
        if (!canFitFluidZoneOutputs(fluidOutputs, partition, tanks)) return false;
        if (!canConsumeAllFluids(recipe, partition, tanks)) return false;

        // 执行消耗
        for (RecipeInput<?> input : recipe.getInputs()) {
            if (input.type() == ModIOTypes.ITEM.get()) {
                SlotZone zone = partition.getZoneByName(input.zoneName());
                if (zone != null) {
                    consumeItemInZone(zone, (NonNullList<Ingredient>) input.requirement(), partition, items);
                }
            } else if (input.type() == ModIOTypes.FLUID.get()) {
                SlotZone zone = partition.getZoneByName(input.zoneName());
                if (zone != null) {
                    consumeFluidInZone(zone, (NonNullList<ZhenRecipe.FluidIngredient>) input.requirement(), partition, tanks);
                }
            }
        }

        // 执行产出
        for (RecipeOutput<?> output : recipe.getOutputs()) {
            if (output.zoneName().equals(SlotZone.DROP_OUTPUT.getName())) {
                handleDropOutput(level, pos, zoneOutputs, zoneFaceAccess);
            } else if (output.type() == ModIOTypes.ITEM.get()) {
                SlotZone zone = partition.getZoneByName(output.zoneName());
                if (zone != null) {
                    NonNullList<ItemStack> outputItems = zoneOutputs.get(output.zoneName());
                    if (outputItems != null) {
                        produceItemInZone(zone, outputItems, partition, items);
                    }
                }
            } else if (output.type() == ModIOTypes.FLUID.get()) {
                SlotZone zone = partition.getZoneByName(output.zoneName());
                if (zone != null) {
                    NonNullList<FluidStack> outputFluids = fluidOutputs.get(output.zoneName());
                    if (outputFluids != null) {
                        produceFluidInZone(zone, outputFluids, partition, tanks);
                    }
                }
            }
        }

        if (onChanged != null) onChanged.run();
        return true;
    }

    /** 预检：配方要求的 IOComponent 和 Zone 是否都存在 */
    private static boolean canProcess(ZhenRecipe recipe, SlotPartition partition, IOProcessor ioProcessor) {
        for (RecipeInput<?> input : recipe.getInputs()) {
            if (ioProcessor.get(input.type()) == null) return false;
            if (partition.getZoneByName(input.zoneName()) == null) return false;
        }
        for (RecipeOutput<?> output : recipe.getOutputs()) {
            if (output.zoneName().equals(SlotZone.DROP_OUTPUT.getName())) continue;
            if (ioProcessor.get(output.type()) == null) return false;
            if (partition.getZoneByName(output.zoneName()) == null) return false;
        }
        return true;
    }

    // ============ 物品相关 ============

    public static boolean canFitItem(SlotZone zone, NonNullList<ItemStack> outputs,
            SlotPartition partition, NonNullList<ItemStack> items) {
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !insertItemInZone(zone, stack.copy(), true, partition, items).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canFitAllZoneItems(Map<String, NonNullList<ItemStack>> zoneOutputs,
            SlotPartition partition, NonNullList<ItemStack> items, IOProcessor ioProcessor) {
        for (Map.Entry<String, NonNullList<ItemStack>> entry : zoneOutputs.entrySet()) {
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            if (!canFitItem(zone, entry.getValue(), partition, items)) return false;
        }
        return true;
    }

    public static boolean canConsumeItem(SlotZone zone, NonNullList<Ingredient> ingredients,
            SlotPartition partition, NonNullList<ItemStack> items) {
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
                if (ingredient.test(items.get(slot))) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean canConsumeAllZoneItems(ZhenRecipe recipe,
            SlotPartition partition, NonNullList<ItemStack> items) {
        for (RecipeInput<?> input : recipe.getInputs()) {
            if (input.type() == ModIOTypes.ITEM.get()) {
                SlotZone zone = partition.getZoneByName(input.zoneName());
                if (zone == null) return false;
                if (!canConsumeItem(zone, (NonNullList<Ingredient>) input.requirement(), partition, items)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static ItemStack insertItemInZone(SlotZone zone, ItemStack stack, boolean simulate,
            SlotPartition partition, NonNullList<ItemStack> items) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();

        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int canInsert = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canInsert > 0) {
                    if (!simulate) {
                        existing.grow(canInsert);
                    }
                    remaining.shrink(canInsert);
                }
            }
        }

        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                if (!simulate) {
                    items.set(slot, remaining.split(remaining.getCount()));
                } else {
                    remaining = ItemStack.EMPTY;
                }
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    private static void consumeItemInZone(SlotZone zone, NonNullList<Ingredient> ingredients,
            SlotPartition partition, NonNullList<ItemStack> items) {
        for (Ingredient ingredient : ingredients) {
            for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
                ItemStack existing = items.get(slot);
                if (ingredient.test(existing)) {
                    existing.shrink(1);
                    if (existing.isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                    break;
                }
            }
        }
    }

    private static void produceItemInZone(SlotZone zone, NonNullList<ItemStack> outputs,
            SlotPartition partition, NonNullList<ItemStack> items) {
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                insertItemInZone(zone, output.copy(), false, partition, items);
            }
        }
    }

    // ============ 流体相关 ============

    public static boolean canFitFluid(SlotZone zone, FluidStack fluid,
            SlotPartition partition, NonNullList<FluidStack> tanks, int tankCapacity) {
        if (fluid.isEmpty() || tankCapacity <= 0) return false;
        return insertFluidInZone(zone, fluid.copy(), true, partition, tanks, tankCapacity) >= fluid.getAmount();
    }

    private static boolean canFitFluidZoneOutputs(Map<String, NonNullList<FluidStack>> fluidOutputs,
            SlotPartition partition, NonNullList<FluidStack> tanks) {
        for (Map.Entry<String, NonNullList<FluidStack>> entry : fluidOutputs.entrySet()) {
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            for (FluidStack fluid : entry.getValue()) {
                if (!fluid.isEmpty() && !canFitFluid(zone, fluid, partition, tanks, Integer.MAX_VALUE)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean canConsumeFluid(SlotZone zone, ZhenRecipe.FluidIngredient ingredient,
            SlotPartition partition, NonNullList<FluidStack> tanks) {
        if (ingredient == null || ingredient.amount() <= 0) return false;
        for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
            if (ingredient.test(tanks.get(tank))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canConsumeAllFluids(ZhenRecipe recipe,
            SlotPartition partition, NonNullList<FluidStack> tanks) {
        for (RecipeInput<?> input : recipe.getInputs()) {
            if (input.type() != ModIOTypes.FLUID.get()) continue;
            SlotZone zone = partition.getZoneByName(input.zoneName());
            if (zone == null) return false;
            NonNullList<ZhenRecipe.FluidIngredient> ingredients =
                    (NonNullList<ZhenRecipe.FluidIngredient>) input.requirement();
            for (ZhenRecipe.FluidIngredient fluid : ingredients) {
                if (!canConsumeFluid(zone, fluid, partition, tanks)) return false;
            }
        }
        return true;
    }

    public static int insertFluidInZone(SlotZone zone, FluidStack fluid, boolean simulate,
            SlotPartition partition, NonNullList<FluidStack> tanks, int tankCapacity) {
        if (fluid.isEmpty() || tankCapacity <= 0) return 0;
        int filled = 0;
        FluidStack toFill = fluid.copy();

        for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
            if (toFill.isEmpty()) break;
            FluidStack existing = tanks.get(tank);
            if (existing.isEmpty()) {
                int canInsert = Math.min(toFill.getAmount(), tankCapacity);
                if (!simulate) {
                    tanks.set(tank, toFill.copyWithAmount(canInsert));
                }
                filled += canInsert;
                toFill.shrink(canInsert);
            } else if (FluidStack.isSameFluid(existing, toFill)) {
                int canInsert = Math.min(toFill.getAmount(), tankCapacity - existing.getAmount());
                if (canInsert > 0) {
                    if (!simulate) {
                        existing.grow(canInsert);
                    }
                    filled += canInsert;
                    toFill.shrink(canInsert);
                }
            }
        }
        return filled;
    }

    private static void consumeFluidInZone(SlotZone zone,
            NonNullList<ZhenRecipe.FluidIngredient> ingredients,
            SlotPartition partition, NonNullList<FluidStack> tanks) {
        for (ZhenRecipe.FluidIngredient ingredient : ingredients) {
            for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
                FluidStack existing = tanks.get(tank);
                if (ingredient.test(existing)) {
                    int drained = Math.min(ingredient.amount(), existing.getAmount());
                    existing.shrink(drained);
                    if (existing.isEmpty()) {
                        tanks.set(tank, FluidStack.EMPTY);
                    }
                    break;
                }
            }
        }
    }

    private static void produceFluidInZone(SlotZone zone, NonNullList<FluidStack> fluids,
            SlotPartition partition, NonNullList<FluidStack> tanks) {
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                insertFluidInZone(zone, fluid.copy(), false, partition, tanks, Integer.MAX_VALUE);
            }
        }
    }

    // ============ 世界掉落 ============

    private static void handleDropOutput(
            Level level,
            BlockPos pos,
            Map<String, NonNullList<ItemStack>> zoneOutputs,
            Map<Direction, Set<String>> zoneFaceAccess
    ) {
        Direction dropDir = Direction.UP;
        for (Direction dir : Direction.values()) {
            if (zoneFaceAccess.getOrDefault(dir, Set.of()).contains(SlotZone.DROP_OUTPUT.getName())) {
                dropDir = dir;
                break;
            }
        }
        WorldDropIOComponent dropComponent = new WorldDropIOComponent(level, pos, dropDir);
        NonNullList<ItemStack> dropItems = zoneOutputs.get(SlotZone.DROP_OUTPUT.getName());
        if (dropItems != null) {
            for (ItemStack stack : dropItems) {
                dropComponent.produce(stack);
            }
        }
    }
}

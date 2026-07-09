package cn.yhzcake.magicio.item.crafting;

import java.util.Map;
import java.util.Set;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.io.FluidIOComponent;
import cn.yhzcake.magicio.io.IOProcessor;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.WorldDropIOComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
        public ZhenRecipe lastValidRecipe = null;
        /** 实际加工时长（由配方基础时长 × 等级/插件倍率计算得出） */
        public int effectiveProcessingTime = 0;
        /** 产出倍率（由等级/插件倍率累计得出） */
        public double outputMultiplier = 1.0;
        /** 上次检查时的输入哈希值（用于避免输入未变化时的重复扫描） */
        public int lastInputHash = 0;
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
     * @param centerDrop    是否在方块正中心掉落（false 则在 DROP_OUTPUT 面外侧掉落）
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
            boolean centerDrop,
            Runnable onChanged
    ) {
        boolean needSync = false;

        // 阶段一：没有配方 → 间隔检查（避免每 tick 扫描全部配方）
        if (state.currentRecipe == null) {
            state.recipeCheckTimer++;
            if (state.inputsChanged || state.recipeCheckTimer >= RECIPE_RECHECK_INTERVAL) {
                state.recipeCheckTimer = 0;
                if (state.inputsChanged && findRecipeFromCache(state, zhenType, items, tanks, partition, level)) {
                    MagicIO.LOGGER.trace("[{}] processTick: restored recipe {} from cache", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                    needSync = true;
                }
                if (state.currentRecipe == null) {
                    tryFindNewRecipe(state, zhenType, items, tanks, partition, level);
                    if (state.currentRecipe != null) {
                        MagicIO.LOGGER.trace("[{}] processTick: found new recipe {} ({} ticks)", pos.toShortString(), state.currentRecipe.getZhenTypeStr(), state.currentRecipe.getProcessingTime());
                        needSync = true;
                    }
                }
            }
        }

        // 阶段二：有配方 → 推进进度
        if (state.currentRecipe != null) {
            // 检查输入是否仍然匹配（含等级检查）
            if (state.inputsChanged) {
                if (!isRecipeLevelAllowed(state.currentRecipe, zhenType)
                        || !state.currentRecipe.matches(items, partition, level)
                        || !state.currentRecipe.matchesFluid(tanks, partition)) {
                    MagicIO.LOGGER.trace("[{}] processTick: recipe {} no longer valid (level/input/fluid mismatch), aborting", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
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
                if (state.processTime >= state.effectiveProcessingTime) {
                    MagicIO.LOGGER.trace("[{}] processTick: recipe {} complete! attempting to produce output...", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                    if (tryCompleteRecipe(level, pos, state, partition, items, tanks, ioProcessor, zoneFaceAccess, centerDrop, onChanged)) {
                        MagicIO.LOGGER.trace("[{}] processTick: recipe {} output produced successfully", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                        // 配方成功：保持 currentRecipe，仅重置进度，继续加工（避免重新遍历配方列表）
                        state.processTime = 0;
                    } else {
                        MagicIO.LOGGER.trace("[{}] processTick: recipe {} output FAILED (output full?), backing off by {} ticks", pos.toShortString(), state.currentRecipe.getZhenTypeStr(), PROCESS_COLL_SPEED);
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

    /**
     * 计算当前输入状态的哈希值。
     * 仅基于物品 ID 和数量，不依赖 NBT（Ingredient.test 也不检查 NBT）。
     * 用于在输入未变化时跳过完整的配方扫描。
     */
    private static int computeInputHash(NonNullList<ItemStack> items, NonNullList<FluidStack> tanks,
            SlotPartition partition) {
        int hash = 1;
        for (int slot : partition.getAllSlots(ModIOTypes.ITEM.get())) {
            ItemStack stack = items.get(slot);
            hash = 31 * hash + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getCount());
        }
        for (int slot : partition.getAllSlots(ModIOTypes.FLUID.get())) {
            FluidStack fluid = tanks.get(slot);
            hash = 31 * hash + (fluid.isEmpty() ? 0 : System.identityHashCode(fluid.getFluid()));
            hash = 31 * hash + (fluid.isEmpty() ? 0 : fluid.getAmount());
        }
        return hash;
    }

    /**
     * 检查配方的等级是否不超过实际阵的等级。
     * 如果配方 zhen_type 的等级高于当前阵的等级，返回 false 禁止加工。
     */
    private static boolean isRecipeLevelAllowed(ZhenRecipe recipe, String zhenType) {
        String recipePath = recipe.getZhenTypeId().getPath();
        ZhenLevel recipeLevel = ZhenLevel.fromFullId(recipePath);
        String zhenPath = zhenType.contains(":") ? zhenType.substring(zhenType.indexOf(':') + 1) : zhenType;
        ZhenLevel actualLevel = ZhenLevel.fromFullId(zhenPath);

        // 无法识别等级时，保守地允许（保持兼容）
        if (recipeLevel == null || actualLevel == null) return true;
        return recipeLevel.level() <= actualLevel.level();
    }

    // ============ 阶段一辅助 ============

    private static boolean findRecipeFromCache(State state, String zhenType,
            NonNullList<ItemStack> items, NonNullList<FluidStack> tanks,
            SlotPartition partition, Level level) {
        if (state.lastValidRecipe == null) return false;
        if (!isRecipeLevelAllowed(state.lastValidRecipe, zhenType)) {
            state.lastValidRecipe = null;
            return false;
        }
        if (state.lastValidRecipe.matches(items, partition, level)
                && state.lastValidRecipe.matchesFluid(tanks, partition)) {
            state.currentRecipe = state.lastValidRecipe;
            computeEffectiveValues(state, zhenType);
            state.processTime = 0;
            state.inputsChanged = false;
            return true;
        }
        state.lastValidRecipe = null;
        return false;
    }

    private static void tryFindNewRecipe(State state, String zhenType,
            NonNullList<ItemStack> items, NonNullList<FluidStack> tanks,
            SlotPartition partition, Level level) {
        int currentHash = computeInputHash(items, tanks, partition);
        // 输入未变化且上次缓存的配方仍然有效：直接复用，跳过线性扫描
        if (currentHash == state.lastInputHash && state.lastValidRecipe != null
                && state.lastValidRecipe.matchesFluid(tanks, partition)) {
            state.currentRecipe = state.lastValidRecipe;
            computeEffectiveValues(state, zhenType);
            state.processTime = 0;
            state.inputsChanged = false;
            return;
        }

        ZhenRecipe newRecipe = ZhenRecipeManager.getInstance().findRecipe(
                zhenType, items, partition, level);
        if (newRecipe != null && newRecipe.matchesFluid(tanks, partition)
                && isRecipeLevelAllowed(newRecipe, zhenType)) {
            state.currentRecipe = newRecipe;
            computeEffectiveValues(state, zhenType);
            state.lastValidRecipe = newRecipe;
            state.processTime = 0;
            state.inputsChanged = false;
        } else {
            state.lastValidRecipe = null;
        }
        state.lastInputHash = currentHash;
    }

    /**
     * 根据配方所在等级和实际阵等级的差值，计算有效加工时长和产出倍率。
     * 同时叠加 RecipeModifiers 的贡献。
     */
    private static void computeEffectiveValues(State state, String zhenType) {
        if (state.currentRecipe == null) {
            state.effectiveProcessingTime = 0;
            state.outputMultiplier = 1.0;
            return;
        }

        // 提取配方等级（配方自身的 zhen_type_id 中的等级）
        String recipePath = state.currentRecipe.getZhenTypeId().getPath();
        ZhenLevel recipeLevel = ZhenLevel.fromFullId(recipePath);
        // 提取实际阵等级
        String zhenPath = zhenType.contains(":") ? zhenType.substring(zhenType.indexOf(':') + 1) : zhenType;
        ZhenLevel actualLevel = ZhenLevel.fromFullId(zhenPath);

        if (recipeLevel == null || actualLevel == null) {
            // 无法识别等级，使用原始值
            state.effectiveProcessingTime = state.currentRecipe.getProcessingTime();
            state.outputMultiplier = 1.0;
            return;
        }

        double speedMult = ZhenLevel.totalSpeedMultiplier(recipeLevel.level(), actualLevel.level());
        double outputMult = ZhenLevel.totalOutputMultiplier(recipeLevel.level(), actualLevel.level());

        // 叠加 RecipeModifiers
        String typeIdStr = "magic_io:" + zhenPath;
        speedMult *= RecipeModifiers.getTimeMultiplier(typeIdStr, state.currentRecipe);
        outputMult *= RecipeModifiers.getOutputMultiplier(typeIdStr, state.currentRecipe);

        state.effectiveProcessingTime = (int) Math.round(state.currentRecipe.getProcessingTime() * speedMult);
        if (state.effectiveProcessingTime < 1) state.effectiveProcessingTime = 1;
        state.outputMultiplier = outputMult;
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
            boolean centerDrop,
            Runnable onChanged
    ) {
        ZhenRecipe recipe = state.currentRecipe;
        if (!(level instanceof ServerLevel serverLevel)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: not a ServerLevel, aborting", pos.toShortString());
            return false;
        }

        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: rolling outputs for recipe {}...", pos.toShortString(), recipe.getZhenTypeStr());
        Map<String, NonNullList<ItemStack>> zoneOutputs = recipe.rollOutput(serverLevel);
        // 应用产出倍率
        if (state.outputMultiplier != 1.0) {
            for (NonNullList<ItemStack> stacks : zoneOutputs.values()) {
                for (int i = 0; i < stacks.size(); i++) {
                    ItemStack stack = stacks.get(i);
                    if (!stack.isEmpty()) {
                        int newCount = (int) Math.round(stack.getCount() * state.outputMultiplier);
                        if (newCount < 1) newCount = 1;
                        stack.setCount(newCount);
                    }
                }
            }
        }
        Map<String, NonNullList<FluidStack>> fluidOutputs = recipe.rollFluidOutput();
        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: zoneOutputs={}, fluidOutputs={}", pos.toShortString(), zoneOutputs, fluidOutputs);

        // 获取实际流体罐容量
        int tankCapacity = 0;
        Object rawFluid = ioProcessor.get(ModIOTypes.FLUID.get());
        if (rawFluid instanceof FluidIOComponent fluidComp) {
            Integer cap = fluidComp.getTankCapacity();
            if (cap != null) tankCapacity = cap;
        }

        // 预检：所有输入输出是否可满足
        if (!canProcess(recipe, partition, ioProcessor)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canProcess (missing IO or Zone)", pos.toShortString());
            return false;
        }
        if (!canFitAllZoneItems(zoneOutputs, partition, items, ioProcessor)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canFitAllZoneItems (output full?)", pos.toShortString());
            return false;
        }
        if (!canConsumeAllZoneItems(recipe, partition, items)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canConsumeAllZoneItems (input missing?)", pos.toShortString());
            return false;
        }
        if (!canFitFluidZoneOutputs(fluidOutputs, partition, tanks, tankCapacity)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canFitFluidZoneOutputs (tankCapacity={}, fluidOutputs={})", pos.toShortString(), tankCapacity, fluidOutputs);
            return false;
        }
        if (!canConsumeAllFluids(recipe, partition, tanks)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canConsumeAllFluids", pos.toShortString());
            return false;
        }
        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: all checks passed, executing...", pos.toShortString());

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
                handleDropOutput(level, pos, zoneOutputs, zoneFaceAccess, centerDrop);
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
                        produceFluidInZone(zone, outputFluids, partition, tanks, tankCapacity);
                        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: produced fluid in zone {}", pos.toShortString(), output.zoneName());
                    }
                }
            }
        }

        if (onChanged != null) onChanged.run();
        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: SUCCESS", pos.toShortString());
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
            if (entry.getKey().equals(SlotZone.DROP_OUTPUT.getName())) continue;
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
            SlotPartition partition, NonNullList<FluidStack> tanks, int tankCapacity) {
        if (fluidOutputs.isEmpty()) return true;
        if (tankCapacity <= 0) return false;
        for (Map.Entry<String, NonNullList<FluidStack>> entry : fluidOutputs.entrySet()) {
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            for (FluidStack fluid : entry.getValue()) {
                if (!fluid.isEmpty() && !canFitFluid(zone, fluid, partition, tanks, tankCapacity)) {
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
            SlotPartition partition, NonNullList<FluidStack> tanks, int tankCapacity) {
        if (tankCapacity <= 0) return;
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                insertFluidInZone(zone, fluid.copy(), false, partition, tanks, tankCapacity);
            }
        }
    }

    // ============ 世界掉落 ============

    private static void handleDropOutput(
            Level level,
            BlockPos pos,
            Map<String, NonNullList<ItemStack>> zoneOutputs,
            Map<Direction, Set<String>> zoneFaceAccess,
            boolean centerDrop
    ) {
        NonNullList<ItemStack> dropItems = zoneOutputs.get(SlotZone.DROP_OUTPUT.getName());
        if (dropItems == null) {
            MagicIO.LOGGER.trace("[{}] handleDropOutput: DROP_OUTPUT zone not found in zoneOutputs, no items to drop", pos.toShortString());
            return;
        }
        if (dropItems.isEmpty()) {
            MagicIO.LOGGER.trace("[{}] handleDropOutput: DROP_OUTPUT items list is empty", pos.toShortString());
            return;
        }

        Direction dropDir = Direction.UP;
        for (Direction dir : Direction.values()) {
            if (zoneFaceAccess.getOrDefault(dir, Set.of()).contains(SlotZone.DROP_OUTPUT.getName())) {
                dropDir = dir;
                break;
            }
        }
        MagicIO.LOGGER.trace("[{}] handleDropOutput: dropping {} items towards {}: {}", pos.toShortString(), dropItems.size(), dropDir, dropItems);

        if (centerDrop) {
            Vec3 center = Vec3.atCenterOf(pos);
            for (ItemStack stack : dropItems) {
                if (stack.isEmpty()) continue;
                ItemEntity item = new ItemEntity(level, center.x, center.y - 0.4, center.z, stack, 0, 0, 0);
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
        } else {
            boolean canDrop = level.getBlockState(pos.relative(dropDir)).isAir();
            if (!canDrop) return;
            WorldDropIOComponent dropComponent = new WorldDropIOComponent(level, pos, dropDir);
            for (ItemStack stack : dropItems) {
                dropComponent.produce(stack);
            }
        }
    }
}

package cn.yhzcake.magicio.item.crafting;

import java.util.Map;
import java.util.Set;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.io.IOComponent;
import cn.yhzcake.magicio.io.IOProcessor;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.IOTypeDescriptor;
import cn.yhzcake.magicio.io.IOTypeDescriptors;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.WorldDropSink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

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
        /** 捕获配方版本号，用于检测重载后旧配方引用失效 */
        public int recipeGeneration = 0;
        public long stateRevision = 0;
        public long cycleId = 0;
        public boolean outputBlocked = false;
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
     */
    public static void processTick(
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
        Identifier previousRecipeId = state.currentRecipe == null ? null : state.currentRecipe.getRecipeId();
        long previousCycleId = state.cycleId;
        boolean previousOutputBlocked = state.outputBlocked;

        // 阶段一：没有配方 → 间隔检查（避免每 tick 扫描全部配方）
        if (state.currentRecipe == null) {
            state.recipeCheckTimer++;
            if (state.inputsChanged || state.recipeCheckTimer >= RECIPE_RECHECK_INTERVAL) {
                state.recipeCheckTimer = 0;
                if (state.inputsChanged && findRecipeFromCache(state, zhenType, ioProcessor, partition, level)) {
                    MagicIO.LOGGER.trace("[{}] processTick: restored recipe {} from cache", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                    state.recipeGeneration = ZhenRecipeManager.getRecipeGeneration();
                }
                if (state.currentRecipe == null) {
                    tryFindNewRecipe(state, zhenType, ioProcessor, partition, level);
                    if (state.currentRecipe != null) {
                        MagicIO.LOGGER.trace("[{}] processTick: found new recipe {} ({} ticks)", pos.toShortString(), state.currentRecipe.getZhenTypeStr(), state.currentRecipe.getProcessingTime());
                        state.recipeGeneration = ZhenRecipeManager.getRecipeGeneration();
                    }
                }
            }
        }

        // 阶段二：有配方 → 推进进度
        if (state.currentRecipe != null) {
            // 检查配方版本：资源重载后管理器版本递增，机器旧引用应失效
            if (state.recipeGeneration != ZhenRecipeManager.getRecipeGeneration()) {
                MagicIO.LOGGER.trace("[{}] processTick: recipe generation changed ({} vs {}), invalidating",
                        pos.toShortString(), state.recipeGeneration, ZhenRecipeManager.getRecipeGeneration());
                state.currentRecipe = null;
                state.lastValidRecipe = null;
                state.processTime = 0;
                state.inputsChanged = true;
                state.recipeGeneration = 0;
                state.outputBlocked = false;
            }

            // 检查输入是否仍然匹配（含等级检查）
            if (state.inputsChanged && state.currentRecipe != null) {
                if (!isRecipeLevelAllowed(state.currentRecipe, zhenType)
                        || !state.currentRecipe.matchesAll(ioProcessor, partition, level)) {
                    MagicIO.LOGGER.trace("[{}] processTick: recipe {} no longer valid (level/input/fluid mismatch), aborting", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                    state.currentRecipe = null;
                    state.processTime = 0;
                    state.inputsChanged = false;
                    state.outputBlocked = false;
                }
            }

            if (state.currentRecipe != null) {
                state.processTime += 1;
                // 配方完成
                if (state.processTime >= state.effectiveProcessingTime) {
                    MagicIO.LOGGER.trace("[{}] processTick: recipe {} complete! attempting to produce output...", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                    if (tryCompleteRecipe(level, pos, state, partition, items, tanks, ioProcessor, zoneFaceAccess, centerDrop, onChanged)) {
                        MagicIO.LOGGER.trace("[{}] processTick: recipe {} output produced successfully", pos.toShortString(), state.currentRecipe.getZhenTypeStr());
                        // 配方成功：保持 currentRecipe，仅重置进度，继续加工（避免重新遍历配方列表）
                        state.processTime = 0;
                        state.outputBlocked = false;
                        state.cycleId++;
                    } else {
                        MagicIO.LOGGER.trace("[{}] processTick: recipe {} output FAILED (output full?), backing off by {} ticks", pos.toShortString(), state.currentRecipe.getZhenTypeStr(), PROCESS_COLL_SPEED);
                        state.processTime = Math.max(0, state.processTime - PROCESS_COLL_SPEED);
                        state.inputsChanged = true;
                        state.outputBlocked = true;
                    }
                }
            }
        } else if (state.processTime > 0) {
            // 没有配方时缓慢衰减进度
            state.processTime = Math.max(0, state.processTime - PROCESS_COLL_SPEED);
        }

        Identifier currentRecipeId = state.currentRecipe == null ? null : state.currentRecipe.getRecipeId();
        boolean stateChanged = !java.util.Objects.equals(previousRecipeId, currentRecipeId)
                || previousCycleId != state.cycleId
                || previousOutputBlocked != state.outputBlocked;
        if (stateChanged) {
            state.stateRevision++;
        }
        if (stateChanged || state.currentRecipe != null || state.processTime > 0) onChanged.run();
    }

    public static ProcessingStateSnapshot snapshot(State state) {
        ProcessingPhase phase = state.currentRecipe == null
                ? ProcessingPhase.IDLE
                : state.outputBlocked ? ProcessingPhase.OUTPUT_BLOCKED : ProcessingPhase.RUNNING;
        return new ProcessingStateSnapshot(
                state.stateRevision,
                state.cycleId,
                phase,
                state.currentRecipe == null ? null : state.currentRecipe.getRecipeId(),
                state.processTime,
                state.effectiveProcessingTime);
    }

    /**
     * 计算当前输入状态的哈希值。
     * 通过 IOTypeDescriptor 统一计算各 IOType 的输入哈希。
     * 用于在输入未变化时跳过完整的配方扫描。
     */
    private static int computeInputHash(IOProcessor ioProcessor, SlotPartition partition) {
        int hash = 1;
        for (IOType type : IOTypeDescriptors.getRegisteredTypes()) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(type);
            if (descriptor == null) continue;
            IOComponent<?, ?> component = ioProcessor.get(type);
            if (component == null) continue;
            hash = 31 * hash + descriptor.computeHash(component, partition);
        }
        return hash;
    }

    /**
     * 检查配方的等级是否不超过实际阵的等级。
     * 如果配方 zhen_type 的等级高于当前阵的等级，返回 false 禁止加工。
     */
    public static boolean isRecipeLevelAllowed(ZhenRecipe recipe, String zhenType) {
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
            IOProcessor ioProcessor,
            SlotPartition partition, Level level) {
        if (state.lastValidRecipe == null) return false;
        if (!isRecipeLevelAllowed(state.lastValidRecipe, zhenType)) {
            state.lastValidRecipe = null;
            return false;
        }
        ZhenRecipe cached = resolveCurrentRecipe(state.lastValidRecipe, zhenType);
        if (cached == null) {
            state.lastValidRecipe = null;
            return false;
        }
        state.lastValidRecipe = cached;
        if (cached.matchesAll(ioProcessor, partition, level)) {
            state.currentRecipe = cached;
            computeEffectiveValues(state, zhenType);
            state.processTime = 0;
            state.inputsChanged = false;
            state.outputBlocked = false;
            return true;
        }
        state.lastValidRecipe = null;
        return false;
    }

    private static ZhenRecipe resolveCurrentRecipe(ZhenRecipe cached, String zhenType) {
        for (ZhenRecipe r : ZhenRecipeManager.getInstance().getRecipes(zhenType)) {
            if (r.getRecipeId().equals(cached.getRecipeId())
                    && r.getZhenTypeStr().equals(cached.getZhenTypeStr())) {
                return r;
            }
        }
        return null;
    }

    private static void tryFindNewRecipe(State state, String zhenType,
            IOProcessor ioProcessor,
            SlotPartition partition, Level level) {
        int currentHash = computeInputHash(ioProcessor, partition);
        // 输入未变化且上次缓存的配方仍然有效：直接复用，跳过线性扫描
        ZhenRecipe cached = state.lastValidRecipe == null
                ? null : resolveCurrentRecipe(state.lastValidRecipe, zhenType);
        if (currentHash == state.lastInputHash && cached != null
                && isRecipeLevelAllowed(cached, zhenType)
                && cached.matchesAll(ioProcessor, partition, level)) {
            state.lastValidRecipe = cached;
            state.currentRecipe = cached;
            computeEffectiveValues(state, zhenType);
            state.processTime = 0;
            state.inputsChanged = false;
            state.outputBlocked = false;
            return;
        }

        ZhenRecipe newRecipe = ZhenRecipeManager.getInstance().findRecipe(
                zhenType, ioProcessor, partition, level);
        if (newRecipe != null && isRecipeLevelAllowed(newRecipe, zhenType)) {
            state.currentRecipe = newRecipe;
            computeEffectiveValues(state, zhenType);
            state.lastValidRecipe = newRecipe;
            state.processTime = 0;
            state.inputsChanged = false;
            state.outputBlocked = false;
        } else {
            state.lastValidRecipe = null;
        }
        state.lastInputHash = currentHash;
    }

    /**
     * 根据配方所在等级和实际阵等级的差值，计算有效加工时长和产出倍率。
     * 同时叠加 RecipeModifiers 的贡献。
     * 公开方法，供方块实体从 NBT 恢复配方后重新计算有效参数。
     */
    public static void computeEffectiveValues(State state, String zhenType) {
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
        Map<IOType, Map<String, NonNullList<?>>> allOutputs = recipe.rollAllOutputs(serverLevel);

        // 应用产出倍率（仅 ITEM 型产出受倍率影响）
        if (state.outputMultiplier != 1.0) {
            Map<String, NonNullList<?>> itemOutputs = allOutputs.get(ModIOTypes.ITEM.get());
            if (itemOutputs != null) {
                for (NonNullList<?> stacks : itemOutputs.values()) {
                    for (int i = 0; i < stacks.size(); i++) {
                        Object obj = stacks.get(i);
                        if (obj instanceof ItemStack stack) {
                            int newCount = (int) Math.round(stack.getCount() * state.outputMultiplier);
                            if (newCount < 1) newCount = 1;
                            stack.setCount(newCount);
                        }
                    }
                }
            }
        }

        // 获取 ITEM 掉落专用输出映射（用于掉落出口检查）
        Map<String, NonNullList<ItemStack>> dropOutputs = extractDropOutputs(allOutputs);

        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: allOutputs={}", pos.toShortString(), allOutputs);

        // 预检：配方要求的 IOComponent 和 Zone 是否存在
        if (!canProcess(recipe, partition, ioProcessor)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canProcess (missing IO or Zone)", pos.toShortString());
            return false;
        }

        // 先检查世界掉落出口是否可用（避免消耗输入后掉落被阻挡而损失物品）
        if (!canDropToWorld(dropOutputs, zoneFaceAccess, level, pos, centerDrop)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED canDropToWorld (drop direction blocked)", pos.toShortString());
            return false;
        }

        // 事务性验证：通过 IOTypeDescriptor 在快照上模拟全部消耗和产出
        if (!validateTransaction(recipe, partition, ioProcessor, allOutputs)) {
            MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: FAILED validateTransaction (insufficient input or output space)", pos.toShortString());
            return false;
        }
        MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: all checks passed, executing...", pos.toShortString());

        // 通过 IOTypeDescriptor 统一执行消耗
        for (RecipeInput<?> input : recipe.getInputs()) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(input.type());
            if (descriptor == null) continue;
            IOComponent<?, ?> component = ioProcessor.get(input.type());
            if (component == null) continue;
            SlotZone zone = partition.getZoneByName(input.zoneName());
            if (zone != null) {
                descriptor.commitConsume(component, zone, input.requirement(), partition);
            }
        }

        // 按聚合后的 IOType 和 Zone 统一提交产出
        for (Map.Entry<IOType, Map<String, NonNullList<?>>> typeEntry : allOutputs.entrySet()) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(typeEntry.getKey());
            if (descriptor == null) continue;
            IOComponent<?, ?> component = ioProcessor.get(typeEntry.getKey());
            if (component == null) continue;
            for (Map.Entry<String, NonNullList<?>> zoneEntry : typeEntry.getValue().entrySet()) {
                if (zoneEntry.getKey().equals(SlotZone.DROP_OUTPUT.getName())) continue;
                SlotZone zone = partition.getZoneByName(zoneEntry.getKey());
                if (zone == null || zoneEntry.getValue().isEmpty()) continue;
                descriptor.commitProduce(component, zone, zoneEntry.getValue(), partition);
                MagicIO.LOGGER.trace("[{}] tryCompleteRecipe: produced {} in zone {}", pos.toShortString(), typeEntry.getKey(), zoneEntry.getKey());
            }
        }

        handleDropOutput(level, pos, dropOutputs, zoneFaceAccess, centerDrop);

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

    /**
     * 事务性验证：通过 IOTypeDescriptor 创建快照并在快照上模拟全部消耗和产出。
     * 全部通过才返回 true，真实状态不受任何影响。
     * <p>
     * 新增 IOType 时只需实现对应的 {@link IOTypeDescriptor} 并注册到
     * {@link IOTypeDescriptors}，无需修改此方法。
     */
    private static boolean validateTransaction(
            ZhenRecipe recipe, SlotPartition partition,
            IOProcessor ioProcessor,
            Map<IOType, Map<String, NonNullList<?>>> allOutputs) {

        // 创建各 IOType 的快照
        java.util.Map<IOType, Object> snapshots = new java.util.HashMap<>();
        for (RecipeInput<?> input : recipe.getInputs()) {
            IOType type = input.type();
            if (snapshots.containsKey(type)) continue;
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(type);
            if (descriptor == null) return false;
            IOComponent<?, ?> component = ioProcessor.get(type);
            if (component == null) return false;
            snapshots.put(type, descriptor.createSnapshot(component));
        }
        for (RecipeOutput<?> output : recipe.getOutputs()) {
            IOType type = output.type();
            if (snapshots.containsKey(type)) continue;
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(type);
            if (descriptor == null) continue;
            IOComponent<?, ?> component = ioProcessor.get(type);
            if (component == null) continue;
            snapshots.put(type, descriptor.createSnapshot(component));
        }

        // 在快照上模拟所有输入消耗
        for (RecipeInput<?> input : recipe.getInputs()) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(input.type());
            if (descriptor == null) return false;
            Object snapshot = snapshots.get(input.type());
            if (snapshot == null) return false;
            SlotZone zone = partition.getZoneByName(input.zoneName());
            if (zone == null) return false;
            if (!descriptor.simulateConsume(snapshot, zone, input.requirement(), partition)) {
                return false;
            }
        }

        // 在消耗后的快照上按聚合键模拟所有产出
        for (Map.Entry<IOType, Map<String, NonNullList<?>>> typeEntry : allOutputs.entrySet()) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(typeEntry.getKey());
            if (descriptor == null) return false;
            Object snapshot = snapshots.get(typeEntry.getKey());
            if (snapshot == null) return false;
            for (Map.Entry<String, NonNullList<?>> zoneEntry : typeEntry.getValue().entrySet()) {
                if (zoneEntry.getKey().equals(SlotZone.DROP_OUTPUT.getName())) continue;
                SlotZone zone = partition.getZoneByName(zoneEntry.getKey());
                if (zone == null) return false;
                if (!zoneEntry.getValue().isEmpty()
                        && !descriptor.simulateProduce(snapshot, zone, zoneEntry.getValue(), partition)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 从统一产出映射中提取 DROP_OUTPUT 区域的 ITEM 产出。
     */
    private static Map<String, NonNullList<ItemStack>> extractDropOutputs(
            Map<IOType, Map<String, NonNullList<?>>> allOutputs) {
        Map<String, NonNullList<ItemStack>> dropOutputs = new java.util.LinkedHashMap<>();
        Map<String, NonNullList<?>> itemOutputs = allOutputs.get(ModIOTypes.ITEM.get());
        if (itemOutputs != null) {
            NonNullList<?> drops = itemOutputs.get(SlotZone.DROP_OUTPUT.getName());
            if (drops != null && !drops.isEmpty()) {
                NonNullList<ItemStack> items = NonNullList.create();
                for (Object obj : drops) {
                    if (obj instanceof ItemStack stack) items.add(stack);
                }
                dropOutputs.put(SlotZone.DROP_OUTPUT.getName(), items);
            }
        }
        return dropOutputs;
    }

    /**
     * 检查世界掉落出口是否可用。
     * 如果有掉落输出需求且不是中心掉落模式，则需要掉落方向对面是空气。
     * 此检查必须在消耗输入之前执行，避免物品被消耗后掉落被阻挡而丢失。
     */
    private static boolean canDropToWorld(
            Map<String, NonNullList<ItemStack>> zoneOutputs,
            Map<Direction, Set<String>> zoneFaceAccess,
            Level level, BlockPos pos, boolean centerDrop) {
        NonNullList<ItemStack> dropItems = zoneOutputs.get(SlotZone.DROP_OUTPUT.getName());
        if (dropItems == null || dropItems.isEmpty()) return true; // 没有掉落需求
        if (centerDrop) return true; // 中心掉落不需要检查出口

        Direction dropDir = Direction.UP;
        for (Direction dir : Direction.values()) {
            if (zoneFaceAccess.getOrDefault(dir, Set.of()).contains(SlotZone.DROP_OUTPUT.getName())) {
                dropDir = dir;
                break;
            }
        }

        boolean canDrop = level.getBlockState(pos.relative(dropDir)).isAir();
        if (!canDrop) {
            MagicIO.LOGGER.trace("[{}] canDropToWorld: drop direction {} blocked", pos.toShortString(), dropDir);
        }
        return canDrop;
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
            WorldDropSink dropComponent = new WorldDropSink(level, pos, dropDir);
            for (ItemStack stack : dropItems) {
                dropComponent.drop(stack);
            }
        }
    }
}

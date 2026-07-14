package cn.yhzcake.magicio.item.crafting;

import java.util.List;
import java.util.Map;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.io.IOComponent;
import cn.yhzcake.magicio.io.IOProcessor;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.IOTypeDescriptor;
import cn.yhzcake.magicio.io.IOTypeDescriptors;
import cn.yhzcake.magicio.io.ModIOTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;

public class ZhenRecipe implements Recipe<ZhenRecipeInput> {
    private final Identifier recipeId;
    private final Identifier zhenTypeId;
    private final List<RecipeInput<?>> inputs;
    private final List<RecipeOutput<?>> outputs;
    private final int processingTime;

    public ZhenRecipe(Identifier recipeId, Identifier zhenTypeId, List<RecipeInput<?>> inputs, List<RecipeOutput<?>> outputs, int processingTime) {
        this.recipeId = recipeId;
        this.zhenTypeId = zhenTypeId;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.processingTime = processingTime;
    }

    public ZhenRecipe(Identifier zhenTypeId, List<RecipeInput<?>> inputs, List<RecipeOutput<?>> outputs, int processingTime) {
        this(zhenTypeId, zhenTypeId, inputs, outputs, processingTime);
    }

    public Identifier getRecipeId() {
        return recipeId;
    }

    public String getZhenTypeStr() {
        return zhenTypeId.toString();
    }

    public Identifier getZhenTypeId() {
        return zhenTypeId;
    }

    public List<RecipeInput<?>> getInputs() {
        return inputs;
    }

    public List<RecipeOutput<?>> getOutputs() {
        return outputs;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    /**
     * 通过 IOTypeDescriptor 统一匹配所有类型的输入。
     *
     * @param ioProcessor IO 处理器（包含所有已注册的组件）
     * @param partition   槽位分区
     * @param level       世界
     * @return 所有输入是否都满足
     */
    public boolean matchesAll(IOProcessor ioProcessor, SlotPartition partition, Level level) {
        Map<IOType, Object> snapshots = new java.util.HashMap<>();
        for (RecipeInput<?> input : inputs) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(input.type());
            if (descriptor == null) return false;
            IOComponent<?, ?> component = ioProcessor.get(input.type());
            if (component == null) return false;
            Object snapshot = snapshots.computeIfAbsent(input.type(), ignored -> descriptor.createSnapshot(component));
            SlotZone zone = partition.getZoneByName(input.zoneName());
            if (zone == null) return false;
            if (!descriptor.simulateConsume(snapshot, zone, input.requirement(), partition)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 通过 IOTypeDescriptor 统一滚动所有类型的产出。
     *
     * @param level 服务端世界（用于战利品表）
     * @return 按 IOType → 区域名 → 产出值列表 聚合的结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<IOType, Map<String, NonNullList<?>>> rollAllOutputs(ServerLevel level) {
        Map<IOType, Map<String, NonNullList<?>>> result = new java.util.LinkedHashMap<>();
        for (RecipeOutput<?> output : outputs) {
            IOTypeDescriptor descriptor = IOTypeDescriptors.get(output.type());
            if (descriptor == null) continue;
            NonNullList<?> rolled = descriptor.rollOutputs(output.specification(), level);
            if (rolled == null || rolled.isEmpty()) continue;
            NonNullList target = result.computeIfAbsent(output.type(), k -> new java.util.LinkedHashMap<>())
                  .computeIfAbsent(output.zoneName(), k -> NonNullList.create());
            target.addAll(rolled);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ZhenRecipeInput input, Level level) {
        boolean hasItemInput = false;
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) remaining.set(i, input.getItem(i).copy());
        for (RecipeInput<?> recipeInput : inputs) {
            if (recipeInput.type() != ModIOTypes.ITEM.get()) continue;
            hasItemInput = true;
            NonNullList<ItemRequirement> requirements = (NonNullList<ItemRequirement>) recipeInput.requirement();
            for (ItemRequirement requirement : requirements) {
                int required = requirement.count();
                for (int i = 0; i < remaining.size(); i++) {
                    ItemStack stack = remaining.get(i);
                    if (!stack.isEmpty() && requirement.ingredient().test(stack)) {
                        int consumed = Math.min(required, stack.getCount());
                        stack.shrink(consumed);
                        required -= consumed;
                        if (required == 0) break;
                    }
                }
                if (required > 0) return false;
            }
        }
        return hasItemInput || inputs.isEmpty();
    }

    public ItemStack assemble(ZhenRecipeInput input, HolderLookup.Provider registries) {
        var outputs = getFixedOutputs();
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    public ItemStack getResultItem(HolderLookup.Provider registries) {
        var outputs = getFixedOutputs();
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    public RecipeSerializer<? extends Recipe<ZhenRecipeInput>> getSerializer() {
        return ZhenRecipeSerializer.INSTANCE;
    }

    public RecipeType<? extends Recipe<ZhenRecipeInput>> getType() {
        return ModRecipeManager.ZHEN_RECIPE.get();
    }

    @SuppressWarnings("unchecked")
    public NonNullList<ItemRequirement> getItemRequirements() {
        NonNullList<ItemRequirement> flat = NonNullList.create();
        for (RecipeInput<?> input : inputs) {
            if (input.type() == ModIOTypes.ITEM.get()) {
                flat.addAll((NonNullList<ItemRequirement>) input.requirement());
            }
        }
        return flat;
    }

    @SuppressWarnings("unchecked")
    public NonNullList<ItemStack> getFixedOutputs() {
        NonNullList<ItemStack> flat = NonNullList.create();
        for (RecipeOutput<?> output : outputs) {
            if (output.type() == ModIOTypes.ITEM.get()) {
                NonNullList<OutputEntry> entries = (NonNullList<OutputEntry>) output.specification();
                for (OutputEntry entry : entries) {
                    if (entry.stack() != null && !entry.stack().isEmpty()) {
                        flat.add(entry.stack());
                    }
                }
            }
        }
        return flat;
    }

    @Override
    public ItemStack assemble(ZhenRecipeInput input) {
        var outputs = getFixedOutputs();
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return null;
    }

}

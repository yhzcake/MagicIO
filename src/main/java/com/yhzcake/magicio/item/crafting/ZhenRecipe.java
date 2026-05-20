package com.yhzcake.magicio.item.crafting;

import java.util.List;
import java.util.Map;

import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.io.ModIOTypes;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.Level;

public class ZhenRecipe implements Recipe<ZhenRecipeInput> {
    private final String type;
    private final List<RecipeInput<?>> inputs;
    private final List<RecipeOutput<?>> outputs;
    private final int processingTime;

    public ZhenRecipe(String type, List<RecipeInput<?>> inputs, List<RecipeOutput<?>> outputs, int processingTime) {
        this.type = type;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.processingTime = processingTime;
    }

    public String getRecipeType() {
        return type;
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

    @SuppressWarnings("unchecked")
    public Map<String, NonNullList<ItemStack>> rollOutput(ServerLevel level) {
        Map<String, NonNullList<ItemStack>> result = new java.util.LinkedHashMap<>();
        for (RecipeOutput<?> output : outputs) {
            if (output.type() == ModIOTypes.ITEM.get()) {
                NonNullList<OutputEntry> entries = (NonNullList<OutputEntry>) output.specification();
                NonNullList<ItemStack> rolled = NonNullList.create();
                for (OutputEntry entry : entries) {
                    rolled.addAll(entry.roll(level));
                }
                result.merge(output.zoneName(), rolled, (a, b) -> { a.addAll(b); return a; });
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, NonNullList<FluidStack>> rollFluidOutput() {
        Map<String, NonNullList<FluidStack>> result = new java.util.LinkedHashMap<>();
        for (RecipeOutput<?> output : outputs) {
            if (output.type() == ModIOTypes.FLUID.get()) {
                NonNullList<FluidStack> entries = (NonNullList<FluidStack>) output.specification();
                NonNullList<FluidStack> rolled = NonNullList.create();
                for (FluidStack fluid : entries) {
                    if (!fluid.isEmpty()) rolled.add(fluid.copy());
                }
                result.put(output.zoneName(), rolled);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public boolean matches(NonNullList<ItemStack> allItems, SlotPartition partition, Level level) {
        for (RecipeInput<?> input : inputs) {
            if (input.type() == ModIOTypes.ITEM.get()) {
                SlotZone zone = partition.getZoneByName(input.zoneName());
                if (zone == null) return false;
                NonNullList<Ingredient> ingredients = (NonNullList<Ingredient>) input.requirement();
                for (Ingredient ingredient : ingredients) {
                    boolean found = false;
                    for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
                        if (ingredient.test(allItems.get(slot))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean matchesFluid(NonNullList<FluidStack> allTanks, SlotPartition partition) {
        for (RecipeInput<?> input : inputs) {
            if (input.type() == ModIOTypes.FLUID.get()) {
                SlotZone zone = partition.getZoneByName(input.zoneName());
                if (zone == null) return false;
                NonNullList<FluidIngredient> ingredients = (NonNullList<FluidIngredient>) input.requirement();
                for (FluidIngredient ingredient : ingredients) {
                    boolean found = false;
                    for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
                        if (ingredient.test(allTanks.get(tank))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(ZhenRecipeInput input, Level level) {
        boolean hasItemInput = false;
        for (RecipeInput<?> recipeInput : inputs) {
            if (recipeInput.type() != ModIOTypes.ITEM.get()) continue;
            hasItemInput = true;
            NonNullList<Ingredient> ingredients = (NonNullList<Ingredient>) recipeInput.requirement();
            for (Ingredient ingredient : ingredients) {
                boolean found = false;
                for (int i = 0; i < input.size(); i++) {
                    if (ingredient.test(input.getItem(i))) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }
        return hasItemInput || inputs.isEmpty();
    }

    public ItemStack assemble(ZhenRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public RecipeSerializer<? extends Recipe<ZhenRecipeInput>> getSerializer() {
        return ZhenRecipeSerializer.INSTANCE;
    }

    public RecipeType<? extends Recipe<ZhenRecipeInput>> getType() {
        return ModRecipeManager.ZHEN_RECIPE.get();
    }

    public String getTypeStr() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> flat = NonNullList.create();
        for (RecipeInput<?> input : inputs) {
            if (input.type() == ModIOTypes.ITEM.get()) {
                flat.addAll((NonNullList<Ingredient>) input.requirement());
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
                    if (entry.stack() != null) {
                        flat.add(entry.stack());
                    }
                }
            }
        }
        return flat;
    }

    @Override
    public ItemStack assemble(ZhenRecipeInput input) {
        throw new UnsupportedOperationException("Unimplemented method 'assemble'");
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

    public static record FluidIngredient(HolderSet<Fluid> fluids, int amount) {
        public boolean test(FluidStack stack) {
            if (stack.isEmpty() || stack.getAmount() < amount) return false;
            return fluids.contains(stack.typeHolder());
        }

        public FluidStack toFluidStack() {
            Holder<Fluid> first = fluids.iterator().next();
            return new FluidStack(first, amount);
        }
    }
}

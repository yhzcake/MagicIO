package com.yhzcake.magicio.item.crafting;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
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
    private final Map<String, NonNullList<Ingredient>> zoneInputs;
    private final Map<String, NonNullList<OutputEntry>> zoneOutputs;
    private final Map<String, NonNullList<FluidIngredient>> fluidInputs;
    private final Map<String, NonNullList<FluidStack>> fluidOutputs;
    @Nullable
    private final Identifier lootTableId;
    private final int processingTime;

    public ZhenRecipe(String type, Map<String, NonNullList<Ingredient>> zoneInputs, Map<String, NonNullList<OutputEntry>> zoneOutputs, @Nullable Identifier lootTableId, int processingTime) {
        this(type, zoneInputs, zoneOutputs, Map.of(), Map.of(), lootTableId, processingTime);
    }

    public ZhenRecipe(String type, Map<String, NonNullList<Ingredient>> zoneInputs, Map<String, NonNullList<OutputEntry>> zoneOutputs, Map<String, NonNullList<FluidIngredient>> fluidInputs, Map<String, NonNullList<FluidStack>> fluidOutputs, @Nullable Identifier lootTableId, int processingTime) {
        this.type = type;
        this.zoneInputs = zoneInputs;
        this.zoneOutputs = zoneOutputs;
        this.fluidInputs = fluidInputs;
        this.fluidOutputs = fluidOutputs;
        this.lootTableId = lootTableId;
        this.processingTime = processingTime;
    }

    public String getRecipeType() {
        return type;
    }

    public Map<String, NonNullList<Ingredient>> getZoneInputs() {
        return zoneInputs;
    }

    public Map<String, NonNullList<OutputEntry>> getZoneOutputs() {
        return zoneOutputs;
    }

    public Map<String, NonNullList<FluidIngredient>> getFluidZoneInputs() {
        return fluidInputs;
    }

    public Map<String, NonNullList<FluidStack>> getFluidZoneOutputs() {
        return fluidOutputs;
    }

    @Nullable
    public Identifier getLootTableId() {
        return lootTableId;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public Map<String, NonNullList<ItemStack>> rollOutput(ServerLevel level) {
        Map<String, NonNullList<ItemStack>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, NonNullList<OutputEntry>> entry : zoneOutputs.entrySet()) {
            NonNullList<ItemStack> rolled = NonNullList.create();
            for (OutputEntry output : entry.getValue()) {
                rolled.addAll(output.roll(level));
            }
            result.put(entry.getKey(), rolled);
        }
        if (lootTableId != null) {
            java.util.List<ItemStack> lootItems = ZhenRecipeLoader.getItemsFromLootTable(level.getServer(), level, lootTableId);
            if (!result.isEmpty()) {
                String firstZone = result.keySet().iterator().next();
                result.get(firstZone).addAll(lootItems);
            } else {
                NonNullList<ItemStack> lootList = NonNullList.create();
                lootList.addAll(lootItems);
                result.put(SlotZone.ITEM_OUTPUT_ALL.getName(), lootList);
            }
        }
        return result;
    }

    public Map<String, NonNullList<FluidStack>> rollFluidOutput() {
        Map<String, NonNullList<FluidStack>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, NonNullList<FluidStack>> entry : fluidOutputs.entrySet()) {
            NonNullList<FluidStack> rolled = NonNullList.create();
            for (FluidStack fluid : entry.getValue()) {
                if (!fluid.isEmpty()) {
                    rolled.add(fluid.copy());
                }
            }
            result.put(entry.getKey(), rolled);
        }
        return result;
    }

    public boolean matches(NonNullList<ItemStack> allItems, SlotPartition partition, Level level) {
        for (Map.Entry<String, NonNullList<Ingredient>> entry : zoneInputs.entrySet()) {
            SlotZone zone = partition.getZoneByName(entry.getKey());
            if (zone == null) return false;
            for (Ingredient ingredient : entry.getValue()) {
                boolean found = false;
                for (int slot : partition.getSlots(zone)) {
                    if (ingredient.test(allItems.get(slot))) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }
        return true;
    }

    public boolean matchesFluid(NonNullList<FluidStack> allTanks, SlotPartition partition) {
        for (Map.Entry<String, NonNullList<FluidIngredient>> entry : fluidInputs.entrySet()) {
            SlotZone zone = partition.getTankZoneByName(entry.getKey());
            if (zone == null) return false;
            for (FluidIngredient ingredient : entry.getValue()) {
                boolean found = false;
                for (int tank : partition.getTanks(zone)) {
                    if (ingredient.test(allTanks.get(tank))) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(ZhenRecipeInput input, Level level) {
        return false;
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

    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> flat = NonNullList.create();
        for (NonNullList<Ingredient> list : zoneInputs.values()) {
            flat.addAll(list);
        }
        return flat;
    }

    public NonNullList<ItemStack> getFixedOutputs() {
        NonNullList<ItemStack> flat = NonNullList.create();
        for (NonNullList<OutputEntry> list : zoneOutputs.values()) {
            for (OutputEntry entry : list) {
                if (entry.stack() != null) {
                    flat.add(entry.stack());
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

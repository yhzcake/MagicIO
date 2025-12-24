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
import org.jetbrains.annotations.NotNull;

public abstract class AbstractZhenBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {

    public static final int PROCESS_COOL_SPEED = 2;
    private final RecipeType<? extends ZhenRecipe> recipeType;
    protected NonNullList<ItemStack> items;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> quickCheck;
    int processTime;
    int processTimeTotal;

    @SuppressWarnings("unchecked")
    protected AbstractZhenBlockEntity(BlockPos pos, BlockState blockState, RecipeType<? extends ZhenRecipe> recipeType) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), pos, blockState);
        this.quickCheck = RecipeManager.createCheck((RecipeType<ZhenRecipe>) recipeType);
        this.recipeType = recipeType;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
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
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ProcessTime", this.processTime);
        tag.putInt("ProcessTimeTotal", this.processTimeTotal);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        CompoundTag compoundTag = new CompoundTag();
        this.recipesUsed.forEach((resourceLocation, integer) -> compoundTag.putInt(resourceLocation.toString(), integer));
        tag.put("RecipesUsed", compoundTag);
    }



    public RecipeManager.CachedCheck<ZhenRecipeInput, ? extends ZhenRecipe> getQuickCheck() {
        return quickCheck;
    }

    public RecipeType<? extends ZhenRecipe> getRecipeType() {
        return recipeType;
    }
}

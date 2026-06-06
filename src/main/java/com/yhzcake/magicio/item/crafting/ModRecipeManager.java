package com.yhzcake.magicio.item.crafting;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeManager {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MagicIO.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ZhenRecipe>> ZHEN_RECIPE =
            RECIPE_TYPES.register("zhen_block", () -> ZhenRecipeType.INSTANCE);
    
    private final RecipeManager.CachedCheck<ZhenRecipeInput, ZhenRecipe> quickCheck =
            RecipeManager.createCheck(ZHEN_RECIPE.get());
            
    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
    
    public RecipeManager.CachedCheck<ZhenRecipeInput, ZhenRecipe> getQuickCheck() {
        return quickCheck;
    }
}
package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

public class MIORecipeProvider extends RecipeProvider {
    protected MIORecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {

    }

    public static class Runner extends RecipeProvider.Runner {

        protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new MIORecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Recipe Gen For:"+ MagicIO.MOD_ID;
        }
    }
}

``` java
public class MyRecipeProvider extends RecipeProvider {
    public MyRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // Register your recipes here.
    }

    // The data provider class
    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new MyRecipeProvider(registries, output);
        }
    }
}

// In some event handler class
@SubscribeEvent // on the mod event bus
public static void gatherData(GatherDataEvent.Client event) {
    // Data providers should start by calling event.createDatapackRegistryObjects(...)
    // to register their datapack registry objects. This allows other providers
    // to use these objects during their own data generation.

    // From there, providers can generally be registered using event.createProvider(...),
    // which acts as a function that provides the PackOutput and optionally the
    // CompletableFuture<HolderLookup.Provider>.

    // Register the provider.
    event.createProvider(MyRecipeProvider.Runner::new);
    // Other data providers here.

    // If you want to create a datapack within the global pack, you can call
    // DataGenerator#getBuiltinDatapack. From there, you must use the
    // PackGenerator#addProvider method to add any providers to that pack.
    DataGenerator.PackGenerator examplePack = event.getGenerator().getBuiltinDatapack(
        true, // Should always be true.
        "examplemod", // The mod id.
        "example_pack" // The name of the pack.
    );
    
    examplePack.addProvider(output -> ...);
}
```
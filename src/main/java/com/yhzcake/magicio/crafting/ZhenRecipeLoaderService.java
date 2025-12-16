package com.yhzcake.magicio.crafting;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;

public class ZhenRecipeLoaderService extends SimplePreparableReloadListener<Void> {
    private static final Logger LOGGER = MagicIO.LOGGER;
    
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }
    
    @Override
    protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOGGER.info("开始加载Zhen配方...");
        
        // 清除现有的配方
        ZhenRecipeManager.getInstance().clearRecipes();
        
        // 加载所有recipes目录下的配方（你希望配方在recipes目录中）
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
            "recipes", 
            (path) -> path.getPath().endsWith(".json") && path.getPath().contains("sift")
        );
        
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            Resource resource = entry.getValue();
            
            try {
                LOGGER.info("正在加载配方: {}", resourceLocation);
                InputStream inputStream = resource.open();
                ZhenRecipe recipe = ZhenRecipeLoader.loadRecipeFromJson(inputStream);
                if (recipe != null) {
                    ZhenRecipeManager.getInstance().addRecipe(recipe);
                    LOGGER.info("成功加载配方: {}", recipe.getType());
                } else {
                    LOGGER.warn("无法加载配方: {}", resourceLocation);
                }
                inputStream.close();
            } catch (Exception e) {
                LOGGER.error("加载配方时出错: {}", resourceLocation, e);
            }
        }
        
        LOGGER.info("配方加载完成，共加载 {} 个配方", ZhenRecipeManager.getInstance().getRecipes().size());
    }
}
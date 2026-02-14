package com.yhzcake.magicio;

import com.yhzcake.magicio.block.ModBlocks;
import com.yhzcake.magicio.block.entity.ModBlockEntities;
import com.yhzcake.magicio.block.zhen.ZhenType;
import com.yhzcake.magicio.block.zhen.ZhenTypes;
import com.yhzcake.magicio.item.crafting.ZhenRecipeLoaderService;
import com.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import com.yhzcake.magicio.item.ModCreativeModeTabs;
import com.yhzcake.magicio.item.ModItems;
import com.yhzcake.magicio.item.crafting.ModRecipeManager;
import com.yhzcake.magicio.utils.ModDataComponents;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@SuppressWarnings("null")
// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MagicIO.MOD_ID)
public class MagicIO {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "magicio";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 配方加载服务
    private final ZhenRecipeLoaderService recipeLoaderService = new ZhenRecipeLoaderService();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MagicIO(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for mod-loading
        modEventBus.addListener(this::commonSetup);
        
        modEventBus.addListener(this::registerZhenType);
        
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ZhenTypes.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModRecipeManager.register(modEventBus);
        
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (MagicIO) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
//        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
        
        // 初始化配方管理器
        ZhenRecipeManager.getInstance();
    }

    private void registerZhenType(NewRegistryEvent event) {
        ZhenType.register(event);
    }

    // Add the example block item to the building blocks tab
//    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//
//    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
    
    // 添加这一方法，使用SubscribeEvent注解在NeoForge总线上注�?
    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(recipeLoaderService);
    }
}

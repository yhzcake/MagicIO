package com.yhzcake.magicio;

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.yhzcake.magicio.block.ModBlocks;
import com.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import com.yhzcake.magicio.block.entity.ModBlockEntities;
import com.yhzcake.magicio.block.zhen.ZhenType;
import com.yhzcake.magicio.block.zhen.ZhenTypes;
import com.yhzcake.magicio.config.Config;
import com.yhzcake.magicio.item.ModDataComponents;
import com.yhzcake.magicio.item.ModItems;
import com.yhzcake.magicio.item.crafting.ModRecipeManager;
import com.yhzcake.magicio.item.crafting.ZhenRecipe;
import com.yhzcake.magicio.item.crafting.ZhenRecipeLoader;
import com.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import com.yhzcake.magicio.utils.ElementType;
import com.yhzcake.magicio.utils.ElementTypes;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

@Mod(MagicIO.MOD_ID)
public class MagicIO {
    public static final String MOD_ID = "magic_io";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.magic_io"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                for (var blockItem : ModBlocks.ZHEN_BLOCK_ITEMS.values()) {
                    output.accept(blockItem.get());
                }
            }).build());

    public MagicIO(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModDataComponents.register(modEventBus);
        modEventBus.register(ElementType.class);
        ElementTypes.register(modEventBus);
        modEventBus.register(ZhenType.class);
        ZhenTypes.register(modEventBus);
        ModBlocks.registerZhenBlocks(BLOCKS);
        ModBlocks.registerZhenBlockItems(ITEMS);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeManager.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.ZHEN_BLOCK.get(),
                (be, direction) -> {
                    Integer capacity = ((AbstractZhenBlockEntity) be).getTankCapacity();
                    if (capacity == null) return null;
                    return new FluidStacksResourceHandler(((AbstractZhenBlockEntity) be).getTanks(), capacity);
                }
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");

        var server = event.getServer();
        var resourceManager = server.getResourceManager();

        ZhenRecipeManager.getInstance().clearRecipes();

        Map<Identifier, Resource> resources = resourceManager.listResources(
            "recipe",
            (path) -> path.getPath().endsWith(".json") && path.getPath().contains("sift")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceLocation = entry.getKey();
            Resource resource = entry.getValue();

            try {
                LOGGER.info("正在加载配方: {}", resourceLocation);
                InputStream inputStream = resource.open();
                ZhenRecipe recipe = ZhenRecipeLoader.loadRecipeFromJson(inputStream, server);
                if (recipe != null) {
                    ZhenRecipeManager.getInstance().addRecipe(recipe);
                    LOGGER.info("成功加载配方: {}", recipe.getRecipeType());
                } else {
                    LOGGER.warn("无法加载配方: {}", resourceLocation);
                }
                inputStream.close();
            } catch (Exception e) {
                LOGGER.error("加载配方时出错 {}", resourceLocation, e);
            }
        }

        LOGGER.info("配方加载完成，共加载 {} 个配方", ZhenRecipeManager.getInstance().getRecipes().size());
    }
}

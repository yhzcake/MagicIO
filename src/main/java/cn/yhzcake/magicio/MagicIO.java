package cn.yhzcake.magicio;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import cn.yhzcake.magicio.block.ModBlocks;
import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import cn.yhzcake.magicio.block.entity.ModBlockEntities;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import cn.yhzcake.magicio.block.zhenbus.ModZhenBusBlocks;
import cn.yhzcake.magicio.config.Config;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import cn.yhzcake.magicio.io.EnergyIOComponent;
import cn.yhzcake.magicio.io.FluidIOComponent;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ItemIOComponent;
import cn.yhzcake.magicio.io.LinkedItemHandler;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import cn.yhzcake.magicio.item.ModDataComponents;
import cn.yhzcake.magicio.item.ModItems;
import cn.yhzcake.magicio.item.crafting.ModRecipeManager;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeLoader;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import cn.yhzcake.magicio.utils.ElementType;
import cn.yhzcake.magicio.utils.ElementTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.core.Direction;
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
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.BlockCapability;
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

    public static final DeferredItem<BlockItem> ZHEN_BUS_ITEM = ITEMS.registerSimpleBlockItem("zhen_bus", ModZhenBusBlocks.ZHEN_BUS);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.magic_io"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
                for (var blockItem : ModBlocks.ZHEN_BLOCK_ITEMS.values()) {
                    output.accept(blockItem.get());
                }
                output.accept(ZHEN_BUS_ITEM.get());
            }).build());

    public MagicIO(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModDataComponents.register(modEventBus);
        modEventBus.register(ElementType.class);
        ElementTypes.register(modEventBus);
        modEventBus.register(ZhenType.class);
        ZhenTypes.register(modEventBus);
        modEventBus.register(IOType.class);
        ModIOTypes.register(modEventBus);
        ModBlocks.registerZhenBlocks(BLOCKS);
        ModBlocks.registerZhenBlockItems(ITEMS);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModZhenBusBlocks.BLOCKS.register(modEventBus);
        ModZhenBusBlocks.BLOCK_ENTITIES.register(modEventBus);
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
                    Integer capacity = ((AbstractZhenBlockEntity) be).getFluidTankCapacity();
                    if (capacity == null) return null;
                    return new FluidStacksResourceHandler(((AbstractZhenBlockEntity) be).getFluidTanks(), capacity);
                }
        );
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.ZHEN_BLOCK.get(),
                (be, direction) -> {
                    EnergyIOComponent energyComponent = ((AbstractZhenBlockEntity) be).getEnergyIOComponent();
                    if (energyComponent == null) return null;
                    return energyComponent.getHandler();
                }
        );

        // ZhenBus ITEM（内联注册以接入变更通知链）
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                ModZhenBusBlocks.ZHEN_BUS_BE.get(),
                (be, direction) -> {
                    if (direction == null) return null;
                    SideProcessor processor = be.getProcessor(direction);
                    if (processor == null) return null;
                    Map<IOType, Set<Integer>> access = processor.getFaceAccess(direction);
                    if (access == null) return null;
                    Set<Integer> slots = access.get(ModIOTypes.ITEM.get());
                    if (slots == null || slots.isEmpty()) return null;
                    Object raw = processor.getIOProcessor().get(ModIOTypes.ITEM.get());
                    if (!(raw instanceof ItemIOComponent itemIO)) return null;
                    // 管道只能向输入槽插入、从输出槽提取
                    AbstractSideProcessor asp = (AbstractSideProcessor) processor;
                    LinkedItemHandler handler = new LinkedItemHandler(itemIO.getItems(),
                            asp.getInputItemSlots(), asp.getOutputItemSlots());
                    handler.setOnChange(() -> {
                        itemIO.notifyChanged();
                        be.setChanged();
                    });
                    return handler;
                }
        );
        registerZhenBusCap(event, Capabilities.Fluid.BLOCK, ModIOTypes.FLUID.get(), (comp) -> {
            if (!(comp instanceof FluidIOComponent fluidComp)) return null;
            return new FluidStacksResourceHandler(fluidComp.getTanks(),
                    fluidComp.getTankCapacity() != null ? fluidComp.getTankCapacity() : 0);
        });
        registerZhenBusCap(event, Capabilities.Energy.BLOCK, ModIOTypes.ENERGY.get(), (comp) -> {
            if (!(comp instanceof EnergyIOComponent energyComp)) return null;
            return energyComp.getHandler();
        });
    }

    private static <T, C> void registerZhenBusCap(RegisterCapabilitiesEvent event,
            BlockCapability<T, @Nullable Direction> cap, IOType ioType,
            java.util.function.Function<Object, T> handlerFactory) {
        event.registerBlockEntity(
                (BlockCapability<T, @Nullable Direction>) cap,
                ModZhenBusBlocks.ZHEN_BUS_BE.get(),
                (be, direction) -> {
                    if (direction == null) return null;
                    SideProcessor processor = be.getProcessor(direction);
                    if (processor == null) return null;
                    if (ioType != ModIOTypes.ENERGY.get()) {
                        Map<IOType, Set<Integer>> access = processor.getFaceAccess(direction);
                        if (access == null) return null;
                        Set<Integer> slots = access.get(ioType);
                        if (slots == null || slots.isEmpty()) return null;
                    }
                    Object component = processor.getIOProcessor().get(ioType);
                    if (component == null) return null;
                    return handlerFactory.apply(component);
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
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipes"), new ZhenRecipeReloadListener());
    }

    private static class ZhenRecipeReloadListener extends SimplePreparableReloadListener<Void> {
        @Override
        protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            return null;
        }

        @Override
        protected void apply(Void data, ResourceManager resourceManager, ProfilerFiller profiler) {
            // 客户端reload时（F3+T）注册表尚未就绪，跳过加载，服务端onServerStarting会加载
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                LOGGER.info("跳过客户端配方加载，等待服务端启动时加载");
                return;
            }
            loadAllRecipes(resourceManager, null);
        }
    }

    private static void loadAllRecipes(ResourceManager resourceManager, @Nullable MinecraftServer server) {
        ZhenRecipeManager.getInstance().clearRecipes();

        Map<Identifier, Resource> resources = resourceManager.listResources(
            "recipe",
            (path) -> path.getPath().endsWith(".json") && path.getNamespace().equals(MagicIO.MOD_ID)
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
                    LOGGER.info("成功加载配方: {}", recipe.getZhenTypeStr());
                } else {
                    LOGGER.warn("无法加载配方: {}", resourceLocation);
                }
                inputStream.close();
            } catch (Exception e) {
                LOGGER.error("加载配方时出错 {}", resourceLocation, e);
            }
        }

        LOGGER.info("配方加载完成，共加载 {} 个配方", ZhenRecipeManager.getInstance().getRecipeCount());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
        loadAllRecipes(event.getServer().getResourceManager(), event.getServer());
    }
}

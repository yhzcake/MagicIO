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
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import cn.yhzcake.magicio.block.zhenbus.ModZhenBusBlocks;
import cn.yhzcake.magicio.block.gridcell.CellAction;
import cn.yhzcake.magicio.config.Config;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import cn.yhzcake.magicio.io.EnergyIOComponent;
import cn.yhzcake.magicio.io.FluidIOComponent;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ItemIOComponent;
import cn.yhzcake.magicio.io.LinkedFluidHandler;
import cn.yhzcake.magicio.io.LinkedItemHandler;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import cn.yhzcake.magicio.item.ModDataComponents;
import cn.yhzcake.magicio.item.crafting.ModRecipeManager;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeLoader;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import cn.yhzcake.magicio.utils.ElementType;
import cn.yhzcake.magicio.utils.ElementTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MagicIO 主模组类。
 */
@Mod(MagicIO.MOD_ID)
public class MagicIO {
    public static final String MOD_ID = "magic_io";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredItem<Item> COAL_COKE = ITEMS.registerSimpleItem("coal_coke");
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item");

    public static final DeferredItem<BlockItem> ZHEN_BUS_ITEM = ITEMS.registerSimpleBlockItem("zhen_bus", ModZhenBusBlocks.ZHEN_BUS);
    public static final DeferredItem<BlockItem> GRID_CELL_PANEL_ITEM = ITEMS.registerSimpleBlockItem("grid_cell_panel", ModBlocks.GRID_CELL_PANEL);

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
                output.accept(GRID_CELL_PANEL_ITEM.get());
            }).build());

    public MagicIO(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        modEventBus.register(ElementType.class);
        ElementTypes.register(modEventBus);
        modEventBus.register(ZhenType.class);
        modEventBus.register(IOType.class);
        ModIOTypes.register(modEventBus);

        ZhenTypes.register(modEventBus);
        ModRecipeManager.register(modEventBus);

        ModBlocks.registerZhenBlocks(MagicIO.BLOCKS);
        ModBlocks.registerZhenBlockItems(MagicIO.ITEMS);

        MagicIO.BLOCKS.register(modEventBus);
        MagicIO.ITEMS.register(modEventBus);

        ModZhenBusBlocks.BLOCKS.register(modEventBus);
        ModZhenBusBlocks.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.register(CellAction.class);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerCapabilities);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // ===== example_item 右键空气循环 side，右键 zhen_bus 安装处理器 =====

    @SubscribeEvent
    public void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (!stack.is(EXAMPLE_ITEM.get())) return;

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String current = tag.getString("side").orElse("");
        Direction currentDir = Direction.byName(current);

        Direction[] cycle = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        Direction next = Direction.UP;
        if (currentDir != null) {
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == currentDir) {
                    next = cycle[(i + 1) % cycle.length];
                    break;
                }
            }
        }

        CompoundTag newTag = new CompoundTag();
        newTag.putString("side", next.getName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));

        event.getEntity().sendSystemMessage(
                Component.literal("§e[Example] Side: §f" + next.getName().toUpperCase()));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public void onItemRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (!stack.is(EXAMPLE_ITEM.get())) return;

        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof ZhenBusBlockEntity be)) return;

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String sideName = tag.getString("side").orElse("");
        if (sideName.isEmpty()) {
            event.getEntity().sendSystemMessage(
                    Component.literal("§c[Example] No side set! Right-click air to cycle."));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        Direction dir = Direction.byName(sideName);
        if (dir == null) {
            event.getEntity().sendSystemMessage(
                    Component.literal("§c[Example] Invalid side: " + sideName));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        var zhenType = ZhenTypes.getType(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "unstable_sieve"));
        if (zhenType == null) {
            event.getEntity().sendSystemMessage(
                    Component.literal("§c[Example] Unstable sieve type not found!"));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        be.addProcessor(zhenType, dir, event.getEntity());
        be.markForUpdate();
        event.getLevel().invalidateCapabilities(event.getPos());

        event.getEntity().sendSystemMessage(
                Component.literal("§a[Example] Installed sieve on " + dir.getName().toUpperCase()));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ===== 原有方法 =====

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
             ModBlockEntities.ZHEN_BLOCK.get(), 
            (be, direction) -> {
                if (direction == null) return null;
                AbstractZhenBlockEntity azbe = (AbstractZhenBlockEntity) be;
                Set<Integer> insertSlots = azbe.getInsertSlots(direction);
                Set<Integer> extractSlots = azbe.getExtractSlots(direction);
                if (insertSlots.isEmpty() && extractSlots.isEmpty()) return null;
                ItemIOComponent itemIO = azbe.getItemIOComponent();
                LinkedItemHandler handler = new LinkedItemHandler(
                        itemIO.getItems(), insertSlots, extractSlots);
                handler.setOnChange(() -> {
                    itemIO.notifyChanged();
                    azbe.setChanged();
                });
                return handler;
            }
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.ZHEN_BLOCK.get(),
                (be, direction) -> {
                    if (direction == null) return null;
                    AbstractZhenBlockEntity azbe = (AbstractZhenBlockEntity) be;
                    Integer capacity = azbe.getFluidTankCapacity();
                    if (capacity == null) return null;
                    Set<Integer> insertSlots = azbe.getInsertFluidSlots(direction);
                    Set<Integer> extractSlots = azbe.getExtractFluidSlots(direction);
                    if (insertSlots.isEmpty() && extractSlots.isEmpty()) return null;
                    FluidIOComponent fluidIO = azbe.getFluidIOComponent();
                    LinkedFluidHandler handler = new LinkedFluidHandler(
                            fluidIO.getTanks(), capacity, insertSlots, extractSlots);
                    handler.setOnChange(() -> {
                        fluidIO.notifyChanged();
                        azbe.setChanged();
                    });
                    return handler;
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
                    AbstractSideProcessor asp = (AbstractSideProcessor) processor;
                    Set<Integer> insertSlots = new java.util.HashSet<>(slots);
                    insertSlots.retainAll(asp.getInputItemSlots());
                    Set<Integer> extractSlots = new java.util.HashSet<>(slots);
                    extractSlots.retainAll(asp.getOutputItemSlots());
                    if (insertSlots.isEmpty() && extractSlots.isEmpty()) return null;
                    LinkedItemHandler handler = new LinkedItemHandler(itemIO.getItems(),
                            insertSlots, extractSlots);
                    handler.setOnChange(() -> {
                        itemIO.notifyChanged();
                        be.setChanged();
                    });
                    return handler;
                }
        );
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModZhenBusBlocks.ZHEN_BUS_BE.get(),
                (be, direction) -> {
                    if (direction == null) return null;
                    SideProcessor processor = be.getProcessor(direction);
                    if (processor == null) return null;
                    Map<IOType, Set<Integer>> access = processor.getFaceAccess(direction);
                    if (access == null) return null;
                    Set<Integer> slots = access.get(ModIOTypes.FLUID.get());
                    if (slots == null || slots.isEmpty()) return null;
                    Object raw = processor.getIOProcessor().get(ModIOTypes.FLUID.get());
                    if (!(raw instanceof FluidIOComponent fluidIO)) return null;
                    AbstractSideProcessor asp = (AbstractSideProcessor) processor;
                    Set<Integer> insertSlots = new java.util.HashSet<>(slots);
                    insertSlots.retainAll(asp.getFluidInputSlots());
                    Set<Integer> extractSlots = new java.util.HashSet<>(slots);
                    extractSlots.retainAll(asp.getFluidOutputSlots());
                    if (insertSlots.isEmpty() && extractSlots.isEmpty()) return null;
                    LinkedFluidHandler handler = new LinkedFluidHandler(
                            fluidIO.getTanks(),
                            fluidIO.getTankCapacity() != null ? fluidIO.getTankCapacity() : 0,
                            insertSlots, extractSlots);
                    handler.setOnChange(() -> {
                        fluidIO.notifyChanged();
                        be.setChanged();
                    });
                    return handler;
                }
        );
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
            event.accept(EXAMPLE_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        var server = event.getServer();
        loadRecipesToManager(server.getResourceManager(), server);
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
            if (FMLEnvironment.getDist() == Dist.CLIENT) return;
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null || !server.isRunning()) return;
            loadRecipesToManager(resourceManager, server);
        }
    }

    private static void loadRecipesToManager(ResourceManager resourceManager, MinecraftServer server) {
        ZhenRecipeManager.getInstance().clearRecipes();
        Map<Identifier, Resource> resources = resourceManager.listResources(
            "recipe",
            (path) -> path.getPath().endsWith(".json") && path.getNamespace().equals(MagicIO.MOD_ID)
        );
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (InputStream inputStream = entry.getValue().open()) {
                ZhenRecipe recipe = ZhenRecipeLoader.loadRecipeFromJson(inputStream, server);
                if (recipe != null) {
                    ZhenRecipeManager.getInstance().addRecipe(recipe);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading recipe {}: {}", entry.getKey(), e.getMessage());
            }
        }
        LOGGER.info("Loaded {} recipes into ZhenRecipeManager", ZhenRecipeManager.getInstance().getRecipeCount());
    }
}

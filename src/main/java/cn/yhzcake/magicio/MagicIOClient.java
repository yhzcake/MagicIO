package cn.yhzcake.magicio;

import cn.yhzcake.magicio.block.zhenbus.ModZhenBusBlocks;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntityRenderer;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = MagicIO.MOD_ID, value = Dist.CLIENT)
public class MagicIOClient {

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModZhenBusBlocks.ZHEN_BUS_BE.get(), ZhenBusBlockEntityRenderer::new);
    }

    @SubscribeEvent
    static void onAgeratumGuidebookUse(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        if (MagicIO.AGERATUM_GUIDEBOOK == null) return;
        if (!event.getItemStack().is(MagicIO.AGERATUM_GUIDEBOOK.get())) return;
        if (!ClientCommandHandler.runCommand("ageratum \"magic_io\" \"index\"")) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        cn.yhzcake.magicio.item.crafting.ZhenRecipeManager.getClientInstance().clearClientCache();
        cn.yhzcake.magicio.network.ZhenRecipeNetworkSync.resetClient();
        if (!ModList.get().isLoaded("jei")) return;
        try {
            Class<?> pluginClass = Class.forName("cn.yhzcake.magicio.compat.jei.MagicIOJeiPlugin");
            pluginClass.getMethod("clearRecipes").invoke(null);
        } catch (ReflectiveOperationException e) {
            MagicIO.LOGGER.debug("JEI runtime cleanup skipped: {}", e.getMessage());
        }
    }

}

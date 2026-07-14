package cn.yhzcake.magicio;

import cn.yhzcake.magicio.block.zhenbus.ModZhenBusBlocks;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MagicIO.MOD_ID, value = Dist.CLIENT)
public class MagicIOClient {

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModZhenBusBlocks.ZHEN_BUS_BE.get(), ZhenBusBlockEntityRenderer::new);
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

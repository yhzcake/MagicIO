package cn.yhzcake.magicio;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = MagicIO.MOD_ID, value = Dist.CLIENT)
public class MagicIOClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.getContainer().registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        MagicIO.LOGGER.info("HELLO FROM CLIENT SETUP");
        MagicIO.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // 配方加载完全由服务端管理，通过 ZhenRecipeSyncPayload 网络同步
        // 客户端不预加载配方，避免注册表未就绪时的坏数据污染缓存
        // JEI 通过 onRuntimeAvailable/refreshRecipes 动态注入
    }
}

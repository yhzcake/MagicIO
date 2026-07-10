package cn.yhzcake.magicio.datagen;

import cn.yhzcake.magicio.MagicIO;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = MagicIO.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ModDataGen {

    @SubscribeEvent
    static void onGatherData(GatherDataEvent.Client event) {
        event.addProvider(new ModModelProvider(event.getGenerator().getPackOutput()));
    }
}

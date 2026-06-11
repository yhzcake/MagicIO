package cn.yhzcake.magicio.common.data.gen;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.data.gen.sub.MIOBlockModelSubProvider;
import cn.yhzcake.magicio.common.data.gen.sub.MIOItemModelSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 功能: 所有数据生成器的中心注册器，虽属事件部分但仍乐意将其放于此处。
 */
@EventBusSubscriber(modid = MagicIO.MOD_ID)
public class MIODataGenerators {
    @SubscribeEvent
    public static void gatherClientData(GatherDataEvent.Client event) {
        event.createProvider(MIOModelProvider::new);
        event.createProvider(MIOBlockModelSubProvider::new);
        event.createProvider(MIOItemModelSubProvider::new);
        event.createProvider(MIOBlockTagsProvider::new);
        event.createProvider(MIOItemTagsProvider::new);
        event.createProvider((MIOLootTableProvider::new));
        event.createProvider(MIOChineseProvider::new);
        event.createProvider(MIOEnglishProvider::new);
        event.createProvider(MIORecipeProvider.Runner::new);
    }
    /*@SubscribeEvent
    public void gatherServerData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
    }*/
}

package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlock;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MagicIOJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                ZhenBusServerProvider.INSTANCE, ZhenBusBlockEntity.class);
        registration.registerItemStorage(
                ZhenBusItemProvider.INSTANCE, ZhenBusBlockEntity.class);
        registration.registerFluidStorage(
                ZhenBusFluidProvider.INSTANCE, ZhenBusBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                ZhenBusClientProvider.INSTANCE, ZhenBusBlock.class);
        registration.registerItemStorageClient(
                ZhenBusItemClientProvider.INSTANCE);
        registration.registerFluidStorageClient(
                ZhenBusFluidClientProvider.INSTANCE);
    }
}

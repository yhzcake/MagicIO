package com.yhzcake.magicio.utils;
import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@SuppressWarnings("null")
@EventBusSubscriber(modid = MagicIO.MOD_ID)
public class ModCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ZHEN_BLOCK.get(),
                (blockEntity, context) -> blockEntity.getInventory()
        );
    }
}

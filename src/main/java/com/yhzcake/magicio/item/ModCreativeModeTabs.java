package com.yhzcake.magicio.item;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicIO.MOD_ID);

    public static final Supplier<CreativeModeTab> MAGICIO_TAB =
            CREATIVE_MODE_TABS.register("magicio_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.TEST_ITEM.get()))
                    .title(Component.translatable("itemGroup.magicio_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TEST_ITEM);
                        // output.accept(ModBlocks.TEST_BLOCK);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

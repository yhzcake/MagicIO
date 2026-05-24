package com.yhzcake.magicio.item;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MagicIO.MOD_ID);

    public static final Supplier<CreativeModeTab> MAGICIO_TAB =
            CREATIVE_MODE_TABS.register("magicio_tab", () -> CreativeModeTab.builder()
                    .icon(() -> {
                        var items = ModBlocks.ZHEN_BLOCK_ITEMS.values().stream()
                                .map(DeferredItem::get).toList();
                        if (items.isEmpty()) return ItemStack.EMPTY;
                        int idx = (int) ((System.currentTimeMillis() / 1000) % items.size());
                        return new ItemStack(items.get(idx));
                    })
                    .title(Component.translatable("itemGroup.magicio_tab"))
                    .displayItems((parameters, output) -> {
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

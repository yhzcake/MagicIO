package com.yhzcake.magicio.item;

import com.yhzcake.magicio.MagicIO;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MagicIO.MOD_ID);

    public static final DeferredItem<Item> TEST_ITEM =
            ITEMS.registerSimpleItem("test_item", p -> p.stacksTo(1));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

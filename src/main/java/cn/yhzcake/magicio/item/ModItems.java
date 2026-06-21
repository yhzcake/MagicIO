package cn.yhzcake.magicio.item;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MagicIO.MOD_ID);

    public static final DeferredItem<Item> COKE = ITEMS.registerSimpleItem("coal_coke");

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
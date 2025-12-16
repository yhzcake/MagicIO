package com.yhzcake.magicio.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class ModItemEvents {
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // 检查玩家物品栏中的物品
        for (int i = 0; i < event.getEntity().getInventory().getContainerSize(); i++) {
            ItemStack stack = event.getEntity().getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.TEST_ITEM.get())) {
                // 如果是test_item，将其设置为空
                event.getEntity().getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
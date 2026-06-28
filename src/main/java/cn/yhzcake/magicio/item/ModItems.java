package cn.yhzcake.magicio.item;

import cn.yhzcake.magicio.MagicIO;

/**
 * 物品引用入口。注册统一在 {@link MagicIO#ITEMS MagicIO.ITEMS} 中进行。
 * 此处只暴露 DeferredItem 引用。
 */
public class ModItems {
    // 全部物品注册在 MagicIO.ITEMS 中，如下写法仅为引用暴露
    // public static final DeferredItem<Item> EXAMPLE_ITEM = MagicIO.ITEMS.register...
}

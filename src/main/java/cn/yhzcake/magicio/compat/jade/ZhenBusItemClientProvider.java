package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * 客户端物品渲染器：将服务端下发的 ItemStack 转换为 Jade 原生物品图标网格。
 */
public enum ZhenBusItemClientProvider implements IClientExtensionProvider<ItemStack, ItemView> {

    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus_items");
    }

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(
            Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        return ClientViewGroup.map(groups, ItemView::new, (group, clientGroup) -> {});
    }
}

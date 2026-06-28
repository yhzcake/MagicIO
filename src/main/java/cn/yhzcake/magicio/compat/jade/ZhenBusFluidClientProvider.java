package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.resources.Identifier;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * 客户端流体渲染器：将服务端下发的 FluidView.Data 转换为 Jade 原生流体条渲染。
 */
public enum ZhenBusFluidClientProvider implements IClientExtensionProvider<FluidView.Data, FluidView> {

    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus_fluids");
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(
            Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, (group, clientGroup) -> {});
    }
}

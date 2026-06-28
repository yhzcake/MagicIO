package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端流体提供器：从 IOProcessor 自动读取 FLUID 数据。
 */
public enum ZhenBusFluidProvider implements IServerExtensionProvider<FluidView.Data> {

    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus_fluids");
    }

    @Override
    public List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return List.of();
        if (!(blockAccessor.getBlockEntity() instanceof ZhenBusBlockEntity be)) return List.of();

        Direction hitFace = ZhenBusClientProvider.pickFace(
                be, accessor.getPlayer(), blockAccessor.getPosition());
        if (hitFace == null) hitFace = Direction.DOWN;

        SideProcessor sp = be.getProcessor(hitFace);
        if (sp == null) return List.of();

        cn.yhzcake.magicio.io.IOComponent<?, ?> comp = sp.getIOProcessor().get(ModIOTypes.FLUID.get());
        if (!(comp instanceof cn.yhzcake.magicio.io.FluidIOComponent fluidIO)) return List.of();

        int capacity = fluidIO.getTankCapacity() != null ? fluidIO.getTankCapacity() : 1000;
        List<FluidView.Data> views = new ArrayList<>();
        for (var tank : fluidIO.getTanks()) {
            if (!tank.isEmpty()) {
                views.add(new FluidView.Data(
                        JadeFluidObject.of(tank.getFluid(), tank.getAmount()), capacity));
            }
        }
        return views.isEmpty() ? List.of() : List.of(new ViewGroup<>(views));
    }
}

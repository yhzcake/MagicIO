package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlock;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端物品提供器：从 IOProcessor 自动读取 ITEM 数据。
 */
public enum ZhenBusItemProvider implements IServerExtensionProvider<ItemStack> {

    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus_items");
    }

    @Override
    public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return List.of();
        if (!(blockAccessor.getBlockEntity() instanceof ZhenBusBlockEntity be)) return List.of();

        Direction hitFace = ZhenBusBlock.pickFace(
                be, accessor.getPlayer(), blockAccessor.getPosition());
        if (hitFace == null) hitFace = Direction.DOWN;

        SideProcessor sp = be.getProcessor(hitFace);
        if (sp == null) return List.of();

        cn.yhzcake.magicio.io.IOComponent<?, ?> comp = sp.getIOProcessor().get(ModIOTypes.ITEM.get());
        if (!(comp instanceof cn.yhzcake.magicio.io.ItemIOComponent itemIO)) return List.of();

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : itemIO.getItems()) {
            if (!stack.isEmpty()) items.add(stack.copy());
        }
        return items.isEmpty() ? List.of() : List.of(new ViewGroup<>(items));
    }
}

package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlock;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * 客户端渲染器：使用玩家视线做射线-薄片AABB命中检测，精确判断指向的面。
 * 替换方块名为阵名称。物品网格由 {@link ZhenBusItemProvider} 通过 Jade 原生存储系统渲染。
 */
public enum ZhenBusClientProvider implements IBlockComponentProvider {

    INSTANCE;

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ZhenBusBlockEntity be)) return;

        Direction hitFace = ZhenBusBlock.pickFace(be, accessor.getPlayer(), accessor.getPosition());
        if (hitFace == null) hitFace = accessor.getSide();

        SideProcessor sp = be.getProcessor(hitFace);
        if (sp == null) return;

        // 阵名称：替换默认方块名，应用 Jade 方块名样式（粗体白色）
        String typeStr = sp.getZhenType() != null ? sp.getZhenType().getType() : "";
        Component nameComponent = makeZhenName(typeStr);
        if (nameComponent != null) {
            tooltip.replace(JadeIds.CORE_OBJECT_NAME,nameComponent.copy().withStyle(s ->
                    s.withColor(0xFFFFFF)));
        }

        // 能量显示
        cn.yhzcake.magicio.io.IOComponent<?, ?> rawEnergy = sp.getIOProcessor().get(ModIOTypes.ENERGY.get());
        if (rawEnergy instanceof cn.yhzcake.magicio.io.EnergyIOComponent eic) {
            tooltip.add(Component.literal(
                    "§e⚡ " + eic.getEnergy() + " / " + eic.getCapacity() + " FE"));
        }
    }

    /** 从 zhenType 字符串构造翻译组件，如 "magic_io:unstable_dew_zhen" → translatable("magic_io.zhen_name", level, type) */
    private static Component makeZhenName(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) return null;
        String path = typeStr.contains(":") ? typeStr.substring(typeStr.indexOf(':') + 1) : typeStr;
        if (!path.endsWith("_zhen")) return Component.literal(path);
        String base = path.substring(0, path.length() - "_zhen".length());
        for (ZhenLevel level : ZhenLevel.ALL) {
            String prefix = level.prefix();
            if (base.startsWith(prefix)) {
                String typeName = base.substring(prefix.length());
                return Component.translatable("magic_io.zhen_name",
                        Component.translatable("magic_io.level." + prefix.substring(0, prefix.length() - 1)),
                        Component.translatable("magic_io.zhen_type." + typeName));
            }
        }
        return Component.literal(path);
    }
}

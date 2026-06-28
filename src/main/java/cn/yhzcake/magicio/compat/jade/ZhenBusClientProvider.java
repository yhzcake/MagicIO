package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenLevel;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    static final double T = 1.0 / 16;
    static final AABB[] FACE_BOUNDS = new AABB[] {
        new AABB(0, 0, 0, 1, T, 1),          // DOWN
        new AABB(0, 1 - T, 0, 1, 1, 1),      // UP
        new AABB(0, 0, 0, 1, 1, T),          // NORTH
        new AABB(0, 0, 1 - T, 1, 1, 1),      // SOUTH
        new AABB(0, 0, 0, T, 1, 1),          // WEST
        new AABB(1 - T, 0, 0, 1, 1, 1),      // EAST
    };
    static final Direction[] DIRS = Direction.values();

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus");
    }

    /** 根据玩家视线做射线-薄片命中检测，返回指向的面 */
    static Direction pickFace(ZhenBusBlockEntity be, Player player, BlockPos pos) {
        if (player == null) return null;
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 to = from.add(dir.scale(6));
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            if (be.getProcessor(DIRS[i]) == null) continue;
            AABB worldBox = FACE_BOUNDS[i].move(pos);
            Vec3 hit = worldBox.clip(from, to).orElse(null);
            if (hit != null) {
                double dist = hit.distanceToSqr(from);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = DIRS[i];
                }
            }
        }
        return best;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ZhenBusBlockEntity be)) return;

        Direction hitFace = pickFace(be, accessor.getPlayer(), accessor.getPosition());
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

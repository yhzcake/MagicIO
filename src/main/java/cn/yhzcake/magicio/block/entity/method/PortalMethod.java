package cn.yhzcake.magicio.block.entity.method;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortalMethod extends ZhenMethod {

    /** x,y,z 或 x,y,z yaw pitch，其中 yaw∈(-180,180), pitch∈(-90,90) */
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "(-?[0-9]+(\\.[0-9]+)?,-?[0-9]+(\\.[0-9]+)?,-?[0-9]+(\\.[0-9]+)?)" +
            "( (-?[0-9]+(\\.[0-9]+)?) (-?[0-9]+(\\.[0-9]+)?))?");

    public PortalMethod(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        super(level, pos, state, blockEntity);
    }

    @Override
    public void run() {
        portal_tick();
    }

    public void portal_tick() {
        if (level.isClientSide()) return;

        for (ServerPlayer player : level.getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(pos).inflate(0.5, 0, 0.5),
                p -> p.getBoundingBox().intersects(new AABB(pos))
        )) {
            ItemStack held = player.getMainHandItem();
            if (!held.is(MagicIO.EXAMPLE_ITEM.get())) continue;

            var customData = held.get(DataComponents.CUSTOM_DATA);
            if (customData == null) continue;

            String positionStr = customData.copyTag().getString("position").orElse("");
            if (positionStr.isEmpty() || !COORD_PATTERN.matcher(positionStr).matches()) continue;

            if (player.isShiftKeyDown()) {
                Matcher m = COORD_PATTERN.matcher(positionStr);
                if (!m.matches()) continue;
                try {
                    String[] xyz = m.group(1).split(",");
                    double x = Double.parseDouble(xyz[0]);
                    double y = Double.parseDouble(xyz[1]);
                    double z = Double.parseDouble(xyz[2]);

                    if (m.group(5) != null) {
                        double yaw = Double.parseDouble(m.group(6));
                        double pitch = Double.parseDouble(m.group(8));
                        if (yaw > -180 && yaw < 180 && pitch > -90 && pitch < 90) {
                            player.teleportTo((ServerLevel) level, x, y, z, Set.of(), (float) yaw, (float) pitch, false);
                        }
                    } else {
                        player.teleportTo(x, y, z);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }
}

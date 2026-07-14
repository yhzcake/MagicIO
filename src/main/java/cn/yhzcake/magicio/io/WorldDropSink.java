package cn.yhzcake.magicio.io;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 轻量世界掉落输出端。
 * <p>不再实现 {@link IOComponent}，只负责将物品实体投放到方块面指定方向。</p>
 */
public class WorldDropSink {
    private final Level level;
    private final BlockPos pos;
    private final Direction dropDirection;

    public WorldDropSink(Level level, BlockPos pos, Direction dropDirection) {
        this.level = level;
        this.pos = pos;
        this.dropDirection = dropDirection;
    }

    public WorldDropSink(Level level, BlockPos pos) {
        this(level, pos, Direction.UP);
    }

    public void drop(ItemStack value) {
        if (value.isEmpty()) return;
        BlockPos dropPos = pos.relative(dropDirection);
        Vec3 center = Vec3.atCenterOf(dropPos);
        ItemEntity item = new ItemEntity(level, center.x, center.y - 0.5, center.z, value, 0, 0, 0);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}

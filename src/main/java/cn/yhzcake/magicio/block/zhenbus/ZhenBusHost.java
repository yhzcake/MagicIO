package cn.yhzcake.magicio.block.zhenbus;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ZhenBusHost {

    @Nullable SideProcessor getProcessor(Direction direction);

    boolean canAddProcessor(Direction direction, ZhenType type);

    <T extends SideProcessor> T addProcessor(ZhenType type, Direction direction, Player player);

    void removeProcessor(Direction direction);

    void markForUpdate();

    void markForSave();

    boolean isEmpty();

    BlockEntity getBlockEntity();
}

package cn.yhzcake.magicio.block.entity.method;

import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ZhenMethod implements Runnable {
    public final Level level;
    public final BlockPos pos;
    public final BlockState state;
    public final AbstractZhenBlockEntity blockEntity;

    public ZhenMethod(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.blockEntity = blockEntity;
    }

    @Override
    public void run() {
    }

}

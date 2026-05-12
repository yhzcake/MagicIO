package com.yhzcake.magicio.block.entity.method;

import com.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SmallSiftMethod implements Runnable {
    public final Level level;
    public final BlockPos pos;
    public final BlockState state;
    public final AbstractZhenBlockEntity blockEntity;

    public SmallSiftMethod(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.blockEntity = blockEntity;
    }

    @Override
    public void run() {
    }

    public void small_sift_tick() {
    }
}

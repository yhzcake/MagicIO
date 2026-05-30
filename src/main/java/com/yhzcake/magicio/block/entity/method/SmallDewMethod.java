package com.yhzcake.magicio.block.entity.method;

import com.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SmallDewMethod extends ZhenMethod {
    public SmallDewMethod(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        super(level, pos, state, blockEntity);
    }

    @Override
    public void run() {
    }

    public void small_dew_tick() {
    }
}

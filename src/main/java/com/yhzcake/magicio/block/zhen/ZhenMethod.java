package com.yhzcake.magicio.block.zhen;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.ZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record ZhenMethod(Level level, BlockPos pos, BlockState state, BlockEntity entity) implements Runnable {

    @Override
    public void run() {
        ZhenType<?> type = ((ZhenBlockEntity) entity).getZhenType();
        if (type instanceof ZhenType<?> zhenType) {
            zhenType.executeMethod(this);
        }
    }

    public void small_sift_tick() {
//        MagicIO.LOGGER.info("Small sift at position: {}", this.pos.toString());
    }

    
}
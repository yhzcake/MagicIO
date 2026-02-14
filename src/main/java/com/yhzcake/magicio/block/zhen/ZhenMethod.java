package com.yhzcake.magicio.block.zhen;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.ZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public record ZhenMethod(Level level, BlockPos pos, BlockState state, BlockEntity entity) implements Runnable {

    @Override
    public void run() {
        // 这个方法现在可以保持为空或者用于默认行为
        // 实际的特定方法调用由ZhenType.executeMethod处理
    }

    public void small_sift_tick() {
//        MagicIO.LOGGER.info("Small sift at position: {}", this.pos.toString());
    }

    
}

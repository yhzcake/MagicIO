package cn.yhzcake.magicio.block.entity.method;

import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class UnstableDewMethod extends ZhenMethod {
    public UnstableDewMethod(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        super(level, pos, state, blockEntity);
    }

    @Override
    public void run() {
    }

    public void unstable_dew_tick() {
    }
}

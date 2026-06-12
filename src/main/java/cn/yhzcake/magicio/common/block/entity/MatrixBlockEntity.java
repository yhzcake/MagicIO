package cn.yhzcake.magicio.common.block.entity;

import cn.yhzcake.magicio.common.registry.MIOMatrixTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MatrixBlockEntity extends AbstractMatrixBlockEntity {

    public MatrixBlockEntity(BlockPos pos, BlockState state) {
        super(MIOMatrixTypes.DEFAULT.value(), pos, state);
    }

}

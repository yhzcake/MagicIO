package cn.yhzcake.magicio.common.block;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import cn.yhzcake.magicio.common.block.entity.MatrixBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * @author MakerTechno
 * @since origin
 */
public class MatrixBlock extends BaseEntityBlock {

    private MapCodec<MatrixBlock> codec;

    public MatrixBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatrixBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }
}

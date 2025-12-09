package com.yhzcake.magicio.block.zhen;

import com.mojang.serialization.MapCodec;
import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.ModBlockEntities;
import com.yhzcake.magicio.block.entity.ZhenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ZhenBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final DirectionProperty TURNING = DirectionProperty.create("turning",Direction.Plane.HORIZONTAL);
    //if !DOWN && !UP, up = north, down = south, left = east, right = west
    public static final MapCodec<ZhenBlock> CODEC = simpleCodec(ZhenBlock::new);
    
    public ZhenBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TURNING, Direction.NORTH));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0,0,0,16,16,1);
            case SOUTH -> Block.box(0,0,15,16,16,16);
            case WEST -> Block.box(0,0,0,1,16,16);
            case EAST -> Block.box(15,0,0,16,16,16);
            case UP -> Block.box(0,15,0,16,16,16);
            case DOWN -> Block.box(0,0,0,16,1,16);
        };
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
        if (context.getClickedFace() == Direction.UP || context.getClickedFace() == Direction.DOWN){
            state = state.setValue(TURNING, context.getHorizontalDirection());
        }
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TURNING);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving){
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ZhenBlockEntity) {
                ((ZhenBlockEntity) blockEntity).dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public double[] getHitPoint(double x,double y,double z,Direction facing,Direction turning){
        return switch (facing) {
            case NORTH -> new double[]{x, 1-y};
            case SOUTH -> new double[]{1-x, 1-y};
            case EAST -> new double[]{z, 1-y};
            case WEST -> new double[]{1-z, 1-y};
            case UP -> switch (turning){
                case DOWN, UP -> null;
                case NORTH -> new double[]{x,1-z};
                case SOUTH -> new double[]{1-x,z};
                case EAST -> new double[]{z,x};
                case WEST -> new double[]{1-z,1-x};
            };
            case DOWN -> switch (turning){
                case DOWN, UP -> null;
                case NORTH -> new double[]{x,z};
                case SOUTH -> new double[]{1-x,1-z};
                case EAST -> new double[]{z,1-x};
                case WEST -> new double[]{1-z,x};
            };
        };
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (!level.isClientSide()){
            BlockEntity entity = level.getBlockEntity(pos);

            Vec3 hitVec = hitResult.getLocation();
            double relativeX = hitVec.x - pos.getX();
            double relativeY = hitVec.y - pos.getY();
            double relativeZ = hitVec.z - pos.getZ();
            MagicIO.LOGGER.info(Arrays.toString(getHitPoint(relativeX, relativeY, relativeZ, state.getValue(FACING), state.getValue(TURNING))));

            if(entity instanceof ZhenBlockEntity){
                player.openMenu(state.getMenuProvider(level, pos));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ZhenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state,@NotNull BlockEntityType<T> type) {
        if (type == ModBlockEntities.ZHEN_BLOCK.get()) {
            return ZhenBlockEntity::tick;
        }
        return null;
    }
}
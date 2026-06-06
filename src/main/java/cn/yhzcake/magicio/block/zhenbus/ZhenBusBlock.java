package cn.yhzcake.magicio.block.zhenbus;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ZhenBusBlock extends BaseEntityBlock {

    public static final MapCodec<ZhenBusBlock> CODEC = simpleCodec(ZhenBusBlock::new);

    public ZhenBusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZhenBusBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        if (blockEntityType != ModZhenBusBlocks.ZHEN_BUS_BE.get()) return null;
        return createTickerHelper(blockEntityType, ModZhenBusBlocks.ZHEN_BUS_BE.get(), ZhenBusBlockEntity::serverTick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (level.getBlockEntity(pos) instanceof ZhenBusBlockEntity be) {
            VoxelShape shape = be.getCombinedShape();
            if (!shape.isEmpty()) return shape;
        }
        // 永远不返回空，否则 MC 会渲染异常或无法选中
        return ZhenBusContainer.DEFAULT_FACE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // 返回与视觉形状一致的碰撞箱，使管道/导管模组能够识别并连接
        if (level.getBlockEntity(pos) instanceof ZhenBusBlockEntity be) {
            VoxelShape shape = be.getCombinedShape();
            if (!shape.isEmpty()) return shape;
        }
        return ZhenBusContainer.DEFAULT_FACE;
    }

    @Override
    public VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return Shapes.empty();
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        if (blockEntity instanceof ZhenBusBlockEntity host) {
            for (ItemStack drop : host.collectDrops()) {
                Block.popResource(level, pos, drop);
            }
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}

package cn.yhzcake.magicio.block.zhenbus;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.BlockHitResult;
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof ZhenBusBlockEntity be) {
            be.addProcessor(ZhenTypes.SMALL_SIFT_ZHEN.get(), Direction.DOWN, null);
        }
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!stack.is(MagicIO.EXAMPLE_ITEM.get())) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof ZhenBusBlockEntity be) {
            be.addProcessor(ZhenTypes.SMALL_DEW_ZHEN.get(), Direction.DOWN, player);
            be.markForUpdate();
            level.invalidateCapabilities(pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}

package com.yhzcake.magicio.block.zhen;

import com.mojang.serialization.MapCodec;
import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.ModBlockEntities;
import com.yhzcake.magicio.block.entity.ZhenBlockEntity;
import com.yhzcake.magicio.utils.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("null")
public class ZhenBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final DirectionProperty TURNING = DirectionProperty.create("turning",Direction.Plane.HORIZONTAL);
    //if !DOWN && !UP, up = north, down = south, left = east, right = west
    public static final MapCodec<ZhenBlock> CODEC = simpleCodec(ZhenBlock::new);
    private static ZhenType<?> type;
    
    public ZhenBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TURNING, Direction.NORTH));
    }

    // 方块基本属性和状态
    @Override
    public  VoxelShape getShape( BlockState state,  BlockGetter getter,  BlockPos pos,  CollisionContext context) {
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
    public BlockState getStateForPlacement( BlockPlaceContext context) {
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
    protected  MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public  RenderShape getRenderShape( BlockState state) {
        return RenderShape.MODEL;
    }

    // 方块实体相关方法
    @Override
    public @Nullable BlockEntity newBlockEntity( BlockPos pos,  BlockState state) {
        return new ZhenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
             Level level,  BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.ZHEN_BLOCK.get()) {
            return ZhenBlockEntity::tick;
        }
        return null;
    }

    // 方块放置和移除
    @Override
    public void setPlacedBy( Level level,  BlockPos pos,  BlockState state, @Nullable LivingEntity placer,  ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ZhenBlockEntity zhenBlockEntity) {
                // 从物品组件中加载内容（仅在有内容时）
                ItemContainerContents contents = stack.getOrDefault(ModDataComponents.ZHEN_CONTENTS, ItemContainerContents.EMPTY);
                if (contents != ItemContainerContents.EMPTY) {
                    zhenBlockEntity.loadFromComponent(contents);
                }
            }
            
            // 从物品堆栈中获取类型信息
            String typeStr = stack.getOrDefault(ModDataComponents.ZHEN_TYPE, "small_sift_zhen");
            // 添加空值检查和异常处理
            if (!typeStr.isEmpty()) {
                try {
                    type = ZhenTypes.getType(typeStr);
                } catch (Exception e) {
                    // 如果无法获取指定类型，回退到默认类型
                    type = ZhenTypes.SMALL_SIFT_ZHEN.get();
                }
            } else {
                type = ZhenTypes.SMALL_SIFT_ZHEN.get();
            }
            
            // 直接清理方块实体NBT中的components标签
            directlyCleanBlockEntityNBT(level, pos);
        }
    }
    
    @Override
    public void onRemove( BlockState state,  Level level,  BlockPos pos,  BlockState newState, boolean isMoving){
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ZhenBlockEntity) {
                ((ZhenBlockEntity) blockEntity).dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    // 添加一个新的方法来直接清理方块实体的NBT数据
    private void directlyCleanBlockEntityNBT(Level level, BlockPos pos) {
        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            // 获取方块实体的NBT数据
            CompoundTag tag = blockEntity.saveWithoutMetadata(level.registryAccess());
            
            // 删除components标签
            tag.remove("components");
            
            // 重新加载清理后的NBT数据
            blockEntity.loadWithComponents(tag, level.registryAccess());
            
            // 标记方块实体为脏以触发保存
            blockEntity.setChanged();
        }
    }

    // 玩家交互
    @Override
    public  InteractionResult useWithoutItem( BlockState state,  Level level,  BlockPos pos,  Player player,  BlockHitResult hitResult) {
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
    public  ItemStack getCloneItemStack( BlockState state,  HitResult target,  LevelReader level,  BlockPos pos,  Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ZhenBlockEntity be) {
            // 设置类型数据
            stack.set(ModDataComponents.ZHEN_TYPE, be.getZhenType().getType());
            // 使用Shift键来决定是否复制inventory（而不是Alt键）
            if (player.isShiftKeyDown()) {
                stack.set(ModDataComponents.ZHEN_CONTENTS, be.getContentsComponent());
            }
        }
        return stack;
    }

    // 其他辅助方法
    public ZhenType<?> getType() {
        return type!=null?type:ZhenTypes.SMALL_SIFT_ZHEN.get();
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
}
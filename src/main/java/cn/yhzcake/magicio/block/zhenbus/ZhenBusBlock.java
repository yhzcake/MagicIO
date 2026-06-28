package cn.yhzcake.magicio.block.zhenbus;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.gridcell.CellAction;
import cn.yhzcake.magicio.block.gridcell.GridCellSideProcessor;
import cn.yhzcake.magicio.block.gridcell.GridCellStorage;
import cn.yhzcake.magicio.block.zhen.ZhenBlock;
import cn.yhzcake.magicio.block.zhen.ZhenFunctions;
import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.block.zhen.ZhenTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

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
            be.addProcessor(ZhenFunctions.UNSTABLE_SIEVE_ZHEN.get(), Direction.DOWN, null);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        // 掉落实体方块自身
        super.playerDestroy(level, player, pos, state, blockEntity, stack);
        // 掉落处理器内的物品（setRemoved 不再处理掉落以规避世界重载时误掉）
        if (!level.isClientSide() && blockEntity instanceof ZhenBusBlockEntity be) {
            for (ItemStack drop : be.collectDrops()) {
                Block.popResource(level, pos, drop);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ZhenBusBlockEntity be)) return InteractionResult.PASS;

        Direction face = hitResult.getDirection();
        var processor = be.getProcessor(face);

        // GridCell 面交互：主手持 stick
        if (processor instanceof GridCellSideProcessor gridProc && stack.is(Items.STICK)) {
            // 计算点击位置在面内的相对坐标
            double fx, fz;
            Vec3 hit = hitResult.getLocation();
            if (face == Direction.DOWN || face == Direction.UP) {
                // 水平面：使用 x,z
                fx = hit.x - pos.getX();
                fz = hit.z - pos.getZ();
            } else if (face == Direction.NORTH || face == Direction.SOUTH) {
                // 南北面：使用 x,y
                fx = hit.x - pos.getX();
                fz = hit.y - pos.getY();
            } else {
                // 东西面：使用 z,y
                fx = hit.z - pos.getZ();
                fz = hit.y - pos.getY();
            }

            int[] cell = GridCellStorage.hitToCell(fx, fz);
            int cellX = cell[0];
            int cellZ = cell[1];

            ItemStack offhand = player.getOffhandItem();
            if (offhand.isEmpty()) {
                // 副手为空 → 清除该格
                gridProc.getGridStorage().clearCell(cellX, cellZ);
            } else {
                CellAction action = GridCellStorage.getActionForOffhand(offhand);
                if (action == null) return InteractionResult.PASS;
                gridProc.getGridStorage().setCell(cellX, cellZ, action);
            }
            be.markForUpdate();
            return InteractionResult.SUCCESS;
        }

        // 主手持 ZhenBlock 物品 → 在该面（或副手 example_item 指定面）安装处理器
        Block carriedBlock = Block.byItem(stack.getItem());
        if (carriedBlock instanceof ZhenBlock) {
            // 确定目标面：副手有 example_item 且带 side NBT 时用其值，否则用 hit 面
            Direction targetFace = face;
            ItemStack offhand = player.getOffhandItem();
            if (offhand.is(MagicIO.EXAMPLE_ITEM.get())) {
                CompoundTag tag = offhand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                String sideName = tag.getString("side").orElse("");
                Direction offhandDir = Direction.byName(sideName);
                if (offhandDir != null) {
                    targetFace = offhandDir;
                }
            }

            Identifier blockId = BuiltInRegistries.BLOCK.getKey(carriedBlock);
            ZhenType type = ZhenTypes.getType(blockId);
            if (type != null) {
                be.addProcessor(type, targetFace, player);
                be.markForUpdate();
                level.invalidateCapabilities(pos);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}

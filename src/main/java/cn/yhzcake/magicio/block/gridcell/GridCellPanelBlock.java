package cn.yhzcake.magicio.block.gridcell;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import cn.yhzcake.magicio.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 网格面板方块。
 * 碰撞箱为 1/16 高的平面薄片，支持 4×4 单元格交互。
 */
public class GridCellPanelBlock extends BaseEntityBlock {

    public static final MapCodec<GridCellPanelBlock> CODEC = simpleCodec(GridCellPanelBlock::new);

    /** 平面碰撞箱，与 ZhenBus 一致 */
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1.0 / 16, 1);

    public GridCellPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GridCellPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        if (type != ModBlockEntities.GRID_CELL_PANEL_BE.get()) return null;
        return createTickerHelper(type, ModBlockEntities.GRID_CELL_PANEL_BE.get(), GridCellPanelBlockEntity::serverTick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
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
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack stack) {
        super.playerDestroy(level, player, pos, state, blockEntity, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 仅主手持木棍时触发
        if (!stack.is(Items.STICK)) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof GridCellPanelBlockEntity be)) return InteractionResult.PASS;

        // 计算点击的单元格坐标
        Vec3 hit = hitResult.getLocation();
        double fx = hit.x - pos.getX();  // 0~1 相对坐标
        double fz = hit.z - pos.getZ();
        int[] cell = GridCellStorage.hitToCell(fx, fz);
        int cellX = cell[0];
        int cellZ = cell[1];

        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty()) {
            // 副手为空 → 清除该格
            be.clearCell(cellX, cellZ);
            be.markForUpdate();
        } else {
            // 副手有物品 → 查找 CellAction 并填充
            CellAction action = GridCellStorage.getActionForOffhand(offhand);
            if (action == null) return InteractionResult.PASS;

            be.setCell(cellX, cellZ, action);
            be.markForUpdate();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}

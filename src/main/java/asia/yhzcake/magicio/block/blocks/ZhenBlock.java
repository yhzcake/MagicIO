package asia.yhzcake.magicio.block.blocks;

import static java.lang.Math.pow;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.WindMethod;
import asia.yhzcake.magicio.block.blocks.method.ZhenHash;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;

public class ZhenBlock extends Block implements EntityBlock {
  public static final DirectionProperty FACING = BlockStateProperties.FACING;
  public static final MapCodec<ZhenBlock> CODEC = simpleCodec(properties -> new ZhenBlock(ZhenProperties.from(properties)));
  protected static final VoxelShape BASE_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
  protected static final VoxelShape INNER_SHAPE = Block.box(2.0, 0.0, 6.0, 6.0, 4.0, 10.0);
  protected static final VoxelShape OUTER_SHAPE = Block.box(10.0, 0.0, 6.0, 14.0, 4.0, 10.0);
  protected static final VoxelShape HAS_INNER_SHAPE = Shapes.or(BASE_SHAPE, INNER_SHAPE);
  protected static final VoxelShape HAS_OUTER_SHAPE = Shapes.or(BASE_SHAPE, OUTER_SHAPE);
  protected static final VoxelShape FULL_SHAPE = Shapes.or(HAS_INNER_SHAPE, HAS_OUTER_SHAPE);

  private static int hasInner;
  private static int hasOuter;
  private ZhenType zhenType;

  public ZhenBlock(ZhenProperties properties) {
    super(properties.getProperties());
    this.zhenType = properties.getZhenType();
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  // 阵法类型注册
  public static void registriesZhenType() {
    ZhenHash.ZHENMETHOD.addMethod("wind", WindMethod::windTick);
  }

  // 设置碰撞箱
  @Override
  protected VoxelShape getShape(
      @Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
    BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
    if (blockEntity instanceof ZhenBlockEntity) {
      hasInner = ((ZhenBlockEntity) blockEntity).isHasInner() ? 1 : 0;
      hasOuter = ((ZhenBlockEntity) blockEntity).isHasOuter() ? 1 : 0;
    }
    int shape = (int) pow(2, hasInner) + (int) pow(1, hasOuter);
    return switch (shape) {
      case 1 -> HAS_OUTER_SHAPE;
      case 2 -> HAS_INNER_SHAPE;
      case 3 -> FULL_SHAPE;
      default -> BASE_SHAPE;
    };
  }

  @Override
  public MapCodec<ZhenBlock> codec() {
    return CODEC;
  }

  /**
   * 获取方块的阵法类型
   * @return 阵法类型
   */
  public ZhenType getZhenType() {
    return this.zhenType;
  }

  // 不知道为什么这么写,但是关联方块和方块实体要这么写
  @Override
  public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
    return new ZhenBlockEntity(pos, state, this.zhenType);
  }

  @Override
  @Nullable
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      @Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
    if (type == Blocks.ZHEN_BLOCK_ENTITY.get()) {
      return (BlockEntityTicker<T>) ZhenBlockEntity::tick;
    }
    return null;
  }

  @Override
  protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> pBuilder) {
    // this is where the properties are actually added to the state
    pBuilder.add(FACING);
  }

  // 改写use方法
  @Override
  protected InteractionResult useWithoutItem(
      @Nonnull BlockState state,
      @Nonnull Level level,
      @Nonnull BlockPos pos,
      @Nonnull Player player,
      @Nonnull BlockHitResult hit) {
    if (!level.isClientSide() && player.isShiftKeyDown()) {
      BlockEntity zhenSelf = level.getBlockEntity(pos);
      if (zhenSelf instanceof ZhenBlockEntity) {
        // TODO: 添加具体的交互逻辑
      }
    }
    return InteractionResult.SUCCESS;
  }
}
/*// Note: The ticker is defined in the block, not the block entity. However, it is good practice to
// keep the ticking logic in the block entity in some way, for example by defining a static #tick method.
public class MyEntityBlock extends Block implements EntityBlock {
    // other stuff here

    @SuppressWarnings("unchecked") // Due to generics, an unchecked cast is necessary here.
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // You can return different tickers here, depending on whatever factors you want. A common use case would be
        // to return different tickers on the client or server, only tick one side to begin with,
        // or only return a ticker for some blockstates (e.g. when using a "my machine is working" blockstate property).
        return type == MY_BLOCK_ENTITY.get() ? (BlockEntityTicker<T>) MyBlockEntity::tick : null;
    }
}

public class MyBlockEntity extends BlockEntity {
    // other stuff here

    // The signature of this method matches the signature of the BlockEntityTicker functional interface.
    public static void tick(Level level, BlockPos pos, BlockState state, MyBlockEntity blockEntity) {
        // Whatever you want to do during ticking.
        // For example, you could change a crafting progress value or consume power here.
    }
}*/

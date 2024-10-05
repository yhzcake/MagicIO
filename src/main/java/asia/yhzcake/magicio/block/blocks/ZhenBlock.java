package asia.yhzcake.magicio.block.blocks;

import asia.yhzcake.magicio.block.Blocks;
import asia.yhzcake.magicio.block.blocks.method.ZhenHash;
import asia.yhzcake.magicio.block.blocks.method.windMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ZhenBlock extends Block implements EntityBlock {

  public ZhenBlock(Block.Properties properties) {
    super(properties);
  }

  // 阵法类型注册
  public static void registriesZhenType() {
    ZhenHash.ZHENMETHOD.addMethod("wind", windMethod::windtick);
  }

  // 不知道为什么这么写,但是关联方块和方块实体要这么写
  @Override
  public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
    return new ZhenBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
    return type == Blocks.ZHEN_BLOCK_ENTITY.get() ? ZhenBlockEntity::tick : null;
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

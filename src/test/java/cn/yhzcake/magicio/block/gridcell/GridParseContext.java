package cn.yhzcake.magicio.block.gridcell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 网格解析规则的执行上下文。
 * 当 4×4 网格全部填满后，匹配到的 GridParseRule 会获得此上下文。
 *
 * @param level       当前世界
 * @param pos         方块位置
 * @param state       方块状态
 * @param blockEntity 方块实体（Stage 2 实现为 GridCellPanelBlockEntity）
 * @param grid        4×4 网格的只读快照（16 个 CellAction）
 */
public record GridParseContext(
        Level level,
        BlockPos pos,
        BlockState state,
        BlockEntity blockEntity,
        CellAction[][] grid
) {
}

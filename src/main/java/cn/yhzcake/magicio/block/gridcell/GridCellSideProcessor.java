package cn.yhzcake.magicio.block.gridcell;

import cn.yhzcake.magicio.block.zhen.ZhenType;
import cn.yhzcake.magicio.io.AbstractSideProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 网格面板面处理器 — 作为 ZhenBus 的一个面使用。
 * 持有 {@link GridCellStorage}，在其 tick() 中检查 4×4 网格是否全满并触发解析规则。
 */
public class GridCellSideProcessor extends AbstractSideProcessor {

    private final GridCellStorage storage = new GridCellStorage();

    public GridCellSideProcessor(Direction side, ZhenType zhenType, BlockPos pos, Level level) {
        super(side, zhenType, pos, level);
    }

    public GridCellStorage getGridStorage() {
        return storage;
    }

    @Override
    public void tick() {
        // 不调用 super.tick()，完全使用 GridCell 自身的 tick 逻辑
        if (storage.isFullyFilled() && !storage.isParseTriggered()) {
            storage.setParseTriggered(true);
            runParseRules();
        }
    }

    /** 遍历所有 GridParseRule，执行第一个匹配的规则 */
    private void runParseRules() {
        CellAction[][] snapshot = storage.getSnapshot();
        for (GridParseRule rule : GridParseRule.GRID_PARSE_RULES) {
            if (rule.matches(snapshot)) {
                GridParseContext ctx = new GridParseContext(
                        level, pos, level.getBlockState(pos), null, snapshot);
                rule.execute(ctx);
                break;
            }
        }
    }

    @Override
    public void writeToNBT(ValueOutput output) {
        super.writeToNBT(output);
        ValueOutput child = output.child("grid_data");
        storage.writeToNBT(child);
    }

    @Override
    public void readFromNBT(ValueInput input) {
        super.readFromNBT(input);
        input.child("grid_data").ifPresent(storage::readFromNBT);
    }
}

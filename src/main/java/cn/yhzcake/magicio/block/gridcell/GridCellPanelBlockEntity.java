package cn.yhzcake.magicio.block.gridcell;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 网格面板方块实体。
 * 管理 4×4 网格数据，server tick 时检查是否全满并触发解析规则。
 */
public class GridCellPanelBlockEntity extends BlockEntity {

    private final GridCellStorage storage = new GridCellStorage();

    public GridCellPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRID_CELL_PANEL_BE.get(), pos, state);
    }

    public GridCellStorage getStorage() {
        return storage;
    }

    // ===== 单元格操作委托 =====

    public void setCell(int x, int z, CellAction action) {
        storage.setCell(x, z, action);
    }

    public void clearCell(int x, int z) {
        storage.clearCell(x, z);
    }

    public @Nullable CellAction getCell(int x, int z) {
        return storage.getCell(x, z);
    }

    // ===== Server Tick =====

    public static void serverTick(Level level, BlockPos pos, BlockState state, GridCellPanelBlockEntity be) {
        // 检查 16 格是否全满且未触发解析
        if (be.storage.isFullyFilled() && !be.storage.isParseTriggered()) {
            be.storage.setParseTriggered(true);
            be.runParseRules();
            be.setChanged();
        }
    }

    /** 遍历所有 GridParseRule，执行第一个匹配的规则 */
    private void runParseRules() {
        if (level == null || level.isClientSide()) return;

        CellAction[][] snapshot = storage.getSnapshot();
        for (GridParseRule rule : GridParseRule.GRID_PARSE_RULES) {
            if (rule.matches(snapshot)) {
                GridParseContext ctx = new GridParseContext(
                        level, worldPosition, getBlockState(), this, snapshot);
                rule.execute(ctx);
                break;  // 只执行第一个匹配的规则
            }
        }
    }

    // ===== 标记更新 =====

    public void markForUpdate() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ===== NBT（使用 NeoForge ValueOutput/ValueInput） =====

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput child = output.child("grid_data");
        storage.writeToNBT(child);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("grid_data").ifPresent(storage::readFromNBT);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        storage.writeToUpdateTag(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

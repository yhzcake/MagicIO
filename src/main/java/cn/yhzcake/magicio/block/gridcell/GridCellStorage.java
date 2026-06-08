package cn.yhzcake.magicio.block.gridcell;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 4×4 网格数据存储。
 * 管理 16 个单元格的 CellAction，提供序列化/反序列化以及副手物品查找。
 */
public class GridCellStorage {

    private static final String TAG_CELL_PREFIX = "cell_";
    private static final int SIZE = 4;

    /** grid[x][z]，null 表示该格未填充 */
    private final CellAction[][] grid = new CellAction[SIZE][SIZE];

    /** 是否已在填满后触发过解析（防止重复触发） */
    private boolean parseTriggered = false;

    // ===== 单元格读写 =====

    public void setCell(int x, int z, @Nullable CellAction action) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) return;
        grid[x][z] = action;
    }

    public @Nullable CellAction getCell(int x, int z) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) return null;
        return grid[x][z];
    }

    /** 清除指定单元格并重置解析触发状态 */
    public void clearCell(int x, int z) {
        setCell(x, z, null);
        parseTriggered = false;
    }

    /** 检查是否 16 格全部非空 */
    public boolean isFullyFilled() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (grid[x][z] == null) return false;
            }
        }
        return true;
    }

    /** 获取 4×4 只读快照 */
    public CellAction[][] getSnapshot() {
        CellAction[][] copy = new CellAction[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            System.arraycopy(grid[x], 0, copy[x], 0, SIZE);
        }
        return copy;
    }

    // ===== 解析触发控制 =====

    public boolean isParseTriggered() {
        return parseTriggered;
    }

    public void setParseTriggered(boolean triggered) {
        this.parseTriggered = triggered;
    }

    // ===== 副手物品查找 =====

    /**
     * 根据副手物品栈查找对应的 CellAction。
     * 遍历 CellAction 注册表，匹配物品类型。
     */
    public static @Nullable CellAction getActionForOffhand(ItemStack offhand) {
        if (offhand.isEmpty()) return null;
        Item item = offhand.getItem();
        return CellAction.getByItem(item);
    }

    // ===== 坐标计算 =====

    /**
     * 将点击位置相对坐标 (0~1) 转换为单元格索引 (0~3)。
     *
     * @param fx 方块内相对 X 坐标 (0~1)
     * @param fz 方块内相对 Z 坐标 (0~1)
     * @return 包含 cellX, cellZ 的数组
     */
    public static int[] hitToCell(double fx, double fz) {
        int cellX = net.minecraft.util.Mth.clamp((int) (fx * SIZE), 0, SIZE - 1);
        int cellZ = net.minecraft.util.Mth.clamp((int) (fz * SIZE), 0, SIZE - 1);
        return new int[]{cellX, cellZ};
    }

    // ===== NBT 序列化（使用 CompoundTag，用于 getUpdateTag/网络同步） =====

    /** 写入 CompoundTag（用于网络同步的 getUpdateTag） */
    public void writeToUpdateTag(CompoundTag tag) {
        tag.putBoolean("parse_triggered", parseTriggered);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                CellAction action = grid[x][z];
                if (action != null) {
                    Identifier id = CellAction.CELL_ACTIONS.getKey(action);
                    if (id != null) {
                        tag.putString(TAG_CELL_PREFIX + x + "_" + z, id.toString());
                    }
                }
            }
        }
    }

    /** 从 CompoundTag 读取（用于网络同步） */
    public void readFromUpdateTag(CompoundTag tag) {
        parseTriggered = tag.getBoolean("parse_triggered").orElse(false);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                String key = TAG_CELL_PREFIX + x + "_" + z;
                final int cx = x;
                final int cz = z;
                tag.getString(key).ifPresent(idStr -> {
                    Identifier id = Identifier.parse(idStr);
                    CellAction.CELL_ACTIONS.get(id)
                            .map(Holder.Reference::value)
                            .ifPresent(action -> grid[cx][cz] = action);
                });
            }
        }
    }

    // ===== NBT 序列化（使用 ValueOutput/ValueInput，用于持久化存储） =====

    public void writeToNBT(ValueOutput output) {
        output.putBoolean("parse_triggered", parseTriggered);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                CellAction action = grid[x][z];
                if (action != null) {
                    Identifier id = CellAction.CELL_ACTIONS.getKey(action);
                    if (id != null) {
                        output.putString(TAG_CELL_PREFIX + x + "_" + z, id.toString());
                    }
                }
            }
        }
    }

    public void readFromNBT(ValueInput input) {
        parseTriggered = input.getBooleanOr("parse_triggered", false);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                String key = TAG_CELL_PREFIX + x + "_" + z;
                final int cx = x;
                final int cz = z;
                input.getString(key).ifPresent(idStr -> {
                    Identifier id = Identifier.parse(idStr);
                    CellAction.CELL_ACTIONS.get(id)
                            .map(Holder.Reference::value)
                            .ifPresent(action -> grid[cx][cz] = action);
                });
            }
        }
    }
}

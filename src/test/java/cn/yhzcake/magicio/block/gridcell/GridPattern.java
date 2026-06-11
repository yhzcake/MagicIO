package cn.yhzcake.magicio.block.gridcell;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * 网格匹配模式 — 将用户配置化的匹配规则抽象为可复用的匹配器。
 *
 * <p>配置格式示例：
 * <pre>
 * {
 *   "dir": 5,            // 检测区域 (1-9)
 *   "type": "sharp",     // sharp=按位精确匹配, sharpless=仅包含即可
 *   "grid": [["a","a"],["a","a"]],  // 标签模式二维数组
 *   "a": "minecraft:coal"           // 标签 → 物品ID映射
 * }
 *
 * <h3>dir → 4×4 网格起始坐标映射规则</h3>
 *
 * dir 将 4×4 网格抽象为一个 3×3 的九宫格参考系，起始坐标自适应 pattern 尺寸：
 * <pre>
 *         z=0   z=1   z=2
 *   x=0  [ 1 ] [ 2 ] [ 3 ]
 *   x=1  [ 4 ] [ 5 ] [ 6 ]
 *   x=2  [ 7 ] [ 8 ] [ 9 ]
 * </pre>
 *
 * 实际起始坐标 = maxStart × ((dir-1) % 3) / 2<br>
 * 其中 maxStart = 4 - patternSize，确保 pattern 不越界。
 *
 * <p><b>示例：</b>
 * <ul>
 *   <li>2×2 pattern: maxStart=2，9 个 dir 分别映射到 (0,0)(1,0)(2,0)...(2,2) 全部有效</li>
 *   <li>3×3 pattern: maxStart=1，仅 1/3/7/9 映射到唯一位置 (0,0)(1,0)(0,1)(1,1)，其余 dir 折叠到最近角</li>
 * </ul>
 */
public class GridPattern {

    /** 匹配类型 */
    public enum MatchType {
        /** 按位置精确匹配：pattern[i][j] 必须与 grid[startX+i][startZ+j] 一致 */
        SHARP,
        /** 仅包含匹配：检测区域内的每个 cell 只要在 pattern 定义的标签集合中即可，不关心顺序 */
        SHARPLESS
    }

    private final int dir;
    private final MatchType type;
    private final String[][] pattern;
    private final Map<String, Supplier<CellAction>> tags;

    public GridPattern(int dir, MatchType type, String[][] pattern, Map<String, Supplier<CellAction>> tags) {
        if (dir < 1 || dir > 9) throw new IllegalArgumentException("dir must be 1-9, got " + dir);
        this.dir = dir;
        this.type = Objects.requireNonNull(type, "type is null");
        this.pattern = Objects.requireNonNull(pattern, "pattern is null");
        this.tags = Map.copyOf(Objects.requireNonNull(tags, "tags is null"));
    }

    public int getDir() { return dir; }
    public MatchType getType() { return type; }
    public String[][] getPattern() { return pattern; }
    public Map<String, Supplier<CellAction>> getTags() { return tags; }

    /**
     * 计算 dir 在 4×4 网格中的起始坐标，自适应 pattern 尺寸。
     * <p>
     * 原理：将 4×4 网格抽象为 3×3 九宫格参考系，起始坐标在 [0, maxStart] 间均匀分布。
     * <pre>
     * startX = maxStartX × ((dir-1) % 3) / 2
     * startZ = maxStartZ × ((dir-1) / 3) / 2
     * maxStartX = 4 - pattern列数,  maxStartZ = 4 - pattern行数
     * </pre>
     *
     * @param rows pattern 行数
     * @param cols pattern 列数
     * @return 包含 startX, startZ 的长度为 2 的数组
     */
    private int[] computeStartPos(int rows, int cols) {
        int maxStartX = 4 - cols;
        int maxStartZ = 4 - rows;
        int startX = maxStartX * ((dir - 1) % 3) / 2;
        int startZ = maxStartZ * ((dir - 1) / 3) / 2;
        return new int[]{startX, startZ};
    }

    /**
     * 测试当前模式是否匹配给定的 4×4 网格。
     */
    public boolean matches(CellAction[][] grid) {
        if (grid == null || grid.length != 4) return false;
        // 确保每行长度也是 4
        for (int x = 0; x < 4; x++) {
            if (grid[x] == null || grid[x].length != 4) return false;
        }

        int rows = pattern.length;
        if (rows == 0) return false;
        int cols = pattern[0].length;

        int[] startPos = computeStartPos(rows, cols);
        int startX = startPos[0];
        int startZ = startPos[1];

        // 边界检查
        if (startX + rows > 4 || startZ + cols > 4) return false;

        if (type == MatchType.SHARP) {
            return matchesSharp(grid, startX, startZ, rows, cols);
        } else {
            return matchesSharpless(grid, startX, startZ, rows, cols);
        }
    }

    /** sharp：按位置精确比对 */
    private boolean matchesSharp(CellAction[][] grid, int startX, int startZ, int rows, int cols) {
        for (int x = 0; x < rows; x++) {
            for (int z = 0; z < cols; z++) {
                String label = pattern[x][z];
                Supplier<CellAction> supplier = tags.get(label);
                if (supplier == null) return false;
                CellAction expected = supplier.get();
                if (grid[startX + x][startZ + z] != expected) return false;
            }
        }
        return true;
    }

    /** sharpless：检测区域内每个 cell 只要在 pattern 定义的标签集合中即可 */
    private boolean matchesSharpless(CellAction[][] grid, int startX, int startZ, int rows, int cols) {
        // 构建允许的 CellAction 集合
        Set<CellAction> allowed = new HashSet<>();
        for (int x = 0; x < rows; x++) {
            for (int z = 0; z < cols; z++) {
                String label = pattern[x][z];
                Supplier<CellAction> supplier = tags.get(label);
                if (supplier == null) return false;
                allowed.add(supplier.get());
            }
        }
        if (allowed.isEmpty()) return false;

        // 检查区域内每个 cell 是否都在 allowed 中
        for (int x = 0; x < rows; x++) {
            for (int z = 0; z < cols; z++) {
                if (!allowed.contains(grid[startX + x][startZ + z])) return false;
            }
        }
        return true;
    }

    // ===== 工厂方法 =====

    /**
     * 创建一个 {@link Predicate}，可直接作为 {@link GridParseRule} 的 matcher。
     *
     * @param dir     检测区域 (1-9)
     * @param type    匹配类型
     * @param pattern 标签模式二维数组
     * @param tags    标签 → CellAction 映射
     */
    public static Predicate<CellAction[][]> createMatcher(
            int dir, MatchType type, String[][] pattern,
            Map<String, Supplier<CellAction>> tags) {
        GridPattern gp = new GridPattern(dir, type, pattern, tags);
        return gp::matches;
    }

    /**
     * 从标签映射创建便捷构建器。
     * 后续可通过 {@link Builder#tag(String, Supplier)} 添加标签映射。
     */
    public static Builder builder(int dir, MatchType type, String[][] pattern) {
        return new Builder(dir, type, pattern);
    }

    /** 便捷构建器 */
    public static class Builder {
        private final int dir;
        private final MatchType type;
        private final String[][] pattern;
        private final Map<String, Supplier<CellAction>> tags = new HashMap<>();

        Builder(int dir, MatchType type, String[][] pattern) {
            this.dir = dir;
            this.type = type;
            this.pattern = pattern;
        }

        public Builder tag(String label, Supplier<CellAction> action) {
            tags.put(label, action);
            return this;
        }

        public Predicate<CellAction[][]> buildMatcher() {
            return createMatcher(dir, type, pattern, Collections.unmodifiableMap(tags));
        }

        public GridPattern build() {
            return new GridPattern(dir, type, pattern, Collections.unmodifiableMap(tags));
        }
    }
}

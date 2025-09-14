package asia.yhzcake.magicio.block.blocks;

import java.util.Objects;

/**
 * 阵法类型类 - 可扩展设计
 * 允许其他模组注册自定义阵法类型
 */
public class ZhenType {
    private final String id;
    private final String displayName;
    private final String description;
    
    /**
     * 创建一个新的阵法类型
     * @param id 唯一标识符
     * @param displayName 显示名称
     * @param description 描述
     */
    public ZhenType(String id, String displayName, String description) {
        this.id = Objects.requireNonNull(id, "ZhenType ID cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.description = description != null ? description : "";
    }
    
    /**
     * 创建一个新的阵法类型（简化版本）
     * @param id 唯一标识符
     * @param displayName 显示名称
     */
    public ZhenType(String id, String displayName) {
        this(id, displayName, "");
    }

    /**
     * 获取阵法类型的唯一标识符
     * @return 阵法类型ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取阵法类型的显示名称
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取阵法类型的描述
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ZhenType zhenType = (ZhenType) obj;
        return Objects.equals(id, zhenType.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
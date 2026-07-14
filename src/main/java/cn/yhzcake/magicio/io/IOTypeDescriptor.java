package cn.yhzcake.magicio.io;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;

/**
 * IOType 行为描述符。
 * <p>
 * 每种 IOType 注册一个实例，封装该类型在配方执行、事务验证、产出滚动
 * 和输入哈希中的全部行为。新增 IOType 时只需新增实现并注册到
 * {@link IOTypeDescriptors}，无需修改 {@code RecipeProcessor} 或 {@code ZhenRecipe}。
 * <p>
 * 所有方法的 {@code requirement} / {@code value} 参数均为无类型 Object，
 * 具体实现内部按已知类型转换。
 */
public interface IOTypeDescriptor {

    /** 对应的 IOType 标识。 */
    IOType ioType();

    // ==================== 事务快照 ====================

    /**
     * 创建该组件状态的深拷贝快照，用于事务模拟。
     * 快照必须独立于原始组件，模拟对其的修改不影响真实数据。
     */
    Object createSnapshot(IOComponent<?, ?> component);

    // ==================== 模拟（在快照上进行） ====================

    /**
     * 在快照上模拟消耗一个 requirement。
     *
     * @param snapshot    {@link #createSnapshot} 创建的副本
     * @param zone        消耗发生的区域
     * @param requirement 消耗需求
     * @param partition   槽位分区
     * @return 消耗是否全部满足
     */
    boolean simulateConsume(Object snapshot, SlotZone zone, Object requirement, SlotPartition partition);

    /**
     * 在快照上模拟产出 value。
     *
     * @param snapshot  {@link #createSnapshot} 创建的副本
     * @param zone      产出写入的区域
     * @param value     产出值（类型由具体实现决定）
     * @param partition 槽位分区
     * @return 是否全部可容纳（不可容纳时返回 false）
     */
    boolean simulateProduce(Object snapshot, SlotZone zone, Object value, SlotPartition partition);

    // ==================== 实际执行（在真实组件上进行） ====================

    /**
     * 在真实组件上消耗 requirement。
     *
     * @param component   该类型的 IOComponent
     * @param zone        消耗区域
     * @param requirement 消耗需求
     * @param partition   槽位分区
     * @return 消耗是否全部成功
     */
    void commitConsume(IOComponent<?, ?> component, SlotZone zone, Object requirement, SlotPartition partition);

    /**
     * 在真实组件上产出 value。
     *
     * @param component 该类型的 IOComponent
     * @param zone      产出区域
     * @param value     产出值
     * @param partition 槽位分区
     * @return 是否全部产出成功
     */
    void commitProduce(IOComponent<?, ?> component, SlotZone zone, Object value, SlotPartition partition);

    // ==================== 产出滚动 ====================

    /**
     * 根据配方 specification 滚动生成实际产出值列表。
     * 例如 ITEM 类型可能包含战利品表滚动，FLUID 类型直接复制值。
     *
     * @param specification 输出规格（由具体实现确定类型）
     * @param level         服务端世界（用于战利品表滚动）
     * @return 滚动后的产出值列表，按区域名聚合
     */
    @Nullable
    NonNullList<?> rollOutputs(Object specification, ServerLevel level);

    // ==================== 输入匹配 ====================

    /**
     * 检查该类型的组件是否能满足给定的 requirement。
     *
     * @param component   该类型的 IOComponent
     * @param zone        检查区域
     * @param requirement 输入需求
     * @param partition   槽位分区
     * @return 是否能够满足
     */
    boolean matches(IOComponent<?, ?> component, SlotZone zone, Object requirement, SlotPartition partition);

    // ==================== 输入哈希 ====================

    /**
     * 计算该类型组件的输入状态哈希值，用于配方缓存快速判断输入是否变化。
     *
     * @param component 该类型的 IOComponent
     * @param partition 槽位分区
     * @return 输入哈希值
     */
    int computeHash(IOComponent<?, ?> component, SlotPartition partition);
}

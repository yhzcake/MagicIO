package cn.yhzcake.magicio.block.zhen;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.utils.ElementType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 阵的功能定义。
 * <p>描述一种功能（如"筛子""凝水"）的基本属性：元素类型、槽位布局、tick 逻辑等。</p>
 */
public class ZhenFunction {

    @FunctionalInterface
    public interface TickFactory {
        Runnable create(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity be);
    }

    private final String baseName;
    private final Supplier<ElementType> elementSupplier;
    private final int introducedAtLevel;
    private final Supplier<SlotPartition> partitionSupplier;
    private final Map<Direction, Map<IOType, Set<String>>> faceAccess;
    private final TickFactory tickFactory;
    private final Integer tankCapacity;

    public ZhenFunction(String baseName, Supplier<ElementType> elementSupplier, int introducedAtLevel,
            Supplier<SlotPartition> partitionSupplier,
            Map<Direction, Map<IOType, Set<String>>> faceAccess,
            TickFactory tickFactory, Integer tankCapacity) {
        this.baseName = baseName;
        this.elementSupplier = elementSupplier;
        this.introducedAtLevel = introducedAtLevel;
        this.partitionSupplier = partitionSupplier;
        this.faceAccess = faceAccess;
        this.tickFactory = tickFactory;
        this.tankCapacity = tankCapacity;
    }

    public String baseName() { return baseName; }
    public int introducedAtLevel() { return introducedAtLevel; }

    /**
     * 将该功能注册到所有可用的等级。
     * @return 最低引入等级对应的 Supplier（用于外部引用）
     */
    public Supplier<ZhenType> register(DeferredRegister<ZhenType> reg) {
        Supplier<ZhenType> firstResult = null;
        for (ZhenLevel level : ZhenLevel.ALL) {
            if (level.level() < introducedAtLevel) continue;

            String fullId = level.prefix() + baseName + "_zhen";
            Supplier<ZhenType> supplier = reg.register(fullId, () -> {
                ElementType elem = elementSupplier.get();
                SlotPartition partition = partitionSupplier.get();
                Map<Direction, Map<IOType, Set<String>>> access = faceAccess != null
                        ? faceAccess : ZhenTypes.defaultAccess();
                Function<ZhenType.ZhenTickContext, Runnable> tickFn = tickFactory != null
                        ? (ctx) -> tickFactory.create(ctx.level(), ctx.pos(), ctx.state(), ctx.blockEntity())
                        : null;

                return new ZhenType(elem, "magic_io:" + fullId,
                        partition, level.level(),
                        tickFn, tankCapacity, null, access, Map.of());
            });
            if (firstResult == null) firstResult = supplier;
        }
        return firstResult;
    }

    // ===== 便捷工厂方法 =====
    // 这些方法的 SlotPartition 构造被封装在 Supplier 中，仅在 register lambda 内延迟执行

    /** 仅物品、默认面访问、无 tick */
    public static ZhenFunction simpleItem(String baseName, Supplier<ElementType> element, int introducedAtLevel) {
        return new ZhenFunction(baseName, element, introducedAtLevel,
                () -> SlotPartition.of(ModIOTypes.ITEM.get(), ZhenTypes.defaultItemSlots()),
                null, null, null);
    }

    /** 仅物品 + tick */
    public static ZhenFunction simpleItemWithTick(String baseName, Supplier<ElementType> element,
            int introducedAtLevel, TickFactory tickFactory) {
        return new ZhenFunction(baseName, element, introducedAtLevel,
                () -> SlotPartition.of(ModIOTypes.ITEM.get(), ZhenTypes.defaultItemSlots()),
                null, tickFactory, null);
    }

    /** 物品+流体混合 + tick */
    public static ZhenFunction fluidItem(String baseName, Supplier<ElementType> element,
            int introducedAtLevel, TickFactory tickFactory, int tankCapacity) {
        return new ZhenFunction(baseName, element, introducedAtLevel,
                () -> SlotPartition.of(Map.of(
                    ModIOTypes.ITEM.get(), ZhenTypes.defaultItemSlots(),
                    ModIOTypes.FLUID.get(), Map.of(SlotZone.FLUID_OUTPUT_ALL, ZhenTypes.range(0, 0))
                )),
                null, tickFactory, tankCapacity);
    }
}

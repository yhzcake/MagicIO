package cn.yhzcake.magicio.block.zhen;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.utils.ElementType;
import cn.yhzcake.magicio.utils.ElementTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZhenTypes {
    public static final DeferredRegister<ZhenType> ZHEN_TYPES = DeferredRegister.create(ZhenType.ZHEN_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);

    public static Supplier<ZhenType> GRID_CELL;

    public static Set<Integer> range(int start, int end) {
        return IntStream.range(start, end + 1).boxed().collect(Collectors.toSet());
    }

    // ===== 共享的懒加载辅助方法（仅在 lambda 内部调用，此时注册表已就绪）=====

    /** 默认面访问：6 个方向全部暴露 ITEM 输入+输出 */
    static Map<Direction, Map<IOType, Set<String>>> defaultAccess() {
        var slots = Set.of(SlotZone.ITEM_INPUT_ALL.getName(), SlotZone.ITEM_OUTPUT_ALL.getName());
        return Map.of(
            Direction.UP, Map.of(ModIOTypes.ITEM.get(), slots),
            Direction.DOWN, Map.of(ModIOTypes.ITEM.get(), slots),
            Direction.EAST, Map.of(ModIOTypes.ITEM.get(), slots),
            Direction.WEST, Map.of(ModIOTypes.ITEM.get(), slots),
            Direction.NORTH, Map.of(ModIOTypes.ITEM.get(), slots),
            Direction.SOUTH, Map.of(ModIOTypes.ITEM.get(), slots)
        );
    }

    /** 默认物品槽位：槽 0 输入，槽 1 输出 */
    static Map<SlotZone, Set<Integer>> defaultItemSlots() {
        return Map.of(
            SlotZone.ITEM_INPUT_ALL, range(0, 0),
            SlotZone.ITEM_OUTPUT_ALL, range(1, 1)
        );
    }

    // ===== 便利注册方法 =====

    /** 最简：纯物品，默认槽位 + 默认面访问，无 tick */
    public static Supplier<ZhenType> registerSimple(String id, Supplier<ElementType> element, int level) {
        return ZHEN_TYPES.register(id, () -> new ZhenType(
                element.get(), "magic_io:" + id,
                SlotPartition.of(ModIOTypes.ITEM.get(), defaultItemSlots()),
                level, defaultAccess()));
    }

    // ===== 注册入口 =====

    public static void register(IEventBus eventBus) {
        UnstableZhenTypes.register(eventBus);
        StableZhenTypes.register(eventBus);
        SturdyZhenTypes.register(eventBus);
        AbundantZhenTypes.register(eventBus);
        ArchaicZhenTypes.register(eventBus);
        PrimevalZhenTypes.register(eventBus);

        // GridCell 面：0 个槽位的 ZhenType，由 GridCellSideProcessor 接管全部逻辑
        GRID_CELL = ZHEN_TYPES.register("grid_cell",
                () -> new ZhenType(ElementTypes.EARTH.get(), "magic_io:grid_cell",
                        SlotPartition.of(Map.of()), 0));

        ZHEN_TYPES.register(eventBus);
    }

    // ===== 查询 =====

    public static ZhenType getType(String name) {
        if (name == null || name.isEmpty()) return getFallback();
        if (ZhenType.ZHEN_TYPES == null) return getFallback();
        try {
            Identifier id = name.contains(":") ? Identifier.parse(name) : Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, name);
            return ZhenType.ZHEN_TYPES.get(id).map(Reference::value).orElse(getFallback());
        } catch (Exception e) {
            return getFallback();
        }
    }

    public static ZhenType getType(Identifier location) {
        if (location == null || ZhenType.ZHEN_TYPES == null) return getFallback();
        try {
            return ZhenType.ZHEN_TYPES.get(location).map(Reference::value).orElse(getFallback());
        } catch (Exception e) {
            return getFallback();
        }
    }

    public static ZhenType getType() { return getFallback(); }

    private static ZhenType getFallback() {
        if (UnstableZhenTypes.UNSTABLE_SIEVE_ZHEN != null) return UnstableZhenTypes.UNSTABLE_SIEVE_ZHEN.get();
        if (GRID_CELL != null) return GRID_CELL.get();
        return null;
    }
}

package cn.yhzcake.magicio.block.gridcell;

import java.util.function.Supplier;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.utils.ElementTypes;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册内置的 CellAction 条目。
 * 沿用了 {@code ModIOTypes} / {@code ElementTypes} 的注册模式。
 */
public class ModCellActions {

    public static final DeferredRegister<CellAction> CELL_ACTIONS =
            DeferredRegister.create(CellAction.CELL_ACTION_REGISTRY_KEY, MagicIO.MOD_ID);

    // 基础元素动作（调试用）
    public static Supplier<CellAction> EMPTY;
    public static Supplier<CellAction> FIRE;
    public static Supplier<CellAction> WATER;
    public static Supplier<CellAction> EARTH;
    public static Supplier<CellAction> WIND;

    public static void register(IEventBus eventBus) {
        EMPTY = CELL_ACTIONS.register("empty",() -> new CellAction(Items.AIR, null, 0));
        FIRE  = CELL_ACTIONS.register("fire",() -> new CellAction(Items.BLAZE_POWDER, () -> ElementTypes.FIRE.get(), 0));
        WATER = CELL_ACTIONS.register("water",() -> new CellAction(Items.WATER_BUCKET, () -> ElementTypes.WATER.get(), 0));
        EARTH = CELL_ACTIONS.register("earth",() -> new CellAction(Items.DIRT, () -> ElementTypes.EARTH.get(), 0));
        WIND  = CELL_ACTIONS.register("wind",() -> new CellAction(Items.FEATHER, () -> ElementTypes.WIND.get(), 0));
        CELL_ACTIONS.register(eventBus);
    }
}

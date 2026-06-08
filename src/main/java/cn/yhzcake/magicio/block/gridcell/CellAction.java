package cn.yhzcake.magicio.block.gridcell;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.utils.ElementType;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 单元格动作类型 — 自定义注册表。
 * 每个实例代表一种可以被"填入" GridCellPanel 单元格的动作类型，
 * 并通过 {@link #getItem()} 关联到触发该动作的副手物品。
 * 沿用了 {@code IOType} / {@code ElementType} 的注册表模式。
 */
public class CellAction {

    public static final ResourceKey<Registry<CellAction>> CELL_ACTION_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "cell_action"));
    public static Registry<CellAction> CELL_ACTIONS;

    private final Item item;
    private final ElementType elementType;
    private final int level;

    public CellAction(Item item, ElementType elementType, int level) {
        this.item = Objects.requireNonNull(item, "item is null");
        this.elementType = elementType;
        this.level = level;
    }

    public Item getItem() {
        return item;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CellAction that = (CellAction) o;
        return Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }

    @Override
    public String toString() {
        return item.toString();
    }

    /**
     * 根据物品查找对应的 CellAction。
     * 遍历当前注册表中所有 CellAction，返回第一个 item 匹配的实例。
     *
     * @param item 要查找的物品
     * @return 匹配的 CellAction，未找到返回 null
     */
    @Nullable
    public static CellAction getByItem(@Nullable Item item) {
        if (item == null || CELL_ACTIONS == null) return null;
        for (CellAction action : CELL_ACTIONS) {
            if (action.item == item) return action;
        }
        return null;
    }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        CELL_ACTIONS = new RegistryBuilder<>(CELL_ACTION_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "empty"))
                .create();
        event.register(CELL_ACTIONS);
    }
}

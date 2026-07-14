package cn.yhzcake.magicio.io;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.item.crafting.ItemRequirement;
import cn.yhzcake.magicio.item.crafting.OutputEntry;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * {@link ModIOTypes#ITEM} 的 {@link IOTypeDescriptor} 实现。
 * 封装物品消耗、产出、输出滚动、匹配和哈希的全部行为。
 */
@SuppressWarnings({"unchecked"})
public class ItemDescriptor implements IOTypeDescriptor {

    private static final int HASH_SEED = 31;

    @Override
    public IOType ioType() {
        return ModIOTypes.ITEM.get();
    }

    // ==================== 快照 ====================

    @Override
    public Object createSnapshot(IOComponent<?, ?> component) {
        ItemIOComponent itemComp = (ItemIOComponent) component;
        NonNullList<ItemStack> original = itemComp.getItems();
        NonNullList<ItemStack> copy = NonNullList.withSize(original.size(), ItemStack.EMPTY);
        for (int i = 0; i < original.size(); i++) {
            ItemStack stack = original.get(i);
            if (!stack.isEmpty()) {
                copy.set(i, stack.copy());
            }
        }
        return copy;
    }

    // ==================== 模拟消耗 ====================

    @Override
    public boolean simulateConsume(Object snapshot, SlotZone zone, Object requirement,
            SlotPartition partition) {
        NonNullList<ItemStack> simItems = (NonNullList<ItemStack>) snapshot;
        NonNullList<ItemRequirement> requirements = (NonNullList<ItemRequirement>) requirement;

        java.util.List<ItemRequirement> ordered = new java.util.ArrayList<>(requirements);
        ordered.sort(java.util.Comparator.comparingInt(itemRequirement ->
                countMatchingSlots(itemRequirement, simItems, zone, partition)));
        for (ItemRequirement itemRequirement : ordered) {
            int remaining = itemRequirement.count();
            for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
                ItemStack stack = simItems.get(slot);
                if (!stack.isEmpty() && itemRequirement.ingredient().test(stack)) {
                    int consumed = Math.min(remaining, stack.getCount());
                    stack.shrink(consumed);
                    remaining -= consumed;
                    if (stack.isEmpty()) {
                        simItems.set(slot, ItemStack.EMPTY);
                    }
                    if (remaining == 0) break;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static int countMatchingSlots(ItemRequirement requirement, NonNullList<ItemStack> items,
            SlotZone zone, SlotPartition partition) {
        int count = 0;
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (!items.get(slot).isEmpty() && requirement.ingredient().test(items.get(slot))) count++;
        }
        return count;
    }

    // ==================== 模拟产出 ====================

    @Override
    public boolean simulateProduce(Object snapshot, SlotZone zone, Object value,
            SlotPartition partition) {
        NonNullList<ItemStack> simItems = (NonNullList<ItemStack>) snapshot;
        NonNullList<ItemStack> outputs = (NonNullList<ItemStack>) value;

        for (ItemStack stack : outputs) {
            if (!stack.isEmpty()) {
                ItemStack remaining = insertItem(simItems, zone, stack.copy(), partition);
                if (!remaining.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 纯物品插入算法：不触发通知、不修改组件状态。返回未能插入的部分。 */
    static ItemStack insertItem(NonNullList<ItemStack> storage, SlotZone zone,
            ItemStack stack, SlotPartition partition) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();

        // 堆叠到已有同类物品上
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (remaining.isEmpty()) break;
            ItemStack existing = storage.get(slot);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int canInsert = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canInsert > 0) {
                    existing.grow(canInsert);
                    remaining.shrink(canInsert);
                }
            }
        }

        // 填入空槽（按最大堆叠数拆分，避免产出倍率造成超堆叠）
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (remaining.isEmpty()) break;
            ItemStack existing = storage.get(slot);
            if (existing.isEmpty()) {
                int toInsert = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                storage.set(slot, remaining.split(toInsert));
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    // ==================== 实际消耗 ====================

    @Override
    public void commitConsume(IOComponent<?, ?> component, SlotZone zone, Object requirement,
            SlotPartition partition) {
        NonNullList<ItemRequirement> requirements = (NonNullList<ItemRequirement>) requirement;
        ItemIOComponent itemComp = (ItemIOComponent) component;
        if (!itemComp.consumeItem(zone, requirements)) {
            throw new IllegalStateException("Validated item transaction failed during commit");
        }
    }

    // ==================== 实际产出 ====================

    @Override
    public void commitProduce(IOComponent<?, ?> component, SlotZone zone, Object value,
            SlotPartition partition) {
        NonNullList<ItemStack> outputs = (NonNullList<ItemStack>) value;
        ItemIOComponent itemComp = (ItemIOComponent) component;
        itemComp.produceItem(zone, outputs);
    }

    // ==================== 产出滚动 ====================

    @Override
    public NonNullList<?> rollOutputs(Object specification, ServerLevel level) {
        NonNullList<OutputEntry> entries = (NonNullList<OutputEntry>) specification;
        NonNullList<ItemStack> rolled = NonNullList.create();
        for (OutputEntry entry : entries) {
            rolled.addAll(entry.roll(level));
        }
        return rolled;
    }

    // ==================== 输入匹配 ====================

    @Override
    public boolean matches(IOComponent<?, ?> component, SlotZone zone, Object requirement,
            SlotPartition partition) {
        return simulateConsume(createSnapshot(component), zone, requirement, partition);
    }

    // ==================== 输入哈希 ====================

    @Override
    public int computeHash(IOComponent<?, ?> component, SlotPartition partition) {
        NonNullList<ItemStack> items = ((ItemIOComponent) component).getItems();
        int hash = 1;
        for (int slot : partition.getAllSlots(ModIOTypes.ITEM.get())) {
            ItemStack stack = items.get(slot);
            hash = HASH_SEED * hash + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            hash = HASH_SEED * hash + (stack.isEmpty() ? 0 : stack.getCount());
            hash = HASH_SEED * hash + (stack.isEmpty() ? 0 : stack.getComponents().hashCode());
        }
        return hash;
    }
}

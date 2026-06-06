package cn.yhzcake.magicio.io;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 直接链接到 NonNullList&lt;ItemStack&gt; 的 ResourceHandler。
 * 与 ItemStacksResourceHandler 不同，本类不会在构造时复制列表。
 * <p>
 * 通过 {@code insertSlots} / {@code extractSlots} 分别控制允许插入/提取的槽位，
 * 外部管道只能按权限操作面暴露的槽位：
 * <ul>
 *   <li>{@code insert()} 只允许插入到 insertSlots 中的槽位</li>
 *   <li>{@code extract()} 只允许从 extractSlots 中的槽位提取</li>
 *   <li>满载时容量归零，拒绝插入</li>
 * </ul>
 */
public class LinkedItemHandler implements ResourceHandler<ItemResource> {

    private static final int MAX_STACK = 99;
    private final NonNullList<ItemStack> items;
    private final int[] allSlots;
    private final boolean[] canInsert;
    private final boolean[] canExtract;
    private Runnable onChange = () -> {};

    public LinkedItemHandler(NonNullList<ItemStack> items, Set<Integer> insertSlots, Set<Integer> extractSlots) {
        this.items = items;
        TreeSet<Integer> combined = new TreeSet<>();
        combined.addAll(insertSlots);
        combined.addAll(extractSlots);
        this.allSlots = combined.stream().mapToInt(Integer::intValue).toArray();
        this.canInsert = new boolean[allSlots.length];
        this.canExtract = new boolean[allSlots.length];
        for (int i = 0; i < allSlots.length; i++) {
            int real = allSlots[i];
            canInsert[i] = insertSlots.contains(real);
            canExtract[i] = extractSlots.contains(real);
        }
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private int realSlot(int handlerSlot) {
        return allSlots[handlerSlot];
    }

    @Override
    public int size() {
        return allSlots.length;
    }

    @Override
    public ItemResource getResource(int slot) {
        Objects.checkIndex(slot, size());
        ItemStack stack = items.get(realSlot(slot));
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int slot) {
        Objects.checkIndex(slot, size());
        return items.get(realSlot(slot)).getCount();
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        Objects.checkIndex(slot, size());
        if (!canInsert[slot]) return 0;
        if (resource == null || resource.isEmpty()) return 0;
        return Math.min(resource.getMaxStackSize(), MAX_STACK);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext ctx) {
        Objects.checkIndex(slot, size());
        if (!canInsert[slot]) return 0;
        if (amount <= 0 || resource == null || resource.isEmpty()) return 0;

        int idx = realSlot(slot);
        ItemStack existing = items.get(idx);
        if (!existing.isEmpty() && !resource.matches(existing)) return 0;

        long cap = getCapacityAsLong(slot, resource);
        int current = existing.getCount();
        int insertable = (int) Math.min(amount, cap - current);
        if (insertable <= 0) return 0;

        if (existing.isEmpty()) {
            items.set(idx, resource.toStack(insertable));
        } else {
            existing.grow(insertable);
        }
        onChange.run();
        return insertable;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext ctx) {
        Objects.checkIndex(slot, size());
        if (!canExtract[slot]) return 0;
        if (amount <= 0 || resource == null || resource.isEmpty()) return 0;

        int idx = realSlot(slot);
        ItemStack existing = items.get(idx);
        if (existing.isEmpty() || !resource.matches(existing)) return 0;

        int extractable = Math.min(amount, existing.getCount());
        if (extractable <= 0) return 0;

        existing.shrink(extractable);
        if (existing.isEmpty()) items.set(idx, ItemStack.EMPTY);
        onChange.run();
        return extractable;
    }
}

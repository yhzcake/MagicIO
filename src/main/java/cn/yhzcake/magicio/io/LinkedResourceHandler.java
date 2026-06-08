package cn.yhzcake.magicio.io;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 抽象基类：直接链接到 {@link NonNullList} 的 {@link ResourceHandler}。
 * <p>
 * 与 {@code StacksResourceHandler} 不同，本类不会在构造时复制列表。
 * 通过 {@code insertSlots} / {@code extractSlots} 分别控制允许插入/提取的槽位。
 *
 * @param <S> 栈/罐存储类型（ItemStack / FluidStack）
 * @param <T> 资源类型（ItemResource / FluidResource）
 */
public abstract class LinkedResourceHandler<S, T extends Resource> implements ResourceHandler<T> {

    protected final NonNullList<S> stacks;
    protected final int[] allSlots;
    protected final boolean[] canInsert;
    protected final boolean[] canExtract;
    protected Runnable onChange = () -> {};

    protected LinkedResourceHandler(NonNullList<S> stacks, Set<Integer> insertSlots, Set<Integer> extractSlots) {
        this.stacks = stacks;
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

    protected abstract boolean isEmpty(S stack);

    protected abstract int getAmount(S stack);

    protected abstract S setAmount(S stack, int amount);

    protected abstract T getResource(S stack);

    protected abstract S toStack(T resource, int amount);

    protected abstract boolean matches(S stack, T resource);

    protected abstract long capacity(int slot, T resource);

    protected abstract S emptyInstance();

    private int realSlot(int handlerSlot) {
        return allSlots[handlerSlot];
    }

    @Override
    public int size() {
        return allSlots.length;
    }

    @Override
    public T getResource(int slot) {
        Objects.checkIndex(slot, size());
        S stack = stacks.get(realSlot(slot));
        return getResource(stack);
    }

    @Override
    public long getAmountAsLong(int slot) {
        Objects.checkIndex(slot, size());
        return getAmount(stacks.get(realSlot(slot)));
    }

    @Override
    public long getCapacityAsLong(int slot, T resource) {
        Objects.checkIndex(slot, size());
        if (!canInsert[slot]) return 0;
        if (resource == null) return 0;
        if (!resource.isEmpty() && !isValid(slot, resource)) return 0;
        return capacity(slot, resource);
    }

    @Override
    public boolean isValid(int slot, T resource) {
        return true;
    }

    @Override
    public int insert(int slot, T resource, int amount, TransactionContext ctx) {
        Objects.checkIndex(slot, size());
        if (!canInsert[slot]) return 0;
        if (amount <= 0 || resource == null || resource.isEmpty()) return 0;

        int idx = realSlot(slot);
        S existing = stacks.get(idx);
        if (!isEmpty(existing) && !matches(existing, resource)) return 0;

        long cap = getCapacityAsLong(slot, resource);
        int current = getAmount(existing);
        int insertable = (int) Math.min(amount, cap - current);
        if (insertable <= 0) return 0;

        if (isEmpty(existing)) {
            stacks.set(idx, toStack(resource, insertable));
        } else {
            stacks.set(idx, setAmount(existing, current + insertable));
        }
        onChange.run();
        return insertable;
    }

    @Override
    public int extract(int slot, T resource, int amount, TransactionContext ctx) {
        Objects.checkIndex(slot, size());
        if (!canExtract[slot]) return 0;
        if (amount <= 0 || resource == null || resource.isEmpty()) return 0;

        int idx = realSlot(slot);
        S existing = stacks.get(idx);
        if (isEmpty(existing) || !matches(existing, resource)) return 0;

        int extractable = Math.min(amount, getAmount(existing));
        if (extractable <= 0) return 0;

        int remaining = getAmount(existing) - extractable;
        stacks.set(idx, remaining > 0 ? setAmount(existing, remaining) : emptyInstance());
        return extractable;
    }
}

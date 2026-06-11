package cn.yhzcake.magicio.io;

import java.util.Set;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 直接链接到 {@link NonNullList}&lt;{@link ItemStack}&gt; 的 {@link ResourceHandler}。
 */
public class LinkedItemHandler extends LinkedResourceHandler<ItemStack, ItemResource> {

    private static final int MAX_STACK = 99;

    public LinkedItemHandler(NonNullList<ItemStack> items, Set<Integer> insertSlots, Set<Integer> extractSlots) {
        super(items, insertSlots, extractSlots);
    }

    @Override
    protected boolean isEmpty(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    protected int getAmount(ItemStack stack) {
        return stack.getCount();
    }

    @Override
    protected ItemStack setAmount(ItemStack stack, int amount) {
        ItemStack copy = stack.copy();
        copy.setCount(amount);
        return copy;
    }

    @Override
    protected ItemResource getResource(ItemStack stack) {
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    protected ItemStack toStack(ItemResource resource, int amount) {
        return resource.toStack(amount);
    }

    @Override
    protected boolean matches(ItemStack stack, ItemResource resource) {
        return resource.matches(stack);
    }

    @Override
    protected long capacity(int slot, ItemResource resource) {
        return Math.min(resource.getMaxStackSize(), MAX_STACK);
    }

    @Override
    protected ItemStack emptyInstance() {
        return ItemStack.EMPTY;
    }
}

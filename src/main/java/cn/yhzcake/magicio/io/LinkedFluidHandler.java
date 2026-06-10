package cn.yhzcake.magicio.io;

import java.util.Set;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * 直接链接到 {@link NonNullList}&lt;{@link FluidStack}&gt; 的 {@link ResourceHandler}。
 */
public class LinkedFluidHandler extends LinkedResourceHandler<FluidStack, FluidResource> {

    private final int tankCapacity;

    public LinkedFluidHandler(NonNullList<FluidStack> tanks, int tankCapacity,
                               Set<Integer> insertSlots, Set<Integer> extractSlots) {
        super(tanks, insertSlots, extractSlots);
        this.tankCapacity = tankCapacity;
    }

    @Override
    protected boolean isEmpty(FluidStack stack) {
        return stack.isEmpty();
    }

    @Override
    protected int getAmount(FluidStack stack) {
        return stack.getAmount();
    }

    @Override
    protected FluidStack setAmount(FluidStack stack, int amount) {
        FluidStack copy = stack.copy();
        copy.setAmount(amount);
        return copy;
    }

    @Override
    protected FluidResource getResource(FluidStack stack) {
        return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack);
    }

    @Override
    protected FluidStack toStack(FluidResource resource, int amount) {
        return resource.toStack(amount);
    }

    @Override
    protected boolean matches(FluidStack stack, FluidResource resource) {
        return resource.matches(stack);
    }

    @Override
    protected long capacity(int slot, FluidResource resource) {
        return tankCapacity;
    }

    @Override
    protected FluidStack emptyInstance() {
        return FluidStack.EMPTY;
    }

    @Override
    protected FluidStack copyStack(FluidStack stack) {
        return stack.copy();
    }
}

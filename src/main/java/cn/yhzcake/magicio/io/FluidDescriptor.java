package cn.yhzcake.magicio.io;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.item.crafting.FluidRequirement;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * {@link ModIOTypes#FLUID} 的 {@link IOTypeDescriptor} 实现。
 * 封装流体消耗、产出、输出滚动、匹配和哈希的全部行为。
 */
@SuppressWarnings({"unchecked"})
public class FluidDescriptor implements IOTypeDescriptor {

    private static final int HASH_SEED = 31;

    private record FluidSnapshot(NonNullList<FluidStack> tanks, int tankCapacity) {}

    @Override
    public IOType ioType() {
        return ModIOTypes.FLUID.get();
    }

    // ==================== 快照 ====================

    @Override
    public Object createSnapshot(IOComponent<?, ?> component) {
        FluidIOComponent fluidComp = (FluidIOComponent) component;
        NonNullList<FluidStack> original = fluidComp.getTanks();
        NonNullList<FluidStack> copy = NonNullList.withSize(original.size(), FluidStack.EMPTY);
        for (int i = 0; i < original.size(); i++) {
            FluidStack fluid = original.get(i);
            if (!fluid.isEmpty()) {
                copy.set(i, fluid.copy());
            }
        }
        Integer capacity = fluidComp.getTankCapacity();
        return new FluidSnapshot(copy, capacity == null ? 0 : capacity);
    }

    // ==================== 模拟消耗 ====================

    @Override
    public boolean simulateConsume(Object snapshot, SlotZone zone, Object requirement,
            SlotPartition partition) {
        NonNullList<FluidStack> simTanks = ((FluidSnapshot) snapshot).tanks();
        NonNullList<FluidRequirement> ingredients =
                (NonNullList<FluidRequirement>) requirement;

        for (FluidRequirement ingredient : ingredients) {
            int remaining = ingredient.amount();
            for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
                FluidStack existing = simTanks.get(tank);
                if (ingredient.matchesType(existing)) {
                    int drained = Math.min(remaining, existing.getAmount());
                    existing.shrink(drained);
                    remaining -= drained;
                    if (existing.isEmpty()) {
                        simTanks.set(tank, FluidStack.EMPTY);
                    }
                    if (remaining == 0) break;
                }
            }
            if (remaining != 0) return false;
        }
        return true;
    }

    // ==================== 模拟产出 ====================

    @Override
    public boolean simulateProduce(Object snapshot, SlotZone zone, Object value,
            SlotPartition partition) {
        FluidSnapshot fluidSnapshot = (FluidSnapshot) snapshot;
        NonNullList<FluidStack> simTanks = fluidSnapshot.tanks();
        NonNullList<FluidStack> fluids = (NonNullList<FluidStack>) value;

        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                int filled = insertFluid(simTanks, zone, fluid.copy(), partition, fluidSnapshot.tankCapacity());
                if (filled < fluid.getAmount()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 纯流体插入算法：不触发通知、不修改组件状态。返回实际填充量。 */
    static int insertFluid(NonNullList<FluidStack> storage, SlotZone zone,
            FluidStack toFill, SlotPartition partition, int tankCapacity) {
        if (toFill.isEmpty() || tankCapacity <= 0) return 0;
        int filled = 0;

        for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
            if (toFill.isEmpty()) break;
            FluidStack existing = storage.get(tank);
            if (existing.isEmpty()) {
                int canInsert = Math.min(toFill.getAmount(), tankCapacity);
                storage.set(tank, toFill.copyWithAmount(canInsert));
                filled += canInsert;
                toFill.shrink(canInsert);
            } else if (isSameFluidAndComponents(existing, toFill)) {
                int canInsert = Math.min(toFill.getAmount(), tankCapacity - existing.getAmount());
                if (canInsert > 0) {
                    existing.grow(canInsert);
                    filled += canInsert;
                    toFill.shrink(canInsert);
                }
            }
        }
        return filled;
    }

    // ==================== 实际消耗 ====================

    @Override
    public void commitConsume(IOComponent<?, ?> component, SlotZone zone, Object requirement,
            SlotPartition partition) {
        NonNullList<FluidRequirement> ingredients =
                (NonNullList<FluidRequirement>) requirement;
        FluidIOComponent fluidComp = (FluidIOComponent) component;
        NonNullList<FluidStack> tanks = fluidComp.getTanks();

        for (FluidRequirement ingredient : ingredients) {
            int remaining = ingredient.amount();
            for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
                FluidStack existing = tanks.get(tank);
                if (ingredient.matchesType(existing)) {
                    int drained = Math.min(remaining, existing.getAmount());
                    existing.shrink(drained);
                    remaining -= drained;
                    if (existing.isEmpty()) {
                        tanks.set(tank, FluidStack.EMPTY);
                    }
                    if (remaining == 0) break;
                }
            }
            if (remaining != 0) {
                throw new IllegalStateException("Validated fluid transaction failed during commit");
            }
        }
        fluidComp.notifyChanged();
    }

    // ==================== 实际产出 ====================

    @Override
    public void commitProduce(IOComponent<?, ?> component, SlotZone zone, Object value,
            SlotPartition partition) {
        NonNullList<FluidStack> fluids = (NonNullList<FluidStack>) value;
        FluidIOComponent fluidComp = (FluidIOComponent) component;
        fluidComp.produceFluid(zone, fluids);
    }

    // ==================== 产出滚动 ====================

    @Override
    public NonNullList<?> rollOutputs(Object specification, ServerLevel level) {
        NonNullList<FluidStack> entries = (NonNullList<FluidStack>) specification;
        NonNullList<FluidStack> rolled = NonNullList.create();
        for (FluidStack fluid : entries) {
            if (!fluid.isEmpty()) {
                rolled.add(fluid.copy());
            }
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
        NonNullList<FluidStack> tanks = ((FluidIOComponent) component).getTanks();
        int hash = 1;
        for (int slot : partition.getAllSlots(ModIOTypes.FLUID.get())) {
            FluidStack fluid = tanks.get(slot);
            hash = HASH_SEED * hash + (fluid.isEmpty() ? 0 : System.identityHashCode(fluid.getFluid()));
            hash = HASH_SEED * hash + (fluid.isEmpty() ? 0 : fluid.getAmount());
            hash = HASH_SEED * hash + (fluid.isEmpty() ? 0 : fluid.getComponents().hashCode());
        }
        return hash;
    }

    static boolean isSameFluidAndComponents(FluidStack first, FluidStack second) {
        return FluidStack.isSameFluid(first, second)
                && first.getComponents().equals(second.getComponents());
    }
}

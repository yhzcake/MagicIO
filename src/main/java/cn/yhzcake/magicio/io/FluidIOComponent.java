package cn.yhzcake.magicio.io;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidIOComponent implements IOComponent<Object, FluidStack> {
    private final NonNullList<FluidStack> tanks;
    private final SlotPartition partition;
    private final @Nullable Integer tankCapacity;
    private Runnable onChange = () -> {};

    public FluidIOComponent(NonNullList<FluidStack> tanks, SlotPartition partition, @Nullable Integer tankCapacity) {
        this.tanks = tanks;
        this.partition = partition;
        this.tankCapacity = tankCapacity;
    }

    @Override
    public IOType type() {
        return ModIOTypes.FLUID.get();
    }

    public NonNullList<FluidStack> getTanks() {
        return tanks;
    }

    public @Nullable Integer getTankCapacity() {
        return tankCapacity;
    }

    @Override
    public int slotCount() {
        return partition.getTotalSlots(ModIOTypes.FLUID.get());
    }

    @Override
    public void setChangeCallback(Runnable onChanged) {
        this.onChange = onChanged;
    }

    public void notifyChanged() {
        if (onChange != null) {
            onChange.run();
        }
    }

    // ===== 查询 =====

    @Override
    public boolean canSupply(int slot, Object requirement) {
        return !tanks.get(slot).isEmpty();
    }

    @Override
    public boolean hasSupply(Object requirement) {
        for (int tank : partition.getAllSlots(ModIOTypes.FLUID.get())) {
            if (!tanks.get(tank).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canFit(int slot, FluidStack value) {
        if (value.isEmpty() || tankCapacity == null) return false;
        FluidStack existing = tanks.get(slot);
        if (existing.isEmpty()) return tankCapacity >= value.getAmount();
        if (FluidDescriptor.isSameFluidAndComponents(existing, value)) {
            return existing.getAmount() + value.getAmount() <= tankCapacity;
        }
        return false;
    }

    public boolean canFit(SlotZone zone, NonNullList<FluidStack> fluids) {
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty() && insertFluid(zone, fluid.copy(), true) < fluid.getAmount()) {
                return false;
            }
        }
        return true;
    }

    // ===== 提取 =====

    @Override
    public FluidStack extract(int slot, int amount, boolean simulate) {
        if (amount <= 0) return FluidStack.EMPTY;
        FluidStack existing = tanks.get(slot);
        if (existing.isEmpty()) return FluidStack.EMPTY;
        int drained = Math.min(amount, existing.getAmount());
        FluidStack result = existing.copyWithAmount(drained);
        if (!simulate) {
            existing.shrink(drained);
            if (existing.isEmpty()) {
                tanks.set(slot, FluidStack.EMPTY);
            }
            notifyChanged();
        }
        return result;
    }

    public FluidStack extractFluid(SlotZone zone, int tank, int amount, boolean simulate) {
        if (!partition.getSlots(ModIOTypes.FLUID.get(), zone).contains(tank) || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return extract(tank, amount, simulate);
    }

    public FluidStack extractFluid(SlotZone zone, int amount, boolean simulate) {
        if (amount <= 0) return FluidStack.EMPTY;
        for (int tank : partition.getSlots(ModIOTypes.FLUID.get(), zone)) {
            if (!tanks.get(tank).isEmpty()) {
                return extractFluid(zone, tank, Math.min(amount, tanks.get(tank).getAmount()), simulate);
            }
        }
        return FluidStack.EMPTY;
    }

    // ===== 插入 =====

    @Override
    public FluidStack insert(int slot, FluidStack value, boolean simulate) {
        if (value.isEmpty()) return FluidStack.EMPTY;
        if (tankCapacity == null) return value.copy();
        FluidStack existing = tanks.get(slot);
        if (existing.isEmpty()) {
            int canInsert = Math.min(value.getAmount(), tankCapacity);
            if (!simulate) {
                tanks.set(slot, value.copyWithAmount(canInsert));
                notifyChanged();
            }
            FluidStack result = value.copy();
            result.shrink(canInsert);
            return result.isEmpty() ? FluidStack.EMPTY : result;
        }
        if (FluidDescriptor.isSameFluidAndComponents(existing, value)) {
            int canInsert = Math.min(value.getAmount(), tankCapacity - existing.getAmount());
            if (canInsert > 0) {
                if (!simulate) {
                    existing.grow(canInsert);
                    notifyChanged();
                }
                FluidStack result = value.copy();
                result.shrink(canInsert);
                return result.isEmpty() ? FluidStack.EMPTY : result;
            }
        }
        return value.copy();
    }

    @Override
    public FluidStack insert(FluidStack value, boolean simulate) {
        if (value.isEmpty()) return FluidStack.EMPTY;
        FluidStack remaining = value.copy();
        for (int tank : partition.getAllSlots(ModIOTypes.FLUID.get())) {
            if (remaining.isEmpty()) break;
            remaining = insert(tank, remaining, simulate);
        }
        return remaining;
    }

    public int insertFluid(SlotZone zone, FluidStack fluid, boolean simulate) {
        if (fluid.isEmpty() || tankCapacity == null) return 0;

        if (simulate) {
            NonNullList<FluidStack> copy = NonNullList.withSize(tanks.size(), FluidStack.EMPTY);
            for (int i = 0; i < tanks.size(); i++) {
                if (!tanks.get(i).isEmpty()) copy.set(i, tanks.get(i).copy());
            }
            return FluidDescriptor.insertFluid(copy, zone, fluid.copy(), partition, tankCapacity);
        }

        int filled = FluidDescriptor.insertFluid(tanks, zone, fluid.copy(), partition, tankCapacity);
        if (filled > 0) notifyChanged();
        return filled;
    }

    // ===== 产出 =====

    @Override
    public void produce(int slot, FluidStack value) {
        if (value.isEmpty()) return;
        insert(slot, value, false);
    }

    @Override
    public void produce(FluidStack value) {
        if (value.isEmpty()) return;
        insert(value, false);
    }

    public void produceFluid(SlotZone zone, NonNullList<FluidStack> fluids) {
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                insertFluid(zone, fluid.copy(), false);
            }
        }
    }

    // ===== NBT 持久化 =====

    @Override
    public void saveNBT(ValueOutput output) {
        for (int i = 0; i < tanks.size(); i++) {
            if (!tanks.get(i).isEmpty()) {
                output.store("FluidTank_" + i, FluidStack.CODEC, tanks.get(i));
            }
        }
    }

    @Override
    public void loadNBT(ValueInput input) {
        for (int i = 0; i < tanks.size(); i++) {
            tanks.set(i, input.read("FluidTank_" + i, FluidStack.CODEC).orElse(FluidStack.EMPTY));
        }
    }
}

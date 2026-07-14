package cn.yhzcake.magicio.io;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

public class EnergyIOComponent implements IOComponent<Object, Integer> {

    private final int slot;
    private final SimpleEnergyHandler handler;
    private Runnable onChanged = () -> {};

    public EnergyIOComponent(int capacity, int slot) {
        this.slot = slot;
        this.handler = new SimpleEnergyHandler(capacity, capacity, capacity, 0) {
            @Override
            protected void onEnergyChanged(int previousAmount) {
                notifyChanged();
            }
        };
    }

    public EnergyHandler getHandler() {
        return handler;
    }

    public int getCapacity() {
        return handler.getCapacityAsInt();
    }

    public int getEnergy() {
        return handler.getAmountAsInt();
    }

    @Override
    public IOType type() {
        return ModIOTypes.ENERGY.get();
    }

    @Override
    public boolean canSupply(int slot, Object requirement) {
        return handler.getAmountAsInt() > 0;
    }

    @Override
    public boolean canFit(int slot, Integer value) {
        return handler.getAmountAsInt() + value <= handler.getCapacityAsInt();
    }

    @Override
    public Integer extract(int slot, int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int extracted = Math.min(amount, handler.getAmountAsInt());
        if (!simulate) {
            handler.set(handler.getAmountAsInt() - extracted);
        }
        return extracted;
    }

    @Override
    public void produce(int slot, Integer value) {
        if (value <= 0) return;
        handler.set(handler.getAmountAsInt() + value);
    }

    @Override
    public void saveNBT(ValueOutput output) {
        handler.serialize(output);
    }

    @Override
    public void loadNBT(ValueInput input) {
        handler.deserialize(input);
    }

    @Override
    public int slotCount() {
        return slot;
    }

    @Override
    public boolean hasSupply(Object requirement) {
        return handler.getAmountAsInt() > 0;
    }

    @Override
    public Integer insert(int slot, Integer value, boolean simulate) {
        if (slot != 0) return 0;
        return insert(value, simulate);
    }

    @Override
    public Integer insert(Integer value, boolean simulate) {
        if (value <= 0) return 0;
        int capacity = handler.getCapacityAsInt();
        int current = handler.getAmountAsInt();
        int inserted = Math.min(value, capacity - current);
        if (!simulate) {
            handler.set(current + inserted);
        }
        return value - inserted;
    }

    @Override
    public void produce(Integer value) {
        if (value <= 0) return;
        handler.set(handler.getAmountAsInt() + value);
    }

    @Override
    public void setChangeCallback(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private void notifyChanged() {
        onChanged.run();
    }
}

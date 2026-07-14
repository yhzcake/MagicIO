package cn.yhzcake.magicio.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.zhenbus.PortBinding;
import cn.yhzcake.magicio.block.zhenbus.VirtualPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

final class SidePortManager {

    private static final int PORT_SCAN_INTERVAL_IDLE = 200;

    private final Level level;
    private final BlockPos pos;
    private final IOProcessor ioProcessor;
    private final NonNullList<ItemStack> items;
    private final NonNullList<FluidStack> tanks;
    private final Set<Integer> outputItemSlots;
    private final Set<Integer> fluidOutputSlots;
    private final Set<Integer> energyOutputSlots;
    private final @Nullable Integer energyCapacity;
    private final Map<String, VirtualPort> virtualPorts = new HashMap<>();
    private boolean hasEverConnectedPort;
    private int portScanTimer;

    SidePortManager(Level level, BlockPos pos, Direction side, IOProcessor ioProcessor,
            NonNullList<ItemStack> items, NonNullList<FluidStack> tanks,
            Set<Integer> outputItemSlots, Set<Integer> fluidOutputSlots, Set<Integer> energyOutputSlots,
            @Nullable Integer energyCapacity) {
        this.level = level;
        this.pos = pos;
        this.ioProcessor = ioProcessor;
        this.items = items;
        this.tanks = tanks;
        this.outputItemSlots = outputItemSlots;
        this.fluidOutputSlots = fluidOutputSlots;
        this.energyOutputSlots = energyOutputSlots;
        this.energyCapacity = energyCapacity;
        virtualPorts.put("self", new VirtualPort("self", List.of(new PortBinding(side, side.getOpposite()))));
        for (Direction direction : Direction.values()) {
            virtualPorts.put(direction.getName(), new VirtualPort(direction.getName(), List.of(
                    new PortBinding(direction, direction.getOpposite()))));
        }
    }

    void scanAll() {
        long currentTick = level.getGameTime();
        for (VirtualPort port : virtualPorts.values()) {
            port.forceScan(level, pos, currentTick);
        }
        updateConnectionState();
    }

    void invalidate() {
        for (VirtualPort port : virtualPorts.values()) {
            port.invalidate();
        }
    }

    void tickRefresh() {
        long currentTick = level.getGameTime();
        if (hasEverConnectedPort) {
            for (VirtualPort port : virtualPorts.values()) {
                port.tickRefresh(level, pos, currentTick);
            }
            return;
        }
        if (portScanTimer++ >= PORT_SCAN_INTERVAL_IDLE) {
            portScanTimer = 0;
            for (VirtualPort port : virtualPorts.values()) {
                port.tickRefresh(level, pos, currentTick);
            }
            updateConnectionState();
        }
    }

    boolean hasConnectedPort() {
        return hasEverConnectedPort;
    }

    boolean hasPushableOutput() {
        for (int slot : outputItemSlots) {
            if (!items.get(slot).isEmpty()) return true;
        }
        for (int slot : fluidOutputSlots) {
            if (!tanks.get(slot).isEmpty()) return true;
        }
        if (!energyOutputSlots.contains(0) || energyCapacity == null) return false;
        IOComponent<?, ?> component = ioProcessor.get(ModIOTypes.ENERGY.get());
        return component instanceof EnergyIOComponent energyComponent && energyComponent.getEnergy() > 0;
    }

    @Nullable VirtualPort getPort(String name) {
        return virtualPorts.get(name);
    }

    Map<String, VirtualPort> getPorts() {
        return Collections.unmodifiableMap(virtualPorts);
    }

    void pushOutputs(Runnable onChanged) {
        boolean changed = false;
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            IOType type = component.type();
            if (type == ModIOTypes.ENERGY.get()) {
                changed = pushEnergyOutput() || changed;
            } else if (type == ModIOTypes.FLUID.get()) {
                changed = pushOutput(fluidOutputSlots, tanks, type) || changed;
            } else {
                changed = pushOutput(outputItemSlots, items, type) || changed;
            }
        }
        if (changed && onChanged != null) {
            onChanged.run();
        }
    }

    private void updateConnectionState() {
        hasEverConnectedPort = false;
        for (VirtualPort port : virtualPorts.values()) {
            if (port.hasDirectConnection()) {
                hasEverConnectedPort = true;
                return;
            }
        }
    }

    private <T> boolean pushOutput(Set<Integer> outputSlots, NonNullList<T> storage, IOType type) {
        boolean changed = false;
        for (int slot : outputSlots) {
            T value = storage.get(slot);
            if (isSlotEmpty(value)) continue;
            for (VirtualPort port : virtualPorts.values()) {
                if (!port.hasDirectConnection()) continue;
                T remaining = port.transfer(value, type, false);
                if (!isSlotSameAmount(remaining, value)) {
                    storage.set(slot, remaining);
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean isSlotEmpty(Object value) {
        if (value instanceof ItemStack itemStack) return itemStack.isEmpty();
        if (value instanceof FluidStack fluidStack) return fluidStack.isEmpty();
        return false;
    }

    private static boolean isSlotSameAmount(Object first, Object second) {
        if (first instanceof ItemStack firstItem && second instanceof ItemStack secondItem) {
            return firstItem.getCount() == secondItem.getCount();
        }
        if (first instanceof FluidStack firstFluid && second instanceof FluidStack secondFluid) {
            return firstFluid.getAmount() == secondFluid.getAmount();
        }
        return true;
    }

    @SuppressWarnings({"rawtypes"})
    private boolean pushEnergyOutput() {
        if (energyCapacity == null || !energyOutputSlots.contains(0)) return false;
        IOComponent component = ioProcessor.get(ModIOTypes.ENERGY.get());
        if (!(component instanceof EnergyIOComponent energyComponent)) return false;
        if (energyComponent.getEnergy() <= 0) return false;
        for (VirtualPort port : virtualPorts.values()) {
            if (!port.hasDirectConnection() || !port.canAccept(ModIOTypes.ENERGY.get())) continue;
            int available = energyComponent.getEnergy();
            Integer remaining = port.transfer(available, ModIOTypes.ENERGY.get(), false);
            if (remaining < available) {
                energyComponent.extract(0, available - remaining, false);
                return true;
            }
        }
        return false;
    }
}

package cn.yhzcake.magicio.io;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;

/**
 * {@link ModIOTypes#ENERGY} 的 {@link IOTypeDescriptor} 实现。
 * 封装能量消耗、产出、匹配和哈希的全部行为。
 * <p>
 * 能量使用全局槽位 0 进行所有标识，不按 Zone 区分输入输出区。
 */
public class EnergyDescriptor implements IOTypeDescriptor {

    private static final int ENERGY_SLOT = 0;

    private static final class EnergySnapshot {
        private int energy;
        private final int capacity;

        private EnergySnapshot(int energy, int capacity) {
            this.energy = energy;
            this.capacity = capacity;
        }
    }

    @Override
    public IOType ioType() {
        return ModIOTypes.ENERGY.get();
    }

    // ==================== 快照 ====================

    @Override
    public Object createSnapshot(IOComponent<?, ?> component) {
        EnergyIOComponent energyComp = (EnergyIOComponent) component;
        return new EnergySnapshot(energyComp.getEnergy(), energyComp.getCapacity());
    }

    // ==================== 模拟消耗 ====================

    @Override
    public boolean simulateConsume(Object snapshot, SlotZone zone, Object requirement,
            SlotPartition partition) {
        EnergySnapshot energySnapshot = (EnergySnapshot) snapshot;
        int required = requirement instanceof Integer i ? i : 0;
        if (required < 0 || energySnapshot.energy < required) return false;
        energySnapshot.energy -= required;
        return true;
    }

    // ==================== 模拟产出 ====================

    @Override
    public boolean simulateProduce(Object snapshot, SlotZone zone, Object value,
            SlotPartition partition) {
        @SuppressWarnings("unchecked")
        NonNullList<Integer> outputs = (NonNullList<Integer>) value;
        EnergySnapshot energySnapshot = (EnergySnapshot) snapshot;
        for (int output : outputs) {
            if (output < 0) return false;
            if (energySnapshot.energy > energySnapshot.capacity - output) return false;
            energySnapshot.energy += output;
        }
        return true;
    }

    // ==================== 实际消耗 ====================

    @Override
    public void commitConsume(IOComponent<?, ?> component, SlotZone zone, Object requirement,
            SlotPartition partition) {
        EnergyIOComponent energyComp = (EnergyIOComponent) component;
        int required = requirement instanceof Integer i ? i : 0;
        int extracted = energyComp.extract(ENERGY_SLOT, required, false);
        if (extracted < required) {
            throw new IllegalStateException("Validated energy transaction failed during commit");
        }
    }

    // ==================== 实际产出 ====================

    @Override
    @SuppressWarnings("unchecked")
    public void commitProduce(IOComponent<?, ?> component, SlotZone zone, Object value,
            SlotPartition partition) {
        NonNullList<Integer> outputs = (NonNullList<Integer>) value;
        EnergyIOComponent energyComp = (EnergyIOComponent) component;
        for (int output : outputs) {
            if (output > 0) {
                int remaining = energyComp.insert(ENERGY_SLOT, output, false);
                if (remaining != 0) {
                    throw new IllegalStateException("Validated energy output transaction failed during commit");
                }
            }
        }
    }

    // ==================== 产出滚动 ====================

    @Override
    public NonNullList<?> rollOutputs(Object specification, ServerLevel level) {
        @SuppressWarnings("unchecked")
        NonNullList<Integer> entries = (NonNullList<Integer>) specification;
        NonNullList<Integer> rolled = NonNullList.create();
        rolled.addAll(entries);
        return rolled;
    }

    // ==================== 输入匹配 ====================

    @Override
    public boolean matches(IOComponent<?, ?> component, SlotZone zone, Object requirement,
            SlotPartition partition) {
        // 能量需求通常仅在配方完成阶段校验，初筛阶段默认放行
        return true;
    }

    // ==================== 输入哈希 ====================

    @Override
    public int computeHash(IOComponent<?, ?> component, SlotPartition partition) {
        // 能量不参与输入哈希（能量为辅助资源，变化不影响配方选择）
        return 0;
    }
}

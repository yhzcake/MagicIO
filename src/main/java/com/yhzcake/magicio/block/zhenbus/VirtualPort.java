package com.yhzcake.magicio.block.zhenbus;

import java.util.ArrayList;
import java.util.List;

import com.yhzcake.magicio.io.IOComponent;
import com.yhzcake.magicio.io.IOType;
import com.yhzcake.magicio.io.SideProcessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

public class VirtualPort {

    private final String name;
    private final List<PortBinding> bindings;

    private List<SideProcessor> connectedProcessors = List.of();
    private long lastScanTick = -1;
    private static final long SCAN_INTERVAL = 200;

    public VirtualPort(String name, List<PortBinding> bindings) {
        this.name = name;
        this.bindings = List.copyOf(bindings);
    }

    public String getName() {
        return name;
    }

    public List<PortBinding> getBindings() {
        return bindings;
    }

    public boolean hasDirectConnection() {
        return !connectedProcessors.isEmpty();
    }

    /** 将 value 平均分给所有能接受该类型的邻居。返回未能推送的部分。 */
    public <T> T transfer(T value, IOType type, boolean simulate) {
        List<SideProcessor> targets = new ArrayList<>();
        for (SideProcessor sp : connectedProcessors) {
            if (sp.getIOProcessor().get(type) != null) {
                targets.add(sp);
            }
        }
        if (targets.isEmpty()) return value;

        return transferSplit(value, targets, type, simulate);
    }

    /** 将值均分给所有目标，返回被拒绝的总量。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T transferSplit(T value, List<SideProcessor> targets, IOType type, boolean simulate) {
        int total = getAmount(value);
        int n = targets.size();
        int each = total / n;
        int rem = total % n;
        int rejectedTotal = 0;

        for (int i = 0; i < n; i++) {
            int amount = each + (i < rem ? 1 : 0);
            if (amount <= 0) continue;
            T portion = (T) withAmount(value, amount);
            IOComponent raw = (IOComponent) targets.get(i).getIOProcessor().get(type);
            if (raw == null) { rejectedTotal += amount; continue; }
            Object left = raw.insert(portion, simulate);
            rejectedTotal += getAmount(left);
        }

        return (T) withAmount(value, rejectedTotal);
    }

    /** 提取值的总量。新增 IOType 时在此添加一行。 */
    private static int getAmount(Object value) {
        if (value instanceof ItemStack is) return is.getCount();
        if (value instanceof FluidStack fs) return fs.getAmount();
        if (value instanceof Integer i) return i;
        return 0;
    }

    /** 用指定数量重建值。新增 IOType 时在此添加一行。 */
    private static Object withAmount(Object value, int amount) {
        if (value instanceof ItemStack) return ((ItemStack) value).copyWithCount(amount);
        if (value instanceof FluidStack) return ((FluidStack) value).copyWithAmount(amount);
        if (value instanceof Integer) return amount;
        return value;
    }

    public boolean canAccept(IOType type) {
        for (SideProcessor sp : connectedProcessors) {
            if (sp.getIOProcessor().get(type) != null) return true;
        }
        return false;
    }

    /** 扫描所有绑定面，收集所有可连接的处理器。 */
    public void scanNeighbors(Level level, BlockPos hostPos) {
        List<SideProcessor> found = new ArrayList<>();
        for (PortBinding binding : bindings) {
            BlockPos targetPos = hostPos.relative(binding.hostFace());
            if (!level.isLoaded(targetPos)) continue;
            BlockEntity be = level.getBlockEntity(targetPos);

            if (be instanceof ZhenBusBlockEntity zhenBus) {
                SideProcessor sp = zhenBus.getProcessor(binding.targetFace());
                if (sp != null) {
                    found.add(sp);
                }
            }
        }
        this.connectedProcessors = found.isEmpty() ? List.of() : List.copyOf(found);
    }

    public void invalidate() {
        this.connectedProcessors = List.of();
        this.lastScanTick = -1;
    }

    public void tickRefresh(Level level, BlockPos pos, long currentTick) {
        if (currentTick - lastScanTick >= SCAN_INTERVAL) {
            scanNeighbors(level, pos);
            lastScanTick = currentTick;
        }
    }

    public void forceScan(Level level, BlockPos pos, long currentTick) {
        scanNeighbors(level, pos);
        lastScanTick = currentTick;
    }
}

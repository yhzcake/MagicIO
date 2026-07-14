package cn.yhzcake.magicio.io;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IOComponent<R, T> {

    IOType type();

    int slotCount();

    // ===== 查询 =====

    boolean canSupply(int slot, R requirement);

    boolean hasSupply(R requirement);

    boolean canFit(int slot, T value);

    // ===== 模拟/实际 提取 =====

    T extract(int slot, int amount, boolean simulate);

    // ===== 模拟/实际 插入，返回未能插入的部分 =====

    T insert(int slot, T value, boolean simulate);

    T insert(T value, boolean simulate);

    // ===== 实际产出 =====

    void produce(int slot, T value);

    void produce(T value);

    // ===== 变更通知 =====

    void setChangeCallback(Runnable onChanged);

    // ===== 持久化 =====

    void saveNBT(ValueOutput output);

    void loadNBT(ValueInput input);
}

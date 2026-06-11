package cn.yhzcake.magicio.block.zhenbus;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.io.SideProcessor;
import net.minecraft.core.Direction;

public class ZhenBusStorage {

    private final Map<Direction, SideProcessor> processors = new EnumMap<>(Direction.class);
    /** 客户端侧，从更新包重建的方向标记（无完整 SideProcessor） */
    private final Set<Direction> clientMarkers = new HashSet<>();

    public @Nullable SideProcessor get(Direction dir) {
        return processors.get(dir);
    }

    public void set(Direction dir, SideProcessor processor) {
        processors.put(dir, processor);
        clientMarkers.remove(dir);
    }

    public void remove(Direction dir) {
        processors.remove(dir);
        clientMarkers.remove(dir);
    }

    public boolean has(Direction dir) {
        return processors.containsKey(dir) || clientMarkers.contains(dir);
    }

    public boolean isEmpty() {
        return processors.isEmpty() && clientMarkers.isEmpty();
    }

    public int size() {
        return processors.size() + clientMarkers.size();
    }

    public Iterable<SideProcessor> all() {
        return processors.values();
    }

    public Map<Direction, SideProcessor> asMap() {
        return Map.copyOf(processors);
    }

    /** 客户端：标记该方向有处理器（仅用于碰撞箱重建） */
    public void markDirection(Direction dir) {
        clientMarkers.add(dir);
    }

    /** 客户端：清除该方向的标记 */
    public void clearDirection(Direction dir) {
        clientMarkers.remove(dir);
    }
}

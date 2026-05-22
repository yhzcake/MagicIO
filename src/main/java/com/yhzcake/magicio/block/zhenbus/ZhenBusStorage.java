package com.yhzcake.magicio.block.zhenbus;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.yhzcake.magicio.io.SideProcessor;

import net.minecraft.core.Direction;

public class ZhenBusStorage {

    private final Map<Direction, SideProcessor> processors = new EnumMap<>(Direction.class);

    public @Nullable SideProcessor get(Direction dir) {
        return processors.get(dir);
    }

    public void set(Direction dir, SideProcessor processor) {
        processors.put(dir, processor);
    }

    public void remove(Direction dir) {
        processors.remove(dir);
    }

    public boolean has(Direction dir) {
        return processors.containsKey(dir);
    }

    public boolean isEmpty() {
        return processors.isEmpty();
    }

    public int size() {
        return processors.size();
    }

    public Iterable<SideProcessor> all() {
        return processors.values();
    }

    public Map<Direction, SideProcessor> asMap() {
        return Map.copyOf(processors);
    }
}

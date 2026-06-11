package cn.yhzcake.magicio.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IOProcessor {
    private final Map<IOType, IOComponent<?, ?>> components = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <R, T> IOComponent<R, T> get(IOType type) {
        return (IOComponent<R, T>) components.get(type);
    }

    public <R, T> void register(IOComponent<R, T> component) {
        components.put(component.type(), component);
    }

    public boolean hasComponent(IOType type) {
        return components.containsKey(type);
    }

    public Collection<IOComponent<?, ?>> getAll() {
        return components.values();
    }

    public void registerChangeCallback(Runnable onChanged) {
        for (IOComponent<?, ?> component : components.values()) {
            component.setChangeCallback(onChanged);
        }
    }
}

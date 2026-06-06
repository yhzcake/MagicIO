package cn.yhzcake.magicio.block.inventory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ModIOTypes;
import net.minecraft.core.Direction;

public class FaceAccessController {

    private final Map<Direction, Map<IOType, Set<Integer>>> ioFaceAccess = new EnumMap<>(Direction.class);
    private final Map<Direction, Set<String>> zoneFaceAccess = new EnumMap<>(Direction.class);

    // ===== IO 槽位映射 =====

    private void setIOFaceSlot(Direction direction, IOType type, Set<Integer> slots) {
        Map<IOType, Set<Integer>> typeMap = ioFaceAccess.computeIfAbsent(direction, k -> new HashMap<>());
        if (slots == null || slots.isEmpty()) {
            typeMap.remove(type);
            if (typeMap.isEmpty()) ioFaceAccess.remove(direction);
        } else {
            typeMap.put(type, Set.copyOf(slots));
        }
    }

    public Set<Integer> getIOFaceSlots(Direction direction, IOType type) {
        Map<IOType, Set<Integer>> typeMap = ioFaceAccess.get(direction);
        if (typeMap == null) return Set.of();
        return typeMap.getOrDefault(type, Set.of());
    }

    public Map<Direction, Map<IOType, Set<Integer>>> getIoFaceAccess() {
        return ioFaceAccess;
    }

    public Map<Direction, Set<Integer>> getFaceSlotsForType(IOType type) {
        Map<Direction, Set<Integer>> result = new EnumMap<>(Direction.class);
        for (var entry : ioFaceAccess.entrySet()) {
            Set<Integer> slots = entry.getValue().get(type);
            if (slots != null && !slots.isEmpty()) result.put(entry.getKey(), slots);
        }
        return result;
    }

    public void setSlotsForFace(Direction direction, IOType type, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            setIOFaceSlot(direction, type, null);
        } else {
            setIOFaceSlot(direction, type, slots);
        }
    }

    public void setSlotsForFace(Direction direction, Set<Integer> slots) {
        setSlotsForFace(direction, ModIOTypes.ITEM.get(), slots);
        zoneFaceAccess.remove(direction);
    }

    public void setSlotsForFace(Direction direction) {
        setIOFaceSlot(direction, ModIOTypes.ITEM.get(), null);
        zoneFaceAccess.remove(direction);
    }

    public void addSlotsToFaceAccess(Direction direction, IOType type, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            setIOFaceSlot(direction, type, null);
            if (type == ModIOTypes.ITEM.get()) zoneFaceAccess.remove(direction);
        } else {
            Set<Integer> existing = new HashSet<>(getIOFaceSlots(direction, type));
            existing.addAll(slots);
            setIOFaceSlot(direction, type, existing);
        }
    }

    public void addSlotsToFaceAccess(Direction direction, Set<Integer> slots) {
        addSlotsToFaceAccess(direction, ModIOTypes.ITEM.get(), slots);
    }

    public void addSlotToFaceAccess(Direction direction, IOType type, int slot) {
        Set<Integer> existing = new HashSet<>(getIOFaceSlots(direction, type));
        existing.add(slot);
        setIOFaceSlot(direction, type, existing);
    }

    public void addSlotToFaceAccess(Direction direction, int slot) {
        addSlotToFaceAccess(direction, ModIOTypes.ITEM.get(), slot);
    }

    public void removeSlotFromFaceAccess(Direction direction, IOType type, int slot) {
        Set<Integer> slots = new HashSet<>(getIOFaceSlots(direction, type));
        slots.remove(slot);
        if (slots.isEmpty()) {
            setIOFaceSlot(direction, type, null);
            if (type == ModIOTypes.ITEM.get()) zoneFaceAccess.remove(direction);
        } else {
            setIOFaceSlot(direction, type, slots);
        }
    }

    public void removeSlotFromFaceAccess(Direction direction, int slot) {
        removeSlotFromFaceAccess(direction, ModIOTypes.ITEM.get(), slot);
    }

    public void removeSlotsFromFaceAccess(Direction direction, IOType type, Set<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            setIOFaceSlot(direction, type, null);
            if (type == ModIOTypes.ITEM.get()) zoneFaceAccess.remove(direction);
        } else {
            Set<Integer> remaining = new HashSet<>(getIOFaceSlots(direction, type));
            remaining.removeAll(slots);
            if (remaining.isEmpty()) {
                setIOFaceSlot(direction, type, null);
                if (type == ModIOTypes.ITEM.get()) zoneFaceAccess.remove(direction);
            } else {
                setIOFaceSlot(direction, type, remaining);
            }
        }
    }

    public void removeSlotsFromFaceAccess(Direction direction, Set<Integer> slots) {
        removeSlotsFromFaceAccess(direction, ModIOTypes.ITEM.get(), slots);
    }

    public void clearType(IOType type) {
        for (var entry : ioFaceAccess.entrySet()) {
            entry.getValue().remove(type);
        }
        if (type == ModIOTypes.ITEM.get()) zoneFaceAccess.clear();
    }

    public void clear() {
        ioFaceAccess.clear();
        zoneFaceAccess.clear();
    }

    // ===== Zone 映射 =====

    public Map<Direction, Set<String>> getZoneFaceAccess() {
        return zoneFaceAccess;
    }

    public void setZoneForFace(Direction direction, String zoneName) {
        Set<String> zones = new HashSet<>(zoneFaceAccess.getOrDefault(direction, Set.of()));
        zones.add(zoneName);
        zoneFaceAccess.put(direction, Collections.unmodifiableSet(zones));
    }

    public void removeZoneFromFace(Direction direction, String zoneName) {
        Set<String> zones = new HashSet<>(zoneFaceAccess.getOrDefault(direction, Set.of()));
        zones.remove(zoneName);
        if (zones.isEmpty()) {
            zoneFaceAccess.remove(direction);
        } else {
            zoneFaceAccess.put(direction, Collections.unmodifiableSet(zones));
        }
    }

    public void clearZones() {
        zoneFaceAccess.clear();
    }

    // ===== 批量设置 =====

    public void setItemFaceAccess(Map<Direction, Set<Integer>> faceAccess) {
        clear();
        for (var entry : faceAccess.entrySet()) {
            setIOFaceSlot(entry.getKey(), ModIOTypes.ITEM.get(), entry.getValue());
        }
    }

    public void setFluidFaceAccess(Map<Direction, Set<Integer>> fluidFaceAccess) {
        for (var entry : fluidFaceAccess.entrySet()) {
            setIOFaceSlot(entry.getKey(), ModIOTypes.FLUID.get(), entry.getValue());
        }
    }

    public void addFluidSlotsToFaceAccess(Direction direction, Set<Integer> tanks) {
        if (tanks == null || tanks.isEmpty()) {
            setIOFaceSlot(direction, ModIOTypes.FLUID.get(), null);
        } else {
            Set<Integer> existing = new HashSet<>(getIOFaceSlots(direction, ModIOTypes.FLUID.get()));
            existing.addAll(tanks);
            setIOFaceSlot(direction, ModIOTypes.FLUID.get(), existing);
        }
    }

    public void removeFluidSlotsFromFaceAccess(Direction direction) {
        setIOFaceSlot(direction, ModIOTypes.FLUID.get(), null);
    }
}
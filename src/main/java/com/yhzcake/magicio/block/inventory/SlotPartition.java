package com.yhzcake.magicio.block.inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.yhzcake.magicio.io.IOType;

public class SlotPartition {
    private final Map<SlotZone, Map<IOType, Set<Integer>>> zoneMappings;
    private final int totalAllSlots;

    private SlotPartition(Map<SlotZone, Map<IOType, Set<Integer>>> zoneMappings) {
        this.zoneMappings = Map.copyOf(zoneMappings);
        Set<Integer> all = new HashSet<>();
        for (var zoneEntry : zoneMappings.entrySet()) {
            for (var typeEntry : zoneEntry.getValue().entrySet()) {
                all.addAll(typeEntry.getValue());
            }
        }
        this.totalAllSlots = all.size();
    }

    public static SlotPartition of(IOType type, Map<SlotZone, Set<Integer>> slotMap) {
        Map<SlotZone, Map<IOType, Set<Integer>>> result = new LinkedHashMap<>();
        for (var entry : slotMap.entrySet()) {
            result.put(entry.getKey(), Map.of(type, entry.getValue()));
        }
        return new SlotPartition(result);
    }

    public static SlotPartition of(Map<IOType, Map<SlotZone, Set<Integer>>> mappings) {
        Map<SlotZone, Map<IOType, Set<Integer>>> result = new LinkedHashMap<>();
        for (var typeEntry : mappings.entrySet()) {
            IOType type = typeEntry.getKey();
            for (var zoneEntry : typeEntry.getValue().entrySet()) {
                result.merge(zoneEntry.getKey(), new HashMap<>(Map.of(type, zoneEntry.getValue())), (a, b) -> {
                    Map<IOType, Set<Integer>> merged = new LinkedHashMap<>(a);
                    merged.putAll(b);
                    return merged;
                });
            }
        }
        return new SlotPartition(result);
    }

    public Set<Integer> getSlots(IOType type, SlotZone zone) {
        Map<IOType, Set<Integer>> typeMap = zoneMappings.get(zone);
        if (typeMap == null) return Set.of();
        return typeMap.getOrDefault(type, Set.of());
    }

    public Set<Integer> getAllSlots(IOType type) {
        Set<Integer> all = new HashSet<>();
        for (var entry : zoneMappings.entrySet()) {
            var typeMap = entry.getValue();
            var slots = typeMap.get(type);
            if (slots != null) {
                all.addAll(slots);
            }
        }
        return all;
    }

    public SlotZone getZoneByName(String name) {
        for (SlotZone zone : zoneMappings.keySet()) {
            if (zone.getName().equals(name)) {
                return zone;
            }
        }
        return null;
    }

    public Set<SlotZone> getZoneForSlot(int slot) {
        Set<SlotZone> result = new HashSet<>();
        for (var entry : zoneMappings.entrySet()) {
            for (var typeEntry : entry.getValue().entrySet()) {
                if (typeEntry.getValue().contains(slot)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }

    public int getTotalSlots() {
        return totalAllSlots;
    }

    public int getTotalSlots(IOType type) {
        Set<Integer> all = new HashSet<>();
        for (var entry : zoneMappings.entrySet()) {
            var typeMap = entry.getValue();
            var slots = typeMap.get(type);
            if (slots != null) {
                all.addAll(slots);
            }
        }
        return all.size();
    }

    public boolean isInput(IOType type, int index) {
        for (var entry : zoneMappings.entrySet()) {
            if (entry.getKey().getName().contains("input")) {
                var typeMap = entry.getValue();
                var slots = typeMap.get(type);
                if (slots != null && slots.contains(index)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isOutput(IOType type, int index) {
        for (var entry : zoneMappings.entrySet()) {
            if (entry.getKey().getName().contains("output")) {
                var typeMap = entry.getValue();
                var slots = typeMap.get(type);
                if (slots != null && slots.contains(index)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<IOType> getTypesForSlot(int slot) {
        Set<IOType> result = new HashSet<>();
        for (var entry : zoneMappings.entrySet()) {
            for (var typeEntry : entry.getValue().entrySet()) {
                if (typeEntry.getValue().contains(slot)) {
                    result.add(typeEntry.getKey());
                }
            }
        }
        return result;
    }

    public Set<IOType> getTypesForZone(SlotZone zone) {
        Map<IOType, Set<Integer>> typeMap = zoneMappings.get(zone);
        if (typeMap == null) return Set.of();
        return typeMap.keySet();
    }

    public Map<SlotZone, Map<IOType, Set<Integer>>> getMappings() {
        return zoneMappings;
    }
}

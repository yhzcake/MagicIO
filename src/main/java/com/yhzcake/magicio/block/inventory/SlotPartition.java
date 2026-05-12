package com.yhzcake.magicio.block.inventory;

import java.util.Map;
import java.util.Set;

public class SlotPartition {
    private final Map<SlotZone, Set<Integer>> zoneSlots;

    public SlotPartition(Map<SlotZone, Set<Integer>> zoneSlots) {
        Set<Integer> inputAll = zoneSlots.get(SlotZone.INPUT_ALL);
        Set<Integer> outputAll = zoneSlots.get(SlotZone.OUTPUT_ALL);

        if (inputAll == null) {
            throw new IllegalArgumentException("SlotPartition must contain INPUT_ALL zone");
        }
        if (outputAll == null) {
            throw new IllegalArgumentException("SlotPartition must contain OUTPUT_ALL zone");
        }

        for (Map.Entry<SlotZone, Set<Integer>> entry : zoneSlots.entrySet()) {
            SlotZone zone = entry.getKey();
            Set<Integer> slots = entry.getValue();

            if (zone == SlotZone.INPUT_ALL || zone == SlotZone.OUTPUT_ALL) continue;

            if (zone.isInput() && !zone.isOutput()) {
                if (!inputAll.containsAll(slots)) {
                    throw new IllegalArgumentException(
                            "Input zone '" + zone.getName() + "' contains slots outside INPUT_ALL: " + slots);
                }
            } else if (zone.isOutput() && !zone.isInput()) {
                if (!outputAll.containsAll(slots)) {
                    throw new IllegalArgumentException(
                            "Output zone '" + zone.getName() + "' contains slots outside OUTPUT_ALL: " + slots);
                }
            }
        }

        this.zoneSlots = Map.copyOf(zoneSlots);
    }

    public Set<Integer> getSlots(SlotZone zone) {
        Set<Integer> slots = zoneSlots.get(zone);
        return slots != null ? slots : Set.of();
    }

    public SlotZone getZoneByName(String name) {
        for (SlotZone zone : zoneSlots.keySet()) {
            if (zone.getName().equals(name)) {
                return zone;
            }
        }
        return null;
    }

    public Set<SlotZone> getZoneForSlot(int slot) {
        Set<SlotZone> result = new java.util.HashSet<>();
        for (Map.Entry<SlotZone, Set<Integer>> entry : zoneSlots.entrySet()) {
            if (entry.getValue().contains(slot)) {
                result.add(entry.getKey());
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Slot " + slot + " is not in any zone");
        }
        return result;
    }

    public int getTotalSlots() {
        return getSlots(SlotZone.INPUT_ALL).size() + getSlots(SlotZone.OUTPUT_ALL).size();
    }
}

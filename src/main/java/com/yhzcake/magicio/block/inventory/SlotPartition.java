package com.yhzcake.magicio.block.inventory;

import java.util.Map;
import java.util.Set;

public class SlotPartition {
    private final Map<SlotZone, Set<Integer>> zoneSlots;
    private final Map<SlotZone, Set<Integer>> zoneTanks;

    public SlotPartition(Map<SlotZone, Set<Integer>> zoneSlots) {
        this(zoneSlots, Map.of());
    }

    public SlotPartition(Map<SlotZone, Set<Integer>> zoneSlots, Map<SlotZone, Set<Integer>> zoneTanks) {
        // ---- 验证物品区 ----
        Set<Integer> inputAll = zoneSlots.get(SlotZone.ITEM_INPUT_ALL);
        Set<Integer> outputAll = zoneSlots.get(SlotZone.ITEM_OUTPUT_ALL);

        if (inputAll == null) {
            throw new IllegalArgumentException("SlotPartition must contain ITEM_INPUT_ALL zone");
        }
        if (outputAll == null) {
            throw new IllegalArgumentException("SlotPartition must contain ITEM_OUTPUT_ALL zone");
        }

        for (Map.Entry<SlotZone, Set<Integer>> entry : zoneSlots.entrySet()) {
            SlotZone zone = entry.getKey();
            Set<Integer> slots = entry.getValue();

            if (zone == SlotZone.ITEM_INPUT_ALL || zone == SlotZone.ITEM_OUTPUT_ALL) continue;

            if (zone.isItemInput() && !zone.isItemOutput()) {
                if (!inputAll.containsAll(slots)) {
                    throw new IllegalArgumentException(
                            "Input zone '" + zone.getName() + "' contains slots outside ITEM_INPUT_ALL: " + slots);
                }
            } else if (zone.isItemOutput() && !zone.isItemInput()) {
                if (!outputAll.containsAll(slots)) {
                    throw new IllegalArgumentException(
                            "Output zone '" + zone.getName() + "' contains slots outside ITEM_OUTPUT_ALL: " + slots);
                }
            }
        }

        // ---- 验证流体区 ----
        Set<Integer> fluidInputAll = zoneTanks.get(SlotZone.FLUID_INPUT_ALL);
        Set<Integer> fluidOutputAll = zoneTanks.get(SlotZone.FLUID_OUTPUT_ALL);

        for (Map.Entry<SlotZone, Set<Integer>> entry : zoneTanks.entrySet()) {
            SlotZone zone = entry.getKey();
            Set<Integer> tanks = entry.getValue();

            if (zone == SlotZone.FLUID_INPUT_ALL || zone == SlotZone.FLUID_OUTPUT_ALL) continue;

            if (zone.isLiquitInput() && !zone.isLiquitOutput()) {
                if (fluidInputAll == null || !fluidInputAll.containsAll(tanks)) {
                    throw new IllegalArgumentException(
                            "Fluid input zone '" + zone.getName() + "' contains tanks outside FLUID_INPUT_ALL: " + tanks);
                }
            } else if (zone.isLiquitOutput() && !zone.isLiquitInput()) {
                if (fluidOutputAll == null || !fluidOutputAll.containsAll(tanks)) {
                    throw new IllegalArgumentException(
                            "Fluid output zone '" + zone.getName() + "' contains tanks outside FLUID_OUTPUT_ALL: " + tanks);
                }
            }
        }

        this.zoneSlots = Map.copyOf(zoneSlots);
        this.zoneTanks = Map.copyOf(zoneTanks);
    }

    // ===== 物品槽位 =====

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
        return getSlots(SlotZone.ITEM_INPUT_ALL).size() + getSlots(SlotZone.ITEM_OUTPUT_ALL).size();
    }

    // ===== 流体槽位 =====

    public Set<Integer> getTanks(SlotZone zone) {
        Set<Integer> tanks = zoneTanks.get(zone);
        return tanks != null ? tanks : Set.of();
    }

    public SlotZone getTankZoneByName(String name) {
        for (SlotZone zone : zoneTanks.keySet()) {
            if (zone.getName().equals(name)) {
                return zone;
            }
        }
        return null;
    }

    public Set<SlotZone> getZoneForTank(int tank) {
        Set<SlotZone> result = new java.util.HashSet<>();
        for (Map.Entry<SlotZone, Set<Integer>> entry : zoneTanks.entrySet()) {
            if (entry.getValue().contains(tank)) {
                result.add(entry.getKey());
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Tank " + tank + " is not in any zone");
        }
        return result;
    }

    public int getTotalTanks() {
        Set<Integer> fluidInput = getTanks(SlotZone.FLUID_INPUT_ALL);
        Set<Integer> fluidOutput = getTanks(SlotZone.FLUID_OUTPUT_ALL);
        return fluidInput.size() + fluidOutput.size();
    }

    public boolean isFluidInput(int tank) {
        return getTanks(SlotZone.FLUID_INPUT_ALL).contains(tank);
    }

    public boolean isFluidOutput(int tank) {
        return getTanks(SlotZone.FLUID_OUTPUT_ALL).contains(tank);
    }
}

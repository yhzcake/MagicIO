package com.yhzcake.magicio.block.inventory;

import java.util.Objects;

public final class SlotZone {
    public static final SlotZone INPUT_ALL = new SlotZone("input_all", true, false);
    public static final SlotZone OUTPUT_ALL = new SlotZone("output_all", false, true);
    public static final SlotZone DROP_OUTPUT = new SlotZone("drop_output", false, true);

    private final String name;
    private final boolean isInput;
    private final boolean isOutput;

    public SlotZone(String name, boolean isInput, boolean isOutput) {
        this.name = Objects.requireNonNull(name, "name is null");
        this.isInput = isInput;
        this.isOutput = isOutput;
    }

    public String getName() { return name; }
    public boolean isInput() { return isInput; }
    public boolean isOutput() { return isOutput; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotZone slotZone)) return false;
        return Objects.equals(name, slotZone.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}

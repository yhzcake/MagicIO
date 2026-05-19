package com.yhzcake.magicio.block.inventory;

import java.util.Objects;

public final class SlotZone {
    public static final SlotZone ITEM_INPUT_ALL = new SlotZone("item_input_all", true, false, false, false);
    public static final SlotZone ITEM_OUTPUT_ALL = new SlotZone("item_output_all", false, true, false, false);
    public static final SlotZone DROP_OUTPUT = new SlotZone("drop_output", false, true, false, false);
    public static final SlotZone FLUID_INPUT_ALL = new SlotZone("fluid_input_all", false, false, true, false);
    public static final SlotZone FLUID_OUTPUT_ALL = new SlotZone("fluid_output_all", false, false, false, true);

    private final String name;
    private final boolean isItemInput;
    private final boolean isItemOutput;
    private final boolean isItem;
    private final boolean isLiquitOutput;
    private final boolean isLiquitInput;
    private final boolean isLiquit;

    public SlotZone(String name, boolean isItemInput, boolean isItemOutput, boolean isLiquitInput, boolean isLiquitOutput) {
        this.name = Objects.requireNonNull(name, "name is null");
        this.isItemInput = isItemInput;
        this.isItemOutput = isItemOutput;
        this.isItem = isItemInput || isItemOutput;
        this.isLiquitInput = isLiquitInput;
        this.isLiquitOutput = isLiquitOutput;
        this.isLiquit = isLiquitInput || isLiquitOutput;
    }

    public String getName() { return name; }
    public boolean isItemInput() { return isItemInput; }
    public boolean isItemOutput() { return isItemOutput; }
    public boolean isItem() { return isItem; }
    public boolean isLiquitInput() { return isLiquitInput; }
    public boolean isLiquitOutput() { return isLiquitOutput; }
    public boolean isLiquit() { return isLiquit; }

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

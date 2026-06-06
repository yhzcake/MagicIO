package cn.yhzcake.magicio.block.inventory;

import java.util.Objects;

public final class SlotZone {
    public static final SlotZone ITEM_INPUT_ALL = new SlotZone("item_input_all");
    public static final SlotZone ITEM_OUTPUT_ALL = new SlotZone("item_output_all");
    public static final SlotZone DROP_OUTPUT = new SlotZone("drop_output");
    public static final SlotZone FLUID_ALL = new SlotZone("fluid_all");
    public static final SlotZone ENERGY_INPUT_ALL = new SlotZone("fe_input_all");
    public static final SlotZone ENERGY_OUTPUT_ALL = new SlotZone("fe_output_all");

    private final String name;

    public SlotZone(String name) {
        this.name = Objects.requireNonNull(name, "name is null");
    }

    public String getName() {
        return name;
    }

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

package cn.yhzcake.magicio.io;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class ItemIOComponent implements IOComponent<Ingredient, ItemStack> {
    private final NonNullList<ItemStack> items;
    private final SlotPartition partition;
    private final ResourceHandler<ItemResource> handler;
    private Runnable onChange = () -> {};

    public ItemIOComponent(NonNullList<ItemStack> items, SlotPartition partition) {
        this.items = items;
        this.partition = partition;
        var allSlots = partition.getAllSlots(ModIOTypes.ITEM.get());
        this.handler = new LinkedItemHandler(items, allSlots, allSlots);
    }

    public ResourceHandler<ItemResource> getHandler() {
        return handler;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public IOType type() {
        return ModIOTypes.ITEM.get();
    }

    @Override
    public int slotCount() {
        return partition.getTotalSlots(ModIOTypes.ITEM.get());
    }

    @Override
    public void setChangeCallback(Runnable onChanged) {
        this.onChange = onChanged;
        if (handler instanceof LinkedItemHandler lh) {
            lh.setOnChange(onChanged);
        }
    }

    public void notifyChanged() {
        if (onChange != null) {
            onChange.run();
        }
    }

    // ===== 查询 =====

    @Override
    public boolean canSupply(int slot, Ingredient requirement) {
        return requirement.test(items.get(slot));
    }

    @Override
    public boolean hasSupply(Ingredient requirement) {
        for (int slot : partition.getAllSlots(ModIOTypes.ITEM.get())) {
            if (requirement.test(items.get(slot))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSupply(Ingredient requirement, SlotZone zone) {
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (requirement.test(items.get(slot))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canFit(int slot, ItemStack value) {
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(existing, value)
                && existing.getCount() + value.getCount() <= existing.getMaxStackSize();
    }

    public boolean canFit(SlotZone zone, NonNullList<ItemStack> outputs) {
        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && !insertItem(zone, stack.copy(), true).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ===== 提取 =====

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int extracted = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(extracted);
        if (!simulate) {
            existing.shrink(extracted);
            if (existing.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            notifyChanged();
        }
        return result;
    }

    public ItemStack extractItem(SlotZone zone, int slot, int amount, boolean simulate) {
        if (!partition.getSlots(ModIOTypes.ITEM.get(), zone).contains(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        return extract(slot, amount, simulate);
    }

    public ItemStack extractItem(SlotZone zone, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (!items.get(slot).isEmpty()) {
                return extract(slot, Math.min(amount, items.get(slot).getCount()), simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    // ===== 插入 =====

    @Override
    public ItemStack insert(int slot, ItemStack value, boolean simulate) {
        if (value.isEmpty()) return ItemStack.EMPTY;
        ItemStack existing = items.get(slot);
        if (ItemStack.isSameItemSameComponents(existing, value)) {
            int canInsert = Math.min(value.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (canInsert > 0) {
                if (!simulate) {
                    existing.grow(canInsert);
                    notifyChanged();
                }
                ItemStack remaining = value.copy();
                remaining.shrink(canInsert);
                return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
            }
            return value.copy();
        }
        if (existing.isEmpty()) {
            if (!simulate) {
                items.set(slot, value.copy());
                notifyChanged();
            }
            return ItemStack.EMPTY;
        }
        return value.copy();
    }

    @Override
    public ItemStack insert(ItemStack value, boolean simulate) {
        if (value.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = value.copy();

        for (int slot : partition.getAllSlots(ModIOTypes.ITEM.get())) {
            if (remaining.isEmpty()) break;
            remaining = insert(slot, remaining, simulate);
        }
        return remaining;
    }

    public ItemStack insertItem(SlotZone zone, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack remaining = stack.copy();

        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int canInsert = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canInsert > 0) {
                    if (!simulate) {
                        existing.grow(canInsert);
                        notifyChanged();
                    }
                    remaining.shrink(canInsert);
                }
            }
        }

        for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
            if (remaining.isEmpty()) break;
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                ItemStack placed = remaining.split(remaining.getCount());
                if (!simulate) {
                    items.set(slot, placed);
                    notifyChanged();
                }
            }
        }

        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }

    // ===== 消耗 =====

    @Override
    public void consume(int slot, Ingredient requirement) {
        ItemStack existing = items.get(slot);
        if (requirement.test(existing)) {
            existing.shrink(1);
            if (existing.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            notifyChanged();
        }
    }

    public boolean consumeItem(SlotZone zone, NonNullList<Ingredient> ingredients) {
        if (ingredients.isEmpty()) return true;

        for (Ingredient ingredient : ingredients) {
            boolean consumed = false;
            for (int slot : partition.getSlots(ModIOTypes.ITEM.get(), zone)) {
                if (consumeCheck(ingredient, slot)) {
                    consumed = true;
                    break;
                }
            }
            if (!consumed) return false;
        }
        return true;
    }

    private boolean consumeCheck(Ingredient ingredient, int slot) {
        ItemStack existing = items.get(slot);
        if (ingredient.test(existing)) {
            existing.shrink(1);
            if (existing.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            notifyChanged();
            return true;
        }
        return false;
    }

    // ===== 产出 =====

    @Override
    public void produce(int slot, ItemStack value) {
        if (value.isEmpty()) return;
        insert(slot, value, false);
    }

    @Override
    public void produce(ItemStack value) {
        if (value.isEmpty()) return;
        insert(value, false);
    }

    public void produceItem(SlotZone zone, NonNullList<ItemStack> outputs) {
        for (ItemStack output : outputs) {
            if (!output.isEmpty()) {
                insertItem(zone, output.copy(), false);
            }
        }
    }

    // ===== NBT 持久化 =====

    @Override
    public void saveNBT(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    public void loadNBT(ValueInput input) {
        ContainerHelper.loadAllItems(input, items);
    }
}

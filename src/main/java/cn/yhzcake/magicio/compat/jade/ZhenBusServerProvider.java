package cn.yhzcake.magicio.compat.jade;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.zhenbus.ZhenBusBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum ZhenBusServerProvider implements IServerDataProvider<BlockAccessor> {

    INSTANCE;

    static final String KEY = "zhenbus_data";

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_bus_server");
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return accessor.getBlockEntity() instanceof ZhenBusBlockEntity;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ZhenBusBlockEntity be)) return;

        CompoundTag faces = new CompoundTag();
        for (Direction dir : Direction.values()) {
            var sp = be.getProcessor(dir);
            if (sp == null) continue;
            CompoundTag tag = new CompoundTag();
            tag.putString("type", sp.getZhenType() != null ? sp.getZhenType().getType() : "unknown");
            tag.putInt("process_time", sp.getProcessTime());
            ListTag itemsList = new ListTag();
            if (sp.getItemsForSerialization() != null) {
                for (ItemStack stack : sp.getItemsForSerialization()) {
                    if (!stack.isEmpty()) {
                        ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
                                .result().ifPresent(itemsList::add);
                    }
                }
            }
            tag.put("items", itemsList);
            faces.put(dir.getName(), tag);
        }
        data.put(KEY, faces);
    }
}

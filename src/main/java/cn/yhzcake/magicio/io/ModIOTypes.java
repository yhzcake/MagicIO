package cn.yhzcake.magicio.io;

import java.util.function.Supplier;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModIOTypes {
    public static final DeferredRegister<IOType> IO_TYPES =
            DeferredRegister.create(IOType.IO_TYPE_REGISTRY_KEY, MagicIO.MOD_ID);

    public static Supplier<IOType> ITEM;
    public static Supplier<IOType> FLUID;
    public static Supplier<IOType> ENERGY;

    public static void register(IEventBus eventBus) {
        ITEM = IO_TYPES.register("item", () -> new IOType("item"));
        FLUID = IO_TYPES.register("fluid", () -> new IOType("fluid"));
        ENERGY = IO_TYPES.register("energy", () -> new IOType("energy"));
        IO_TYPES.register(eventBus);
    }

    public static IOType getType(String name) {
        if (name == null || name.isEmpty()) {
            return ITEM.get();
        }
        if (IOType.IO_TYPES == null) {
            return ITEM.get();
        }
        try {
            return IOType.IO_TYPES.get(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, name))
                    .map(Reference::value).orElse(ITEM.get());
        } catch (Exception e) {
            return ITEM.get();
        }
    }

    public static IOType getType(Identifier location) {
        if (location == null) {
            return ITEM.get();
        }
        if (IOType.IO_TYPES == null) {
            return ITEM.get();
        }
        try {
            return IOType.IO_TYPES.get(location)
                    .map(Reference::value).orElse(ITEM.get());
        } catch (Exception e) {
            return ITEM.get();
        }
    }
}

package cn.yhzcake.magicio.io;

import java.util.Objects;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class IOType {
    public static final ResourceKey<Registry<IOType>> IO_TYPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "io_type"));
    public static Registry<IOType> IO_TYPES;

    private final String name;

    public IOType(String name) {
        this.name = Objects.requireNonNull(name, "name is null");
    }

    public String getName() {
        return name;
    }

    public Component getDisplayName() {
        return Component.translatable("io_type." + MagicIO.MOD_ID + "." + name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IOType ioType = (IOType) o;
        return Objects.equals(name, ioType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        IO_TYPES = new RegistryBuilder<>(IO_TYPE_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "item"))
                .create();
        event.register(IO_TYPES);
    }
}

package cn.yhzcake.magicio.common.registry;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.common.sys.matrix.ElementType;
import cn.yhzcake.magicio.common.sys.matrix.MatrixType;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class MIORegistries {
    public static void register(IEventBus bus) {
        bus.register(MATRIX);
        bus.register(ELEMENTS);
    }

    public static final Registry<MatrixType> MATRIX = new RegistryBuilder<>(MIOKeys.MATRIX_TYPE_REGISTRY_KEY).create();
    public static final Registry<ElementType> ELEMENTS = new RegistryBuilder<>(MIOKeys.ELEMENT_TYPE_REGISTRY_KEY).create();


    public static final class MIOKeys {
        public static final ResourceKey<Registry<MatrixType>> MATRIX_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "matrix_type"));
        public static final ResourceKey<Registry<ElementType>> ELEMENT_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "element_type"));
    }
}

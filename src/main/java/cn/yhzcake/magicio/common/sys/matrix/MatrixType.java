package cn.yhzcake.magicio.common.sys.matrix;

import java.util.Objects;
import java.util.function.Function;

import cn.yhzcake.magicio.common.registry.MIORegistries;
import cn.yhzcake.magicio.common.sys.mio.IOConfig;
import cn.yhzcake.magicio.common.sys.mio.IOPartition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author yhz_cake
 * @since origin
 */
public class MatrixType {
    public static final String PREFIX = "matrix_type";

    public record MatrixTickContext(Level level, BlockPos pos, BlockState state) {
    }

    private String descriptionId;
    private final ElementType elementType;
    private final IOConfig ioConfig;
    private final Rarity rarity;
    private final Function<MatrixTickContext, Runnable> tickFactory;

    private final IOPartition partition;

    public MatrixType(Properties properties) {
        this.descriptionId = properties.descriptionId;
        this.elementType = properties.elementType;
        this.ioConfig = properties.ioConfig;
        this.rarity = properties.rarity;
        this.tickFactory = properties.tickFactory;
        this.partition = properties.partition;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public String getDescriptionId() {
        return descriptionId;
    }

    public boolean hasTickFactory() {
        return tickFactory != null;
    }

    public void execute(Level level, BlockPos pos, BlockState state) {
        if (tickFactory != null)
            tickFactory.apply(new MatrixTickContext(level, pos, state)).run();
    }

    public IOPartition getPartition() {
        return partition;
    }

    public IOConfig getIoConfig() {
        return ioConfig;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MatrixType type)) return false;
        return Objects.equals(MIORegistries.MATRIX_TYPE_REGISTRY.getKey(this), MIORegistries.MATRIX_TYPE_REGISTRY.getKey(type));
    }

    @Override
    public String toString() {
        Identifier name = MIORegistries.MATRIX_TYPE_REGISTRY.getKey(this);
        return name != null ? name.toString() : "Unregistered Matrix Type";
    }

    public static final class Properties {
        private String descriptionId;
        private ElementType elementType;
        private Rarity rarity;
        private Function<MatrixTickContext, Runnable> tickFactory;
        private IOPartition partition;
        private IOConfig ioConfig;

        public static Properties create() {
            return new Properties();
        }

        public Properties setDescriptionId(String descriptionId) {
            this.descriptionId = descriptionId;
            return this;
        }

        public Properties setElementType(ElementType elementType) {
            this.elementType = elementType;
            return this;
        }

        public Properties setRarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Properties setTickFactory(Function<MatrixTickContext, Runnable> tickFactory) {
            this.tickFactory = tickFactory;
            return this;
        }

        public Properties setPartition(IOPartition partition) {
            this.partition = partition;
            return this;
        }

        public Properties setIoConfig(IOConfig ioConfig) {
            this.ioConfig = ioConfig;
            return this;
        }



    }

}

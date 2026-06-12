package cn.yhzcake.magicio.common.sys.matrix;

import cn.yhzcake.magicio.common.registry.MIORegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ElementType {
    public static final String PREFIX = "element_type";

    private String descriptionId;
    private final @Nullable Set<ElementType> source;
    private final Rarity rarity;
    public ElementType(Properties properties) {
        this.descriptionId = properties.descriptionId;
        this.source = properties.source;
        this.rarity = properties.rarity;
    }

    public String getDescriptionId() {
        if (this.descriptionId == null)
            this.descriptionId = Util.makeDescriptionId(PREFIX, MIORegistries.ELEMENT_TYPE_REGISTRY.getKey(this));
        return this.descriptionId;
    }

    public @Nullable Set<ElementType> getSource() {
        return source;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public boolean matchSource(Set<ElementType> elements) {
        if (elements.isEmpty()) return source == null || source.isEmpty();
        if (source == null) return false;
        return source.equals(elements);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ElementType type)) return false;
        return Objects.equals(MIORegistries.ELEMENT_TYPE_REGISTRY.getKey(this), MIORegistries.ELEMENT_TYPE_REGISTRY.getKey(type));
    }

    @Override
    public String toString() {
        Identifier name = MIORegistries.ELEMENT_TYPE_REGISTRY.getKey(this);
        return name != null ? name.toString() : "Unregistered Element Type";
    }
    public static final class Properties {
        private String descriptionId;
        private Set<ElementType> source;
        private Rarity rarity = Rarity.COMMON;

        public static Properties create() {
            return new Properties();
        }

        public Properties setDescriptionId(String descriptionId) {
            this.descriptionId = descriptionId;
            return this;
        }

        public Properties withSource(ElementType type) {
            if (this.source == null) source = new HashSet<>();
            source.add(type);
            return this;
        }

        public Properties setRarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }
    }
}

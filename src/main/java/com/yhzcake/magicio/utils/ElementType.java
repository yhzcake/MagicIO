package com.yhzcake.magicio.utils;

import java.util.Objects;

import com.yhzcake.magicio.MagicIO;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class ElementType {
    public static final ResourceKey<Registry<ElementType>> ELEMENT_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "element_type"));
    public static Registry<ElementType> ELEMENT_TYPES;

    private final String type;
    private final ElementType[] parents;

    public ElementType(String type, ElementType... parents) {
        this.type = Objects.requireNonNull(type, "type is null");
        this.parents = parents;
    }

    public String getType() {
        return type;
    }

    public Component getDisplayName() {
        return Component.translatable("element_type." + MagicIO.MOD_ID + "." + type);
    }

    public ElementType[] getParents() {
        return parents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementType that = (ElementType) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return type;
    }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        ELEMENT_TYPES = new RegistryBuilder<>(ELEMENT_TYPE_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "earth"))
                .create();
        event.register(ELEMENT_TYPES);
    }
}

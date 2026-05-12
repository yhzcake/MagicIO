package com.yhzcake.magicio.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceKey;

public class ElementFunction {
    public static Set<ElementType> findCraftable(ElementType... inputs) {
        Set<ElementType> inputSet = Set.of(inputs);
        Set<ElementType> results = new HashSet<>();

        for (Map.Entry<ResourceKey<ElementType>,ElementType> entrySet : ElementType.ELEMENT_TYPES.entrySet()){
            ElementType candidate = entrySet.getValue();
            if (candidate.getParents().length > 0 && inputSet.containsAll(List.of(candidate.getParents()))) {
                results.add(candidate);
            }
        }
        return results;
    }
}

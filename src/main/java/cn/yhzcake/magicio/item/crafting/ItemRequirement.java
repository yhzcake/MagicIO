package cn.yhzcake.magicio.item.crafting;

import net.minecraft.world.item.crafting.Ingredient;

public record ItemRequirement(Ingredient ingredient, int count) {
    public ItemRequirement {
        if (ingredient == null || ingredient.isEmpty()) {
            throw new IllegalArgumentException("Ingredient must not be empty");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Count must be positive");
        }
    }
}

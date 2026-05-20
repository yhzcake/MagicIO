package com.yhzcake.magicio.item.crafting;

import com.yhzcake.magicio.io.IOType;

public record RecipeInput<T>(IOType type, String zoneName, T requirement) {
}

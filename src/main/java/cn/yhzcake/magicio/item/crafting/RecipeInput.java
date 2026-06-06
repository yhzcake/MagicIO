package cn.yhzcake.magicio.item.crafting;

import cn.yhzcake.magicio.io.IOType;

public record RecipeInput<T>(IOType type, String zoneName, T requirement) {
}

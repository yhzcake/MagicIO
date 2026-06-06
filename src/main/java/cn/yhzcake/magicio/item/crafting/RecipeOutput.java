package cn.yhzcake.magicio.item.crafting;

import cn.yhzcake.magicio.io.IOType;

public record RecipeOutput<T>(IOType type, String zoneName, T specification) {
}

package cn.yhzcake.magicio.item.crafting;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

public record ProcessingStateSnapshot(
        long stateRevision,
        long cycleId,
        ProcessingPhase phase,
        @Nullable Identifier recipeId,
        int processTime,
        int effectiveProcessingTime) {
}

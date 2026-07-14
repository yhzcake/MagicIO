package cn.yhzcake.magicio.io;

import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.item.crafting.RecipeProcessor;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

final class SideRecipeStateAdapter {

    private final RecipeProcessor.State state = new RecipeProcessor.State();
    private @Nullable Identifier pendingCurrentRecipeId;
    private @Nullable Identifier pendingLastValidRecipeId;
    private boolean clientHasRecipe;

    boolean hasWork() {
        return state.currentRecipe != null || pendingCurrentRecipeId != null || state.inputsChanged;
    }

    void processTick(Level level, BlockPos pos, String zhenType, SlotPartition partition,
            NonNullList<ItemStack> items, NonNullList<FluidStack> tanks, IOProcessor ioProcessor,
            Map<net.minecraft.core.Direction, Set<String>> zoneFaceAccess, Runnable onChanged) {
        RecipeProcessor.processTick(level, pos, state, zhenType, partition, items, tanks,
                ioProcessor, zoneFaceAccess, true, onChanged);
    }

    void markInputsChanged() {
        state.inputsChanged = true;
    }

    int getProcessTime() {
        return state.processTime;
    }

    cn.yhzcake.magicio.item.crafting.ProcessingStateSnapshot snapshot() {
        return RecipeProcessor.snapshot(state);
    }

    void setProcessTime(int processTime) {
        state.processTime = processTime;
    }

    boolean isInputsChanged() {
        return state.inputsChanged;
    }

    void setInputsChanged(boolean inputsChanged) {
        state.inputsChanged = inputsChanged;
    }

    boolean hasActiveRecipe() {
        return state.currentRecipe != null || clientHasRecipe;
    }

    void applyClientState(int processTime, boolean hasRecipe) {
        state.processTime = processTime;
        clientHasRecipe = hasRecipe;
        if (!hasRecipe) state.currentRecipe = null;
    }

    void save(ValueOutput output) {
        output.putInt("process_time", state.processTime);
        output.putBoolean("inputs_changed", state.inputsChanged);
        output.putInt("effective_processing_time", state.effectiveProcessingTime);
        output.putDouble("output_multiplier", state.outputMultiplier);
        output.putInt("last_input_hash", state.lastInputHash);
        output.putInt("recipe_generation", state.recipeGeneration);
        output.putInt("recipe_check_timer", state.recipeCheckTimer);
        output.putLong("state_revision", state.stateRevision);
        output.putLong("cycle_id", state.cycleId);
        output.putBoolean("output_blocked", state.outputBlocked);
        if (state.currentRecipe != null) {
            output.putString("current_recipe_id", state.currentRecipe.getRecipeId().toString());
        }
        if (state.lastValidRecipe != null) {
            output.putString("last_valid_recipe_id", state.lastValidRecipe.getRecipeId().toString());
        }
    }

    void load(ValueInput input) {
        state.processTime = input.getIntOr("process_time", 0);
        state.inputsChanged = input.getBooleanOr("inputs_changed", true);
        state.effectiveProcessingTime = input.getIntOr("effective_processing_time", 0);
        state.outputMultiplier = input.getDoubleOr("output_multiplier", 1.0);
        state.lastInputHash = input.getIntOr("last_input_hash", 0);
        state.recipeGeneration = input.getIntOr("recipe_generation", 0);
        state.recipeCheckTimer = input.getIntOr("recipe_check_timer", 0);
        state.stateRevision = input.getLongOr("state_revision", 0);
        state.cycleId = input.getLongOr("cycle_id", 0);
        state.outputBlocked = input.getBooleanOr("output_blocked", false);
        pendingCurrentRecipeId = readRecipeId(input, "current_recipe_id");
        pendingLastValidRecipeId = readRecipeId(input, "last_valid_recipe_id");
        state.currentRecipe = null;
        state.lastValidRecipe = null;
        state.inputsChanged = true;
    }

    boolean resolvePendingRecipes(String zhenType) {
        if (pendingCurrentRecipeId == null && pendingLastValidRecipeId == null) return false;
        ZhenRecipeManager manager = ZhenRecipeManager.getInstance();
        if (!manager.isLoaded()) return false;
        if (pendingCurrentRecipeId != null) {
            state.currentRecipe = manager.getRecipe(pendingCurrentRecipeId);
            pendingCurrentRecipeId = null;
        }
        if (pendingLastValidRecipeId != null) {
            state.lastValidRecipe = manager.getRecipe(pendingLastValidRecipeId);
            pendingLastValidRecipeId = null;
        }
        if (state.currentRecipe != null) {
            RecipeProcessor.State restored = new RecipeProcessor.State();
            restored.currentRecipe = state.currentRecipe;
            RecipeProcessor.computeEffectiveValues(restored, zhenType);
            state.effectiveProcessingTime = restored.effectiveProcessingTime;
            state.outputMultiplier = restored.outputMultiplier;
            state.processTime = Math.min(state.processTime, Math.max(0, state.effectiveProcessingTime - 1));
            state.recipeGeneration = ZhenRecipeManager.getRecipeGeneration();
            state.inputsChanged = true;
        } else if (pendingCurrentRecipeId == null) {
            state.processTime = 0;
            state.effectiveProcessingTime = 0;
            state.outputMultiplier = 1.0;
            state.recipeGeneration = 0;
            state.inputsChanged = true;
            state.outputBlocked = false;
            state.stateRevision++;
        }
        return false;
    }

    void writeToStream(FriendlyByteBuf buf) {
        buf.writeInt(state.processTime);
        buf.writeBoolean(state.currentRecipe != null);
    }

    boolean readFromStream(FriendlyByteBuf buf) {
        boolean changed = false;
        int processTime = buf.readInt();
        if (processTime != state.processTime) {
            state.processTime = processTime;
            changed = true;
        }
        boolean hasRecipe = buf.readBoolean();
        if ((state.currentRecipe != null) != hasRecipe) {
            changed = true;
        }
        return changed;
    }

    private @Nullable Identifier readRecipeId(ValueInput input, String key) {
        String value = input.getString(key).orElse("");
        if (value.isEmpty()) return null;
        try {
            return Identifier.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}

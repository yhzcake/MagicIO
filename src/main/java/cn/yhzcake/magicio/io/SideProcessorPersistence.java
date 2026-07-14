package cn.yhzcake.magicio.io;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

final class SideProcessorPersistence {

    private final String zhenType;
    private final Direction side;
    private final IOProcessor ioProcessor;
    private final SideRecipeStateAdapter recipeState;

    SideProcessorPersistence(String zhenType, Direction side, IOProcessor ioProcessor,
            SideRecipeStateAdapter recipeState) {
        this.zhenType = zhenType;
        this.side = side;
        this.ioProcessor = ioProcessor;
        this.recipeState = recipeState;
    }

    void write(ValueOutput output) {
        output.putString("type", zhenType);
        output.putString("side", side.getName());
        recipeState.save(output);
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            component.saveNBT(output);
        }
    }

    void read(ValueInput input) {
        recipeState.load(input);
        for (IOComponent<?, ?> component : ioProcessor.getAll()) {
            component.loadNBT(input);
        }
        recipeState.resolvePendingRecipes(zhenType);
    }

    void writeToStream(FriendlyByteBuf buf) {
        recipeState.writeToStream(buf);
    }

    boolean readFromStream(FriendlyByteBuf buf) {
        return recipeState.readFromStream(buf);
    }
}

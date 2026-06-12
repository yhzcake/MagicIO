package cn.yhzcake.magicio.common.block.entity;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.common.registry.MIOBlockEntities;
import cn.yhzcake.magicio.common.sys.matrix.MatrixType;
import cn.yhzcake.magicio.common.sys.mio.IOConfig;
import cn.yhzcake.magicio.common.sys.mio.IOPartition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AbstractMatrixBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {

    private final MatrixType matrixType;
    private IOPartition partition;
    private IOConfig ioConfig;
    private NonNullList<ItemStack> items;

    protected AbstractMatrixBlockEntity(MatrixType matrixType, BlockPos pos, BlockState state) {
        super(MIOBlockEntities.MATRIX_BLOCK.get(), pos, state);
        this.matrixType = matrixType;
        this.partition = matrixType.getPartition();
        this.ioConfig = matrixType.getIoConfig();
    }
    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
        
    }

    // ===== Container =====

    @Override
    public int getContainerSize() {
        // TODO
        return 0;
    }

    @Override
    public boolean isEmpty() {
        // TODO
        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        // TODO
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // TODO
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // TODO
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // TODO
    }

    @Override
    public boolean stillValid(Player player) {
        // TODO
        return false;
    }

    @Override
    public void clearContent() {
        // TODO
    }

    // ===== WorldlyContainer =====

    @Override
    public int[] getSlotsForFace(Direction direction) {
        // TODO
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        // TODO
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        // TODO
        return false;
    }

    // ===== RecipeCraftingHolder =====

    private @Nullable RecipeHolder<?> recipeUsed;

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        this.recipeUsed = recipe;
    }

    @Override
    public @Nullable RecipeHolder<?> getRecipeUsed() {
        return recipeUsed;
    }

    // ===== StackedContentsCompatible =====

    @Override
    public void fillStackedContents(StackedItemContents contents) {
        // TODO
    }

    // ===== BaseContainerBlockEntity =====

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        // TODO
        return null;
    }

    @Override
    protected net.minecraft.network.chat.Component getDefaultName() {
        // TODO
        return null;
    }
}

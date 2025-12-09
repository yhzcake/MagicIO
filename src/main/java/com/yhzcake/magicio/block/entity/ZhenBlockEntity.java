package com.yhzcake.magicio.block.entity;

import com.yhzcake.magicio.block.zhen.ZhenMethod;
import com.yhzcake.magicio.block.zhen.ZhenType;
import com.yhzcake.magicio.block.zhen.ZhenTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZhenBlockEntity extends BlockEntity implements MenuProvider {

    private ZhenType<?> type;


    public ZhenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ZHEN_BLOCK.get(), pos, blockState);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.magicio.zhen_block");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return null;
    }

    public void dropContents() {
        
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntity entity) {
        ZhenMethod method = new ZhenMethod(level,pos, state, entity);
        method.run();
    }

    public ZhenType<?> getZhenType() {
        if (type == null) {
            type = ZhenTypes.SMALL_SIFT_ZHEN.get();
        }
        return type;
    }
}
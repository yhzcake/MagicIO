package com.yhzcake.magicio.io;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.yhzcake.magicio.block.zhen.ZhenType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

public interface SideProcessor {

    Direction getSide();

    ZhenType getZhenType();

    IOProcessor getIOProcessor();

    BlockPos getPos();

    Level getLevel();

    void tick();

    VoxelShape getShape();

    boolean hasWork();

    void setChangeCallback(Runnable onChanged);

    @Nullable Map<IOType, Set<Integer>> getFaceAccess(Direction direction);

    boolean onActivate(Player player, InteractionHand hand, Vec3 hitPos);

    boolean onShiftActivate(Player player, InteractionHand hand, Vec3 hitPos);

    void addDrops(List<ItemStack> drops);

    void onAdd();

    void onRemove();

    void writeToNBT(ValueOutput output);

    void readFromNBT(ValueInput input);

    /** 返回物品槽副本（用于容器序列化），默认返回 null */
    default @Nullable NonNullList<ItemStack> getItemsForSerialization() {
        return null;
    }

    /** 返回流体槽副本（用于容器序列化），默认返回 null */
    default @Nullable NonNullList<FluidStack> getFluidsForSerialization() {
        return null;
    }

    /** 返回配方处理时间，默认返回 0 */
    default int getProcessTime() {
        return 0;
    }

    /** 设置配方处理时间，默认无操作 */
    default void setProcessTime(int time) {
    }

    void writeToStream(FriendlyByteBuf buf);

    boolean readFromStream(FriendlyByteBuf buf);
}

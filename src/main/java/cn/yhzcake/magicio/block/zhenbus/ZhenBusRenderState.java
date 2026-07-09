package cn.yhzcake.magicio.block.zhenbus;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

import java.util.EnumSet;
import java.util.Set;

public class ZhenBusRenderState extends BlockEntityRenderState {
    public final Set<Direction> activeFaces = EnumSet.noneOf(Direction.class);
}

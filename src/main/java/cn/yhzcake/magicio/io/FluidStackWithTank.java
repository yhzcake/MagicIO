package cn.yhzcake.magicio.io;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 流体罐 + 槽位索引的组合对象，类似 {@link net.minecraft.world.ItemStackWithSlot}。
 * 用于将流体以列表格式持久化，便于阅读：Fluids: [{tank:0, fluid:{FluidName:"minecraft:water", Amount:1000}}]
 */
public record FluidStackWithTank(int tank, FluidStack fluid) {

    public static final Codec<FluidStackWithTank> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.INT.fieldOf("tank").forGetter(FluidStackWithTank::tank),
                    FluidStack.CODEC.fieldOf("fluid").forGetter(FluidStackWithTank::fluid)
            ).apply(instance, FluidStackWithTank::new));

    public FluidStackWithTank {
        fluid = fluid.copy();
    }
}

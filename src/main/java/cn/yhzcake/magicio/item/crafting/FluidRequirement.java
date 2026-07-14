package cn.yhzcake.magicio.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings("null")
public record FluidRequirement(HolderSet<Fluid> fluids, int amount) {
    private static final Codec<HolderSet<Fluid>> FLUIDS_CODEC =
            HolderSetCodec.create(Registries.FLUID, BuiltInRegistries.FLUID.holderByNameCodec(), false);

    public static final Codec<FluidRequirement> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    FLUIDS_CODEC.fieldOf("fluid").forGetter(FluidRequirement::fluids),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(FluidRequirement::amount)
            ).apply(instance, FluidRequirement::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidRequirement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.FLUID), FluidRequirement::fluids,
            ByteBufCodecs.VAR_INT, FluidRequirement::amount,
            FluidRequirement::new
    );

    public FluidRequirement {
        if (fluids == null || fluids.size() == 0) {
            throw new IllegalArgumentException("Fluids must not be empty");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public boolean matchesType(FluidStack stack) {
        return !stack.isEmpty() && fluids.contains(stack.typeHolder());
    }
}

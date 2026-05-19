package com.yhzcake.magicio.block.zhen;

import java.util.Objects;
import java.util.function.Function;

import com.yhzcake.magicio.MagicIO;
import com.yhzcake.magicio.block.entity.method.SmallSiftMethod;
import com.yhzcake.magicio.block.inventory.SlotPartition;
import com.yhzcake.magicio.block.inventory.SlotZone;
import com.yhzcake.magicio.utils.ElementType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import org.jspecify.annotations.Nullable;

import com.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;

public class ZhenType {
    public static final ResourceKey<Registry<ZhenType>> ZHEN_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_type"));
    public static Registry<ZhenType> ZHEN_TYPES;

    private final ElementType elementType;
    private final String type;
    private final SlotPartition partition;
    private final int level;
    private final Function<SmallSiftMethod, Runnable> tickFactory;
    private final @Nullable Integer tankCapacity;

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level, Function<SmallSiftMethod, Runnable> tickFactory, @Nullable Integer tankCapacity) {
        this.elementType = Objects.requireNonNull(elementType, "elementType is null");
        this.type = Objects.requireNonNull(type, "type is null");
        this.partition = Objects.requireNonNull(partition, "partition is null");
        this.level = level;
        this.tickFactory = tickFactory;
        this.tankCapacity = tankCapacity;
    }

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level, Function<SmallSiftMethod, Runnable> tickFactory) {
        this(elementType, type, partition, level, tickFactory, null);
    }

    public ElementType getElementType() {
        return elementType;
    }

    public String getType() {
        return type;
    }

    public SlotPartition getPartition() {
        return partition;
    }

    public int getInput() {
        return partition.getSlots(SlotZone.ITEM_INPUT_ALL).size();
    }

    public int getOutput() {
        return partition.getSlots(SlotZone.ITEM_OUTPUT_ALL).size();
    }

    public int getFluidInput() {
        return partition.getTanks(SlotZone.FLUID_INPUT_ALL).size();
    }

    public int getFluidOutput() {
        return partition.getTanks(SlotZone.FLUID_OUTPUT_ALL).size();
    }

    public int getLevel() {
        return level;
    }

    public Function<SmallSiftMethod, Runnable> getTickFactory() {
        return tickFactory;
    }

    public void execute(Level level, BlockPos pos, BlockState state, AbstractZhenBlockEntity blockEntity) {
        tickFactory.apply(new SmallSiftMethod(level, pos, state, blockEntity)).run();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ZhenType zhenType = (ZhenType) obj;
        return Objects.equals(type, zhenType.type) && Objects.equals(elementType, zhenType.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, elementType);
    }

    @Override
    public String toString() {
        return type;
    }

    @SubscribeEvent
    public static void register(NewRegistryEvent event) {
        ZHEN_TYPES = new RegistryBuilder<>(ZHEN_TYPE_REGISTRY_KEY)
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "small_sift_zhen"))
                .create();
        event.register(ZHEN_TYPES);
    }

    public @Nullable Integer getTankCapacity() {
        return this.tankCapacity;
    }
}

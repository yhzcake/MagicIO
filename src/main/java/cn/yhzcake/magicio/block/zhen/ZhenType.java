package cn.yhzcake.magicio.block.zhen;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.block.entity.AbstractZhenBlockEntity;
import cn.yhzcake.magicio.block.inventory.SlotPartition;
import cn.yhzcake.magicio.block.inventory.SlotZone;
import cn.yhzcake.magicio.io.IOType;
import cn.yhzcake.magicio.io.ModIOTypes;
import cn.yhzcake.magicio.utils.ElementType;

public class ZhenType {
    public static final ResourceKey<Registry<ZhenType>> ZHEN_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_type"));
    public static Registry<ZhenType> ZHEN_TYPES;

    public record ZhenTickContext(Level level, BlockPos pos, BlockState state, @Nullable AbstractZhenBlockEntity blockEntity) {}

    private final ElementType elementType;
    private final String type;
    private final SlotPartition partition;
    private final int level;
    private final Function<ZhenTickContext, Runnable> tickFactory;
    private final @Nullable Integer tankCapacity;
    private final @Nullable Integer energyCapacity;

    /** 面访问控制：外部从某个世界方向连接时暴露哪些槽位。Direction → IOType → 槽位集合。 */
    private final Map<Direction, Map<IOType, Set<Integer>>> faceAccess;

    /** 虚拟端口分区：每个端口名可推送的 zone 名称集合。空集合表示全部 zone。 */
    private final Map<String, Set<String>> portZones;

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level,
            Function<ZhenTickContext, Runnable> tickFactory,
            @Nullable Integer tankCapacity, @Nullable Integer energyCapacity,
            Map<Direction, Map<IOType, Set<String>>> faceZoneAccess,
            Map<String, Set<String>> portZones) {
        this.elementType = Objects.requireNonNull(elementType, "elementType is null");
        this.type = Objects.requireNonNull(type, "type is null");
        this.partition = Objects.requireNonNull(partition, "partition is null");
        this.level = level;
        this.tickFactory = tickFactory;
        this.tankCapacity = tankCapacity;
        this.energyCapacity = energyCapacity;
        this.faceAccess = buildFaceAccess(partition, faceZoneAccess);
        this.portZones = portZones != null ? Map.copyOf(portZones) : Map.of();
    }

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level,
            Function<ZhenTickContext, Runnable> tickFactory,
            @Nullable Integer tankCapacity, @Nullable Integer energyCapacity) {
        this(elementType, type, partition, level, tickFactory, tankCapacity, energyCapacity, Map.of(), Map.of());
    }

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level,
            Function<ZhenTickContext, Runnable> tickFactory
        , @Nullable Integer tankCapacity) {
        this(elementType, type, partition, level, tickFactory, tankCapacity, null, Map.of(), Map.of());
    }

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level,
            Function<ZhenTickContext, Runnable> tickFactory) {
        this(elementType, type, partition, level, tickFactory, null, null);
    }

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level) {
        this(elementType, type, partition, level, null, null, null);
    }

    public ZhenType(ElementType elementType, String type, SlotPartition partition, int level, Map<Direction, Map<IOType, Set<String>>> faceZoneAccess) {
        this(elementType, type, partition, level, null, null,null, faceZoneAccess, Map.of());
    }

    private static Map<Direction, Map<IOType, Set<Integer>>> buildFaceAccess(
            SlotPartition partition, Map<Direction, Map<IOType, Set<String>>> zoneAccess) {
        if (zoneAccess == null || zoneAccess.isEmpty()) return Map.of();
        Map<Direction, Map<IOType, Set<Integer>>> result = new HashMap<>();
        for (var dirEntry : zoneAccess.entrySet()) {
            Direction dir = dirEntry.getKey();
            Map<IOType, Set<Integer>> typeMap = new HashMap<>();
            for (var typeEntry : dirEntry.getValue().entrySet()) {
                IOType ioType = typeEntry.getKey();
                Set<Integer> slots = new HashSet<>();
                for (String zoneName : typeEntry.getValue()) {
                    SlotZone zone = partition.getZoneByName(zoneName);
                    if (zone != null) {
                        slots.addAll(partition.getSlots(ioType, zone));
                    }
                }
                typeMap.put(ioType, Collections.unmodifiableSet(slots));
            }
            result.put(dir, Collections.unmodifiableMap(typeMap));
        }
        return Collections.unmodifiableMap(result);
    }

    public Map<Direction, Map<IOType, Set<Integer>>> getFaceAccess() {
        return faceAccess;
    }

    public Map<String, Set<String>> getPortZones() {
        return portZones;
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
        return partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_INPUT_ALL).size();
    }

    public int getOutput() {
        return partition.getSlots(ModIOTypes.ITEM.get(), SlotZone.ITEM_OUTPUT_ALL).size();
    }

    public int getFluidInput() {
        return partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_INPUT_ALL).size();
    }

    public int getFluidOutput() {
        return partition.getSlots(ModIOTypes.FLUID.get(), SlotZone.FLUID_OUTPUT_ALL).size();
    }

    public int getLevel() {
        return level;
    }

    public boolean hasTickFactory() {
        return tickFactory != null;
    }

    public void execute(Level level, BlockPos pos, BlockState state, @Nullable AbstractZhenBlockEntity blockEntity) {
        if (tickFactory != null)
            tickFactory.apply(new ZhenTickContext(level, pos, state, blockEntity)).run();
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
                .defaultKey(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "unstable_sift_zhen"))
                .create();
        event.register(ZHEN_TYPES);
    }

    public @Nullable Integer getTankCapacity() {
        return this.tankCapacity;
    }

    public @Nullable Integer getEnergyCapacity() {
        return this.energyCapacity;
    }
}

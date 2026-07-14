package cn.yhzcake.magicio.item.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import cn.yhzcake.magicio.io.ModIOTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings({"null", "unchecked"})
public class ZhenRecipeSerializer {

    private static final Codec<OutputEntry> OUTPUT_ENTRY_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Identifier.CODEC.optionalFieldOf("item").forGetter(entry ->
                            entry.itemId() != null
                                    ? Optional.of(entry.itemId())
                                    : Optional.ofNullable(entry.rawStack()).filter(stack -> !stack.isEmpty())
                                            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(entry ->
                            entry.itemId() != null ? entry.count()
                                    : entry.rawStack() == null || entry.rawStack().isEmpty() ? 1 : entry.rawStack().getCount()),
                    Identifier.CODEC.optionalFieldOf("loot_table").forGetter(entry -> Optional.ofNullable(entry.lootTableId()))
            ).apply(instance, (itemId, count, lootTable) -> {
                if (lootTable.isPresent()) return OutputEntry.lootTable(lootTable.get());
                if (itemId.isPresent()) return OutputEntry.item(itemId.get(), count);
                return OutputEntry.item(ItemStack.EMPTY);
            })
    );

    private static final Codec<NonNullList<OutputEntry>> OUTPUT_LIST_CODEC =
            OUTPUT_ENTRY_CODEC.listOf().xmap(
                    (List<OutputEntry> list) -> {
                        NonNullList<OutputEntry> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<OutputEntry>) list
            );

    public static final Codec<ItemRequirement> ITEM_REQUIREMENT_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Ingredient.CODEC.fieldOf("item").forGetter(ItemRequirement::ingredient),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(ItemRequirement::count)
            ).apply(instance, ItemRequirement::new)
    );

    private static final Codec<Map<String, NonNullList<ItemRequirement>>> INPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, ITEM_REQUIREMENT_CODEC.listOf().xmap(
                    list -> {
                        NonNullList<ItemRequirement> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<ItemRequirement>) list
            ));

    private static final Codec<Map<String, NonNullList<OutputEntry>>> OUTPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, OUTPUT_LIST_CODEC);

    private static final Codec<Map<String, NonNullList<FluidRequirement>>> FLUID_INPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FluidRequirement.CODEC.listOf().xmap(
                    list -> {
                        NonNullList<FluidRequirement> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<FluidRequirement>) list
            ));

    public static final Codec<FluidStack> FLUID_OUTPUT_CODEC = FluidStack.CODEC;

    private static final Codec<Map<String, NonNullList<FluidStack>>> FLUID_OUTPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FLUID_OUTPUT_CODEC.listOf().xmap(
                    (List<FluidStack> list) -> {
                        NonNullList<FluidStack> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<FluidStack>) list
            ));

    private static ZhenRecipe buildFromParsed(
            Identifier recipeId,
            Identifier type,
            Map<String, NonNullList<ItemRequirement>> itemInputs,
            Map<String, NonNullList<OutputEntry>> itemOutputs,
            Map<String, NonNullList<FluidRequirement>> fluidInputs,
            Map<String, NonNullList<FluidStack>> fluidOutputs,
            int processingTime
    ) {
        List<RecipeInput<?>> inputs = new ArrayList<>();
        List<RecipeOutput<?>> outputs = new ArrayList<>();

        for (var entry : itemInputs.entrySet()) {
            inputs.add(new RecipeInput<>(ModIOTypes.ITEM.get(), entry.getKey(), entry.getValue()));
        }
        for (var entry : fluidInputs.entrySet()) {
            inputs.add(new RecipeInput<>(ModIOTypes.FLUID.get(), entry.getKey(), entry.getValue()));
        }

        for (var entry : itemOutputs.entrySet()) {
            outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), entry.getKey(), entry.getValue()));
        }
        for (var entry : fluidOutputs.entrySet()) {
            outputs.add(new RecipeOutput<>(ModIOTypes.FLUID.get(), entry.getKey(), entry.getValue()));
        }

        return new ZhenRecipe(recipeId, type, inputs, outputs, processingTime);
    }

    public static final MapCodec<ZhenRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Identifier.CODEC.optionalFieldOf("recipe_id", Identifier.fromNamespaceAndPath("magic_io", "legacy/codec")).forGetter(ZhenRecipe::getRecipeId),
                    Identifier.CODEC.fieldOf("zhen_type").forGetter(ZhenRecipe::getZhenTypeId),
                    INPUT_MAP_CODEC.optionalFieldOf("inputs", Map.of()).forGetter(
                            r -> {
                                Map<String, NonNullList<ItemRequirement>> map = new java.util.LinkedHashMap<>();
                                for (RecipeInput<?> input : r.getInputs()) {
                                    if (input.type() == ModIOTypes.ITEM.get()) {
                                        map.put(input.zoneName(), (NonNullList<ItemRequirement>) input.requirement());
                                    }
                                }
                                return map;
                            }),
                    OUTPUT_MAP_CODEC.optionalFieldOf("outputs", Map.of()).forGetter(
                            r -> {
                                Map<String, NonNullList<OutputEntry>> map = new java.util.LinkedHashMap<>();
                                for (RecipeOutput<?> output : r.getOutputs()) {
                                    if (output.type() == ModIOTypes.ITEM.get()) {
                                        map.put(output.zoneName(), (NonNullList<OutputEntry>) output.specification());
                                    }
                                }
                                return map;
                            }),
                    FLUID_INPUT_MAP_CODEC.optionalFieldOf("fluid_inputs", Map.of()).forGetter(
                            r -> {
                                Map<String, NonNullList<FluidRequirement>> map = new java.util.LinkedHashMap<>();
                                for (RecipeInput<?> input : r.getInputs()) {
                                    if (input.type() == ModIOTypes.FLUID.get()) {
                                        map.put(input.zoneName(), (NonNullList<FluidRequirement>) input.requirement());
                                    }
                                }
                                return map;
                            }),
                    FLUID_OUTPUT_MAP_CODEC.optionalFieldOf("fluid_outputs", Map.of()).forGetter(
                            r -> {
                                Map<String, NonNullList<FluidStack>> map = new java.util.LinkedHashMap<>();
                                for (RecipeOutput<?> output : r.getOutputs()) {
                                    if (output.type() == ModIOTypes.FLUID.get()) {
                                        map.put(output.zoneName(), (NonNullList<FluidStack>) output.specification());
                                    }
                                }
                                return map;
                            }),
                    Codec.INT.fieldOf("processing_time").forGetter(ZhenRecipe::getProcessingTime)
            ).apply(instance, (recipeId, type, itemInputs, itemOutputs, fluidInputs, fluidOutputs, processingTime) ->
                    buildFromParsed(recipeId, type, itemInputs, itemOutputs, fluidInputs, fluidOutputs, processingTime))
    );

    private static final int MAX_ZONES = 256;
    private static final int MAX_ENTRIES_PER_ZONE = 100;

    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                List<RecipeInput<?>> inputs = recipe.getInputs();
                List<RecipeOutput<?>> outputs = recipe.getOutputs();

                Identifier.STREAM_CODEC.encode(buf, recipe.getRecipeId());
                Identifier.STREAM_CODEC.encode(buf, recipe.getZhenTypeId());

                List<RecipeInput<?>> itemInputs = new ArrayList<>();
                List<RecipeInput<?>> fluidInputsList = new ArrayList<>();
                for (RecipeInput<?> input : inputs) {
                    if (input.type() == ModIOTypes.ITEM.get()) itemInputs.add(input);
                    else if (input.type() == ModIOTypes.FLUID.get()) fluidInputsList.add(input);
                }

                if (itemInputs.size() > MAX_ZONES) throw new IllegalArgumentException("Too many item input zones: " + itemInputs.size());
                buf.writeInt(itemInputs.size());
                for (RecipeInput<?> input : itemInputs) {
                    buf.writeUtf(input.zoneName());
                    NonNullList<ItemRequirement> requirements = (NonNullList<ItemRequirement>) input.requirement();
                    if (requirements.size() > MAX_ENTRIES_PER_ZONE) throw new IllegalArgumentException("Too many item requirements: " + requirements.size());
                    buf.writeInt(requirements.size());
                    for (ItemRequirement requirement : requirements) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, requirement.ingredient());
                        buf.writeVarInt(requirement.count());
                    }
                }

                List<RecipeOutput<?>> itemOutputs = new ArrayList<>();
                List<RecipeOutput<?>> fluidOutputsList = new ArrayList<>();
                for (RecipeOutput<?> output : outputs) {
                    if (output.type() == ModIOTypes.ITEM.get()) itemOutputs.add(output);
                    else if (output.type() == ModIOTypes.FLUID.get()) fluidOutputsList.add(output);
                }

                if (itemOutputs.size() > MAX_ZONES) throw new IllegalArgumentException("Too many item output zones: " + itemOutputs.size());
                buf.writeInt(itemOutputs.size());
                for (RecipeOutput<?> output : itemOutputs) {
                    buf.writeUtf(output.zoneName());
                    NonNullList<OutputEntry> entries = (NonNullList<OutputEntry>) output.specification();
                    if (entries.size() > MAX_ENTRIES_PER_ZONE) throw new IllegalArgumentException("Too many item outputs: " + entries.size());
                    buf.writeInt(entries.size());
                    for (OutputEntry entry : entries) {
                        if (entry.isLootTable()) {
                            buf.writeByte(1);
                            Identifier.STREAM_CODEC.encode(buf, entry.lootTableId());
                        } else {
                            buf.writeByte(0);
                            ItemStack stack = entry.stack();
                            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack != null ? stack : ItemStack.EMPTY);
                        }
                    }
                }

                if (fluidInputsList.size() > MAX_ZONES) throw new IllegalArgumentException("Too many fluid input zones: " + fluidInputsList.size());
                buf.writeInt(fluidInputsList.size());
                for (RecipeInput<?> input : fluidInputsList) {
                    buf.writeUtf(input.zoneName());
                    NonNullList<FluidRequirement> fluids = (NonNullList<FluidRequirement>) input.requirement();
                    if (fluids.size() > MAX_ENTRIES_PER_ZONE) throw new IllegalArgumentException("Too many fluid ingredients: " + fluids.size());
                    buf.writeInt(fluids.size());
                    for (FluidRequirement fluid : fluids) {
                        FluidRequirement.STREAM_CODEC.encode(buf, fluid);
                    }
                }

                if (fluidOutputsList.size() > MAX_ZONES) throw new IllegalArgumentException("Too many fluid output zones: " + fluidOutputsList.size());
                buf.writeInt(fluidOutputsList.size());
                for (RecipeOutput<?> output : fluidOutputsList) {
                    buf.writeUtf(output.zoneName());
                    NonNullList<FluidStack> fluids = (NonNullList<FluidStack>) output.specification();
                    if (fluids.size() > MAX_ENTRIES_PER_ZONE) throw new IllegalArgumentException("Too many fluid outputs: " + fluids.size());
                    buf.writeInt(fluids.size());
                    for (FluidStack fluid : fluids) {
                        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid);
                    }
                }

                buf.writeInt(recipe.getProcessingTime());
            },
            buf -> {
                Identifier recipeId = Identifier.STREAM_CODEC.decode(buf);
                Identifier type = Identifier.STREAM_CODEC.decode(buf);

                int inputZoneCount = buf.readInt();
                if (inputZoneCount < 0 || inputZoneCount > MAX_ZONES)
                    throw new IllegalArgumentException("Invalid item input zone count: " + inputZoneCount);
                List<RecipeInput<?>> inputs = new ArrayList<>();
                for (int i = 0; i < inputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int ingredientCount = buf.readInt();
                    if (ingredientCount < 0 || ingredientCount > MAX_ENTRIES_PER_ZONE)
                        throw new IllegalArgumentException("Invalid item ingredient count: " + ingredientCount);
                    NonNullList<ItemRequirement> requirements = NonNullList.create();
                    for (int j = 0; j < ingredientCount; j++) {
                        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        int count = buf.readVarInt();
                        requirements.add(new ItemRequirement(ingredient, count));
                    }
                    inputs.add(new RecipeInput<>(ModIOTypes.ITEM.get(), zoneName, requirements));
                }

                int outputZoneCount = buf.readInt();
                if (outputZoneCount < 0 || outputZoneCount > MAX_ZONES)
                    throw new IllegalArgumentException("Invalid item output zone count: " + outputZoneCount);
                List<RecipeOutput<?>> outputs = new ArrayList<>();
                for (int i = 0; i < outputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int entryCount = buf.readInt();
                    if (entryCount < 0 || entryCount > MAX_ENTRIES_PER_ZONE)
                        throw new IllegalArgumentException("Invalid item output entry count: " + entryCount);
                    NonNullList<OutputEntry> entries = NonNullList.create();
                    for (int j = 0; j < entryCount; j++) {
                        byte entryType = buf.readByte();
                        switch (entryType) {
                            case 1 -> entries.add(OutputEntry.lootTable(Identifier.STREAM_CODEC.decode(buf)));
                            default -> {
                                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                                if (!stack.isEmpty()) {
                                    entries.add(OutputEntry.item(stack));
                                }
                            }
                        }
                    }
                    outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), zoneName, entries));
                }

                int fluidInputZoneCount = buf.readInt();
                if (fluidInputZoneCount < 0 || fluidInputZoneCount > MAX_ZONES)
                    throw new IllegalArgumentException("Invalid fluid input zone count: " + fluidInputZoneCount);
                for (int i = 0; i < fluidInputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int fluidCount = buf.readInt();
                    if (fluidCount < 0 || fluidCount > MAX_ENTRIES_PER_ZONE)
                        throw new IllegalArgumentException("Invalid fluid ingredient count: " + fluidCount);
                    NonNullList<FluidRequirement> fluids = NonNullList.create();
                    for (int j = 0; j < fluidCount; j++) {
                        fluids.add(FluidRequirement.STREAM_CODEC.decode(buf));
                    }
                    inputs.add(new RecipeInput<>(ModIOTypes.FLUID.get(), zoneName, fluids));
                }

                int fluidOutputZoneCount = buf.readInt();
                if (fluidOutputZoneCount < 0 || fluidOutputZoneCount > MAX_ZONES)
                    throw new IllegalArgumentException("Invalid fluid output zone count: " + fluidOutputZoneCount);
                for (int i = 0; i < fluidOutputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int fluidCount = buf.readInt();
                    if (fluidCount < 0 || fluidCount > MAX_ENTRIES_PER_ZONE)
                        throw new IllegalArgumentException("Invalid fluid output count: " + fluidCount);
                    NonNullList<FluidStack> fluids = NonNullList.create();
                    for (int j = 0; j < fluidCount; j++) {
                        fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    outputs.add(new RecipeOutput<>(ModIOTypes.FLUID.get(), zoneName, fluids));
                }

                int processingTime = buf.readInt();
                return new ZhenRecipe(recipeId, type, inputs, outputs, processingTime);
            }
    );

    public static final RecipeSerializer<ZhenRecipe> INSTANCE = new RecipeSerializer<ZhenRecipe>(CODEC, STREAM_CODEC);
}

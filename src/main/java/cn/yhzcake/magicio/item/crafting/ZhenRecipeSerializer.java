package cn.yhzcake.magicio.item.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import cn.yhzcake.magicio.io.ModIOTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings({"null", "unchecked"})
public class ZhenRecipeSerializer {

    private static final Codec<OutputEntry> OUTPUT_ENTRY_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ItemStack.CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(OutputEntry::stack),
                    Identifier.CODEC.optionalFieldOf("loot_table", (Identifier) null).forGetter(OutputEntry::lootTableId)
            ).apply(instance, (stack, lootTable) -> {
                return lootTable != null ? OutputEntry.lootTable(lootTable) : OutputEntry.item(stack);
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

    private static final Codec<Map<String, NonNullList<Ingredient>>> INPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC.listOf().xmap(
                    (List<Ingredient> list) -> {
                        NonNullList<Ingredient> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<Ingredient>) list
            ));

    private static final Codec<Map<String, NonNullList<OutputEntry>>> OUTPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, OUTPUT_LIST_CODEC);

    private static final Codec<ZhenRecipe.FluidIngredient> FLUID_INGREDIENT_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    HolderSetCodec.create(Registries.FLUID, BuiltInRegistries.FLUID.holderByNameCodec(), false)
                            .fieldOf("fluid").forGetter(ZhenRecipe.FluidIngredient::fluids),
                    Codec.INT.fieldOf("amount").forGetter(ZhenRecipe.FluidIngredient::amount)
            ).apply(instance, (fluids, amount) -> new ZhenRecipe.FluidIngredient(fluids, amount))
    );

    private static final Codec<Map<String, NonNullList<ZhenRecipe.FluidIngredient>>> FLUID_INPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FLUID_INGREDIENT_CODEC.listOf().xmap(
                    (List<ZhenRecipe.FluidIngredient> list) -> {
                        NonNullList<ZhenRecipe.FluidIngredient> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<ZhenRecipe.FluidIngredient>) list
            ));

    private static final Codec<Map<String, NonNullList<FluidStack>>> FLUID_OUTPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FluidStack.CODEC.listOf().xmap(
                    (List<FluidStack> list) -> {
                        NonNullList<FluidStack> result = NonNullList.create();
                        result.addAll(list);
                        return result;
                    },
                    list -> (List<FluidStack>) list
            ));

    private static ZhenRecipe buildFromParsed(
            Identifier type,
            Map<String, NonNullList<Ingredient>> itemInputs,
            Map<String, NonNullList<OutputEntry>> itemOutputs,
            Map<String, NonNullList<ZhenRecipe.FluidIngredient>> fluidInputs,
            Map<String, NonNullList<FluidStack>> fluidOutputs,
            Identifier lootTableId,
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
            String zoneName = entry.getKey();
            NonNullList<OutputEntry> entries = entry.getValue();
            if (lootTableId != null) {
                NonNullList<OutputEntry> merged = NonNullList.create();
                merged.addAll(entries);
                merged.add(OutputEntry.lootTable(lootTableId));
                outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), zoneName, merged));
            } else {
                outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), zoneName, entries));
            }
        }
        if (itemOutputs.isEmpty() && lootTableId != null) {
            outputs.add(new RecipeOutput<>(ModIOTypes.ITEM.get(), "item_output_all",
                    NonNullList.of(OutputEntry.lootTable(lootTableId))));
        }
        for (var entry : fluidOutputs.entrySet()) {
            outputs.add(new RecipeOutput<>(ModIOTypes.FLUID.get(), entry.getKey(), entry.getValue()));
        }

        return new ZhenRecipe(type, inputs, outputs, processingTime);
    }

    public static final MapCodec<ZhenRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Identifier.CODEC.fieldOf("zhen_type").forGetter(ZhenRecipe::getZhenTypeId),
                    INPUT_MAP_CODEC.fieldOf("inputs").forGetter(
                            r -> {
                                Map<String, NonNullList<Ingredient>> map = new java.util.LinkedHashMap<>();
                                for (RecipeInput<?> input : r.getInputs()) {
                                    if (input.type() == ModIOTypes.ITEM.get()) {
                                        map.put(input.zoneName(), (NonNullList<Ingredient>) input.requirement());
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
                                Map<String, NonNullList<ZhenRecipe.FluidIngredient>> map = new java.util.LinkedHashMap<>();
                                for (RecipeInput<?> input : r.getInputs()) {
                                    if (input.type() == ModIOTypes.FLUID.get()) {
                                        map.put(input.zoneName(), (NonNullList<ZhenRecipe.FluidIngredient>) input.requirement());
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
                    Identifier.CODEC.optionalFieldOf("loot_table", (Identifier) null).forGetter(r -> null),
                    Codec.INT.fieldOf("processing_time").forGetter(ZhenRecipe::getProcessingTime)
            ).apply(instance, ZhenRecipeSerializer::buildFromParsed)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe.FluidIngredient> FLUID_INGREDIENT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.FLUID), ZhenRecipe.FluidIngredient::fluids,
            ByteBufCodecs.VAR_INT, ZhenRecipe.FluidIngredient::amount,
            (fluids, amount) -> new ZhenRecipe.FluidIngredient(fluids, amount)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                List<RecipeInput<?>> inputs = recipe.getInputs();
                List<RecipeOutput<?>> outputs = recipe.getOutputs();

                Identifier.STREAM_CODEC.encode(buf, recipe.getZhenTypeId());

                List<RecipeInput<?>> itemInputs = new ArrayList<>();
                List<RecipeInput<?>> fluidInputsList = new ArrayList<>();
                for (RecipeInput<?> input : inputs) {
                    if (input.type() == ModIOTypes.ITEM.get()) itemInputs.add(input);
                    else if (input.type() == ModIOTypes.FLUID.get()) fluidInputsList.add(input);
                }

                buf.writeInt(itemInputs.size());
                for (RecipeInput<?> input : itemInputs) {
                    buf.writeUtf(input.zoneName());
                    NonNullList<Ingredient> ingredients = (NonNullList<Ingredient>) input.requirement();
                    buf.writeInt(ingredients.size());
                    for (Ingredient ingredient : ingredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                    }
                }

                List<RecipeOutput<?>> itemOutputs = new ArrayList<>();
                List<RecipeOutput<?>> fluidOutputsList = new ArrayList<>();
                for (RecipeOutput<?> output : outputs) {
                    if (output.type() == ModIOTypes.ITEM.get()) itemOutputs.add(output);
                    else if (output.type() == ModIOTypes.FLUID.get()) fluidOutputsList.add(output);
                }

                buf.writeInt(itemOutputs.size());
                for (RecipeOutput<?> output : itemOutputs) {
                    buf.writeUtf(output.zoneName());
                    NonNullList<OutputEntry> entries = (NonNullList<OutputEntry>) output.specification();
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

                buf.writeInt(fluidInputsList.size());
                for (RecipeInput<?> input : fluidInputsList) {
                    buf.writeUtf(input.zoneName());
                    NonNullList<ZhenRecipe.FluidIngredient> fluids = (NonNullList<ZhenRecipe.FluidIngredient>) input.requirement();
                    buf.writeInt(fluids.size());
                    for (ZhenRecipe.FluidIngredient fluid : fluids) {
                        FLUID_INGREDIENT_STREAM_CODEC.encode(buf, fluid);
                    }
                }

                buf.writeInt(fluidOutputsList.size());
                for (RecipeOutput<?> output : fluidOutputsList) {
                    buf.writeUtf(output.zoneName());
                    NonNullList<FluidStack> fluids = (NonNullList<FluidStack>) output.specification();
                    buf.writeInt(fluids.size());
                    for (FluidStack fluid : fluids) {
                        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid);
                    }
                }

                buf.writeInt(recipe.getProcessingTime());
            },
            buf -> {
                Identifier type = Identifier.STREAM_CODEC.decode(buf);

                int inputZoneCount = buf.readInt();
                List<RecipeInput<?>> inputs = new ArrayList<>();
                for (int i = 0; i < inputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int ingredientCount = buf.readInt();
                    NonNullList<Ingredient> ingredients = NonNullList.create();
                    for (int j = 0; j < ingredientCount; j++) {
                        ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    inputs.add(new RecipeInput<>(ModIOTypes.ITEM.get(), zoneName, ingredients));
                }

                int outputZoneCount = buf.readInt();
                List<RecipeOutput<?>> outputs = new ArrayList<>();
                for (int i = 0; i < outputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int entryCount = buf.readInt();
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
                for (int i = 0; i < fluidInputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int fluidCount = buf.readInt();
                    NonNullList<ZhenRecipe.FluidIngredient> fluids = NonNullList.create();
                    for (int j = 0; j < fluidCount; j++) {
                        fluids.add(FLUID_INGREDIENT_STREAM_CODEC.decode(buf));
                    }
                    inputs.add(new RecipeInput<>(ModIOTypes.FLUID.get(), zoneName, fluids));
                }

                int fluidOutputZoneCount = buf.readInt();
                for (int i = 0; i < fluidOutputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int fluidCount = buf.readInt();
                    NonNullList<FluidStack> fluids = NonNullList.create();
                    for (int j = 0; j < fluidCount; j++) {
                        fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    outputs.add(new RecipeOutput<>(ModIOTypes.FLUID.get(), zoneName, fluids));
                }

                int processingTime = buf.readInt();
                return new ZhenRecipe(type, inputs, outputs, processingTime);
            }
    );

    public static final RecipeSerializer<ZhenRecipe> INSTANCE = new RecipeSerializer<ZhenRecipe>(CODEC, STREAM_CODEC);
}

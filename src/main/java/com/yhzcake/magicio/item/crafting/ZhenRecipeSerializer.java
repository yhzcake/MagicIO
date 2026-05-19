package com.yhzcake.magicio.item.crafting;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

@SuppressWarnings("null")
public class ZhenRecipeSerializer {

    private static final Codec<OutputEntry> OUTPUT_ENTRY_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ItemStack.CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(OutputEntry::stack),
                    Identifier.CODEC.optionalFieldOf("loot_table", (Identifier) null).forGetter(OutputEntry::lootTableId)
            ).apply(instance, (stack, lootTable) ->
                    lootTable != null ? OutputEntry.lootTable(lootTable) : OutputEntry.item(stack)
            )
    );

    private static final Codec<NonNullList<OutputEntry>> OUTPUT_LIST_CODEC =
            OUTPUT_ENTRY_CODEC.listOf().xmap(NonNullList::copyOf, list -> (java.util.List<OutputEntry>) list);

    private static final Codec<Map<String, NonNullList<Ingredient>>> INPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Ingredient.CODEC.listOf().xmap(
                    NonNullList::copyOf,
                    list -> (java.util.List<Ingredient>) list
            ));

    private static final Codec<Map<String, NonNullList<OutputEntry>>> OUTPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, OUTPUT_LIST_CODEC);

    private static final Codec<ZhenRecipe.FluidIngredient> FLUID_INGREDIENT_CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    HolderSetCodec.create(Registries.FLUID, BuiltInRegistries.FLUID.holderByNameCodec(), false)
                            .fieldOf("fluid").forGetter(ZhenRecipe.FluidIngredient::fluids),
                    Codec.INT.fieldOf("amount").forGetter(ZhenRecipe.FluidIngredient::amount)
            ).apply(instance, ZhenRecipe.FluidIngredient::new)
    );

    private static final Codec<Map<String, NonNullList<ZhenRecipe.FluidIngredient>>> FLUID_INPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FLUID_INGREDIENT_CODEC.listOf().xmap(
                    NonNullList::copyOf,
                    list -> (java.util.List<ZhenRecipe.FluidIngredient>) list
            ));

    private static final Codec<Map<String, NonNullList<FluidStack>>> FLUID_OUTPUT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FluidStack.CODEC.listOf().xmap(
                    NonNullList::copyOf,
                    list -> (java.util.List<FluidStack>) list
            ));

    public static final MapCodec<ZhenRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.fieldOf("type").forGetter(ZhenRecipe::getRecipeType),
                    INPUT_MAP_CODEC.fieldOf("inputs").forGetter(ZhenRecipe::getZoneInputs),
                    OUTPUT_MAP_CODEC.optionalFieldOf("outputs", Map.of()).forGetter(ZhenRecipe::getZoneOutputs),
                    FLUID_INPUT_MAP_CODEC.optionalFieldOf("fluid_inputs", Map.of()).forGetter(ZhenRecipe::getFluidZoneInputs),
                    FLUID_OUTPUT_MAP_CODEC.optionalFieldOf("fluid_outputs", Map.of()).forGetter(ZhenRecipe::getFluidZoneOutputs),
                    Identifier.CODEC.optionalFieldOf("loot_table", (Identifier) null).forGetter(ZhenRecipe::getLootTableId),
                    Codec.INT.fieldOf("processing_time").forGetter(ZhenRecipe::getProcessingTime)
            ).apply(instance, ZhenRecipe::new)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe.FluidIngredient> FLUID_INGREDIENT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.FLUID), ZhenRecipe.FluidIngredient::fluids,
            ByteBufCodecs.VAR_INT, ZhenRecipe.FluidIngredient::amount,
            ZhenRecipe.FluidIngredient::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.getRecipeType());
                buf.writeInt(recipe.getZoneInputs().size());
                for (Map.Entry<String, NonNullList<Ingredient>> entry : recipe.getZoneInputs().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    NonNullList<Ingredient> ingredients = entry.getValue();
                    buf.writeInt(ingredients.size());
                    for (Ingredient ingredient : ingredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                    }
                }
                buf.writeInt(recipe.getZoneOutputs().size());
                for (Map.Entry<String, NonNullList<OutputEntry>> entry : recipe.getZoneOutputs().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    NonNullList<OutputEntry> entries = entry.getValue();
                    buf.writeInt(entries.size());
                    for (OutputEntry output : entries) {
                        buf.writeBoolean(output.isLootTable());
                        if (output.isLootTable()) {
                            Identifier.STREAM_CODEC.encode(buf, output.lootTableId());
                        } else {
                            ItemStack.STREAM_CODEC.encode(buf, output.stack());
                        }
                    }
                }
                buf.writeInt(recipe.getFluidZoneInputs().size());
                for (Map.Entry<String, NonNullList<ZhenRecipe.FluidIngredient>> entry : recipe.getFluidZoneInputs().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    NonNullList<ZhenRecipe.FluidIngredient> fluids = entry.getValue();
                    buf.writeInt(fluids.size());
                    for (ZhenRecipe.FluidIngredient fluid : fluids) {
                        FLUID_INGREDIENT_STREAM_CODEC.encode(buf, fluid);
                    }
                }
                buf.writeInt(recipe.getFluidZoneOutputs().size());
                for (Map.Entry<String, NonNullList<FluidStack>> entry : recipe.getFluidZoneOutputs().entrySet()) {
                    buf.writeUtf(entry.getKey());
                    NonNullList<FluidStack> fluids = entry.getValue();
                    buf.writeInt(fluids.size());
                    for (FluidStack fluid : fluids) {
                        FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, fluid);
                    }
                }
                Identifier lootTableId = recipe.getLootTableId();
                buf.writeBoolean(lootTableId != null);
                if (lootTableId != null) {
                    Identifier.STREAM_CODEC.encode(buf, lootTableId);
                }
                buf.writeInt(recipe.getProcessingTime());
            },
            buf -> {
                String type = buf.readUtf();
                int inputZoneCount = buf.readInt();
                Map<String, NonNullList<Ingredient>> zoneInputs = new java.util.LinkedHashMap<>();
                for (int i = 0; i < inputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int ingredientCount = buf.readInt();
                    NonNullList<Ingredient> ingredients = NonNullList.create();
                    for (int j = 0; j < ingredientCount; j++) {
                        ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    }
                    zoneInputs.put(zoneName, ingredients);
                }
                int outputZoneCount = buf.readInt();
                Map<String, NonNullList<OutputEntry>> zoneOutputs = new java.util.LinkedHashMap<>();
                for (int i = 0; i < outputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int entryCount = buf.readInt();
                    NonNullList<OutputEntry> entries = NonNullList.create();
                    for (int j = 0; j < entryCount; j++) {
                        boolean isLoot = buf.readBoolean();
                        if (isLoot) {
                            entries.add(OutputEntry.lootTable(Identifier.STREAM_CODEC.decode(buf)));
                        } else {
                            entries.add(OutputEntry.item(ItemStack.STREAM_CODEC.decode(buf)));
                        }
                    }
                    zoneOutputs.put(zoneName, entries);
                }
                int fluidInputZoneCount = buf.readInt();
                Map<String, NonNullList<ZhenRecipe.FluidIngredient>> fluidInputs = new java.util.LinkedHashMap<>();
                for (int i = 0; i < fluidInputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int fluidCount = buf.readInt();
                    NonNullList<ZhenRecipe.FluidIngredient> fluids = NonNullList.create();
                    for (int j = 0; j < fluidCount; j++) {
                        fluids.add(FLUID_INGREDIENT_STREAM_CODEC.decode(buf));
                    }
                    fluidInputs.put(zoneName, fluids);
                }
                int fluidOutputZoneCount = buf.readInt();
                Map<String, NonNullList<FluidStack>> fluidOutputs = new java.util.LinkedHashMap<>();
                for (int i = 0; i < fluidOutputZoneCount; i++) {
                    String zoneName = buf.readUtf();
                    int fluidCount = buf.readInt();
                    NonNullList<FluidStack> fluids = NonNullList.create();
                    for (int j = 0; j < fluidCount; j++) {
                        fluids.add(FluidStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    fluidOutputs.put(zoneName, fluids);
                }
                Identifier lootTableId = buf.readBoolean() ? Identifier.STREAM_CODEC.decode(buf) : null;
                int processingTime = buf.readInt();
                return new ZhenRecipe(type, zoneInputs, zoneOutputs, fluidInputs, fluidOutputs, lootTableId, processingTime);
            }
    );

    public static final RecipeSerializer<ZhenRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}

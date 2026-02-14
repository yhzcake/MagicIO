package com.yhzcake.magicio.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

@SuppressWarnings("null")
public class ZhenRecipeSerializer implements RecipeSerializer<ZhenRecipe> {
    public static final RecipeSerializer<ZhenRecipe> INSTANCE = new ZhenRecipeSerializer();

    public static final MapCodec<ZhenRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.fieldOf("type").forGetter(ZhenRecipe::getRecipeType),
                    Ingredient.CODEC_NONEMPTY.listOf().xmap(NonNullList::copyOf, list -> list).fieldOf("inputs").forGetter(ZhenRecipe::getIngredients),
                    ItemStack.CODEC.listOf().xmap(NonNullList::copyOf, list -> list).fieldOf("outputs").forGetter(ZhenRecipe::getOutputs),
                    Codec.INT.fieldOf("processing_time").forGetter(ZhenRecipe::getProcessingTime)
            ).apply(instance, ZhenRecipe::new)
    );
    
    @Override
    public MapCodec<ZhenRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ZhenRecipe> streamCodec() {
        return StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.getRecipeType());
                buf.writeInt(recipe.getIngredients().size());
                recipe.getIngredients().forEach(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient));
                buf.writeInt(recipe.getOutputs().size());
                recipe.getOutputs().forEach(stack -> ItemStack.STREAM_CODEC.encode(buf, stack));
                buf.writeInt(recipe.getProcessingTime());
            },
            buf -> {
                String type = buf.readUtf();
                int inputSize = buf.readInt();
                NonNullList<Ingredient> inputs = NonNullList.withSize(inputSize, Ingredient.EMPTY);
                for (int i = 0; i < inputSize; i++) {
                    inputs.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                int outputSize = buf.readInt();
                NonNullList<ItemStack> outputs = NonNullList.withSize(outputSize, ItemStack.EMPTY);
                for (int i = 0; i < outputSize; i++) {
                    outputs.set(i, ItemStack.STREAM_CODEC.decode(buf));
                }
                int processingTime = buf.readInt();
                return new ZhenRecipe(type, inputs, outputs, processingTime);
            }
        );
    }
}
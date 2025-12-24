package com.yhzcake.magicio.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class ZhenRecipeSerializer implements RecipeSerializer<ZhenRecipeImpl> {
    public static final RecipeSerializer<ZhenRecipeImpl> INSTANCE = new ZhenRecipeSerializer();

    public static final MapCodec<ZhenRecipeImpl> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.fieldOf("zhen_type").forGetter(ZhenRecipeImpl::getZhenType),
                    Ingredient.CODEC_NONEMPTY.listOf().xmap(NonNullList::copyOf, list -> list).fieldOf("inputs").forGetter(ZhenRecipeImpl::getInputs),
                    ItemStack.CODEC.listOf().xmap(NonNullList::copyOf, list -> list).fieldOf("outputs").forGetter(ZhenRecipeImpl::getOutputs),
                    Codec.INT.fieldOf("processing_time").forGetter(ZhenRecipeImpl::getProcessingTime),
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ZhenRecipeImpl::getGroup),
                    ResourceLocation.CODEC.listOf().xmap(NonNullList::copyOf, list -> list).optionalFieldOf("loot_tables", NonNullList.create()).forGetter(ZhenRecipeImpl::getLootTables)
            ).apply(instance, ZhenRecipeImpl::new)
    );
    
    @Override
    public @NotNull MapCodec<ZhenRecipeImpl> codec() {
        return CODEC;
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeImpl> streamCodec() {
        // 实现网络序列化
        return StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.getZhenType());
                buf.writeUtf(recipe.getGroup());
                buf.writeInt(recipe.getInputs().size());
                recipe.getInputs().forEach(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient));
                buf.writeInt(recipe.getOutputs().size());
                recipe.getOutputs().forEach(stack -> ItemStack.STREAM_CODEC.encode(buf, stack));
                buf.writeInt(recipe.getLootTables().size());
                recipe.getLootTables().forEach(lootTable -> ResourceLocation.STREAM_CODEC.encode(buf, lootTable));
                buf.writeInt(recipe.getProcessingTime());
            },
            buf -> {
                String zhenType = buf.readUtf();
                String group = buf.readUtf();
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
                int lootTableSize = buf.readInt();
                NonNullList<ResourceLocation> lootTables = NonNullList.withSize(lootTableSize, ResourceLocation.fromNamespaceAndPath("", ""));
                for (int i = 0; i < lootTableSize; i++) {
                    lootTables.set(i, ResourceLocation.STREAM_CODEC.decode(buf));
                }
                int processingTime = buf.readInt();
                return new ZhenRecipeImpl(zhenType, inputs, outputs, processingTime, group, lootTables);
            }
        );
    }
}
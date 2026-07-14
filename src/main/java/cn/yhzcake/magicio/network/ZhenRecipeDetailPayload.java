package cn.yhzcake.magicio.network;

import org.jspecify.annotations.Nullable;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ZhenRecipeDetailPayload(int protocolVersion, long revision, Identifier recipeId,
        @Nullable ZhenRecipe recipe) implements CustomPacketPayload {
    public static final Type<ZhenRecipeDetailPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipe_detail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeDetailPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.protocolVersion());
                buf.writeVarLong(payload.revision());
                Identifier.STREAM_CODEC.encode(buf, payload.recipeId());
                buf.writeBoolean(payload.recipe() != null);
                if (payload.recipe() != null) ZhenRecipeSerializer.STREAM_CODEC.encode(buf, payload.recipe());
            },
            buf -> {
                int protocolVersion = buf.readVarInt();
                long revision = buf.readVarLong();
                Identifier recipeId = Identifier.STREAM_CODEC.decode(buf);
                ZhenRecipe recipe = buf.readBoolean() ? ZhenRecipeSerializer.STREAM_CODEC.decode(buf) : null;
                return new ZhenRecipeDetailPayload(protocolVersion, revision, recipeId, recipe);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

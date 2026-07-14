package cn.yhzcake.magicio.network;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ZhenRecipeDetailRequestPayload(int protocolVersion, long revision, Identifier recipeId) implements CustomPacketPayload {
    public static final Type<ZhenRecipeDetailRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipe_detail_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeDetailRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.protocolVersion());
                buf.writeVarLong(payload.revision());
                Identifier.STREAM_CODEC.encode(buf, payload.recipeId());
            },
            buf -> new ZhenRecipeDetailRequestPayload(buf.readVarInt(), buf.readVarLong(), Identifier.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

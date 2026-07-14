package cn.yhzcake.magicio.network;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ZhenRecipeCatalogRequestPayload(int protocolVersion, long knownRevision) implements CustomPacketPayload {
    public static final Type<ZhenRecipeCatalogRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipe_catalog_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeCatalogRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.protocolVersion());
                buf.writeVarLong(payload.knownRevision());
            },
            buf -> new ZhenRecipeCatalogRequestPayload(buf.readVarInt(), buf.readVarLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

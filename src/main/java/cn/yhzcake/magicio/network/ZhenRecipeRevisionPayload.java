package cn.yhzcake.magicio.network;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZhenRecipeRevisionPayload(int protocolVersion, long revision) implements CustomPacketPayload {
    public static final Type<ZhenRecipeRevisionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipe_revision"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeRevisionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.protocolVersion());
                buf.writeVarLong(payload.revision());
            },
            buf -> new ZhenRecipeRevisionPayload(buf.readVarInt(), buf.readVarLong()));

    public static void handle(ZhenRecipeRevisionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.protocolVersion() == ZhenRecipeNetworkSync.PROTOCOL_VERSION
                    && payload.revision() != cn.yhzcake.magicio.item.crafting.ZhenRecipeManager.getClientInstance().getRevision()
                    && net.neoforged.fml.ModList.get().isLoaded("jei")) {
                context.reply(new ZhenRecipeCatalogRequestPayload(ZhenRecipeNetworkSync.PROTOCOL_VERSION,
                        cn.yhzcake.magicio.item.crafting.ZhenRecipeManager.getClientInstance().getRevision()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

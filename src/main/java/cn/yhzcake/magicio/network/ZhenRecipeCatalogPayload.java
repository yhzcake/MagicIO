package cn.yhzcake.magicio.network;

import java.util.ArrayList;
import java.util.List;

import cn.yhzcake.magicio.MagicIO;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ZhenRecipeCatalogPayload(int protocolVersion, long baseRevision, long revision, byte mode,
        List<Entry> entries, List<Identifier> removed) implements CustomPacketPayload {
    public static final byte NOT_MODIFIED = 0;
    public static final byte FULL = 1;
    public static final byte DELTA = 2;
    public static final Type<ZhenRecipeCatalogPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipe_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeCatalogPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.protocolVersion());
                buf.writeVarLong(payload.baseRevision());
                buf.writeVarLong(payload.revision());
                buf.writeByte(payload.mode());
                buf.writeVarInt(payload.entries().size());
                for (Entry entry : payload.entries()) {
                    Identifier.STREAM_CODEC.encode(buf, entry.recipeId());
                    buf.writeVarInt(entry.processTime());
                }
                buf.writeVarInt(payload.removed().size());
                for (Identifier id : payload.removed()) Identifier.STREAM_CODEC.encode(buf, id);
            },
            buf -> {
                int protocolVersion = buf.readVarInt();
                long baseRevision = buf.readVarLong();
                long revision = buf.readVarLong();
                byte mode = buf.readByte();
                int entryCount = boundedCount(buf.readVarInt());
                List<Entry> entries = new ArrayList<>(entryCount);
                for (int i = 0; i < entryCount; i++) {
                    entries.add(new Entry(Identifier.STREAM_CODEC.decode(buf), buf.readVarInt()));
                }
                int removedCount = boundedCount(buf.readVarInt());
                List<Identifier> removed = new ArrayList<>(removedCount);
                for (int i = 0; i < removedCount; i++) removed.add(Identifier.STREAM_CODEC.decode(buf));
                return new ZhenRecipeCatalogPayload(protocolVersion, baseRevision, revision, mode, List.copyOf(entries), List.copyOf(removed));
            });

    private static int boundedCount(int count) {
        if (count < 0 || count > 10000) throw new IllegalArgumentException("Invalid recipe catalog count: " + count);
        return count;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(Identifier recipeId, int processTime) {
    }
}

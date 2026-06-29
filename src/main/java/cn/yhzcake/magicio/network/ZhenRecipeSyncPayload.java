package cn.yhzcake.magicio.network;

import cn.yhzcake.magicio.MagicIO;
import cn.yhzcake.magicio.compat.jei.MagicIOJeiPlugin;
import cn.yhzcake.magicio.item.crafting.ZhenRecipe;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import cn.yhzcake.magicio.item.crafting.ZhenRecipeSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端→客户端同步配方的数据包。
 * 使用 ZhenRecipeSerializer 的流编码器进行序列化。
 */
public record ZhenRecipeSyncPayload(List<ZhenRecipe> recipes) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ZhenRecipeSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MagicIO.MOD_ID, "zhen_recipe_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, ZhenRecipeSyncPayload payload) {
                    buf.writeVarInt(payload.recipes.size());
                    for (var recipe : payload.recipes) {
                        ZhenRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
                    }
                }

                @Override
                public ZhenRecipeSyncPayload decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    List<ZhenRecipe> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(ZhenRecipeSerializer.STREAM_CODEC.decode(buf));
                    }
                    return new ZhenRecipeSyncPayload(list);
                }
            };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 客户端收到数据包后更新缓存并刷新 JEI 显示 */
    public static void handle(ZhenRecipeSyncPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            ZhenRecipeManager.getInstance().clearRecipes();
            for (var recipe : payload.recipes) {
                ZhenRecipeManager.getInstance().addRecipe(recipe);
            }
            MagicIO.LOGGER.info("[Network] Received {} recipes from server", payload.recipes.size());
            // 用服务端配方刷新 JEI 显示
            MagicIOJeiPlugin.refreshFromCache();
        });
    }
}

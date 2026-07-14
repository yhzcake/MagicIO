package cn.yhzcake.magicio.network;

import cn.yhzcake.magicio.MagicIO;
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

    /** 单次同步的配方数量上限，防止异常载荷导致客户端内存耗尽 */
    private static final int MAX_RECIPES = 10000;

    public static final StreamCodec<RegistryFriendlyByteBuf, ZhenRecipeSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, ZhenRecipeSyncPayload payload) {
                    int size = payload.recipes.size();
                    if (size > MAX_RECIPES) {
                        throw new IllegalArgumentException("Too many recipes to sync: " + size);
                    }
                    buf.writeVarInt(size);
                    for (var recipe : payload.recipes) {
                        ZhenRecipeSerializer.STREAM_CODEC.encode(buf, recipe);
                    }
                }

                @Override
                public ZhenRecipeSyncPayload decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    if (size < 0 || size > MAX_RECIPES) {
                        throw new IllegalArgumentException("Invalid recipe sync count: " + size);
                    }
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
            ZhenRecipeManager.getClientInstance().clearClientCache();
            for (var recipe : payload.recipes) {
                ZhenRecipeManager.getClientInstance().addRecipe(recipe);
            }
            MagicIO.LOGGER.info("[Network] Received {} recipes from server", payload.recipes.size());
            refreshOptionalJei();
        });
    }

    private static void refreshOptionalJei() {
        if (!net.neoforged.fml.ModList.get().isLoaded("jei")) return;
        try {
            Class<?> pluginClass = Class.forName("cn.yhzcake.magicio.compat.jei.MagicIOJeiPlugin");
            pluginClass.getMethod("refreshFromCache").invoke(null);
        } catch (ReflectiveOperationException e) {
            MagicIO.LOGGER.error("Failed to refresh optional JEI integration", e);
        }
    }
}

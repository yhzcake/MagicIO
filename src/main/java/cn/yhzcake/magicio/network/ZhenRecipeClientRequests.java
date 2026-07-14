package cn.yhzcake.magicio.network;

import cn.yhzcake.magicio.item.crafting.ZhenRecipeManager;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ZhenRecipeClientRequests {
    private ZhenRecipeClientRequests() {
    }

    public static void requestCatalog() {
        if (Minecraft.getInstance().getConnection() == null) return;
        ClientPacketDistributor.sendToServer(new ZhenRecipeCatalogRequestPayload(
                ZhenRecipeNetworkSync.PROTOCOL_VERSION,
                ZhenRecipeManager.getClientInstance().getRevision()));
    }
}

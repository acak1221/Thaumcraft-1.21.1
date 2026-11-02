package thaumcraft.client.gui;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.network.RecipeTransferPayload;

/** Physical-client sender kept separate from common packet registration. */
public final class RecipeTransferRequestSender {
    private RecipeTransferRequestSender() {}
    public static void send(ResourceLocation recipeId, boolean maximum, boolean fromBook) {
        var player = Minecraft.getInstance().player;
        if (player != null && recipeId != null)
            NetworkManager.sendToServer(new RecipeTransferPayload(player.containerMenu.containerId, recipeId, maximum, fromBook));
    }
}

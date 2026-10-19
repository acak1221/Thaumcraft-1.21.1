package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import thaumcraft.common.items.golems.ItemGolemBell;

/** Actual shared item-key action for a bell, independent of wand inventory selection. */
public final class GolemBellNetworking {
    private static boolean initialized;
    private GolemBellNetworking() {}
    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GolemBellResetPayload.TYPE, GolemBellResetPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handleReset(player, payload.hand());
                }));
    }
    public static void requestReset(InteractionHand hand) { NetworkManager.sendToServer(new GolemBellResetPayload(hand)); }
    public static boolean handleReset(ServerPlayer player, InteractionHand hand) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Bell reset requires the server thread");
        if (!player.isAlive() || player.isSpectator() || player.server.getPlayerList().getPlayer(player.getUUID()) != player) return false;
        boolean changed = ItemGolemBell.clearMarkers(player.getItemInHand(hand), player);
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return changed;
    }
}

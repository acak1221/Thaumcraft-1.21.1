package thaumcraft.common.lib.network;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;

/** Delta-only original feedback, sent AFTER authoritative mutation/knowledge synchronization. */
public final class WarpMessageNetworking {
    private static boolean server, client;
    private WarpMessageNetworking() {}
    public static void initServer() {
        if (server) return;
        server = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(WarpMessagePayload.TYPE, WarpMessagePayload.STREAM_CODEC);
    }
    public static void initClient(Consumer<WarpMessagePayload> receive) {
        if (client) return;
        client = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, WarpMessagePayload.TYPE, WarpMessagePayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> { if (context.getPlayer() != null) receive.accept(payload); }));
    }
    /** Requested amount is preserved even when the server's nonnegative channel clamps the actual change. */
    public static void send(ServerPlayer player, byte type, int requestedAmount) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Warp notifications require the server thread");
        NetworkManager.sendToPlayer(player, new WarpMessagePayload(requestedAmount, type));
    }
    /** Caller supplies ONLY keys completed by its successful committed action, in original completion order. */
    public static void research(ServerPlayer player, Collection<String> completed) {
        if (WarpEvents.isWussMode()) return;
        for (String key : completed) {
            var definition = ResearchCatalog.get(key);
            int warp = definition == null ? 0 : definition.currentWarp();
            if (warp <= 0) continue;
            send(player, (byte) 0, warp - warp / 2);
            if (warp / 2 > 0) send(player, (byte) 1, warp / 2);
        }
    }
    /** Book/administrator actions have no staged Result object; only new committed ordered keys are announced. */
    public static void researchSince(ServerPlayer player, Set<String> previouslyCompleted) {
        research(player, KnowledgeAccess.get(player).getResearchComplete().stream().filter(key -> !previouslyCompleted.contains(key)).toList());
    }
}

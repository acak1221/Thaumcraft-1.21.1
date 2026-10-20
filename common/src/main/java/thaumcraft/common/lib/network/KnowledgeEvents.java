package thaumcraft.common.lib.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;

/** Common lifecycle wiring. Client receiver registration is a separate entry point. */
public final class KnowledgeEvents {
    private static boolean serverInitialized;
    private static boolean clientInitialized;

    private KnowledgeEvents() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(OpenThaumonomiconPayload.TYPE, OpenThaumonomiconPayload.STREAM_CODEC);
        }
        PlayerEvent.PLAYER_JOIN.register(player -> {
            KnowledgeAccess.get(player).initializePrimalAspects(player.getRandom());
            restoreAutoUnlockResearch(player);
            sync(player);
        });
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            KnowledgeAccess.get(newPlayer).copyFrom(KnowledgeAccess.get(oldPlayer));
            restoreAutoUnlockResearch(newPlayer);
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> sync(player));
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> sync(player));
    }

    /** Call only from the platform's client initializer. */
    public static void initClient() {
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() != null) {
                        KnowledgeAccess.get(context.getPlayer()).load(payload.state());
                    }
                }));
    }

    public static void sync(ServerPlayer player) {
        if (!player.server.isSameThread()) {
            player.server.execute(() -> sync(player));
            return;
        }
        NetworkManager.sendToPlayer(player, new KnowledgeSyncPayload(KnowledgeAccess.get(player).save()));
    }

    private static void restoreAutoUnlockResearch(ServerPlayer player) {
        for (var research : ResearchCatalog.all()) {
            if (research.isAutoUnlock() && research.isEnabled()) {
                KnowledgeAccess.get(player).addResearch(research.key());
            }
        }
    }
}

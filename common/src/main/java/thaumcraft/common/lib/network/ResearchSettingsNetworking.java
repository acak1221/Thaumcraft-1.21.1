package thaumcraft.common.lib.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import thaumcraft.common.config.ThaumcraftSettings;

/** Original login PacketConfig, applied before the knowledge snapshot and cleared on disconnect. */
public final class ResearchSettingsNetworking {
    private static boolean server, client;
    private ResearchSettingsNetworking() {}
    public static void initServer() {
        if (server) return; server = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(ResearchSettingsPayload.TYPE, ResearchSettingsPayload.STREAM_CODEC);
        PlayerEvent.PLAYER_JOIN.register(player -> NetworkManager.sendToPlayer(player, new ResearchSettingsPayload(ThaumcraftSettings.server())));
    }
    public static void initClient() {
        if (client) return; client = true;
        ThaumcraftSettings.initializeClient();
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ResearchSettingsPayload.TYPE, ResearchSettingsPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ThaumcraftSettings.receive(payload.settings())));
    }
}

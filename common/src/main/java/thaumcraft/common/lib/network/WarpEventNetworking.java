package thaumcraft.common.lib.network;
import java.util.function.IntConsumer;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
public final class WarpEventNetworking {
    private static boolean server, client;
    private WarpEventNetworking() {}
    public static void initServer() {
        if (server) return; server = true;
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(WarpEventPayload.TYPE, WarpEventPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(GuardianSonicPayload.TYPE, GuardianSonicPayload.STREAM_CODEC);
        }
    }
    public static void initClient(IntConsumer receive, IntConsumer sonic) {
        if (client) return; client = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, WarpEventPayload.TYPE, WarpEventPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> receive.accept(payload.event())));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, GuardianSonicPayload.TYPE, GuardianSonicPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> sonic.accept(payload.source())));
    }
    public static void send(ServerPlayer player, int event) { NetworkManager.sendToPlayer(player, new WarpEventPayload(event)); }
    public static void sonic(EntityEldritchGuardian source) {
        if (!(source.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var packet = new GuardianSonicPayload(source.getId());
        for (ServerPlayer player : level.players()) if (player.distanceToSqr(source) <= 32 * 32) NetworkManager.sendToPlayer(player, packet);
    }
}

package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.lib.research.ScanRunes;

/**
 * The route of the rune particles of the original thaumometer scan.
 *
 * <p>Oracle {@code thaumcraft/common/items/relics/ItemThaumometer.java:92,103,128} draws the rune
 * over the target of the scan on the client, through the client proxy helper
 * {@code Thaumcraft.proxy.blockRunes} (oracle
 * {@code thaumcraft/client/ClientProxy.java:918-922}). The native scan is server owned, so the
 * server sends the rune to the scanning player and the client draws it.</p>
 */
public final class ScanRuneFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private ScanRuneFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(ScanRuneFxPayload.TYPE, ScanRuneFxPayload.STREAM_CODEC);
        }
    }

    public static void initClient(Consumer<ScanRuneFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ScanRuneFxPayload.TYPE, ScanRuneFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())) return;
                    effects.accept(payload);
                }));
    }

    /** The rune of one scan tick, sent to the scanning player alone. */
    public static void send(ServerPlayer player, ScanRunes.Rune rune) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rune, "rune");
        if (!player.server.isSameThread()) throw new IllegalStateException("Scan runes require the server thread");
        NetworkManager.sendToPlayer(player, ScanRuneFxPayload.of(player.level().dimension().location(), rune));
    }
}

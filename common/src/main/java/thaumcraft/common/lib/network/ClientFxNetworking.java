package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The S2C route of the client effect facade: the sparkle, the burst, the runes and the smoke spiral that the server of an
 * add-on asks the players around a point to draw ({@code thaumcraft.api.ThaumcraftFX.sparkle} and its siblings).
 *
 * <p>The 1.7.10 proxy methods these come from were client-side calls, so there is no original packet; the route follows the
 * block effect channels ({@link BlockFxNetworking}): the players within thirty two blocks of the effect, from the server thread
 * of the level, and the client ignores a payload that names another dimension than its own.</p>
 */
public final class ClientFxNetworking {
    /** The radius around the point of the effect whose players are sent the payload, as the block effect packets of the original. */
    public static final double RANGE = BlockFxNetworking.RANGE;
    private static boolean serverInitialized, clientInitialized;
    private ClientFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(ClientFxPayload.TYPE, ClientFxPayload.STREAM_CODEC);
    }

    public static void initClient(Consumer<ClientFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ClientFxPayload.TYPE, ClientFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())) return;
                    effects.accept(payload);
                }));
    }

    /**
     * Sends a payload to the players of the level near its point.
     *
     * @throws IllegalStateException when not called on the server thread of the level
     */
    public static void send(ServerLevel level, ClientFxPayload payload) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(payload, "payload");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Client effects require the server thread");
        if (!level.dimension().location().equals(payload.dimension())) throw new IllegalArgumentException("The payload is for another dimension");
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(payload.x(), payload.y(), payload.z()) <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
        }
    }
}

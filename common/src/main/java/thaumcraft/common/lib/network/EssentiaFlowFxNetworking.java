package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The S2C route of the original essence flow packet.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXEssentiaSource.java:20-78} and its only sender,
 * the public essentia source API (oracle {@code thaumcraft/common/lib/events/EssentiaHandler.java:54}): the
 * packet is written to the players around the consumer with
 * {@code NetworkRegistry.TargetPoint(dim, x, y, z, 32.0)}.</p>
 *
 * <p>The 1.7.10 target point measures the distance from the raw block coordinate of the consumer, so the
 * native does the same and not from the centre of the block.</p>
 */
public final class EssentiaFlowFxNetworking {
    /** The original TargetPoint radius of the essence flow packet. */
    public static final double RANGE = 32.0D;
    private static boolean serverInitialized, clientInitialized;
    private EssentiaFlowFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(EssentiaFlowFxPayload.TYPE, EssentiaFlowFxPayload.STREAM_CODEC);
        }
    }

    public static void initClient(Consumer<EssentiaFlowFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, EssentiaFlowFxPayload.TYPE, EssentiaFlowFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())) return;
                    effects.accept(payload);
                }));
    }

    /**
     * The original {@code PacketFXEssentiaSource} of a successful drain.
     *
     * @param consumer the tile that took the aspect
     * @param source the tile the aspect came from
     * @param rgb the colour of the aspect
     */
    public static void sendEssentiaFlow(ServerLevel level, BlockPos consumer, BlockPos source, int rgb) {
        if (consumer == null || source == null) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Essentia flow effects require the server thread");
        // Oracle EssentiaHandler:54 - the deltas are the byte differences of the original.
        byte dx = (byte) (consumer.getX() - source.getX());
        byte dy = (byte) (consumer.getY() - source.getY());
        byte dz = (byte) (consumer.getZ() - source.getZ());
        var payload = new EssentiaFlowFxPayload(level.dimension().location(), consumer.immutable(), dx, dy, dz, rgb);
        for (ServerPlayer player : level.players()) {
            double x = player.getX() - consumer.getX();
            double y = player.getY() - consumer.getY();
            double z = player.getZ() - consumer.getZ();
            if (x * x + y * y + z * z <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
        }
    }
}

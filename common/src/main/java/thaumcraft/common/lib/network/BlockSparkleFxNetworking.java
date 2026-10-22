package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The S2C route of the original block sparkle packet.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:50-52} and its six
 * senders: the flux scrubber {@code 0xDD00FF} (oracle {@code TileFluxScrubber.java:100}), the
 * eldritch lock {@code 0x400040} (oracle {@code TileEldritchLock.java:130}), the warding focus
 * {@code 16556032} (oracle {@code ItemFocusWarding.java:137,151}), the wand {@code -9999}
 * (oracle {@code WandManager.java:244,245,420,795}), the eldritch warden {@code 0x800080}
 * (oracle {@code EntityEldritchWarden.java:234}) and the server tick {@code 0xC0C0FF}
 * (oracle {@code ServerTickEventsFML.java:154}).</p>
 *
 * <p>Every original sender wrote to the players within the thirty two blocks of the effect
 * ({@code NetworkRegistry.TargetPoint(..., 32.0)}).</p>
 */
public final class BlockSparkleFxNetworking {
    /** The original TargetPoint radius of the block sparkle packet. */
    public static final double RANGE = 32.0D;
    private static boolean serverInitialized, clientInitialized;
    private BlockSparkleFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(BlockSparkleFxPayload.TYPE, BlockSparkleFxPayload.STREAM_CODEC);
        }
    }

    public static void initClient(Consumer<BlockSparkleFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BlockSparkleFxPayload.TYPE, BlockSparkleFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())) return;
                    effects.accept(payload);
                }));
    }

    /**
     * The original {@code PacketFXBlockSparkle}: a burst of block sparkles at a block.
     *
     * @param rgb the raw colour of the original packet, {@link BlockSparkleFxPayload#RANDOM_GREY} for the
     *            original random grey.
     */
    public static void sendBlockSparkle(ServerLevel level, BlockPos target, int rgb) {
        if (target == null) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block sparkles require the server thread");
        var payload = new BlockSparkleFxPayload(level.dimension().location(), target, rgb);
        Vec3 center = Vec3.atCenterOf(target);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
        }
    }
}

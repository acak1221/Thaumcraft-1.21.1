package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The S2C route of the original block visual channels (oracle
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockArc.java},
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockZap.java} and
 * {@code thaumcraft/common/lib/network/fx/PacketFXBlockBubble.java}).
 *
 * <p>Every original sender wrote to the players around the effect: the arc lightning of the eldritch warden
 * and the cultist portal to the thirty two blocks around the effect, the node bolt of the eldritch trap to the
 * thirty two blocks around the trap and the crucible bubbles of the elemental axe to the thirty two blocks
 * around the log.</p>
 */
public final class BlockFxNetworking {
    /** The original TargetPoint radius of the three block effect packets. */
    public static final double RANGE = 32.0D;
    private static boolean serverInitialized, clientInitialized;
    private BlockFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(BlockFxPayload.TYPE, BlockFxPayload.STREAM_CODEC);
    }

    public static void initClient(Consumer<BlockFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, BlockFxPayload.TYPE, BlockFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())) return;
                    effects.accept(payload);
                }));
    }

    /** The original PacketFXBlockArc: the arc lightning from an entity to a block. */
    public static void sendArc(ServerLevel level, Entity source, BlockPos target) {
        if (source == null || target == null) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block effects require the server thread");
        var payload = BlockFxPayload.arc(level.dimension().location(), target, source.getId());
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(Vec3.atCenterOf(target)) <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
    }

    /** The original PacketFXBlockZap: the node bolt between two points. */
    public static void sendZap(ServerLevel level, Vec3 origin, Vec3 destination) {
        if (origin == null || destination == null) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block effects require the server thread");
        var payload = BlockFxPayload.zap(level.dimension().location(), origin, destination);
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(origin) <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
    }

    /** The original PacketFXBlockBubble: the crucible bubbles around a block. */
    public static void sendBubble(ServerLevel level, BlockPos target, int rgb) {
        if (target == null) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block effects require the server thread");
        var payload = BlockFxPayload.bubble(level.dimension().location(), target, rgb);
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(Vec3.atCenterOf(target)) <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
    }

    /**
     * The original PacketFXBlockSparkle: the sparkle burst at a block.
     *
     * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:29-33} and the
     * thirty two block radius of the {@code sendToAllAround} of the original.</p>
     */
    public static void sendSparkle(ServerLevel level, BlockPos target, int rgb) {
        if (target == null) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Block effects require the server thread");
        var payload = BlockFxPayload.sparkle(level.dimension().location(), target, rgb);
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(Vec3.atCenterOf(target)) <= RANGE * RANGE) NetworkManager.sendToPlayer(player, payload);
    }
}

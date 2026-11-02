package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Visual-only replacement for TileNode's original drainer/color update tag. */
public final class NodeDrainFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private NodeDrainFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER)
            NetworkManager.registerS2CPayloadType(NodeDrainFxPayload.TYPE, NodeDrainFxPayload.STREAM_CODEC);
    }

    public static void initClient(Consumer<NodeDrainFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NodeDrainFxPayload.TYPE, NodeDrainFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())
                            || player.distanceToSqr(Vec3.atCenterOf(payload.node())) > 64 * 64
                            || player.level().isOutsideBuildHeight(payload.node()) || !player.level().hasChunkAt(payload.node())) return;
                    effects.accept(payload);
                }));
    }

    /** Called only after the actual wand grant and node debit have committed. */
    public static void send(ServerPlayer actor, BlockPos node, int rgb) { publish(actor, node, rgb, true); }
    public static void stop(ServerPlayer actor, BlockPos node) { publish(actor, node, 0, false); }

    private static void publish(ServerPlayer actor, BlockPos node, int rgb, boolean active) {
        if (!actor.server.isSameThread()) throw new IllegalStateException("Node drain effects require the server thread");
        var level = actor.serverLevel();
        if (actor.isRemoved() || actor.connection == null || actor.hasDisconnected()
                || actor.server.getPlayerList().getPlayer(actor.getUUID()) != actor
                || level.isOutsideBuildHeight(node) || !level.hasChunkAt(node)) return;
        var payload = new NodeDrainFxPayload(level.dimension().location(), node, actor.getId(), actor.getUUID(), rgb & 0xffffff, active);
        for (ServerPlayer observer : level.players()) {
            if (observer.connection == null || observer.hasDisconnected()
                    || actor.server.getPlayerList().getPlayer(observer.getUUID()) != observer
                    || observer.distanceToSqr(Vec3.atCenterOf(node)) > 64 * 64) continue;
            try { NetworkManager.sendToPlayer(observer, payload); }
            catch (RuntimeException failure) {
                thaumcraft.common.Thaumcraft.LOG.warn("Unable to deliver node drain appearance to {}", observer.getUUID(), failure);
            }
        }
    }
}

package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Small S2C-only route. Common/server code has no dependency on client render classes. */
public final class ShockFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private ShockFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(ShockFxPayload.TYPE, ShockFxPayload.STREAM_CODEC);
    }

    public static void initClient(Consumer<ShockFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ShockFxPayload.TYPE, ShockFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())
                            || player.distanceToSqr(payload.origin()) > 64 * 64
                            || !player.level().hasChunkAt(BlockPos.containing(payload.origin()))
                            || !player.level().hasChunkAt(BlockPos.containing(payload.target()))) return;
                    effects.accept(payload);
                }));
    }

    /** Called ONLY after the real hand's vis component has been debited. */
    public static void send(ServerPlayer caster, Vec3 origin, Vec3 target, boolean impact) {
        if (!caster.server.isSameThread()) throw new IllegalStateException("Shock effects require the server thread");
        if (caster.isRemoved() || !ShockFxPayload.validGeometry(origin, target) || caster.getEyePosition().distanceToSqr(origin) > 4) return;
        var level = caster.serverLevel();
        var payload = new ShockFxPayload(level.dimension().location(), caster.getId(), origin, target, level.random.nextLong(), impact);
        for (ServerPlayer observer : level.players()) if (observer.distanceToSqr(origin) <= 64 * 64) NetworkManager.sendToPlayer(observer, payload);
    }
}

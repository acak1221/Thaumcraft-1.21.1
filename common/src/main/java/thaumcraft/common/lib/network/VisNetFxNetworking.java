package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;

/** Original Utils294-305 visual-only per-drainer throttle. Supply/selection and
 * world RNG never depend on it. Maps are scoped to actual server-level lifetime. */
public final class VisNetFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private static final Map<ServerLevel, Map<BlockPos, Long>> EFFECTS = new WeakHashMap<>();
    private VisNetFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(VisNetFxPayload.TYPE, VisNetFxPayload.STREAM_CODEC);
    }
    public static void initClient(Consumer<VisNetFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, VisNetFxPayload.TYPE, VisNetFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())
                            || player.distanceToSqr(Vec3.atCenterOf(payload.origin())) > 64 * 64
                            || player.level().isOutsideBuildHeight(payload.origin()) || player.level().isOutsideBuildHeight(payload.target())
                            || !player.level().hasChunkAt(payload.origin()) || !player.level().hasChunkAt(payload.target())) return;
                    effects.accept(payload);
                }));
    }

    /** origin is the actual consumer, target its selected source/relay. Called
     * only AFTER debit; exceptions here must never turn spent CV into a refund. */
    public static void send(ServerLevel level, BlockPos origin, BlockPos target, Aspect aspect) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("VisNet effects require the server thread");
        if (aspect == null || Aspect.getAspect(aspect.getTag()) != aspect || !aspect.isPrimal()
                || level.isOutsideBuildHeight(origin) || level.isOutsideBuildHeight(target)
                || !level.hasChunkAt(origin) || !level.hasChunkAt(target)) return;
        var payload = new VisNetFxPayload(level.dimension().location(), origin, target, aspect.getColor() & 0xffffff);
        long now = System.currentTimeMillis();
        Map<BlockPos, Long> buffer = EFFECTS.computeIfAbsent(level, ignored -> new HashMap<>());
        Long expires = buffer.get(origin);
        if (expires != null) {
            // Intentional original quirk: an expired entry is removed THIS call;
            // a subsequent committed drain may emit. Not a supply cooldown.
            if (expires < now) buffer.remove(origin);
            return;
        }
        buffer.put(origin.immutable(), now + 500L + new Random(now).nextInt(100));
        for (var player : level.players()) if (player.distanceToSqr(Vec3.atCenterOf(origin)) <= 64 * 64) NetworkManager.sendToPlayer(player, payload);
    }

    public static void clearLevel(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("VisNet effect cleanup requires the server thread");
        EFFECTS.remove(level);
    }
    public static void clearServer(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("VisNet effect cleanup requires the server thread");
        EFFECTS.keySet().removeIf(level -> level.getServer() == server);
    }
}

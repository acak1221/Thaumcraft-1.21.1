package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** S2C only. No speculative client channel, target request, harvest or vis authority. */
public final class ExcavationFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private ExcavationFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(ExcavationFxPayload.TYPE, ExcavationFxPayload.STREAM_CODEC);
    }
    public static void initClient(Consumer<ExcavationFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ExcavationFxPayload.TYPE, ExcavationFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())
                            || player.distanceToSqr(payload.origin()) > 64 * 64
                            || !player.level().hasChunkAt(BlockPos.containing(payload.origin()))
                            || !player.level().hasChunkAt(BlockPos.containing(payload.target()))) return;
                    effects.accept(payload);
                }));
    }

    /** Only the funded server held-tick path calls this; completed=true means already paid. */
    public static void send(ServerPlayer caster, Vec3 origin, Vec3 target, BlockPos blockPos, int crack,
            boolean impact, boolean completed, boolean success) {
        if (!caster.server.isSameThread()) throw new IllegalStateException("Excavation effects require the server thread");
        if (caster.isRemoved() || !ExcavationFxPayload.validGeometry(origin, target) || caster.getEyePosition().distanceToSqr(origin) > 4) return;
        var level = caster.serverLevel();
        var payload = new ExcavationFxPayload(level.dimension().location(), caster.getId(), origin, target, blockPos, crack, impact, completed, success);
        // Includes caster. Native break feedback is owned by ExcavationHarvest,
        // never a duplicated destroy-block packet/event sent from this channel.
        for (ServerPlayer observer : level.players()) if (observer.distanceToSqr(origin) <= 64 * 64) NetworkManager.sendToPlayer(observer, payload);
    }
}

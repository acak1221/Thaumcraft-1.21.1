package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.tiles.TileInfusionMatrix;

/** No client class references or C2S receiver in the common infusion-effects route. */
public final class InfusionFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private InfusionFxNetworking() {}

    public static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(InfusionFxPayload.TYPE, InfusionFxPayload.STREAM_CODEC);
        }
    }

    public static void initClient(Consumer<InfusionFxPayload> effects) {
        Objects.requireNonNull(effects, "effects");
        if (clientInitialized) return;
        clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, InfusionFxPayload.TYPE, InfusionFxPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    var player = context.getPlayer();
                    if (player == null || !player.level().dimension().location().equals(payload.dimension())
                            || !player.level().hasChunkAt(payload.matrix())
                            || !(player.level().getBlockEntity(payload.matrix()) instanceof TileInfusionMatrix)) return;
                    effects.accept(payload);
                }));
    }

    public static void send(TileInfusionMatrix matrix, int event, BlockPos target, int data) {
        if (!(matrix.getLevel() instanceof ServerLevel level)) return;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Infusion effects require the server thread");
        BlockPos pos = matrix.getBlockPos();
        if (matrix.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != matrix) return;
        var payload = new InfusionFxPayload(level.dimension().location(), pos, event, target, data);
        Vec3 center = Vec3.atCenterOf(pos);
        for (var player : level.players()) if (player.distanceToSqr(center) <= 64 * 64) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }
}

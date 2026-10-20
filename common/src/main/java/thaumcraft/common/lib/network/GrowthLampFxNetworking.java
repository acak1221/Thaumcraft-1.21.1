package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.tiles.TileArcaneLampGrowth;

public final class GrowthLampFxNetworking {
    private static boolean serverInitialized, clientInitialized;
    private GrowthLampFxNetworking() {}
    public static void initServer() {
        if (serverInitialized) return; serverInitialized = true;
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(GrowthLampFxPayload.TYPE, GrowthLampFxPayload.STREAM_CODEC);
    }
    public static void initClient(Consumer<GrowthLampFxPayload> effects) {
        if (clientInitialized) return; clientInitialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, GrowthLampFxPayload.TYPE, GrowthLampFxPayload.STREAM_CODEC, (payload, context) -> context.queue(() -> {
            var player = context.getPlayer();
            if (player != null && player.level().dimension().location().equals(payload.dimension()) && player.level().hasChunkAt(payload.target())) effects.accept(payload);
        }));
    }
    public static void send(TileArcaneLampGrowth lamp, BlockPos pos) {
        if (!(lamp.getLevel() instanceof ServerLevel level) || !lamp.current() || !level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return;
        var payload = new GrowthLampFxPayload(level.dimension().location(), pos);
        Vec3 center = Vec3.atCenterOf(pos);
        for (var player : level.players()) if (player.distanceToSqr(center) <= 32 * 32) NetworkManager.sendToPlayer(player, payload);
    }
}

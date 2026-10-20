package thaumcraft.common.lib.network;

import java.util.function.Consumer;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;

public final class RunicNetworking {
    private static boolean server,client;
    private RunicNetworking() {}
    public static void initServer() {
        if(server) return; server=true;
        if(Platform.getEnvironment()==Env.SERVER) {
            NetworkManager.registerS2CPayloadType(RunicChargePayload.TYPE,RunicChargePayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(RunicHitPayload.TYPE,RunicHitPayload.STREAM_CODEC);
        }
    }
    public static void initClient(Consumer<RunicChargePayload> charge,Consumer<RunicHitPayload> hit) {
        if(client) return; client=true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,RunicChargePayload.TYPE,RunicChargePayload.STREAM_CODEC,
                (packet,context)->context.queue(()->charge.accept(packet)));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,RunicHitPayload.TYPE,RunicHitPayload.STREAM_CODEC,
                (packet,context)->context.queue(()->hit.accept(packet)));
    }
    public static void charge(ServerPlayer player,int charge,int maximum) {
        NetworkManager.sendToPlayer(player,new RunicChargePayload(player.getId(),(short)charge,(short)maximum));
    }
    public static void hit(ServerPlayer source,int target) {
        var packet=new RunicHitPayload(source.getId(),target);
        for(ServerPlayer player:source.serverLevel().players()) if(player.distanceToSqr(source)<=64*64) NetworkManager.sendToPlayer(player,packet);
    }
}

package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import thaumcraft.common.entities.monster.EntityWisp;

public final class WispZapNetworking {
    private static boolean server,client;
    private WispZapNetworking() {}
    public static void initServer() { if(server)return;server=true;if(Platform.getEnvironment()==Env.SERVER)NetworkManager.registerS2CPayloadType(WispZapPayload.TYPE,WispZapPayload.STREAM_CODEC); }
    public static void initClient(BiConsumer<Integer,Integer> zap) { if(client)return;client=true;NetworkManager.registerReceiver(NetworkManager.Side.S2C,WispZapPayload.TYPE,WispZapPayload.STREAM_CODEC,(payload,context) -> context.queue(() -> zap.accept(payload.source(),payload.target()))); }
    public static void send(EntityWisp source,Entity target) {
        if(!(source.level() instanceof ServerLevel level))return;
        var payload=new WispZapPayload(source.getId(),target.getId());
        for(var player:level.players())if(player.distanceToSqr(source)<=32*32)NetworkManager.sendToPlayer(player,payload);
    }
}

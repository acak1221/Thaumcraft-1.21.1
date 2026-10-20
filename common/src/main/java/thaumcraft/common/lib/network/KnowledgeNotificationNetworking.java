package thaumcraft.common.lib.network;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Original path-specific RP/discovery/research feedback after native state commit and full knowledge sync. */
public final class KnowledgeNotificationNetworking {
    private static boolean server,client;
    private KnowledgeNotificationNetworking() {}
    public static void initServer() {
        if(server)return;server=true;
        if(Platform.getEnvironment()==Env.SERVER) {
            NetworkManager.registerS2CPayloadType(AspectPoolPayload.TYPE,AspectPoolPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(AspectDiscoveryPayload.TYPE,AspectDiscoveryPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ResearchNoticePayload.TYPE,ResearchNoticePayload.STREAM_CODEC);
        }
    }
    public static void initClient(Consumer<AspectPoolPayload> pool,Consumer<AspectDiscoveryPayload> discovery,Consumer<ResearchNoticePayload> research) {
        if(client)return;client=true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AspectPoolPayload.TYPE,AspectPoolPayload.STREAM_CODEC,
                (payload,context)->context.queue(()-> { if(context.getPlayer()!=null)pool.accept(payload); }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,AspectDiscoveryPayload.TYPE,AspectDiscoveryPayload.STREAM_CODEC,
                (payload,context)->context.queue(()-> { if(context.getPlayer()!=null)discovery.accept(payload); }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,ResearchNoticePayload.TYPE,ResearchNoticePayload.STREAM_CODEC,
                (payload,context)->context.queue(()-> { if(context.getPlayer()!=null)research.accept(payload); }));
    }
    private static void serverThread(ServerPlayer player) { if(!player.server.isSameThread())throw new IllegalStateException("Knowledge notifications require native server thread"); }
    public static void pool(ServerPlayer player,String aspect,int requestedAmount) {
        pool(player,new AspectPoolPayload(aspect,(short)requestedAmount,(short)KnowledgeAccess.get(player).getAspectPoolFor(aspect)));
    }
    /** Exact immutable per-award receipt keeps repeated warp grants' momentary totals instead of resampling a final aggregate. */
    public static void pool(ServerPlayer player,AspectPoolPayload prepared) { serverThread(player); NetworkManager.sendToPlayer(player,prepared); }
    public static void discover(ServerPlayer player,String aspect) { serverThread(player); NetworkManager.sendToPlayer(player,new AspectDiscoveryPayload(aspect)); }
    public static void research(ServerPlayer player,Collection<String> completed) { for(String key:completed) research(player,key); }
    public static void research(ServerPlayer player,String key) { serverThread(player); NetworkManager.sendToPlayer(player,new ResearchNoticePayload(key)); }
    /** Original successful purchase/note order: each key notice BEFORE its permanent/sticky feedback. */
    public static void completed(ServerPlayer player,Collection<String> completed) {
        for(String key:completed) { research(player,key);WarpMessageNetworking.research(player,List.of(key)); }
    }
    /** Source observation order is an action-local native witness, never a persistent world/holder cache. */
    public static void scan(ServerPlayer player,Collection<Aspect> order,Map<String,Integer> awards,Set<String> discovered) {
        for(Aspect aspect:order) {
            String tag=aspect.getTag();
            if(discovered.contains(tag))discover(player,tag);
            if(awards.containsKey(tag))pool(player,tag,awards.get(tag));
        }
    }
}

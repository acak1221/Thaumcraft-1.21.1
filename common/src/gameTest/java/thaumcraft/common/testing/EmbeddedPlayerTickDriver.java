package thaumcraft.common.testing;

import dev.architectury.event.events.common.TickEvent;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** TEST ONLY. Embedded channels have no physical transport loop. At each real
 * world POST, complete only a missing native entity-counter/game-listener
 * cycle. PLAYER_POST witnesses the genuine Player.tick, which drives the real
 * inventory/accessory/armor hooks. Never invoke an item's passive callback.
 * This is declared synthetic native ticking, not client/network acceptance. */
public final class EmbeddedPlayerTickDriver implements AutoCloseable {
    private static final Map<ServerPlayer,EmbeddedPlayerTickDriver> OWNERS=new IdentityHashMap<>();
    private final ServerLevel level;
    private final ServerPlayer player;
    private final TickEvent.ServerLevelTick post;
    private final TickEvent.Player witness;
    private int lastCounter,serverPostTicks,nativeConnectionTicks,nativePlayerTicks,previousPostWitness;
    private long lastWorldTime=Long.MIN_VALUE;
    private boolean closed,driving;
    private Throwable failure;
    private EmbeddedPlayerTickDriver(ServerLevel level,ServerPlayer player){
        this.level=level;this.player=player;lastCounter=player.tickCount;
        witness=actor->{if(!closed&&actor==player)++nativePlayerTicks;};
        post=world->{if(world==level&&!closed)runPost();};
    }
    public static EmbeddedPlayerTickDriver open(ServerLevel level,ServerPlayer player){
        var driver=new EmbeddedPlayerTickDriver(level,player);driver.requireCanonical();
        if(OWNERS.putIfAbsent(player,driver)!=null)throw new IllegalStateException("Embedded player already has a native clock owner");
        try{TickEvent.PLAYER_POST.register(driver.witness);TickEvent.SERVER_LEVEL_POST.register(driver.post);return driver;}
        catch(RuntimeException|Error failure){driver.close();throw failure;}
    }
    private void requireCanonical(){
        if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level||player.server!=level.getServer()
                ||player.level()!=level||level.getServer().getPlayerList().getPlayer(player.getUUID())!=player
                ||!player.isAlive()||player.isRemoved()||player.hasDisconnected()||player.connection==null)
            throw new AssertionError("Synthetic native driver requires its current connected living player/world/server thread");
    }
    private void runPost(){
        if(driving||lastWorldTime==level.getGameTime())return;
        driving=true;lastWorldTime=level.getGameTime();++serverPostTicks;
        try{
            requireCanonical();
            // ServerLevel owns tickCount. ServerPlayer.tick alone omits the
            // Player.tick living/inventory cycle reached through its listener.
            if(player.tickCount==lastCounter){int before=player.tickCount;level.tickNonPassenger(player);if(player.tickCount!=before+1)throw new AssertionError("Native counter fallback did not advance exactly one entity step");}
            if(nativePlayerTicks==previousPostWitness){
                int before=nativePlayerTicks;++nativeConnectionTicks;player.connection.tick();
                if(nativePlayerTicks!=before+1)throw new AssertionError("Native game listener did not witness exactly one genuine Player.tick");
            }
            // A genuine loop which already reached PLAYER_POST owns that
            // cycle. Its equipment hooks must not run again through our driver.
            lastCounter=player.tickCount;previousPostWitness=nativePlayerTicks;
        }catch(RuntimeException|Error problem){
            failure=problem;
            // A failed fixture reports through its GameTest callback/accessors,
            // not a fatal exception escaping the server's global tick event.
            close();
        }finally{driving=false;}
    }
    public void checkHealthy(){
        if(failure instanceof RuntimeException problem)throw problem;
        if(failure instanceof Error problem)throw problem;
        if(failure!=null)throw new AssertionError(failure);
    }
    public int serverPostTicks(){checkHealthy();return serverPostTicks;}
    public int nativeConnectionTicks(){checkHealthy();return nativeConnectionTicks;}
    public int nativePlayerTicks(){checkHealthy();return nativePlayerTicks;}
    public int actualPlayerPostTicks(){return nativePlayerTicks();}
    @Override public void close(){
        if(closed)return;closed=true;
        try{TickEvent.SERVER_LEVEL_POST.unregister(post);}
        finally{try{TickEvent.PLAYER_POST.unregister(witness);}finally{OWNERS.remove(player,this);}}
    }
}

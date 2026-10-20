package thaumcraft.common.items.armor;

import dev.architectury.event.events.common.PlayerEvent;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftTravelGear;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.baubles.ItemGirdleHover;
import thaumcraft.common.items.wands.foci.PortableHoleRules;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.mixin.ServerGamePacketListenerFloatAccess;

/** Real original Hover fuel/controller: server-owned physical jar and counter,
 * sender-bound activation, native flight permissions and paid final frame.
 * The client never pays a second authoritative fuel balance. */
public final class Hover {
    public static final int EFFICIENCY=360;
    public interface ClientEffects {void tick(Player player,ItemStack armor,boolean hovering);void forget(Player player);}
    private static ClientEffects effects=new ClientEffects(){public void tick(Player p,ItemStack s,boolean h){}public void forget(Player p){}};
    private static final Map<Player,State> STATES=Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean initialized;
    private record FlightGrant(boolean mayFly,boolean flying,GameType mode) {}
    private static final class State {boolean desired,executing;FlightGrant grant;State(boolean desired){this.desired=desired;}}
    private Hover() {}
    public static void setClientEffects(ClientEffects provider) {effects=Objects.requireNonNull(provider);}
    public static void initialize() {if(initialized)return;initialized=true;PlayerEvent.PLAYER_QUIT.register(Hover::forget);}
    private static ItemStack armor(Player player) {return player.getItemBySlot(EquipmentSlot.CHEST);}
    public static boolean hasHarness(Player player) {return HarnessData.isHarness(armor(player));}
    private static boolean canonical(ServerPlayer player) {return player.server.isSameThread()&&player.server.getPlayerList().getPlayer(player.getUUID())==player&&player.isAlive()&&!player.isRemoved()&&!player.hasDisconnected()&&!player.isSpectator();}
    private static State state(Player player,ItemStack armor) {return STATES.computeIfAbsent(player,ignored->new State(HarnessData.enabled(armor)));}
    public static boolean girdle(Player player) {var belt=PlayerAccessories.get(player);return !belt.isRejected()&&belt.getItem(3).getCount()==1&&belt.getItem(3).getItem() instanceof ItemGirdleHover;}
    /** The boots and mining hooks query the REAL worn, funded harness state.
     * A stale map entry, foreign player id or detached item cannot grant them. */
    public static boolean getHover(Player player) {
        ItemStack armor=armor(player);if(!HarnessData.isHarness(armor)||HarnessData.fuel(armor)==0)return false;
        try {HarnessData.charge(armor);}catch(IllegalArgumentException invalid){return false;}
        if(player.level().isClientSide)return HarnessData.enabled(armor);
        State state=STATES.get(player);return state==null?HarnessData.enabled(armor):state.desired;
    }
    /** Only the sender is accepted. The packet carries no player id or flight
     * permission. Actual survival gear, jar and source are resolved here. */
    public static boolean toggleHover(ServerPlayer player) {
        if(!canonical(player))return false;ItemStack armor=armor(player);if(!HarnessData.isHarness(armor))return false;
        try {HarnessData.jar(armor);HarnessData.charge(armor);}catch(IllegalArgumentException invalid){return false;}
        State state=state(player,armor);if(state.executing||!state.desired&&HarnessData.fuel(armor)==0)return false;
        state.desired=!state.desired;armor.set(ThaumcraftTravelGear.HOVER_ENABLED.get(),state.desired);
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();return true;
    }
    /** Called once from the shared worn/player tick after native accessories
     * and boots, before Haste. Never install another accessory tick loop. */
    public static void handleHoverArmor(Player player) {
        ItemStack armor=armor(player);
        if(player.level().isClientSide) {effects.tick(player,armor,getHover(player));return;}
        if(!(player instanceof ServerPlayer server)||!canonical(server)) {forget(player);return;}
        State state=STATES.get(player);
        if(!HarnessData.isHarness(armor)) {
            if(state!=null){state.desired=false;release(server,state,false);}return;
        }
        state=state(player,armor);
        if(state.executing)return;state.executing=true;
        try {
        // Native creative/spectator rights come from the actual game mode.
        // The original armor tick excludes creative and spends no fuel there.
        if(player.getAbilities().instabuild) {release(server,state,false);return;}
        if(player.tickCount%20==0&&PortableHoleRules.refusedIn(player.level().dimension().location().toString())
                &&(state.desired||player.getAbilities().flying)) {
            state.desired=false;armor.set(ThaumcraftTravelGear.HOVER_ENABLED.get(),false);release(server,state,true);
            server.displayClientMessage(Component.translatable("tc.break.fly"),false);
        }
        boolean changed=false;
        if(state.desired) {
            try {
                int fuel=HarnessData.fuel(armor),charge=HarnessData.charge(armor);
                if(fuel==0) {state.desired=false;release(server,state,false);player.fallDistance*=.75F;}
                else {
                    var expenditure=TravelGearRules.expend(fuel,charge,girdle(player));
                    // All physical material writes precede abilities packets.
                    armor.set(ThaumcraftTravelGear.HOVER_CHARGE.get(),expenditure.charge());
                    if(expenditure.consumed())HarnessData.setJar(armor,ItemJar.withContents(HarnessData.jar(armor),Aspect.ENERGY,expenditure.fuel()));
                    changed=true;grant(server,state);
                    if(expenditure.sustained()) {
                        ((ServerGamePacketListenerFloatAccess)server.connection).thaumcraft$setAboveGroundTickCount(0);player.fallDistance=0;
                    } else {
                        // Original last-unit expenditure returns false AFTER
                        // consuming it. Flying lasts this paid closing frame;
                        // desired=false releases the grant on the next tick.
                        state.desired=false;player.fallDistance*=.75F;
                    }
                }
            }catch(IllegalArgumentException invalid) {state.desired=false;release(server,state,false);player.fallDistance*=.75F;}
        } else {release(server,state,false);player.fallDistance*=.75F;}
        if(state.desired||armor.has(ThaumcraftTravelGear.HOVER_ENABLED.get())||!armor.getComponentsPatch().isEmpty()) {
            boolean before=HarnessData.enabled(armor);armor.set(ThaumcraftTravelGear.HOVER_ENABLED.get(),state.desired);changed|=before!=state.desired;
        }
        if(changed)player.getInventory().setChanged();
        } finally {state.executing=false;}
    }
    private static void grant(ServerPlayer player,State state) {
        var abilities=player.getAbilities();if(state.grant==null)state.grant=new FlightGrant(abilities.mayfly,abilities.flying,player.gameMode.getGameModeForPlayer());
        boolean changed=!abilities.mayfly||!abilities.flying;abilities.mayfly=true;abilities.flying=true;if(changed)player.onUpdateAbilities();
    }
    private static void release(ServerPlayer player,State state,boolean outerLands) {
        var grant=state.grant;state.grant=null;var abilities=player.getAbilities();
        if(grant==null) {if(outerLands&&abilities.flying){abilities.flying=false;player.onUpdateAbilities();}return;}
        // A game-mode transition installs its own intrinsic abilities. Do not
        // revoke creative/spectator permissions using an older survival frame.
        if(player.gameMode.getGameModeForPlayer()!=grant.mode()||abilities.instabuild||player.isSpectator())return;
        boolean flying=!outerLands&&grant.flying(),changed=abilities.mayfly!=grant.mayFly()||abilities.flying!=flying;
        abilities.mayfly=grant.mayFly();abilities.flying=flying;if(changed)player.onUpdateAbilities();
    }
    public static float miningSpeed(Player player,float nativeSpeed) {return !player.onGround()&&getHover(player)?nativeSpeed*5F:nativeSpeed;}
    /** Ordinary player saves must not turn this temporary paid equipment grant
     * into a permanent native mayfly permission after restart/crash. The saved
     * harness mode still restores through a newly paid worn tick on login. */
    public static void saveIntrinsicFlight(Player player,CompoundTag tag) {
        State state=STATES.get(player);if(state==null||state.grant==null||!(player instanceof ServerPlayer server))return;
        FlightGrant grant=state.grant;if(server.gameMode.getGameModeForPlayer()!=grant.mode()||player.getAbilities().instabuild||player.isSpectator())return;
        if(tag.contains("abilities",10)) {CompoundTag abilities=tag.getCompound("abilities");abilities.putBoolean("mayfly",grant.mayFly());abilities.putBoolean("flying",grant.flying());}
    }
    public static void forget(Player player) {
        State state=STATES.remove(player);if(state!=null&&player instanceof ServerPlayer server)release(server,state,false);
        TravellerBoots.restoreStep(player);if(player.level().isClientSide)effects.forget(player);
    }
}

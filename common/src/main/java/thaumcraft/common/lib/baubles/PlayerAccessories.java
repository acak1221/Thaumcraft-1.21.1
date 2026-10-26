package thaumcraft.common.lib.baubles;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.common.items.baubles.ItemAmuletVis;
import thaumcraft.common.lib.network.AccessoryNetworking;
import thaumcraft.common.tiles.TileVisRelay;

/** Native ownership/lifecycle adapter for the original Baubles four-slot inventory. */
public final class PlayerAccessories {
    private static boolean initialized;
    private static final ThreadLocal<AmuletCharge> CHARGE=new ThreadLocal<>();
    private static final ThreadLocal<Player> WORN=new ThreadLocal<>();
    private record AmuletCharge(ServerPlayer player, PlayerAccessoryInventory inventory, int slot, ItemStack stack,
            ItemAmuletVis amulet, thaumcraft.common.items.baubles.VisAmuletData before, TileVisRelay relay, Aspect aspect, int units) {}
    private PlayerAccessories() {}
    public static PlayerAccessoryInventory get(Player player) { return ((AccessoryHolder)player).thaumcraft$accessories(); }
    public static void initialize() {
        if(initialized)return;initialized=true;
        PlayerEvent.PLAYER_JOIN.register(AccessoryNetworking::sync);
        PlayerEvent.PLAYER_QUIT.register(TileVisRelay::forgetPlayer);
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STOPPED.register(TileVisRelay::forgetServer);
        PlayerEvent.PLAYER_CLONE.register((oldPlayer,newPlayer,wonGame) -> {
            // Native deaths have already emptied non-kept slots. Move remaining ownership, never alias/copy live stacks.
            PlayerAccessoryInventory old=get(oldPlayer), next=get(newPlayer);
            next.loadAttachment(old.saveAttachment(oldPlayer.registryAccess()),newPlayer.registryAccess());
            old.load(empty(),oldPlayer.registryAccess());
        });
        PlayerEvent.PLAYER_RESPAWN.register((player,wonGame,reason) -> AccessoryNetworking.sync(player));
        // A dimension transition retains the ServerPlayer but replaces the LocalPlayer.
        // The unchanged server inventory must therefore bypass syncChanged's last-sent cache.
        PlayerEvent.CHANGE_DIMENSION.register((player,oldLevel,newLevel) -> AccessoryNetworking.sync(player));
        // The port's own convenience: a bauble used from the hand goes into the first free slot of its kind,
        // as the modern accessory mods do; the menu (key B, or the button by the player in the inventory)
        // stays the way to take one off or to replace a worn one.
        dev.architectury.event.events.common.InteractionEvent.RIGHT_CLICK_ITEM.register((player,hand) -> {
            ItemStack held=player.getItemInHand(hand);
            int slot=freeSlotFor(player,held);
            if(slot<0)return dev.architectury.event.CompoundEventResult.pass();
            if(player instanceof ServerPlayer server) {
                get(server).setItem(slot,held.copyWithCount(1));
                held.consume(1,server);
                server.level().playSound(null,server.getX(),server.getY(),server.getZ(),
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC.value(),server.getSoundSource(),1F,1F);
                AccessoryNetworking.syncChanged(server);
            }
            return dev.architectury.event.CompoundEventResult.interruptTrue(player.getItemInHand(hand));
        });
    }

    /**
     * The first empty slot that takes the stack, or -1. A focus pouch keeps its own use (it opens), and
     * a rejected attachment takes nothing.
     */
    public static int freeSlotFor(Player player,ItemStack stack) {
        if(stack.isEmpty()||!(stack.getItem() instanceof IBauble)||stack.getItem() instanceof thaumcraft.common.items.wands.ItemFocusPouch
                ||player.isSpectator())return -1;
        PlayerAccessoryInventory inventory=get(player);
        for(int slot=0;slot<4;slot++)if(inventory.getItem(slot).isEmpty()&&inventory.canPlaceItem(slot,stack))return slot;
        return -1;
    }
    private static CompoundTag empty() { CompoundTag tag=new CompoundTag();tag.put("Items",new net.minecraft.nbt.ListTag());return tag; }
    public static void tick(Player player) {
        PlayerAccessoryInventory inventory=get(player);
        if(inventory.isRejected())return;
        if(WORN.get()!=null)return;
        WORN.set(player);
        try {
            for(int slot=0;slot<4;slot++) {
                ItemStack stack=inventory.getItem(slot);
                if(!stack.isEmpty() && stack.getItem() instanceof IBauble bauble) bauble.onWornTick(stack,player);
            }
        } finally { WORN.remove(); }
        if(player instanceof ServerPlayer server) AccessoryNetworking.syncChanged(server);
    }
    public static void save(Player player,CompoundTag tag) { tag.put("thaumcraft:accessories",get(player).saveAttachment(player.registryAccess())); }
    public static void load(Player player,CompoundTag tag) {
        if(tag.contains("thaumcraft:accessories"))get(player).loadAttachment(tag.get("thaumcraft:accessories"),player.registryAccess());
    }
    public static void dropOnDeath(Player player) {
        if(player.level().isClientSide || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))return;
        var inventory=get(player);
        for(int slot=0;slot<4;slot++) {
            ItemStack stack=inventory.removeItem(slot,1);if(stack.isEmpty())continue;
            ItemEntity drop=new ItemEntity(player.level(),player.getX(),player.getY()+player.getEyeHeight(),player.getZ(),stack);
            drop.setPickUpDelay(40);
            float speed=player.getRandom().nextFloat()*.5F,angle=player.getRandom().nextFloat()*(float)Math.PI*2F;
            drop.setDeltaMovement(-Mth.sin(angle)*speed,.20000000298023224D,Mth.cos(angle)*speed);
            if(!player.level().addFreshEntity(drop)) inventory.setItem(slot,stack);
        }
        if(player instanceof ServerPlayer server)AccessoryNetworking.syncChanged(server);
    }
    public static void chargeAmulet(Player entity,ItemStack stack,ItemAmuletVis amulet) {
        if(!(entity instanceof ServerPlayer player)||WORN.get()!=player||player.tickCount%5!=0||CHARGE.get()!=null)return;
        var inventory=get(player);int slot=-1;
        for(int index=0;index<4;index++)if(inventory.getItem(index)==stack){slot=index;break;}
        if(slot<0)return;
        TileVisRelay relay=TileVisRelay.nearbyPlayer(player);if(relay==null)return;
        for(Aspect aspect:Aspect.getPrimalAspects()) {
            int request=Math.min(5,amulet.getMaxVis(stack)-amulet.getVis(stack,aspect));if(request<=0)continue;
            CHARGE.set(new AmuletCharge(player,inventory,slot,stack,amulet,amulet.data(stack),relay,aspect,request));
            try { relay.consumeVis(aspect,request); } finally { CHARGE.remove(); }
            if(inventory.getItem(slot)!=stack)break;
        }
    }
    /** Only the actual worn tick prepares this exact player/slot/stack/component receive frame. */
    public static boolean hasPreparedAmuletCharge(TileVisRelay relay) { var frame=CHARGE.get();return frame!=null&&frame.relay()==relay; }
    public static boolean readyForAmuletGrant(TileVisRelay relay,Aspect aspect,int units) {
        var frame=CHARGE.get();
        return frame!=null&&WORN.get()==frame.player()&&frame.relay()==relay&&frame.aspect()==aspect&&frame.units()==units&&units>0
                &&frame.player().server.isSameThread()&&frame.player().isAlive()&&!frame.player().isSpectator()
                &&frame.player().server.getPlayerList().getPlayer(frame.player().getUUID())==frame.player()
                &&frame.player().level()==relay.getLevel()&&frame.inventory().getItem(frame.slot())==frame.stack()
                &&frame.stack().getCount()==1&&frame.amulet().data(frame.stack()).equals(frame.before())
                &&TileVisRelay.nearbyPlayer(frame.player())==relay;
    }
    /** Executor credit occurs before source/route pulse callbacks can detach the item. */
    public static void acceptPreparedAmuletGrant(TileVisRelay relay,Aspect aspect,int units) {
        thaumcraft.api.visnet.VisNetHandler.requireAmuletGrant(relay,aspect,units);
        var frame=CHARGE.get();
        if(frame==null||frame.relay()!=relay||frame.aspect()!=aspect||units<1||units>frame.units()
                ||frame.inventory().getItem(frame.slot())!=frame.stack()||!frame.amulet().data(frame.stack()).equals(frame.before()))
            throw new IllegalStateException("Amulet receive frame changed during callback-free source debit");
        frame.amulet().storeVis(frame.stack(),aspect,frame.before().amount(aspect.getTag())+units);
        frame.inventory().setChanged();
    }
}

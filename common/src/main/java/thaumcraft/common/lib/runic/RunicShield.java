package thaumcraft.common.lib.runic;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.WandManager;
import thaumcraft.common.lib.network.RunicNetworking;

/** Original EventHandlerRunic, native armor and four accessory slots. All state is server-owned; client has only a separate HUD snapshot. */
public final class RunicShield {
    private static final Map<MinecraftServer, ServerState> SERVERS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean initialized;
    private static final class ServerState {
        final Map<Integer, State> players = new HashMap<>();
        boolean dirty = true;
        int rechargeDelay;
    }
    private static final class State {
        int charge, max, last = -1, charged, healing, kinetic, emergency;
        boolean present;
        long nextCycle, nextKinetic, nextHealing, nextEmergency;
    }
    public record Snapshot(int charge, int maximum) {}
    private RunicShield() {}
    public static void initialize() {
        if (initialized) return; initialized = true;
        LifecycleEvent.SERVER_STOPPED.register(server -> SERVERS.remove(server));
        PlayerEvent.PLAYER_QUIT.register(player -> {
            var state = SERVERS.get(player.server);
            if (state != null) state.players.remove(player.getId());
        });
    }
    private static ServerState server(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Runic state requires server thread");
        return SERVERS.computeIfAbsent(player.server, ignored -> new ServerState());
    }
    private static State state(ServerPlayer player, ServerState server) { return server.players.computeIfAbsent(player.getId(), ignored -> new State()); }
    public static void markDirty(LivingEntity wearer) {
        if (wearer instanceof ServerPlayer player) server(player).dirty = true;
    }
    public static Snapshot view(ServerPlayer player) {
        State state = state(player, server(player)); return new Snapshot(state.charge, state.max);
    }
    public static void tick(Player entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide) return;
        var server = server(player); var state = state(player, server);
        if (server.dirty || player.tickCount % 40 == 0) {
            server.dirty = false;
            int maximum = 0;
            for (var armor : player.getInventory().armor) maximum += RunicArmor.finalCharge(armor);
            state.charged=state.healing=state.kinetic=state.emergency=0;
            var accessories=thaumcraft.common.lib.baubles.PlayerAccessories.get(player);
            for(int slot=0;slot<4;slot++) {
                var stack=accessories.getItem(slot);
                maximum+=RunicArmor.finalCharge(stack);
                if(stack.getItem() instanceof thaumcraft.common.items.baubles.ItemRunicBauble item) switch(item.upgrade()) {
                    case CHARGED -> ++state.charged;
                    case HEALING -> ++state.healing;
                    case KINETIC -> ++state.kinetic;
                    case EMERGENCY -> ++state.emergency;
                    default -> {}
                }
            }
            state.max = maximum; state.present = maximum > 0;
            if (!state.present) { state.charge = 0; RunicNetworking.charge(player,0,0); }
            else if (state.charge > maximum) { state.charge = maximum; RunicNetworking.charge(player,maximum,maximum); }
        }
        // Original delay is global and decreases ON EACH player's update, not once per world tick.
        if (server.rechargeDelay > 0) { --server.rechargeDelay; return; }
        if (!state.present) return;
        long now = System.currentTimeMillis();
        if (state.charge > state.max) state.charge = state.max;
        else if (state.charge < state.max && state.nextCycle < now && WandManager.consumeVisFromInventory(player,
                new AspectList().add(Aspect.AIR, RunicSettings.current().cost()).add(Aspect.EARTH, RunicSettings.current().cost()))) {
            state.nextCycle = now + RunicRules.rechargeInterval(RunicSettings.current().rechargeMillis(),state.charged); ++state.charge;
        }
        if (state.last != state.charge) {
            RunicNetworking.charge(player,state.charge,state.max); state.last = state.charge;
        }
    }
    public static float onHurt(LivingEntity victim, DamageSource source, float damage) {
        if (!(victim instanceof ServerPlayer player) || victim.level().isClientSide || damage <= 0
                || source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.STARVE)) return damage;
        var server = server(player); var state = server.players.get(player.getId());
        if (state == null || !state.present || state.charge <= 0) return damage;
        // Original func_76346_g is the indirect attacker (official rq/j),
        // whereas the Fortress mask branch uses func_76364_f/direct source.
        int target = source.getEntity() == null ? -1 : source.getEntity().getId();
        if (source.is(DamageTypes.FALL)) target = -2;
        if (source.is(DamageTypes.FALLING_BLOCK)) target = -3;
        RunicNetworking.hit(player,target);
        var paid = RunicRules.absorb(state.charge,damage);
        state.charge = paid.charge();
        long now=System.currentTimeMillis();
        if(state.charge<=0 && state.kinetic>0 && state.nextKinetic<now) {
            state.nextKinetic=now+20000L;
            player.serverLevel().explode(player,player.getX(),player.getY()+player.getBbHeight()/2F,player.getZ(),1.5F+state.kinetic*.5F,false,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        }
        if(state.charge<=0 && state.healing>0 && state.nextHealing<now) {
            state.nextHealing=now+20000L;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,240,state.healing));
            player.level().playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","runicshieldeffect")),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }
        if(state.charge<=0 && state.emergency>0 && state.nextEmergency<now) {
            state.nextEmergency=now+60000L;
            state.charge=Math.min(state.max,8*state.emergency);server.dirty=true;
            player.level().playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","runicshieldcharge")),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        }
        if (state.charge <= 0) server.rechargeDelay = RunicSettings.current().delayTicks();
        RunicNetworking.charge(player,state.charge,state.max);
        return paid.damage();
    }
    /** Original ResearchManager741 persists current charge, never clock/cooldowns/max or the global delay. */
    public static void save(Player entity, CompoundTag tag) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide) return;
        var server = server(player); var state = server.players.get(player.getId());
        if (state != null) tag.putInt("thaumcraft:runic_charge",state.charge);
    }
    public static void load(Player entity, CompoundTag tag) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide || !tag.contains("thaumcraft:runic_charge",Tag.TAG_INT)) return;
        var server = server(player); state(player,server).charge = tag.getInt("thaumcraft:runic_charge"); server.dirty = true;
    }
}

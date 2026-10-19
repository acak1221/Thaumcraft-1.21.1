package thaumcraft.fabric.clienttest;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** STAGED TEST ONLY. No XP/orb/player setters, native calls, world loads or event
 * unregistration. Static mixin bridges are inert without this exact owned actor.
 * Failures are recorded, never thrown into ExperienceOrb.playerTouch. */
public final class FocalXpPickupObserver implements AutoCloseable {
    private static volatile FocalXpPickupObserver active;
    private static final ThreadLocal<ArrayDeque<Touch>> TOUCHES = ThreadLocal.withInitial(ArrayDeque::new);
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final ServerLevel level;
    private final AABB area;
    private final Consumer<Throwable> failure;
    private final List<Map<String,Object>> rows = new CopyOnWriteArrayList<>();
    private final Map<UUID,Orb> previous = new LinkedHashMap<>();
    private final Map<UUID,Integer> creditedByUuid = new LinkedHashMap<>();
    private long credits, touches;
    private boolean closed, inert;

    private record Orb(int value, int count, boolean removed, String position) {
        long mass() { return (long)value * Math.max(0, count); }
    }
    private record Xp(int level, int total, float progress) {}
    private record Touch(FocalXpPickupObserver owner, ExperienceOrb orb, Orb before, Xp xp, long tick) {}

    FocalXpPickupObserver(ServerPlayer player, AABB area, Consumer<Throwable> failure) {
        if (!player.server.isSameThread() || active != null) throw new IllegalStateException("One owned server-thread XP observer required");
        this.player=player; server=player.server; level=player.serverLevel();this.area=area; this.failure=failure;
        if (!player.serverLevel().getEntitiesOfClass(ExperienceOrb.class, area).isEmpty()) throw new IllegalStateException("Refuse inherited scene XP; never erase it");
        active=this;
    }

    List<Map<String,Object>> rows() { return rows; }
    long credits() { return credits; }
    long touches() { return touches; }
    Map<String,Integer> creditedUuids() {
        var out=new LinkedHashMap<String,Integer>(); creditedByUuid.forEach((id,value)->out.put(id.toString(),value)); return Map.copyOf(out);
    }

    void added(ExperienceOrb orb) {
        try { if (owns(orb))row("native XP ADD",orb,snapshot(orb),Map.of()); }
        catch (Throwable error) { fail(error); }
    }

    /** Called at EVERY native SERVER_POST. Count increases/UUID disappearances
     * are retained explicitly: merging is not mistaken for pickup or minting. */
    long post(long expectedEmitted) {
        if (inert || closed) return -1;
        try {
            if (!server.isSameThread()) throw new IllegalStateException("XP POST must remain on owner server thread");
            if (player.level()!=level || player.server!=server) throw new IllegalStateException("Owned XP actor/level/server lifetime changed");
            if (!TOUCHES.get().isEmpty()) throw new IllegalStateException("Native playerTouch failed to RETURN; mark inert, never repair gameplay");
            var now=new LinkedHashMap<UUID,Orb>(); long mass=0;
            for (ExperienceOrb orb:player.serverLevel().getEntitiesOfClass(ExperienceOrb.class,area)) {
                Orb value=snapshot(orb);
                if (value.value()<=0 || value.count()<=0 || value.removed()) throw new IllegalStateException("Invalid live native XP orb");
                now.put(orb.getUUID(),value); mass+=value.mass();
                Orb old=previous.get(orb.getUUID());
                if (!value.equals(old)) row(old==null?"POST first actual UUID":"POST native value/Count/position change (merge or motion)",orb,value,Map.of("previous",old==null?"UNSEEN":old.toString()));
            }
            for (var entry:previous.entrySet()) if (!now.containsKey(entry.getKey())) rows.add(Map.of(
                    "event","POST UUID gone (pickup OR merge; never credited from disappearance)","tick",player.level().getGameTime(),
                    "UUID",entry.getKey().toString(),"previous_value_Count",List.of(entry.getValue().value(),entry.getValue().count()),
                    "actual_credit_for_UUID",creditedByUuid.getOrDefault(entry.getKey(),0)));
            if (mass+credits!=expectedEmitted) throw new IllegalStateException("Native furnace XP mass+actual pickup credit != emitted: "+mass+"+"+credits+" != "+expectedEmitted);
            previous.clear(); previous.putAll(now); return mass;
        } catch (Throwable error) { fail(error); return -1; }
    }

    public static void touchHead(ExperienceOrb orb, Player actor) {
        FocalXpPickupObserver owner=active;
        if (owner==null) return;
        try {
            if (actor!=owner.player || !owner.owns(orb))return;
            if (!owner.server.isSameThread()) throw new IllegalStateException("Native XP touch on wrong thread");
            TOUCHES.get().push(new Touch(owner,orb,snapshot(orb),xp(owner.player),owner.player.level().getGameTime()));
        } catch (Throwable error) { owner.fail(error); }
    }

    public static void touchReturn(ExperienceOrb orb, Player actor) {
        FocalXpPickupObserver owner=active;
        if (owner==null || actor!=owner.player) return;
        try {
            var stack=TOUCHES.get();
            if (stack.isEmpty() || stack.peek().owner()!=owner || stack.peek().orb()!=orb) return;
            Touch before=stack.pop(); if (stack.isEmpty()) TOUCHES.remove();
            if (owner.inert || owner.closed) return;
            Orb after=snapshot(orb); Xp xp=xp(owner.player); owner.touches++;
            int consumed=before.before().count()-Math.max(0,after.count());
            int gained=xp.total()-before.xp().total();
            if (consumed==0 && gained==0 && xp.equals(before.xp())) return; // Native delay/refusal, no credit.
            if (consumed!=1 || gained!=before.before().value()) throw new IllegalStateException("XP native touch diverted/repaired/mismatched; no fabricated pickup credit");
            owner.credits+=gained; owner.creditedByUuid.merge(orb.getUUID(),gained,Integer::sum);
            owner.row("actual playerTouch HEAD/RETURN pickup",orb,after,Map.of(
                    "HEAD_tick",before.tick(),"HEAD_value_Count",List.of(before.before().value(),before.before().count()),
                    "HEAD_XpLevel_Total_Progress",List.of(before.xp().level(),before.xp().total(),before.xp().progress()),
                    "RETURN_XpLevel_Total_Progress",List.of(xp.level(),xp.total(),xp.progress()),
                    "native_awarded_points",gained,"cumulative_actual_points",owner.credits));
        } catch (Throwable error) { owner.fail(error); }
    }

    private boolean owns(ExperienceOrb orb) {
        return !closed && !inert && player.level()==level && player.server==server && orb.level()==level && area.intersects(orb.getBoundingBox());
    }
    private static Xp xp(ServerPlayer player) { return new Xp(player.experienceLevel,player.totalExperience,player.experienceProgress); }
    private static Orb snapshot(ExperienceOrb orb) {
        CompoundTag tag=new CompoundTag(); orb.saveWithoutId(tag);
        return new Orb(orb.getValue(),tag.getInt("Count"),orb.isRemoved(),orb.position().toString());
    }
    private void row(String event,ExperienceOrb orb,Orb state,Map<String,?> extra) {
        var row=new LinkedHashMap<String,Object>(); row.put("event",event); row.put("tick",player.level().getGameTime());
        row.put("UUID",orb.getUUID().toString()); row.put("actual_value",state.value()); row.put("actual_NBT_Count",state.count());
        row.put("actual_removed",state.removed()); row.put("actual_position",state.position()); row.putAll(extra); rows.add(Map.copyOf(row));
    }
    private void fail(Throwable error) {
        if (!inert) { inert=true;try { failure.accept(error); }catch(Throwable ignored) { /* Never escape into a native gameplay callback. */ } }
    }
    @Override public void close() {
        closed=true; if (active==this) active=null;
        if (server.isSameThread()) TOUCHES.remove();
    }
}

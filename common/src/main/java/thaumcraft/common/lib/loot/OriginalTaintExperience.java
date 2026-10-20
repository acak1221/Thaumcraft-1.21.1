package thaumcraft.common.lib.loot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.utils.RejectedEntitySpawn;
import thaumcraft.common.mixin.EntityLevelCallbackAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;

/** Original LivingBase deathTime20 XP for opt-in TC4 tainted actors only.
 * No vanilla/global XP patch, no saved eligibility/reward or free enqueue API. */
public final class OriginalTaintExperience {
    private static final EquipmentSlot[] ORIGINAL_SLOTS={EquipmentSlot.MAINHAND,EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    private static final Map<Mob,boolean[]> EQUIPMENT=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Set<Mob>> ACTIVE=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
    private OriginalTaintExperience(){}

    /** Native equipment loot clears a dropped slot; original sw.a did not.
     * Remember only its presence, never XP eligibility, reward or RNG. Invoke
     * immediately before the native superclass equipment-drop operation. */
    public static void rememberEquipment(Mob actor){
        boolean[] present=new boolean[ORIGINAL_SLOTS.length];
        for(int i=0;i<present.length;i++)present[i]=!actor.getItemBySlot(ORIGINAL_SLOTS[i]).isEmpty();
        EQUIPMENT.putIfAbsent(actor,present);
    }
    /** Original five slots, no offhand/new enchantment XP processing. Base0
     * suppresses ALL equipment bonuses, as original EntityLiving.sw.e did. */
    public static int originalReward(Mob actor,int base,ToDoubleFunction<EquipmentSlot> chance){
        if(base<=0)return base;
        int total=base;boolean[] original=EQUIPMENT.get(actor);
        for(int i=0;i<ORIGINAL_SLOTS.length;i++){
            EquipmentSlot slot=ORIGINAL_SLOTS[i];
            if((!actor.getItemBySlot(slot).isEmpty()||original!=null&&original[i])&&chance.applyAsDouble(slot)<=1)total+=1+actor.getRandom().nextInt(3);
        }return total;
    }
    /** Return true when this native death tick is handled, including reentry.
     * Own actor overrides suppress immediate dropExperience and otherwise call
     * super.tickDeath when this returns false. Recent-hit gate is evaluated at
     * tick20, BEFORE native decrements it, rather than snapshotted at die(). */
    public static boolean finishDeathTick(Mob actor,BooleanSupplier recentOrPlayer,IntSupplier reward){
        if(ACTIVE.get().contains(actor))return true;
        if(actor.deathTime!=19||!(actor.level()instanceof ServerLevel level))return false;
        if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level
                ||actor.isRemoved()||actor.isAlive()||!level.hasChunkAt(actor.blockPosition())||level.getEntity(actor.getUUID())!=actor)return false;
        ACTIVE.get().add(actor);
        // Original increments to20 BEFORE publishing XP. A reentrant save has
        // no deathTime19 source that could award its already paid stock again.
        actor.deathTime=20;
        try{
            if(!actor.wasExperienceConsumed()&&recentOrPlayer.getAsBoolean()&&actor.shouldDropExperience()
                    &&level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)){
                int remaining=reward.getAsInt();Vec3 position=actor.position();
                while(remaining>0){
                    if(level.getServer().getLevel(level.dimension())!=level||actor.level()!=level
                            ||!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position)))break;
                    int value=ExperienceOrb.getExperienceValue(remaining);remaining-=value;
                    ExperienceOrb orb=new PaidOrb(level,position,value);
                    // Public native constructor uses the ORB's RNG, not WORLD.
                    // Replace its scatter with original Math.random float
                    // rounding; no native award/nextInt40 merge roll is called.
                    orb.setYRot((float)(Math.random()*360));
                    orb.setDeltaMovement((float)(Math.random()*(double).2F-(double).1F)*2F,
                            (float)(Math.random()*.2)*2F,(float)(Math.random()*(double).2F-(double).1F)*2F);
                    var snapshot=RejectedEntitySpawn.snapshotBefore(level,orb);
                    boolean added=level.addFreshEntity(orb); // One original admission per canonical split; no retries/refunds.
                    if(!added&&level.getEntity(orb.getUUID())!=orb
                            &&((EntityLevelCallbackAccess)orb).thaumcraft$getLevelCallback()==EntityInLevelCallback.NULL){
                        orb.discard();RejectedEntitySpawn.releaseRejected(level,orb,snapshot,false);
                    }
                }
            }
        }finally{
            try{
                EQUIPMENT.remove(actor);
                // Match native server's exact terminal event/removal while
                // keeping original deathTime==20 and the claimed reentry fence.
                actor.deathTime=20;
                if(!actor.isRemoved()){
                    if(level.getServer().getLevel(level.dimension())==level&&actor.level()==level&&level.hasChunkAt(actor.blockPosition()))level.broadcastEntityEvent(actor,(byte)60);
                    actor.remove(Entity.RemovalReason.KILLED);
                }
            }finally{ACTIVE.get().remove(actor);}
        }
        return true;
    }
    /** This saves/spawns as the ordinary minecraft:experience_orb, with native
     * value/count/merge/pickup. Only the pre-admission/stale callback is fenced. */
    private static final class PaidOrb extends ExperienceOrb{
        PaidOrb(ServerLevel level,Vec3 pos,int value){super(level,pos.x,pos.y,pos.z,value);}
        @Override public void playerTouch(Player player){if(!isRemoved()&&level()instanceof ServerLevel server&&server.getEntity(getUUID())==this)super.playerTouch(player);}
    }
}

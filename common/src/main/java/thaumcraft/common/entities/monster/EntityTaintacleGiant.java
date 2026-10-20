package thaumcraft.common.entities.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 Giant Taintacle: a 125 health, nine damage elder taintacle that enrages above 35 damage,
 * heals itself and drops its special eldritch object when no other giant is close by.
 */
public class EntityTaintacleGiant extends EntityTaintacle implements ITaintedMob {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(EntityTaintacleGiant.class,EntityDataSerializers.INT);

    public EntityTaintacleGiant(EntityType<? extends EntityTaintacleGiant> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.TAINTACLE_GIANT.experience();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityTaintacle.createAttributes()
                .add(Attributes.MAX_HEALTH,EldritchMobRules.TAINTACLE_GIANT.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.TAINTACLE_GIANT.attackDamage());
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(ANGER,0); }

    public int getAnger() { return entityData.get(ANGER); }
    public void setAnger(int anger) { entityData.set(ANGER,anger); }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("Anger",getAnger()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("Anger"))setAnger(tag.getInt("Anger"));
    }

    /** Original giant taintacle heals itself every thirty ticks and counts its anger down. */
    @Override public void tick() {
        super.tick();
        if(getAnger()>0)setAnger(getAnger()-1);
        if(level().isClientSide) {
            if(getRandom().nextInt(15)==0&&getAnger()>0) {
                double dx=getRandom().nextGaussian()*.02D,dy=getRandom().nextGaussian()*.02D,dz=getRandom().nextGaussian()*.02D;
                if(level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        getX()+getRandom().nextFloat()*getBbWidth()-.5F*getBbWidth(),getBoundingBox().maxY+getRandom().nextFloat()*.5F,
                        getZ()+getRandom().nextFloat()*getBbWidth()-.5F*getBbWidth(),1,dx,dy,dz,0D);
            }
        } else if(tickCount%30==0)heal(1F);
    }

    /** Original giant taintacle caps its damage at 35 and enrages for 200 ticks. */
    @Override public boolean hurt(DamageSource source,float damage) {
        if(!level().isClientSide&&EldritchMobRules.giantEnrages(damage,getAnger())) {
            var effects=EldritchMobRules.giantEnrageEffects(damage);
            addEffect(new MobEffectInstance(MobEffects.REGENERATION,EldritchMobRules.GIANT_ANGER_TICKS,effects.regeneration()));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,EldritchMobRules.GIANT_ANGER_TICKS,effects.strength()));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,EldritchMobRules.GIANT_ANGER_TICKS,effects.speed()));
            setAnger(EldritchMobRules.GIANT_ANGER_TICKS);
            if(source.getEntity() instanceof Player player)player.displayClientMessage(Component.translatable("tc.boss.enrage"),false);
        }
        if(!level().isClientSide&&damage>EldritchMobRules.GIANT_DAMAGE_CAP)damage=EldritchMobRules.giantDamageCap(damage);
        return super.hurt(source,damage);
    }

    /** Original giant taintacle drops its special eldritch object only when it is the last giant around. */
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {
        var box=new AABB(getX(),getY(),getZ(),getX(),getY(),getZ()).inflate(48,48,48);
        int nearby=0;
        for(var candidate:level.getEntitiesOfClass(EntityTaintacleGiant.class,box))if(candidate!=this)++nearby;
        if(EldritchMobRules.giantDropsSpecialObject(nearby))EntityThaumcraftBoss.dropSpecialItem(this,level,EldritchMobRules.ELDRITCH_OBJECT_ID,getBbHeight()/2F);
    }

    /** Original giant taintacle never despawns and never spawns on its own. */
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,MobSpawnType reason) { return reason==MobSpawnType.SPAWN_EGG; }
    @Override protected int decreaseAirSupply(int air) { return air; }
}

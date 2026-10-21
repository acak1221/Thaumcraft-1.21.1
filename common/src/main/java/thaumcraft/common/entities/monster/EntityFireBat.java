package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.lib.entities.OriginalLightRules;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalMobTargeting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Original five flag firebat, including the summoned variants used by the Hellbat focus. */
public final class EntityFireBat extends Monster {
    @FunctionalInterface public interface ClientEffects { void tick(EntityFireBat bat); }
    private static ClientEffects clientEffects;
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(EntityFireBat.class, EntityDataSerializers.BYTE);
    private BlockPos currentFlightTarget;
    public Player owner;
    public int damBonus;
    private int attackTime;
    private Path legacyPath;
    private LivingEntity lookTarget;
    private int lookTime;
    private float idleYaw;
    public EntityFireBat(EntityType<? extends EntityFireBat> type, Level level) {
        super(type, level);
        setIsBatHanging(true);
        // The old flight action writes forward motion after its creature AI. A modern WAIT move control would clear that input.
        moveControl = new MoveControl(this) { @Override public void tick() {} };
        jumpControl = new JumpControl(this) { @Override public void tick() {} };
    }
    public static void setClientEffects(ClientEffects effects) { clientEffects = java.util.Objects.requireNonNull(effects); }
    public static AttributeSupplier.Builder createAttributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.ATTACK_DAMAGE, 1); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(FLAGS, (byte)0); }
    private boolean flag(int bit) { return (entityData.get(FLAGS) & 1 << bit) != 0; }
    private void flag(int bit, boolean value) { byte flags=entityData.get(FLAGS); entityData.set(FLAGS,(byte)(value ? flags|1<<bit : flags&~(1<<bit))); }
    public boolean getIsBatHanging() { return flag(0); }
    public void setIsBatHanging(boolean value) { flag(0,value); }
    public boolean getIsSummoned() { return flag(1); }
    public void setIsSummoned(boolean value) { flag(1,value); getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(value ? (getIsDevil()?3:2)+damBonus : 1); }
    public boolean getIsExplosive() { return flag(2); }
    public void setIsExplosive(boolean value) { flag(2,value); }
    public boolean getIsDevil() { return flag(3); }
    public void setIsDevil(boolean value) { flag(3,value); if(value) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getIsSummoned() ? 3+damBonus : 1); }
    public boolean getIsVampire() { return flag(4); }
    public void setIsVampire(boolean value) { flag(4,value); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {}
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public float getLightLevelDependentMagicValue() { return 1; }
    @Override protected float getSoundVolume() { return .1F; }
    @Override public float getVoicePitch() { return super.getVoicePitch()*.95F; }
    @Override protected SoundEvent getAmbientSound() { return getIsBatHanging() && random.nextInt(4)!=0 ? null : SoundEvents.BAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.BAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }
    @Override public void aiStep() { if(isInWater()) hurt(damageSources().drown(),1); super.aiStep(); }
    @Override public void tick() {
        if(attackTime>0) attackTime--;
        super.tick();
        if(level().isClientSide && clientEffects!=null) clientEffects.tick(this);
        if(getIsBatHanging()) {
            setDeltaMovement(Vec3.ZERO); setPos(getX(),Mth.floor(getY())+1-getBbHeight(),getZ());
        } else setDeltaMovement(getDeltaMovement().multiply(1,.6F,1));
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        if(!level().isClientSide && getIsBatHanging()) setIsBatHanging(false);
        boolean hit=super.hurt(source,amount);
        if(hit && source.getEntity() instanceof LivingEntity attacker && attacker!=this && !hasPassenger(attacker) && getVehicle()!=attacker) setTarget(attacker);
        return hit;
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        legacyCreatureAction();
        LivingEntity target;
        if(getIsBatHanging()) {
            if(!solidAbove()) wake();
            else {
                if(random.nextInt(200)==0) setYHeadRot(random.nextInt(360));
                if(level().getNearestPlayer(this,4)!=null) wake();
            }
        } else {
            target=getTarget();
            if(target==null) {
                if(getIsSummoned()) hurt(damageSources().generic(),2);
                if(currentFlightTarget!=null && (!level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY()<1)) currentFlightTarget=null;
                if(currentFlightTarget==null || random.nextInt(30)==0 || currentFlightTarget.distSqr(new BlockPos((int)getX(),(int)getY(),(int)getZ()))<4)
                    currentFlightTarget=new BlockPos((int)getX()+random.nextInt(7)-random.nextInt(7),(int)getY()+random.nextInt(6)-2,(int)getZ()+random.nextInt(7)-random.nextInt(7));
                steer(currentFlightTarget.getX()+.5-getX(),currentFlightTarget.getY()+.1-getY(),currentFlightTarget.getZ()+.5-getZ());
                if(random.nextInt(100)==0 && solidAbove()) setIsBatHanging(true);
            } else steer(target.getX()-getX(),target.getY()+target.getEyeHeight()*.66F-getY(),target.getZ()-getZ());
            if(getTarget() instanceof Player player && player.getAbilities().invulnerable) setTarget(null);
        }
    }
    /** 1.7 EntityCreature's action precedes the custom bat steering. Keep its target/LOS order and random draws;
     * modern Path supplies collision geometry because the removed 1.7 pathfinder cannot run against a1.21 world. */
    private void legacyCreatureAction() {
        setNoActionTime(getNoActionTime()-1); // Native Mob increments unconditionally; old creature increments only in its idle branch.
        LivingEntity target=getTarget();
        if(target==null) {
            if(!getIsSummoned()) setTarget(OriginalMobTargeting.closestVulnerablePlayer(this,12));
            if(getTarget()!=null) legacyPath=getNavigation().createPath(getTarget(),0);
        } else if(target.isAlive()) { if(hasLineOfSight(target)) attackTarget(target,distanceTo(target)); }
        else setTarget(null);
        if(getTarget() instanceof net.minecraft.server.level.ServerPlayer player && player.isCreative()) setTarget(null);
        target=getTarget();
        if(target!=null && (legacyPath==null || random.nextInt(20)==0)) legacyPath=getNavigation().createPath(target,0);
        else if((legacyPath==null && random.nextInt(180)==0 || random.nextInt(120)==0) && getNoActionTime()<100) {
            float best=-99999;BlockPos chosen=null;
            for(int i=0;i<10;i++) {
                BlockPos candidate=BlockPos.containing(getX()+random.nextInt(13)-6,getY()+random.nextInt(7)-3,getZ()+random.nextInt(13)-6);
                float value=getWalkTargetValue(candidate,level());
                if(value>best) { best=value;chosen=candidate; }
            }
            if(chosen!=null) legacyPath=getNavigation().createPath(chosen,0);
        }
        setXRot(0);
        if(legacyPath==null || random.nextInt(100)==0) { legacyIdleAction();legacyPath=null;return; }
        Vec3 point=legacyPath.isDone()?null:legacyPath.getNextEntityPos(this);
        double diameter=getBbWidth()*2;
        while(point!=null && distanceToSqr(point.x,getY(),point.z)<diameter*diameter) {
            legacyPath.advance();
            if(legacyPath.isDone()) { point=null;legacyPath=null; }
            else point=legacyPath.getNextEntityPos(this);
        }
        setJumping(false);
        if(point!=null) {
            double x=point.x-getX(),z=point.z-getZ(),y=point.y-Mth.floor(getBoundingBox().minY+.5);
            float yaw=(float)(Math.atan2(z,x)*180/3.1415927410125732)-90;
            zza=(float)getAttributeValue(Attributes.MOVEMENT_SPEED);
            setYRot(getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-getYRot()),-30,30));
            if(y>0) setJumping(true);
        }
        if(target!=null) lookAt(target,30,30);
        if(horizontalCollision && legacyPath==null) setJumping(true);
        if(random.nextFloat()<.8F && (isInWater()||isInLava())) setJumping(true);
    }
    private void legacyIdleAction() {
        setNoActionTime(getNoActionTime()+1);xxa=zza=0;checkDespawn();
        if(random.nextFloat()<.02F) {
            var player=level().getNearestPlayer(this,8);
            if(player!=null) { lookTarget=player;lookTime=10+random.nextInt(20); }
            else idleYaw=(random.nextFloat()-.5F)*20;
        }
        if(lookTarget!=null) {
            lookAt(lookTarget,10,getMaxHeadXRot());
            if(lookTime--<=0 || lookTarget.isRemoved() || lookTarget.distanceToSqr(this)>64) lookTarget=null;
        } else {
            if(random.nextFloat()<.05F) idleYaw=(random.nextFloat()-.5F)*20;
            setYRot(getYRot()+idleYaw);setXRot(0);
        }
        if(isInWater()||isInLava()) setJumping(random.nextFloat()<.8F);
    }
    private boolean solidAbove() { BlockPos pos=new BlockPos(Mth.floor(getX()),(int)getY()+1,Mth.floor(getZ())); return level().getBlockState(pos).isRedstoneConductor(level(),pos); }
    private void wake() { setIsBatHanging(false); level().levelEvent(1025,new BlockPos((int)getX(),(int)getY(),(int)getZ()),0); }
    private void steer(double x,double y,double z) {
        Vec3 velocity=getDeltaMovement();
        setDeltaMovement(velocity.add((Math.signum(x)*.5-velocity.x)*.1F,(Math.signum(y)*.7F-velocity.y)*.1F,(Math.signum(z)*.5-velocity.z)*.1F));
        Vec3 motion=getDeltaMovement(); float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)-90;
        zza=.5F; setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));
    }
    private void attackTarget(LivingEntity target,float distance) {
        if(attackTime>0 || distance>=Math.max(2.5F,target.getBbWidth()*1.1F) || target.getBoundingBox().maxY<=getBoundingBox().minY || target.getBoundingBox().minY>=getBoundingBox().maxY) return;
        if(getIsSummoned()) target.lastHurtByPlayerTime=100;
        if(getIsVampire()) {
            if(owner!=null && !owner.hasEffect(MobEffects.REGENERATION)) owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION,26,1));
            heal(1);
        }
        attackTime=20;
        if(!(!getIsExplosive() && level().random.nextInt(10)!=0 || level().isClientSide || getIsDevil())) {
            target.hurtTime=0;
            level().explode(this,getX(),getY(),getZ(),1.5F+(getIsExplosive()?damBonus*.33F:0),false,Level.ExplosionInteraction.NONE);
            discard();
        } else if(getIsVampire() || level().random.nextBoolean()) {
            Vec3 motion=target.getDeltaMovement(); doHurtTarget(target); target.hurtMarked=false; target.setDeltaMovement(motion);
        } else target.igniteForSeconds(getIsSummoned()?4:2);
        playSound(SoundEvents.BAT_HURT,.5F,.9F+level().random.nextFloat()*.2F);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("BatFlags",entityData.get(FLAGS)); tag.putByte("damBonus",(byte)damBonus); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(FLAGS,tag.getByte("BatFlags")); damBonus=tag.getByte("damBonus"); }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        int looting=OriginalMobLoot.looting(level,source),count=random.nextInt(3);
        if(looting>0) count+=random.nextInt(looting+1);
        if(!getIsSummoned()) for(int i=0;i<count;i++) spawnAtLocation(Items.GUNPOWDER);
        super.dropCustomDeathLoot(level,source,recent);
        if(recent) OriginalMobLoot.rare(looting,random::nextInt); // Original inherited rare hook is empty, but still consumes this draw.
    }
    /**
     * The original class gate of the fire bat, without the world.
     *
     * <p>Oracle {@code EntityFireBat.getCanSpawnHere:333-340}: the BLOCK light of the cell is
     * rolled against {@code nextInt(7)} and the inherited spawn gate has to accept the cell.</p>
     */
    public static boolean originalSpawnGate(int blockLight,int roll,boolean peaceful) {
        return !peaceful&&OriginalLightRules.validClassLightLevel(blockLight,roll);
    }

    /**
     * Oracle EntityFireBat:333-340 - the original class gate of the fire bat reads the BLOCK
     * light of the cell and rolls it against {@code nextInt(7)}.
     *
     * <p>The original overrides {@code isValidLightLevel} with a plain TRUE
     * (oracle EntityFireBat:346-348), so the generic mob darkness gate does not apply to the
     * fire bat, but the class own gate does: the port had dropped it completely and the fire bat
     * spawned in a fully lit block.</p>
     */
    public static boolean checkFireBatSpawnRules(EntityType<? extends EntityFireBat> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        // The original draws its class roll before the inherited spawn gate.
        int roll=random.nextInt(OriginalLightRules.CLASS_LIGHT_ROLL_BOUND);
        return originalSpawnGate(level.getBrightness(LightLayer.BLOCK,pos),roll,level.getDifficulty()==Difficulty.PEACEFUL);
    }
}

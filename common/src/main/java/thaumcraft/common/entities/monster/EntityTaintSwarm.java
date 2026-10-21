package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.loot.OriginalMobTargeting;

/** Original particle-bodied2x2 taint swarm: old Creature action, asymmetric flight coefficients and no-knockback weakness bite. */
public class EntityTaintSwarm extends Monster implements ITaintedMob {
    private static final EntityDataAccessor<Byte> FLAGS=SynchedEntityData.defineId(EntityTaintSwarm.class,EntityDataSerializers.BYTE);
    private static final SoundEvent ATTACK=SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft","swarmattack"));
    private BlockPos currentFlightTarget;
    public int damBonus;
    private int attackTime;
    private Path legacyPath;
    private LivingEntity lookTarget;
    private int lookTime;
    private float idleYaw;
    public EntityTaintSwarm(EntityType<? extends EntityTaintSwarm> type,Level level) {
        super(type,level);
        moveControl=new MoveControl(this) { @Override public void tick() {} };
        jumpControl=new JumpControl(this) { @Override public void tick() {} };
        applyAttackDamage();
    }
    /**
     * Original EntityTaintSwarm:100 - the attack damage is two plus the damage bonus.
     * The original sets the attribute while the entity is created, so the bonus is
     * zero there; a swarm that is restored from a save keeps the attribute it was
     * created with, exactly like the original.
     */
    private void applyAttackDamage() { getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2+damBonus); }
    public static AttributeSupplier.Builder createAttributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,2); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder);builder.define(FLAGS,(byte)0); }
    public boolean getIsSummoned() { return (entityData.get(FLAGS)&2)!=0; }
    public void setIsSummoned(boolean summoned) {
        byte flags=entityData.get(FLAGS);
        // Original false clears BOTH low bits; other unknown saved bits are deliberately retained.
        entityData.set(FLAGS,(byte)(summoned?flags|2:flags&0xFFFFFFFC));
    }
    @Override public boolean isPushable() { return false; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override protected void checkFallDamage(double distance,boolean grounded,BlockState state,BlockPos pos) {}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    @Override public float getLightLevelDependentMagicValue() { return 1; }
    @Override protected float getSoundVolume() { return .1F; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.EMPTY; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ATTACK; }
    @Override protected SoundEvent getDeathSound() { return ATTACK; }
    @Override public void playSound(SoundEvent sound,float volume,float pitch) {
        if(!isSilent()) level().playSeededSound(null,getX(),getY(),getZ(),Holder.direct(sound),getSoundSource(),volume,pitch,0L);
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(isInvulnerableTo(source))return false;
        boolean accepted=super.hurt(source,amount);
        if(accepted && source.getEntity() instanceof LivingEntity attacker && attacker!=this && attacker!=getVehicle() && !hasPassenger(attacker))setTarget(attacker);
        return accepted;
    }
    @Override public void tick() {
        if(attackTime>0)attackTime--;
        super.tick();
        // Original runs this AFTER vanilla movement on BOTH server and client.
        setDeltaMovement(getDeltaMovement().multiply(1,.6F,1));
        if(level().isClientSide)TaintSwarmVisuals.tick(this,25,10,.22F,15F,.08F);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        legacyCreatureAction();
        LivingEntity target=getTarget();
        if(target==null) {
            if(getIsSummoned())hurt(damageSources().generic(),5);
            if(currentFlightTarget!=null && (!level().isEmptyBlock(currentFlightTarget) || !TaintSwarmFlightRules.aboveWorldBottom(currentFlightTarget.getY(),level().getMinBuildHeight())
                    ||currentFlightTarget.getY()>oldLightHeight(currentFlightTarget.getX(),currentFlightTarget.getZ())+8
                    ||!level().getBiome(currentFlightTarget).is(ThaumcraftBiomes.TAINT)))currentFlightTarget=null;
            if(currentFlightTarget==null || random.nextInt(30)==0 || currentFlightTarget.distSqr(new BlockPos((int)getX(),(int)getY(),(int)getZ()))<4)
                currentFlightTarget=new BlockPos((int)getX()+random.nextInt(7)-random.nextInt(7),(int)getY()+random.nextInt(6)-2,(int)getZ()+random.nextInt(7)-random.nextInt(7));
            steer(currentFlightTarget.getX()+.5-getX(),currentFlightTarget.getY()+.1-getY(),currentFlightTarget.getZ()+.5-getZ(),false);
        } else steer(target.getX()-getX(),target.getY()+target.getEyeHeight()-getY(),target.getZ()-getZ(),true);
        if(getTarget() instanceof Player player && player.getAbilities().invulnerable)setTarget(null);
    }
    /** Old height map used positive light opacity, not modern motion blocking; glass must not become an invented ceiling. */
    private int oldLightHeight(int x,int z) {
        int y=level().getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;
        var position=new BlockPos.MutableBlockPos(x,y,z);
        while(y>=level().getMinBuildHeight()) {
            position.setY(y);
            if(level().getBlockState(position).getLightBlock(level(),position)>0)return y+1;
            y--;
        }
        return level().getMinBuildHeight();
    }
    private void steer(double x,double y,double z,boolean chasing) {
        Vec3 before=getDeltaMovement();
        setDeltaMovement(before.add((Math.signum(x)*.5-before.x)*(chasing?.025000000149011613D:.015000000014901161D),
                (Math.signum(y)*(double).7F-before.y)*(double).1F,
                (Math.signum(z)*.5-before.z)*(chasing?.02500000001490116D:.015000000014901161D)));
        Vec3 motion=getDeltaMovement();float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)-90;
        zza=.1F;setYRot(getYRot()+Mth.wrapDegrees(yaw-getYRot()));
    }
    /** 1.7 EntityCreature's action precedes the custom swarm steering. Keep its target/LOS order and random draws;
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
    private void attackTarget(LivingEntity target,float distance) {
        if(attackTime>0 || distance>=3F || target.getBoundingBox().maxY<=getBoundingBox().minY || target.getBoundingBox().minY>=getBoundingBox().maxY)return;
        if(getIsSummoned())target.lastHurtByPlayerTime=100;
        attackTime=10+random.nextInt(5);
        Vec3 before=target.getDeltaMovement();
        // Original invokes inherited EntityMob melee (vanilla mob damage), not the unused public causeSwarmDamage helper.
        if(doHurtTarget(target) && !level().isClientSide)target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,0));
        target.hurtMarked=false;target.setDeltaMovement(before);
        playSound(ATTACK,.3F,.9F+level().random.nextFloat()*.2F);
    }
    /** Native1.21 melee/enchantment hooks, with original1.7 potion attribute math on a detached copy only. */
    @Override public boolean doHurtTarget(Entity target) {
        float damage=OriginalTaintCombat.mobDamage(this);
        DamageSource source=damageSources().mobAttack(this);
        if(level() instanceof ServerLevel world)damage=EnchantmentHelper.modifyDamage(world,getWeaponItem(),target,source,damage);
        boolean accepted=target.hurt(source,damage);
        if(accepted) {
            float knockback=getKnockback(target,source);
            if(knockback>0 && target instanceof LivingEntity living) {
                living.knockback(knockback*.5F,Mth.sin(getYRot()*((float)Math.PI/180)),-Mth.cos(getYRot()*((float)Math.PI/180)));
                setDeltaMovement(getDeltaMovement().multiply(.6,1,.6));
            }
            if(level() instanceof ServerLevel world)EnchantmentHelper.doPostAttackEffects(world,target,source);
            setLastHurtMob(target);playAttackSound();
        }
        return accepted;
    }
    @Override public float getWalkTargetValue(BlockPos pos,LevelReader level) { return .5F-level.getPathfindingCostFromLightLevels(pos); }
    public static boolean checkTaintSwarmSpawnRules(EntityType<? extends EntityTaintSwarm> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        if(level.getMaxLocalRawBrightness(pos)>random.nextInt(7) || level.getDifficulty()==Difficulty.PEACEFUL
                ||level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
        var world=level.getLevel();
        return world.getMaxLocalRawBrightness(pos,world.isThundering()?10:world.getSkyDarken())<=random.nextInt(8)
                && .5F-level.getPathfindingCostFromLightLevels(pos)>=0;
    }
    @Override public boolean checkSpawnObstruction(LevelReader world) {
        return world.isUnobstructed(this) && world.noCollision(this,getBoundingBox()) && !world.containsAnyLiquid(getBoundingBox())
                && getWalkTargetValue(BlockPos.containing(getX(),getBoundingBox().minY,getZ()),world)>=0;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putByte("Flags",entityData.get(FLAGS));tag.putByte("damBonus",(byte)damBonus); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);entityData.set(FLAGS,tag.getByte("Flags"));damBonus=tag.getByte("damBonus"); }
    @Override protected void dropExperience(Entity killer) {}
    @Override protected void tickDeath() {
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0,()->OriginalTaintExperience.originalReward(this,xpReward,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        if(level.random.nextBoolean())spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get("tainted_goo").get()),getBbHeight()/2);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
        if(recent)OriginalMobLoot.rare(OriginalMobLoot.looting(level,source),random::nextInt);
    }
}

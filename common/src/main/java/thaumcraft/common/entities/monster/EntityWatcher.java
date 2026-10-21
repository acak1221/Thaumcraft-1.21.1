package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.MagicWard;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 Watcher: a floating eyeball with 30 health, 6 damage and the gaze attack that burns the
 * target for one to five damage plus its own attack damage.
 */
public class EntityWatcher extends Monster {
    private static final EntityDataAccessor<Integer> FLAGS=SynchedEntityData.defineId(EntityWatcher.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID=SynchedEntityData.defineId(EntityWatcher.class,EntityDataSerializers.INT);
    private static final int FLAG_GAZING=2;
    private static final int FLAG_ELDER=4;
    private LivingEntity targetedEntity;
    private final GuardianLookHelper lookHelper;
    private final GuardianMoveHelper moveHelper;

    public EntityWatcher(EntityType<? extends EntityWatcher> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.WATCHER.experience();
        lookHelper=new GuardianLookHelper(this);
        moveHelper=new GuardianMoveHelper(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,EldritchMobRules.WATCHER.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.WATCHER.attackDamage())
                .add(Attributes.MOVEMENT_SPEED,EldritchMobRules.WATCHER.movementSpeed())
                .add(Attributes.FOLLOW_RANGE,EldritchMobRules.WATCHER.followRange())
                .add(Attributes.ARMOR,0F);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS,0);
        builder.define(TARGET_ID,0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(4,new GuardianAttackGoal(this));
        goalSelector.addGoal(5,new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this,1.0D));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,1.0D));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(8,new WatchClosestWatcherGoal(this,12.0F));
        goalSelector.addGoal(9,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<LivingEntity>(this,LivingEntity.class,10,true,false,living->true));
    }

    @Override public LookControl getLookControl() { return lookHelper; }
    @Override public MoveControl getMoveControl() { return moveHelper; }

    public boolean isGazing() { return (entityData.get(FLAGS)&FLAG_GAZING)!=0; }
    private void setGazing(boolean gazing) {
        int flags=entityData.get(FLAGS);
        entityData.set(FLAGS,gazing?flags|FLAG_GAZING:flags&~FLAG_GAZING);
    }
    public boolean isElder() { return (entityData.get(FLAGS)&FLAG_ELDER)!=0; }
    public int getGazeDuration() { return EldritchMobRules.watcherGazeDuration(isElder()); }
    public boolean hasTargetedEntity() { return entityData.get(TARGET_ID)!=0; }
    public void setTargetId(int id) { entityData.set(TARGET_ID,id); if(id==0)gazeCounter=0; }
    public LivingEntity getTargetedEntity() {
        if(!hasTargetedEntity())return null;
        if(level().isClientSide) {
            if(targetedEntity!=null)return targetedEntity;
            var found=level().getEntity(entityData.get(TARGET_ID));
            if(found instanceof LivingEntity living) {
                targetedEntity=living;
                return targetedEntity;
            }
            return null;
        }
        return getTarget();
    }

    /** Original watcher walks towards air, so it prefers every open spot. */
    @Override public float getWalkTargetValue(BlockPos pos,LevelReader level) {
        return level.getBlockState(pos).isAir()?10F:super.getWalkTargetValue(pos,level);
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean isPushable() { return true; }

    /** Original watcher thorns the attacker when it is not gazing. */
    @Override public boolean hurt(DamageSource source,float amount) {
        // Original EntityWatcher:287-293 - the thorns answer every source that is not magic
        // damage and not an explosion.
        if(!isGazing()&&!source.is(MagicWard.MAGIC_DAMAGE)&&!source.is(DamageTypeTags.IS_EXPLOSION)
                &&source.getDirectEntity() instanceof LivingEntity attacker) {
            attacker.hurt(damageSources().thorns(this),EldritchMobRules.WATCHER_THORNS_DAMAGE);
            attacker.playSound(SoundEvents.THORNS_HIT,.5F,1.0F);
        }
        return super.hurt(source,amount);
    }

    @Override public void tick() {
        super.tick();
        if(level().isClientSide)clientPresentationTick();
    }

    /** Original watcher floats with a sine driven motion instead of walking. */
    @Override public void travel(Vec3 input) {
        moveRelative((float)input.x,input);
        move(MoverType.SELF,getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(.9D,1D,.9D));
    }

    // ---- Original client presentation state (EntityWatcher fields 175482_b..175486_bm) ----
    private float tailSwing;
    private float tailSwingPrev;
    private float tailSwingSpeed;
    private float spikeExtension;
    private float spikeExtensionPrev;
    private int gazeCounter;

    /** Original EntityWatcher.func_175471_a: the interpolated tail swing. */
    public float getTailSwing(float partialTick) { return tailSwingPrev+(tailSwing-tailSwingPrev)*partialTick; }
    /** Original EntityWatcher.func_175469_o: the interpolated spine extension (1 = spread, 0 = retracted). */
    public float getSpikeExtension(float partialTick) { return spikeExtensionPrev+(spikeExtension-spikeExtensionPrev)*partialTick; }
    /** Original EntityWatcher.func_175474_cn: the watcher currently gazes at a target. */
    public boolean isTargeting() { return isGazing(); }
    /** Original EntityWatcher.func_175477_p: the beam charge progress between 0 and 1. */
    public float getBeamCharge(float partialTick) {
        int duration=getGazeDuration();
        return duration<=0?0F:(gazeCounter+partialTick)/(float)duration;
    }

    /** Original EntityWatcher.func_70636_d: the client side tail, spine and gaze bookkeeping. */
    private void clientPresentationTick() {
        tailSwingPrev=tailSwing;
        if(isGazing()) {
            if(tailSwingSpeed<.5F)tailSwingSpeed=4F;
            else tailSwingSpeed+=(.5F-tailSwingSpeed)*.1F;
        } else tailSwingSpeed+=(.125F-tailSwingSpeed)*.2F;
        tailSwing+=tailSwingSpeed;
        spikeExtensionPrev=spikeExtension;
        if(isGazing())spikeExtension+=(0F-spikeExtension)*.25F;
        else spikeExtension+=(1F-spikeExtension)*.06F;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.GUARDIAN_AMBIENT_LAND; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.GUARDIAN_HURT_LAND; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GUARDIAN_DEATH_LAND; }
    @Override public int getAmbientSoundInterval() { return EldritchMobRules.WATCHER.talkInterval(); }

    /** Original EntityAIWatchClosest of the watcher watches its own kind within twelve blocks. */
    static final class WatchClosestWatcherGoal extends Goal {
        private final EntityWatcher watcher;
        private final double range;

        WatchClosestWatcherGoal(EntityWatcher watcher,double range) {
            this.watcher=watcher;
            this.range=range;
            setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }
        @Override public boolean canUse() {
            return closest()!=null;
        }
        @Override public void tick() {
            var other=closest();
            if(other!=null)watcher.getLookControl().setLookAt(other,1.0F,1.0F);
        }
        private EntityWatcher closest() {
            EntityWatcher closest=null;
            double distance=range;
            for(var candidate:watcher.level().getEntitiesOfClass(EntityWatcher.class,watcher.getBoundingBox().inflate(range))) {
                double next=candidate.distanceToSqr(watcher);
                if(next<distance) {
                    distance=next;
                    closest=candidate;
                }
            }
            return closest;
        }
    }

    /** Original gaze attack: a fixed preparation window, then the burn and the melee damage. */
    static final class GuardianAttackGoal extends Goal {
        private final EntityWatcher watcher;
        private int attackTimer;

        GuardianAttackGoal(EntityWatcher watcher) {
            this.watcher=watcher;
            setFlags(java.util.EnumSet.of(Goal.Flag.MOVE,Goal.Flag.LOOK));
        }
        @Override public boolean canUse() {
            LivingEntity target=watcher.getTarget();
            return target!=null&&target.isAlive();
        }
        @Override public boolean canContinueToUse() {
            LivingEntity target=watcher.getTarget();
            return target!=null&&target.isAlive()&&(watcher.isElder()||watcher.distanceToSqr(target)>9D);
        }
        @Override public void start() {
            attackTimer=-10;
            watcher.getNavigation().stop();
            watcher.getLookControl().setLookAt(watcher.getTarget(),90F,90F);
        }
        @Override public void stop() {
            watcher.setTargetId(0);
            watcher.setTarget(null);
        }
        @Override public void tick() {
            LivingEntity target=watcher.getTarget();
            if(target==null)return;
            watcher.getNavigation().stop();
            watcher.getLookControl().setLookAt(target,90F,90F);
            if(!watcher.hasLineOfSight(target)) {
                watcher.setTarget(null);
                return;
            }
            ++attackTimer;
            if(attackTimer==0) {
                watcher.setTargetId(target.getId());
                watcher.level().broadcastEntityEvent(watcher,(byte)21);
            } else if(attackTimer>=watcher.getGazeDuration()) {
                float damage=EldritchMobRules.watcherGazeDamage(watcher.level().getDifficulty().getId(),watcher.isElder());
                target.hurt(watcher.damageSources().indirectMagic(watcher,null),damage);
                target.hurt(watcher.damageSources().mobAttack(watcher),(float)watcher.getAttributeValue(Attributes.ATTACK_DAMAGE));
                watcher.setTarget(null);
            }
            // Original EntityWatcher counts the gaze preparation ticks for the beam charge.
            if(watcher.gazeCounter<watcher.getGazeDuration())++watcher.gazeCounter;
        }
    }

    /** Original guardian look helper, kept so the watcher can track a target it does not walk to. */
    static final class GuardianLookHelper extends LookControl {
        GuardianLookHelper(EntityWatcher watcher) { super(watcher); }
    }

    /** Original guardian move helper: the watcher drifts forward while it gazes. */
    static final class GuardianMoveHelper extends MoveControl {
        private final EntityWatcher watcher;

        GuardianMoveHelper(EntityWatcher watcher) {
            super(watcher);
            this.watcher=watcher;
        }
        @Override public void tick() {
            LivingEntity target=watcher.getTarget();
            if(target==null||watcher.getNavigation().isDone()) {
                watcher.setSpeed(0F);
                watcher.setGazing(false);
                return;
            }
            double dx=target.getX()-watcher.getX();
            double dy=target.getY()-watcher.getY();
            double dz=target.getZ()-watcher.getZ();
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(distance<=0) {
                watcher.setSpeed(0F);
                watcher.setGazing(false);
                return;
            }
            dy/=distance;
            float yaw=(float)(Math.atan2(dz,dx)*180D/Math.PI)-90F;
            watcher.setYRot(watcher.getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-watcher.getYRot()),-30F,30F));
            float speed=(float)(getSpeedModifier()*watcher.getAttributeValue(Attributes.MOVEMENT_SPEED));
            watcher.setSpeed(watcher.getSpeed()+(speed-watcher.getSpeed())*.125F);
            double drift=Math.sin((watcher.tickCount+watcher.getId())*.5D)*.05D;
            double cos=Math.cos(watcher.getYRot()*(float)Math.PI/180F);
            double sin=Math.sin(watcher.getYRot()*(float)Math.PI/180F);
            watcher.setDeltaMovement(watcher.getDeltaMovement().x+drift*cos,watcher.getDeltaMovement().y,
                    watcher.getDeltaMovement().z+drift*sin);
            drift=Math.sin((watcher.tickCount+watcher.getId())*.75D)*.05D;
            watcher.setDeltaMovement(watcher.getDeltaMovement().x,watcher.getDeltaMovement().y+drift*(sin+cos)*.25D+watcher.getSpeed()*dy*.1D,
                    watcher.getDeltaMovement().z);
            watcher.setGazing(true);
        }
    }
}

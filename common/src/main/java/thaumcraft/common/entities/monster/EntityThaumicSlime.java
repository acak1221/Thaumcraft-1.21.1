package thaumcraft.common.entities.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 Thaumic Slime: a tainted slime whose size drives its health, its damage, its split, its
 * merge with other slimes and its leap of faith at nearby players.
 */
public class EntityThaumicSlime extends Monster implements ITaintedMob {
    private static final EntityDataAccessor<Integer> SIZE=SynchedEntityData.defineId(EntityThaumicSlime.class,EntityDataSerializers.INT);
    private int slimeJumpDelay;
    private int launched=EldritchMobRules.SLIME_LAUNCHED;
    private int spitCounter=EldritchMobRules.SLIME_SPIT_INTERVAL_TICKS;

    public EntityThaumicSlime(EntityType<? extends EntityThaumicSlime> type,Level level) {
        super(type,level);
        setSlimeSize(EldritchMobRules.slimeInitialSize(getRandom().nextInt(EldritchMobRules.SLIME_SIZE_VARIANTS)));
        slimeJumpDelay=EldritchMobRules.slimeInitialJumpDelay(getRandom().nextInt(EldritchMobRules.SLIME_INITIAL_JUMP_SPAN));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,EldritchMobRules.THAUMIC_SLIME.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.THAUMIC_SLIME.attackDamage())
                .add(Attributes.MOVEMENT_SPEED,EldritchMobRules.THAUMIC_SLIME.movementSpeed())
                .add(Attributes.FOLLOW_RANGE,EldritchMobRules.THAUMIC_SLIME.followRange())
                .add(Attributes.ARMOR,0F);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(SIZE,1); }

    public int getSlimeSize() { return entityData.get(SIZE); }

    /** Original slime size drives the body size, the health and the dropped experience. */
    public void setSlimeSize(int size) {
        entityData.set(SIZE,size);
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(EldritchMobRules.slimeHealth(size));
        setHealth(getMaxHealth());
        xpReward=(int)Math.sqrt(size);
    }

    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        float size=EldritchMobRules.slimeDimension(getSlimeSize());
        return EntityDimensions.scalable(size,size);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("Size",getSlimeSize()-1); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSlimeSize(tag.getInt("Size")+1);
    }

    /** Original attack strength equals the slime size. */
    public int getAttackStrength() { return EldritchMobRules.slimeAttackStrength(getSlimeSize()); }

    /** Original slime leaves the world in peaceful difficulty and announces its landing. */
    @Override public void tick() {
        if(!level().isClientSide&&level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL&&getSlimeSize()>0) {
            discard();
            return;
        }
        boolean wasOnGround=onGround();
        super.tick();
        if(launched>0)--launched;
        if(onGround()&&!wasOnGround&&makesSoundOnLand())
            playSound(jumpSound(),getSoundVolume(),((getRandom().nextFloat()-getRandom().nextFloat())*.2F+1F)*.8F);
    }

    /** Original slime behaviour: spit at players, merge with other slimes and jump. */
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        Player player=closestPlayer();
        if(player!=null) {
            if(spitCounter>0)--spitCounter;
            faceEntity(player,10F,20F);
            if(distanceTo(player)>EldritchMobRules.SLIME_SPIT_RANGE&&spitCounter<=0&&EldritchMobRules.slimeCanSpit(getSlimeSize())) {
                spitCounter=EldritchMobRules.SLIME_SPIT_COUNTER;
                if(!level().isClientSide)level().addFreshEntity(spitSlime(player));
                playSound(sound("gore"),1F,((getRandom().nextFloat()-getRandom().nextFloat())*.2F+1F)*.8F);
                setSlimeSize(getSlimeSize()-1);
            }
        } else {
            EntityThaumicSlime other=closestMergableSlime();
            if(other!=null) {
                faceEntity(other,10F,20F);
                if(distanceTo(other)<getBbWidth()+other.getBbWidth()) {
                    other.setSlimeSize(EldritchMobRules.slimeMergedSize(other.getSlimeSize(),getSlimeSize()));
                    discard();
                }
            }
        }
        if(onGround()&&slimeJumpDelay--<=0) {
            slimeJumpDelay=EldritchMobRules.slimeJumpDelay(getRandom().nextInt(EldritchMobRules.SLIME_JUMP_DELAY_SPAN));
            if(player!=null)slimeJumpDelay/=3;
            jumpFromGround();
            if(makesSoundOnJump())playSound(jumpSound(),getSoundVolume(),((getRandom().nextFloat()-getRandom().nextFloat())*.2F+1F)*.8F);
            float forward=(float)Math.sqrt(getSlimeSize());
            double heading=getYRot()*(float)Math.PI/180F;
            setDeltaMovement(getDeltaMovement().x+Math.cos(heading)*forward*.2D,getDeltaMovement().y,
                    getDeltaMovement().z-Math.sin(heading)*forward*.2D);
        }
    }

    /** Original slime looks for the closest player within sixteen blocks. */
    private Player closestPlayer() {
        Player closest=null;
        double distance=16D;
        for(var candidate:level().players()) {
            double next=candidate.distanceToSqr(this);
            if(next<distance) {
                distance=next;
                closest=candidate;
            }
        }
        return closest;
    }

    /** Original faceEntity turns the slime towards its target. */
    public void faceEntity(Entity target,float yawSpeed,float pitchSpeed) {
        double dx=target.getX()-getX(),dy=target.getY()-getEyeY(),dz=target.getZ()-getZ();
        double horizontal=Math.sqrt(dx*dx+dz*dz);
        float yaw=(float)(Math.atan2(dz,dx)*180D/Math.PI)-90F;
        float pitch=(float)-(Math.atan2(dy,horizontal)*180D/Math.PI);
        setYRot(getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-getYRot()),-yawSpeed,yawSpeed));
        setXRot(getXRot()+Mth.clamp(Mth.wrapDegrees(pitch-getXRot()),-pitchSpeed,pitchSpeed));
    }

    /** Original merging search keeps the original distance bookkeeping quirk. */
    protected EntityThaumicSlime closestMergableSlime() {
        EntityThaumicSlime closest=null;
        double distance=Double.MAX_VALUE;
        var box=new AABB(getX(),getY(),getZ(),getX(),getY(),getZ()).inflate(16,8,16);
        for(var candidate:level().getEntitiesOfClass(EntityThaumicSlime.class,box)) {
            if(candidate.getId()!=getId()&&candidate.tickCount>100&&candidate.getSlimeSize()<EldritchMobRules.SLIME_MAX_SIZE
                    &&distanceToSqr(candidate)<distance)closest=candidate;
            distance=distanceToSqr(candidate);
        }
        return closest;
    }

    /** Original spit slime is a size one slime thrown at the target. */
    private EntityThaumicSlime spitSlime(Player target) {
        var slime=new EntityThaumicSlime(ThaumcraftEldritchMobs.THAUMIC_SLIME.get(),level());
        slime.setSlimeSize(1);
        slime.setPos(getX(),getY()+getBbHeight()/2D,getZ());
        double dx=target.getX()-getX();
        double dy=target.getY()+target.getBbHeight()/3D-slime.getY();
        double dz=target.getZ()-getZ();
        double flat=Math.sqrt(dx*dx+dz*dz);
        if(flat>=1.0E-7D) {
            double length=Math.sqrt(dx*dx+dy*dy+dz*dz);
            double spread=.0075D;
            slime.setDeltaMovement((dx/length+getRandom().nextGaussian()*spread)*1.5D,
                    (dy/length+flat*.2D/length+getRandom().nextGaussian()*spread)*1.5D,
                    (dz/length+getRandom().nextGaussian()*spread)*1.5D);
        }
        return slime;
    }

    /** Original slime damages a player that touches it. */
    @Override public void playerTouch(Player player) {
        if(!canDamagePlayer())return;
        int strength=(int)Math.max(1D,Math.sqrt(getSlimeSize()));
        if(launched>0&&strength==2)strength=3;
        if(hasLineOfSight(player)&&distanceToSqr(player)<.8D*strength*.8D*strength
                &&player.hurt(damageSources().mobAttack(this),(float)getAttackStrength()))
            playSound(SoundEvents.SLIME_ATTACK,1F,(getRandom().nextFloat()-getRandom().nextFloat())*.2F+1F);
    }

    /** Original slime splits into the square root of its size when it dies. */
    @Override public void remove(Entity.RemovalReason reason) {
        if(!level().isClientSide&&reason==Entity.RemovalReason.KILLED)splitIntoChildren();
        super.remove(reason);
    }

    private void splitIntoChildren() {
        int count=EldritchMobRules.slimeSplitCount(getSlimeSize());
        if(count<=1||getHealth()>0F)return;
        for(int i=0;i<count;i++) {
            float x=((i%2)-.5F)*count/4F;
            float z=((i/2)-.5F)*count/4F;
            var child=new EntityThaumicSlime(ThaumcraftEldritchMobs.THAUMIC_SLIME.get(),level());
            child.setSlimeSize(1);
            child.moveTo(getX()+x,getY()+.5D,getZ()+z,getRandom().nextFloat()*360F,0F);
            level().addFreshEntity(child);
        }
    }

    /** Original slime only drops the tainted resource while it is small. */
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {
        super.dropCustomDeathLoot(level,source,recentlyHit);
        if(getSlimeSize()<3&&getRandom().nextInt(3)==0) {
            var resource=net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("thaumcraft","resource"));
            if(resource.isPresent())spawnAtLocation(new ItemStack(resource.get()),getBbHeight()/2F);
        }
    }

    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }

    protected boolean canDamagePlayer() { return getSlimeSize()>0; }
    protected boolean makesSoundOnJump() { return getSlimeSize()>3; }
    protected boolean makesSoundOnLand() { return getSlimeSize()>5; }
    protected SoundEvent jumpSound() { return getSlimeSize()>3?SoundEvents.SLIME_JUMP:SoundEvents.SLIME_JUMP_SMALL; }
    @Override protected float getSoundVolume() { return EldritchMobRules.slimeSoundVolume(getSlimeSize()); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return getSlimeSize()>3?SoundEvents.SLIME_HURT:SoundEvents.SLIME_HURT_SMALL; }
    @Override protected SoundEvent getDeathSound() { return getSlimeSize()>3?SoundEvents.SLIME_DEATH:SoundEvents.SLIME_DEATH_SMALL; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
}

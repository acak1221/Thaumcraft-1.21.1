package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 Eldritch Crab: 20 health, 4 damage, a cultist plate helm worth five armour points,
 * the leap onto its victim and the spider eye drop.
 */
public class EntityEldritchCrab extends Monster implements IEldritchMob {
    private static final EntityDataAccessor<Boolean> HELM=SynchedEntityData.defineId(EntityEldritchCrab.class,EntityDataSerializers.BOOLEAN);
    private int attackTime;

    public EntityEldritchCrab(EntityType<? extends EntityEldritchCrab> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.CRAB.experience();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,EldritchMobRules.CRAB.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.CRAB.attackDamage())
                .add(Attributes.MOVEMENT_SPEED,EldritchMobRules.CRAB_BASE_SPEED)
                .add(Attributes.FOLLOW_RANGE,EldritchMobRules.CRAB.followRange())
                .add(Attributes.ARMOR,0F);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(HELM,false); }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new LeapAtTargetGoal(this,.63F));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.0D,false) {
            @Override protected int getAttackInterval() { return EldritchMobRules.MELEE_AI_COOLDOWN; }
        });
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8D));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
                living->living.getType().getDescriptionId().startsWith("entity.thaumcraft.cultist")));
    }

    public boolean hasHelm() { return entityData.get(HELM); }
    public void setHelm(boolean helm) { entityData.set(HELM,helm); }

    /** Original crab armour is five points while it wears its helm. */
    @Override public int getArmorValue() { return hasHelm()?(int)EldritchMobRules.CRAB_HELMET_ARMOR:0; }

    /** Original crab spawn: hard always wears the helm, every other difficulty one third of the time. */
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
            DifficultyInstance difficulty,MobSpawnType reason,net.minecraft.world.entity.SpawnGroupData data) {
        super.finalizeSpawn(level,difficulty,reason,data);
        setHelm(EldritchMobRules.crabHasHelm(level.getDifficulty().getId(),getRandom().nextFloat()));
        return data;
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("Flags",hasHelm()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHelm(tag.getBoolean("Flags"));
    }

    /** Original crab ride and helm break behaviour. */
    @Override public void tick() {
        super.tick();
        if(tickCount<20)fallDistance=0F;
        LivingEntity victim=getTarget();
        if(getVehicle()==null&&victim!=null&&victim.getVehicle()==null&&!onGround()&&!hasHelm()&&victim.isAlive()
                &&getY()-victim.getY()>=victim.getBbHeight()/2F
                &&distanceToSqr(victim)<EldritchMobRules.CRAB_ATTACH_DISTANCE_SQUARED)
            startRiding(victim,true);
        if(!level().isClientSide&&getVehicle()!=null&&attackTime<=0) {
            attackTime=EldritchMobRules.crabRideInterval(getRandom().nextInt(10));
            doHurtTarget(getVehicle());
            if(getVehicle()!=null&&getRandom().nextFloat()<EldritchMobRules.CRAB_DISMOUNT_CHANCE)stopRiding();
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit)playSound(sound("crabclaw"),1.0F,.9F+getRandom().nextFloat()*.2F);
        return hit;
    }

    /** Original crab drops its helm at half health and hands over the cultist chest plate. */
    @Override public boolean hurt(DamageSource source,float damage) {
        boolean hurt=super.hurt(source,damage);
        if(hasHelm()&&EldritchMobRules.crabHelmetBreaks(getHealth(),getMaxHealth())) {
            setHelm(false);
            var plate=net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getOptional(ResourceLocation.fromNamespaceAndPath("thaumcraft",EldritchMobRules.CULTIST_PLATE_CHEST_ID));
            if(plate.isPresent())spawnAtLocation(new ItemStack(plate.get()));
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(EldritchMobRules.CRAB_BASE_SPEED);
        }
        return hurt;
    }

    /** Original crab drop: one spider eye in three, improved by looting. */
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {
        super.dropCustomDeathLoot(level,source,recentlyHit);
        int looting=thaumcraft.common.lib.loot.OriginalMobLoot.looting(level,source);
        if(recentlyHit&&EldritchMobRules.crabDropsEye(getRandom().nextInt(EldritchMobRules.CRAB_EYE_ROLL),
                getRandom().nextInt(1+looting)>0?1:0))spawnAtLocation(new ItemStack(Items.SPIDER_EYE));
    }

    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof EntityEldritchCrab||super.isAlliedTo(entity); }
    @Override public boolean canPickUpLoot() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    /** Original crab is immune to poison. */
    @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return !effect.getEffect().is(net.minecraft.world.effect.MobEffects.POISON)&&super.canBeAffected(effect);
    }

    @Override protected SoundEvent getAmbientSound() { return sound("crabtalk"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.GENERIC_HURT; }
    @Override protected SoundEvent getDeathSound() { return sound("crabdeath"); }
    /** Original crab step sound. */
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state) { playSound(SoundEvents.SPIDER_STEP,.15F,1.0F); }
    @Override public int getAmbientSoundInterval() { return EldritchMobRules.CRAB.talkInterval(); }
    @Override protected float getSoundVolume() { return .5F; }

    static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
}

package thaumcraft.common.entities.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintAnimalRules.TaintedAnimal;

/** Original tainted chicken:8HP/3damage/0.4speed, no fall damage, ten-tick flap
 * animation and the original chicken ambient and hurt sounds. */
public final class EntityTaintChicken extends Chicken implements ITaintedMob {
    public EntityTaintChicken(EntityType<? extends EntityTaintChicken> type,Level level) {
        super(type,level);
        xpReward=TaintAnimalRules.EXPERIENCE;
        // The original EntityAISwimming constructor lets the navigator swim.
        getNavigation().setCanFloat(true);
    }
    public EntityTaintChicken(Level level) { this(ThaumcraftTaintAnimals.TAINT_CHICKEN.get(),level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH,TaintAnimalRules.maxHealth(TaintedAnimal.CHICKEN))
                .add(Attributes.ATTACK_DAMAGE,TaintAnimalRules.attackDamage(TaintedAnimal.CHICKEN))
                .add(Attributes.MOVEMENT_SPEED,TaintAnimalRules.movementSpeed(TaintedAnimal.CHICKEN))
                .add(Attributes.FOLLOW_RANGE,TaintAnimalRules.FOLLOW_RANGE);
    }
    @Override protected void registerGoals() {
        goalSelector.removeAllGoals(goal->true);
        targetSelector.removeAllGoals(goal->true);
        goalSelector.addGoal(0,new TaintAnimalMeleeGoal.Swim(this));
        goalSelector.addGoal(2,new TaintAnimalMeleeGoal(this,Player.class,1.0D,false));
        goalSelector.addGoal(2,new LeapAtTargetGoal(this,0.3F));
        goalSelector.addGoal(3,new TaintAnimalMeleeGoal(this,Villager.class,1.0D,true));
        goalSelector.addGoal(3,new TaintAnimalMeleeGoal(this,Animal.class,1.0D,true));
        goalSelector.addGoal(3,new RandomStrollGoal(this,1.0D));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,6.0F));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Villager.class,0,false,false,null));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Animal.class,0,false,false,null));
    }
    /** Original EntityTaintChicken.onLivingUpdate: the flap animation and the original0.9 fall damping. */
    @Override public void aiStep() {
        float previousPhase=flap,previousSpeed=flapSpeed,previousAmplitude=flapping;
        // The original tainted chicken never lays an egg.
        eggTime=Integer.MAX_VALUE;
        Vec3 before=getDeltaMovement();
        super.aiStep();
        oFlap=previousPhase;
        oFlapSpeed=previousSpeed;
        flapSpeed=TaintAnimalRules.flapSpeed(previousSpeed,onGround());
        flapping=TaintAnimalRules.flapAmplitude(previousAmplitude,onGround());
        flap=TaintAnimalRules.flapPhase(previousPhase,flapping);
        if(!onGround()&&before.y<0.0D)setDeltaMovement(getDeltaMovement().with(net.minecraft.core.Direction.Axis.Y,TaintAnimalRules.fallDamping((float)before.y)));
        if(level().isClientSide&&tickCount<5)TaintSwarmVisuals.sploosh(this,10);
    }
    /** Original EntityTaintChicken.fall: no fall damage at all. */
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.CHICKEN); }
    @Override public int getAmbientSoundInterval() { return 400; }
    /** Original getMaxSpawnedInChunk has no1.21 counterpart. */
    public int originalMaxSpawnedInChunk() { return TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.CHICKEN); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.CHICKEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CHICKEN_HURT; }
    /** Original1.7 death sound is the hurt sound, not entity.chicken.death. */
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CHICKEN_HURT; }
    @Override public float getVoicePitch() { return TaintAnimalRules.voicePitch(); }
    /** Original EntityTaintChicken:64-66 - the total armour of the tainted chicken is a constant two. */
    @Override public int getArmorValue() { return TaintAnimalRules.armor(TaintedAnimal.CHICKEN); }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) { return InteractionResult.PASS; }
    @Override public Chicken getBreedOffspring(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.AgeableMob parent) { return null; }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropExperience(Entity killer) {}
    @Override protected void tickDeath() {
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,xpReward,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        int drop=TaintAnimalRules.taintedDrop(TaintedAnimal.CHICKEN,level.random::nextInt,level.random::nextBoolean);
        if(drop!=TaintAnimalRules.NO_DROP)spawnAtLocation(ThaumcraftTaintAnimals.taintMaterial(drop),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
}

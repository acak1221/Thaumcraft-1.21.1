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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintAnimalRules.TaintedAnimal;

/** Original tainted cow:40HP/6damage/0.27speed, swimming navigation and the
 * original cow ambient and hurt sounds. */
public final class EntityTaintCow extends Cow implements ITaintedMob {
    public EntityTaintCow(EntityType<? extends EntityTaintCow> type,Level level) {
        super(type,level);
        xpReward=TaintAnimalRules.EXPERIENCE;
        getNavigation().setCanFloat(true);
    }
    public EntityTaintCow(Level level) { this(ThaumcraftTaintAnimals.TAINT_COW.get(),level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Cow.createAttributes()
                .add(Attributes.MAX_HEALTH,TaintAnimalRules.maxHealth(TaintedAnimal.COW))
                .add(Attributes.ATTACK_DAMAGE,TaintAnimalRules.attackDamage(TaintedAnimal.COW))
                .add(Attributes.MOVEMENT_SPEED,TaintAnimalRules.movementSpeed(TaintedAnimal.COW))
                .add(Attributes.FOLLOW_RANGE,TaintAnimalRules.FOLLOW_RANGE);
    }
    @Override protected void registerGoals() {
        goalSelector.removeAllGoals(goal->true);
        targetSelector.removeAllGoals(goal->true);
        goalSelector.addGoal(0,new TaintAnimalMeleeGoal.Swim(this));
        goalSelector.addGoal(2,new TaintAnimalMeleeGoal(this,Player.class,1.0D,false));
        goalSelector.addGoal(3,new TaintAnimalMeleeGoal(this,Villager.class,1.0D,true));
        goalSelector.addGoal(5,new RandomStrollGoal(this,1.0D));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6.0F));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        goalSelector.addGoal(8,new TaintAnimalMeleeGoal(this,Animal.class,1.0D,false));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Villager.class,0,false,false,null));
        targetSelector.addGoal(8,new NearestAttackableTargetGoal<>(this,Animal.class,0,false,false,null));
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide&&tickCount<5)TaintSwarmVisuals.sploosh(this,10);
    }
    @Override protected boolean shouldDespawnInPeaceful() { return TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.COW); }
    @Override public int getAmbientSoundInterval() { return 400; }
    /** Original getMaxSpawnedInChunk has no1.21 counterpart. */
    public int originalMaxSpawnedInChunk() { return TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.COW); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.COW_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.COW_HURT; }
    /** Original1.7 death sound is the hurt sound, not entity.cow.death. */
    @Override protected SoundEvent getDeathSound() { return SoundEvents.COW_HURT; }
    @Override protected float getSoundVolume() { return TaintAnimalRules.soundVolume(TaintedAnimal.COW); }
    @Override public float getVoicePitch() { return TaintAnimalRules.voicePitch(); }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) { return InteractionResult.PASS; }
    @Override public Cow getBreedOffspring(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.AgeableMob parent) { return null; }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropExperience(Entity killer) {}
    @Override protected void tickDeath() {
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,xpReward,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        int drop=TaintAnimalRules.taintedDrop(TaintedAnimal.COW,level.random::nextInt,level.random::nextBoolean);
        if(drop!=TaintAnimalRules.NO_DROP)spawnAtLocation(ThaumcraftTaintAnimals.taintMaterial(drop),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
}

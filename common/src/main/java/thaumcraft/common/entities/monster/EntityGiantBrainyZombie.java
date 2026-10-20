package thaumcraft.common.entities.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftContent;

/** TC4 Giant Brainy Zombie, including unpersisted anger, refused-hit anger and the original collision/render scale mismatch. */
public final class EntityGiantBrainyZombie extends EntityBrainyZombie {
    private static final EntityDataAccessor<Float> ANGER = SynchedEntityData.defineId(EntityGiantBrainyZombie.class, EntityDataSerializers.FLOAT);
    private float storedScale = 2.2F;
    private float appliedScale = 1;
    private boolean dimensionBaby;
    public EntityGiantBrainyZombie(EntityType<? extends EntityGiantBrainyZombie> type, Level level) {
        super(type, level); xpReward = 15; goalSelector.addGoal(2, new LeapAtTargetGoal(this, .4F));
    }
    public static AttributeSupplier.Builder createAttributes() { return EntityBrainyZombie.createAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 7); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(ANGER, 1F); }
    public float getAnger() { return entityData.get(ANGER); }
    public void setAnger(float anger) { entityData.set(ANGER, anger); }
    @Override public boolean hurt(DamageSource source, float damage) { setAnger(Math.min(2, getAnger() + .1F)); return super.hurt(source, damage); }
    @Override public void aiStep() {
        super.aiStep();
        // Original Zombie.setSize is final and only updates its stored base size; it does NOT resize the actual box here.
        if (getAnger() > 1) { setAnger(getAnger() - .002F); storedScale = 1.2F + getAnger(); }
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7 + (getAnger() - 1) * 5);
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        float scale = appliedScale > 0 ? appliedScale : 1;
        var dimensions = EntityDimensions.scalable(.6F * scale, 1.8F * scale).withEyeHeight(1.8F * scale * .85F);
        return isBaby() ? dimensions.scale(.5F) : dimensions;
    }
    @Override public void setBaby(boolean baby) {
        super.setBaby(baby); dimensionBaby = baby; appliedScale = storedScale > 0 ? storedScale : 1; refreshDimensions();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (isBaby() != dimensionBaby) { dimensionBaby = isBaby(); appliedScale = storedScale > 0 ? storedScale : 1; refreshDimensions(); }
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData pack) {
        float before = appliedScale;
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, pack);
        if (!isBaby()) { appliedScale = before; refreshDimensions(); } // Old natural adult setup never invokes setChildSize(false).
        return result;
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        float before = appliedScale; super.readAdditionalSaveData(tag);
        if (!isBaby()) { appliedScale = before; refreshDimensions(); } // Old NBT only invokes the child setter when IsBaby is true.
    }
    // Original has two consecutive loops of six independent booleans, each success emits TWO flesh (maximum24).
    @Override protected int fleshAttempts() { return 12; }
    @Override protected int fleshPerDrop() { return 2; }
    @Override protected float brainDropHeight() { return 2; }
    @Override protected void dropRareItem() {
        switch (random.nextInt(4)) {
            case 0 -> spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get("thaumium_ingot").get()), 2);
            case 1 -> spawnAtLocation(Items.CARROT);
            case 2 -> spawnAtLocation(Items.POTATO);
            default -> spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get("amber_gem").get()), 2);
        }
    }
}

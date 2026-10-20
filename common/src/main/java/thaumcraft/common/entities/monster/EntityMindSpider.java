package thaumcraft.common.entities.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;

/** TC4's tiny one-health spider. Harmless visions attack nobody, push nobody, drop nothing and live for 1200 ticks. */
public final class EntityMindSpider extends Spider {
    private static final EntityDataAccessor<Boolean> HARMLESS = SynchedEntityData.defineId(EntityMindSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(EntityMindSpider.class, EntityDataSerializers.STRING);
    private int lifeSpan = Integer.MAX_VALUE;
    public EntityMindSpider(EntityType<? extends EntityMindSpider> type, Level level) { super(type, level); xpReward = 1; }
    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.FOLLOW_RANGE, 12);
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        // The original overrides findPlayerToAttack with closest player12 and ignores the vanilla spider light gate.
        targetSelector.removeAllGoals(goal -> true);
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
    }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(HARMLESS, false); builder.define(VIEWER, ""); }
    public boolean isHarmless() { return entityData.get(HARMLESS); }
    public String getViewer() { return entityData.get(VIEWER); }
    public void setViewer(String viewer) { entityData.set(VIEWER, viewer == null ? "" : viewer); }
    public void setHarmless(boolean harmless) { entityData.set(HARMLESS, harmless); if (harmless) lifeSpan = 1200; }
    @Override public boolean doHurtTarget(Entity target) { return !isHarmless() && super.doHurtTarget(target); }
    @Override public void push(Entity other) { if (!isHarmless()) super.push(other); }
    @Override public boolean isPushable() { return !isHarmless() && super.isPushable(); }
    @Override public void tick() { super.tick(); if (!level().isClientSide && tickCount > lifeSpan) discard(); }
    @Override public float getVoicePitch() { return .7F; }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean player) {}
    @Override protected void dropFromLootTable(DamageSource source, boolean player) {}
    @Override protected int getBaseExperienceReward() { return isHarmless() ? 0 : super.getBaseExperienceReward(); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("harmless", isHarmless()); tag.putString("viewer", getViewer()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // The original reads its harmless watcher without resetting lifeSpan: retain that reload quirk.
        entityData.set(HARMLESS, tag.getBoolean("harmless")); setViewer(tag.getString("viewer"));
    }
}

package thaumcraft.common.entities.projectile;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ThaumcraftWarp;

/** Original zero-gravity Eldritch Orb: 100 ticks, radius-two living hit, 0.666 of owner attack and weakness160. */
public final class EntityEldritchOrb extends ThrowableProjectile {
    private static java.util.function.Consumer<EntityEldritchOrb> impactEffect;
    public static void setClientImpactEffect(java.util.function.Consumer<EntityEldritchOrb> effect) { impactEffect = java.util.Objects.requireNonNull(effect); }
    public EntityEldritchOrb(EntityType<? extends EntityEldritchOrb> type, Level level) { super(type, level); }
    public EntityEldritchOrb(Level level, LivingEntity owner) {
        this(ThaumcraftWarp.ELDRITCH_ORB.get(), level); setOwner(owner); setPos(owner.getX(), owner.getEyeY() - .1, owner.getZ());
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected double getDefaultGravity() { return 0; }
    @Override public void tick() { super.tick(); if (tickCount > 100) discard(); }
    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || !(getOwner() instanceof LivingEntity owner)) return;
        float damage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * .666F;
        for (Entity entity : level().getEntities(owner, getBoundingBox().inflate(2))) {
            // TC4 func_70662_br is isEntityUndead: the blast does not harm its own undead faction.
            if (!(entity instanceof LivingEntity living) || living.isInvertedHealAndHarm() || living.isSpectator()) continue;
            living.hurt(damageSources().indirectMagic(this, owner), damage);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160));
        }
        playSound(SoundEvents.FIRE_EXTINGUISH, .5F, 2.6F + (random.nextFloat() - random.nextFloat()) * .8F);
        tickCount = 100; level().broadcastEntityEvent(this, (byte) 16);
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 16) {
            if (level().isClientSide && impactEffect != null) impactEffect.accept(this);
        } else super.handleEntityEvent(event);
    }
}

package thaumcraft.common.entities.projectile;

import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftExplosiveOrbs;

/** TC4 fireball specialization. The explosion has no owner and never destroys
 * blocks, matching original createExplosion(null,...,onFire,false). Native
 * explosion exposure, damage protections and fire placement replace 1.7 engine
 * internals; the direct hit retains the real thrower. */
public final class EntityExplosiveOrb extends ThrowableProjectile {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_fireball"));
    public static final ResourceKey<DamageType> UNATTRIBUTED_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_fireball_unattributed"));
    private static final EntityDataAccessor<Float> STRENGTH = SynchedEntityData.defineId(EntityExplosiveOrb.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FIRE = SynchedEntityData.defineId(EntityExplosiveOrb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(EntityExplosiveOrb.class, EntityDataSerializers.INT);
    private int age;
    private static Consumer<EntityExplosiveOrb> clientTrail = ignored -> {};
    public static void setClientTrailHandler(Consumer<EntityExplosiveOrb> handler) { clientTrail = java.util.Objects.requireNonNull(handler); }

    public EntityExplosiveOrb(EntityType<? extends EntityExplosiveOrb> type, Level level) { super(type, level); }
    public EntityExplosiveOrb(Level level) { this(ThaumcraftExplosiveOrbs.EXPLOSIVE_ORB.get(), level); }
    public EntityExplosiveOrb(Level level, LivingEntity owner) {
        this(level); setOwner(owner);
        setYRot(owner.getYRot()); setXRot(owner.getXRot());
        float yaw = owner.getYRot() / 180 * (float)Math.PI, pitch = owner.getXRot() / 180 * (float)Math.PI;
        setPos(owner.getX() - (double)(Mth.cos(yaw) * .16F), owner.getEyeY() - (double).1F,
                owner.getZ() - (double)(Mth.sin(yaw) * .16F));
        shoot(-Mth.sin(yaw) * Mth.cos(pitch) * .4F, -Mth.sin(pitch) * .4F,
                Mth.cos(yaw) * Mth.cos(pitch) * .4F, 1.5F, 1F);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STRENGTH, 1F); builder.define(FIRE, false); builder.define(AGE, 0);
    }
    public float getStrength() { return entityData.get(STRENGTH); }
    public void setStrength(float value) {
        if (!Float.isFinite(value) || value < 0 || value > 3) throw new IllegalArgumentException("Invalid explosive orb strength");
        entityData.set(STRENGTH, value);
    }
    public boolean isFiery() { return entityData.get(FIRE); }
    public void setFiery(boolean value) { entityData.set(FIRE, value); }
    public int getAge() { return age; }
    @Override protected double getDefaultGravity() { return .01F; }
    @Override public Vec3 getMovementToShoot(double x, double y, double z, float speed, float scatter) {
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length <= 1e-12F) return Vec3.ZERO;
        return new Vec3((x / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (y / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (z / length + random.nextGaussian() * .007499999832361937 * scatter) * speed);
    }
    @Override protected boolean canHitEntity(Entity target) {
        return !level().isClientSide && !target.isSpectator() && target.isPickable() && (target != getOwner() || age >= 5);
    }
    @Override protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult hit) { onHit(hit); return ProjectileDeflection.NONE; }
    @Override protected void onHit(HitResult hit) {
        if (isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        if (!level().isClientSide) {
            if (hit instanceof EntityHitResult target) {
                Entity owner=getOwner();
                var source = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(owner==null ? UNATTRIBUTED_DAMAGE_TYPE : DAMAGE_TYPE), this, owner==null ? this : owner);
                target.getEntity().hurt(source, getStrength() * 1.5F);
            }
            level().explode(null, getX(), getY(), getZ(), getStrength(), isFiery(), Level.ExplosionInteraction.NONE);
        }
        discard();
    }
    @Override public void tick() {
        if (isRemoved()) return;
        if (age < Integer.MAX_VALUE) ++age;
        if (!level().isClientSide) entityData.set(AGE, age);
        super.tick();
        if (level().isClientSide) clientTrail.accept(this);
        if (age > 500) discard();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key); if (AGE.equals(key)) age = entityData.get(AGE);
    }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt();
        Entity attacker = source.getEntity();
        if (attacker == null) return false;
        setDeltaMovement(attacker.getLookAngle().scale(.9));
        // Original deflection changes motion, never its thrower.
        return true;
    }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // Original omitted these fields. Save them so chunk reload does not
        // erase a paid Potency/alchemist effect or rejuvenate its lifetime.
        tag.putFloat("OrbStrength", getStrength()); tag.putBoolean("OrbFire", isFiery()); tag.putInt("OrbAge", age);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float strength = tag.contains("OrbStrength") ? tag.getFloat("OrbStrength") : 1;
        setStrength(Float.isFinite(strength) && strength >= 0 && strength <= 3 ? strength : 1);
        setFiery(tag.getBoolean("OrbFire")); age = Math.max(0, tag.getInt("OrbAge")); entityData.set(AGE, age);
    }
}

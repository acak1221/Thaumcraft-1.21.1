package thaumcraft.common.entities.projectile;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftProjectiles;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * TC4 EntityGolemOrb, the homing orb of the cultist clerics and the eldritch
 * golem.
 * <p>
 * The original is an {@code EntityThrowable} without gravity whose launch
 * inherits the default throwable speed of 1.5 and the default pitch offset of
 * zero. It homes on its target by adding a fifth of the direction, divided by
 * the SQUARED distance to the target, to every motion axis and clamping each
 * axis to a quarter, dies after 160 ticks (blue) or 240 ticks (red) and deals
 * the thrower's attack damage attribute times 0.6 (blue) or 1.0 (red) to the
 * entity it hits with the magic source that bypasses the armour.
 * <p>
 * Declared native adaptations: the homing target is kept as a synced UUID
 * instead of a spawn packet entity id, the damage uses the native mob
 * projectile damage type, and the client burst is a hook.
 */
public final class EntityGolemOrb extends ThrowableProjectile {
    /**
     * Original {@code DamageSource.causeIndirectMagicDamage} (oracle
     * {@code thaumcraft/common/entities/projectile/EntityGolemOrb.java:78}): the magic source
     * that bypasses the armour. The native type carries the same tags.
     */
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "indirect_magic"));
    private static final EntityDataAccessor<Boolean> RED = SynchedEntityData.defineId(EntityGolemOrb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(EntityGolemOrb.class, EntityDataSerializers.INT);
    private static Consumer<EntityGolemOrb> clientImpact = ignored -> {};
    private int age;

    public EntityGolemOrb(EntityType<? extends EntityGolemOrb> type, Level level) {
        super(type, level);
    }

    public EntityGolemOrb(Level level) {
        this(ThaumcraftProjectiles.GOLEM_ORB.get(), level);
    }

    public EntityGolemOrb(Level level, LivingEntity owner, LivingEntity target, boolean red) {
        this(level);
        setOwner(owner);
        setRed(red);
        setTarget(target);
        float yaw = owner.getYRot() / 180F * (float) Math.PI;
        float pitch = owner.getXRot() / 180F * (float) Math.PI;
        setPos(owner.getX() - (double) (Mth.cos(yaw) * .16F), owner.getEyeY() - (double) .1F,
                owner.getZ() - (double) (Mth.sin(yaw) * .16F));
        // The original golem orb keeps the default throwable speed of 1.5 and no pitch offset.
        shoot(-Mth.sin(yaw) * Mth.cos(pitch) * .4F, -Mth.sin(pitch) * .4F,
                Mth.cos(yaw) * Mth.cos(pitch) * .4F, 1.5F, 1F);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RED, false);
        builder.define(TARGET, 0);
    }

    public boolean isRed() { return entityData.get(RED); }
    public void setRed(boolean value) { entityData.set(RED, value); }
    /** The original keeps the homing target as an entity id, exactly like its spawn packet. */
    public int getTargetId() { return entityData.get(TARGET); }
    public LivingEntity getTarget() {
        int id = getTargetId();
        if (id <= 0) return null;
        Entity entity = level().getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }
    public void setTarget(LivingEntity target) {
        entityData.set(TARGET, target == null ? 0 : target.getId());
    }
    public int getAge() { return age; }

    @Override protected double getDefaultGravity() { return 0; }
    @Override public float getPickRadius() { return .1F; }

    @Override public Vec3 getMovementToShoot(double x, double y, double z, float speed, float scatter) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length <= 1e-12F || !Float.isFinite(speed) || !Float.isFinite(scatter)) return Vec3.ZERO;
        return new Vec3((x / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (y / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (z / length + random.nextGaussian() * .007499999832361937 * scatter) * speed);
    }

    @Override public void tick() {
        if (isRemoved()) return;
        if (age < Integer.MAX_VALUE) ++age;
        super.tick();
        if (isRemoved()) return;
        if (age > ProjectileRules.golemOrbLifetime(isRed())) {
            discard();
            return;
        }
        home();
    }

    /** Original homing: the delta is divided by the squared distance, then scaled by 0.2 and clamped. */
    private void home() {
        LivingEntity target = getTarget();
        if (target == null) return;
        // The original divided the delta by getDistanceSqToEntity, the SQUARED distance.
        double squared = distanceToSqr(target);
        double dx = target.getX() - getX();
        double dy = target.getBoundingBox().minY + (double) target.getBbHeight() * .6D - getY();
        double dz = target.getZ() - getZ();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(
                ProjectileRules.clampGolemOrbSpeed(ProjectileRules.golemOrbHoming(motion.x, dx, squared)),
                ProjectileRules.clampGolemOrbSpeed(ProjectileRules.golemOrbHoming(motion.y, dy, squared)),
                ProjectileRules.clampGolemOrbSpeed(ProjectileRules.golemOrbHoming(motion.z, dz, squared)));
    }

    @Override protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult hit) {
        // The original orb is never deflected and keeps its thrower.
        onHit(hit);
        return ProjectileDeflection.NONE;
    }

    @Override protected void onHit(HitResult hit) {
        if (isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        if (level().isClientSide) {
            clientImpact.accept(this);
            discard();
            return;
        }
        if (hit instanceof EntityHitResult entityHit && getOwner() instanceof LivingEntity owner) {
            float damage = ProjectileRules.golemOrbDamage((float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE), isRed());
            // Original EntityGolemOrb:78 - the orb hurts with DamageSource.causeIndirectMagicDamage,
            // the magic source that also bypasses the armour.
            var holder = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DAMAGE_TYPE);
            entityHit.getEntity().hurt(new DamageSource(holder, this, owner), damage);
        }
        playSound(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "shock")),
                1F, 1F + (random.nextFloat() - random.nextFloat()) * .2F);
        discard();
    }

    /** Original deflection: the orb takes the look direction of its attacker, scaled to nine tenths. */
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt();
        Entity attacker = source.getEntity();
        if (attacker == null) return false;
        setDeltaMovement(attacker.getLookAngle().scale(.9));
        playSound(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "zap")),
                1F, 1F + (random.nextFloat() - random.nextFloat()) * .2F);
        return true;
    }

    public static void setClientImpact(Consumer<EntityGolemOrb> effect) {
        clientImpact = Objects.requireNonNull(effect);
    }
}

package thaumcraft.common.entities.projectile;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftProjectiles;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * TC4 EntityPrimalArrow, the arrow of the bone bow.
 * <p>
 * The original keeps the vanilla arrow flight but replaces the hit: the damage
 * is the ceiling of the arrow speed times 2.1 (times 1.5 for the heavy type
 * and 0.8 for the order and wither types), a critical arrow adds a random
 * roll of {@code damage / 2 + 2}, the air and order types ignore armour, the
 * fire type burns and every type adds its own effect (slowness, weakness or
 * wither). The arrow is never picked up and dies after a hundred ticks in the
 * ground.
 * <p>
 * Declared native adaptations: the 1.7 arrow cannot be pierced and cannot
 * knock back here, because the original shadowed the vanilla arrow fields, so
 * the punch enchantment and the bone bow damage bonus never reached the
 * original's own damage value. The damage sources use the native magic, fire
 * and arrow damage types, and the pick up item is a vanilla arrow because the
 * native arrow has to carry a stack to be saved.
 */
public final class EntityPrimalArrow extends AbstractArrow {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityPrimalArrow.class, EntityDataSerializers.INT);
    private float size = .5F;

    public EntityPrimalArrow(EntityType<? extends EntityPrimalArrow> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
        setBaseDamage(ProjectileRules.PRIMAL_ARROW_BASE_DAMAGE);
    }

    public EntityPrimalArrow(Level level) {
        this(ThaumcraftProjectiles.PRIMAL_ARROW.get(), level);
    }

    /** Original constructor without a shooter: a quarter block wide arrow. */
    public EntityPrimalArrow(Level level, double x, double y, double z) {
        this(level);
        size = .25F;
        setPos(x, y, z);
    }

    /**
     * The original shooter constructor without the launch: the arrow starts at
     * the eye of the shooter, drawn back by the width of the shooter and one
     * block along the look vector, and it keeps the shooter as its owner. The
     * launch itself is left to the bow, which knows the charge of the shot.
     */
    public EntityPrimalArrow(Level level, LivingEntity shooter, int type) {
        this(level);
        setOwner(shooter);
        setArrowType(type);
        float yaw = shooter.getYRot() / 180F * (float) Math.PI;
        setPos(shooter.getX() - (double) (Mth.cos(yaw) * .16F), shooter.getEyeY() - (double) .1F,
                shooter.getZ() - (double) (Mth.sin(yaw) * .16F));
        // The original adds the shooter's look vector to the start position.
        Vec3 look = shooter.getLookAngle();
        setPos(getX() + look.x, getY() + look.y, getZ() + look.z);
    }

    /**
     * Original shooter constructor. The velocity is the bow charge times the
     * arrow damage; the arrow leaves the hand at one and a half times that
     * value with the original gaussian spread of one.
     */
    public EntityPrimalArrow(Level level, LivingEntity shooter, float velocity, int type) {
        this(level, shooter, type);
        float yaw = shooter.getYRot() / 180F * (float) Math.PI;
        float pitch = shooter.getXRot() / 180F * (float) Math.PI;
        shoot(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch),
                ProjectileRules.primalArrowLaunchSpeed(velocity), 1F);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, ProjectileRules.PRIMAL_ARROW_TYPE_AIR);
    }

    public int getArrowType() { return entityData.get(TYPE); }
    public void setArrowType(int type) { entityData.set(TYPE, type); }

    @Override public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(size, size);
    }

    /** The original arrow is never picked up; the native stack only exists to be saved. */
    @Override protected ItemStack getDefaultPickupItem() { return new ItemStack(Items.ARROW); }

    @Override public void playerTouch(Player player) {
        // The original overrode the player collision with an empty method.
    }

    @Override protected boolean canHitEntity(Entity entity) {
        // The original only spared its shooter for the first four air ticks.
        return !level().isClientSide && entity.canBeHitByProjectile() && !entity.isSpectator() && entity.isPickable()
                && (entity != getOwner() || tickCount >= 5);
    }

    /** The original 1.7 launch uses one gaussian draw per axis, not the modern triangle noise. */
    @Override public Vec3 getMovementToShoot(double x, double y, double z, float speed, float scatter) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length <= 1e-12F || !Float.isFinite(speed) || !Float.isFinite(scatter)) return Vec3.ZERO;
        return new Vec3((x / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (y / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (z / length + random.nextGaussian() * .007499999832361937 * scatter) * speed);
    }

    @Override protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult hit) {
        // The original arrow is never deflected and keeps its shooter.
        onHit(hit);
        return ProjectileDeflection.NONE;
    }

    @Override protected void tickDespawn() {
        // The original keeps the arrow in the ground for a hundred ticks only.
        if (inGroundTime >= ProjectileRules.PRIMAL_ARROW_IN_GROUND_LIMIT) discard();
    }

    /**
     * Original {@code EntityPrimalArrow.field_70252_j}: the number of ticks the arrow
     * already spent in the ground. The original renderer fades the arrow with it
     * (oracle {@code thaumcraft/client/renderers/entity/RenderPrimalArrow.java:64}).
     */
    public int spentLife() {
        return Math.min(100, inGroundTime);
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        int type = getArrowType();
        double speed = getDeltaMovement().length();
        int damage = ProjectileRules.primalArrowHitDamage(speed, ProjectileRules.primalArrowDamage(type));
        if (isCritArrow())
            damage = ProjectileRules.primalArrowCritDamage(damage, random.nextInt(ProjectileRules.primalArrowCritBound(damage)));
        DamageSource source = damageSource(type);
        // The original applies the type effect and the fire before the hit.
        applyTypeEffect(target, type);
        int fire = fireSeconds(type);
        if (fire > 0 && !(target.getType() == EntityType.ENDERMAN)) target.igniteForSeconds(fire);
        if (target.hurt(source, damage)) {
            if (getOwner() instanceof LivingEntity shooter && target instanceof LivingEntity living
                    && level() instanceof ServerLevel server)
                EnchantmentHelper.doPostAttackEffectsWithItemSource(server, living, source, shooter.getMainHandItem());
            playSound(SoundEvents.ARROW_HIT, 1F, 1.2F / (random.nextFloat() * .2F + .9F));
            if (!(target.getType() == EntityType.ENDERMAN)) discard();
        } else {
            // The original reverses the arrow when the victim refuses the hit.
            setDeltaMovement(getDeltaMovement().scale(-.1));
            setYRot(getYRot() + 180F);
            yRotO += 180F;
        }
    }

    /** The original effects of the arrow types. */
    private void applyTypeEffect(Entity target, int type) {
        if (!(target instanceof LivingEntity living)) return;
        if (ProjectileRules.primalArrowAppliesSlowness(type))
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    ProjectileRules.PRIMAL_ARROW_SLOWNESS_TICKS, ProjectileRules.PRIMAL_ARROW_SLOWNESS_AMPLIFIER));
        if (ProjectileRules.primalArrowAppliesOrder(type))
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    ProjectileRules.PRIMAL_ARROW_ORDER_TICKS, ProjectileRules.PRIMAL_ARROW_ORDER_AMPLIFIER));
        if (ProjectileRules.primalArrowAppliesWither(type))
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, ProjectileRules.PRIMAL_ARROW_WITHER_TICKS));
    }

    /** The original fire seconds of the arrow types. */
    private int fireSeconds(int type) {
        return ProjectileRules.primalArrowFireSeconds(isOnFire(), type) + ProjectileRules.primalArrowFireTypeSeconds(type);
    }

    private DamageSource damageSource(int type) {
        ResourceKey<DamageType> key = ProjectileRules.primalArrowIgnoresArmour(type) ? DamageTypes.INDIRECT_MAGIC
                : ProjectileRules.primalArrowIsFireDamage(type) ? DamageTypes.FIREBALL : DamageTypes.ARROW;
        var holder = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        Entity owner = getOwner() != null ? getOwner() : this;
        return new DamageSource(holder, this, owner);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("type", (byte) getArrowType());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setArrowType(tag.getByte("type"));
    }
}

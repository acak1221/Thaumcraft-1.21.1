package thaumcraft.common.entities.projectile;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEmbers;

/**
 * TC4 EntityEmber:37-162, with native Projectile ownership, collision and tracking.
 * Not a fireball, arrow, explosion, or persistent flame area. ThrowableProjectile
 * retains the inherited .99F air / .8F water drag and four water bubbles, after
 * Ember's own damping, and uses native ProjectileUtil's .3F entity hit margin.
 *
 * Adaptations: integer native metadata replaces the signed-byte spawn duration;
 * a separately ticked age follows the world's pre-onUpdate age increment and is
 * saved so unloading cannot rejuvenate this brief projectile. Removed/expired
 * entities return immediately instead of TC4's final post-setDead collision.
 * Native portal/fluid/voxel-shape handling and damage protections are retained.
 * The native FIREBALL damage-type/tag policy replaces TC4's string/fire-flag
 * source; this is not a port of the old vanilla armour/enchantment/mob-AI engine.
 * There are deliberately no client classes, particle callbacks or visual RNG.
 */
public final class EntityEmber extends ThrowableProjectile {
    public static final float LAUNCH_SPEED = 1;
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(EntityEmber.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(EntityEmber.class, EntityDataSerializers.INT);
    private float damage = 1;
    private int firey;
    private float scatter;
    private int age;

    public EntityEmber(EntityType<? extends EntityEmber> type, Level level) { super(type, level); }
    public EntityEmber(Level level) { this(ThaumcraftEmbers.EMBER.get(), level); }

    public EntityEmber(Level level, LivingEntity owner, float scatter) {
        this(level);
        setOwner(owner);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        float yaw = owner.getYRot() / 180 * (float)Math.PI;
        float pitch = owner.getXRot() / 180 * (float)Math.PI;
        setPos(owner.getX() - (double)(Mth.cos(yaw) * .16F), owner.getEyeY() - (double).1F,
                owner.getZ() - (double)(Mth.sin(yaw) * .16F));
        // The inherited TC4 EntityThrowable constructor first shoots at scatter1;
        // EntityEmber:48-50 THEN normalizes that noisy vector and shoots again.
        // Six Gaussian draws, X/Y/Z twice. Modern triangle noise or a single
        // scatter15 launch changes both the spread and the random draw order.
        shoot(-Mth.sin(yaw) * Mth.cos(pitch) * .4F, -Mth.sin(pitch) * .4F,
                Mth.cos(yaw) * Mth.cos(pitch) * .4F, LAUNCH_SPEED, 1);
        Vec3 initial = getDeltaMovement();
        shoot(initial.x, initial.y, initial.z, LAUNCH_SPEED, scatter);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DURATION, 20);
        builder.define(AGE, 0);
    }

    @Override public Vec3 getMovementToShoot(double x, double y, double z, float speed, float inaccuracy) {
        scatter = inaccuracy;
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        // Legacy MathHelper.sqrt_double returns FLOAT, even for double input.
        // Do not normalize again after adding Gaussian error.
        x = x / length + random.nextGaussian() * .007499999832361937 * inaccuracy;
        y = y / length + random.nextGaussian() * .007499999832361937 * inaccuracy;
        z = z / length + random.nextGaussian() * .007499999832361937 * inaccuracy;
        return new Vec3(x * speed, y * speed, z * speed);
    }

    @Override protected double getDefaultGravity() { return 0; }
    public int getDuration() { return entityData.get(DURATION); }
    public void setDuration(int duration) { entityData.set(DURATION, duration); }
    public int getAge() { return age; }
    public void setAge(int value) { age = value; entityData.set(AGE, value); }
    public float getDamage() { return damage; }
    public void setDamage(float value) { damage = value; }
    public float getScatter() { return scatter; }
    public int getFirey() { return firey; }
    public void setFirey(int value) { firey = value; }
    public int getFireSeconds() { return 3 + firey; }
    public void setFireSeconds(int seconds) { firey = seconds - 3; }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (AGE.equals(accessor)) age = entityData.get(AGE);
    }

    @Override public void tick() {
        if (isRemoved()) return;
        // World increments ticksExisted BEFORE calling TC4 onUpdate. Keep that
        // ordering with our persisted clock, including when ticked directly.
        ++age;
        if (!level().isClientSide) entityData.set(AGE, age);
        // Preserve TC4's strict > check BEFORE damping/inherited flight.
        if (age > getDuration()) { discard(); return; }
        setDeltaMovement(getDeltaMovement().scale(getDuration() <= 20 ? .95 : .975));
        if (onGround()) setDeltaMovement(getDeltaMovement().scale(.66));
        super.tick();
    }

    @Override protected boolean canHitEntity(Entity entity) {
        // TC4's inherited EntityThrowable excludes ONLY its thrower for the
        // first four air ticks, not its entire vehicle/team until it leaves.
        // No client entity-hit prediction can damage, ignite or pick a victim.
        return !level().isClientSide && !entity.isSpectator() && entity.isPickable()
                && (entity != getOwner() || age >= 5);
    }

    @Override protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult hit) {
        // TC4's ember cannot be batted back or take damage. In particular do not
        // let a modern projectile tag/deflection change its real owner.
        onHit(hit);
        return ProjectileDeflection.NONE;
    }

    @Override protected void onHit(HitResult hit) {
        if (isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        if (!level().isClientSide) {
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();
                // DamageSources.fireball requires the unrelated vanilla Fireball
                // class. Use its native registered damage type with OUR direct
                // entity and native Projectile owner, matching TC4's indirect
                // "fireball" + setFireDamage (not magic or bypass-armour damage).
                var source = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.FIREBALL), this, getOwner());
                if (!target.fireImmune() && target.hurt(source, damage)) target.igniteForSeconds(getFireSeconds());
            } else if (hit instanceof BlockHitResult blockHit && random.nextFloat() < .025F * firey) {
                var pos = blockHit.getBlockPos().relative(blockHit.getDirection());
                // TC4 EntityEmber:96-127: upgrade-level chance, AIR ONLY, raw
                // normal fire, flags2. No mobGriefing, mayInteract, doFireTick,
                // flammability or invented friendly/PVP permission bypass.
                if (level().isEmptyBlock(pos)) level().setBlock(pos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        discard();
    }

    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("firey", firey);
        tag.putInt("duration", getDuration());
        tag.putInt("EmberAge", age);
        tag.putFloat("EmberScatter", scatter);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        firey = tag.getInt("firey");
        setDuration(tag.getInt("duration"));
        setAge(tag.getInt("EmberAge"));
        scatter = tag.getFloat("EmberScatter");
    }
}

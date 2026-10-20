package thaumcraft.common.entities.projectile;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftFrost;

/**
 * TC4 EntityFrostShard, including fragile scatter, boulder and frosty effects.
 *
 * Native Projectile owns UUID ownership, spawn/motion packets and base ticking;
 * ProjectileUtil supplies current voxel collision and its .3F entity margin.
 * Flight is explicit: collision -> POST-bounce move -> .99F/.8F drag -> .05F
 * gravity. Neither modern launch noise/owner movement nor a cached incoming
 * velocity may replace this ordering. Only the owner gets four air ticks' grace.
 *
 * Declared native adaptations: current portal/fluid/current/combat/protection
 * rules, native terrain particles/sounds, and no onProjectileHit block callbacks
 * (a BASIC shard must not ring/activate/break modern blocks). A removed shard
 * returns instead of performing the original final post-setDead movement.
 * Age/impacts are persisted so reload cannot rejuvenate owner grace/bounces.
 * Nonfinite motion is stopped; zero/tiny/overflowing lengths skip the original
 * divide-by-zero nudge. These are safety guards, not gameplay upgrades or a
 * claim to reproduce the whole 1.7 vanilla physics/combat engine.
 */
public final class EntityFrostShard extends Projectile {
    // joined.srg maps zk.e()F to func_70182_d; original client javap returns1.5F.
    public static final float LAUNCH_SPEED = 1.5F;
    public static final byte ENTITY_IMPACT_EVENT = 61;
    public static final byte TERMINAL_IMPACT_EVENT = 62;
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(EntityFrostShard.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> IMPACTS = SynchedEntityData.defineId(EntityFrostShard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(EntityFrostShard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FRAGILE = SynchedEntityData.defineId(EntityFrostShard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BOULDER = SynchedEntityData.defineId(EntityFrostShard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FROST = SynchedEntityData.defineId(EntityFrostShard.class, EntityDataSerializers.INT);
    // Client-only registration installs an unbound method reference, never a world/entity.
    private static BiConsumer<EntityFrostShard, Byte> clientImpact = (entity, event) -> {};
    private static Consumer<EntityFrostShard> clientTrail = ignored -> {};
    private int age;

    public EntityFrostShard(EntityType<? extends EntityFrostShard> type, Level level) { super(type, level); }
    public EntityFrostShard(Level level) { this(ThaumcraftFrost.FROST_SHARD.get(), level); }

    public EntityFrostShard(Level level, LivingEntity owner, float scatter) {
        this(level);
        setOwner(owner);
        float yaw = owner.getYRot() / 180 * (float)Math.PI;
        float pitch = owner.getXRot() / 180 * (float)Math.PI;
        setPos(owner.getX() - (double)(Mth.cos(yaw) * .16F), owner.getEyeY() - (double).1F,
                owner.getZ() - (double)(Mth.sin(yaw) * .16F));
        // Vanilla1.7 EntityThrowable's first scatter1 launch, THEN Frost:59's
        // second normalization/shoot. Exactly six Gaussian draws, X/Y/Z twice.
        shoot(-Mth.sin(yaw) * Mth.cos(pitch) * .4F, -Mth.sin(pitch) * .4F,
                Mth.cos(yaw) * Mth.cos(pitch) * .4F, LAUNCH_SPEED, 1);
        Vec3 first = getDeltaMovement();
        shoot(first.x, first.y, first.z, LAUNCH_SPEED, scatter);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DAMAGE, 3F);
        builder.define(IMPACTS, 0);
        builder.define(AGE, 0);
        builder.define(FRAGILE, false);
        builder.define(BOULDER, false);
        builder.define(FROST, 0);
    }

    @Override public Vec3 getMovementToShoot(double x, double y, double z, float speed, float scatter) {
        // Legacy sqrt_double returns FLOAT; do not normalize after Gaussian error.
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length <= 1e-12F || !Float.isFinite(speed) || !Float.isFinite(scatter)) return Vec3.ZERO;
        return new Vec3((x / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (y / length + random.nextGaussian() * .007499999832361937 * scatter) * speed,
                (z / length + random.nextGaussian() * .007499999832361937 * scatter) * speed);
    }

    public void setDamage(float value) {
        if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("Frost damage must be finite and nonnegative");
        entityData.set(DAMAGE, value);
    }
    public float getDamage() { return entityData.get(DAMAGE); }
    public boolean isFragile() { return entityData.get(FRAGILE); }
    public void setFragile(boolean value) { entityData.set(FRAGILE, value); }
    public boolean isBoulder() { return entityData.get(BOULDER); }
    public void configureBoulder() { entityData.set(BOULDER, true); }
    public double getBounceStrength() { return isBoulder() ? .8 : .5; }
    public int getFrosty() { return entityData.get(FROST); }
    public void setFrosty(int value) {
        if (value < 0 || value > 3) throw new IllegalArgumentException("Invalid frost level");
        entityData.set(FROST, value);
    }
    public int getBounceLimit() { return (isBoulder() ? 6 : 3) - getImpactCount(); }
    public int getImpactCount() { return entityData.get(IMPACTS); }
    public int getAge() { return age; }

    @Override public EntityDimensions getDimensions(Pose pose) {
        float size = .15F + (entityData == null ? 3F : getDamage()) * .15F;
        return EntityDimensions.scalable(size, size);
    }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DAMAGE.equals(accessor)) refreshDimensions();
        if (AGE.equals(accessor)) age = entityData.get(AGE);
    }

    @Override protected double getDefaultGravity() { return isFragile() ? .015F : .05F; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return true; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double range = getBoundingBox().getSize() * 4 * 64;
        return distance < range * range;
    }

    @Override public void tick() {
        if (isRemoved()) return;
        if (getBounceLimit() < 0) { discard(); return; }
        setOldPosAndRot();
        setDeltaMovement(finiteMotion(getDeltaMovement()));
        if (age < Integer.MAX_VALUE) ++age;
        if (!level().isClientSide) entityData.set(AGE, age);
        super.tick();
        if (isRemoved()) return;
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        // Clients predict block ricochets, but never choose/damage an entity or
        // emit impact events. Actual server events alone produce frost bursts.
        if (hit.getType() != HitResult.Type.MISS) onHit(hit);
        if (isRemoved()) return;
        checkInsideBlocks(); // Native portals/fluids, NOT block projectile-hit callbacks.
        if (isRemoved()) return;
        Vec3 motion = finiteMotion(getDeltaMovement());
        Vec3 next = position().add(motion);
        float drag = .99F;
        if (isInWater()) {
            drag = .8F;
            for (int bubble = 0; bubble < 4; ++bubble)
                level().addParticle(ParticleTypes.BUBBLE, next.x - motion.x * .25, next.y - motion.y * .25,
                        next.z - motion.z * .25, motion.x, motion.y, motion.z);
        }
        setDeltaMovement(motion.scale(drag));
        applyGravity();
        setPos(next);
        updateRotation(); // Frost:168-184 uses FINAL dragged/gravity-adjusted motion.
        if (level().isClientSide && getFrosty() > 0) clientTrail.accept(this);
    }

    @Override protected void updateRotation() {
        Vec3 motion = getDeltaMovement();
        float horizontal = (float)Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float)(Math.atan2(motion.x, motion.z) * 180 / Math.PI);
        float pitch = (float)(Math.atan2(motion.y, horizontal) * 180 / Math.PI);
        while (pitch - xRotO < -180) xRotO -= 360;
        while (pitch - xRotO >= 180) xRotO += 360;
        while (yaw - yRotO < -180) yRotO -= 360;
        while (yaw - yRotO >= 180) yRotO += 360;
        setXRot(xRotO + (pitch - xRotO) * .2F);
        setYRot(yRotO + (yaw - yRotO) * .2F);
    }

    @Override protected boolean canHitEntity(Entity entity) {
        return !level().isClientSide && !entity.isSpectator() && entity.isPickable()
                && (entity != getOwner() || age >= 5);
    }

    @Override protected void onHit(HitResult hit) {
        if (isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        Vec3 motion = finiteMotion(getDeltaMovement());
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            // CURRENT shard position, NOT the ray's contact point or hit normal.
            motion = new Vec3(Mth.floor(getX()) != Mth.floor(target.getX()) ? -motion.x : motion.x,
                    Mth.floor(getY()) != Mth.floor(target.getY()) ? motion.y * -.9 : motion.y,
                    Mth.floor(getZ()) != Mth.floor(target.getZ()) ? -motion.z : motion.z).scale(.66);
        } else if (hit instanceof BlockHitResult blockHit) {
            Direction.Axis axis = blockHit.getDirection().getAxis();
            motion = new Vec3(axis == Direction.Axis.X ? -motion.x : motion.x,
                    axis == Direction.Axis.Y ? motion.y * -.9 : motion.y,
                    axis == Direction.Axis.Z ? -motion.z : motion.z);
            if (level() instanceof ServerLevel server) {
                var state = level().getBlockState(blockHit.getBlockPos());
                playSound(state.getSoundType().getBreakSound(), .3F, impactPitch());
                // Count0 packets carry each actual velocity, not Gaussian spread
                // around a fabricated block or a second custom S2C protocol.
                for (int fragment = 0; fragment < getDamage(); ++fragment)
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY(), getZ(),
                            0, 4 * (random.nextFloat() - .5), .5, 4 * (random.nextFloat() - .5), 1);
            }
        }
        motion = motion.scale(getBounceStrength());
        setDeltaMovement(motion);
        float length = (float)Math.sqrt(motion.lengthSqr());
        // Preserve the oracle's SUBTRACT/post-bounce/.05F quirk, with only the
        // finite/zero safety adaptation. No ray-contact teleport or sign fix.
        if (Float.isFinite(length) && length > 1e-12F)
            setPos(position().subtract(motion.x / length * (double).05F, motion.y / length * (double).05F, motion.z / length * (double).05F));
        if (level().isClientSide) return;
        markHurt();
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            Vec3 originalMotion = target.getDeltaMovement();
            target.hurt(damageSources().thrown(this, getOwner()), getDamage());
            // Original applies frost and fragile effects even when hurt refuses.
            if (target instanceof LivingEntity living && getFrosty() > 0)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, getFrosty() - 1));
            level().broadcastEntityEvent(this, ENTITY_IMPACT_EVENT);
            if (isFragile()) {
                target.invulnerableTime = 0;
                discard();
                playSound(Blocks.ICE.defaultBlockState().getSoundType().getBreakSound(), .3F, impactPitch());
                target.setDeltaMovement(originalMotion.add(target.getDeltaMovement().subtract(originalMotion).scale(.1)));
            }
        }
        // if (bounceLimit-- <= 0): 3->2->1->0 survive; 0->-1 is FOURTH terminal.
        entityData.set(IMPACTS, getImpactCount() + 1);
        if (getBounceLimit() < 0) {
            playSound(Blocks.ICE.defaultBlockState().getSoundType().getBreakSound(), .3F, impactPitch());
            level().broadcastEntityEvent(this, TERMINAL_IMPACT_EVENT);
            discard();
        }
    }

    private float impactPitch() { return 1.2F / (random.nextFloat() * .2F + .9F); }

    private static Vec3 finiteMotion(Vec3 motion) {
        return Double.isFinite(motion.x) && Double.isFinite(motion.y) && Double.isFinite(motion.z)
                && Float.isFinite((float)Math.sqrt(motion.lengthSqr())) ? motion : Vec3.ZERO;
    }

    public static void setClientImpactHandler(BiConsumer<EntityFrostShard, Byte> handler) { clientImpact = handler; }
    public static void setClientTrailHandler(Consumer<EntityFrostShard> handler) { clientTrail = java.util.Objects.requireNonNull(handler); }

    @Override public void handleEntityEvent(byte event) {
        if (event == ENTITY_IMPACT_EVENT || event == TERMINAL_IMPACT_EVENT) {
            if (level().isClientSide) clientImpact.accept(this, event);
        } else super.handleEntityEvent(event);
    }

    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", getDamage());
        tag.putBoolean("fragile", isFragile());
        tag.putBoolean("FrostBoulder", isBoulder());
        tag.putInt("frost", getFrosty());
        tag.putInt("FrostImpacts", getImpactCount());
        tag.putInt("FrostAge", age);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float damage = tag.contains("damage") ? tag.getFloat("damage") : 3;
        setDamage(Float.isFinite(damage) && damage >= 0 ? damage : 3);
        setFragile(tag.getBoolean("fragile"));
        entityData.set(BOULDER, tag.getBoolean("FrostBoulder"));
        setFrosty(Mth.clamp(tag.getInt("frost"), 0, 3));
        entityData.set(IMPACTS, Mth.clamp(tag.getInt("FrostImpacts"), 0, isBoulder() ? 7 : 4));
        age = Math.max(0, tag.getInt("FrostAge"));
        entityData.set(AGE, age);
        setDeltaMovement(finiteMotion(getDeltaMovement()));
    }
}

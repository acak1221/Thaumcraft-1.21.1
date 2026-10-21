package thaumcraft.common.entities.golems;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Transient TC4 golem-owned bobber, never a player hook or a loot authority.
 * The fishing goal owns admission, cast time, catches and association cleanup. */
public final class EntityGolemBobber extends Entity {
    private static final EntityDataAccessor<Integer> FISHER_ID = SynchedEntityData.defineId(EntityGolemBobber.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> FISHER_UUID = SynchedEntityData.defineId(EntityGolemBobber.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<BlockPos> WATER_TARGET = SynchedEntityData.defineId(EntityGolemBobber.class, EntityDataSerializers.BLOCK_POS);
    private boolean inBlock;
    private int interpolationSteps;
    private double interpolationX, interpolationY, interpolationZ;
    private float interpolationYaw, interpolationPitch;

    public EntityGolemBobber(EntityType<? extends EntityGolemBobber> type, Level level) {
        super(type, level);
        noCulling = true;
        setNoGravity(true); // The original buoyancy/gravity below owns every force.
        setDeltaMovement(Vec3.ZERO);
    }

    /** Initialize an unpublished server candidate; does not spawn or publish it. */
    public void setup(EntityGolemBase fisher, BlockPos water) {
        if (level().isClientSide || fisher.level() != level()) {
            throw new IllegalArgumentException("A golem bobber must be set up in its fisher's server level");
        }
        entityData.set(FISHER_ID, fisher.getId());
        entityData.set(FISHER_UUID, Optional.of(fisher.getUUID()));
        entityData.set(WATER_TARGET, water.immutable());
        setPos(fisher.getX(), fisher.getY(), fisher.getZ());
        setOldPosAndRot();
        double dx = water.getX() + .5 - fisher.getX();
        double dy = water.getY() + 1.0 - fisher.getY();
        double dz = water.getZ() + .5 - fisher.getZ();
        setDeltaMovement(castDelta(dx, dy, dz));
    }

    /**
     * Original EntityGolemBobber:62-71 (the readable 1.7.10 source of TC4 4.2.3.5): the cast velocity is the
     * direction to the target times 0.1, with {@code sqrt(distance) * 0.08} added to the vertical part. The square
     * root is taken over the DISTANCE, not over the squared distance and not over the distance itself.
     */
    public static Vec3 castDelta(double dx, double dy, double dz) {
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return new Vec3(dx * .1, dy * .1 + Math.sqrt(distance) * .08, dz * .1);
    }

    /** Nullable; resolves only the actual live same-level golem, retrying naturally
     * when the client's owner/data packets arrive out of order. No world scan. */
    public EntityGolemBase getFisher() {
        int id = entityData.get(FISHER_ID);
        Optional<UUID> uuid = entityData.get(FISHER_UUID);
        if (id < 0 || uuid.isEmpty()) return null;
        Entity found = level().getEntity(id);
        if (!(found instanceof EntityGolemBase fisher) || fisher.level() != level()
                || !uuid.get().equals(fisher.getUUID()) || !fisher.isAlive() || fisher.getGolemCore() != 11) return null;
        // In particular, do not consult getFishingBobber(): setup precedes the
        // goal publishing its association, and removal must not clear a new cast.
        return fisher;
    }

    public BlockPos getWaterTarget() { return entityData.get(WATER_TARGET); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FISHER_ID, -1);
        builder.define(FISHER_UUID, Optional.empty());
        builder.define(WATER_TARGET, BlockPos.ZERO);
    }

    @Override public void tick() {
        if (isRemoved()) return;
        ServerLevel server = level() instanceof ServerLevel nativeLevel ? nativeLevel : null;
        EntityGolemBase fisher = null;
        if (!level().isClientSide) {
            fisher = getFisher();
            if (server == null || fisher == null || fisher.isNoAi() || !validPond()
                    || !GolemHarvestPlatform.mayInteract(fisher, getWaterTarget())
                    || !GolemHarvestPlatform.mayInteract(fisher, getWaterTarget().above())) {
                discard();
                return;
            }
        }

        Vec3 motion = getDeltaMovement();
        Vec3 checkedMotion = level().isClientSide && interpolationSteps > 0
                ? new Vec3(interpolationX - getX(), interpolationY - getY(), interpolationZ - getZ()) : motion;
        // Corrupt/addon motion cannot turn one short cast into an unbounded
        // chunk query. Normal home-range casts are far below this 64-block span.
        if (!finite(motion) || !finite(checkedMotion) || checkedMotion.lengthSqr() > 64.0 * 64.0
                || !loadedBox(getBoundingBox().expandTowards(checkedMotion).inflate(1))) {
            if (server != null) discard();
            return; // An unresolved/unloaded client view must not kill the cast.
        }
        if (server != null && (!GolemHarvestPlatform.mayInteract(fisher, blockPosition())
                || !GolemHarvestPlatform.mayInteract(fisher, BlockPos.containing(position().add(motion))))) {
            discard();
            return;
        }

        // Check loaded space before vanilla baseTick's environmental reads.
        super.tick();
        if (isRemoved()) return;
        // Native once-per-tick age, deliberately not the legacy extra ++.
        if (server != null && tickCount > 4000) {
            discard();
            return;
        }
        if (level().isClientSide && interpolationSteps > 0) {
            lerpPositionAndRotationStep(interpolationSteps, interpolationX, interpolationY, interpolationZ, interpolationYaw, interpolationPitch);
            --interpolationSteps;
            return;
        }
        if (server != null && random.nextFloat() < .02F) {
            server.sendParticles(ParticleTypes.SPLASH, getX() + random.nextFloat() - random.nextFloat(),
                    getY() + random.nextFloat(), getZ() + random.nextFloat() - random.nextFloat(),
                    2 + random.nextInt(2), .1, 0, .1, 0);
        }
        tickMotion(server != null);
    }

    private void tickMotion(boolean authoritative) {
        Vec3 motion = getDeltaMovement();
        if (inBlock) {
            inBlock = false;
            motion = motion.multiply(random.nextFloat() * .2F, random.nextFloat() * .2F, random.nextFloat() * .2F);
            setDeltaMovement(motion);
        }
        // Original block ray only: no entity sweep, player hook or latch.
        var hit = level().clip(new ClipContext(position(), position().add(motion),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            if (!level().getBlockState(hit.getBlockPos()).is(Blocks.WATER)) {
                if (authoritative) discard();
                else setDeltaMovement(Vec3.ZERO); // Wait for authoritative removal.
            } else inBlock = true;
            return; // A water block hit also skips movement/forces this update.
        }

        move(MoverType.SELF, motion); // B:136–217 moves BEFORE gravity/buoyancy.
        motion = getDeltaMovement();
        float yaw = (float) (Math.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * Mth.RAD_TO_DEG);
        yRotO = yaw - Mth.wrapDegrees(yaw - yRotO);
        xRotO = pitch - Mth.wrapDegrees(pitch - xRotO);
        setYRot(yRotO + (yaw - yRotO) * .2F);
        setXRot(xRotO + (pitch - xRotO) * .2F);

        float drag = onGround() || horizontalCollision ? .5F : .92F;
        AABB box = getBoundingBox();
        double wet = 0;
        for (int slice = 0; slice < 5; ++slice) {
            double low = box.minY + (box.maxY - box.minY) * slice / 5.0;
            double high = box.minY + (box.maxY - box.minY) * (slice + 1) / 5.0;
            if (intersectsWater(new AABB(box.minX, low, box.minZ, box.maxX, high, box.maxZ))) wet += .2;
        }
        double vy = motion.y + (double) .04F * (wet * 2 - 1);
        if (wet > 0) {
            drag = (float) (drag * .9);
            vy *= .8;
        }
        setDeltaMovement(motion.x * drag, vy * drag, motion.z * drag);
    }

    /** Actual vanilla source/flowing water with air, not a waterlogged solid.
     * The goal alone samples quality/loot; these checks cannot authorize output. */
    private boolean validPond() {
        BlockPos water = getWaterTarget();
        BlockPos above = water.above();
        return level().isInWorldBounds(water) && level().isInWorldBounds(above)
                && level().hasChunkAt(water) && level().hasChunkAt(above)
                && level().getBlockState(water).is(Blocks.WATER) && level().getBlockState(above).isAir();
    }

    private boolean loadedBox(AABB box) {
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        return level().isInWorldBounds(min) && level().isInWorldBounds(max) && level().hasChunksAt(min, max);
    }

    /** Five-slice material query mapped to native WATER block + fluid height.
     * loadedBox already covers these cells and the fluid's above-neighbor read. */
    private boolean intersectsWater(AABB slice) {
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(slice.minX), Mth.floor(slice.minY), Mth.floor(slice.minZ),
                Mth.floor(Math.nextDown(slice.maxX)), Mth.floor(Math.nextDown(slice.maxY)), Mth.floor(Math.nextDown(slice.maxZ)))) {
            var state = level().getBlockState(pos);
            if (state.is(Blocks.WATER) && pos.getY() < slice.maxY
                    && pos.getY() + state.getFluidState().getHeight(level(), pos) > slice.minY) return true;
        }
        return false;
    }

    private static boolean finite(Vec3 motion) {
        return Double.isFinite(motion.x) && Double.isFinite(motion.y) && Double.isFinite(motion.z);
    }

    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }

    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        double range = getBoundingBox().getSize() * 4 * 64;
        return distance < range * range;
    }

    @Override public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        // Plain Entity does not restore the spawn packet's motion itself.
        setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
        setOldPosAndRot();
        interpolationSteps = 0;
    }

    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        if (!level().isClientSide) {
            super.lerpTo(x, y, z, yaw, pitch, steps);
            return;
        }
        interpolationX = x;
        interpolationY = y;
        interpolationZ = z;
        interpolationYaw = yaw;
        interpolationPitch = pitch;
        interpolationSteps = Mth.clamp(steps, 1, 5);
    }

    // The registered EntityType is noSave. Never rehydrate an owner/cast/reward.
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}

package thaumcraft.common.entities.projectile;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftProjectiles;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.entities.ProjectileRules;
import thaumcraft.common.lib.world.BiomeColumns;

/**
 * TC4 EntityBottleTaint, the thrown bottle of taint.
 * <p>
 * The original is a plain {@code EntityThrowable} whose three inherited
 * throwable constants are {@code func_70185_h} gravity 0.05,
 * {@code func_70182_d} launch speed 0.5 and {@code func_70183_g} launch pitch
 * offset -20 (the same trio the vanilla potion and experience bottles use, so
 * the bottle leaves the hand twenty degrees above the aim). On impact it
 * poisons every untainted, non undead living entity of a five block splash
 * box for one hundred ticks, converts up to ten random biome columns to taint
 * and grows taint fibres on a replaceable block over a full cube.
 * <p>
 * Declared native adaptations: the biome column write uses the native
 * {@link BiomeColumns} replacement of the removed 1.7 two dimensional biome
 * array, the taint poison is the registered flux taint effect, and the client
 * burst is a hook instead of a proxy call.
 */
public final class EntityBottleTaint extends ThrowableProjectile {
    private static Consumer<EntityBottleTaint> clientImpact = ignored -> {};

    public EntityBottleTaint(EntityType<? extends EntityBottleTaint> type, Level level) {
        super(type, level);
    }

    public EntityBottleTaint(Level level) {
        this(ThaumcraftProjectiles.BOTTLE_TAINT.get(), level);
    }

    public EntityBottleTaint(Level level, LivingEntity owner) {
        this(level);
        setOwner(owner);
        float yaw = owner.getYRot() / 180F * (float) Math.PI;
        float pitch = owner.getXRot() / 180F * (float) Math.PI;
        setPos(owner.getX() - (double) (Mth.cos(yaw) * .16F), owner.getEyeY() - (double) .1F,
                owner.getZ() - (double) (Mth.sin(yaw) * .16F));
        // The original throws the bottle twenty degrees above the aim line.
        float aimed = pitch + ProjectileRules.BOTTLE_TAINT_PITCH_OFFSET / 180F * (float) Math.PI;
        shoot(-Mth.sin(yaw) * Mth.cos(aimed) * .4F, -Mth.sin(aimed) * .4F,
                Mth.cos(yaw) * Mth.cos(aimed) * .4F, ProjectileRules.BOTTLE_TAINT_LAUNCH_SPEED, 1F);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override protected double getDefaultGravity() {
        return ProjectileRules.BOTTLE_TAINT_GRAVITY;
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
        // The original bottle is never deflected and keeps its thrower.
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
        if (!(level() instanceof ServerLevel server)) return;
        splashTaint(server, blockPosition(), getOwner() instanceof LivingEntity owner ? owner : null);
        growTaintFibres(server, blockPosition());
        discard();
    }

    /**
     * The original impact of the broken bottle: it poisons everything around the spot and it
     * grows the taint fibres there. The method is static so that the effect can be applied at a
     * point, exactly like the original's onUpdate impact branch did at the bottle's position.
     */
    public static void burst(ServerLevel server, BlockPos spot, LivingEntity source) {
        splashTaint(server, spot, source);
        growTaintFibres(server, spot);
    }

    /** Original splash box: a zero size box at the impact point inflated by five on every axis. */
    private static void splashTaint(ServerLevel server, BlockPos spot, LivingEntity source) {
        AABB box = new AABB(spot.getX(), spot.getY(), spot.getZ(), spot.getX(), spot.getY(), spot.getZ())
                .inflate(ProjectileRules.BOTTLE_TAINT_SPLASH_RADIUS);
        for (Entity entity : server.getEntities(source, box)) {
            if (!(entity instanceof LivingEntity living)) continue;
            boolean tainted = living instanceof ITaintedMob;
            boolean undead = living.getType().is(EntityTypeTags.UNDEAD);
            if (!ProjectileRules.bottleTaintAffects(tainted, undead)) continue;
            living.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()),
                    ProjectileRules.BOTTLE_TAINT_POISON_TICKS, 0, false, true));
        }
    }

    /** Original biome conversion and taint fibre growth. */
    private static void growTaintFibres(ServerLevel server, BlockPos spot) {
        var random = server.getRandom();
        int x = spot.getX();
        int y = spot.getY();
        int z = spot.getZ();
        for (int attempt = 0; attempt < ProjectileRules.BOTTLE_TAINT_FIBRE_ATTEMPTS; attempt++) {
            int xx = x + ProjectileRules.bottleTaintFibreOffset(random.nextFloat(), random.nextFloat());
            int zz = z + ProjectileRules.bottleTaintFibreOffset(random.nextFloat(), random.nextFloat());
            // The original draws the fifty percent gate from the world random.
            if (!server.getRandom().nextBoolean()) continue;
            var holder = BiomeColumns.surfaceHolder(server, xx, zz);
            if (holder != null && holder.is(ThaumcraftBiomes.TAINT)) continue;
            BiomeColumns.set(server, xx, zz, ThaumcraftBiomes.TAINT);
            BlockPos pos = new BlockPos(xx, y, zz);
            boolean solidBelow = server.getBlockState(pos.below()).isCollisionShapeFullBlock(server, pos.below());
            boolean replaceable = server.getBlockState(pos).canBeReplaced();
            if (!ProjectileRules.bottleTaintFibreFits(solidBelow, replaceable)) continue;
            server.setBlock(pos, ThaumcraftTaint.FIBRES.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The client burst hook; the client module installs the particle effect. */
    public static void setClientImpact(Consumer<EntityBottleTaint> effect) {
        clientImpact = Objects.requireNonNull(effect);
    }
}

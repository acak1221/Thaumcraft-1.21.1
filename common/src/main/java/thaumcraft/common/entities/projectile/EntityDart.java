package thaumcraft.common.entities.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;

/**
 * TC4 EntityDart: a plain arrow fired by a golem's dart launcher. The original added only the launch geometry (below) and five
 * puffs of smoke on its first client tick; flight, damage ({@code ceil(speed * damage)}), sticking and despawn are the arrow's own,
 * which is what the native {@link AbstractArrow} provides. A dart is never picked up and drops nothing (it still has to carry an arrow stack, because the native arrow cannot be saved without one).
 *
 * <p>Declared native adaptation: the launch spread uses the current {@code Projectile.shoot} triangle distribution instead of the
 * 1.7 gaussian of the same nominal strength.
 */
public final class EntityDart extends AbstractArrow {
    private boolean first = true;

    public EntityDart(EntityType<? extends EntityDart> type, Level level) { super(type, level); }

    /**
     * Original constructor: starts just above the shooter's eye line, a fifth of a block towards the target, aimed at the target's
     * chest (eye minus 0.7) and lifted by a fifth of the horizontal distance to make up for gravity.
     */
    public EntityDart(Level level, LivingEntity shooter, LivingEntity target, float speed, float inaccuracy) {
        this(ThaumcraftGolems.DART.get(), level);
        setOwner(shooter);
        double y = shooter.getY() + shooter.getEyeHeight() - 0.1F;
        double dx = target.getX() - shooter.getX();
        double dy = target.getY() + target.getEyeHeight() - 0.7F - y;
        double dz = target.getZ() - shooter.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal >= 1.0E-7) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) -(Math.atan2(dy, horizontal) * 180.0 / Math.PI);
            moveTo(shooter.getX() + dx / horizontal / 5.0, y, shooter.getZ() + dz / horizontal / 5.0, yaw, pitch);
            shoot(dx, dy + (float) horizontal * 0.2F, dz, speed, inaccuracy);
        } else moveTo(shooter.getX(), y, shooter.getZ(), shooter.getYRot(), 0);
    }

    /** Never picked up (the pickup mode stays DISALLOWED), but the arrow saves this stack and refuses to save an empty one. */
    @Override protected ItemStack getDefaultPickupItem() { return new ItemStack(Items.ARROW); }

    @Override public void tick() {
        if (first && level().isClientSide) {
            first = false;
            Vec3 motion = getDeltaMovement();
            for (int puff = 0; puff < 5; puff++) {
                level().addParticle(ParticleTypes.SMOKE, getX() - motion.x / 1.5, getY() - motion.y / 1.5, getZ() - motion.z / 1.5,
                        motion.x / 9.0 + random.nextGaussian() * 0.01, motion.y / 9.0 + random.nextGaussian() * 0.01, motion.z / 9.0 + random.nextGaussian() * 0.01);
            }
        }
        super.tick();
    }
}

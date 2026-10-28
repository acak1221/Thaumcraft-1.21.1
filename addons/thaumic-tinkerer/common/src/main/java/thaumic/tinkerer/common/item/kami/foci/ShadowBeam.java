/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami.foci;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The beam of the Focus of Shadowbeam, run on the server in one go.
 *
 * <p>1.7.10 created a {@code Beam} (an {@code EntityThrowable} that was never added to the world) and called its
 * {@code onUpdate} 300 times in a loop. Every step moved it by 0.15 blocks along the look direction (plus a constant
 * 0.0075 bias per component of the direction), damaged the first entity within 0.3 blocks of its way (everything but the
 * caster, and no player without PvP) and mirrored it off the face of a block it hit, except a Nether portal, which an
 * {@code EntityThrowable} goes through (it only "enters" it). This class is that loop without an entity.</p>
 */
public final class ShadowBeam {
    /** 1.7.10 {@code initialOffset + length}. */
    public static final int MAX_TICKS = 2 + 298;
    /** The beam shows from the fourth step on. */
    public static final int FIRST_VISIBLE_STEP = 4;

    private static final double STEP = 0.15;
    private static final double BIAS = 0.0075;
    private static final double ENTITY_MARGIN = 0.3;
    private static final double CACHE_RANGE = 8;

    private ShadowBeam() {}

    /** The damage of one hit: 1.7.10 {@code potency > 0 ? (int) (DAMAGE + DAMAGE * (0.2 * potency)) : DAMAGE}. */
    public static int damage(int baseDamage, int potency) {
        return potency > 0 ? (int) (baseDamage + baseDamage * (0.2 * potency)) : baseDamage;
    }

    /** The mirror image of a direction at a face: 1.7.10 {@code normal * (-2 * (movement . normal)) + movement}. */
    public static Vec3 reflect(Vec3 movement, Direction face) {
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        return normal.scale(-2 * movement.dot(normal)).add(movement);
    }

    /** The starting point and the step of a beam shot by a player at the given look direction. */
    public static Vec3[] launch(Vec3 eye, float yaw, Vec3 look) {
        double yawRadians = yaw / 180.0 * Math.PI;
        Vec3 start = new Vec3(eye.x - Math.cos(yawRadians) * 0.16, eye.y - 0.1, eye.z - Math.sin(yawRadians) * 0.16);
        Vec3 step = new Vec3(look.x + BIAS, look.y + BIAS, look.z + BIAS).scale(1.5).scale(0.1);
        return new Vec3[] { start, step };
    }

    /**
     * Fires the beam; answers the points of its visible path (first visible point, every bounce, the end).
     *
     * @param baseDamage {@code ConfigHandler.baseDamageShadowbeam}
     */
    public static List<Vec3> fire(ServerPlayer player, int potency, int baseDamage) {
        ServerLevel level = player.serverLevel();
        Vec3[] launch = launch(player.getEyePosition(), player.getYRot(), player.getLookAngle());
        Vec3 pos = launch[0];
        Vec3 movement = launch[1];
        int damage = damage(baseDamage, potency);

        List<Vec3> path = new ArrayList<>();
        EntityCache entities = new EntityCache(level, player);
        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            Vec3 end = pos.add(movement);
            BlockHitResult block = level.clip(new ClipContext(pos, end, ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE, player));

            // An entity on the way takes the hit and the beam goes on; a block mirrors the beam.
            Entity target = entities.nearest(pos, end);
            if (target != null) {
                hurt(player, target, damage);
            } else if (block.getType() == HitResult.Type.BLOCK
                    && !level.getBlockState(block.getBlockPos()).is(Blocks.NETHER_PORTAL)) {
                if (tick >= FIRST_VISIBLE_STEP - 1) path.add(pos);
                movement = reflect(movement, block.getDirection());
            }
            pos = pos.add(movement);
            if (tick == FIRST_VISIBLE_STEP - 1 && path.isEmpty()) path.add(pos);
        }
        path.add(pos);
        return path;
    }

    private static void hurt(ServerPlayer shooter, Entity target, int damage) {
        if (target == shooter) return;
        if (target instanceof Player other && !shooter.canHarmPlayer(other)) return;
        DamageSource source = shooter.serverLevel().damageSources().playerAttack(shooter);
        target.hurt(source, damage);
    }

    /** The entities near the beam, looked up once per 8 blocks of its way. */
    private static final class EntityCache {
        private final ServerLevel level;
        private final Entity shooter;
        private AABB covered = new AABB(0, 0, 0, 0, 0, 0);
        private List<Entity> entities = List.of();

        EntityCache(ServerLevel level, Entity shooter) {
            this.level = level;
            this.shooter = shooter;
        }

        Entity nearest(Vec3 from, Vec3 to) {
            AABB step = new AABB(from, to).inflate(1);
            if (!covered.contains(step.getMinPosition()) || !covered.contains(step.getMaxPosition())) {
                covered = new AABB(from, from).inflate(CACHE_RANGE);
                entities = level.getEntities(shooter, covered, Entity::isPickable);
            }
            Entity nearest = null;
            double nearestDistance = 0;
            for (Entity entity : entities) {
                if (!entity.isAlive()) continue;
                var hit = entity.getBoundingBox().inflate(ENTITY_MARGIN).clip(from, to);
                if (hit.isPresent()) {
                    double distance = from.distanceTo(hit.get());
                    if (nearest == null || distance < nearestDistance) {
                        nearest = entity;
                        nearestDistance = distance;
                    }
                }
            }
            return nearest;
        }
    }
}

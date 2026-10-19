package thaumcraft.common.lib.world;

import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Item motion for the Axe of the Stream. Real, existing ItemEntity stacks are
 * retargeted in place: no spawn/copy/discard, so stack, owner, thrower and pickup
 * delay survive and nothing can be lost to a failed replacement spawn. The only
 * state is one transient target per item (see StreamAxeItemMixin); nothing is
 * cached globally, journalled or saved. Normal pickup, merging and despawn stay
 * in charge: a followed item is carried next to the player, never inserted.
 *
 * Declared narrowing versus EntityFollowingItem: no special-item fire immunity
 * and no client-side sparkle subclass; the server moves the item and the
 * vanilla entity tracker synchronises it.
 */
public final class StreamAxeItemMotion {
    private static final double VACUUM_RANGE = 10.0, FOLLOW_RANGE = 2.0;
    private static final int FX_PERIOD = 4, FX_CAP = 12;

    private StreamAxeItemMotion() {}

    /** Mixed into ItemEntity. Methods are prefixed so the mixin can implement them softly. */
    public interface Following {
        boolean thaumcraft$streamFollowing();
        /** Starts a fresh approach, unless the item is already following someone. */
        boolean thaumcraft$streamFollow(UUID target);
    }

    /**
     * Held right-click: every live item entity in the player-centred AABB +-10 is pulled
     * toward the player. No vis, wear, item cost or sneaking guard (the original checks
     * Shift only for chopping). Items already following a target are never overridden.
     */
    public static void vacuum(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        ServerLevel level = player.serverLevel();
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        AABB box = new AABB(px - VACUUM_RANGE, py - VACUUM_RANGE, pz - VACUUM_RANGE,
                px + VACUUM_RANGE, py + VACUUM_RANGE, pz + VACUUM_RANGE);
        boolean fx = player.tickCount % FX_PERIOD == 0;
        int shown = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !isFollowing(e))) {
            Vec3 motion = item.getDeltaMovement();
            // Original Y expression kept verbatim: itemY - playerY + playerHeight / 2.
            double[] next = StreamAxeGeometry.pull(motion.x, motion.y, motion.z,
                    item.getX() - px, item.getY() - py + player.getBbHeight() / 2.0, item.getZ() - pz);
            item.setDeltaMovement(next[0], next[1], next[2]);
            item.hasImpulse = true;
            if (fx && shown++ < FX_CAP) {
                level.sendParticles(ParticleTypes.BUBBLE_POP, item.getX(), item.getY() + 0.125, item.getZ(), 1, 0.06, 0.06, 0.06, 0.0);
            }
        }
    }

    /**
     * After a paid, witnessed endpoint removal: only items that did not exist before the
     * removal and now lie in the endpoint-centred AABB +-2 start following the player.
     * Old nearby items are never touched.
     */
    public static int followNewDrops(ServerPlayer player, BlockPos endpoint, Set<Integer> existingBefore) {
        ServerLevel level = player.serverLevel();
        double cx = endpoint.getX() + 0.5, cy = endpoint.getY() + 0.5, cz = endpoint.getZ() + 0.5;
        AABB box = new AABB(cx - FOLLOW_RANGE, cy - FOLLOW_RANGE, cz - FOLLOW_RANGE,
                cx + FOLLOW_RANGE, cy + FOLLOW_RANGE, cz + FOLLOW_RANGE);
        int started = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.isAlive() && !existingBefore.contains(e.getId()) && !isFollowing(e))) {
            if (item instanceof Following following && following.thaumcraft$streamFollow(player.getUUID())) started++;
        }
        return started;
    }

    /** Existing ids in the follow box, captured before the removal. */
    public static Set<Integer> idsAround(ServerLevel level, BlockPos endpoint) {
        double cx = endpoint.getX() + 0.5, cy = endpoint.getY() + 0.5, cz = endpoint.getZ() + 0.5;
        AABB box = new AABB(cx - FOLLOW_RANGE, cy - FOLLOW_RANGE, cz - FOLLOW_RANGE,
                cx + FOLLOW_RANGE, cy + FOLLOW_RANGE, cz + FOLLOW_RANGE);
        Set<Integer> ids = new java.util.HashSet<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) ids.add(item.getId());
        return ids;
    }

    private static boolean isFollowing(ItemEntity item) {
        return item instanceof Following following && following.thaumcraft$streamFollowing();
    }
}

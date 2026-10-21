package thaumcraft.common.lib.entities;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Original {@code World.getClosestPlayer(double, double, double, double)} of MC 1.7.10.
 *
 * <p>The original 1.7.10 implementation of the vanilla helper (the oracle of the vanilla
 * {@code net.minecraft.world.World} of 1.7.10) is:
 *
 * <pre>
 * double d1 = distance * distance;
 * EntityPlayer entityplayer = null;
 * List list = this.getEntitiesWithinAABB(EntityPlayer.class,
 *         AxisAlignedBB.getBoundingBox(x - distance, y - distance, z - distance,
 *                 x + distance, y + distance, z + distance));
 * for (int i = 0; i &lt; list.size(); ++i) {
 *     EntityPlayer entityplayer1 = (EntityPlayer)list.get(i);
 *     if (!entityplayer1.isCreative() &amp;&amp; entityplayer1.getDistanceSq(x, y, z) &lt; d1) {
 *         if (d1 &gt; 0.0D) {
 *             d1 = entityplayer1.getDistanceSq(x, y, z);
 *             entityplayer = entityplayer1;
 *         }
 *     }
 * }
 * return entityplayer;
 * </pre>
 *
 * <p>Three quirks of the original are preserved here:
 * <ul>
 *   <li>the search is a <em>sphere</em> of the given radius and not a box: a player that is
 *       further away than the radius is never found, no matter in which direction it lies;</li>
 *   <li>the comparison is strict, so a player that stands exactly on the radius is not
 *       found;</li>
 *   <li>a creative player is never found, because the original skips it.</li>
 * </ul>
 *
 * <p>The 1.21.1 world has no such helper, so the two eldritch tiles that used it in 1.7.10
 * ({@code TileEldritchTrap} and {@code TileEldritchCrabSpawner}) call this one instead.
 */
public final class OriginalPlayerSearch {
    private OriginalPlayerSearch() {}

    /**
     * The closest player around the given point, or null when there is none.
     *
     * @param level  the level the search runs in
     * @param x      the centre of the search
     * @param y      the centre of the search
     * @param z      the centre of the search
     * @param radius the radius of the search in blocks
     */
    public static Player closestPlayer(Level level, double x, double y, double z, double radius) {
        if (level == null) return null;
        double best = radius * radius;
        Player found = null;
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Player candidate : level.getEntitiesOfClass(Player.class, box)) {
            // Original World.getClosestPlayer: a creative player is never the closest player.
            boolean creative = candidate.getAbilities().instabuild;
            double distance = candidate.distanceToSqr(x, y, z);
            if (!accepts(distance, best, creative)) continue;
            best = distance;
            found = candidate;
        }
        return found;
    }

    /**
     * The rule of the original helper, separated from the world so that it can be asserted.
     *
     * <p>Original {@code World.getClosestPlayer}: a player is taken when it is not in creative
     * mode, when it stands strictly inside the current bound and when the bound is still
     * positive. The last rule is the quirk of the original: a player that stands exactly on
     * the centre of the search sets the bound to zero and no other player is taken afterwards.
     *
     * @param distanceSq the squared distance of the player to the centre of the search
     * @param bound      the squared bound that the search currently holds
     * @param creative   whether the player is in creative mode
     * @return true when the player becomes the closest player of the search
     */
    public static boolean accepts(double distanceSq, double bound, boolean creative) {
        if (creative) return false;
        if (!(distanceSq < bound)) return false;
        return bound > 0.0D;
    }
}

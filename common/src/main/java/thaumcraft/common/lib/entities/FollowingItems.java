package thaumcraft.common.lib.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.entities.EntityFollowingItem;
import thaumcraft.common.lib.utils.BlockUtils;

/**
 * The native wiring of the original following item.
 *
 * <p>The 1.7 original replaced a fresh drop with a drop that flies to the player
 * ({@code thaumcraft/common/lib/utils/BlockUtils.java:78-90}): the drop became an
 * {@link EntityFollowingItem} that kept the motion of the drop it replaced, and the drop itself was removed.
 * The elemental shovel (colour 3), the elemental axe (colour 10) and the primal crusher (colour 2) asked for
 * that behaviour, so the colour is the sparkle type of the following item.</p>
 *
 * <p>The entity layer owns the entity and the helper layer owns the drop, so the spawner is installed here.</p>
 */
public final class FollowingItems {
    private FollowingItems() {}

    /** Installs the following item spawner into the block helper layer. */
    public static void install() {
        BlockUtils.installFollowingItemSpawner(FollowingItems::spawn);
    }

    /**
     * Original {@code BlockUtils.harvestBlock}: the drop becomes an item that follows the player.
     *
     * <p>The original copied the stack of the drop, kept the motion of the drop and removed the drop itself.</p>
     */
    private static void spawn(ServerLevel level, ItemEntity drop, Player target, int color) {
        if (level == null || drop == null || target == null) return;
        EntityFollowingItem following = new EntityFollowingItem(level, drop.getX(), drop.getY(), drop.getZ(),
                drop.getItem().copy(), target, color);
        following.setDeltaMovement(drop.getDeltaMovement());
        level.addFreshEntity(following);
    }
}

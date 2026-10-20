package thaumcraft.api.research;

import net.minecraft.world.entity.player.Player;

/**
 * A scannable target of the original scan surface.
 *
 * <p>Forced 1.21.1 change: {@code EntityPlayer} became {@link Player}.</p>
 */
public interface IScanThing {
    /** True when this handler accepts the scanned target. */
    boolean checkThing(Player player, Object thing);

    /** The research key the scan of this target unlocks. */
    String getResearchKey(Player player, Object thing);

    /** Called after the scan of this target succeeded. */
    default void onSuccess(Player player, Object thing) { }
}

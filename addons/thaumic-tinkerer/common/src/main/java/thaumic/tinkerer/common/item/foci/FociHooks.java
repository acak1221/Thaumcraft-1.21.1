/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the seams of the wand foci to the loader modules.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.foci;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import thaumic.tinkerer.common.ThaumicTinkerer;

/**
 * What the Focus of Uprising needs from outside common code.
 *
 * <p>1.7.10 set {@code playerNetServerHandler.floatingTickCount = 0} on every cast, so that the server's "Flying is not
 * enabled on this server" check does not kick a player who keeps launching himself. In 1.21.1 the counter is the private
 * field {@code ServerGamePacketListenerImpl.aboveGroundTickCount}; common code cannot reach it without an accessor, so the
 * loader module assigns {@link #floatingTickReset} (Fabric: an accessor mixin on the listener, called from
 * {@code ThaumicTinkererFabric.onInitialize}). Until then the cast works, but a player who climbs for more than four seconds
 * on a server with {@code allow-flight=false} can be kicked (the first cast logs a warning).</p>
 */
public final class FociHooks {
    private static boolean warned;

    /** Sets the "ticks above the ground" counter of the connection of this player to zero. */
    public static volatile Consumer<ServerPlayer> floatingTickReset = player -> {
        if (!warned) {
            warned = true;
            ThaumicTinkerer.LOG.warn("Foci: the flight-kick hook is not connected; a player can be kicked for flying "
                    + "after four seconds of climbing with the Focus of Uprising on a server without allow-flight");
        }
    };

    private FociHooks() {}

    /** Called by the Focus of Uprising on the server on every cast. */
    public static void resetFloatingTicks(ServerPlayer player) {
        floatingTickReset.accept(player);
    }

    /** Loader entry point: replaces the reset (null puts the default back). */
    public static void setFloatingTickReset(Consumer<ServerPlayer> reset) {
        floatingTickReset = reset == null ? player -> {} : reset;
    }
}

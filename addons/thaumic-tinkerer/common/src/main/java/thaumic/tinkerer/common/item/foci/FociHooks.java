/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the seam of the wand foci to the server's flight check.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.foci;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * What the Focus of Uprising needs from the server.
 *
 * <p>1.7.10 set {@code playerNetServerHandler.floatingTickCount = 0} on every cast, so that the server's "Flying is not
 * enabled on this server" check does not kick a player who keeps launching himself. In 1.21.1 the counter is the private
 * int field {@code ServerGamePacketListenerImpl.aboveGroundTickCount} (it counts the ticks a player is floating, and the
 * server disconnects him above 80 when {@code allow-flight} is off). The loader layer owns the access to it
 * ({@code thaumic.tinkerer.common.hooks.FloatingTicks}, an accessor mixin of the loader module, no reflection), and the
 * group installs {@code FloatingTicks.RESET} here from {@code Foci.register()}. Nothing is installed by default: without the
 * installation a cast resets nothing.</p>
 */
public final class FociHooks {
    /** Sets the "ticks above the ground" counter of the connection of this player to zero; null: nothing is reset. */
    @Nullable
    public static volatile Consumer<ServerPlayer> floatingTickReset;

    private FociHooks() {}

    /** Called by the Focus of Uprising on the server on every cast that launches the player. */
    public static void resetFloatingTicks(ServerPlayer player) {
        Consumer<ServerPlayer> reset = floatingTickReset;
        if (reset != null) reset.accept(player);
    }

    /** Installs the reset ({@code FloatingTicks.RESET} at start-up; a test may replace it); null removes it. */
    public static void setFloatingTickReset(@Nullable Consumer<ServerPlayer> reset) {
        floatingTickReset = reset;
    }
}

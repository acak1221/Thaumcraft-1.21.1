/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.hooks;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Access to the "ticks above the ground" counter of a player's connection, {@code ServerGamePacketListenerImpl.aboveGroundTickCount}
 * (1.7.10: {@code NetHandlerPlayServer.floatingTickCount}). The server counts the ticks in which a player is floating
 * (not on the ground, not in a liquid, not climbing) while {@code allow-flight} is off, and disconnects him with "Flying is not
 * enabled on this server" above 80. The Focus of Uprising launches a player along his sight on every cast, so 1.7.10 set the
 * counter to zero on each cast (see {@code docs/api-requests/Foci.md} section 3).
 *
 * <p>The field is private. The loader module makes the connection class implement {@link Access} (Fabric: the mixin
 * {@code ServerGamePacketListenerFloatingMixin}; NeoForge: not written yet, an access transformer on the field or a
 * mixin of the same shape), and the methods here go through that interface. If the loader module did not (a platform without
 * the hook), {@link #isAvailable} is false and the reads and writes do nothing; nothing here uses reflection.</p>
 *
 * <p>{@link #RESET} has the type {@code FociHooks.setFloatingTickReset} takes, so the Foci group installs it with
 * {@code FociHooks.setFloatingTickReset(FloatingTicks.RESET)}.</p>
 */
public final class FloatingTicks {
    /** What the loader module's mixin adds to {@code ServerGamePacketListenerImpl}. Do not implement it elsewhere. */
    public interface Access {
        int thaumictinkerer$getAboveGroundTickCount();

        void thaumictinkerer$setAboveGroundTickCount(int ticks);
    }

    /** Sets the counter of the player to zero; does nothing for a player without a connection (a fake player) or without the hook. */
    public static final Consumer<ServerPlayer> RESET = FloatingTicks::reset;

    private FloatingTicks() {}

    /** True when the player has a connection that the loader module's hook reaches. */
    public static boolean isAvailable(ServerPlayer player) {
        return access(player) != null;
    }

    /** The counter of the player's connection, or {@code -1} when it has no connection or the hook is missing. */
    public static int get(ServerPlayer player) {
        Access access = access(player);
        return access == null ? -1 : access.thaumictinkerer$getAboveGroundTickCount();
    }

    /** Writes the counter; false when the player has no connection or the hook is missing. */
    public static boolean set(ServerPlayer player, int ticks) {
        Access access = access(player);
        if (access == null) return false;
        access.thaumictinkerer$setAboveGroundTickCount(ticks);
        return true;
    }

    /** Sets the counter to zero, so the server does not count the player as floating (the cast of the Focus of Uprising). */
    public static void reset(ServerPlayer player) {
        set(player, 0);
    }

    private static Access access(ServerPlayer player) {
        // A player that was just created has no connection yet, and a fake player may have none: nothing to kick either way.
        Object connection = player.connection;
        return connection instanceof Access access ? access : null;
    }
}

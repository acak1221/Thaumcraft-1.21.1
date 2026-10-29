/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the seams of the wand foci to the server's internals.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.foci;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import thaumic.tinkerer.common.ThaumicTinkerer;

/**
 * What the Focus of Uprising needs from the server.
 *
 * <p>1.7.10 set {@code playerNetServerHandler.floatingTickCount = 0} on every cast, so that the server's "Flying is not
 * enabled on this server" check does not kick a player who keeps launching himself. In 1.21.1 the counter is the private
 * int field {@code ServerGamePacketListenerImpl.aboveGroundTickCount} (it counts the ticks a player is floating, and the
 * server disconnects him above 80 when {@code allow-flight} is off). The add-on may not import the mod's accessor mixin for
 * it (the API has no call for it yet, see {@code docs/api-requests/Foci.md} section 3), so the default reset writes the field
 * by reflection, under its official name ({@code aboveGroundTickCount}, the NeoForge and the development name) or its
 * intermediary name ({@code field_14138}, Fabric in production). If neither can be reached the first cast logs a warning and
 * the cast goes on without the reset. A loader module or the API can replace the default with {@link #setFloatingTickReset}.</p>
 */
public final class FociHooks {
    /** The names of {@code ServerGamePacketListenerImpl.aboveGroundTickCount}: official (Mojang) and Fabric's intermediary. */
    private static final String[] COUNTER_NAMES = { "aboveGroundTickCount", "field_14138" };

    private static final Object LOCK = new Object();
    private static volatile boolean lookedUp;
    private static volatile Field counter;
    private static volatile boolean warned;

    /** What a cast does on the server: the reflective write of the counter, until something better is installed. */
    private static final Consumer<ServerPlayer> DEFAULT = FociHooks::resetByReflection;

    /** Sets the "ticks above the ground" counter of the connection of this player to zero. */
    public static volatile Consumer<ServerPlayer> floatingTickReset = DEFAULT;

    private FociHooks() {}

    /** Called by the Focus of Uprising on the server on every cast. */
    public static void resetFloatingTicks(ServerPlayer player) {
        floatingTickReset.accept(player);
    }

    /** Replaces the reset (a loader module with an accessor mixin, or the API when it has a call); null puts the default back. */
    public static void setFloatingTickReset(@Nullable Consumer<ServerPlayer> reset) {
        floatingTickReset = reset == null ? DEFAULT : reset;
    }

    /** True when the counter field can be reached by reflection under one of its names (the default reset then works). */
    public static boolean canReachCounter() {
        return counterField() != null;
    }

    /**
     * The counter of the connection of this player, or {@code -1} when the player has no connection (a fake player) or the
     * field cannot be reached.
     */
    public static int aboveGroundTicks(ServerPlayer player) {
        Field field = counterField();
        ServerGamePacketListenerImpl connection = player.connection;
        if (field == null || connection == null) return -1;
        try {
            return field.getInt(connection);
        } catch (IllegalAccessException | RuntimeException e) {
            return -1;
        }
    }

    /** Writes the counter of the connection of this player; false when the player has no connection or the field cannot be reached. */
    public static boolean setAboveGroundTicks(ServerPlayer player, int ticks) {
        Field field = counterField();
        ServerGamePacketListenerImpl connection = player.connection;
        if (field == null || connection == null) return false;
        try {
            field.setInt(connection, ticks);
            return true;
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
    }

    private static void resetByReflection(ServerPlayer player) {
        if (player.connection == null) return; // a fake player has no connection to be kicked
        if (!setAboveGroundTicks(player, 0) && !warned) {
            warned = true;
            ThaumicTinkerer.LOG.warn("Foci: the flight-kick counter of the connection cannot be reached; a player can be kicked "
                    + "for flying after four seconds of climbing with the Focus of Uprising on a server without allow-flight");
        }
    }

    @Nullable
    private static Field counterField() {
        if (!lookedUp) {
            synchronized (LOCK) {
                if (!lookedUp) {
                    counter = findCounter();
                    lookedUp = true;
                }
            }
        }
        return counter;
    }

    @Nullable
    private static Field findCounter() {
        for (String name : COUNTER_NAMES) {
            try {
                Field field = ServerGamePacketListenerImpl.class.getDeclaredField(name);
                if (field.getType() != int.class) continue;
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | RuntimeException notThisName) {
                // the other mapping, or (RuntimeException) a module that does not open the class
            }
        }
        return null;
    }
}

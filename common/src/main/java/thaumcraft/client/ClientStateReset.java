package thaumcraft.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import thaumcraft.client.lib.PlayerNotifications;
import thaumcraft.common.config.ThaumcraftSettings;

/**
 * The original client restore when the connection ends (oracle
 * {@code thaumcraft/common/lib/network/EventHandlerNetwork.java:69-81}, the {@code clientLogsOut} handler).
 *
 * <p>The original restored the client's own configuration when the connection ended, and the client kept no
 * state of the world it had just left. The port restores the same configuration and clears the client HUD
 * states that belong to the world that was left: the warp overlay, the runic shield, the player notifications,
 * the travel gear and the taint spores.</p>
 *
 * <p>The restore lives in the common layer, so every loader behaves like the original and not only the one
 * that happens to wire a platform disconnect event. The loader may still call {@link #reset()}; the restore is
 * idempotent.</p>
 */
public final class ClientStateReset {
    private static boolean registered;
    private ClientStateReset() {}

    /** The common layer owns the disconnect restore, exactly like the research settings do. */
    public static void init() {
        if (registered) return;
        registered = true;
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> reset());
    }

    /** The idempotent restore of the client's own state. */
    public static void reset() {
        thaumcraft.client.fx.EssentiaFlowClientEffects.clear();
        thaumcraft.client.lib.ScanOverlay.clear();
        ThaumcraftWarpClient.reset();
        ThaumcraftRunicClient.reset();
        PlayerNotifications.reset();
        ThaumcraftTravelGearClient.reset();
        ThaumcraftTaintSporesClient.reset();
        // The original EventHandlerNetwork.clientLogsOut restores the client configuration.
        ThaumcraftSettings.resetClient();
    }
}

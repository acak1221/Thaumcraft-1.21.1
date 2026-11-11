package thaumcraft.common.lib.inventoryscan;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.config.ThaumcraftTweaks;

/**
 * Scanning from an open inventory: with the thaumometer on the cursor, an item (or the player's own
 * portrait) under it is scanned after 25 ticks, like a scan in the world.
 *
 * <p>Not TC4: the behaviour of the 1.7.10 add-on Thaumic Inventory Scanning by BlayTheNinth, first
 * re-implemented for this port as a separate add-on and now part of the mod behind the setting
 * {@code inventory_scanning}. While that add-on ({@code tcinventoryscan}) is installed it keeps the
 * feature and this copy stays off, so nothing is drawn or scanned twice.</p>
 */
public final class InventoryScanning {
    /** The mod id of the companion add-on that provides the same feature. */
    public static final String ADDON_ID = "tcinventoryscan";
    // The original add-on increments 50 times across both START and END client events.
    public static final int SCAN_TICKS = 25;
    public static final int HEARTBEAT_TICKS = 5;
    public static final int HEARTBEAT_TIMEOUT = 12;
    private static boolean server, client;
    private static Boolean addon;

    private InventoryScanning() {}

    /** The setting is on and the companion add-on is not installed. */
    public static boolean enabled() { return ThaumcraftTweaks.inventoryScanning() && !addonInstalled(); }

    private static boolean addonInstalled() {
        Boolean known = addon;
        if (known == null) {
            // Unit tests have no loader; there the add-on is simply absent.
            try { known = Platform.isModLoaded(ADDON_ID); }
            catch (RuntimeException | LinkageError noLoader) { known = false; }
            addon = known;
        }
        return known;
    }

    /** Both sides: the request receiver, the reply sender and the server-side session clock. */
    public static void init() {
        if (server) return;
        server = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ScanRequest.TYPE, ScanRequest.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) InventoryScanServer.receive(player, payload);
                }));
        if (Platform.getEnvironment() == Env.SERVER) NetworkManager.registerS2CPayloadType(ScanReply.TYPE, ScanReply.STREAM_CODEC);
        // A client without this channel (an older build) simply gets no receipts.
        InventoryScanServer.installTransport((player, reply) -> {
            if (NetworkManager.canPlayerReceive(player, ScanReply.TYPE)) NetworkManager.sendToPlayer(player, reply);
        });
        TickEvent.SERVER_POST.register(InventoryScanServer::tick);
        PlayerEvent.PLAYER_QUIT.register(InventoryScanServer::disconnect);
        LifecycleEvent.SERVER_STOPPED.register(InventoryScanServer::stopped);
    }

    /** Physical client only: receipts go to the hover session. */
    public static void initClient(Consumer<ScanReply> receive) {
        if (client) return;
        client = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ScanReply.TYPE, ScanReply.STREAM_CODEC,
                (payload, context) -> context.queue(() -> receive.accept(payload)));
    }
}

package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IArchitect;
import thaumcraft.common.config.ThaumcraftArchitect;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.items.wands.foci.FocusData;

/** The original miscellaneous wand toggle; only the currently held, actually upgraded Architect focus may change the area. */
public final class WandArchitectNetworking {
    private static boolean initialized;
    private WandArchitectNetworking() {}
    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, WandArchitectPayload.TYPE, WandArchitectPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> { if (context.getPlayer() instanceof ServerPlayer player) handle(player); }));
    }
    public static void request() { NetworkManager.sendToServer(new WandArchitectPayload()); }
    public static boolean handle(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Architect control requires the server thread");
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.hasDisconnected() || player.connection == null
                || player.server.getPlayerList().getPlayer(player.getUUID()) != player) return false;
        ItemStack wand = player.getMainHandItem(), focus = FocusData.get(wand);
        if (!WandArchitect.enabled(wand) || !(focus.getItem() instanceof IArchitect architect)) return false;
        WandArchitect.Area next = WandArchitect.area(wand, focus).next(architect.getMaxAreaSize(focus), architect.getAxisModes(), player.isShiftKeyDown());
        wand.set(ThaumcraftArchitect.AREA.get(), next);
        player.getInventory().setChanged();
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("tc.architect.area", next.x(), next.y(), next.z(),
                Component.translatable("tc.architect.axis." + next.axis())), true);
        return true;
    }
}

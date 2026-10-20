package thaumcraft.common.lib.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;
import thaumcraft.common.items.equipment.ItemElementalShovel;

/** The misc toggle key with a Shovel of the Earthmover in the main hand turns its placing orientation on to the next of three. */
public final class ShovelOrientationNetworking {
    private static boolean initialized;
    private ShovelOrientationNetworking() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ShovelOrientationPayload.TYPE, ShovelOrientationPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) handle(player);
                }));
    }

    public static void request() { NetworkManager.sendToServer(new ShovelOrientationPayload()); }

    /** Returns whether the orientation changed. */
    public static boolean handle(ServerPlayer player) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Shovel orientation requires the server thread");
        if (!player.isAlive() || player.isSpectator() || player.server.getPlayerList().getPlayer(player.getUUID()) != player) return false;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemElementalShovel)) return false;
        held.set(ThaumcraftEquipment.SHOVEL_ORIENTATION.get(), ElementalShovelGeometry.next(ItemElementalShovel.orientation(held)));
        player.inventoryMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }
}

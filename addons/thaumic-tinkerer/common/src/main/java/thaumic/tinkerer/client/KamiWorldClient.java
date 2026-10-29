/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the client registrations of the KamiWorld group.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import thaumic.tinkerer.client.core.handler.kami.SoulHeartClientHandler;
import thaumic.tinkerer.client.gui.kami.GuiWarpGate;
import thaumic.tinkerer.client.gui.kami.GuiWarpGateDestinations;
import thaumic.tinkerer.client.render.tile.kami.RenderTileWarpGate;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.content.KamiWorld;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.network.packet.kami.PacketSoulHearts;

/**
 * Client registrations of the Kami world, called from {@code KamiWorld.registerClient()}: the screen of the Celestial
 * Gateway and the one that opens when a player sneaks on it, the block entity renderer of the circling cubes, the cut-out
 * render type of the gateway (the frame of its top face is a hole filled by an emissive layer), the Soul Hearts of the HUD
 * and the receiver of their packet.
 */
public final class KamiWorldClient {
    private KamiWorldClient() {}

    public static void register() {
        KamiHooks.setLocalPlayerSupplier(() -> Minecraft.getInstance().player);

        MenuRegistry.registerScreenFactory(KamiWorld.warpGateMenu(), GuiWarpGate::new);
        BlockEntityRendererRegistry.register(KamiWorld.warpGateType(), RenderTileWarpGate::new);
        RenderTypeRegistry.register(RenderType.cutout(), KamiWorld.warpGateBlock());

        // 1.7.10 TileWarpGate.updateEntity opened the screen from the tile; the tile asks this to.
        TileWarpGate.setDestinationsOpener(gate -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && !gate.isRemoved()) mc.setScreen(new GuiWarpGateDestinations(gate));
        });

        SoulHeartClientHandler.register();
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PacketSoulHearts.TYPE, PacketSoulHearts.CODEC,
                (packet, context) -> context.queue(() -> SoulHeartClientHandler.clientPlayerHP = packet.hearts()));
    }
}

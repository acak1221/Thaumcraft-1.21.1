/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.renderer.RenderType;
import thaumic.tinkerer.client.gui.GuiAnimationTablet;
import thaumic.tinkerer.client.gui.GuiRemotePlacer;
import thaumic.tinkerer.client.render.tile.RenderTileAnimationTablet;
import thaumic.tinkerer.client.render.tile.RenderTileCamoDevice;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * Client registration of the TileBlocks group: the two screens, the block entity renderers (the tablet's and the
 * camouflage renderer of the four camouflageable devices) and the render types. The relay's own renderer of the original
 * is deliberately not registered, as in 1.7.10 (see {@link thaumic.tinkerer.client.render.tile.RenderTileRelay}).
 */
public final class TileBlocksClient {
    private TileBlocksClient() {}

    public static void register() {
        MenuRegistry.registerScreenFactory(TileBlocks.animationTabletMenu(), GuiAnimationTablet::new);
        MenuRegistry.registerScreenFactory(TileBlocks.remotePlacerMenu(), GuiRemotePlacer::new);

        BlockEntityRendererRegistry.register(TileBlocks.animationTabletType(), RenderTileAnimationTablet::new);
        BlockEntityRendererRegistry.register(TileBlocks.interfaceType(), RenderTileCamoDevice::new);
        BlockEntityRendererRegistry.register(TileBlocks.dislocatorType(), RenderTileCamoDevice::new);
        BlockEntityRendererRegistry.register(TileBlocks.placerType(), RenderTileCamoDevice::new);
        BlockEntityRendererRegistry.register(TileBlocks.golemConnectorType(), RenderTileCamoDevice::new);

        // The devices' textures have cut-out pixels like every 1.7.10 block of the first render pass.
        RenderTypeRegistry.register(RenderType.cutout(), TileBlocks.animationTabletBlock(), TileBlocks.interfaceBlock(),
                TileBlocks.dislocatorBlock(), TileBlocks.remotePlacerBlock(), TileBlocks.golemConnectorBlock(),
                TileBlocks.mobilizerBlock(), TileBlocks.relayBlock());
    }
}

package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.gui.GuiArcaneBore;
import thaumcraft.client.renderers.tile.TileArcaneBoreBaseRenderer;
import thaumcraft.client.renderers.tile.TileArcaneBoreRenderer;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.tiles.TileArcaneBore;

/** Client-only factories. Root owns registry promotion, bootstrap and builtin
 * item routing; no renderer, target or GUI action supplies server authority. */
public final class ThaumcraftBoreClient {
    private static boolean initialized;
    private ThaumcraftBoreClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        MenuRegistry.registerScreenFactory(ThaumcraftArcaneBore.ARCANE_BORE_MENU.get(), GuiArcaneBore::new);
        BlockEntityRendererRegistry.register(ThaumcraftArcaneBore.ARCANE_BORE_BLOCK_ENTITY.get(), TileArcaneBoreRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftArcaneBore.ARCANE_BORE_BASE_BLOCK_ENTITY.get(), TileArcaneBoreBaseRenderer::new);
        // Original TileArcaneBore:496-501 - the dig event of the original makes
        // the client play the break sound of the dug block and its dig particles.
        TileArcaneBore.setClientEffects((level, bore, mining, target) -> {
            if (level instanceof ClientLevel client) BoreClientEffects.dig(client, bore, mining, target);
        });
    }
}

package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import thaumcraft.client.gui.GuiArcaneWorkbench;
import thaumcraft.client.renderers.tile.TileArcaneWorkbenchRenderer;
import thaumcraft.common.config.ThaumcraftArcane;

/** Client-only integration point; common bootstrap never loads rendering classes. */
public final class ThaumcraftArcaneClient {
    private static boolean initialized;
    private ThaumcraftArcaneClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        MenuRegistry.registerScreenFactory(ThaumcraftArcane.ARCANE_WORKBENCH_MENU.get(), GuiArcaneWorkbench::new);
        BlockEntityRendererRegistry.register(ThaumcraftArcane.ARCANE_WORKBENCH_BLOCK_ENTITY.get(), TileArcaneWorkbenchRenderer::new);
    }
}

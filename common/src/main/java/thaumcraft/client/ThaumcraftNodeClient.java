package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import thaumcraft.client.renderers.tile.TileNodeRenderer;
import thaumcraft.common.config.ThaumcraftContent;

/** Client-only entry point; common bootstrap must never load this class. */
public final class ThaumcraftNodeClient {
    private static boolean initialized;
    private ThaumcraftNodeClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), TileNodeRenderer::new);
    }
}

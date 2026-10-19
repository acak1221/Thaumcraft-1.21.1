package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import thaumcraft.client.renderers.tile.TileAlembicRenderer;
import thaumcraft.common.config.ThaumcraftAlembics;

/** Client-only registration; no renderer is loaded by the common/server bootstrap. */
public final class ThaumcraftAlembicClient {
    private static boolean initialized;
    private ThaumcraftAlembicClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftAlembics.ALEMBIC_BLOCK_ENTITY.get(), TileAlembicRenderer::new);
    }
}

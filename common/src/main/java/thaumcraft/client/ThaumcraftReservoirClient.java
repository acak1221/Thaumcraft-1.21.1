package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import thaumcraft.client.renderers.tile.TileEssentiaReservoirRenderer;
import thaumcraft.common.config.ThaumcraftTubes;

/** Client-only registration; the native reservoir/server never loads renderer classes. */
public final class ThaumcraftReservoirClient {
    private static boolean initialized;
    private ThaumcraftReservoirClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftTubes.RESERVOIR_BLOCK_ENTITY.get(), TileEssentiaReservoirRenderer::new);
    }
}

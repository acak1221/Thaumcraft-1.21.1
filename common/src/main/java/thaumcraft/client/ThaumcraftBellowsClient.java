package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import thaumcraft.client.renderers.tile.TileBellowsRenderer;
import thaumcraft.common.config.ThaumcraftBellows;

public final class ThaumcraftBellowsClient {
    private ThaumcraftBellowsClient() {}
    public static void init() { BlockEntityRendererRegistry.register(ThaumcraftBellows.BELLOWS_TILE.get(), TileBellowsRenderer::new); }
}

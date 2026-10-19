package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import thaumcraft.client.renderers.tile.TileChestHungryRenderer;
import thaumcraft.common.config.ThaumcraftHungryChest;

/** Client-only presentation. The vanilla GENERIC_9x3 menu already has a screen. */
public final class ThaumcraftHungryChestClient {
    private static boolean initialized;
    private ThaumcraftHungryChestClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftHungryChest.HUNGRY_CHEST_BLOCK_ENTITY.get(), TileChestHungryRenderer::new);
    }
}

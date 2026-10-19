package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.renderers.entity.RenderEmber;
import thaumcraft.common.config.ThaumcraftEmbers;

/** Client-only binding. No shared atlas mutation or replacement flame particles. */
public final class ThaumcraftEmberClient {
    private static boolean initialized;
    private ThaumcraftEmberClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EntityRendererRegistry.register(ThaumcraftEmbers.EMBER, RenderEmber::new);
    }
}

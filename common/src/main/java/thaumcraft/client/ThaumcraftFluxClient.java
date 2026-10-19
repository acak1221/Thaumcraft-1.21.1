package thaumcraft.client;

import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.common.config.ThaumcraftFlux;

/** Native animated original flux textures; no vanilla water/lava rendering substitution. */
public final class ThaumcraftFluxClient {
    private static boolean initialized;
    private ThaumcraftFluxClient() {}
    public static void init() {
        if (initialized) return;
        initialized = true;
        RenderTypeRegistry.register(RenderType.translucent(), ThaumcraftFlux.FLUX_GAS.get(), ThaumcraftFlux.FLUX_GOO.get());
    }
}

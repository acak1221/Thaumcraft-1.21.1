package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import thaumcraft.client.fx.particles.GrowthLampParticle;
import thaumcraft.client.renderers.tile.TileEssentiaLampRenderer;
import thaumcraft.common.config.ThaumcraftBiologicalLamps;
import thaumcraft.common.lib.network.GrowthLampFxNetworking;

/** Loader-client-only entry point: both lamp nozzles and original green post-growth sparkles. */
public final class ThaumcraftBiologicalLampClient {
    private static boolean initialized;
    private ThaumcraftBiologicalLampClient() {}
    public static void init() {
        if (initialized) return; initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftBiologicalLamps.GROWTH_TILE.get(), TileEssentiaLampRenderer::new);
        BlockEntityRendererRegistry.register(ThaumcraftBiologicalLamps.FERTILITY_TILE.get(), TileEssentiaLampRenderer::new);
        GrowthLampFxNetworking.initClient(payload -> {
            var client = Minecraft.getInstance(); var level = client.level;
            if (level == null || !level.dimension().location().equals(payload.dimension()) || !level.hasChunkAt(payload.target())) return;
            var setting = client.options.particles().get();
            int count = setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? 7 : 14;
            for (int i = 0; i < count; ++i) client.particleEngine.add(new GrowthLampParticle(level, payload.target()));
        });
    }
}

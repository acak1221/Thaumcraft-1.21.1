package thaumcraft.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import java.util.Random;
import thaumcraft.client.fx.particles.CrystalSparkParticle;
import thaumcraft.client.renderers.tile.TileCrystalRenderer;
import thaumcraft.common.blocks.BlockCrystal;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.lib.crystals.CrystalClusterGeometry;

/** Loader-neutral renderer/effects hooks. Fabric must additionally bind renderItem to all seven builtin/entity item models. */
public final class ThaumcraftCrystalClustersClient {
    private static boolean initialized;
    private ThaumcraftCrystalClustersClient() {}
    public static void init() {
        if (initialized) return; initialized = true;
        BlockEntityRendererRegistry.register(ThaumcraftCrystalClusters.CRYSTAL_TILE.get(), TileCrystalRenderer::new);
        BlockCrystal.setClientEffects(CrystalSparkParticle::sparkle);
        // Original mixed block-color query starts a fresh private Random, independent of the server/world stream.
        ColorHandlerRegistry.registerBlockColors((state, level, position, tint) -> CrystalClusterGeometry.color(new Random().nextInt(6)), ThaumcraftCrystalClusters.MIXED);
    }
}

package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.fx.particles.ResourceParticle;
import thaumcraft.client.renderers.entity.RenderAlumentum;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.tiles.TileNitor;

/** Client-only resource effects, using the existing, original TC4 particle atlas. */
public final class ThaumcraftResourceClient {
    private static boolean initialized;
    private ThaumcraftResourceClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EntityRendererRegistry.register(ThaumcraftResources.ALUMENTUM, RenderAlumentum::new);
        TileNitor.setClientEffects(new ResourceParticle.NitorEffects());
        EntityAlumentum.setClientEffects(ResourceParticle::alumentumTrail);
    }
}

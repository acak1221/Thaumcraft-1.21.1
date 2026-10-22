package thaumcraft.client;

import thaumcraft.client.fx.particles.ResourceParticle;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.tiles.TileNitor;

/** Client-only resource effects, using the existing, original TC4 particle atlas. */
public final class ThaumcraftResourceClient {
    private static boolean initialized;
    private ThaumcraftResourceClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        TileNitor.setClientEffects(new ResourceParticle.NitorEffects());
        EntityAlumentum.setClientEffects(ResourceParticle::alumentumTrail);
    }
}

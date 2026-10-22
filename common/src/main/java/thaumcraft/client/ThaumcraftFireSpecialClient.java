package thaumcraft.client;

import thaumcraft.client.fx.particles.FocusFireballParticle;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;

public final class ThaumcraftFireSpecialClient {
    private static boolean initialized;
    private ThaumcraftFireSpecialClient() {}
    public static void init() {
        if (initialized) return; initialized=true;
        EntityExplosiveOrb.setClientTrailHandler(FocusFireballParticle::trail);
    }
}

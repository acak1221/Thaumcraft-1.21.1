package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.fx.particles.FocusFireballParticle;
import thaumcraft.client.renderers.entity.RenderFocusOrb;
import thaumcraft.common.config.ThaumcraftExplosiveOrbs;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;

public final class ThaumcraftFireSpecialClient {
    private static boolean initialized;
    private ThaumcraftFireSpecialClient() {}
    public static void init() {
        if (initialized) return; initialized=true;
        EntityRendererRegistry.register(ThaumcraftExplosiveOrbs.EXPLOSIVE_ORB, context -> new RenderFocusOrb<>(context,true));
        EntityExplosiveOrb.setClientTrailHandler(FocusFireballParticle::trail);
    }
}

package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.fx.particles.PrimalWispParticle;
import thaumcraft.client.renderers.entity.RenderPrimalOrb;
import thaumcraft.common.config.ThaumcraftPrimalFoci;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;

public final class ThaumcraftPrimalFociClient {
    private static boolean initialized;
    private ThaumcraftPrimalFociClient(){}
    public static void init(){
        if(initialized)return;initialized=true;
        EntityRendererRegistry.register(ThaumcraftPrimalFoci.PRIMAL_ORB,RenderPrimalOrb::new);
        EntityPrimalOrb.setClientTrail(PrimalWispParticle::trail);EntityPrimalOrb.setClientImpact(PrimalWispParticle::impact);
    }
}

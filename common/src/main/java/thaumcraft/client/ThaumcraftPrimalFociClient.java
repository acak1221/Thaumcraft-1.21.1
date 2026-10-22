package thaumcraft.client;

import thaumcraft.client.fx.particles.PrimalWispParticle;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;

public final class ThaumcraftPrimalFociClient {
    private static boolean initialized;
    private ThaumcraftPrimalFociClient(){}
    public static void init(){
        if(initialized)return;initialized=true;
        EntityPrimalOrb.setClientTrail(PrimalWispParticle::trail);EntityPrimalOrb.setClientImpact(PrimalWispParticle::impact);
    }
}

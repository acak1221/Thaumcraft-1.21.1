package thaumcraft.client;

import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.fx.particles.WispBoltParticle;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.lib.network.WispZapNetworking;

public final class ThaumcraftMonsterClient {
    private static boolean initialized;
    private ThaumcraftMonsterClient() {}
    public static void init() {
        if (initialized) return; initialized = true;
        EntityFireBat.setClientEffects(MonsterSpriteParticle::fireBat);
        EntityWisp.setClientEffects(MonsterSpriteParticle.WISP_EFFECTS);
        WispZapNetworking.initClient(WispBoltParticle::accept);
    }
}

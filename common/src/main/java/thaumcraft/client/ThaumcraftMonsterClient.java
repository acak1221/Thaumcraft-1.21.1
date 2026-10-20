package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.renderers.entity.RenderBrainyZombie;
import thaumcraft.client.renderers.entity.RenderFireBat;
import thaumcraft.client.renderers.entity.RenderWisp;
import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.fx.particles.WispBoltParticle;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.lib.network.WispZapNetworking;
import thaumcraft.common.config.ThaumcraftMonsters;

public final class ThaumcraftMonsterClient {
    private static boolean initialized;
    private ThaumcraftMonsterClient() {}
    public static void init() {
        if (initialized) return; initialized = true;
        EntityRendererRegistry.register(ThaumcraftMonsters.BRAINY_ZOMBIE, RenderBrainyZombie::new);
        EntityRendererRegistry.register(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE, RenderBrainyZombie::new);
        EntityRendererRegistry.register(ThaumcraftMonsters.FIREBAT, RenderFireBat::new);
        EntityRendererRegistry.register(ThaumcraftMonsters.WISP, RenderWisp::new);
        EntityFireBat.setClientEffects(MonsterSpriteParticle::fireBat);
        EntityWisp.setClientEffects(MonsterSpriteParticle.WISP_EFFECTS);
        WispZapNetworking.initClient(WispBoltParticle::accept);
    }
}

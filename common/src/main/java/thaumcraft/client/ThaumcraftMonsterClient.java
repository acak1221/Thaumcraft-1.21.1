package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.renderers.entity.RenderBrainyZombie;
import thaumcraft.common.config.ThaumcraftMonsters;

public final class ThaumcraftMonsterClient {
    private static boolean initialized;
    private ThaumcraftMonsterClient() {}
    public static void init() {
        if (initialized) return; initialized = true;
        EntityRendererRegistry.register(ThaumcraftMonsters.BRAINY_ZOMBIE, RenderBrainyZombie::new);
        EntityRendererRegistry.register(ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE, RenderBrainyZombie::new);
    }
}

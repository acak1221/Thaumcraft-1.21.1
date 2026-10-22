package thaumcraft.client;

import thaumcraft.client.fx.particles.FXArc;
import thaumcraft.common.entities.monster.EntityEldritchGolem;

/**
 * Client registration for the eldritch, cultist and taint animal renderers.
 * The entity types come from the eldritch, cultist and taint animal content classes;
 * every renderer here is the original TC4 client presentation of its mob.
 */
public final class ThaumcraftEldritchClient {
    private static boolean initialized;
    private ThaumcraftEldritchClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        // Original Thaumcraft.proxy.arcLightning: the golem's lightning arc and its target sparkle.
        EntityEldritchGolem.setClientEffects(FXArc::arc);
    }
}

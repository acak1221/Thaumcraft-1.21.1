package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import thaumcraft.client.renderers.entity.RenderCultist;
import thaumcraft.client.renderers.entity.RenderCultistPortal;
import thaumcraft.client.renderers.entity.RenderEldritchCrab;
import thaumcraft.client.renderers.entity.RenderEldritchGolem;
import thaumcraft.client.renderers.entity.RenderEldritchWarden;
import thaumcraft.client.renderers.entity.RenderInhabitedZombie;
import thaumcraft.client.renderers.entity.RenderTaintChicken;
import thaumcraft.client.renderers.entity.RenderTaintCow;
import thaumcraft.client.renderers.entity.RenderTaintPig;
import thaumcraft.client.renderers.entity.RenderTaintSheep;
import thaumcraft.client.renderers.entity.RenderTaintVillager;
import thaumcraft.client.renderers.entity.RenderThaumicSlime;
import thaumcraft.client.renderers.entity.RenderWatcher;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftTaintAnimals;

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
        EntityRendererRegistry.register(ThaumcraftEldritchMobs.ELDRITCH_WARDEN, RenderEldritchWarden::new);
        EntityRendererRegistry.register(ThaumcraftEldritchMobs.ELDRITCH_GOLEM, RenderEldritchGolem::new);
        EntityRendererRegistry.register(ThaumcraftEldritchMobs.ELDRITCH_CRAB, RenderEldritchCrab::new);
        EntityRendererRegistry.register(ThaumcraftEldritchMobs.INHABITED_ZOMBIE, RenderInhabitedZombie::new);
        EntityRendererRegistry.register(ThaumcraftEldritchMobs.WATCHER, RenderWatcher::new);
        EntityRendererRegistry.register(ThaumcraftEldritchMobs.THAUMIC_SLIME, RenderThaumicSlime::new);
        EntityRendererRegistry.register(ThaumcraftCultists.CULTIST_CLERIC, RenderCultist::new);
        EntityRendererRegistry.register(ThaumcraftCultists.CULTIST_KNIGHT, RenderCultist::new);
        EntityRendererRegistry.register(ThaumcraftCultists.CULTIST_LEADER, RenderCultist::new);
        EntityRendererRegistry.register(ThaumcraftCultists.CULTIST_PORTAL, RenderCultistPortal::new);
        EntityRendererRegistry.register(ThaumcraftCultists.CULTIST_PORTAL_GREATER, RenderCultistPortal::new);
        EntityRendererRegistry.register(ThaumcraftTaintAnimals.TAINT_CHICKEN, RenderTaintChicken::new);
        EntityRendererRegistry.register(ThaumcraftTaintAnimals.TAINT_COW, RenderTaintCow::new);
        EntityRendererRegistry.register(ThaumcraftTaintAnimals.TAINT_PIG, RenderTaintPig::new);
        EntityRendererRegistry.register(ThaumcraftTaintAnimals.TAINT_SHEEP, RenderTaintSheep::new);
        EntityRendererRegistry.register(ThaumcraftTaintAnimals.TAINT_VILLAGER, RenderTaintVillager::new);
    }
}

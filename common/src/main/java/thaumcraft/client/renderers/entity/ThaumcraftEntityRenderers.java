package thaumcraft.client.renderers.entity;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import thaumcraft.common.config.ThaumcraftAspectOrbs;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.config.ThaumcraftEmbers;
import thaumcraft.common.config.ThaumcraftExplosiveOrbs;
import thaumcraft.common.config.ThaumcraftFrost;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.config.ThaumcraftPrimalFoci;
import thaumcraft.common.config.ThaumcraftProjectiles;
import thaumcraft.common.config.ThaumcraftResources;
import thaumcraft.common.config.ThaumcraftShockOrbs;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.config.ThaumcraftTaintCreeper;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.EntityAspectOrb;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EntityGolemBobber;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityCultistKnight;
import thaumcraft.common.entities.monster.EntityCultistLeader;
import thaumcraft.common.entities.monster.EntityCultistPortal;
import thaumcraft.common.entities.monster.EntityEldritchCrab;
import thaumcraft.common.entities.monster.EntityEldritchGolem;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.EntityEldritchWarden;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityInhabitedZombie;
import thaumcraft.common.entities.monster.EntityMindSpider;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityTaintChicken;
import thaumcraft.common.entities.monster.EntityTaintCreeper;
import thaumcraft.common.entities.monster.EntityTaintCrawler;
import thaumcraft.common.entities.monster.EntityTaintCow;
import thaumcraft.common.entities.monster.EntityTaintPig;
import thaumcraft.common.entities.monster.EntityTaintSeed;
import thaumcraft.common.entities.monster.EntityTaintSheep;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.entities.monster.EntityTaintSpore;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.entities.monster.EntityTaintVillager;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWatcher;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.entities.projectile.EntityDart;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.entities.projectile.EntityEmber;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;
import thaumcraft.common.entities.projectile.EntityFrostShard;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.entities.projectile.EntityPechBlast;
import thaumcraft.common.entities.projectile.EntityPrimalArrow;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;
import thaumcraft.common.entities.projectile.EntityShockOrb;

/**
 * Every entity type of the port and the client renderer that draws it.
 *
 * <p>Minecraft 1.21.1 looks the renderer of an entity up <em>by entity type</em> and has no
 * fallback: {@code EntityRenderDispatcher.getRenderer} answers
 * {@code this.renderers.get(entity.getType())} and the client throws a
 * {@code NullPointerException} as soon as an entity without a registered renderer is drawn.
 * The original 1.7.10 registry had no such lookup, so the original
 * {@code ClientProxy.setupEntityRenderers} registered a renderer for every entity the mod
 * added (oracle {@code thaumcraft/client/ClientProxy.java:464-517}). This class is the
 * native mirror of that list and the single place where the native renderers are
 * registered.</p>
 *
 * <p>The renderer of an entity type is the one of the original registration:</p>
 * <ul>
 *   <li>the original {@code RenderSpecialItem} draws the special item and the permanent
 *       item (oracle {@code ClientProxy.java:466} and {@code :468}), the original
 *       {@code RenderFollowingItem} draws the following item ({@code :467}) and the original
 *       {@code RenderItem} draws the item grate ({@code :465});</li>
 *   <li>the original {@code RenderSnowball} draws the bottle of taint with the bottle item
 *       ({@code :513}); the native counterpart of that renderer is the vanilla renderer of a
 *       thrown item;</li>
 *   <li>the original {@code RenderElectricOrb} draws the golem orb ({@code :476}) and the
 *       shock orb ({@code :478}), the original {@code RenderExplosiveOrb} the explosive orb
 *       ({@code :479}) and the original {@code RenderPrimalArrow} the primal arrow
 *       ({@code :488});</li>
 *   <li>the original {@code RenderTaintacle} draws the taintacle ({@code :492}), the small
 *       taintacle ({@code :493}) and the giant taintacle ({@code :494}).</li>
 * </ul>
 *
 * <p>The renderer providers are written as lambda bodies and not as constructor references
 * on purpose: a lambda body keeps the renderer class unloaded until the renderer is really
 * built, so the coverage test can read this table in a plain unit test JVM.</p>
 */
public final class ThaumcraftEntityRenderers {
    /** One entity type and the renderer that draws it. */
    public static final class Binding<T extends Entity> {
        private final Class<T> entityClass;
        private final RegistrySupplier<? extends EntityType<? extends T>> type;
        private final EntityRendererProvider<T> provider;

        private Binding(Class<T> entityClass, RegistrySupplier<? extends EntityType<? extends T>> type,
                        EntityRendererProvider<T> provider) {
            this.entityClass = entityClass;
            this.type = type;
            this.provider = provider;
        }

        /** The registry id of the entity type this binding draws. */
        public ResourceLocation id() {
            return type.getId();
        }

        /**
         * The entity class the renderer of this binding accepts.
         *
         * <p>Minecraft 1.21.1 renders the item pickup animation with a copy of the picked
         * entity, and {@code ItemEntity.copy()} keeps the entity type but not the entity
         * class, so the renderer of an item entity has to accept any {@link ItemEntity} and
         * not only its own subclass.</p>
         */
        public Class<T> entityClass() {
            return entityClass;
        }

        /** Registers this renderer with the native entity renderer registry. */
        public void register() {
            EntityRendererRegistry.register(type, provider);
        }

        @Override public String toString() {
            return type.getId() + " -> " + entityClass.getSimpleName();
        }
    }

    /**
     * One binding of an entity type and its renderer.
     *
     * <p>The entity class is the explicit witness of the renderer: the renderer of the
     * special item draws a plain item entity, the renderer of the giant taintacle draws a
     * taintacle and the renderer of the prime cultist portal draws a cultist portal, so
     * the witness is the class of the renderer and not always the class of the entity.</p>
     */
    private static <T extends Entity> Binding<T> of(Class<T> entityClass,
                                                      RegistrySupplier<? extends EntityType<? extends T>> type,
                                                      EntityRendererProvider<T> provider) {
        return new Binding<>(entityClass, type, provider);
    }

    /**
     * Every entity type the port registers. The list mirrors the registration classes of
     * {@code thaumcraft.common.config}; the unit test compares it with the ids those
     * classes really register, so a new entity type cannot slip in without a renderer.
     */
    public static final List<RegistrySupplier<? extends EntityType<?>>> ENTITY_TYPES = List.of(
            ThaumcraftContent.SPECIAL_ITEM,
            ThaumcraftContent.INVENTORY_ITEM,
            ThaumcraftProjectiles.BOTTLE_TAINT,
            ThaumcraftProjectiles.GOLEM_ORB,
            ThaumcraftProjectiles.PRIMAL_ARROW,
            ThaumcraftProjectiles.FOLLOW_ITEM,
            ThaumcraftProjectiles.PERMANENT_ITEM,
            ThaumcraftProjectiles.ITEM_GRATE,
            ThaumcraftTaint.FALLING_TAINT,
            ThaumcraftTaint.TAINT_SPIDER,
            ThaumcraftTaint.TAINT_SPORE,
            ThaumcraftTaint.TAINT_SWARMER,
            ThaumcraftTaint.TAINT_SWARM,
            ThaumcraftTaint.TAINTACLE,
            ThaumcraftTaint.TAINTACLE_SMALL,
            ThaumcraftTaintAnimals.TAINT_CHICKEN,
            ThaumcraftTaintAnimals.TAINT_COW,
            ThaumcraftTaintAnimals.TAINT_PIG,
            ThaumcraftTaintAnimals.TAINT_SHEEP,
            ThaumcraftTaintAnimals.TAINT_VILLAGER,
            ThaumcraftTaintAnimals.TAINT_CRAWLER,
            ThaumcraftTaintAnimals.TAINT_SEED,
            ThaumcraftTaintAnimals.TAINT_SEED_PRIME,
            ThaumcraftTaintCreeper.TAINT_CREEPER,
            ThaumcraftCultists.CULTIST_KNIGHT,
            ThaumcraftCultists.CULTIST_CLERIC,
            ThaumcraftCultists.CULTIST_LEADER,
            ThaumcraftCultists.CULTIST_PORTAL,
            ThaumcraftCultists.CULTIST_PORTAL_GREATER,
            ThaumcraftMonsters.BRAINY_ZOMBIE,
            ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE,
            ThaumcraftMonsters.FIREBAT,
            ThaumcraftMonsters.WISP,
            ThaumcraftWarp.MIND_SPIDER,
            ThaumcraftWarp.ELDRITCH_GUARDIAN,
            ThaumcraftWarp.ELDRITCH_ORB,
            ThaumcraftGolems.GOLEM,
            ThaumcraftGolems.TRUNK,
            ThaumcraftGolems.DART,
            ThaumcraftGolems.GOLEM_BOBBER,
            ThaumcraftPech.PECH,
            ThaumcraftPech.PECH_BLAST,
            ThaumcraftPrimalFoci.PRIMAL_ORB,
            ThaumcraftEmbers.EMBER,
            ThaumcraftResources.ALUMENTUM,
            ThaumcraftShockOrbs.SHOCK_ORB,
            ThaumcraftExplosiveOrbs.EXPLOSIVE_ORB,
            ThaumcraftAspectOrbs.ORB,
            ThaumcraftEldritchMobs.ELDRITCH_WARDEN,
            ThaumcraftEldritchMobs.ELDRITCH_GOLEM,
            ThaumcraftEldritchMobs.ELDRITCH_CRAB,
            ThaumcraftEldritchMobs.INHABITED_ZOMBIE,
            ThaumcraftEldritchMobs.WATCHER,
            ThaumcraftEldritchMobs.THAUMIC_SLIME,
            ThaumcraftEldritchMobs.TAINTACLE_GIANT,
            ThaumcraftFrost.FROST_SHARD);

    /** The renderer of every entity type, the native mirror of the original registration list. */
    public static final List<Binding<?>> BINDINGS = List.of(
            // The special item and the permanent item share the original RenderSpecialItem.
            of(ItemEntity.class, ThaumcraftContent.SPECIAL_ITEM, ctx -> new RenderSpecialItem(ctx)),
            of(ItemEntity.class, ThaumcraftContent.INVENTORY_ITEM, ctx -> new ItemEntityRenderer(ctx)),
            // The bottle of taint is the native thrown item renderer; the original drew the
            // bottle item on the snowball model (oracle ClientProxy.java:513).
            of(EntityBottleTaint.class, ThaumcraftProjectiles.BOTTLE_TAINT,
                    ctx -> new ThrownItemRenderer<EntityBottleTaint>(ctx)),
            // The golem orb is the original electric orb (oracle ClientProxy.java:476).
            of(EntityGolemOrb.class, ThaumcraftProjectiles.GOLEM_ORB, ctx -> new RenderGolemOrb(ctx)),
            // The primal arrow is the original arrow (oracle ClientProxy.java:488).
            of(EntityPrimalArrow.class, ThaumcraftProjectiles.PRIMAL_ARROW, ctx -> new RenderPrimalArrowEntity(ctx)),
            of(ItemEntity.class, ThaumcraftProjectiles.FOLLOW_ITEM, ctx -> new RenderFollowingItem(ctx)),
            of(ItemEntity.class, ThaumcraftProjectiles.PERMANENT_ITEM, ctx -> new RenderSpecialItem(ctx)),
            of(ItemEntity.class, ThaumcraftProjectiles.ITEM_GRATE, ctx -> new ItemEntityRenderer(ctx)),
            of(EntityFallingTaint.class, ThaumcraftTaint.FALLING_TAINT, ctx -> new RenderFallingTaint(ctx)),
            of(EntityTaintSpider.class, ThaumcraftTaint.TAINT_SPIDER, ctx -> new RenderTaintSpider(ctx)),
            of(EntityTaintSpore.class, ThaumcraftTaint.TAINT_SPORE, ctx -> new RenderTaintSpore(ctx)),
            of(EntityTaintSporeSwarmer.class, ThaumcraftTaint.TAINT_SWARMER, ctx -> new RenderTaintSporeSwarmer(ctx)),
            of(EntityTaintSwarm.class, ThaumcraftTaint.TAINT_SWARM, ctx -> new RenderTaintSwarm(ctx)),
            of(EntityTaintacle.class, ThaumcraftTaint.TAINTACLE, ctx -> new RenderTaintacle(ctx, .6F, 10)),
            of(EntityTaintacle.class, ThaumcraftTaint.TAINTACLE_SMALL, ctx -> new RenderTaintacle(ctx, .2F, 6)),
            of(EntityTaintChicken.class, ThaumcraftTaintAnimals.TAINT_CHICKEN, ctx -> new RenderTaintChicken(ctx)),
            of(EntityTaintCow.class, ThaumcraftTaintAnimals.TAINT_COW, ctx -> new RenderTaintCow(ctx)),
            of(EntityTaintPig.class, ThaumcraftTaintAnimals.TAINT_PIG, ctx -> new RenderTaintPig(ctx)),
            of(EntityTaintSheep.class, ThaumcraftTaintAnimals.TAINT_SHEEP, ctx -> new RenderTaintSheep(ctx)),
            of(EntityTaintVillager.class, ThaumcraftTaintAnimals.TAINT_VILLAGER, ctx -> new RenderTaintVillager(ctx)),
            // The tainted crawler of the 1.12.2 backport uses the vanilla silverfish model.
            of(EntityTaintCrawler.class, ThaumcraftTaintAnimals.TAINT_CRAWLER, ctx -> new RenderTaintCrawler(ctx)),
            // The tainted seed and the prime seed of the 1.12.2 backport.
            of(EntityTaintSeed.class, ThaumcraftTaintAnimals.TAINT_SEED, ctx -> new RenderTaintSeed(ctx, .4F)),
            of(EntityTaintSeed.class, ThaumcraftTaintAnimals.TAINT_SEED_PRIME, ctx -> new RenderTaintSeed(ctx, .6F)),
            of(EntityTaintCreeper.class, ThaumcraftTaintCreeper.TAINT_CREEPER, ctx -> new RenderTaintCreeper(ctx)),
            of(EntityCultistKnight.class, ThaumcraftCultists.CULTIST_KNIGHT, ctx -> new RenderCultist<EntityCultistKnight>(ctx)),
            of(EntityCultistCleric.class, ThaumcraftCultists.CULTIST_CLERIC, ctx -> new RenderCultist<EntityCultistCleric>(ctx)),
            of(EntityCultistLeader.class, ThaumcraftCultists.CULTIST_LEADER, ctx -> new RenderCultist<EntityCultistLeader>(ctx)),
            of(EntityCultistPortal.class, ThaumcraftCultists.CULTIST_PORTAL, ctx -> new RenderCultistPortal<EntityCultistPortal>(ctx)),
            of(EntityCultistPortal.class, ThaumcraftCultists.CULTIST_PORTAL_GREATER, ctx -> new RenderCultistPortal<EntityCultistPortal>(ctx)),
            of(EntityBrainyZombie.class, ThaumcraftMonsters.BRAINY_ZOMBIE, ctx -> new RenderBrainyZombie(ctx)),
            of(EntityBrainyZombie.class, ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE, ctx -> new RenderBrainyZombie(ctx)),
            of(EntityFireBat.class, ThaumcraftMonsters.FIREBAT, ctx -> new RenderFireBat(ctx)),
            of(EntityWisp.class, ThaumcraftMonsters.WISP, ctx -> new RenderWisp(ctx)),
            of(EntityMindSpider.class, ThaumcraftWarp.MIND_SPIDER, ctx -> new RenderMindSpider(ctx)),
            of(EntityEldritchGuardian.class, ThaumcraftWarp.ELDRITCH_GUARDIAN, ctx -> new RenderEldritchGuardian(ctx)),
            of(EntityEldritchOrb.class, ThaumcraftWarp.ELDRITCH_ORB, ctx -> new RenderEldritchOrb(ctx)),
            of(EntityGolemBase.class, ThaumcraftGolems.GOLEM, ctx -> new RenderGolemBase(ctx)),
            of(EntityTravelingTrunk.class, ThaumcraftGolems.TRUNK, ctx -> new RenderTravelingTrunk(ctx)),
            of(EntityDart.class, ThaumcraftGolems.DART, ctx -> new RenderDart(ctx)),
            of(EntityGolemBobber.class, ThaumcraftGolems.GOLEM_BOBBER, ctx -> new RenderGolemBobber(ctx)),
            of(EntityPech.class, ThaumcraftPech.PECH, ctx -> new RenderPech(ctx)),
            of(EntityPechBlast.class, ThaumcraftPech.PECH_BLAST, ctx -> new RenderPechBlast(ctx)),
            of(EntityPrimalOrb.class, ThaumcraftPrimalFoci.PRIMAL_ORB, ctx -> new RenderPrimalOrb(ctx)),
            of(EntityEmber.class, ThaumcraftEmbers.EMBER, ctx -> new RenderEmber(ctx)),
            of(EntityAlumentum.class, ThaumcraftResources.ALUMENTUM, ctx -> new RenderAlumentum(ctx)),
            of(EntityShockOrb.class, ThaumcraftShockOrbs.SHOCK_ORB, ctx -> new RenderFocusOrb<EntityShockOrb>(ctx, false)),
            of(EntityExplosiveOrb.class, ThaumcraftExplosiveOrbs.EXPLOSIVE_ORB, ctx -> new RenderFocusOrb<EntityExplosiveOrb>(ctx, true)),
            of(EntityAspectOrb.class, ThaumcraftAspectOrbs.ORB, ctx -> new RenderAspectOrb(ctx)),
            of(EntityEldritchWarden.class, ThaumcraftEldritchMobs.ELDRITCH_WARDEN, ctx -> new RenderEldritchWarden(ctx)),
            of(EntityEldritchGolem.class, ThaumcraftEldritchMobs.ELDRITCH_GOLEM, ctx -> new RenderEldritchGolem(ctx)),
            of(EntityEldritchCrab.class, ThaumcraftEldritchMobs.ELDRITCH_CRAB, ctx -> new RenderEldritchCrab(ctx)),
            of(EntityInhabitedZombie.class, ThaumcraftEldritchMobs.INHABITED_ZOMBIE, ctx -> new RenderInhabitedZombie(ctx)),
            of(EntityWatcher.class, ThaumcraftEldritchMobs.WATCHER, ctx -> new RenderWatcher(ctx)),
            of(EntityThaumicSlime.class, ThaumcraftEldritchMobs.THAUMIC_SLIME, ctx -> new RenderThaumicSlime(ctx)),
            // The giant taintacle is the original taintacle with the shadow of one and the
            // length of fourteen (oracle ClientProxy.java:494).
            of(EntityTaintacle.class, ThaumcraftEldritchMobs.TAINTACLE_GIANT, ctx -> new RenderTaintacle(ctx, 1.0F, 14)),
            of(EntityFrostShard.class, ThaumcraftFrost.FROST_SHARD, ctx -> new RenderFrostShard(ctx)));

    private ThaumcraftEntityRenderers() {}

    /** The registry id of every entity type the port registers. */
    public static Set<String> entityTypeIds() {
        Set<String> ids = new TreeSet<>();
        for (RegistrySupplier<? extends EntityType<?>> type : ENTITY_TYPES) ids.add(type.getId().getPath());
        return ids;
    }

    /** The registry id of every entity type that has a client renderer. */
    public static Set<String> renderedEntityTypeIds() {
        Set<String> ids = new TreeSet<>();
        for (Binding<?> binding : BINDINGS) ids.add(binding.id().getPath());
        return ids;
    }

    /** The binding of one entity type, or null when the type has no renderer. */
    public static Binding<?> binding(String entityTypeId) {
        for (Binding<?> binding : BINDINGS) {
            if (binding.id().getPath().equals(entityTypeId)) return binding;
        }
        return null;
    }

    /**
     * The entity types of {@code entityTypeIds} that have no client renderer.
     *
     * <p>The rule is a pure function of its argument: it reads no registry and builds no
     * renderer, so the unit test can assert it without a running game.</p>
     */
    public static Set<String> entityTypesWithoutRenderer(Collection<String> entityTypeIds) {
        Set<String> missing = new TreeSet<>(entityTypeIds);
        missing.removeAll(renderedEntityTypeIds());
        return missing;
    }

    /**
     * Registers every entity renderer of the port.
     *
     * <p>The client looks the renderer up by entity type and has no fallback, so an entity
     * type without a renderer crashes the client with a {@code NullPointerException} as
     * soon as the entity is drawn. The guard below turns that into a readable failure at
     * the start of the client.</p>
     */
    public static void registerAll() {
        Set<String> missing = entityTypesWithoutRenderer(entityTypeIds());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("thaumcraft entity types without a client renderer: " + missing);
        }
        for (Binding<?> binding : BINDINGS) binding.register();
    }
}

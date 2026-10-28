package thaumcraft.api.entities;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * The stable keys of the creatures and projectiles of Thaumcraft an add-on may need to recognise.
 *
 * <p>1.7.10 add-ons recognised a Thaumcraft entity by its class ({@code thaumcraft.common.entities.monster.EntityWisp},
 * {@code EntityGolemBase}...), which is not public in 1.21.1. The identity of an entity is now its
 * {@link EntityType}, registered under the {@code thaumcraft} namespace; these keys are those identities, so an
 * add-on never writes the strings. {@link IEldritchMob}, {@link ITaintedMob} and {@link IThaumcraftBoss} stay the way to
 * ask for a whole family; a key names one creature.</p>
 *
 * <p>The keys exist from the first line of the class, the entity types are registered by Thaumcraft before
 * {@code IThaumcraftAddon.onThaumcraftReady}. {@link #holder} and {@link #type} throw {@link IllegalStateException} while
 * a key is not registered. Everything here is safe on any thread once the registry is frozen.</p>
 *
 * <pre>{@code
 * if (ThaumcraftEntities.is(target, ThaumcraftEntities.GOLEM)) { ... }       // an entity in hand
 * EntityType<?> wisp = ThaumcraftEntities.type(ThaumcraftEntities.WISP);       // a type for a spawner or a predicate
 * }</pre>
 */
public final class ThaumcraftEntities {
    // The natural monsters.
    /** Original {@code EntityWisp}: the wisp of an aspect. */
    public static final ResourceKey<EntityType<?>> WISP = key("wisp");
    /** Original {@code EntityFireBat}. */
    public static final ResourceKey<EntityType<?>> FIREBAT = key("firebat");
    /** Original {@code EntityBrainyZombie}. */
    public static final ResourceKey<EntityType<?>> BRAINY_ZOMBIE = key("brainy_zombie");
    /** Original {@code EntityGiantBrainyZombie}. */
    public static final ResourceKey<EntityType<?>> GIANT_BRAINY_ZOMBIE = key("giant_brainy_zombie");
    /** Original {@code EntityPech}. */
    public static final ResourceKey<EntityType<?>> PECH = key("pech");
    /** Original {@code EntityMindSpider}. */
    public static final ResourceKey<EntityType<?>> MIND_SPIDER = key("mind_spider");
    /** Original {@code EntityThaumicSlime}. */
    public static final ResourceKey<EntityType<?>> THAUMIC_SLIME = key("thaum_slime");

    // The cultists.
    /** Original {@code EntityCultistCleric}. */
    public static final ResourceKey<EntityType<?>> CULTIST_CLERIC = key("cultist_cleric");
    /** Original {@code EntityCultistKnight}. */
    public static final ResourceKey<EntityType<?>> CULTIST_KNIGHT = key("cultist_knight");
    /** Original {@code EntityCultistLeader}. */
    public static final ResourceKey<EntityType<?>> CULTIST_LEADER = key("cultist_leader");
    /** Original {@code EntityCultistPortal}. */
    public static final ResourceKey<EntityType<?>> CULTIST_PORTAL = key("cultist_portal");
    /** Original {@code EntityCultistPortalGreater}. */
    public static final ResourceKey<EntityType<?>> CULTIST_PORTAL_GREATER = key("cultist_portal_greater");

    // The eldritch creatures.
    /** Original {@code EntityEldritchGuardian}. */
    public static final ResourceKey<EntityType<?>> ELDRITCH_GUARDIAN = key("eldritch_guardian");
    /** Original {@code EntityEldritchWarden}. */
    public static final ResourceKey<EntityType<?>> ELDRITCH_WARDEN = key("eldritch_warden");
    /** Original {@code EntityEldritchGolem}. */
    public static final ResourceKey<EntityType<?>> ELDRITCH_GOLEM = key("eldritch_golem");
    /** Original {@code EntityEldritchCrab}. */
    public static final ResourceKey<EntityType<?>> ELDRITCH_CRAB = key("eldritch_crab");
    /** Original {@code EntityWatcher}. */
    public static final ResourceKey<EntityType<?>> WATCHER = key("watcher");
    /** Original {@code EntityInhabitedZombie}. */
    public static final ResourceKey<EntityType<?>> INHABITED_ZOMBIE = key("inhabited_zombie");

    // The tainted creatures.
    /** Original {@code EntityTaintCreeper}. */
    public static final ResourceKey<EntityType<?>> TAINT_CREEPER = key("taint_creeper");
    /** Original {@code EntityTaintSpider}. */
    public static final ResourceKey<EntityType<?>> TAINT_SPIDER = key("taint_spider");
    /** Original {@code EntityTaintSwarm}. */
    public static final ResourceKey<EntityType<?>> TAINT_SWARM = key("taint_swarm");
    /** Original {@code EntityTaintSporeSwarmer}. */
    public static final ResourceKey<EntityType<?>> TAINT_SWARMER = key("taint_swarmer");
    /** Original {@code EntityTaintSpore}. */
    public static final ResourceKey<EntityType<?>> TAINT_SPORE = key("taint_spore");
    /** Original {@code EntityTaintCrawler}. */
    public static final ResourceKey<EntityType<?>> TAINT_CRAWLER = key("taint_crawler");
    /** Original {@code EntityTaintSeed}. */
    public static final ResourceKey<EntityType<?>> TAINT_SEED = key("taint_seed");
    /** Original {@code EntityTaintSeedPrime}. */
    public static final ResourceKey<EntityType<?>> TAINT_SEED_PRIME = key("taint_seed_prime");
    /** Original {@code EntityTaintacle}. */
    public static final ResourceKey<EntityType<?>> TAINTACLE = key("taintacle");
    /** Original {@code EntityTaintacleSmall}. */
    public static final ResourceKey<EntityType<?>> TAINTACLE_SMALL = key("taintacle_small");
    /** Original {@code EntityTaintacleGiant}. */
    public static final ResourceKey<EntityType<?>> TAINTACLE_GIANT = key("taintacle_giant");
    /** Original {@code EntityTaintChicken}. */
    public static final ResourceKey<EntityType<?>> TAINTED_CHICKEN = key("tainted_chicken");
    /** Original {@code EntityTaintCow}. */
    public static final ResourceKey<EntityType<?>> TAINTED_COW = key("tainted_cow");
    /** Original {@code EntityTaintPig}. */
    public static final ResourceKey<EntityType<?>> TAINTED_PIG = key("tainted_pig");
    /** Original {@code EntityTaintSheep}. */
    public static final ResourceKey<EntityType<?>> TAINTED_SHEEP = key("tainted_sheep");
    /** Original {@code EntityTaintVillager}. */
    public static final ResourceKey<EntityType<?>> TAINTED_VILLAGER = key("tainted_villager");

    // The golems and one projectile.
    /** Original {@code EntityGolemBase}: every golem, whatever its material and core. */
    public static final ResourceKey<EntityType<?>> GOLEM = key("golem");
    /** Original {@code EntityTravelingTrunk}. */
    public static final ResourceKey<EntityType<?>> TRAVELING_TRUNK = key("traveling_trunk");
    /** Original {@code EntityFrostShard}: the shard of the frost focus. */
    public static final ResourceKey<EntityType<?>> FROST_SHARD = key("frost_shard");

    private ThaumcraftEntities() {}

    /**
     * The registered entity type of a key, as the holder the 1.21.1 entity calls take.
     *
     * @throws IllegalStateException when the key is not registered (Thaumcraft's registration has not run yet, or the
     *                               key is not one of this class)
     */
    public static Holder<EntityType<?>> holder(ResourceKey<EntityType<?>> key) {
        return BuiltInRegistries.ENTITY_TYPE.getHolderOrThrow(key);
    }

    /**
     * The registered entity type of a key.
     *
     * @throws IllegalStateException when the key is not registered, see {@link #holder}
     */
    public static EntityType<?> type(ResourceKey<EntityType<?>> key) {
        return holder(key).value();
    }

    /**
     * Whether an entity is of the type of a key: the recogniser that replaces {@code entity instanceof EntityWisp}.
     * An entity of an add-on's own type, a vanilla entity and {@code null} are no match; this never throws for an
     * unregistered key.
     */
    public static boolean is(Entity entity, ResourceKey<EntityType<?>> key) {
        return entity != null && key != null && key.location().equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("thaumcraft", path));
    }
}

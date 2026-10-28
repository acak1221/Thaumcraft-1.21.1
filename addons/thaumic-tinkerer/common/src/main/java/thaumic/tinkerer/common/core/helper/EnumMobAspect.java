/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;

/**
 * The mobs the Tablet of Necromancy can summon: three aspects (an unordered set of three soul aspects on the pedestals)
 * make one mob, and the same table says which aspects a mob gives when it is reaped with the cursed spirit's blade.
 *
 * <p>The order of the constants matters: {@link #getAspectsForEntity} answers with the first constant that matches, so
 * a subclass comes before its superclass ({@code MushroomCow} before {@code Cow}, {@code PigZombie} before
 * {@code Zombie}...), exactly as 1.7.10 ordered them. 1.7.10 held the entity class and made the mob with its
 * constructor; here an entity type is looked up in the registry when it is needed (the Thaumcraft ones are not
 * constants of the API).</p>
 */
public enum EnumMobAspect {

    // Passive
    Chicken("minecraft:chicken", new Aspect[] { Aspect.CROP, Aspect.FLIGHT, Aspect.BEAST }, 1.0f, 0,
            e -> e instanceof Chicken),
    Pig("minecraft:pig", new Aspect[] { Aspect.BEAST, Aspect.EARTH, Aspect.TRAVEL }, 1.0f, 0, e -> e instanceof Pig),
    /// Must be before Cow because MushroomCow extends Cow
    MushroomCow("minecraft:mooshroom", new Aspect[] { Aspect.BEAST, Aspect.EARTH, Aspect.CROP }, 0.9f, 0,
            e -> e instanceof MushroomCow),
    Cow("minecraft:cow", new Aspect[] { Aspect.BEAST, Aspect.EARTH, Aspect.BEAST }, 0.9f, 0, e -> e instanceof Cow),
    Sheep("minecraft:sheep", new Aspect[] { Aspect.EARTH, Aspect.CLOTH, Aspect.BEAST }, 0.9f, 0,
            e -> e instanceof Sheep),
    /** 1.7.10 ocelots covered the wild ocelot and the tamed cats. */
    Ozelot("minecraft:ocelot", new Aspect[] { Aspect.BEAST, Aspect.EARTH, Aspect.ELDRITCH }, 1.0f, 0,
            e -> e instanceof Ocelot || e instanceof Cat),
    Wolf("minecraft:wolf", new Aspect[] { Aspect.BEAST, Aspect.BEAST, Aspect.BEAST }, 1.0f, 0, e -> e instanceof Wolf),
    /** 1.7.10 horses covered the horse, donkey, mule and the two undead horses (not the llamas of later versions). */
    EntityHorse("minecraft:horse", new Aspect[] { Aspect.BEAST, Aspect.BEAST, Aspect.TRAVEL }, 0.8f, 0,
            e -> e instanceof Horse || e instanceof Donkey || e instanceof Mule || e instanceof SkeletonHorse
                    || e instanceof ZombieHorse),
    SnowMan("minecraft:snow_golem", new Aspect[] { Aspect.WATER, Aspect.WATER, Aspect.MAN }, 0.8f, 0,
            e -> e instanceof SnowGolem),
    VillagerGolem("minecraft:iron_golem", new Aspect[] { Aspect.METAL, Aspect.METAL, Aspect.MAN }, 0.6f, 0,
            e -> e instanceof IronGolem),
    Villager("minecraft:villager", new Aspect[] { Aspect.MAN, Aspect.MAN, Aspect.MAN }, 0.8f, 0,
            e -> e instanceof Villager),
    Squid("minecraft:squid", new Aspect[] { Aspect.WATER, Aspect.WATER, Aspect.WATER }, 0.6f, 0.75f,
            e -> e instanceof Squid),
    Bat("minecraft:bat", new Aspect[] { Aspect.AIR, Aspect.AIR, Aspect.FLIGHT }, 1.9f, 0, e -> e instanceof Bat),
    // Hostiles
    Blaze("minecraft:blaze", new Aspect[] { Aspect.FIRE, Aspect.FIRE, Aspect.FIRE }, 0.8f, 0, e -> e instanceof Blaze),
    PigZombie("minecraft:zombified_piglin", new Aspect[] { Aspect.UNDEAD, Aspect.FLESH, Aspect.FIRE }, 0.8f, 0,
            e -> e instanceof ZombifiedPiglin),
    /// Must be before Slime because MagmaCube extends Slime
    LavaSlime("minecraft:magma_cube", new Aspect[] { Aspect.FIRE, Aspect.SLIME, Aspect.SLIME }, 1.5f, 0,
            e -> e instanceof MagmaCube) {

        @Override
        public Mob createEntity(Level level) {
            return setSlimeSize(super.createEntity(level), 1);
        }

        @Override
        public Mob finalizeSummoned(Mob mob) {
            return setSlimeSize(mob, 1);
        }
    },
    Ghast("minecraft:ghast", new Aspect[] { Aspect.FIRE, Aspect.FLIGHT, Aspect.FLIGHT }, 0.2f, 0.6f,
            e -> e instanceof Ghast),
    Firebat("thaumcraft:firebat", new Aspect[] { Aspect.FLIGHT, Aspect.FIRE, Aspect.MAGIC }, 1.9f, 0f,
            e -> isType(e, "thaumcraft:firebat")),
    /// Must be before Zombie because the brainy zombies of Thaumcraft extend Zombie
    BrainyZombie("thaumcraft:brainy_zombie", new Aspect[] { Aspect.MAGIC, Aspect.UNDEAD, Aspect.FLESH }, 0.8f, 0,
            e -> isType(e, "thaumcraft:brainy_zombie") || isType(e, "thaumcraft:giant_brainy_zombie")),
    Zombie("minecraft:zombie", new Aspect[] { Aspect.FLESH, Aspect.FLESH, Aspect.UNDEAD }, 0.8f, 0,
            e -> e instanceof Zombie),
    /** Plain skeletons only: 1.7.10 skeletons had type 0 (skeleton) and 1 (wither skeleton). */
    Skeleton("minecraft:skeleton", new Aspect[] { Aspect.UNDEAD, Aspect.MAN, Aspect.UNDEAD }, 0.8f, 0,
            e -> e instanceof Skeleton),
    WitherSkeleton("minecraft:wither_skeleton", new Aspect[] { Aspect.UNDEAD, Aspect.POISON, Aspect.FIRE }, 0.8f, 0,
            e -> e instanceof WitherSkeleton),
    Creeper("minecraft:creeper", new Aspect[] { Aspect.MAGIC, Aspect.BEAST, Aspect.ELDRITCH }, 0.8f, 0,
            e -> e instanceof Creeper),
    Witch("minecraft:witch", new Aspect[] { Aspect.MAGIC, Aspect.UNDEAD, Aspect.ELDRITCH }, 0.8f, 0,
            e -> e instanceof Witch),
    /// Must be before Spider because CaveSpider extends Spider
    CaveSpider("minecraft:cave_spider", new Aspect[] { Aspect.BEAST, Aspect.POISON, Aspect.POISON }, 0.8f, 0,
            e -> e instanceof CaveSpider),
    Spider("minecraft:spider", new Aspect[] { Aspect.BEAST, Aspect.UNDEAD, Aspect.UNDEAD }, 0.8f, 0,
            e -> e instanceof Spider),
    Slime("minecraft:slime", new Aspect[] { Aspect.SLIME, Aspect.SLIME, Aspect.BEAST }, 1.5f, 0,
            e -> e instanceof Slime) {

        @Override
        public Mob createEntity(Level level) {
            return setSlimeSize(super.createEntity(level), 1);
        }

        @Override
        public Mob finalizeSummoned(Mob mob) {
            return setSlimeSize(mob, 1);
        }
    },
    Silverfish("minecraft:silverfish", new Aspect[] { Aspect.METAL, Aspect.METAL, Aspect.EARTH }, 1.2f, 0,
            e -> e instanceof Silverfish),
    Enderman("minecraft:enderman", new Aspect[] { Aspect.ELDRITCH, Aspect.ELDRITCH, Aspect.MAN }, 0.7f, 0,
            e -> e instanceof EnderMan),
    Wisp("thaumcraft:wisp", new Aspect[] { Aspect.AIR, Aspect.MAGIC, Aspect.MAGIC }, 1.0f, 0,
            e -> isType(e, "thaumcraft:wisp"));

    private static final Map<String, EnumMobAspect> LOOKUP = new LinkedHashMap<>();

    static {
        for (EnumMobAspect value : values()) LOOKUP.put(value.name(), value);
    }

    public final Aspect[] aspects;
    private final ResourceLocation entityTypeId;
    private final Predicate<Entity> matcher;
    private final float scale;
    private final float offset;

    EnumMobAspect(String entityTypeId, Aspect[] aspects, float scale, float offset, Predicate<Entity> matcher) {
        this.aspects = aspects;
        this.entityTypeId = ResourceLocation.parse(entityTypeId);
        this.scale = scale;
        this.offset = offset;
        this.matcher = matcher;
    }

    public static EnumMobAspect get(String name) {
        return LOOKUP.get(name);
    }

    private static boolean isType(Entity entity, String id) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(id);
    }

    private static Mob setSlimeSize(Mob entity, int size) {
        if (entity instanceof Slime slime) {
            slime.setSize(size, true);
        }

        return entity;
    }

    /** The aspects the mob gives when reaped, or null if no row of the table matches it. */
    public static Aspect[] getAspectsForEntity(Entity e) {
        for (EnumMobAspect aspect : EnumMobAspect.values()) {
            if (aspect.matches(e)) {
                return aspect.aspects;
            }
        }
        return null;
    }

    /** True when the aspect set of the three pedestals ({@code pedestalAspects}) is the set of this mob. */
    public boolean matchesAspectSet(java.util.Collection<Aspect> pedestalAspects) {
        return Arrays.asList(aspects).containsAll(pedestalAspects) && pedestalAspects.containsAll(Arrays.asList(aspects));
    }

    public boolean matches(Entity e) {
        return matcher.test(e);
    }

    public float getVerticalOffset() {
        return offset;
    }

    public float getScale() {
        return scale;
    }

    /** The entity type of the mob, or null when the mod that provides it is not loaded. */
    public EntityType<?> getEntityType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
    }

    /** The name of the mob as the game shows it. */
    public Component getDisplayName() {
        EntityType<?> type = getEntityType();
        return type == null ? Component.translatable("entity.generic") : type.getDescription();
    }

    /** Makes the mob (not yet in the world); null when its entity type does not exist or is no mob. */
    public Mob createEntity(Level level) {
        EntityType<?> type = getEntityType();
        if (type == null) return null;
        Entity entity = type.create(level);
        return entity instanceof Mob mob ? mob : null;
    }

    /**
     * What is done to a summoned mob after {@code finalizeSpawn} (the spawn-egg initialisation): slimes are size one.
     * 1.7.10 made the slimes size one in {@code createEntity} and its spawn-egg initialisation left the size alone;
     * 1.21.1 draws a random size in {@code finalizeSpawn}, so the size is set again afterwards.
     */
    public Mob finalizeSummoned(Mob mob) {
        return mob;
    }

    /** Whether the entity is a living thing this table knows (used by the cursed spirit's blade). */
    public static boolean isKnownMob(Entity e) {
        return e instanceof LivingEntity && getAspectsForEntity(e) != null;
    }
}

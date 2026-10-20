package thaumcraft.common.lib.mods;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.api.entities.IThaumcraftBoss;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.OuterLandsProvider;

/**
 * The original champion lifecycle of the 1.7 bestiary, wired into the native mob
 * lifecycle. The 1.7 original kept the champion modifier of every EntityMob in the
 * custom tc.mobmod attribute, so the modifier travelled with the entity and the
 * original rolled a mob only while that attribute still held its base value. The
 * native port keeps the modifier id in one int field of the mob
 * ({@link ChampionAccess}), mirrors it into the entity data for the client and rolls
 * a mob exactly once, at the moment it enters the world.
 */
public final class ChampionHooks {
    /**
     * The value of a mob that was never rolled. The original attribute held -2.0 as
     * its base value and the original roll only answered while the value was below
     * -1.0, so a rolled mob that lost the roll answered -1.0 and was never rolled
     * again. The native field keeps that third state, because a two state field
     * could not tell a mob that was rolled from a mob that was not.
     */
    public static final int NOT_ROLLED = -2;
    /** The nbt key of the champion modifier id inside the entity data. */
    public static final String SAVE_KEY = "tc.mobmod";
    /**
     * The native key of the Outer Lands. The 1.7 original tested the dimension id of
     * Config.dimensionOuterId; the native port owns that dimension now, the eldritch
     * lands biome stays the marker of the same place for the surfaces that are not
     * inside the dimension.
     */
    public static final ResourceKey<Biome> ELDRITCH_LANDS = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch_lands"));
    /** The original SPOOKY biome dictionary type. */
    public static final TagKey<Biome> SPOOKY = biomeType("spooky");
    /** The original NETHER biome dictionary type. */
    public static final TagKey<Biome> NETHER = biomeType("nether");
    /** The original END biome dictionary type. */
    public static final TagKey<Biome> END = biomeType("end");

    /**
     * The original dangerous location test. The 1.7 original called a place
     * dangerous when it stood in the Outer Lands and in a maze cell of feature six
     * or eight. The native port reads the same cells from the saved data of the
     * labyrinth, see {@link ChampionWiring}.
     */
    public interface DangerousLocation {
        /** Whether the block position is a dangerous location of the original maze. */
        boolean test(ServerLevel level, int x, int y, int z);
    }

    private static volatile boolean championsEnabled = true;
    private static volatile DangerousLocation dangerousLocation = (level, x, y, z) -> false;
    private static boolean initialized;

    private ChampionHooks() {}

    /**
     * The original champion mobs configuration. The 1.7 original config flag
     * champion_mobs was true by default and stayed false while champions were
     * disabled, the roll only lost its chance of a champion.
     */
    public static void setChampionsEnabled(boolean value) { championsEnabled = value; }

    /** Whether the original champion mobs are enabled. */
    public static boolean championsEnabled() { return championsEnabled; }

    /** Wires the original maze cells as the dangerous locations, null clears them. */
    public static void setDangerousLocation(DangerousLocation predicate) {
        dangerousLocation = predicate == null ? (level, x, y, z) -> false : predicate;
    }

    /**
     * Registers the original join event. The 1.7 original rolled every EntityMob
     * that entered the world, the native port uses the common entity add event of
     * Architectury, which stands in the same place on every loader.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        EntityEvent.ADD.register((entity, world) -> {
            if (world instanceof ServerLevel level) onEntityAdded(entity, level);
            return EventResult.pass();
        });
    }

    /**
     * The original spawn roll of a mob that enters the world. The 1.7 original rolled
     * every EntityMob whose champion attribute still held its base value, and left
     * every other mob with the ordinary attribute value.
     */
    public static void onEntityAdded(Entity entity, ServerLevel level) {
        if (!(entity instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;
        if (championId(mob) != NOT_ROLLED) return;
        SpawnFacts facts = facts(level, mob);
        int tier = ChampionRules.tierFor(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        float health = baseMaxHealth(mob);
        int modifier = roll(level.getRandom(), facts, tier, health);
        setChampionModifier(mob, modifier);
        if (modifier != ChampionRules.NO_CHAMPION) ChampionRules.Adapter.promote(mob, modifier);
    }

    /**
     * The original champion decision of a spawn roll, without a world. The roll of
     * the 1.7 original, the shift of the world and the whitelist level decide
     * whether the mob becomes a champion, and a champion always received exactly
     * one modifier.
     */
    public static int roll(RandomSource random, SpawnFacts facts, int tier, float baseMaxHealth) {
        int rolled = ChampionRules.Adapter.spawnRoll(random, facts.championsEnabled(), facts.easy(), facts.hard(),
                facts.outerDimension(), facts.hostileBiome(), facts.dangerousLocation());
        if (!ChampionRules.becomesChampion(rolled, tier, baseMaxHealth, facts.championsEnabled())) return ChampionRules.NO_CHAMPION;
        return ChampionRules.rollModifier(new ChampionRules.WorldRandom(random), facts.creeper()).id;
    }

    /**
     * The world facts of the original spawn roll. The 1.7 original shifted its roll
     * by the difficulty, by the Outer Lands, by a spooky, nether or end biome and
     * by a dangerous location, and it always rolled the bold modifier of a creeper.
     */
    public record SpawnFacts(boolean championsEnabled, boolean easy, boolean hard, boolean outerDimension,
                             boolean hostileBiome, boolean dangerousLocation, boolean creeper) {}

    /** The world facts of a mob in a native world. */
    public static SpawnFacts facts(ServerLevel level, Mob mob) {
        Holder<Biome> biome = surfaceBiome(level, mob);
        return new SpawnFacts(championsEnabled, level.getDifficulty() == Difficulty.EASY,
                level.getDifficulty() == Difficulty.HARD,
                outerLands(level, biome),
                biome != null && (biome.is(SPOOKY) || biome.is(NETHER) || biome.is(END)),
                dangerous(level, Mth.floor(mob.getX()), Mth.floor(mob.getY()), Mth.floor(mob.getZ())),
                mob instanceof Creeper);
    }

    /**
     * The original Outer Lands test of the spawn roll. The 1.7 original compared the
     * dimension id of the world with Config.dimensionOuterId; the native port owns that
     * dimension and keeps the eldritch lands biome as the marker of the same place.
     */
    private static boolean outerLands(ServerLevel level, Holder<Biome> biome) {
        return OuterLandsProvider.isOuterLands(level) || (biome != null && biome.is(ELDRITCH_LANDS));
    }

    /** The original champion modifier id of a mob, {@code NOT_ROLLED} while it was never rolled. */
    public static int championId(Mob mob) {
        if (!(mob instanceof ChampionAccess access)) return NOT_ROLLED;
        return access.thaumcraft$getChampionModifier();
    }

    /** The champion modifier id the client received with the entity data. */
    public static int clientChampionId(Entity entity) {
        // Only the living entities define the champion data id.
        if (!(entity instanceof LivingEntity)) return ChampionRules.NO_CHAMPION;
        return entity.getEntityData().get(ChampionData.CHAMPION);
    }

    /**
     * Writes the champion modifier id of a mob and mirrors it into the entity data,
     * so the client can draw the original champion particles.
     */
    public static void setChampionModifier(Mob mob, int modifier) {
        if (!(mob instanceof ChampionAccess access)) return;
        access.thaumcraft$setChampionModifier(modifier);
        if (!mob.level().isClientSide) {
            mob.getEntityData().set(ChampionData.CHAMPION,
                    modifier < 0 ? ChampionRules.NO_CHAMPION : modifier);
        }
    }

    /**
     * The original living update of a champion. The 1.7 original answered the living
     * update of every champion mob whose modifier triggered on the tick, and the
     * client drew the champion particles on the same event.
     */
    public static void tickChampion(Mob mob) {
        if (mob.level().isClientSide) {
            if (!mob.isAlive()) return;
            int shown = clientChampionId(mob);
            if (shown >= 0) ChampionRules.ClientFx.showChampionFx(mob, shown, mob.level().getRandom());
            return;
        }
        int modifier = championId(mob);
        if (modifier < 0) return;
        ChampionRules.Adapter.tickChampion(mob, modifier);
    }

    /**
     * The original hurt trigger of a champion. The 1.7 original answered the living
     * hurt event of a champion that a living attacker hurt, and returned the damage
     * the champion takes.
     */
    public static float damageChampion(LivingEntity victim, DamageSource source, float amount) {
        if (victim.level().isClientSide) return amount;
        if (!(victim instanceof Mob mob)) return amount;
        int modifier = championId(mob);
        if (modifier < 0) return amount;
        return ChampionRules.Adapter.damageChampion(mob, modifier, source, amount);
    }

    /**
     * The original attack trigger of a champion. The 1.7 original answered the living
     * hurt event of the victim when a champion mob was the attacker, and the effect
     * saw the damage the victim takes.
     */
    public static void attackChampion(Entity attacker, LivingEntity victim, float amount) {
        if (victim.level().isClientSide) return;
        if (!(attacker instanceof Mob mob)) return;
        int modifier = championId(mob);
        if (modifier < 0) return;
        ChampionRules.Adapter.attackChampion(mob, modifier, victim, amount);
    }

    /** The base max health the original champion health gate used. */
    public static float baseMaxHealth(Mob mob) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        return health == null ? mob.getMaxHealth() : (float) health.getBaseValue();
    }

    /**
     * The original dangerous location test of a block position. The native port reads the
     * labyrinth cells of the Outer Lands, see {@link ChampionWiring}.
     */
    public static boolean dangerous(ServerLevel level, int x, int y, int z) {
        return dangerousLocation.test(level, x, y, z);
    }

    /**
     * The original champion death reward.
     *
     * <p>Oracle {@code EventHandlerEntity.livingDrops:453-461} paid every champion mob
     * that died five to seven extra experience and one loot bag to a mob that was hit by a
     * player, and it paid nothing to a boss. The native port stands in the same place, in
     * the death loot of the mob.
     */
    public static void onDeathLoot(LivingEntity mob, ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (!(mob instanceof Mob mobEntity)) return;
        int modifier = championId(mobEntity);
        if (!ChampionWiringRules.championReward(recentlyHit, true, mobEntity instanceof IThaumcraftBoss, modifier)) return;
        int experience = ChampionRules.championExperience(new ChampionRules.WorldRandom(level.getRandom()));
        ExperienceOrb.award(level, mob.position(), experience);
        int looting = OriginalMobLoot.looting(level, source);
        int tier = ChampionRules.championLootBagTier(new ChampionRules.WorldRandom(level.getRandom()), looting);
        OriginalMobLoot.spawn(mob, new ItemStack(lootBag(tier)), mob.getBbHeight() / 2.0F);
    }

    /** The registered loot bag of an original loot bag metadata. */
    private static Item lootBag(int tier) {
        return switch (tier) {
            case 0 -> ThaumcraftLoot.LOOT_BAG_COMMON.get();
            case 1 -> ThaumcraftLoot.LOOT_BAG_UNCOMMON.get();
            default -> ThaumcraftLoot.LOOT_BAG_RARE.get();
        };
    }

    /** The biome of the surface column of the mob, null when the column is not ready. */
    private static Holder<Biome> surfaceBiome(ServerLevel level, Entity entity) {
        if (!level.getServer().isSameThread()) return null;
        if (level.getServer().getLevel(level.dimension()) != level) return null;
        return BiomeColumns.surfaceHolder(level, Mth.floor(entity.getX()), Mth.floor(entity.getZ()));
    }

    private static TagKey<Biome> biomeType(String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "node_biome_types/" + name));
    }
}

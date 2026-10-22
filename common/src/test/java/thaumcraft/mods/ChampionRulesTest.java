package thaumcraft.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.ZombieVillager;
import thaumcraft.common.entities.monster.EntityCultistKnight;
import thaumcraft.common.entities.monster.EntityEldritchGolem;
import thaumcraft.common.entities.monster.EntityTaintacleGiant;
import thaumcraft.common.entities.monster.EntityTaintSpider;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.mods.ChampionRules;
import thaumcraft.common.lib.mods.ChampionWiringRules;

/** Original 1.7 champion selection: the spawn roll, the whitelist levels and the modifier roll. */
class ChampionRulesTest {
    /**
     * The native registries must exist before an attribute instance can be created.
     *
     * <p>The attribute registry is a dynamic one, so without this bootstrap the
     * {@code generic.max_health} holder is unbound and the test fails with
     * "Trying to access unbound value" — and it only passed by accident when
     * another test class of the suite had bootstrapped the registries first.
     * The suite must not depend on the order of its classes.</p>
     */
    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    /** Random that answers with fixed values so the original chances can be checked exactly. */
    private static final class ScriptedRandom extends Random {
        private final float floatValue;
        private final int intValue;
        ScriptedRandom(float floatValue, int intValue) { this.floatValue = floatValue; this.intValue = intValue; }
        @Override public float nextFloat() { return floatValue; }
        @Override public int nextInt(int bound) { return intValue; }
    }

    @Test void originalSpawnRollKeepsEveryWorldShift() {
        ScriptedRandom fixed = new ScriptedRandom(0.5f, 50);
        assertEquals(50, ChampionRules.spawnRoll(fixed, true, false, false, false, false, false));
        assertEquals(52, ChampionRules.spawnRoll(fixed, true, true, false, false, false, false));
        assertEquals(48, ChampionRules.spawnRoll(fixed, true, false, true, false, false, false));
        assertEquals(47, ChampionRules.spawnRoll(fixed, true, false, false, true, false, false));
        assertEquals(48, ChampionRules.spawnRoll(fixed, true, false, false, false, true, false));
        assertEquals(40, ChampionRules.spawnRoll(fixed, true, false, false, false, false, true));
        assertEquals(52, ChampionRules.spawnRoll(fixed, false, false, false, false, false, false));
        assertEquals(49, ChampionRules.spawnRoll(fixed, false, false, false, false, false, true));
    }

    @Test void originalWhitelistShiftIsTheLargestLevelMinusOneAndNeverBelowZero() {
        assertEquals(0, ChampionRules.tierBonus(ChampionRules.TIER_COMMON, true));
        assertEquals(0, ChampionRules.tierBonus(ChampionRules.TIER_UNCOMMON, true));
        assertEquals(1, ChampionRules.tierBonus(ChampionRules.TIER_RARE, true));
        assertEquals(2, ChampionRules.tierBonus(ChampionRules.TIER_ELITE, true));
        assertEquals(199, ChampionRules.tierBonus(ChampionRules.TIER_BOSS, true));
        assertEquals(0, ChampionRules.tierBonus(ChampionRules.TIER_ELITE, false));
        assertEquals(199, ChampionRules.tierBonus(ChampionRules.TIER_BOSS, false));
    }

    @Test void originalChampionDecisionNeedsTheRollTheLevelAndTheHealth() {
        assertTrue(ChampionRules.becomesChampion(0, ChampionRules.TIER_COMMON, 20.0f, true));
        assertFalse(ChampionRules.becomesChampion(1, ChampionRules.TIER_COMMON, 20.0f, true));
        assertTrue(ChampionRules.becomesChampion(1, ChampionRules.TIER_ELITE, 20.0f, true));
        assertFalse(ChampionRules.becomesChampion(3, ChampionRules.TIER_ELITE, 20.0f, true));
        assertTrue(ChampionRules.becomesChampion(99, ChampionRules.TIER_BOSS, 20.0f, true));
        assertFalse(ChampionRules.becomesChampion(0, ChampionRules.TIER_NONE, 20.0f, true));
        assertFalse(ChampionRules.becomesChampion(0, ChampionRules.TIER_COMMON, 9.9f, true));
        assertTrue(ChampionRules.becomesChampion(0, ChampionRules.TIER_COMMON, 10.0f, true));
    }

    @Test void originalHealthGateHasOneThresholdAndNoHealthTiers() {
        assertEquals(ChampionRules.TIER_NONE, ChampionRules.tierForHealth(9.9f, ChampionRules.MIN_CHAMPION_HEALTH));
        assertEquals(ChampionRules.TIER_COMMON, ChampionRules.tierForHealth(10.0f, ChampionRules.MIN_CHAMPION_HEALTH));
        assertEquals(ChampionRules.TIER_COMMON, ChampionRules.tierForHealth(20.0f, ChampionRules.MIN_CHAMPION_HEALTH));
    }

    @Test void originalWhitelistKeepsItsLevels() {
        assertEquals(0, ChampionRules.tierFor("minecraft:zombie"));
        assertEquals(0, ChampionRules.tierFor("minecraft:spider"));
        assertEquals(0, ChampionRules.tierFor("minecraft:blaze"));
        assertEquals(0, ChampionRules.tierFor("minecraft:enderman"));
        assertEquals(0, ChampionRules.tierFor("minecraft:skeleton"));
        assertEquals(1, ChampionRules.tierFor("minecraft:witch"));
        assertEquals(0, ChampionRules.tierFor("thaumcraft:eldritch_crab"));
        assertEquals(2, ChampionRules.tierFor("thaumcraft:taintacle"));
        assertEquals(1, ChampionRules.tierFor("thaumcraft:wisp"));
        assertEquals(3, ChampionRules.tierFor("thaumcraft:inhabited_zombie"));
        assertEquals(1, ChampionRules.tierFor("thaumcraft:cultist"));
        assertEquals(2, ChampionRules.tierFor("thaumcraft:watcher"));
        assertEquals(1, ChampionRules.tierFor("thaumcraft:pech"));
        assertEquals(0, ChampionRules.tierFor("thaumcraft:brainy_zombie"));
        assertEquals(0, ChampionRules.tierFor("thaumcraft:taint_spider"));
        assertEquals(ChampionRules.TIER_NONE, ChampionRules.tierFor("minecraft:creeper"));
    }

    @Test void originalWhitelistCoversTheCaveSpiderOfTheOriginalSpiderClass() {
        // Oracle ConfigEntities:196 - the original whitelisted the class of the spider, so the
        // cave spider of the original was a champion candidate of the level of the spider.
        assertEquals(ChampionRules.TIER_COMMON, ChampionRules.tierFor("minecraft:cave_spider"));
        assertEquals(ChampionRules.TIER_COMMON, ChampionRules.tierFor(CaveSpider.class));
        assertEquals(ChampionRules.TIER_COMMON, ChampionRules.tierFor(Spider.class));
    }

    @Test void originalWhitelistMatchesTheClassOfTheEntity() {
        // Oracle EventHandlerEntity.entitySpawns:409-414 - the original matched the class of the
        // entity, so every subclass of a whitelisted class inherited the level of its parent.
        assertEquals(ChampionRules.TIER_COMMON, ChampionRules.tierFor(EntityTaintSpider.class));
        assertEquals(ChampionRules.TIER_RARE, ChampionRules.tierFor(EntityTaintacleGiant.class));
        assertEquals(ChampionRules.TIER_UNCOMMON, ChampionRules.tierFor(EntityCultistKnight.class));
        // The id and the class of a mob answer the same level.
        assertEquals(ChampionRules.tierFor("thaumcraft:taint_spider"),
                ChampionRules.tierFor("thaumcraft:taint_spider", EntityTaintSpider.class));
        assertEquals(ChampionRules.TIER_BOSS,
                ChampionRules.tierFor("thaumcraft:eldritch_golem", EntityEldritchGolem.class));
        // The zombie villager of 1.21.1 shares the class of the zombie, the zombie villager of the
        // original 1.7 was a class of its own that the original never whitelisted.
        assertEquals(ChampionRules.TIER_NONE, ChampionRules.tierFor(ZombieVillager.class));
        assertEquals(ChampionRules.TIER_NONE, ChampionRules.tierFor(Creeper.class));
    }

    @Test void originalWardedChampionTakesHalfOfItsTotalMaxHealthAsItsWard() {
        // Oracle EntityUtils.makeChampion:271-274 - the ward of the warded champion is half of
        // the total of the max health attribute, the base of the mob plus the thirty health of
        // the champion, so a champion of a spider keeps twenty three of ward.
        AttributeInstance health = new AttributeInstance(Attributes.MAX_HEALTH, instance -> {});
        health.setBaseValue(16.0D);
        health.addPermanentModifier(new AttributeModifier(ChampionRules.Adapter.CHAMPION_HEALTH_ID, 30.0D,
                AttributeModifier.Operation.ADD_VALUE));
        assertEquals(46.0D, health.getValue(), 1.0E-9D);
        assertEquals(23, ChampionRules.wardedChampionWard(health));
        // The base health of the mob alone would only be eight.
        assertEquals(8, ChampionWiringRules.wardedChampionWard(health.getBaseValue()));
    }

    @Test void originalModifierRollIsUniformOverTheThirteenModifiers() {
        assertEquals(13, ChampionRules.modifierCount());
        assertEquals(12, ChampionRules.rollModifierIndex(new ScriptedRandom(0.5f, 12)));
        assertSame(ChampionModifier.mods[ChampionModifier.INFESTED], ChampionRules.rollModifier(new ScriptedRandom(0.5f, 12)));
        Set<Integer> seen = new HashSet<>();
        Random random = new Random(20121001L);
        for (int i = 0; i < 13000; i++) seen.add(ChampionRules.rollModifierIndex(random));
        assertEquals(13, seen.size());
    }

    @Test void originalGaveEveryChampionExactlyOneModifierAtEveryLevel() {
        Random random = new Random(7L);
        for (int tier : new int[] {ChampionRules.TIER_COMMON, ChampionRules.TIER_UNCOMMON, ChampionRules.TIER_RARE,
                ChampionRules.TIER_ELITE, ChampionRules.TIER_BOSS}) {
            List<ChampionModifier> rolled = ChampionRules.rollModifiers(random, tier);
            assertEquals(1, rolled.size());
            assertTrue(rolled.get(0).id >= 0 && rolled.get(0).id < ChampionRules.modifierCount());
        }
    }

    @Test void originalCreeperChampionWasAlwaysBold() {
        assertSame(ChampionModifier.mods[ChampionModifier.BOLD], ChampionRules.rollModifier(new ScriptedRandom(0.5f, 9), true));
        Random random = new Random(3L);
        for (int i = 0; i < 100; i++) {
            ChampionModifier rolled = ChampionRules.rollModifier(random, false);
            assertTrue(rolled.id >= 0 && rolled.id < ChampionRules.modifierCount());
        }
    }

    @Test void originalModifierLookupAnswersTheOriginalIds() {
        assertSame(ChampionModifier.mods[ChampionModifier.BOLD], ChampionRules.modifier(0));
        assertSame(ChampionModifier.mods[ChampionModifier.INFESTED], ChampionRules.modifier(12));
        assertNull(ChampionRules.modifier(-1));
        assertNull(ChampionRules.modifier(13));
    }

    @Test void originalChampionRewardKeepsItsExperienceAndItsLootBagTier() {
        assertEquals(5, ChampionRules.championExperience(new ScriptedRandom(0.5f, 0)));
        assertEquals(7, ChampionRules.championExperience(new ScriptedRandom(0.5f, 2)));
        assertEquals(0, ChampionRules.championLootBagTier(new ScriptedRandom(0.5f, 0), 0));
        assertEquals(0, ChampionRules.championLootBagTier(new ScriptedRandom(0.5f, 4), 0));
        assertEquals(1, ChampionRules.championLootBagTier(new ScriptedRandom(0.5f, 5), 0));
        assertEquals(1, ChampionRules.championLootBagTier(new ScriptedRandom(0.5f, 8), 0));
        assertEquals(2, ChampionRules.championLootBagTier(new ScriptedRandom(0.5f, 8), 3));
        assertEquals(2, ChampionRules.championLootBagTier(new ScriptedRandom(0.5f, 8), 9));
    }
}

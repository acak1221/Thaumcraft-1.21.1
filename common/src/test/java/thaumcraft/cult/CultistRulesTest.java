package thaumcraft.cult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.cult.CultistRules;

/** The original TC4 cultist numbers and branches, asserted without a world or a registry. */
class CultistRulesTest {

    // ---------------------------------------------------------------- cultist drops
    @Test void oneRollPicksTheOriginalCultistDrop() {
        assertEquals(CultistRules.RESOURCE_KNOWLEDGE_FRAGMENT, CultistRules.resourceDrop(0, 0));
        assertEquals(CultistRules.RESOURCE_VOID_SEED, CultistRules.resourceDrop(1, 0));
        assertEquals(CultistRules.RESOURCE_GOLD_COIN, CultistRules.resourceDrop(2, 0));
        assertEquals(CultistRules.RESOURCE_GOLD_COIN, CultistRules.resourceDrop(3, 0));
        assertEquals(CultistRules.RESOURCE_NONE, CultistRules.resourceDrop(4, 0));
    }

    @Test void lootingWidensTheCoinWindowOnly() {
        assertEquals(CultistRules.RESOURCE_NONE, CultistRules.resourceDrop(4, 0));
        assertEquals(CultistRules.RESOURCE_NONE, CultistRules.resourceDrop(5, 1));
        assertEquals(CultistRules.RESOURCE_GOLD_COIN, CultistRules.resourceDrop(4, 1));
        assertEquals(CultistRules.RESOURCE_GOLD_COIN, CultistRules.resourceDrop(6, 3));
        assertEquals(CultistRules.RESOURCE_VOID_SEED, CultistRules.resourceDrop(1, 9));
        assertEquals(CultistRules.RESOURCE_KNOWLEDGE_FRAGMENT, CultistRules.resourceDrop(0, 9));
    }

    // ---------------------------------------------------------------- cultist gear
    @Test void theClericBootsAreRareOnNormalAndCommonOnHard() {
        assertEquals(0.1F, CultistRules.clericBootsChance(false));
        assertEquals(0.3F, CultistRules.clericBootsChance(true));
    }

    @Test void theKnightRareGearIsRareOnNormalAndCommonOnHard() {
        assertEquals(0.01F, CultistRules.knightRareChance(false));
        assertEquals(0.05F, CultistRules.knightRareChance(true));
        assertEquals(CultistRules.KNIGHT_WEAPON_VOID, CultistRules.knightRareWeapon(0));
        assertEquals(CultistRules.KNIGHT_WEAPON_THAUMIUM, CultistRules.knightRareWeapon(1));
        assertEquals(CultistRules.KNIGHT_WEAPON_THAUMIUM, CultistRules.knightRareWeapon(4));
        assertTrue(CultistRules.knightUsesRobeHelm(0));
        assertFalse(CultistRules.knightUsesRobeHelm(1));
        assertFalse(CultistRules.knightKeepsHelm(true));
        assertTrue(CultistRules.knightKeepsHelm(false));
    }

    @Test void theEnchantmentChanceAndPowerKeepTheOriginalFormulas() {
        assertEquals(0.25F, CultistRules.knightEnchantChance(1.0F));
        assertEquals(0.5F, CultistRules.leaderEnchantChance(1.0F));
        assertEquals(5, CultistRules.knightEnchantPower(1.0F, 0));
        assertEquals(22, CultistRules.knightEnchantPower(1.0F, 17));
        assertEquals(7, CultistRules.leaderEnchantPower(1.0F, 0));
        assertEquals(28, CultistRules.leaderEnchantPower(1.0F, 21));
    }

    @Test void theLeaderTakesTheVoidSwordOnlyOnEasy() {
        assertTrue(CultistRules.leaderUsesVoidSword(true));
        assertFalse(CultistRules.leaderUsesVoidSword(false));
    }

    // ---------------------------------------------------------------- cleric combat
    @Test void theClericFiresTheOrbOnlyAboveTheOriginalRoll() {
        assertFalse(CultistRules.clericFiresOrb(0.66F));
        assertTrue(CultistRules.clericFiresOrb(0.67F));
        assertEquals(3, CultistRules.clericFireballCount());
        assertEquals(0.5F, CultistRules.fireballSpread(1.0F));
        assertEquals(1.0F, CultistRules.fireballSpread(4.0F));
    }

    @Test void theRitualistStopsOutsideTheOriginalBlockDistanceOrWithoutTheAltar() {
        assertFalse(CultistRules.ritualistShouldStop(16.0, true));
        assertTrue(CultistRules.ritualistShouldStop(16.01, true));
        assertTrue(CultistRules.ritualistShouldStop(0.0, false));
        assertFalse(CultistRules.ritualistShouldStop(0.0, true));
    }

    // ---------------------------------------------------------------- cultist leader
    @Test void theLeaderKeepsTheOriginalFifteenTitles() {
        assertEquals(15, CultistRules.LEADER_TITLES.length);
        assertEquals("Alberic", CultistRules.leaderTitle(0));
        assertEquals("Zelipe", CultistRules.leaderTitle(14));
        assertEquals("Alberic", CultistRules.leaderTitle(-3));
        assertEquals("Zelipe", CultistRules.leaderTitle(99));
    }

    @Test void theLeaderAuraIsRegenerationTwoForThreeSecondsWithinEightBlocks() {
        assertEquals(8.0, CultistRules.LEADER_AURA_RANGE);
        assertEquals(60, CultistRules.LEADER_AURA_DURATION);
        assertEquals(1, CultistRules.LEADER_AURA_AMPLIFIER);
    }

    // ---------------------------------------------------------------- boss base
    @Test void aHeavyHitEnragesTheBossAndCapsTheDamage() {
        assertEquals(35.0F, CultistRules.BOSS_ENRAGE_THRESHOLD);
        assertEquals(35.0F, CultistRules.bossDamageCap(80.0F));
        assertEquals(12.0F, CultistRules.bossDamageCap(12.0F));
        assertEquals(2, CultistRules.bossEnrageRegenerationAmplifier(35.0F));
        assertEquals(0, CultistRules.bossEnrageBuffAmplifier(35.0F));
        assertEquals(1, CultistRules.bossEnrageBuffAmplifier(40.0F));
        assertEquals(200, CultistRules.BOSS_ENRAGE_ANGER);
    }

    @Test void theAggroTableAccumulatesTheHitsOfTheSameAttacker() {
        assertEquals(7, CultistRules.bossAggroDamage(7, 0));
        assertEquals(12, CultistRules.bossAggroDamage(7, 5));
    }

    @Test void theRotationOnlyAcceptsACandidateThatBeatsTheCurrentTarget() {
        Map<Integer, Integer> aggro = new LinkedHashMap<>();
        aggro.put(1, 100);
        aggro.put(2, 200);
        // The original resolves the entity of the previous candidate id, so the switch keeps
        // the current target while the candidate id becomes the new current one.
        CultistRules.AggroRotation rotation = CultistRules.rotateAggro(aggro, 1, id -> 2);
        assertEquals(1, rotation.targetId());
        assertEquals(2, rotation.finalId());
        assertEquals(1, rotation.players());
        assertEquals(List.of(), rotation.removed());
    }

    @Test void aCandidateWithoutAResolvableEntityIsDroppedFromTheTable() {
        Map<Integer, Integer> aggro = new LinkedHashMap<>();
        aggro.put(1, 100);
        aggro.put(2, 200);
        CultistRules.AggroRotation rotation = CultistRules.rotateAggro(aggro, 1, id -> 0);
        assertEquals(-1, rotation.targetId());
        assertEquals(1, rotation.finalId());
        assertEquals(0, rotation.players());
        assertEquals(List.of(2), rotation.removed());
    }

    @Test void theRotationKeepsTheOriginalMarginAndRatio() {
        Map<Integer, Integer> aggro = new LinkedHashMap<>();
        aggro.put(1, 100);
        aggro.put(2, 125);
        aggro.put(3, 111);
        CultistRules.AggroRotation rotation = CultistRules.rotateAggro(aggro, 1, id -> 2);
        assertEquals(-1, rotation.targetId());
        assertEquals(1, rotation.finalId());
        assertEquals(0, rotation.players());
    }

    @Test void aNonPlayerCandidateDoesNotCountAsAPlayer() {
        Map<Integer, Integer> aggro = new LinkedHashMap<>();
        aggro.put(1, 100);
        aggro.put(2, 400);
        CultistRules.AggroRotation rotation = CultistRules.rotateAggro(aggro, 1, id -> 1);
        assertEquals(0, rotation.players());
        assertEquals(2, rotation.finalId());
    }

    // ---------------------------------------------------------------- champion modifiers
    @Test void theChampionListKeepsTheOriginalThirteenModifiers() {
        assertEquals(13, CultistRules.CHAMPION_COUNT);
        assertEquals(CultistRules.CHAMPION_COUNT, CultistRules.CHAMPION_MOD_NAMES.length);
        assertEquals(CultistRules.CHAMPION_COUNT, CultistRules.CHAMPION_MOD_DISPLAY_NAMES.length);
        assertEquals("bold", CultistRules.CHAMPION_MOD_NAMES[CultistRules.CHAMPION_BOLD]);
        assertEquals("mighty", CultistRules.CHAMPION_MOD_NAMES[CultistRules.CHAMPION_MIGHTY]);
        assertEquals("warded", CultistRules.CHAMPION_MOD_NAMES[CultistRules.CHAMPION_WARDED]);
        assertEquals("warp", CultistRules.CHAMPION_MOD_NAMES[CultistRules.CHAMPION_WARP]);
        assertEquals(62.5F, CultistRules.championAbsorption(125.0F));
    }

    @Test void theWarpedChampionRewardsOneToThreeTemporaryWarp() {
        assertEquals(0.33F, CultistRules.WARP_REWARD_CHANCE);
        assertEquals(1, CultistRules.warpReward(0));
        assertEquals(3, CultistRules.warpReward(2));
    }

    // ---------------------------------------------------------------- cultist portal
    @Test void thePortalCountsTwentyTicksPerLivingCultist() {
        assertEquals(0, CultistRules.portalTiming(0));
        assertEquals(60, CultistRules.portalTiming(3));
    }

    @Test void theFirstFiveStagesArmShortCounters() {
        for (int stage = 0; stage <= 4; stage++) {
            int counter = CultistRules.portalStageCounter(stage, 0, bound -> bound);
            // 15 + nextInt(10 - stage) - stage with the largest possible roll.
            assertEquals(15 + (10 - stage) - stage, counter);
        }
    }

    @Test void theTwelfthStageArmsThePraetorAndTheOtherStagesSendMinions() {
        int counter = CultistRules.portalStageCounter(12, 2, bound -> bound);
        assertEquals(50 + CultistRules.portalTiming(2) * 2 + 50, counter);
        assertTrue(CultistRules.portalSpawnsBoss(12));
        assertFalse(CultistRules.portalSpawnsMinions(12));
        assertTrue(CultistRules.portalSpawnsMinions(0));
        assertTrue(CultistRules.portalSpawnsMinions(5));
        assertTrue(CultistRules.portalSpawnsMinions(13));
        assertTrue(CultistRules.portalSpawnsMinions(11));
    }

    @Test void theDefaultStageArmsTheTempoOfTheCultistsAroundThePortal() {
        int counter = CultistRules.portalStageCounter(6, 3, bound -> bound);
        int timing = CultistRules.portalTiming(3);
        assertEquals(timing + (5 + timing / 3), counter);
    }

    @Test void theMinionsAndTheCratesKeepTheOriginalRolls() {
        assertTrue(CultistRules.minionIsKnight(0.34F));
        assertFalse(CultistRules.minionIsKnight(0.33F));
        assertEquals(2, CultistRules.lootCrateVariant(0.04F));
        assertEquals(1, CultistRules.lootCrateVariant(0.1F));
        assertEquals(0, CultistRules.lootCrateVariant(0.5F));
        assertEquals(0, CultistRules.portalSelfDamage(12, 4));
        assertEquals(9, CultistRules.portalSelfDamage(13, 4));
        assertEquals(1.0F, CultistRules.portalHeal(11));
        assertEquals(0.0F, CultistRules.portalHeal(12));
    }

    @Test void thePortalGeometryAndTimingsKeepTheOriginalNumbers() {
        assertEquals(160, CultistRules.PORTAL_BANNER_STAGE_COUNTER);
        assertEquals(6, CultistRules.PORTAL_BANNER_RADIUS);
        assertEquals(13, CultistRules.PORTAL_CRATE_INTERVAL);
        assertEquals(48.0, CultistRules.PORTAL_ACTIVATION_RANGE);
        assertEquals(32.0, CultistRules.PORTAL_MINION_RANGE);
        assertEquals(32, CultistRules.PORTAL_MINION_HOME_RADIUS);
        assertEquals(8, CultistRules.bannerFacing(2));
        assertEquals(0, CultistRules.bannerFacing(3));
        assertEquals(12, CultistRules.bannerFacing(4));
        assertEquals(4, CultistRules.bannerFacing(5));
    }

    // ---------------------------------------------------------------- the faction numbers
    @Test void theFactionKeepsTheOriginalAttributes() {
        assertEquals(20.0, CultistRules.CULTIST_HEALTH);
        assertEquals(4.0, CultistRules.CULTIST_DAMAGE);
        assertEquals(0.3, CultistRules.CULTIST_SPEED);
        assertEquals(32.0, CultistRules.CULTIST_FOLLOW_RANGE);
        assertEquals(10, CultistRules.CULTIST_EXPERIENCE);
        assertEquals(30.0, CultistRules.CLERIC_HEALTH);
        assertEquals(36.0, CultistRules.KNIGHT_HEALTH);
        assertEquals(125.0, CultistRules.LEADER_HEALTH);
        assertEquals(5.0, CultistRules.LEADER_DAMAGE);
        assertEquals(0.32, CultistRules.LEADER_SPEED);
        assertEquals(40, CultistRules.LEADER_EXPERIENCE);
        assertEquals(500.0, CultistRules.PORTAL_HEALTH);
        assertEquals(1.0, CultistRules.PORTAL_KNOCKBACK_RESISTANCE);
        assertEquals(0.95, CultistRules.BOSS_KNOCKBACK_RESISTANCE);
        assertEquals(40.0, CultistRules.BOSS_FOLLOW_RANGE);
        assertEquals(30, CultistRules.PORTAL_EXPERIENCE);
    }

    // ---------------------------------------------------------------- the door restriction
    @Test void theDoorRestrictionLooksForAVillageWithinSixteenBlocks() {
        // Original EntityAIRestrictOpenDoor:37 - the nearest village within sixteen blocks.
        assertEquals(16, CultistRules.RESTRICT_DOOR_VILLAGE_RADIUS);
        // Original EntityAIRestrictOpenDoor:41 - the mob stands within 2.25 blocks of the door.
        assertEquals(2.25D, CultistRules.RESTRICT_DOOR_DISTANCE_SQ, 1E-9D);
    }

    @Test void aTargetHasToStandInsideTheHomeAreaOfTheCultist() {
        // Original EntityAITarget.isSuitableTarget - the target has to be within the home
        // area of the mob; a mob without a home may target anybody.
        assertTrue(CultistRules.withinHomeArea(false, 4096.0D, 8.0D));
        assertTrue(CultistRules.withinHomeArea(true, 0.0D, 8.0D));
        assertTrue(CultistRules.withinHomeArea(true, 63.0D, 8.0D));
        assertFalse(CultistRules.withinHomeArea(true, 64.0D, 8.0D));
        assertFalse(CultistRules.withinHomeArea(true, 100.0D, 8.0D));
        // The praetor keeps a home of twenty four blocks, the ritualist cleric one of eight.
        assertTrue(CultistRules.withinHomeArea(true, 23.0D * 23.0D, CultistRules.BOSS_HOME_RADIUS));
        assertFalse(CultistRules.withinHomeArea(true, 25.0D * 25.0D, CultistRules.BOSS_HOME_RADIUS));
    }

    // ---------------------------------------------------------------- native ids
    @Test void theNativeGearIdsAreTheOnesTheLanguageFileReserves() throws Exception {        String lang = readLang();
        for (String id : List.of(CultistRules.ITEM_ROBE_HOOD, CultistRules.ITEM_ROBE_CHESTPLATE, CultistRules.ITEM_ROBE_LEGGINGS,
                CultistRules.ITEM_PLATE_HELM, CultistRules.ITEM_PLATE_CHESTPLATE, CultistRules.ITEM_PLATE_LEGGINGS,
                CultistRules.ITEM_LEADER_HELM, CultistRules.ITEM_LEADER_CHESTPLATE, CultistRules.ITEM_LEADER_LEGGINGS,
                CultistRules.ITEM_BOOTS, CultistRules.ITEM_CRIMSON_SWORD)) {
            assertTrue(lang.contains("\"item.thaumcraft." + id), "the language file has no entry for " + id);
        }
        assertTrue(lang.contains("\"entity.thaumcraft.cultistknight.name\""));
        assertTrue(lang.contains("\"entity.thaumcraft.cultistcleric.name\""));
        assertTrue(lang.contains("\"entity.thaumcraft.cultistleader.name\""));
        assertTrue(lang.contains("\"entity.thaumcraft.cultistportal.name\""));
        assertTrue(lang.contains("\"entity.thaumcraft.golemorb.name\""));
        for (String name : CultistRules.CHAMPION_MOD_NAMES) {
            assertTrue(lang.contains("\"champion.mod." + name + "\""), "the language file has no champion name " + name);
        }
    }

    @Test void theCultistGearIdsAreDistinct() {
        List<String> ids = List.of(CultistRules.ITEM_ROBE_HOOD, CultistRules.ITEM_ROBE_CHESTPLATE, CultistRules.ITEM_ROBE_LEGGINGS,
                CultistRules.ITEM_PLATE_HELM, CultistRules.ITEM_PLATE_CHESTPLATE, CultistRules.ITEM_PLATE_LEGGINGS,
                CultistRules.ITEM_LEADER_HELM, CultistRules.ITEM_LEADER_CHESTPLATE, CultistRules.ITEM_LEADER_LEGGINGS,
                CultistRules.ITEM_BOOTS, CultistRules.ITEM_CRIMSON_SWORD);
        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    private static String readLang() throws Exception {
        try (InputStream stream = CultistRulesTest.class.getClassLoader()
                .getResourceAsStream("assets/thaumcraft/lang/en_us.json")) {
            if (stream == null) throw new AssertionError("the packaged language file is missing");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

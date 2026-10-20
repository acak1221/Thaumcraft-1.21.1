package thaumcraft.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import thaumcraft.common.lib.mods.ChampionRules;
import thaumcraft.common.lib.mods.ChampionWiringRules;

/**
 * The glue that joins the champion system to the Outer Lands labyrinth, to the original
 * eldritch bosses and to the original death reward.
 */
class ChampionWiringRulesTest {
    @Test void originalDangerousLocationIsTheKeyRoomAndTheLibraryRoom() {
        // Oracle EventHandlerEntity.isDangerousLocation:426-431 - the cell feature six is the
        // key room and the cell feature eight is the library room.
        assertTrue(ChampionWiringRules.dangerousMazeFeature(6));
        assertTrue(ChampionWiringRules.dangerousMazeFeature(8));
        // Every other cell of the labyrinth is safe.
        for (int feature = 0; feature <= 14; feature++) {
            if (feature == 6 || feature == 8) continue;
            assertFalse(ChampionWiringRules.dangerousMazeFeature(feature), "feature " + feature);
        }
    }

    @Test void originalDangerousLocationNeedsTheOuterLands() {
        assertTrue(ChampionWiringRules.dangerousLocation(true, 6));
        assertTrue(ChampionWiringRules.dangerousLocation(true, 8));
        assertFalse(ChampionWiringRules.dangerousLocation(false, 6));
        assertFalse(ChampionWiringRules.dangerousLocation(false, 8));
        assertFalse(ChampionWiringRules.dangerousLocation(true, 0));
    }

    @Test void originalDangerousLocationReadsTheCellOfItsChunk() {
        // The 1.7 original shifted the block coordinate by four bits to find the maze cell.
        assertEquals(0, ChampionWiringRules.mazeChunk(0));
        assertEquals(0, ChampionWiringRules.mazeChunk(15));
        assertEquals(1, ChampionWiringRules.mazeChunk(16));
        assertEquals(1, ChampionWiringRules.mazeChunk(31));
        assertEquals(-1, ChampionWiringRules.mazeChunk(-1));
        assertEquals(-1, ChampionWiringRules.mazeChunk(-16));
        assertEquals(-2, ChampionWiringRules.mazeChunk(-17));
    }

    @Test void originalBossSkipsTheOrdinaryChampionBonuses() {
        // Oracle EntityUtils.makeChampion:243-254 - a boss keeps its own scaling.
        assertTrue(ChampionWiringRules.ordinaryChampionBonuses(false));
        assertFalse(ChampionWiringRules.ordinaryChampionBonuses(true));
    }

    @Test void originalBossIsAlwaysAChampion() {
        // Oracle ConfigEntities:208 - the boss base was whitelisted with the level two hundred,
        // so the shifted roll always reached the champion, even while the champion mobs were
        // switched off.
        assertEquals(ChampionRules.TIER_BOSS, ChampionRules.tierFor("thaumcraft:eldritch_warden"));
        assertEquals(ChampionRules.TIER_BOSS, ChampionRules.tierFor("thaumcraft:eldritch_golem"));
        assertEquals(199, ChampionRules.tierBonus(ChampionRules.TIER_BOSS, true));
        assertEquals(199, ChampionRules.tierBonus(ChampionRules.TIER_BOSS, false));
        assertTrue(ChampionRules.becomesChampion(99, ChampionRules.TIER_BOSS, 200.0F, true));
        assertTrue(ChampionRules.becomesChampion(99, ChampionRules.TIER_BOSS, 200.0F, false));
        // A boss never leaves the ordinary whitelist behind.
        assertEquals(ChampionRules.TIER_NONE, ChampionRules.tierFor("thaumcraft:cultist_leader"));
    }

    @Test void originalChampionRewardOnlyPaysAChampionMobThatWasHit() {
        // Oracle EventHandlerEntity.livingDrops:453 - the reward needs a hit, a mob, no boss
        // and a champion modifier.
        assertTrue(ChampionWiringRules.championReward(true, true, false, 0));
        assertTrue(ChampionWiringRules.championReward(true, true, false, 12));
        assertFalse(ChampionWiringRules.championReward(false, true, false, 0));
        assertFalse(ChampionWiringRules.championReward(true, false, false, 0));
        assertFalse(ChampionWiringRules.championReward(true, true, true, 0));
        assertFalse(ChampionWiringRules.championReward(true, true, false, ChampionRules.NO_CHAMPION));
        assertFalse(ChampionWiringRules.championReward(true, true, false, -2));
    }
}

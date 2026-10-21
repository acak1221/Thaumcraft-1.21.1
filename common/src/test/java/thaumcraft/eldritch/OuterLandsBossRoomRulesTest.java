package thaumcraft.eldritch;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.OuterLandsBossRooms;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The boss rooms that the eldritch lock opens in the Outer Lands.
 *
 * <p>The numbers come from the oracle of the original TC4 4.2.3.5,
 * {@code TileEldritchLock:62-354}: the counter of the boss map picks the boss, the four
 * arenas are built in the middle of the four chunks of the boss room at the height fifty,
 * and the exit of the boss room decides where the obelisks and the loot stand.
 */
class OuterLandsBossRoomRulesTest {

    @Test void theArenaStandsAtTheHeightFiftyInTheMiddleOfTheBossRoom() {
        // Oracle TileEldritchLock:132 - the arena of the boss.
        assertEquals(50, OuterLandsBossRooms.ARENA_Y);
    }

    @Test void theExitOfTheBossRoomDecidesWhereTheArenaStands() {
        // Oracle TileEldritchLock:136-152 and :219-239 - the exit two is the upper right
        // quarter of the boss room, the exit three the upper left, the exit four the lower
        // right and the exit five the lower left.
        assertEquals(8, OuterLandsBossRooms.exitOffsetX(2));
        assertEquals(8, OuterLandsBossRooms.exitOffsetZ(2));
        assertEquals(-8, OuterLandsBossRooms.exitOffsetX(3));
        assertEquals(8, OuterLandsBossRooms.exitOffsetZ(3));
        assertEquals(8, OuterLandsBossRooms.exitOffsetX(4));
        assertEquals(-8, OuterLandsBossRooms.exitOffsetZ(4));
        assertEquals(-8, OuterLandsBossRooms.exitOffsetX(5));
        assertEquals(-8, OuterLandsBossRooms.exitOffsetZ(5));
        assertEquals(0, OuterLandsBossRooms.exitOffsetX(0));
        assertEquals(0, OuterLandsBossRooms.exitOffsetZ(0));
    }

    @Test void aQuarterOfTheLocksCountTwice() {
        // Oracle TileEldritchLock:88-90 - the counter of the boss map is raised by one and
        // by a second one in a quarter of the cases.
        assertEquals(0.25F, OuterLandsBossRooms.DOUBLE_BOSS_CHANCE);
    }

    @Test void theBossOfTheArenaIsAnnouncedToThePlayersAroundTheLock() {
        // Oracle TileEldritchLock:126-130 - the squared distance of the announcement.
        assertEquals(300.0D, OuterLandsBossRooms.ANNOUNCE_DISTANCE_SQ);
    }

    @Test void thePedestalOfTheBossKeepsItsOriginalPattern() {
        // Oracle TileEldritchLock:42 - the three by three pattern of the pedestal, the minus
        // one is the eldritch block and the other values are the metadata of the stairs.
        assertArrayEquals(new int[] {2, 2, 2}, OuterLandsBossRooms.PEDESTAL[0]);
        assertArrayEquals(new int[] {0, -1, 1}, OuterLandsBossRooms.PEDESTAL[1]);
        assertArrayEquals(new int[] {3, 3, 3}, OuterLandsBossRooms.PEDESTAL[2]);
    }

    @Test void theLootUrnOfTheWardenIsRareInOneOfTenCells() {
        // Oracle TileEldritchLock:165-173 - the urn appears in nine of ten cells, ten percent
        // of them are rare and twenty percent of them are uncommon.
        assertEquals(0.9F, OuterLandsBossRooms.WARDEN_URN_CHANCE);
        assertEquals(0.1F, OuterLandsBossRooms.WARDEN_URN_RARE);
        assertEquals(0.3F, OuterLandsBossRooms.WARDEN_URN_UNCOMMON);
        assertEquals(2, OuterLandsBossRooms.wardenUrnMeta(0.05F));
        assertEquals(1, OuterLandsBossRooms.wardenUrnMeta(0.2F));
        assertEquals(0, OuterLandsBossRooms.wardenUrnMeta(0.5F));
    }

    @Test void theLootUrnOfTheGolemIsRarerThanTheOneOfTheWarden() {
        // Oracle TileEldritchLock:257-260 - five percent of the urns of the golem are rare,
        // fifteen percent are uncommon and the rest are plain.
        assertEquals(0.05F, OuterLandsBossRooms.GOLEM_URN_RARE);
        assertEquals(0.2F, OuterLandsBossRooms.GOLEM_URN_UNCOMMON);
        assertEquals(0.15F, OuterLandsBossRooms.GOLEM_LOOT_CHANCE);
        assertEquals(0.3F, OuterLandsBossRooms.GOLEM_CRATE_CHANCE);
        assertEquals(2, OuterLandsBossRooms.golemUrnMeta(0.01F));
        assertEquals(1, OuterLandsBossRooms.golemUrnMeta(0.1F));
        assertEquals(0, OuterLandsBossRooms.golemUrnMeta(0.5F));
    }

    @Test void theLootOfTheGolemStandsInTheFourCornersOfTheArena() {
        // Oracle TileEldritchLock:257 - only the four corners of the floor carry the loot.
        assertTrue(OuterLandsBossRooms.golemLootCorner(-3, -3));
        assertTrue(OuterLandsBossRooms.golemLootCorner(3, 3));
        assertTrue(OuterLandsBossRooms.golemLootCorner(-3, 3));
        assertTrue(OuterLandsBossRooms.golemLootCorner(3, -3));
        assertFalse(OuterLandsBossRooms.golemLootCorner(0, 0));
        assertFalse(OuterLandsBossRooms.golemLootCorner(-2, -2));
        assertFalse(OuterLandsBossRooms.golemLootCorner(3, 0));
    }

    @Test void theFloorOfTheCultIsHollowedInThreeRings() {
        // Oracle TileEldritchLock:288 - the ring two is halved, the ring three keeps two
        // thirds and the ring four keeps three quarters of its cells.
        assertTrue(OuterLandsBossRooms.cultistRingTwo(2, 0));
        assertTrue(OuterLandsBossRooms.cultistRingTwo(0, -2));
        assertFalse(OuterLandsBossRooms.cultistRingTwo(3, 0));
        assertTrue(OuterLandsBossRooms.cultistRingThree(3, 0));
        assertTrue(OuterLandsBossRooms.cultistRingThree(0, -3));
        assertFalse(OuterLandsBossRooms.cultistRingThree(2, 0));
        assertTrue(OuterLandsBossRooms.cultistRingFour(4, 0));
        assertTrue(OuterLandsBossRooms.cultistRingFour(0, 4));
        assertFalse(OuterLandsBossRooms.cultistRingFour(3, 0));
        assertEquals(0.33F, OuterLandsBossRooms.CULTIST_RING_THREE_KEEP);
        assertEquals(0.25F, OuterLandsBossRooms.CULTIST_RING_FOUR_KEEP);
    }

    @Test void theFloorOfTheTaintIsHollowedInThreeRings() {
        // Oracle TileEldritchLock:330 - the ring four is halved, the ring five keeps two
        // thirds and the ring seven keeps three quarters of its cells.
        assertTrue(OuterLandsBossRooms.taintRingFour(4, 0));
        assertTrue(OuterLandsBossRooms.taintRingFour(0, -4));
        assertFalse(OuterLandsBossRooms.taintRingFour(5, 0));
        assertTrue(OuterLandsBossRooms.taintRingFive(5, 0));
        assertTrue(OuterLandsBossRooms.taintRingFive(0, -6));
        assertFalse(OuterLandsBossRooms.taintRingFive(4, 0));
        assertTrue(OuterLandsBossRooms.taintRingSeven(7, 0));
        assertTrue(OuterLandsBossRooms.taintRingSeven(0, -9));
        assertFalse(OuterLandsBossRooms.taintRingSeven(6, 0));
        assertEquals(0.33F, OuterLandsBossRooms.TAINT_RING_FIVE_KEEP);
        assertEquals(0.25F, OuterLandsBossRooms.TAINT_RING_SEVEN_KEEP);
    }
}

package thaumcraft.eldritch;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.entities.OriginalPlayerSearch;
import thaumcraft.common.tiles.TileEldritchCrabSpawner;
import thaumcraft.common.tiles.TileEldritchLock;
import thaumcraft.common.tiles.TileEldritchTrap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original eldritch tiles: the runed trap, the crab spawner and the eldritch lock.
 *
 * <p>The numbers come from the oracle of the original TC4 4.2.3.5:
 * <ul>
 *   <li>{@code TileEldritchTrap:20-33} - the trap waits twenty ticks, then it looks for the
 *       closest player within three blocks, hurts that player for two points of magic damage
 *       and warps half of the hits one or two points into the void.</li>
 *   <li>{@code TileEldritchCrabSpawner:24-124} - the spawner counts down, warns with a block
 *       event and a fizz, releases the crab with the sound of the gore and never lets more
 *       than five crabs live around itself.</li>
 *   <li>{@code TileEldritchLock:49-60} - the lock counts to a hundred and one and pumps every
 *       fifth tick.</li>
 * </ul>
 */
class EldritchTileRulesTest {

    /* ------------------------------------------------------------------ */
    /* The runed trap                                                      */
    /* ------------------------------------------------------------------ */

    @Test void theTrapWaitsTwentyTicksAndLooksThreeBlocksAroundItself() {
        assertEquals(3.0D, TileEldritchTrap.PLAYER_RADIUS, 1.0E-9D);
        assertEquals(10, TileEldritchTrap.NEXT_CHECK_MIN);
        assertEquals(25, TileEldritchTrap.NEXT_CHECK_SPAN);
    }

    @Test void theTrapHurtsForTwoPointsOfMagicDamage() {
        assertEquals(2.0F, TileEldritchTrap.TRAP_DAMAGE, 1.0E-6F);
    }

    @Test void theTrapWarpsOneOrTwoPointsInHalfOfItsHits() {
        assertEquals(1, TileEldritchTrap.WARP_MIN);
        assertEquals(2, TileEldritchTrap.WARP_MAX);
    }

    /* ------------------------------------------------------------------ */
    /* The search of the closest player                                    */
    /* ------------------------------------------------------------------ */

    @Test void theSearchOfTheClosestPlayerIsASphereAndNotABox() {
        // Original World.getClosestPlayer - a player is only taken when it stands strictly
        // inside the bound of the search.
        assertTrue(OriginalPlayerSearch.accepts(8.99D, 9.0D, false));
        assertFalse(OriginalPlayerSearch.accepts(9.0D, 9.0D, false));
        assertFalse(OriginalPlayerSearch.accepts(9.01D, 9.0D, false));
    }

    @Test void theSearchOfTheClosestPlayerNeverTakesACreativePlayer() {
        // Original World.getClosestPlayer - the creative player is skipped.
        assertFalse(OriginalPlayerSearch.accepts(1.0D, 9.0D, true));
        assertTrue(OriginalPlayerSearch.accepts(1.0D, 9.0D, false));
    }

    @Test void aPlayerOnTheCentreOfTheSearchEndsTheSearch() {
        // Original World.getClosestPlayer - the inner check "if (d1 > 0.0)" refuses every
        // further player once the bound reached zero.
        assertTrue(OriginalPlayerSearch.accepts(0.0D, 9.0D, false));
        assertFalse(OriginalPlayerSearch.accepts(0.5D, 0.0D, false));
        assertFalse(OriginalPlayerSearch.accepts(0.0D, 0.0D, false));
    }

    /* ------------------------------------------------------------------ */
    /* The crab spawner                                                    */
    /* ------------------------------------------------------------------ */

    @Test void theSpawnerNeedsAPlayerWithinSixteenBlocks() {
        assertEquals(16.0D, TileEldritchCrabSpawner.ACTIVATION_RADIUS, 1.0E-9D);
    }

    @Test void theSpawnerNeverLetsMoreThanFiveCrabsLiveAroundItself() {
        // Original TileEldritchCrabSpawner:79-81 - the block of the spawner and thirty two
        // blocks around it may hold six crabs.
        assertEquals(5, TileEldritchCrabSpawner.MAX_CRABS);
    }

    @Test void theSpawnerWarnsWithABlockEventAndAVentOfTwentyTicks() {
        // Original TileEldritchCrabSpawner:37 and :47-55.
        assertEquals(1, TileEldritchCrabSpawner.VENT_EVENT);
        assertEquals(20, TileEldritchCrabSpawner.VENT_TICKS);
    }

    @Test void theVentOfTheSpawnerIsTheColourOfTheOriginal() {
        // Original TileEldritchCrabSpawner:67 - the colour of the vent particles.
        assertEquals(0x9988AA, TileEldritchCrabSpawner.VENT_COLOUR);
    }

    /* ------------------------------------------------------------------ */
    /* The eldritch lock                                                   */
    /* ------------------------------------------------------------------ */

    @Test void theLockPumpsEveryFifthTickAndOpensAfterAHundredAndOneTicks() {
        // Original TileEldritchLock:49-60.
        assertEquals(5, TileEldritchLock.PUMP_INTERVAL);
        assertEquals(100, TileEldritchLock.OPEN_AFTER);
    }
}

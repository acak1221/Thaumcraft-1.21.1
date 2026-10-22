package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.lib.golems.GolemLadderRules;
import thaumcraft.common.lib.golems.GolemLadderRules.Entry;
import thaumcraft.common.lib.golems.GolemRouteRules;

/**
 * The ladder of the golem, pinned against the original.
 *
 * <p>The original registered <b>five</b> selector entries per inventory core
 * (TC4 4.2.3.5 oracle, {@code thaumcraft/common/entities/golems/EntityGolemBase.java:270-277,278-285,335-341})
 * and every one of them took the mutex bits three, so the entries of a core excluded each other and
 * the lowest number won. The 1.7.10 selector tested two goals with
 * {@code (mutexA & mutexB) == 0}, so the bits overlap and not the numbers are equal.</p>
 *
 * <p>No test of this class constructs a {@code Block}: every rule is a pure static function of a
 * world-free class and the only Minecraft type it touches is the {@link Goal.Flag} enum.</p>
 */
class GolemLadderRulesTest {

    @Test void theOriginalRegisteredFiveEntriesPerCore() {
        List<Entry> fill = GolemLadderRules.ladderOf("fill");
        assertEquals(5, fill.size(), "the Fill core of the original had five entries");
        assertEquals("AIHomeReplace", fill.get(0).goal());
        assertEquals(0, fill.get(0).priority());
        assertEquals("AIHomePlace", fill.get(1).goal());
        assertEquals(1, fill.get(1).priority());
        assertEquals("AIHomeDrop", fill.get(2).goal());
        assertEquals(2, fill.get(2).priority());
        assertEquals("AIFillTake", fill.get(3).goal());
        assertEquals(3, fill.get(3).priority());
        assertEquals("AIFillGoto", fill.get(4).goal());
        assertEquals(4, fill.get(4).priority());

        List<Entry> empty = GolemLadderRules.ladderOf("empty");
        assertEquals(5, empty.size(), "the Empty core of the original had five entries");
        assertEquals("AIHomeReplace", empty.get(0).goal());
        assertEquals(0, empty.get(0).priority());
        assertEquals("AIEmptyPlace", empty.get(1).goal());
        assertEquals(1, empty.get(1).priority());
        assertEquals("AIEmptyDrop", empty.get(2).goal());
        assertEquals(2, empty.get(2).priority());
        assertEquals("AIEmptyGoto", empty.get(3).goal());
        assertEquals(3, empty.get(3).priority());
        assertEquals("AIHomeTake", empty.get(4).goal());
        assertEquals(4, empty.get(4).priority());

        List<Entry> sorting = GolemLadderRules.ladderOf("sorting");
        assertEquals(4, sorting.size(), "the Sorting core of the original had four entries");
        assertEquals("AIHomeReplace", sorting.get(0).goal());
        assertEquals(0, sorting.get(0).priority());
        assertEquals("AISortingPlace", sorting.get(1).goal());
        assertEquals(1, sorting.get(1).priority());
        assertEquals("AISortingGoto", sorting.get(2).goal());
        assertEquals(3, sorting.get(2).priority());
        assertEquals("AIHomeTakeSorting", sorting.get(3).goal());
        assertEquals(4, sorting.get(3).priority());
    }

    @Test void everyEntryOfTheLadderTookTheMutexBitsThree() {
        for (Entry entry : GolemLadderRules.LADDER) {
            assertEquals(3, entry.mutex(), entry.goal() + " of the " + entry.core() + " core");
        }
        assertEquals(14, GolemLadderRules.LADDER.size(), "5 + 5 + 4 entries");
    }

    @Test void theMutexBitsExcludeEachOtherWhenTheyOverlap() {
        // The 1.7.10 test was (mutexA & mutexB) == 0, so the bits OVERLAP and the
        // numbers are not compared for equality.
        assertFalse(GolemLadderRules.compatible(3, 3), "two ladder entries exclude each other");
        assertFalse(GolemLadderRules.compatible(3, 1), "the flight from a creeper excludes the ladder");
        assertFalse(GolemLadderRules.compatible(3, 2), "the watch of the closest player excludes the ladder");
        assertTrue(GolemLadderRules.compatible(3, 0), "the idle look excludes nothing");
        assertTrue(GolemLadderRules.compatible(1, 2), "the flight and the watch do not exclude each other");
        // The original's mask of the shared goals of every core.
        assertEquals(1, GolemLadderRules.CREEPER_MUTEX, "AIAvoidCreeperSwell");
        assertEquals(2, GolemLadderRules.WATCH_MUTEX, "EntityAIWatchClosest");
        assertEquals(0, GolemLadderRules.IDLE_MUTEX, "EntityAILookIdle");
    }

    @Test void theNativeFlagsAreTheTwoFlagsOfTheMutexBitsThree() {
        assertEquals(java.util.Set.of(Goal.Flag.MOVE, Goal.Flag.LOOK), GolemLadderRules.ladderFlags(),
                "the bits three of the original are the movement and the look");
    }

    @Test void theLadderIsHeldForTwoHundredTicksAfterAPartialTransfer() {
        // Original AIEmptyPlace.func_75249_e:99 and AIHomeDrop.func_75249_e:72 set the
        // counter to 200, AIEmptyPlace.func_75246_d:95 lowers it and
        // AIEmptyPlace.func_75253_b:80 keeps the goal running while it is positive.
        assertEquals(200, GolemLadderRules.PARTIAL_HOLD);
        // The chest of a transfer stays open for five ticks.
        assertEquals(5, GolemRouteRules.CHEST_WINDOW);
    }

    @Test void theInstalledNumbersAreTheOnesOfTheOriginal() {
        // The native installs the entries with the numbers of the original.
        assertEquals(GolemLadderRules.ladderOf("fill").get(0).priority(), GoalPriority.HOME_REPLACE);
        assertEquals(GolemLadderRules.ladderOf("fill").get(1).priority(), GoalPriority.HOME_PLACE);
        assertEquals(GolemLadderRules.ladderOf("fill").get(2).priority(), GoalPriority.HOME_DROP);
        assertEquals(GolemLadderRules.ladderOf("fill").get(3).priority(), GoalPriority.FILL_TAKE);
        assertEquals(GolemLadderRules.ladderOf("fill").get(4).priority(), GoalPriority.FILL_GOTO);
        assertEquals(GolemLadderRules.ladderOf("empty").get(1).priority(), GoalPriority.EMPTY_PLACE);
        assertEquals(GolemLadderRules.ladderOf("empty").get(2).priority(), GoalPriority.EMPTY_DROP);
        assertEquals(GolemLadderRules.ladderOf("empty").get(3).priority(), GoalPriority.EMPTY_GOTO);
        assertEquals(GolemLadderRules.ladderOf("empty").get(4).priority(), GoalPriority.HOME_TAKE);
        assertEquals(GolemLadderRules.ladderOf("sorting").get(1).priority(), GoalPriority.SORTING_PLACE);
        assertEquals(GolemLadderRules.ladderOf("sorting").get(2).priority(), GoalPriority.SORTING_GOTO);
        assertEquals(GolemLadderRules.ladderOf("sorting").get(3).priority(), GoalPriority.HOME_TAKE_SORTING);
    }
}

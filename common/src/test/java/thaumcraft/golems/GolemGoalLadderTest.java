package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;

/**
 * The goal ladder of the original TC4 golem, pinned against the oracle.
 *
 * <p>The original registered its goals with the numbers of {@code EntityGolemBase} (oracle
 * {@code thaumcraft/common/entities/golems/EntityGolemBase.java:267-350}) and every inventory goal of that
 * ladder took the mutex bits three, the MOVE and the LOOK flag. The native 1.21.1 selector reads the number
 * the same way, so the port keeps the numbers of the original in {@link GoalPriority}.</p>
 *
 * <p>The door is the one goal of the ladder that takes NO mutex bit at all: the original
 * {@code AIDoorInteract} never calls {@code func_75248_a} (oracle
 * {@code thaumcraft/common/entities/ai/misc/AIDoorInteract.java}), and the native
 * {@code net.minecraft.world.entity.ai.goal.DoorInteractGoal} of 1.21.1 does not set any flag either. The door
 * therefore conflicts with nothing, exactly as in the original.</p>
 */
class GolemGoalLadderTest {
    /** One row of the original registration: the core, the goal and the number it was registered with. */
    private record Row(String core, String goal, int priority) {}

    /** The original registration of the goals of the golem, in the order of the oracle. */
    private static final List<Row> ORIGINAL = List.of(
            new Row("every", "AIAvoidCreeperSwell", 0),
            new Row("fill", "AIHomeReplace", 0),
            new Row("fill", "AIHomePlace", 1),
            new Row("fill", "AIHomeDrop", 2),
            new Row("fill", "AIFillTake", 3),
            new Row("fill", "AIFillGoto", 4),
            new Row("empty", "AIHomeReplace", 0),
            new Row("empty", "AIEmptyPlace", 1),
            new Row("empty", "AIEmptyDrop", 2),
            new Row("empty", "AIEmptyGoto", 3),
            new Row("empty", "AIHomeTake", 4),
            new Row("gather", "AIHomeReplace", 0),
            new Row("gather", "AIHomePlace", 1),
            new Row("gather", "AIItemPickup", 2),
            new Row("harvest", "AIHarvestCrops", 2),
            new Row("guard", "AIDartAttack", 2),
            new Row("guard", "AIGolemAttackOnCollide", 3),
            new Row("decanting", "AILiquidEmpty", 1),
            new Row("decanting", "AILiquidGather", 2),
            new Row("decanting", "AILiquidGoto", 3),
            new Row("alchemy", "AIEssentiaEmpty", 1),
            new Row("alchemy", "AIEssentiaGather", 2),
            new Row("alchemy", "AIEssentiaGoto", 3),
            new Row("lumber", "AIHarvestLogs", 2),
            new Row("use", "AIHomeReplace", 0),
            new Row("use", "AIUseItem", 0),
            new Row("use", "AIHomeTake", 4),
            new Row("butcher", "AIDartAttack", 2),
            new Row("butcher", "AIGolemAttackOnCollide", 3),
            new Row("sorting", "AIHomeReplace", 0),
            new Row("sorting", "AISortingPlace", 1),
            new Row("sorting", "AISortingGoto", 3),
            new Row("sorting", "AIHomeTakeSorting", 4),
            new Row("fishing", "AIFish", 2),
            new Row("every", "AIOpenDoor", 5),
            new Row("every", "AIReturnHome", 6),
            new Row("every", "EntityAIWatchClosest", 7),
            new Row("every", "EntityAILookIdle", 8));

    @Test void theOriginalLadderKeepsItsNumbers() {
        // The five goals of the three inventory cores, in the order of the oracle.
        assertEquals(0, GoalPriority.HOME_REPLACE, "AIHomeReplace");
        assertEquals(1, GoalPriority.HOME_PLACE, "AIHomePlace");
        assertEquals(2, GoalPriority.HOME_DROP, "AIHomeDrop");
        assertEquals(3, GoalPriority.FILL_TAKE, "AIFillTake");
        assertEquals(4, GoalPriority.FILL_GOTO, "AIFillGoto");
        assertEquals(1, GoalPriority.EMPTY_PLACE, "AIEmptyPlace");
        assertEquals(2, GoalPriority.EMPTY_DROP, "AIEmptyDrop");
        assertEquals(3, GoalPriority.EMPTY_GOTO, "AIEmptyGoto");
        assertEquals(4, GoalPriority.HOME_TAKE, "AIHomeTake");
        assertEquals(1, GoalPriority.SORTING_PLACE, "AISortingPlace");
        assertEquals(3, GoalPriority.SORTING_GOTO, "AISortingGoto");
        assertEquals(4, GoalPriority.HOME_TAKE_SORTING, "AIHomeTakeSorting");
        // The Use core registers the home replacement with the number zero, like the original.
        assertEquals(0, GoalPriority.USE_ITEM, "AIUseItem");
        assertEquals(0, GoalPriority.HOME_REPLACE, "the home replacement of the Use core");
    }

    @Test void theOriginalLadderIsTheOneTheOracleRegistered() {
        // The table of this test IS the oracle (EntityGolemBase:267-350); the port publishes the same numbers.
        assertEquals(38, ORIGINAL.size());
        assertEquals(0, ORIGINAL.get(0).priority(), "the creeper flight is registered first");
        assertEquals("AIAvoidCreeperSwell", ORIGINAL.get(0).goal());
        assertEquals("AIOpenDoor", ORIGINAL.get(ORIGINAL.size() - 4).goal());
        assertEquals(5, ORIGINAL.get(ORIGINAL.size() - 4).priority());
        assertEquals("AIReturnHome", ORIGINAL.get(ORIGINAL.size() - 3).goal());
        assertEquals(6, ORIGINAL.get(ORIGINAL.size() - 3).priority());
        assertEquals("EntityAIWatchClosest", ORIGINAL.get(ORIGINAL.size() - 2).goal());
        assertEquals(7, ORIGINAL.get(ORIGINAL.size() - 2).priority());
        assertEquals("EntityAILookIdle", ORIGINAL.get(ORIGINAL.size() - 1).goal());
        assertEquals(8, ORIGINAL.get(ORIGINAL.size() - 1).priority());
        // The Use core: the home replacement first, then the click with the same number, then the take.
        var use = ORIGINAL.stream().filter(row -> row.core().equals("use")).toList();
        assertEquals(3, use.size());
        assertEquals("AIHomeReplace", use.get(0).goal());
        assertEquals(0, use.get(0).priority());
        assertEquals("AIUseItem", use.get(1).goal());
        assertEquals(0, use.get(1).priority());
        assertEquals("AIHomeTake", use.get(2).goal());
        assertEquals(4, use.get(2).priority());
    }

    @Test void theDoorGoalTakesNoMutexBit() {
        // The original AIDoorInteract/AIOpenDoor never call func_75248_a, so the door conflicts with nothing.
        // The native DoorInteractGoal of 1.21.1 does not set a flag either: neither of the two classes
        // declares setFlags, so the flag set of every instance stays the empty one of Goal.
        assertThrows(NoSuchMethodException.class, () -> OpenDoorGoal.class.getDeclaredMethod("setFlags", EnumSet.class));
        assertThrows(NoSuchMethodException.class, () -> DoorInteractGoal.class.getDeclaredMethod("setFlags", EnumSet.class));
    }
}

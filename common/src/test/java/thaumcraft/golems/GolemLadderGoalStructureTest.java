package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.ai.goal.Goal;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.lib.golems.GolemLadderRules;
import thaumcraft.common.lib.golems.GolemLadderRules.Entry;

/**
 * The structure of the goal ladder of the native tree, pinned against the original.
 *
 * <p>The original registered <b>five</b> selector entries per inventory core (TC4 4.2.3.5 oracle,
 * {@code thaumcraft/common/entities/golems/EntityGolemBase.java:270-277,278-285,335-341}), every one
 * of them with the mutex bits three and with its own number. The 1.21.1 selector gives the two
 * exclusive flags of the bits three to the entry with the <b>lowest</b> number that wants to run
 * ({@code GoalSelector.tick}: {@code goalCanBeReplacedForAllFlags} and
 * {@code WrappedGoal.canBeReplacedBy}), so the numbers and the flags of the entries ARE the ladder:
 * a wrong number or a missing exclusive flag lets the wrong entry take the ladder, and the golem
 * stands still.</p>
 *
 * <p><b>The regression this class guards.</b> The wave-3 rewrite gave every entry its own action
 * hook, {@code ladderStart()}, and never wired it to the vanilla {@link Goal#start()} the selector
 * calls: {@code Goal.start()} is a no-op in 1.21.1, so every entry took the two exclusive flags of
 * the ladder and did nothing at all. The three GameTest scenarios of the inventory cores timed out
 * with a golem that stood still. The action of an entry is therefore {@link Goal#start()} itself and
 * it is <b>abstract</b> in the base of the entries, so the compiler refuses an entry without one;
 * the tests below pin that structure against the original.</p>
 *
 * <p>The entries live in the package-private {@code thaumcraft.common.entities.golems}, so this test
 * reads them reflectively — the same idiom the {@code GolemGoalLadderTest} of the same directory
 * uses for the vanilla door goal. No test of this class constructs a {@code Block}, an
 * {@code Entity} or a {@code Level}: the only Minecraft types it touches are the
 * {@link Goal.Flag} enum and the class files of the entries.</p>
 */
class GolemLadderGoalStructureTest {

    /** The class that holds the entries of the ladder of the three inventory cores. */
    private static final String OWNER = "thaumcraft.common.entities.golems.InventoryGolemGoals";

    /** The entries of the native tree, one per goal of the original. */
    private static final Set<String> ENTRIES = Set.of("HomeReplace", "HomePlace", "HomeDrop", "FillTake",
            "EmptyPlace", "EmptyDrop", "HomeTake", "SortingPlace", "HomeTakeSorting", "Route");

    /** The goal of the original and the entry of the native tree that reproduces it. */
    private static final Map<String, String> ENTRY_OF_GOAL = Map.ofEntries(
            Map.entry("AIHomeReplace", "HomeReplace"),
            Map.entry("AIHomePlace", "HomePlace"),
            Map.entry("AIHomeDrop", "HomeDrop"),
            Map.entry("AIFillTake", "FillTake"),
            Map.entry("AIEmptyPlace", "EmptyPlace"),
            Map.entry("AIEmptyDrop", "EmptyDrop"),
            Map.entry("AIHomeTake", "HomeTake"),
            Map.entry("AISortingPlace", "SortingPlace"),
            Map.entry("AIHomeTakeSorting", "HomeTakeSorting"),
            Map.entry("AIFillGoto", "Route"),
            Map.entry("AIEmptyGoto", "Route"),
            Map.entry("AISortingGoto", "Route"));

    // ---- the entries ------------------------------------------------------------------------------

    @Test void theLadderHasExactlyTheEntriesOfTheOriginal() throws Exception {
        Set<String> actual = new LinkedHashSet<>();
        for (Class<?> nested : owner().getDeclaredClasses()) {
            if (!Goal.class.isAssignableFrom(nested) || Modifier.isAbstract(nested.getModifiers())) continue;
            actual.add(nested.getSimpleName());
        }
        assertEquals(ENTRIES, actual,
                "the ladder of the native tree has the entries of the original and nothing else");
    }

    /**
     * The regression guard: every entry declares the action of the entry.
     *
     * <p>The action is the original's {@code startExecuting} (the oracle's {@code func_75249_e}),
     * which the 1.7.10 selector calls when the entry takes the ladder; the 1.21.1 selector calls
     * {@link Goal#start()} in the same place. An entry that does not declare {@code start()} would
     * inherit the no-op of the vanilla goal, take the two exclusive flags of the ladder and never
     * do anything: the golem would stand still.</p>
     */
    @Test void everyEntryOfTheLadderHasAnAction() throws Exception {
        for (String name : ENTRIES) {
            Class<?> entry = entryClass(name);
            assertNotNull(entry.getDeclaredMethod("start"),
                    name + " must declare the action of the entry: the vanilla start() is a no-op");
            assertNotNull(entry.getDeclaredMethod("ladderCanUse"),
                    name + " must declare the condition of the entry");
            assertTrue(Modifier.isPublic(entry.getDeclaredMethod("start").getModifiers()),
                    name + " overrides the vanilla start()");
        }
    }

    /** The base of the entries declares the action abstract, so an entry without one cannot be written. */
    @Test void theActionOfTheLadderIsAbstract() throws Exception {
        Method action = entryClass("LadderGoal").getDeclaredMethod("start");
        assertTrue(Modifier.isAbstract(action.getModifiers()),
                "the action of the ladder is abstract: the compiler refuses an entry without one");
        Method condition = entryClass("LadderGoal").getDeclaredMethod("canUse");
        assertTrue(Modifier.isPublic(condition.getModifiers()) && !Modifier.isAbstract(condition.getModifiers()),
                "the base of the entries wires the condition of an entry into the vanilla canUse()");
    }

    // ---- the numbers -----------------------------------------------------------------------------

    /** Every entry carries the number the original registered it with. */
    @Test void everyEntryCarriesTheNumberOfTheOriginal() throws Exception {
        for (Entry row : GolemLadderRules.LADDER) {
            assertEquals(row.priority(), numberOf(row),
                    row.goal() + " of the " + row.core() + " core carries the number of the original");
        }
    }

    /** The numbers of the entries are the numbers of the published goal priorities. */
    @Test void theNumbersOfTheEntriesAreTheOnesOfTheOriginal() throws Exception {
        assertEquals(0, number("HomeReplace"), "AIHomeReplace");
        assertEquals(1, number("HomePlace"), "AIHomePlace");
        assertEquals(2, number("HomeDrop"), "AIHomeDrop");
        assertEquals(3, number("FillTake"), "AIFillTake");
        assertEquals(1, number("EmptyPlace"), "AIEmptyPlace");
        assertEquals(2, number("EmptyDrop"), "AIEmptyDrop");
        assertEquals(4, number("HomeTake"), "AIHomeTake");
        assertEquals(1, number("SortingPlace"), "AISortingPlace");
        assertEquals(4, number("HomeTakeSorting"), "AIHomeTakeSorting");
        // The route of a core: the Fill route is the number four, the Empty and the Sorting route three.
        assertEquals(4, number("Route", GolemData.FILL), "AIFillGoto");
        assertEquals(3, number("Route", GolemData.EMPTY_CORE), "AIEmptyGoto");
        assertEquals(3, number("Route", GolemData.SORTING), "AISortingGoto");
    }

    /** The entries of the ladder are the goals of the table of the original, one for one. */
    @Test void theEntriesAreTheGoalsOfTheTableOfTheOriginal() {
        Set<String> goals = new LinkedHashSet<>();
        for (Entry row : GolemLadderRules.LADDER) goals.add(row.goal());
        assertEquals(ENTRY_OF_GOAL.keySet(), goals, "every goal of the table has an entry of the native tree");
        assertEquals(ENTRIES, new LinkedHashSet<>(ENTRY_OF_GOAL.values()),
                "every entry of the native tree is a goal of the table of the original");
    }

    // ---- the exclusive flags ---------------------------------------------------------------------

    /**
     * The entries of the ladder take the two exclusive flags of the mutex bits three.
     *
     * <p>The 1.7.10 selector tested two goals with {@code (mutexA & mutexB) == 0}, so the bits
     * three of the original (the movement and the look) are the two exclusive flags of the 1.21.1
     * selector: the entries of a core exclude each other, and the return home, the creeper flight
     * and the watch of the closest player are excluded as well.</p>
     */
    @Test void theEntriesTakeTheTwoExclusiveFlags() throws Exception {
        assertEquals(Set.of(Goal.Flag.MOVE, Goal.Flag.LOOK), GolemLadderRules.ladderFlags(),
                "the bits three of the original are the movement and the look");
        String bytecode = bytecodeOf(entryClass("LadderGoal"));
        assertTrue(bytecode.contains("ladderFlags"),
                "the base of the entries takes its flags from the rules of the ladder");
        assertTrue(bytecode.contains("setFlags"),
                "the base of the entries hands the two exclusive flags to the vanilla goal");
    }

    // ---- the helpers -----------------------------------------------------------------------------

    private static Class<?> owner() throws Exception {
        return Class.forName(OWNER);
    }

    private static Class<?> entryClass(String simpleName) throws Exception {
        for (Class<?> nested : owner().getDeclaredClasses()) {
            if (nested.getSimpleName().equals(simpleName)) return nested;
        }
        throw new AssertionError("the ladder of the golem has no entry named " + simpleName);
    }

    /** The number an entry of the table carries. */
    private static int numberOf(Entry row) throws Exception {
        String entry = ENTRY_OF_GOAL.get(row.goal());
        if (row.goal().endsWith("Goto")) return number(entry, coreOf(row.core()));
        return number(entry);
    }

    private static int number(String entry) throws Exception {
        Method method = entryClass(entry).getDeclaredMethod("number");
        method.setAccessible(true);
        return (int) method.invoke(null);
    }

    private static int number(String entry, int core) throws Exception {
        Method method = entryClass(entry).getDeclaredMethod("number", int.class);
        method.setAccessible(true);
        return (int) method.invoke(null, core);
    }

    private static int coreOf(String core) {
        return switch (core) {
            case "fill" -> GolemData.FILL;
            case "empty" -> GolemData.EMPTY_CORE;
            case "sorting" -> GolemData.SORTING;
            default -> GolemData.NO_CORE;
        };
    }

    /** The class file of a class as text: its constant pool holds the names of the methods it calls. */
    private static String bytecodeOf(Class<?> type) throws Exception {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getResourceAsStream(resource)) {
            assertNotNull(in, "the class file of " + type.getName());
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    /** The table of the ladder, for the reader of this test. */
    @Test void theTableOfTheLadderIsTheOneOfTheOriginal() {
        List<Entry> ladder = GolemLadderRules.LADDER;
        assertEquals(14, ladder.size(), "5 + 5 + 4 entries");
        for (Entry row : ladder) {
            assertEquals(3, row.mutex(), row.goal() + " took the mutex bits three");
        }
    }
}

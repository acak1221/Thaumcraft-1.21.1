package thaumcraft.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The isolation of the GameTest scenarios of the port.
 *
 * <p>Three GameTest scenarios of the golems (the entropy item match, the order colours and the
 * golem fetter) failed in the whole suite and passed alone: a scenario of the same batch cleaned
 * up the entities of a wide area and discarded their golems, and another scenario of the same
 * batch released the forced chunks of their ring. The two defects are in the GameTest harness of
 * the port, which the JUnit suite cannot reach at run time, so this test pins the rules of the
 * harness in the sources that carry them:</p>
 *
 * <ul>
 * <li>the clean up of the shared lane fixture stays inside the lane: the vanilla grid packs the
 * scenarios of a batch thirteen blocks apart (an eight block wide structure and five blocks of
 * margin) and the fixture of the neighbour starts nine blocks away, so the sweep of the lane must
 * stay below nine blocks;</li>
 * <li>the golem scenarios arm their golem again when it was discarded, so a foreign clean up can
 * never stop them;</li>
 * <li>the golem scenarios take their ring of chunks back, so they never depend on the chunk
 * tickets of another scenario.</li>
 * </ul>
 */
class GameTestIsolationTest {
    /** The width of the vanilla GameTest structure and the margin the grid leaves around it. */
    private static final int STRUCTURE = 8, MARGIN = 5;
    /** The distance between the origins of the lanes of two neighbouring scenarios. */
    private static final int NEIGHBOUR = STRUCTURE + MARGIN;
    /** The blocks of the lane's own fixture around the origin of the lane. */
    private static final int FIXTURE = 4;

    private static String source(String name) throws IOException {
        Path file = Path.of("src/gameTest/java", "thaumcraft/common/testing", name);
        assertTrue(Files.exists(file), "the GameTest source " + file + " is there");
        return Files.readString(file);
    }

    @Test void theLaneCleanupStaysInsideItsOwnFixture() throws IOException {
        String lane = source("WaveLane.java");
        int radius = -1;
        for (String line : lane.split("\n")) {
            if (line.contains("CLEAN_RADIUS") && line.contains("=")) {
                radius = Integer.parseInt(line.replaceAll(".*= ", "").replace(";", "").trim());
                break;
            }
        }
        assertTrue(radius > 0, "the lane names the radius of its clean up sweep");
        assertTrue(radius <= FIXTURE + 1,
                "the sweep of the lane stays inside the lane's own footprint of " + FIXTURE + " blocks, was " + radius);
        assertTrue(radius < NEIGHBOUR - FIXTURE,
                "the sweep of the lane stops before the fixture of the neighbour, which starts "
                        + (NEIGHBOUR - FIXTURE) + " blocks away, was " + radius);
    }

    @Test void theGolemScenariosArmTheirGolemAgain() throws IOException {
        for (String name : new String[] {"GolemItemMatchRuntimeChecks.java", "GolemColorsRuntimeChecks.java",
                "GolemFetterRuntimeChecks.java"}) {
            String scenario = source(name);
            assertTrue(scenario.contains("isRemoved()"),
                    name + " watches its golem: a foreign clean up may discard it");
            assertTrue(scenario.contains("arms it again") || scenario.contains("armRouter()") || scenario.contains("arm(lane)"),
                    name + " arms its golem again when it was discarded");
        }
    }

    @Test void theGolemScenariosTakeTheirChunkRingBack() throws IOException {
        for (String name : new String[] {"GolemItemMatchRuntimeChecks.java", "GolemColorsRuntimeChecks.java",
                "GolemFetterRuntimeChecks.java"}) {
            assertTrue(source(name).contains("GolemChunks.refresh("),
                    name + " takes its ring of chunks back: another scenario may release a chunk of it");
        }
    }

    /**
     * The Guard range row of the advanced golems reaches two chunks from its home. A spider added to a chunk that only
     * {@code level.getChunk} loaded is not a level entity until a later main-thread task makes the chunk's entities visible,
     * so the Guard refused it whenever the random GameTest origin put a chunk border inside the row (CI on main, 27.10.2026).
     */
    @Test void theAdvancedGuardRowWaitsUntilItsChunksTickEntities() throws IOException {
        String advanced = source("GolemAdvancedRuntimeChecks.java");
        assertTrue(advanced.contains("level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk);")
                        && advanced.contains("GameTestChunkAwait.entityTicking(test, base.offset(distance, 0, 0))")
                        && advanced.contains("for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);"),
                "the row's chunks are forced, awaited until they tick entities, and released");
        assertFalse(advanced.contains("level.getChunk(pos);"), "a bare getChunk does not make the row's spiders level entities");
        String wait = source("GameTestChunkAwait.java");
        assertTrue(wait.contains("level.isPositionEntityTicking(pos)") && wait.contains("source.pollTask()"),
                "the wait services the native load until the chunk ticks entities");
    }

    @Test void everyFabricScenarioHasAnOwnershipAndGlobalStateVerdict() throws IOException {
        Path fabric = Path.of("../fabric/src/gameTest");
        String mixins = Files.readString(fabric.resolve("resources/thaumcraft-gametest-selection.mixins.json"));
        for (String name : new String[] {"GameTestStateGuardMixin", "GameTestForcedChunkGuardMixin",
                "GameTestRegionTicketGuardMixin", "GameTestConnectedPlayerGuardMixin"}) {
            assertTrue(mixins.contains('"' + name + '"'), name + " must actually be loaded");
        }
        String hook = Files.readString(fabric.resolve("java/thaumcraft/fabric/testing/mixin/GameTestStateGuardMixin.java"));
        assertTrue(hook.contains("tickInternal()V") && hook.contains("shift = At.Shift.AFTER"),
                "check after synchronous finally blocks but before native verdict reporting");
        String guard = source("GameTestStateGuard.java");
        for (String invariant : new String[] {"info.getTestName()", "before.settings", "before.recipes",
                "before.live", "RESOURCES", "info.fail("}) {
            assertTrue(guard.contains(invariant), "native isolation verdict retains " + invariant);
        }
    }

    @Test void integrityVerdictsAlsoWitnessNativeWorkerProgress() throws IOException {
        String guard = source("GameTestStateGuard.java");
        assertTrue(guard.contains("unfinishedPickupTarget()") && guard.contains("incompleteApproaches < 3"),
                "repeated completed paths outside actual action reach must be named, not mistaken for global leaks");
        assertTrue(guard.contains("cleanup.run()") && guard.contains("GameTest work witness:"),
                "progress refusal cleans its owner and retains the actual worker state before publication");
        assertTrue(source("GolemEndToEndRuntimeChecks.java").contains("GameTestStateGuard.watchWorker(golem, this::close)"));
        assertTrue(source("GolemEndToEndRuntimeChecks.java").contains("gatherFractionalDropPositions"),
                "the cell-edge native drop negative control remains in the runtime suite");
    }

    @Test void nativeReloadUsesABoundedRealTimeBudgetWithoutAnotherTicket() throws IOException {
        String wait = source("GameTestChunkAwait.java");
        assertTrue(wait.contains("System.nanoTime()") && wait.contains("Duration.ofSeconds(10)"),
                "asynchronous disk/light completion needs an explicit monotonic deadline");
        assertTrue(wait.contains("source.pollTask()") && wait.contains("source.getChunkNow("),
                "service the existing native load while observing FULL visibility");
        assertFalse(wait.contains("source.getChunk(") || wait.contains("source.getChunkFuture(")
                || wait.contains("addRegionTicket("), "awaiting must not acquire extra terrain or an UNKNOWN ticket");
        String furnace = source("AdvancedFurnaceUnloadRuntimeChecks.java");
        assertTrue(furnace.contains("GameTestChunkAwait.ticketed(test,coreChunk)")
                && furnace.contains("GameTestChunkAwait.ticketed(test,shellChunk)"));
        assertFalse(furnace.contains("polls<40") || furnace.contains("polls<200"),
                "do not reintroduce an accelerated-test-tick disk deadline");
        assertTrue(furnace.contains("testFailed(GameTestInfo") && furnace.contains("testAddedForRerun(GameTestInfo"),
                "native timeout/failure/rerun must close the fixture too");
    }

    @Test void theEmberCollisionFixtureDeclaresAnOrdinaryChampionJoinInput() throws IOException {
        String ember = source("EmberRuntimeChecks.java");
        assertTrue(ember.contains("ChampionJoinRoll.join(player.serverLevel(), target)"),
                "a random native champion starts at45/50 health and invalidates an exact-health fixture");
        assertTrue(ember.contains("ordinary collision target starts at full health and cold"),
                "establish the owned target precondition before the transaction under test");
    }
}

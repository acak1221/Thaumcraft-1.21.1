package thaumcraft.integration;

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
}

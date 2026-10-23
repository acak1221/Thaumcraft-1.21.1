package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsRules;

/**
 * The fidelity proof of the Outer Lands labyrinth.
 *
 * <p>The native {@link MazeGenerator} is compared with the reference generator of the
 * original TC4 4.2.3.5 ({@link OracleMazeGenerator}, a transcription of the oracle
 * {@code thaumcraft/common/lib/world/dim/MazeGenerator.java}) over the eight labyrinth sizes
 * the original altar draws and over forty seeds. The comparison is byte for byte: the walls
 * of a cell and its feature marker have to be the same in both generators.
 *
 * <p>The original skips a cell without a single exit and does not draw a random number for
 * it (oracle {@code MazeGenerator:332}). A port that draws for such a cell shifts the whole
 * random stream of the labyrinth and changes every room of every seed.
 */
class MazeFidelityTest {
    /** The eight labyrinth sizes of the original altar: fifteen plus a roll of eight times two. */
    private static final int[] SIZES = {15, 17, 19, 21, 23, 25, 27, 29};
    /** The number of seeds of the comparison. */
    private static final int SEEDS = 40;

    @Test void runtimeAuditSeedMatchesTheOracleIncludingItsOccupiedExtent() {
        var oracle = new OracleMazeGenerator(15, 15, 20261021L);
        var nativeMaze = new MazeGenerator(15, 15, 20261021L);
        assertTrue(oracle.generate());
        assertTrue(nativeMaze.generate());
        int occupied = 0, minX = 15, minZ = 15, maxX = 0, maxZ = 0;
        for (int z = 0; z < 15; z++) for (int x = 0; x < 15; x++) {
            assertEquals(oracle.grid[z][x], nativeMaze.grid[z][x], "audit cell " + x + "," + z);
            if (oracle.grid[z][x] <= 0) continue;
            occupied++;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }
        assertEquals(153, occupied);
        assertEquals(1, minX); assertEquals(14, maxX);
        assertEquals(0, minZ); assertEquals(13, maxZ);
        assertEquals(1, oracle.grid[8][8] >> 8);
        assertEquals(2, oracle.grid[0][13] >> 8);
    }

    @Test void theNativeLabyrinthIsByteIdenticalToTheOracle() {
        int cells = 0;
        int expectedCells = 0;
        int wallDifferences = 0;
        int featureDifferences = 0;
        int differences = 0;
        String first = null;
        for (int size : SIZES) {
            for (long seed = 0; seed < SEEDS; ++seed) {
                OracleMazeGenerator oracle = new OracleMazeGenerator(size, size, seed);
                MazeGenerator port = new MazeGenerator(size, size, seed);
                boolean oracleReady = oracle.generate();
                boolean portReady = port.generate();
                assertEquals(oracleReady, portReady,
                        "the labyrinth of the size " + size + " and the seed " + seed + " completes in both generators");
                if (!oracleReady) continue;
                expectedCells += size * size;
                for (int y = 0; y < size; ++y) {
                    for (int x = 0; x < size; ++x) {
                        int expected = oracle.grid[y][x];
                        int actual = port.grid[y][x];
                        ++cells;
                        if ((expected & 0xFF) != (actual & 0xFF)) ++wallDifferences;
                        if ((expected >> 8) != (actual >> 8)) ++featureDifferences;
                        if (expected == actual) continue;
                        ++differences;
                        if (first == null) {
                            first = "the size " + size + " and the seed " + seed + " the cell " + x + "/" + y
                                    + ": the oracle has 0x" + Integer.toHexString(expected)
                                    + " and the port has 0x" + Integer.toHexString(actual);
                        }
                    }
                }
            }
        }
        assertEquals(expectedCells, cells, "the comparison covered every cell of the eight sizes and the forty seeds");
        assertEquals(0, wallDifferences, "the walls of every cell are the same, " + wallDifferences + " of the "
                + cells + " cells differ, the first difference: " + first);
        assertEquals(0, featureDifferences, "the feature marker of every cell is the same, " + featureDifferences
                + " of the " + cells + " cells differ, the first difference: " + first);
        assertEquals(0, differences, "the labyrinth of the port is byte identical to the oracle, " + differences
                + " of the " + cells + " cells differ, the first difference: " + first);
    }

    @Test void aCellWithoutAnExitNeverCarriesARoomFeature() {
        // Oracle MazeGenerator:332 - a cell without a single exit is skipped without drawing a
        // random number, so it never receives one of the room features of the last draw. The
        // only cells without an exit that carry a feature are the four quadrants of the boss
        // room, which the labyrinth places before that draw.
        for (int size : SIZES) {
            for (long seed = 0; seed < SEEDS; ++seed) {
                MazeGenerator generator = new MazeGenerator(size, size, seed);
                if (!generator.generate()) continue;
                for (int y = 0; y < size; ++y) {
                    for (int x = 0; x < size; ++x) {
                        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) generator.grid[y][x]);
                        if (cell.exits() != 0) continue;
                        boolean bossQuadrant = OuterLandsRules.isBossQuadrant(cell.feature);
                        assertTrue(cell.feature == 0 || bossQuadrant,
                                "the cell " + x + "/" + y + " of the size " + size + " and the seed " + seed
                                        + " has no exit and carries the feature " + cell.feature);
                    }
                }
            }
        }
    }

    @Test void everyLabyrinthHoldsTheOriginalMarkers() {
        for (int size : SIZES) {
            for (long seed = 0; seed < SEEDS; ++seed) {
                OracleMazeGenerator oracle = new OracleMazeGenerator(size, size, seed);
                MazeGenerator port = new MazeGenerator(size, size, seed);
                if (!oracle.generate() || !port.generate()) continue;
                int portal = 0;
                int boss = 0;
                int keyRoom = 0;
                for (int y = 0; y < size; ++y) {
                    for (int x = 0; x < size; ++x) {
                        int feature = new MazeGenerator.Cell((short) port.grid[y][x]).feature;
                        if (feature == OuterLandsRules.FEATURE_PORTAL) ++portal;
                        if (OuterLandsRules.isBossQuadrant(feature)) ++boss;
                        if (feature == OuterLandsRules.FEATURE_KEY_ROOM) ++keyRoom;
                    }
                }
                assertEquals(1, portal, "the labyrinth of the size " + size + " and the seed " + seed + " holds one portal");
                assertEquals(4, boss, "the labyrinth of the size " + size + " and the seed " + seed + " holds four boss quadrants");
                assertEquals(1, keyRoom, "the labyrinth of the size " + size + " and the seed " + seed + " holds one key room");
            }
        }
    }
}

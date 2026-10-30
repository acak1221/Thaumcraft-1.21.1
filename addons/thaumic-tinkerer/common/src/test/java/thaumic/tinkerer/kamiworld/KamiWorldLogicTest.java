/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): JUnit tests of the plain rules of the KamiWorld group. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.kamiworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.core.handler.kami.SoulHeartHandler;
import thaumic.tinkerer.common.dim.EnumOreFrequency;
import thaumic.tinkerer.common.dim.OreClusterGenerator;
import thaumic.tinkerer.common.dim.TeleporterBedrock;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The pure rules of the KamiWorld group: the ore table and the weighted pick of the Bedrock dimension, the ore veins
 * and clusters (the 1.7.10 {@code WorldGenMinable} loop and the order of the random draws), the Soul Heart arithmetic
 * (with the quirks of the original) and the scan for the ground of the Overworld. Nothing here needs a world.</p>
 */
class KamiWorldLogicTest {

    // ---- the ore table -------------------------------------------------------------------------------------------

    @Test
    void theOreTableIsTheOriginalOne() {
        assertEquals(33, EnumOreFrequency.values().length);
        int sum = 0;
        for (EnumOreFrequency e : EnumOreFrequency.values()) sum += e.freq;
        assertEquals(11389, sum, "the weights of 1.7.10 add up to 11389");
        assertEquals(2648, EnumOreFrequency.COAL.freq);
        assertEquals("oreCoal", EnumOreFrequency.COAL.name);
        assertEquals(1503, EnumOreFrequency.IRON.freq);
        assertEquals(48, EnumOreFrequency.EMERALD.freq);
        assertEquals(27, EnumOreFrequency.WATER.freq);
        assertEquals("oreInfusedWater", EnumOreFrequency.WATER.name);
        assertEquals(1024, EnumOreFrequency.CHEESE.freq);
        assertEquals("oreNetherQuartz", EnumOreFrequency.NETHERQUARTZ.name);
    }

    @Test
    void thePickWalksTheWeightsInOrderAfterOneDraw() {
        int[] weights = { 5, 3, 2 };
        for (long seed = 0; seed < 200; seed++) {
            int draw = new Random(seed).nextInt(10);
            int expected = draw < 5 ? 0 : draw < 8 ? 1 : 2;
            assertEquals(expected, EnumOreFrequency.pickIndex(weights, new Random(seed)), "seed " + seed);
        }
    }

    @Test
    void thePickUsesExactlyOneDraw() {
        Random used = new Random(99);
        EnumOreFrequency.pickIndex(new int[] { 1, 2, 3 }, used);
        Random reference = new Random(99);
        reference.nextInt(6);
        assertEquals(reference.nextLong(), used.nextLong());
    }

    @Test
    void thePickOfNoWeightsIsMinusOne() {
        assertEquals(-1, EnumOreFrequency.pickIndex(new int[0], new Random(1)));
        assertEquals(-1, EnumOreFrequency.pickIndex(new int[] { 0, 0 }, new Random(1)));
    }

    @Test
    void thePickFollowsTheWeights() {
        int[] weights = Arrays.stream(EnumOreFrequency.values()).mapToInt(e -> e.freq).toArray();
        int total = Arrays.stream(weights).sum();
        int draws = 400_000;
        int[] counts = new int[weights.length];
        Random random = new Random(20140331L);
        for (int i = 0; i < draws; i++) counts[EnumOreFrequency.pickIndex(weights, random)]++;
        for (int i = 0; i < weights.length; i++) {
            double expected = (double) draws * weights[i] / total;
            double tolerance = 5 * Math.sqrt(expected) + 2;
            assertEquals(expected, counts[i], tolerance, EnumOreFrequency.values()[i] + " is drawn in proportion to its weight");
        }
    }

    // ---- veins ---------------------------------------------------------------------------------------------------

    /** A target that accepts every position and records what was placed. */
    private static final class Recorder implements OreClusterGenerator.VeinTarget {
        final Set<Long> placed = new HashSet<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE,
                minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        @Override
        public boolean isReplaceable(int x, int y, int z) {
            return !placed.contains(key(x, y, z));
        }

        @Override
        public void place(int x, int y, int z) {
            placed.add(key(x, y, z));
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }

        static long key(int x, int y, int z) {
            return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
        }
    }

    @Test
    void aVeinOfSizeZeroPlacesNothing() {
        for (long seed = 0; seed < 50; seed++) {
            Recorder target = new Recorder();
            OreClusterGenerator.generateVein(new Random(seed), 0, 32, 100, 48, target);
            assertTrue(target.placed.isEmpty(), "size 0, seed " + seed);
        }
    }

    @Test
    void aVeinIsDeterministicForASeed() {
        Recorder a = new Recorder();
        Recorder b = new Recorder();
        OreClusterGenerator.generateVein(new Random(5), 17, 16, 80, 16, a);
        OreClusterGenerator.generateVein(new Random(5), 17, 16, 80, 16, b);
        assertEquals(a.placed, b.placed);
        Recorder c = new Recorder();
        OreClusterGenerator.generateVein(new Random(6), 17, 16, 80, 16, c);
        assertFalse(a.placed.equals(c.placed), "another seed makes another vein");
    }

    @Test
    void aVeinStaysWithinFourBlocksOfTheCentreAndTheChunkAndItsEastAndSouthNeighbours() {
        for (long seed = 0; seed < 3000; seed++) {
            Random random = new Random(seed);
            int chunkX = random.nextInt(2000) - 1000;
            int chunkZ = random.nextInt(2000) - 1000;
            int x = 16 * chunkX + random.nextInt(16);
            int z = 16 * chunkZ + random.nextInt(16);
            int y = random.nextInt(OreClusterGenerator.VEIN_Y_RANGE) + OreClusterGenerator.MIN_VEIN_Y;
            int size = random.nextInt(OreClusterGenerator.MAX_VEIN_SIZE);
            Recorder target = new Recorder();
            OreClusterGenerator.generateVein(random, size, x, y, z, target);
            if (target.placed.isEmpty()) continue;
            // The centre of the line is at x + 8; the line reaches size / 8 to each side and a block has a radius under 2.
            assertTrue(target.minX >= x + 8 - 5 && target.maxX <= x + 8 + 5, "x of seed " + seed);
            assertTrue(target.minZ >= z + 8 - 5 && target.maxZ <= z + 8 + 5, "z of seed " + seed);
            assertTrue(target.minY >= y - 4 && target.maxY <= y + 2, "y of seed " + seed);
            for (int dx : new int[] { target.minX, target.maxX }) {
                for (int dz : new int[] { target.minZ, target.maxZ }) {
                    assertTrue(OreClusterGenerator.reaches(chunkX, chunkZ, dx, dz), "the written area of the chunk covers the vein, seed " + seed);
                }
            }
        }
    }

    @Test
    void theWrittenAreaIsTheChunkAndTheOneEastAndSouth() {
        assertTrue(OreClusterGenerator.reaches(3, -2, 48, -32));
        assertTrue(OreClusterGenerator.reaches(3, -2, 79, -17));
        assertFalse(OreClusterGenerator.reaches(3, -2, 47, -32), "west of the chunk");
        assertFalse(OreClusterGenerator.reaches(3, -2, 48, -33), "north of the chunk");
        assertFalse(OreClusterGenerator.reaches(3, -2, 80, -32), "two chunks east");
        assertTrue(OreClusterGenerator.reaches(3, -2, 48, -16), "the chunk south of it (z -16 is chunk -1)");
        assertFalse(OreClusterGenerator.reaches(3, -2, 48, 0), "two chunks south");
        assertTrue(OreClusterGenerator.reaches(-1, -1, -16, -16), "negative coordinates round down");
        assertFalse(OreClusterGenerator.reaches(-1, -1, -17, -16));
    }

    @Test
    void aLargeVeinPlacesSeveralBlocksAndNeverMoreThanItsEllipsoidsHold() {
        long total = 0;
        int veins = 400;
        for (long seed = 0; seed < veins; seed++) {
            Recorder target = new Recorder();
            OreClusterGenerator.generateVein(new Random(seed), 19, 0, 100, 0, target);
            assertTrue(target.placed.size() <= 20 * 27, "a vein of 19 is a chain of 20 small ellipsoids, seed " + seed);
            total += target.placed.size();
        }
        double average = (double) total / veins;
        assertTrue(average > 8 && average < 120, "an average vein of size 19 has about a few dozen blocks: " + average);
    }

    @Test
    void aVeinOnlyPlacesWhereTheTargetAllows() {
        final Set<Long> placed = new HashSet<>();
        OreClusterGenerator.VeinTarget onlyEvenX = new OreClusterGenerator.VeinTarget() {
            @Override
            public boolean isReplaceable(int x, int y, int z) {
                return (x & 1) == 0;
            }

            @Override
            public void place(int x, int y, int z) {
                assertTrue((x & 1) == 0, "placed at an odd x");
                placed.add(Recorder.key(x, y, z));
            }
        };
        OreClusterGenerator.generateVein(new Random(3), 19, 0, 60, 0, onlyEvenX);
        assertFalse(placed.isEmpty());
    }

    // ---- clusters ------------------------------------------------------------------------------------------------

    @Test
    void theChunkRandomIsSeededLikeTheOriginalPopulate() {
        long seed = 8675309L;
        Random expected = new Random(seed);
        long i1 = expected.nextLong() / 2L * 2L + 1L;
        long j1 = expected.nextLong() / 2L * 2L + 1L;
        expected.setSeed(4L * i1 + (-7L) * j1 ^ seed);
        Random actual = OreClusterGenerator.chunkRandom(seed, 4, -7);
        for (int i = 0; i < 20; i++) assertEquals(expected.nextInt(), actual.nextInt());
        assertFalse(OreClusterGenerator.chunkRandom(seed, 4, -7).nextLong() == OreClusterGenerator.chunkRandom(seed, 5, -7).nextLong(),
                "neighbouring chunks have different randoms");
    }

    @Test
    void theClustersDrawInTheOrderOfTheOriginal() {
        // Reference: the 1.7.10 loop written out with the same draws.
        Recorder expected = new Recorder();
        Random reference = new Random(42);
        for (int k = 0; k < 2; k++) {
            int x = 16 * 5 + reference.nextInt(16);
            int z = 16 * -3 + reference.nextInt(16);
            reference.nextInt(100); // the pick of the ore (one draw)
            for (int l = 0; l < 200; l++) {
                int y = reference.nextInt(245) + 6;
                int size = reference.nextInt(20);
                OreClusterGenerator.generateVein(reference, size, x, y, z, expected);
            }
        }
        final Recorder actual = new Recorder();
        final List<String> ores = new ArrayList<>();
        OreClusterGenerator.generate(new Random(42), 5, -3, 2, rnd -> {
            rnd.nextInt(100);
            return "ore" + ores.size();
        }, new OreClusterGenerator.ClusterWorld<String>() {
            @Override
            public boolean isReplaceable(int x, int y, int z) {
                return actual.isReplaceable(x, y, z);
            }

            @Override
            public void place(int x, int y, int z, String ore) {
                if (!ores.contains(ore)) ores.add(ore);
                actual.place(x, y, z);
            }
        });
        assertEquals(expected.placed, actual.placed);
        assertTrue(actual.placed.size() > 100, "two clusters of 200 veins place plenty: " + actual.placed.size());
        assertEquals(2, ores.size(), "one ore per cluster");
    }

    @Test
    void theClustersStayBetweenHeightTwoAndTwoHundredFiftyOne() {
        Recorder all = new Recorder();
        OreClusterGenerator.generate(new Random(7), 0, 0, 5, rnd -> "ore", new OreClusterGenerator.ClusterWorld<String>() {
            @Override
            public boolean isReplaceable(int x, int y, int z) {
                return true;
            }

            @Override
            public void place(int x, int y, int z, String ore) {
                all.place(x, y, z);
            }
        });
        assertTrue(all.minY >= 2, "lowest " + all.minY);
        assertTrue(all.maxY <= 251, "highest " + all.maxY);
        assertTrue(all.minX >= 8 - 5 && all.maxX <= 15 + 8 + 5);
    }

    @Test
    void aDensityOfZeroOrAnUnpickableOreMakesNoCluster() {
        final int[] placed = { 0 };
        OreClusterGenerator.ClusterWorld<String> world = new OreClusterGenerator.ClusterWorld<String>() {
            @Override
            public boolean isReplaceable(int x, int y, int z) {
                return true;
            }

            @Override
            public void place(int x, int y, int z, String ore) {
                placed[0]++;
            }
        };
        OreClusterGenerator.generate(new Random(1), 0, 0, 0, rnd -> "ore", world);
        assertEquals(0, placed[0]);
        OreClusterGenerator.generate(new Random(1), 0, 0, 3, rnd -> null, world);
        assertEquals(0, placed[0], "no valid ore: the cluster is skipped (1.7.10 crashed)");
    }

    // ---- the Soul Hearts -----------------------------------------------------------------------------------------

    @Test
    void thePoolFillsByOnePointUpToTwenty() {
        assertEquals(20, SoulHeartHandler.MAX_HP);
        assertEquals(1, SoulHeartHandler.afterAdd(0, 1));
        assertEquals(20, SoulHeartHandler.afterAdd(19, 5), "clamped to the maximum");
        assertEquals(20, SoulHeartHandler.afterAdd(20, 1), "a full pool does not change");
        assertEquals(25, SoulHeartHandler.afterAdd(25, 1), "a value above the maximum (from a file) is left alone, as addHP's early return did");
    }

    @Test
    void thePoolIsSpentBeforeHealthAndNeverGoesBelowZero() {
        assertEquals(0, SoulHeartHandler.afterRemove(3, 5));
        assertEquals(2, SoulHeartHandler.afterRemove(5, 3));
        assertEquals(0, SoulHeartHandler.overflow(5, 3), "covered");
        assertEquals(0, SoulHeartHandler.overflow(5, 5), "exactly covered");
        assertEquals(2, SoulHeartHandler.overflow(3, 5), "two points of damage go on");
        assertEquals(7, SoulHeartHandler.overflow(0, 7), "no hearts: the damage goes on whole");
    }

    @Test
    void aHitIsReducedByItsWholePointsAndTheFractionIsLost() {
        // 1.7.10: event.ammount = removeHP(player, (int) event.ammount): an int, whatever the hit was.
        assertEquals(0, SoulHeartHandler.damageAfterPool(10, 4.9F));
        assertEquals(1, SoulHeartHandler.damageAfterPool(3, 4.9F), "4 points off a pool of 3: one goes on");
        assertEquals(0, SoulHeartHandler.damageAfterPool(0, 0.9F), "a hit below one point is swallowed even with no hearts");
        assertEquals(6, SoulHeartHandler.damageAfterPool(0, 6.99F));
    }

    @Test
    void theHitThatIsDealtAgainIsUnscaledByTheDifficulty() {
        for (int rest = 1; rest <= 40; rest++) {
            // Easy: min(x / 2 + 1, x); Hard: x * 3 / 2; Normal: x. After the player's own scaling the rest must come back.
            float easy = SoulHeartHandler.beforeDifficultyScaling(rest, true, Difficulty.EASY);
            assertEquals(rest, Math.min(easy / 2F + 1F, easy), 1e-4F);
            float hard = SoulHeartHandler.beforeDifficultyScaling(rest, true, Difficulty.HARD);
            assertEquals(rest, hard * 3F / 2F, 1e-4F);
            assertEquals(rest, SoulHeartHandler.beforeDifficultyScaling(rest, true, Difficulty.NORMAL), 0F);
            assertEquals(rest, SoulHeartHandler.beforeDifficultyScaling(rest, false, Difficulty.HARD), 0F, "a hit that does not scale");
        }
    }

    // ---- where the portal leads ----------------------------------------------------------------------------------

    @Test
    void theEntranceOfTheBedrockDimensionIsTheOriginalColumn() {
        assertEquals(251, TeleporterBedrock.ENTRANCE_Y);
        assertEquals(249, TeleporterBedrock.FLOOR_Y);
        assertEquals(250, TeleporterBedrock.CAVITY_MIN_Y);
        assertEquals(253, TeleporterBedrock.CAVITY_MAX_Y);
        assertEquals(254, TeleporterBedrock.EXIT_Y);
        assertEquals(120, TeleporterBedrock.SURFACE_START_Y);
        assertEquals(50, TeleporterBedrock.SPREAD);
    }

    @Test
    void theScanStopsAtTheFirstSolidBlockBelowTheStart() {
        Set<Integer> placed = new HashSet<>();
        int y = TeleporterBedrock.scanDown(h -> h > 70, 120, 1, placed::add);
        assertEquals(70, y);
        assertTrue(placed.isEmpty(), "no scaffold on solid ground");
    }

    @Test
    void aColumnOfAirGetsAStoneAtOneNineteenAndTheScanStopsOnIt() {
        Set<Integer> stone = new HashSet<>();
        int y = TeleporterBedrock.scanDown(h -> !stone.contains(h), 120, 1, stone::add);
        assertEquals(119, y);
        assertEquals(Set.of(119), stone);
    }

    @Test
    void aStartThatIsNotAirIsTheAnswerAtOnce() {
        assertEquals(120, TeleporterBedrock.scanDown(h -> false, 120, 1, h -> {
            throw new AssertionError("no scaffold");
        }));
    }

    @Test
    void aScaffoldThatCannotBePlacedDoesNotLoopForEver() {
        assertEquals(120, TeleporterBedrock.scanDown(h -> true, 120, 1, h -> {}));
    }

    @Test
    void theScanReachesTheFloorOfAHighTerrain() {
        // Ground at 2: the scan goes down to it and stops (the floor is 1).
        assertEquals(2, TeleporterBedrock.scanDown(h -> h > 2, 120, 1, h -> {
            throw new AssertionError("no scaffold");
        }));
    }
}

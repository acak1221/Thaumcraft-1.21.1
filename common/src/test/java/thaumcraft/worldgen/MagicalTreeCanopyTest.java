package thaumcraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.WorldGenBigMagicTree;
import thaumcraft.common.lib.world.WorldGenGreatwoodTrees;

/**
 * The canopy geometry of the two TC4 trees that own their own leaf node list.
 *
 * <p>Every expectation is transcribed from the readable 1.7.10 reference
 * {@code /home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/lib/world}:
 * <ul>
 *   <li>{@code WorldGenBigMagicTree:23} - {@code double leafDensity = 1.0F},</li>
 *   <li>{@code WorldGenBigMagicTree:39} - {@code (int)(1.382 + Math.pow(this.leafDensity
 *       * (double)this.heightLimit / (double)13.0F, 2.0F))},</li>
 *   <li>{@code WorldGenBigMagicTree:354} - {@code this.heightLimit = 11 + this.rand.nextInt(12)},</li>
 *   <li>{@code WorldGenGreatwoodTrees:29} - {@code double leafDensity = 0.9},</li>
 *   <li>{@code WorldGenGreatwoodTrees:45} - the same formula,</li>
 *   <li>{@code WorldGenGreatwoodTrees:355} - {@code this.heightLimit = 11 + this.rand.nextInt(11)}.</li>
 * </ul>
 *
 * <p>The factor {@code 0.8} of {@code setScale} ({@code WorldGenBigMagicTree:343}) is the
 * removed 1.6 tree scale and never applies: the only construction of the original tree is
 * {@code new WorldGenBigMagicTree(false)}, which never calls {@code setScale}.
 */
class MagicalTreeCanopyTest {

    /** The original big magic tree leaf node count for every height it can draw. */
    private static final int[] BIG_MAGIC_EXPECTED = {
            /* heightLimit 11 */ 2, /* 12 */ 2, /* 13 */ 2, /* 14 */ 2, /* 15 */ 2, /* 16 */ 2,
            /* 17 */ 3, /* 18 */ 3, /* 19 */ 3, /* 20 */ 3, /* 21 */ 3, /* 22 */ 4
    };

    /** The original greatwood leaf node count for every height it can draw. */
    private static final int[] GREATWOOD_EXPECTED = {
            /* heightLimit 11 */ 1, /* 12 */ 2, /* 13 */ 2, /* 14 */ 2, /* 15 */ 2, /* 16 */ 2,
            /* 17 */ 2, /* 18 */ 2, /* 19 */ 3, /* 20 */ 3, /* 21 */ 3
    };

    @Test void theBigMagicTreeKeepsTheOriginalLeafDensity() {
        for (int height = 11; height <= 22; height++) {
            assertEquals(BIG_MAGIC_EXPECTED[height - 11], WorldGenBigMagicTree.leafNodeCount(height),
                    "big magic tree leaf nodes at height " + height);
        }
    }

    @Test void theBigMagicTreeKeepsTheOriginalHeightRange() {
        // Oracle WorldGenBigMagicTree:354 - 11 + nextInt(12).
        assertEquals(11, WorldGenBigMagicTree.MIN_HEIGHT_LIMIT);
        assertEquals(12, WorldGenBigMagicTree.HEIGHT_LIMIT_ROLLS);
        assertEquals(22, WorldGenBigMagicTree.MAX_HEIGHT_LIMIT);
    }

    @Test void theGreatwoodKeepsItsOwnHeightRange() {
        // Oracle WorldGenGreatwoodTrees:355 - 11 + nextInt(11).
        assertEquals(11, WorldGenGreatwoodTrees.MIN_HEIGHT_LIMIT);
        assertEquals(11, WorldGenGreatwoodTrees.HEIGHT_LIMIT_ROLLS);
        assertEquals(21, WorldGenGreatwoodTrees.MAX_HEIGHT_LIMIT);
    }

    @Test void theGreatwoodKeepsItsOwnLeafDensity() {
        for (int height = 11; height <= 21; height++) {
            assertEquals(GREATWOOD_EXPECTED[height - 11], WorldGenGreatwoodTrees.leafNodeCount(height),
                    "greatwood leaf nodes at height " + height);
        }
    }

    @Test void theTwoTreesDoNotShareTheirLeafDensity() {
        // The greatwood really uses 0.9 (WorldGenGreatwoodTrees:29), the big magic tree 1.0
        // (WorldGenBigMagicTree:23). A shared factor would change one of the two canopies.
        // The two formulas agree on some heights; they must not be derived from one constant.
        int[] different = {11, 17, 18, 22};
        for (int height : different) {
            assertTrue(WorldGenBigMagicTree.leafNodeCount(height)
                            != WorldGenGreatwoodTrees.leafNodeCount(height),
                    "the two leaf densities must differ at height " + height);
        }
        // The big magic tree reaches one more leaf node at the top of its height range.
        assertEquals(4, WorldGenBigMagicTree.leafNodeCount(22));
        assertEquals(3, WorldGenGreatwoodTrees.leafNodeCount(22));
    }
}

package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The eldritch stairs of the original take the icon of the ancient stone of the cosmetic solid,
 * and the ancient stone draws that icon from the hash of the position.
 *
 * <p>Oracle {@code thaumcraft/common/blocks/BlockCosmeticSolid.java:157-162}:</p>
 * <pre>
 * if (md == 11 || md == 13 || side &gt;= 100) {
 *     String l = x + "" + y + "" + z;
 *     Random r1 = new Random(Math.abs(l.hashCode() * 100) + 1);
 *     int i = r1.nextInt(12345 + side) % 4;
 *     return this.icon[17 + i];
 * }
 * </pre>
 *
 * <p>The cosmetic stairs reach that branch with the side index plus a hundred
 * ({@code oracle thaumcraft/common/blocks/BlockCosmeticStairs.java:38-40}), so the bound of
 * the draw of a stair is {@code 12445 + side}. The face vectors below are frozen evaluations
 * of the oracle with Java-compatible signed-int/hash/48-bit RNG semantics; no expected
 * algorithm runs in this test. The audit added vectors using an independent Python evaluator,
 * not by calling the port.</p>
 *
 * <p>No test of this file constructs a block: the rule under test is a pure static function.</p>
 */
class CosmeticStairsTextureParityTest {
    @Test void originalFacesAlreadyDifferAtTheOrigin() {
        // This is not a one-in-12000 discrepancy: nextInt's bound changes the modulus.
        int[] expected = {3, 0, 1, 2, 3, 0};
        for (int side = 0; side < expected.length; side++) {
            assertEquals(expected[side], BlockCosmeticStairs.textureIndex(0, 0, 0, side));
        }
    }

    @Test void theTextureIndexIsTheDrawOfTheOriginal() {
        // The up face of the stair: the side index one plus the hundred of the stairs.
        assertEquals(0, BlockCosmeticStairs.textureIndex(0, 0, 0));
        assertEquals(3, BlockCosmeticStairs.textureIndex(1, 2, 3));
        assertEquals(1, BlockCosmeticStairs.textureIndex(-5, 64, 12));
        assertEquals(1, BlockCosmeticStairs.textureIndex(100, -60, 77));
    }

    @Test void theTextureIndexMatchesTheOracleForEverySide() {
        // Frozen outputs of oracle BlockCosmeticSolid:157-162, reached with side+100
        // from BlockCosmeticStairs:38-40; no local copy of the hash/RNG algorithm.
        int[] origin = {3, 0, 1, 2, 3, 0};
        int[] remote = {3, 2, 2, 0, 1, 0};
        for (int side = 0; side < 6; ++side) {
            assertEquals(origin[side], BlockCosmeticStairs.textureIndex(0, 0, 0, side), "side " + side);
            assertEquals(remote[side], BlockCosmeticStairs.textureIndex(-17, 71, 240, side), "side " + side);
        }
    }

    @Test void coordinateHashingSelectsTheOracleFaceVectors() {
        // Exercise production output, not a seed helper that production never calls.
        int[][] positions = {{0, 0, 0}, {1, 2, 3}, {-5, 64, 12}, {100, -60, 77}};
        int[][] faces = {{3, 0, 1, 2, 3, 0}, {0, 3, 1, 1, 2, 1},
                {0, 1, 0, 1, 0, 1}, {3, 1, 2, 1, 0, 3}};
        for (int i = 0; i < positions.length; i++) {
            int[] pos = positions[i];
            for (int side = 0; side < 6; side++) {
                assertEquals(faces[i][side], BlockCosmeticStairs.textureIndex(pos[0], pos[1], pos[2], side));
            }
        }
    }

    @Test void theTextureIndexIsInTheRangeOfTheFourTextures() {
        for (int x = -3; x <= 3; ++x) {
            for (int y = 60; y <= 66; ++y) {
                for (int z = -3; z <= 3; ++z) {
                    int index = BlockCosmeticStairs.textureIndex(x, y, z);
                    assertTrue(index >= 0 && index < 4, "index " + index + " at " + x + "," + y + "," + z);
                }
            }
        }
    }

    /**
     * The hash of a position gives four textures, so two neighbours may collide. The oracle
     * itself collides at (10,64,10) and (11,64,10) - both draw index 2 - so the pair below is the
     * one measured to differ with the formula of the oracle, not a pair chosen by eye.
     */
    @Test void theTextureIndexDependsOnThePositionAndIsStable() {
        assertNotEquals(BlockCosmeticStairs.textureIndex(-20, 64, -5), BlockCosmeticStairs.textureIndex(-19, 64, -5),
                "two stairs of the same kind look different");
        assertEquals(3, BlockCosmeticStairs.textureIndex(-20, 64, -5),
                "the texture of the left stair is the draw of the original");
        assertEquals(1, BlockCosmeticStairs.textureIndex(-19, 64, -5),
                "the texture of the right stair is the draw of the original");
        assertEquals(2, BlockCosmeticStairs.textureIndex(10, 64, 10));
        BlockCosmeticStairs.textureIndex(-17, 71, 240);
        assertEquals(2, BlockCosmeticStairs.textureIndex(10, 64, 10),
                "an intervening lookup must not alter the texture");
    }

    @Test void theStateCarriesTheFourTextures() {
        assertEquals("texture", BlockCosmeticStairs.TEXTURE.getName());
        assertEquals(4, BlockCosmeticStairs.TEXTURE.getPossibleValues().size());
        assertEquals(4, BlockCosmeticStairs.TEXTURE_COUNT);
        assertEquals(1, BlockCosmeticStairs.SIDE_UP);
        assertEquals(0, BlockCosmeticStairs.SIDE_DOWN);
    }
}

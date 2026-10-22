package thaumcraft.common.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
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
 * the draw of a stair is {@code 12445 + side}. The expected values below were measured on the
 * JVM with the formula of the oracle, they are not a transcription of the port.</p>
 *
 * <p>No test of this file constructs a block: the rule under test is a pure static function.</p>
 */
class CosmeticStairsTextureParityTest {
    /** The oracle rule of BlockCosmeticSolid:157-162 as it is reached by the stairs. */
    private static int oracleIndex(int x, int y, int z, int side) {
        String key = x + "" + y + "" + z;
        Random random = new Random(Math.abs(key.hashCode() * 100) + 1);
        return random.nextInt(12345 + 100 + side) % 4;
    }

    /** The seed of the original: the hash of the concatenated coordinates times a hundred plus one. */
    private static long oracleSeed(int x, int y, int z) {
        String key = x + "" + y + "" + z;
        return Math.abs(key.hashCode() * 100) + 1;
    }

    @Test void theTextureIndexIsTheDrawOfTheOriginal() {
        // The up face of the stair: the side index one plus the hundred of the stairs.
        assertEquals(0, BlockCosmeticStairs.textureIndex(0, 0, 0));
        assertEquals(3, BlockCosmeticStairs.textureIndex(1, 2, 3));
        assertEquals(1, BlockCosmeticStairs.textureIndex(-5, 64, 12));
        assertEquals(1, BlockCosmeticStairs.textureIndex(100, -60, 77));
    }

    @Test void theTextureIndexMatchesTheOracleForEverySide() {
        for (int side = 0; side < 6; ++side) {
            assertEquals(oracleIndex(0, 0, 0, side), BlockCosmeticStairs.textureIndex(0, 0, 0, side), "side " + side);
            assertEquals(oracleIndex(-17, 71, 240, side), BlockCosmeticStairs.textureIndex(-17, 71, 240, side), "side " + side);
        }
    }

    @Test void theSeedIsTheHashOfTheConcatenatedCoordinates() {
        assertEquals(4766401L, oracleSeed(0, 0, 0));
        assertEquals(4869001L, oracleSeed(1, 2, 3));
        assertEquals(747880125L, oracleSeed(-5, 64, 12));
        assertEquals(793276953L, oracleSeed(100, -60, 77));
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
        assertEquals(oracleIndex(-20, 64, -5, 1), BlockCosmeticStairs.textureIndex(-20, 64, -5),
                "the texture of the left stair is the draw of the original");
        assertEquals(oracleIndex(-19, 64, -5, 1), BlockCosmeticStairs.textureIndex(-19, 64, -5),
                "the texture of the right stair is the draw of the original");
        assertEquals(BlockCosmeticStairs.textureIndex(10, 64, 10), BlockCosmeticStairs.textureIndex(10, 64, 10),
                "the texture of a stair is stable");
    }

    @Test void theStateCarriesTheFourTextures() {
        assertEquals("texture", BlockCosmeticStairs.TEXTURE.getName());
        assertEquals(4, BlockCosmeticStairs.TEXTURE.getPossibleValues().size());
        assertEquals(4, BlockCosmeticStairs.TEXTURE_COUNT);
        assertEquals(1, BlockCosmeticStairs.SIDE_UP);
        assertEquals(0, BlockCosmeticStairs.SIDE_DOWN);
    }
}

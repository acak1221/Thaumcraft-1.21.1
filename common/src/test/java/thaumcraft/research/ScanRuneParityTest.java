package thaumcraft.research;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ScanRunes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rune particles of the original thaumometer scan of TC4 4.2.3.5.
 *
 * <p>Oracle {@code thaumcraft/common/items/relics/ItemThaumometer.java}:
 * {@code :92} draws the rune over the pointed entity,
 * {@code :103} over a node and {@code :128} over a block. All three go through
 * {@code Thaumcraft.proxy.blockRunes}, whose client half is
 * {@code thaumcraft/client/ClientProxy.java:918-922} and creates exactly ONE
 * {@code FXBlockRunes} at {@code (x + 0.5, y + 0.5, z + 0.5)} of the given point.</p>
 */
class ScanRuneParityTest {
    @Test void theOriginalRuneOfABlockSitsAQuarterBlockAboveIt() {
        var rune = ScanRunes.block(12, 70, -3);
        assertEquals(12.0D, rune.x());
        assertEquals(70.25D, rune.y());
        assertEquals(-3.0D, rune.z());
        assertEquals(15, rune.duration());
        assertEquals(0.03F, rune.gravity());
    }

    @Test void theOriginalRuneOfAnEntityIsCentredOnItsEyeHeight() {
        // The original reads the eye height func_70047_e() for the position and the
        // height field field_70131_O for the duration.
        var rune = ScanRunes.entity(10.5D, 64.0D, 20.25D, 1.62D, 1.8D);
        assertEquals(10.0D, rune.x());
        assertEquals(64.81D, rune.y(), 1.0E-9D);
        assertEquals(19.75D, rune.z());
        assertEquals(27, rune.duration());
        assertEquals(0.03F, rune.gravity());
    }

    @Test void theOriginalRuneOfAnEntityLastsFifteenTicksPerBlockOfHeight() {
        assertEquals(0, ScanRunes.entity(0, 0, 0, 0, 0).duration());
        assertEquals(15, ScanRunes.entity(0, 0, 0, 0, 1).duration());
        assertEquals(13, ScanRunes.entity(0, 0, 0, 0, 0.9D).duration());
    }

    @Test void theOriginalParticleSitsHalfABlockAboveThePoint() {
        var rune = ScanRunes.block(1, 2, 3);
        assertEquals(1.5D, rune.particleX());
        assertEquals(2.75D, rune.particleY());
        assertEquals(3.5D, rune.particleZ());
    }

    @Test void theOriginalRuneColourIsRedAndBlueBetweenPointThreeAndOne() {
        var random = RandomSource.create(42L);
        for (int index = 0; index < 64; index++) {
            float[] colour = ScanRunes.colour(random);
            assertEquals(0.0F, colour[1]);
            assertTrue(colour[0] >= 0.3F && colour[0] <= 1.0F, "red " + colour[0]);
            assertTrue(colour[2] >= 0.3F && colour[2] <= 1.0F, "blue " + colour[2]);
        }
    }

    @Test void theOriginalRuneColourDrawsTwoValuesFromTheGivenRandom() {
        var random = RandomSource.create(7L);
        float[] first = ScanRunes.colour(random);
        float[] second = ScanRunes.colour(random);
        // Two independent draws per rune: the original reads the world random twice.
        assertTrue(first[0] != second[0] || first[2] != second[2]);
    }
}

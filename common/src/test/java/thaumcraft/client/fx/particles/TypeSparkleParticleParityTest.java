package thaumcraft.client.fx.particles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;

/**
 * The configurable sparkle of the original (TC4 4.2.3.5) against the 1.21.1 port.
 *
 * <p>The colours of the types are the ones of the original
 * ({@code thaumcraft/client/fx/particles/FXSparkle.java:50-105}); the function under test is a
 * pure function of the type and of the random, so no particle and no client is needed.</p>
 */
class TypeSparkleParticleParityTest {

    private static float[] colour(int type, long seed) {
        return TypeSparkleParticle.colour(type, new LegacyRandomSource(seed));
    }

    /**
     * The type two is the blue of the portable hole: {@code 0.2f, 0.2f, 0.7f + rand * 0.3f}.
     */
    @Test void theTypeTwoIsTheBlueOfThePortableHole() {
        float[] first = colour(2, 1L);
        assertEquals(.2F, first[0], 1.0E-6F, "FXSparkle:65 - the red of the type two is 0.2f");
        assertEquals(.2F, first[1], 1.0E-6F, "FXSparkle:66 - the green of the type two is 0.2f");
        assertTrue(first[2] >= .7F && first[2] <= 1.0F, "FXSparkle:67 - the blue of the type two is 0.7f + rand * 0.3f");
    }

    /** The type six is the white of the frost focus: {@code 0.8f + rand * 0.2f} on the three channels. */
    @Test void theTypeSixIsTheWhiteOfTheFrostFocus() {
        float[] first = colour(6, 7L);
        for (int channel = 0; channel < 3; channel++) {
            assertTrue(first[channel] >= .8F && first[channel] <= 1.0F,
                    "FXSparkle:93-97 - the type six is 0.8f + rand * 0.2f on every channel");
        }
    }

    /** The type zero is the green of the shimmerleaf and the type one the orange. */
    @Test void theGreenAndTheOrangeOfTheOriginalAreKept() {
        float[] green = colour(0, 3L);
        assertTrue(green[0] >= .75F && green[0] <= 1.0F, "FXSparkle:52 - the red of the type zero");
        assertTrue(green[1] >= .25F && green[1] <= .5F, "FXSparkle:53 - the green of the type zero");
        assertTrue(green[2] >= .75F && green[2] <= 1.0F, "FXSparkle:54 - the blue of the type zero");
        float[] orange = colour(1, 3L);
        assertTrue(orange[0] >= .5F && orange[0] <= .8F, "FXSparkle:58 - the red of the type one");
        assertTrue(orange[1] >= .5F && orange[1] <= .8F, "FXSparkle:59 - the green of the type one");
        assertEquals(.2F, orange[2], 1.0E-6F, "FXSparkle:60 - the blue of the type one is 0.2f");
    }

    /** The type five is the dark random grey of the original. */
    @Test void theTypeFiveIsTheDarkRandomGrey() {
        float[] first = colour(5, 11L);
        for (int channel = 0; channel < 3; channel++) {
            assertTrue(first[channel] >= 0F && first[channel] <= .1F,
                    "FXSparkle:80-84 - the type five is rand * 0.1f on every channel");
        }
    }
}

package thaumcraft.essentia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.CustomSoundType;

/**
 * The jar sound type of the 1.7.10 oracle against the native port.
 *
 * <p>Oracle {@code thaumcraft/common/blocks/BlockJar.java:88} gives the jar one custom sound type,
 * {@code this.func_149672_a(new CustomStepSound("jar", 1.0f, 1.0f))}, and
 * {@code thaumcraft/common/blocks/CustomStepSound.java} answers {@code thaumcraft:jar} for the step
 * sound and for the break sound. The base sound type gives the same name to the place, the fall and
 * the hit sound as well, so all five sounds of the jar are the sound {@code thaumcraft:jar}.</p>
 *
 * <p>The native port played the vanilla glass step sound instead, so the original audio of the jar
 * was lost. The native {@code CustomSoundType} resolved the sounds of the name plus the kind
 * ({@code thaumcraft:jar.step}) instead of the single name of the original, so it could not carry
 * the original sound either.</p>
 */
class JarSoundTypeTest {
    @Test void theJarSoundTypeCarriesTheOriginalSoundForAllFiveSounds() {
        CustomSoundType sound = new CustomSoundType("jar", 1.0F, 1.0F);
        assertEquals("thaumcraft:jar", sound.getName());
        assertEquals("thaumcraft:jar", sound.getStepSound().getLocation().toString());
        assertEquals("thaumcraft:jar", sound.getBreakSound().getLocation().toString());
        assertEquals("thaumcraft:jar", sound.getPlaceSound().getLocation().toString());
        assertEquals("thaumcraft:jar", sound.getFallSound().getLocation().toString());
        assertEquals("thaumcraft:jar", sound.getHitSound().getLocation().toString());
        assertEquals(1.0F, sound.getVolume());
        assertEquals(1.0F, sound.getPitch());
    }

    @Test void theJarBlockUsesTheOriginalSoundType() {
        assertEquals("thaumcraft:jar", ThaumcraftContent.JAR_SOUND.getStepSound().getLocation().toString());
        assertEquals(1.0F, ThaumcraftContent.JAR_SOUND.getVolume());
        assertEquals(1.0F, ThaumcraftContent.JAR_SOUND.getPitch());
        assertTrue(ThaumcraftContent.JAR_SOUND instanceof CustomSoundType,
                "the jar keeps the original custom sound type");
    }
}

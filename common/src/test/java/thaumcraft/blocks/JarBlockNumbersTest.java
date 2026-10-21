package thaumcraft.blocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import thaumcraft.common.blocks.BlockJar;

/**
 * The numbers of the jar family against the 4.2.3.5 oracle.
 *
 * <p>Oracle {@code thaumcraft/common/blocks/BlockJar.java}:87-90 - the jar block sets the hardness 0.3
 * and the light level with the 1.7.10 setter value 0.66. That setter stores {@code (int)(15*0.66)} = 9,
 * and every metadata value of the original jar block emits that light, the brain jar and the node jar
 * included.</p>
 */
class JarBlockNumbersTest {
    @Test void everyJarOfTheFamilyEmitsTheLightOfTheOriginal() {
        assertEquals(9, BlockJar.LIGHT);
    }
}

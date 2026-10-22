package thaumcraft.common.lib.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

/** Oracle BlockMagicalLeaves:214-215, BlockLifter:78-82, ClientProxy:648-655. No blocks constructed. */
class SparkleCallRulesParityTest {
    @Test void leavesRequireSilverwoodAndTheOneInFiveHundredRoll() {
        assertEquals(500, SparkleCallRules.LEAVES_ROLL);
        for (int roll = 0; roll < 500; roll++) {
            assertFalse(SparkleCallRules.leavesSparkle(false, roll));
            assertEquals(roll == 0, SparkleCallRules.leavesSparkle(true, roll));
        }
        assertEquals(7, SparkleCallRules.LEAVES_TYPE);
        assertEquals(2.0F, SparkleCallRules.LEAVES_SIZE);
        assertEquals(0.0F, SparkleCallRules.LEAVES_GRAVITY);
    }

    @Test void leavesUseSixWorldRandomFloatsInOriginalAxisOrder() {
        Random oracle = new Random(42);
        double[] expected = {
                -17 + 0.5F + oracle.nextFloat() - oracle.nextFloat(),
                64 + 0.5F + oracle.nextFloat() - oracle.nextFloat(),
                240 + 0.5F + oracle.nextFloat() - oracle.nextFloat()};
        assertArrayEquals(expected, SparkleCallRules.leavesPosition(-17, 64, 240, RandomSource.create(42)));
    }

    @Test void lifterNeedsAnUnpoweredOpenColumn() {
        assertTrue(SparkleCallRules.lifterSparkle(false, 1));
        assertFalse(SparkleCallRules.lifterSparkle(false, 0));
        assertFalse(SparkleCallRules.lifterSparkle(true, 10));
        assertEquals(3, SparkleCallRules.LIFTER_TYPE);
        assertEquals(1.0F, SparkleCallRules.LIFTER_SIZE);
        assertEquals(-0.3F, SparkleCallRules.LIFTER_GRAVITY);
    }

    @Test void lifterUsesDisplayRandomOnlyOnXAndZ() {
        Random oracle = new Random(123);
        double[] expected = {-17 + 0.2F + oracle.nextFloat() * 0.6F,
                65, 240 + 0.2F + oracle.nextFloat() * 0.6F};
        assertArrayEquals(expected, SparkleCallRules.lifterPosition(-17, 64, 240, RandomSource.create(123)));
        assertEquals(6, SparkleCallRules.SPARKLE_ROLL);
        assertEquals(6, SparkleCallRules.SPARKLE_MULTIPLIER);
    }
}

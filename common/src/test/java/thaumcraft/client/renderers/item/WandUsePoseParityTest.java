package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * The use pose of the wand of the original.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/item/ItemWandRenderer.java:88-113}: the wand
 * raises itself while the player uses it. The pose turns around a pivot one block above the
 * model, the third person view rolls the wand by thirty three degrees around Z, the first person
 * view turns it by ten degrees around X and ten degrees around Z, and the wand itself is raised
 * by up to sixty degrees around -X over the first three ticks of the use. A focus with the WAVE
 * animation adds a wave of ten degrees around Z and ten degrees around X, a focus with the CHARGE
 * animation adds a wave of one degree around Z and one degree around X.</p>
 *
 * <p>The elapsed use time is the number of ticks the item has been used: the 1.7.10
 * {@code EntityPlayer.getItemInUseDuration()} (MCP stable 12-1.7.10: {@code field_71057_bx}) and
 * the 1.21.1 {@code LivingEntity#getTicksUsingItem()} both count it up.</p>
 *
 * <p>No test constructs a {@code Block}: every rule under test is a pure static function of
 * {@link WandUsePose}.</p>
 */
class WandUsePoseParityTest {
    @Test void thePoseIsCappedAtThreeTicks() {
        // Original ItemWandRenderer:89-92.
        assertEquals(0.0F, WandUsePose.useTicks(0, 0.0F), 1.0E-6F);
        assertEquals(0.5F, WandUsePose.useTicks(0, 0.5F), 1.0E-6F);
        assertEquals(2.25F, WandUsePose.useTicks(2, 0.25F), 1.0E-6F);
        assertEquals(3.0F, WandUsePose.useTicks(3, 0.0F), 1.0E-6F);
        assertEquals(3.0F, WandUsePose.useTicks(3, 0.9F), 1.0E-6F, "the original caps the pose at three ticks");
        assertEquals(3.0F, WandUsePose.useTicks(40, 0.5F), 1.0E-6F, "the original caps the pose at three ticks");
    }

    @Test void theRaiseIsSixtyDegreesOverThreeTicks() {
        // Original ItemWandRenderer:100: glRotatef(60.0f * (t / 3.0f), -1, 0, 0).
        assertEquals(0.0F, WandUsePose.raiseDegrees(0.0F), 1.0E-6F);
        assertEquals(20.0F, WandUsePose.raiseDegrees(1.0F), 1.0E-6F);
        assertEquals(40.0F, WandUsePose.raiseDegrees(2.0F), 1.0E-6F);
        assertEquals(60.0F, WandUsePose.raiseDegrees(3.0F), 1.0E-6F);
        assertEquals(10F, WandUsePose.raiseDegrees(WandUsePose.useTicks(0, .5F)), 1e-6F);
        assertEquals(45F, WandUsePose.raiseDegrees(WandUsePose.useTicks(2, .25F)), 1e-6F);
        assertEquals(60F, WandUsePose.raiseDegrees(WandUsePose.useTicks(63, .5F)), 1e-6F);
    }

    @Test void theWaveOfTheFocusUsesTheOriginalSine() {
        // Original ItemWandRenderer:102 and :104. The sine of 1.7.10 is the table of 65536
        // values that is read with the factor 10430.378f (the bytecode of the official 1.7.10
        // client.jar, MathHelper.sin), the very table of the 1.21.1 Mth.sin.
        int[] ticks = {0, 1, 10, 40, 100, 127};
        float[][] expected = {
                {.24924606F, .16585407F, .30739352F, .34958667F},
                {1.2459888F, .8321798F, .9999652F, .9770077F},
                {8.547073F, 6.313498F, .24363112F, .8748654F},
                {-7.7288876F, 4.4240336F, .046960883F, .8141477F},
                {-5.647827F, 3.8949327F, -.34410143F, -.9632886F},
                {1.5791152F, 8.084305F, .9162955F, -.41390017F}};
        for (int i = 0; i < ticks.length; i++) {
            assertEquals(expected[i][0], WandUsePose.waveRoll(ticks[i], .25F), 1e-6F);
            assertEquals(expected[i][1], WandUsePose.wavePitch(ticks[i], .25F), 1e-6F);
            assertEquals(expected[i][2], WandUsePose.chargeRoll(ticks[i], .25F), 1e-6F);
            assertEquals(expected[i][3], WandUsePose.chargePitch(ticks[i], .25F), 1e-6F);
        }
    }

    @Test void theWaveKeepsRunningAfterTheRaiseIsFull() {
        // Original ItemWandRenderer:102 uses the UNCAPPED elapsed time, so the wave of a focus
        // still moves when the raise has already reached its sixty degrees.
        float capped = WandUsePose.useTicks(40, 0.0F);
        assertEquals(3.0F, capped, 1.0E-6F);
        float roll = WandUsePose.waveRoll(40, 0.0F);
        assertEquals(-7.5677032F, roll, 1.0E-6F, "the wave of the original is not capped at three ticks");
    }

    @Test void theConstantsAreTheOriginalOnes() {
        // Original ItemWandRenderer:93, :95, :97, :98, :100.
        assertEquals(1.0F, WandUsePose.PIVOT_Y, 0.0F);
        assertEquals(33.0F, WandUsePose.THIRD_PERSON_ROLL, 0.0F);
        assertEquals(10.0F, WandUsePose.FIRST_PERSON_PITCH, 0.0F);
        assertEquals(10.0F, WandUsePose.FIRST_PERSON_ROLL, 0.0F);
        assertEquals(60.0F, WandUsePose.RAISE_DEGREES, 0.0F);
        assertEquals(3.0F, WandUsePose.MAX_USE_TICKS, 0.0F);
        assertEquals(10.0F, WandUsePose.WAVE_DEGREES, 0.0F);
        assertEquals(1.0F, WandUsePose.CHARGE_DEGREES, 0.0F);
    }
}

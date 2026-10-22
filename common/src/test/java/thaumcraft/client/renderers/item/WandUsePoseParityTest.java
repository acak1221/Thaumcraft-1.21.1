package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
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
        for (int tick = 0; tick < 64; tick++) {
            float capped = WandUsePose.useTicks(tick, 0.0F);
            assertEquals(60.0F * (capped / 3.0F), WandUsePose.raiseDegrees(capped), 1.0E-6F,
                    "the raise of the original is 60 * (t / 3)");
        }
    }

    @Test void theWaveOfTheFocusUsesTheOriginalSine() {
        // Original ItemWandRenderer:102 and :104. The sine of 1.7.10 is the table of 65536
        // values that is read with the factor 10430.378f (the bytecode of the official 1.7.10
        // client.jar, MathHelper.sin), the very table of the 1.21.1 Mth.sin.
        for (int tick = 0; tick < 128; tick++) {
            float partial = 0.25F;
            assertEquals(Mth.sin((tick + partial) / 10.0F) * 10.0F, WandUsePose.waveRoll(tick, partial), 1.0E-6F);
            assertEquals(Mth.sin((tick + partial) / 15.0F) * 10.0F, WandUsePose.wavePitch(tick, partial), 1.0E-6F);
            assertEquals(Mth.sin((tick + partial) / 0.8F) * 1.0F, WandUsePose.chargeRoll(tick, partial), 1.0E-6F);
            assertEquals(Mth.sin((tick + partial) / 0.7F) * 1.0F, WandUsePose.chargePitch(tick, partial), 1.0E-6F);
        }
    }

    @Test void theWaveKeepsRunningAfterTheRaiseIsFull() {
        // Original ItemWandRenderer:102 uses the UNCAPPED elapsed time, so the wave of a focus
        // still moves when the raise has already reached its sixty degrees.
        float capped = WandUsePose.useTicks(40, 0.0F);
        assertEquals(3.0F, capped, 1.0E-6F);
        float roll = WandUsePose.waveRoll(40, 0.0F);
        float expected = Mth.sin(40.0F / 10.0F) * 10.0F;
        assertEquals(expected, roll, 1.0E-6F, "the wave of the original is not capped at three ticks");
        assertTrue(Math.abs(expected) > 1.0E-3F, "the wave of the original really moves at the tick forty");
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

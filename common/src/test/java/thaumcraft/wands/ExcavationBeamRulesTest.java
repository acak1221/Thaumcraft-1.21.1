package thaumcraft.wands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.client.ThaumcraftExcavationClient;

/**
 * The beam of the excavation focus.
 *
 * <p>Oracle {@code thaumcraft/client/fx/beams/FXBeamWand.java:37-297}: the beam of the original is
 * a fan of three planes that turn around the axis of the beam ({@code :229-255}), the rotation of
 * the fan follows the time of the world ({@code :171}), the texture of the beam slides with the age
 * of the player ({@code :201-206}), the width of a plane is a quarter of a block ({@code :235-238})
 * and the opacity of the beam is four tenths that fades to a tenth in the last four ticks of its
 * life ({@code :177-180}).</p>
 *
 * <p>No test constructs a {@code Block}: every rule under test is a pure static function.</p>
 */
class ExcavationBeamRulesTest {
    @Test void theFanOfTheOriginalHasThreePlanesOfSixtyDegrees() {
        assertEquals(3, ThaumcraftExcavationClient.PLANES, "the fan of the original has three planes");
        assertEquals(60.0F, ThaumcraftExcavationClient.PLANE_STEP, 1.0E-6F,
                "every plane of the fan is turned by sixty degrees");
        assertEquals(5, ThaumcraftExcavationClient.ROTATION_SPEED, "the fan turns with five degrees per tick");
        assertEquals(0.15F, ThaumcraftExcavationClient.PLANE_HALF_WIDTH, 1.0E-6F,
                "a plane of the original is three tenths of a block wide");
        assertEquals(0.4F, ThaumcraftExcavationClient.BEAM_OPACITY, 1.0E-6F,
                "the opacity of the beam of the original is four tenths");
    }

    @Test void theRotationOfTheFanFollowsTheTimeOfTheWorld() {
        // Original FXBeamWand:171 - (worldTime % (360 / 5)) * 5 + 5 * partial.
        assertEquals(0.0F, ThaumcraftExcavationClient.spin(0L, 0.0F), 1.0E-6F, "the fan starts at zero");
        assertEquals(5.0F, ThaumcraftExcavationClient.spin(0L, 1.0F), 1.0E-6F, "the fan turns five degrees a tick");
        assertEquals(5.0F, ThaumcraftExcavationClient.spin(1L, 0.0F), 1.0E-6F, "the fan turns five degrees a tick");
        assertEquals(360.0F, ThaumcraftExcavationClient.spin(72L, 0.0F), 1.0E-6F,
                "the fan of the original wraps after seventy two ticks");
        assertEquals(0.0F, ThaumcraftExcavationClient.spin(72L, 0.0F) - 360.0F, 1.0E-6F, "the fan wraps");
    }

    @Test void theTextureOfTheBeamSlidesWithTheAgeOfThePlayer() {
        // Original FXBeamWand:201-206 - -value * 0.2 - sin(-value * 0.1).
        assertEquals(0.0F, ThaumcraftExcavationClient.scroll(0, 0.0F), 1.0E-6F, "the texture starts at rest");
        float value = 10.0F;
        assertEquals(-value * 0.2F - (float) Math.sin(-value * 0.1F),
                ThaumcraftExcavationClient.scroll(10, 0.0F), 1.0E-6F, "the slide of the original");
        assertTrue(ThaumcraftExcavationClient.scroll(11, 0.0F) != ThaumcraftExcavationClient.scroll(10, 0.0F),
                "the slide follows the age of the player");
    }

    @Test void theFrameOfTheBeamPutsItsLocalYOnTheDirection() {
        assertEquals(0.0F, ThaumcraftExcavationClient.yaw(new Vec3(0, 0, 1)), 1.0E-4F,
                "the yaw of a beam that points to the south is zero");
        assertEquals(90.0F, ThaumcraftExcavationClient.yaw(new Vec3(1, 0, 0)), 1.0E-4F,
                "the yaw of a beam that points to the west is ninety degrees");
        assertEquals(0.0F, ThaumcraftExcavationClient.elevation(new Vec3(0, 0, 1)), 1.0E-4F,
                "a horizontal beam has no elevation");
        assertEquals(90.0F, ThaumcraftExcavationClient.elevation(new Vec3(0, 1, 0)), 1.0E-4F,
                "a beam that points up has an elevation of ninety degrees");
        assertEquals(-90.0F, ThaumcraftExcavationClient.elevation(new Vec3(0, -1, 0)), 1.0E-4F,
                "a beam that points down has an elevation of minus ninety degrees");
    }
}

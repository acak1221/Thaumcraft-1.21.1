package thaumcraft.scanning;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.relics.ItemThaumometer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The original thaumometer numbers of TC4 4.2.3.5.
 *
 * <p>Oracle {@code thaumcraft/common/items/relics/ItemThaumometer.java}:
 * {@code :59-60} {@code getMaxItemUseDuration} returns 25,
 * {@code :148-162} {@code onUsingTick} completes the scan when {@code count <= 5}
 * (that is after 20 of the 25 ticks) and plays {@code thaumcraft:cameraticks} on
 * every second tick with loudness {@code .2F} and the pitch
 * {@code .45F + worldRand.nextFloat() * .1F}.</p>
 */
class ThaumometerScanParityTest {
    @Test void theOriginalUseDurationIsTwentyFiveTicks() {
        assertEquals(25, ItemThaumometer.USE_DURATION);
    }

    @Test void theOriginalScanCompletesAfterTwentyOfTheTwentyFiveTicks() {
        assertEquals(20, ItemThaumometer.SCAN_TICKS);
        // The original finishes when the remaining use count reaches five.
        assertEquals(ItemThaumometer.USE_DURATION - 5, ItemThaumometer.SCAN_TICKS);
    }

    @Test void theOriginalCameraTickSoundsOnEverySecondTick() {
        assertEquals(2, ItemThaumometer.CAMERA_TICK_INTERVAL);
        assertTrue(ItemThaumometer.cameraTickDue(24));
        assertFalse(ItemThaumometer.cameraTickDue(23));
        assertTrue(ItemThaumometer.cameraTickDue(22));
        assertFalse(ItemThaumometer.cameraTickDue(21));
        assertTrue(ItemThaumometer.cameraTickDue(6));
        assertFalse(ItemThaumometer.cameraTickDue(5));
    }

    @Test void theOriginalCameraTickUsesVolumePointTwoAndPitchPointFourFive() {
        assertEquals(0.2F, ItemThaumometer.CAMERA_VOLUME);
        assertEquals(0.45F, ItemThaumometer.CAMERA_PITCH);
        assertEquals(0.1F, ItemThaumometer.CAMERA_PITCH_SPREAD);
    }
}

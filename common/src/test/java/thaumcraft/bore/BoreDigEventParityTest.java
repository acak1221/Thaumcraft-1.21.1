package thaumcraft.bore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.particles.BoreDigSparkleParticle;
import thaumcraft.common.tiles.TileArcaneBore;

/**
 * The numbers of the dig event of the arcane bore and of its flying dig sparkle.
 *
 * <p>Original dig event (oracle {@code thaumcraft/common/lib/network/misc/PacketBoreDig.java:39-47}
 * and {@code thaumcraft/common/tiles/TileArcaneBore.java:707-713}): the event carries the dig
 * target as the offset to the bore, encoded as {@code (offset &amp; 0xFF) - 64}, and it is sent
 * to the players around the bore with a {@code TargetPoint} of {@code 64.0} blocks. The target of
 * a dig event can therefore never be more than sixty four blocks away from the bore.
 *
 * <p>Original flying dig sparkle (oracle {@code thaumcraft/client/fx/particles/FXBoreSparkle.java}):
 * the life is {@code base/2 + rand(base)} with {@code base = max(1, (int)(distance*3))}, the
 * size is {@code 0.1*scale*(sin(age/3)*0.5+1)} and the scale is {@code rand*0.5+0.5}.
 *
 * <p>These tests were AUTHORED but NOT EXECUTED: the wave forbids a Gradle run, so the leader
 * has to run {@code :common:test} before the verdicts are trusted.</p>
 */
class BoreDigEventParityTest {
    @Test void theDigEventOnlyReachesTheSixtyFourBlocksAroundTheBore() {
        assertEquals(64, TileArcaneBore.DIG_EVENT_RANGE,
                "the original sends the dig event to the players within sixty four blocks");
        assertTrue(TileArcaneBore.inDigEventRange(64, 0, 0), "sixty four blocks are inside the range");
        assertTrue(TileArcaneBore.inDigEventRange(0, -64, 0), "the range is the same on every axis");
        assertTrue(TileArcaneBore.inDigEventRange(0, 0, 64), "the range is the same on every axis");
        assertFalse(TileArcaneBore.inDigEventRange(65, 0, 0), "sixty five blocks are outside the range");
        assertFalse(TileArcaneBore.inDigEventRange(0, 65, 0), "sixty five blocks are outside the range");
        assertFalse(TileArcaneBore.inDigEventRange(0, 0, -65), "sixty five blocks are outside the range");
    }

    @Test void theLifeOfTheDigSparkleIsHalfTheBasePlusTheRandomOfTheBase() {
        // Original FXBoreSparkle:42-46.
        for (int distance = 0; distance < 40; distance++) {
            int base = Math.max(1, (int) (distance * 3.0F));
            for (int random = 0; random < base; random++) {
                assertEquals(base / 2 + random, BoreDigSparkleParticle.lifetimeFor(distance, random),
                        "the life of a sparkle of " + distance + " blocks");
            }
        }
    }

    @Test void theBaseOfTheDigSparkleNeverDropsBelowOne() {
        // Original FXBoreSparkle:43-45 - a sparkle that starts on its target has a
        // base of one, so its life is one/2 + rand(1) = 0 and it dies at once.
        assertEquals(0, BoreDigSparkleParticle.lifetimeFor(0, 0), "the base is one, so the life is zero");
        assertEquals(0, BoreDigSparkleParticle.lifetimeFor(0, 1), "the base is one, so the life is zero");
        // One block: the base is (int)(1*3) = 3, so the life is 3/2 + rand(3).
        assertEquals(1, BoreDigSparkleParticle.lifetimeFor(1, 0), "the base is three, so the life starts at one");
        assertEquals(3, BoreDigSparkleParticle.lifetimeFor(1, 2), "the base is three, so the life ends at three");
    }

    @Test void theSizeOfTheDigSparkleIsATenthOfTheScaleTimesTheBob() {
        // Original FXBoreSparkle:67 and :74.
        assertEquals(0.075F, BoreDigSparkleParticle.sizeFor(0.75F, 0), 1.0E-6F,
                "the bob of the first frame is one, so the size is a tenth of the scale");
        for (int age = 0; age < 12; age++) {
            // The 1.21.1 Mth.sin is the table of the 65536 steps, so the sine of
            // the test is the exact one of the original with that accuracy.
            float bob = (float) Math.sin(age / 3.0F) * 0.5F + 1.0F;
            assertEquals(0.1F * 0.75F * bob, BoreDigSparkleParticle.sizeFor(0.75F, age), 1.0E-3F,
                    "the size of a sparkle of scale 0.75 at age " + age);
        }
    }

    @Test void theRenderDistanceOfTheDigSparkleIsTheOriginalOne() {
        // Original FXBoreSparkle:57-60.
        assertEquals(64, BoreDigSparkleParticle.RENDER_DISTANCE);
        assertEquals(32, BoreDigSparkleParticle.FAST_RENDER_DISTANCE);
    }
}

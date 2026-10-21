package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.client.fx.particles.ParticleDeflection;

/**
 * The push-out-of-blocks rule of the TC4 4.2.3.5 client particles.
 *
 * <p>Original (oracle {@code thaumcraft/client/fx/particles/FXWisp.java:225-294}, the same body in
 * {@code FXWispEG.java:110-139} and {@code FXEssentiaTrail.java:145-197}): a mote that ends up inside a
 * solid block is pushed out through the nearest open face. The face order is west, east, down, up, north
 * and south, each one tested with a strict {@code <} against the distance that is still the best one, and
 * the two random numbers are drawn before the face is applied: the speed
 * {@code rand.nextFloat() * 0.05f + 0.025f} and the sideways jitter
 * {@code (rand.nextFloat() - rand.nextFloat()) * 0.1f}.</p>
 *
 * <p>The rule is asserted through the pure function {@code ParticleDeflection.deflect}, the one the
 * production code calls. No block is ever constructed here: the 1.21.1 block constructor creates an
 * intrusive holder in the block registry, and a block that is never registered makes the later registry
 * freeze throw and breaks every other test of the JVM.</p>
 */
class ParticleDeflectionTest {
    /** The speed and the jitter of the first three draws of the seed 0, the values of the real 1.21.1 random. */
    private static final float SEED_0_SPEED = 0.06154839F;
    private static final float SEED_0_JITTER = 0.05909046F;
    /** The speed and the jitter of the first three draws of the seed 42. */
    private static final float SEED_42_SPEED = 0.06137819F;
    private static final float SEED_42_JITTER = -0.062855825F;
    /** The speed and the jitter of the draws four to six of the seed 42. */
    private static final float SEED_42_SECOND_SPEED = 0.027396966F;
    private static final float SEED_42_SECOND_JITTER = -0.06333541F;

    private static ParticleDeflection.Motion deflect(RandomSource random, boolean west, boolean east,
                                                     boolean down, boolean up, boolean north, boolean south,
                                                     double x, double y, double z) {
        return ParticleDeflection.deflect(random, west, east, down, up, north, south, x, y, z);
    }

    @Test void theMoteLeavesThroughTheOnlyOpenFace() {
        var random = RandomSource.create(0L);
        // The original face order: west 0, east 1, down 2, up 3, north 4, south 5.
        var west = deflect(random, true, false, false, false, false, false, .5D, .5D, .5D);
        assertEquals(-SEED_0_SPEED, west.x(), 1.0E-6, "the west face pushes the mote to -x");
        assertEquals(SEED_0_JITTER, west.y(), 1.0E-6);
        assertEquals(SEED_0_JITTER, west.z(), 1.0E-6);

        random = RandomSource.create(0L);
        var east = deflect(random, false, true, false, false, false, false, .5D, .5D, .5D);
        assertEquals(SEED_0_SPEED, east.x(), 1.0E-6, "the east face pushes the mote to +x");
        assertEquals(SEED_0_JITTER, east.y(), 1.0E-6);
        assertEquals(SEED_0_JITTER, east.z(), 1.0E-6);

        random = RandomSource.create(0L);
        var down = deflect(random, false, false, true, false, false, false, .5D, .5D, .5D);
        assertEquals(-SEED_0_SPEED, down.y(), 1.0E-6, "the down face pushes the mote to -y");
        assertEquals(SEED_0_JITTER, down.x(), 1.0E-6);
        assertEquals(SEED_0_JITTER, down.z(), 1.0E-6);

        random = RandomSource.create(0L);
        var up = deflect(random, false, false, false, true, false, false, .5D, .5D, .5D);
        assertEquals(SEED_0_SPEED, up.y(), 1.0E-6, "the up face pushes the mote to +y");
        assertEquals(SEED_0_JITTER, up.x(), 1.0E-6);
        assertEquals(SEED_0_JITTER, up.z(), 1.0E-6);

        random = RandomSource.create(0L);
        var north = deflect(random, false, false, false, false, true, false, .5D, .5D, .5D);
        assertEquals(-SEED_0_SPEED, north.z(), 1.0E-6, "the north face pushes the mote to -z");
        assertEquals(SEED_0_JITTER, north.x(), 1.0E-6);
        assertEquals(SEED_0_JITTER, north.y(), 1.0E-6);

        random = RandomSource.create(0L);
        var south = deflect(random, false, false, false, false, false, true, .5D, .5D, .5D);
        assertEquals(SEED_0_SPEED, south.z(), 1.0E-6, "the south face pushes the mote to +z");
        assertEquals(SEED_0_JITTER, south.x(), 1.0E-6);
        assertEquals(SEED_0_JITTER, south.y(), 1.0E-6);
    }

    @Test void theNearestOpenFaceWins() {
        // The mote sits at x 0.1 inside its block: the west face is the nearest one.
        var random = RandomSource.create(42L);
        var motion = deflect(random, true, true, false, false, false, false, .1D, .5D, .5D);
        assertEquals(-SEED_42_SPEED, motion.x(), 1.0E-6, "the nearest open face is the west one");
        assertEquals(SEED_42_JITTER, motion.y(), 1.0E-6);
        assertEquals(SEED_42_JITTER, motion.z(), 1.0E-6);

        // The mote sits at x 0.9: the east face is the nearest one.
        random = RandomSource.create(42L);
        motion = deflect(random, true, true, false, false, false, false, .9D, .5D, .5D);
        assertEquals(SEED_42_SPEED, motion.x(), 1.0E-6, "the nearest open face is the east one");
    }

    @Test void theFaceOrderIsTheOriginalOne() {
        // Every face is open and the mote sits in the middle of the block: the original tests the faces in
        // the order west, east, down, up, north, south with a strict comparison, so the west one wins.
        var random = RandomSource.create(0L);
        var motion = deflect(random, true, true, true, true, true, true, .5D, .5D, .5D);
        assertEquals(-SEED_0_SPEED, motion.x(), 1.0E-6, "the west face is tested first and wins the tie");
        assertEquals(SEED_0_JITTER, motion.y(), 1.0E-6);
        assertEquals(SEED_0_JITTER, motion.z(), 1.0E-6);

        // With the west face closed the east one wins the same tie.
        random = RandomSource.create(0L);
        motion = deflect(random, false, true, true, true, true, true, .5D, .5D, .5D);
        assertEquals(SEED_0_SPEED, motion.x(), 1.0E-6, "the east face is the second one of the original order");
    }

    @Test void theSpeedAndTheJitterAreTheOriginalNumbers() {
        // The original draws the speed with one nextFloat and the jitter with two of them, in that order.
        var random = RandomSource.create(0L);
        var reference = RandomSource.create(0L);
        float speed = reference.nextFloat() * 0.05F + 0.025F;
        float jitter = (reference.nextFloat() - reference.nextFloat()) * 0.1F;
        var motion = deflect(random, true, false, false, false, false, false, .5D, .5D, .5D);
        assertEquals(speed, (float) -motion.x(), 1.0E-6, "the speed is rand.nextFloat() * 0.05f + 0.025f");
        assertEquals(jitter, (float) motion.y(), 1.0E-6, "the jitter is (rand.nextFloat() - rand.nextFloat()) * 0.1f");
        assertEquals(SEED_0_SPEED, (float) -motion.x(), 1.0E-6, "the pinned value of the seed 0");
        assertEquals(SEED_0_JITTER, (float) motion.y(), 1.0E-6, "the pinned value of the seed 0");
    }

    @Test void theRandomNumbersAreDrawnEvenWhenEveryFaceIsClosed() {
        // The original draws the two numbers before it applies the face, so a mote that is walled in still
        // consumes them and the next deflection of the same particle uses the following pair.
        var random = RandomSource.create(42L);
        assertNull(deflect(random, false, false, false, false, false, false, .5D, .5D, .5D),
                "a mote that is walled in is not deflected at all");
        var motion = deflect(random, true, false, false, false, false, false, .5D, .5D, .5D);
        assertEquals(SEED_42_SECOND_SPEED, (float) -motion.x(), 1.0E-6,
                "the walled in deflection still consumed the two random numbers of the original");
        assertEquals(SEED_42_SECOND_JITTER, (float) motion.y(), 1.0E-6);
    }
}

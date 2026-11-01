package thaumcraft.client.renderers.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JarNodeAnimationTest {
    @Test void captureShrinksBothShellAndNodeAcrossOneSecond() {
        var start = JarNodeAnimation.sample(2_000L, 1_000L);
        var middle = JarNodeAnimation.sample(2_000L, 1_500L);
        var end = JarNodeAnimation.sample(2_000L, 2_000L);
        assertEquals(3F, start.shellScale());
        assertEquals(1F, start.nodeScale());
        assertEquals(2F, middle.shellScale());
        assertEquals(.85F, middle.nodeScale(), 1e-6F);
        assertTrue(start.active());
        assertTrue(middle.active());
        assertEquals(1F, end.shellScale());
        assertEquals(.7F, end.nodeScale());
        assertFalse(end.active());
    }

    @Test void delayedFramesAndReloadedJarsSettleWithoutRestarting() {
        assertSame(JarNodeAnimation.STORED, JarNodeAnimation.sample(2_000L, 5_000L));
        assertSame(JarNodeAnimation.STORED, JarNodeAnimation.sample(0L, 1_000L));
        assertSame(JarNodeAnimation.STORED, JarNodeAnimation.sample(-1L, 1_000L));
    }

    @Test void backwardClockCannotEnlargeCaptureBeyondItsInitialSize() {
        var frame = JarNodeAnimation.sample(2_000L, 500L);
        assertEquals(3F, frame.shellScale());
        assertEquals(1F, frame.nodeScale());
        assertTrue(frame.active());
    }

    @Test void clearedDeadlineDoesNotReadClockAgain() {
        var reads = new AtomicInteger();
        var completed = JarNodeAnimation.sample(2_000L, () -> {
            reads.incrementAndGet();
            return 2_000L;
        });
        assertFalse(completed.active());
        long clearedDeadline = completed.active() ? 2_000L : 0L;
        for (int frame = 0; frame < 100; frame++)
            assertSame(JarNodeAnimation.STORED, JarNodeAnimation.sample(clearedDeadline, () -> {
                reads.incrementAndGet();
                return 2_001L;
            }));
        assertEquals(1, reads.get());
    }
}

package thaumcraft.notifications;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.client.lib.NotificationQueue;
import static org.junit.jupiter.api.Assertions.*;

class NotificationQueueTest {
    @Test void idleFramesReuseTheImmutableEmptySnapshot() {
        var queue = new NotificationQueue(5000);
        var empty = queue.update(0);
        for (int frame = 1; frame <= 1000; frame++) assertSame(empty, queue.update(frame));
        queue.add("first", null, 1, 1001);
        assertEquals(1, queue.update(1001).size());
        queue.clear();
        assertSame(empty, queue.update(1002));
    }
    @Test void firstOriginalBonusAndQuarterDelayAreIndependentOfFollowingArrival() {
        var queue = new NotificationQueue(5000);
        queue.add("first", null, 0xFFFFFF, 1000);
        assertEquals(List.of(new NotificationQueue.Notification("first", null, 0xFFFFFF, 8500, 2250)), queue.update(1000));
        queue.add("second", "thaumcraft:textures/aspects/aer.png", 0x123456, 3000);
        var values = queue.update(4000);
        assertEquals(8500, values.getFirst().expire());
        assertEquals(new NotificationQueue.Notification("second", "thaumcraft:textures/aspects/aer.png", 0x123456, 9000, 4250), values.get(1));
        assertThrows(UnsupportedOperationException.class, () -> values.clear());
    }
    @Test void queuedRowsKeepTheirOriginalCreationButExtendUntilBecomingFirst() {
        var queue = new NotificationQueue(5000);
        queue.add("first", null, 1, 0); queue.add("second", null, 2, 100); queue.add("third", null, 3, 200);
        // A real HUD updates while queued rows are still alive. Jumping past their initial
        //5100/5200ms expiry without rendering would correctly discard them in the original too.
        assertEquals(9000, queue.update(4000).get(1).expire());
        assertEquals(7500, queue.update(7000).getFirst().expire());
        var transition = queue.update(7501);
        assertEquals(List.of("second", "third"), transition.stream().map(NotificationQueue.Notification::text).toList());
        assertEquals(12501, transition.getFirst().expire()); // Expired prefix still advances original first=false.
        assertEquals(1350, transition.getFirst().created());
        assertEquals(12501, queue.update(8000).getFirst().expire());
        assertEquals(13000, queue.update(8000).get(1).expire());
    }
    @Test void exactExpirationBoundaryDuplicateMessagesAndResetMatchOriginalQueue() {
        var queue = new NotificationQueue(5000);
        queue.add("same", null, 1, 0);
        assertEquals(1, queue.update(7500).size());
        assertTrue(queue.update(7501).isEmpty());
        queue.add("same", null, 1, 7502); queue.add("same", null, 1, 7502);
        assertEquals(2, queue.update(7502).size());
        queue.clear(); assertTrue(queue.update(7502).isEmpty());
        queue.add("new", null, 1, 8000); assertEquals(15500, queue.update(8000).getFirst().expire());
    }
}

package thaumcraft.notifications;

import org.junit.jupiter.api.Test;
import thaumcraft.client.lib.AspectFlightQueue;
import thaumcraft.client.lib.ResearchToastQueue;
import static org.junit.jupiter.api.Assertions.*;

class NotificationMotionTest {
    @Test void originalResearchToastKeepsFullThreeSecondsAndQuarticEntryExit() {
        var queue=new ResearchToastQueue();queue.add("JARBRAIN",1000);
        assertEquals(new ResearchToastQueue.Frame("JARBRAIN",-36),queue.frame(1000));
        assertEquals(new ResearchToastQueue.Frame("JARBRAIN",-2),queue.frame(1188));
        assertEquals(new ResearchToastQueue.Frame("JARBRAIN",0),queue.frame(1375));
        assertEquals(new ResearchToastQueue.Frame("JARBRAIN",0),queue.frame(3625));
        assertEquals(new ResearchToastQueue.Frame("JARBRAIN",-36),queue.frame(4000));
        assertNull(queue.frame(4001));assertNull(queue.frame(5000));
    }
    @Test void queuedResearchStartsAfterFirstExpiryAndHasOriginalBlankTransitionFrame() {
        var queue=new ResearchToastQueue();queue.add("first",1000);queue.add("second",1500);
        assertEquals("first",queue.frame(3999).key());
        assertNull(queue.frame(4001));
        assertEquals(new ResearchToastQueue.Frame("second",-36),queue.frame(4001));
        assertEquals(new ResearchToastQueue.Frame("second",0),queue.frame(5501));
        queue.clear();assertNull(queue.frame(5501));
    }
    @Test void originalAspectFlightUsesFutureBirthQuadraticPathAndStrictExpiry() {
        var queue=new AspectFlightQueue();queue.add("aer",0xFFFF00,.5F,.5F,1000);
        var flight=queue.update(500).getFirst();assertNull(AspectFlightQueue.frame(flight,999,400,300));
        var birth=AspectFlightQueue.frame(flight,1000,400,300);
        assertEquals(200,birth.x());assertEquals(150,birth.y());assertEquals(0,birth.alpha());
        var mid=AspectFlightQueue.frame(flight,1750,400,300);
        assertEquals(300,mid.x());assertEquals(110.5,mid.y());assertEquals(1,mid.alpha());
        var end=AspectFlightQueue.frame(flight,2500,400,300);
        assertEquals(400,end.x());assertEquals(-8,end.y());assertEquals(0,end.alpha(),.000001F);
        assertEquals(1,queue.update(2500).size());assertTrue(queue.update(2501).isEmpty());
    }
    @Test void startPositionUsesOriginalDoubleTimesFloatTruncationAndScaledWidthFraction() {
        var flight=new AspectFlightQueue.Flight("aer",0,.58F,.4F,0,1500);
        var birth=AspectFlightQueue.frame(flight,0,500,300);
        assertEquals(289,birth.x()); // float multiplication would round to290 before truncation.
        assertEquals(120,birth.y());
        var fractional=AspectFlightQueue.frame(flight,1500,500.75,300.75);
        assertEquals(500,fractional.x());assertEquals(-8,fractional.y());
    }
}

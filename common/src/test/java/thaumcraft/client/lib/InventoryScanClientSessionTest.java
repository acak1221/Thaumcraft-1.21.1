package thaumcraft.client.lib;

import static org.junit.jupiter.api.Assertions.*;

import thaumcraft.common.lib.inventoryscan.ScanReply;
import thaumcraft.common.lib.inventoryscan.ScanRequest;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class InventoryScanClientSessionTest {
    @Test void heartbeatsKeepTheirRequestAndTransitionCancelsOnlyTheOldRequest() {
        var sent = new ArrayList<ScanRequest>();
        var session = new InventoryScanClient.Session(packet -> { sent.add(packet); return true; });
        session.begin(2, 9);
        var first = sent.getFirst();
        assertEquals(2, first.containerId()); assertEquals(9, first.slot()); assertTrue(first.active());
        for (int i = 0; i < 4; i++) session.tick();
        assertEquals(1, sent.size());
        session.tick();
        assertEquals(first, sent.getLast());
        session.begin(0, -1);
        assertEquals(new ScanRequest(2, 9, first.requestId(), false), sent.get(sent.size() - 2));
        var self = sent.getLast();
        assertEquals(-1, self.slot()); assertEquals(0, self.containerId());
        assertTrue(self.requestId() > first.requestId());
    }

    @Test void delayedAndReorderedReceiptsCannotCompleteAnotherTargetOrRegressProgress() {
        var sent = new ArrayList<ScanRequest>();
        var session = new InventoryScanClient.Session(packet -> { sent.add(packet); return true; });
        session.begin(0, 10);
        int old = sent.getLast().requestId();
        session.begin(0, 11);
        int current = sent.getLast().requestId();
        session.accept(new ScanReply(old, 25, ScanReply.SUCCESS));
        assertFalse(session.acknowledged); assertEquals(ScanReply.SCANNING, session.status);
        session.accept(new ScanReply(current, 12, ScanReply.SCANNING));
        session.accept(new ScanReply(current, 3, ScanReply.SCANNING));
        assertEquals(12, session.progress);
        session.accept(new ScanReply(current, Integer.MAX_VALUE, ScanReply.SUCCESS));
        assertEquals(25, session.progress); assertEquals(ScanReply.SUCCESS, session.status);
        session.accept(new ScanReply(current, 0, ScanReply.SCANNING));
        for (int i = 0; i < 20; i++) session.tick();
        assertEquals(ScanReply.SUCCESS, session.status);
        assertEquals(current, sent.getLast().requestId());
        assertEquals(3, sent.size(), "old start/cancel and new start only; terminal scans stop heartbeats");
    }

    @Test void missingServerCapabilityProducesNoProgressSoundsOrRepeatedPayloadAttempts() {
        var sent = new ArrayList<ScanRequest>();
        var session = new InventoryScanClient.Session(packet -> { sent.add(packet); return false; });
        session.begin(0, 9);
        int id = sent.getLast().requestId();
        for (int i = 0; i < 80; i++) session.tick();
        session.accept(new ScanReply(id, 20, ScanReply.SCANNING));
        assertFalse(session.available); assertFalse(session.cameraTick()); assertFalse(session.acknowledged);
        assertEquals(1, sent.size());
    }

    @Test void closingAndReconnectingNeverReusesAnIdOrAcceptsTheClosedTargetsReply() {
        var sent = new ArrayList<ScanRequest>();
        var session = new InventoryScanClient.Session(packet -> { sent.add(packet); return true; });
        session.begin(0, 9);
        int previous = sent.getLast().requestId();
        session.cancel();
        session.accept(new ScanReply(previous, 25, ScanReply.SUCCESS));
        assertEquals(ScanReply.CANCELLED, session.status);
        session.cancel();
        session.begin(0, 9);
        int next = sent.getLast().requestId();
        assertTrue(next > previous);
        session.accept(new ScanReply(previous, 25, ScanReply.SUCCESS));
        assertEquals(ScanReply.SCANNING, session.status);
        assertFalse(session.acknowledged);
    }

    @Test void cameraTicksRequireAReceiptAndTheOriginalThreeGameTickLeadIn() {
        var sent = new ArrayList<ScanRequest>();
        var session = new InventoryScanClient.Session(packet -> { sent.add(packet); return true; });
        session.begin(0, 9);
        int id = sent.getLast().requestId();
        session.accept(new ScanReply(id, 0, ScanReply.SCANNING));
        session.tick(); assertFalse(session.cameraTick());
        session.tick(); assertFalse(session.cameraTick());
        session.tick(); assertTrue(session.cameraTick());
        session.accept(new ScanReply(id, 3, ScanReply.FAILED));
        assertFalse(session.cameraTick());
    }

    @Test void originalProgressDotsRetainTheirQuarterThresholds() {
        assertEquals("", InventoryScanClient.Session.dots(0));
        assertEquals("", InventoryScanClient.Session.dots(6));
        assertEquals(".", InventoryScanClient.Session.dots(7));
        assertEquals(".", InventoryScanClient.Session.dots(12));
        assertEquals("..", InventoryScanClient.Session.dots(13));
        assertEquals("..", InventoryScanClient.Session.dots(18));
        assertEquals("...", InventoryScanClient.Session.dots(19));
        assertEquals("...", InventoryScanClient.Session.dots(25));
    }
}

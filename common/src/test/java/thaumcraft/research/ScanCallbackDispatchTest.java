package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.research.IScanEventHandler;
import thaumcraft.api.research.ScanResult;

class ScanCallbackDispatchTest {
    private ArrayList<IScanEventHandler> previous;
    @BeforeEach void isolate() { previous = ThaumcraftApi.scanEventhandlers; ThaumcraftApi.scanEventhandlers = new ArrayList<>(); }
    @AfterEach void restore() { ThaumcraftApi.scanEventhandlers = previous; }

    @Test void callbacksKeepRegistrationOrderAndStopAtTheFirstNonnullIncludingInvalidResults() {
        var calls = new ArrayList<String>(); var unsupported = new ScanResult((byte) 99, 0, 0, null, "unsupported");
        ThaumcraftApi.registerScanEventhandler((stack, level, player) -> { calls.add("first"); return null; });
        ThaumcraftApi.registerScanEventhandler((stack, level, player) -> { calls.add("second"); return unsupported; });
        ThaumcraftApi.registerScanEventhandler((stack, level, player) -> { fail("A nonnull invalid result still occludes later callbacks"); return null; });
        assertSame(unsupported, ScanCallbacks.dispatch(null, null, null)); assertEquals(List.of("first", "second"), calls);
    }

    @Test void registrationDuringDispatchTakesEffectNextTimeWithoutOriginalConcurrentModificationCrash() {
        var result = new ScanResult((byte) 1, 7, 0, null, ""); var once = new AtomicBoolean();
        ThaumcraftApi.registerScanEventhandler((stack, level, player) -> {
            if (once.compareAndSet(false, true)) ThaumcraftApi.registerScanEventhandler((nextStack, nextWorld, nextPlayer) -> result);
            return null;
        });
        assertNull(ScanCallbacks.dispatch(null, null, null));
        assertSame(result, ScanCallbacks.dispatch(null, null, null));
        ThaumcraftApi.scanEventhandlers.clear(); assertNull(ScanCallbacks.dispatch(null, null, null));
    }

    @Test void originalMutableDtoEqualityQuirkIsNotAReceiverAuthorityCheck() {
        var first = new ScanResult((byte) 1, 12, 3, null, ""); var other = new ScanResult((byte) 1, 12, 3, null, "");
        assertTrue(first.equals(other)); other.meta = 4; assertFalse(first.equals(other));
        assertTrue(first.equals(new Object())); assertTrue(first.equals(null));
        assertFalse(first.equals(new ScanResult((byte) 3, 12, 3, null, "NODEfixture")));
    }
}

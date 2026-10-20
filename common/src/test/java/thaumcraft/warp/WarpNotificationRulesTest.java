package thaumcraft.warp;

import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.WarpNotificationRules;
import static org.junit.jupiter.api.Assertions.*;

class WarpNotificationRulesTest {
    @Test void originalPermanentStickyAndTemporaryTextAndWhisperBranches() {
        assertEquals(new WarpNotificationRules.Message("tc.addwarp", true), WarpNotificationRules.message((byte)0, 3));
        assertEquals(new WarpNotificationRules.Message("tc.addwarpsticky", true), WarpNotificationRules.message((byte)1, 2));
        assertEquals(new WarpNotificationRules.Message("tc.removewarpsticky", false), WarpNotificationRules.message((byte)1, -100));
        assertEquals(new WarpNotificationRules.Message("tc.addwarptemp", false), WarpNotificationRules.message((byte)2, 4));
        // These removal translations exist in original lang but are unreachable because of its outer positive guards.
        assertNull(WarpNotificationRules.message((byte)0, -3));
        assertNull(WarpNotificationRules.message((byte)2, -4));
        for (byte kind : new byte[]{0,1,2,127,-128}) assertNull(WarpNotificationRules.message(kind, 0));
    }
    @Test void originalUnknownSignedByteKindsUsePositiveTemporaryFallback() {
        for (byte kind : new byte[]{-128,-1,3,127}) {
            assertEquals(new WarpNotificationRules.Message("tc.addwarptemp", false), WarpNotificationRules.message(kind, Integer.MAX_VALUE));
            assertNull(WarpNotificationRules.message(kind, Integer.MIN_VALUE));
        }
    }
}

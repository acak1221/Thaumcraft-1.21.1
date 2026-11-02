package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class NodeDrainColorTest {
    @Test void originalFivePartIntegerColorEasingKeepsChannelsIndependent() {
        assertEquals(0xffcccc, NodeDrainClientEffects.smoothColor(0xffffff, 0xff0000));
        assertEquals(0xffa3a3, NodeDrainClientEffects.smoothColor(0xffcccc, 0xff0000));
        assertEquals(0x102030, NodeDrainClientEffects.smoothColor(0x102030, 0x102030));
        assertEquals(0x000033, NodeDrainClientEffects.smoothColor(0, 0x0000ff));
    }
}

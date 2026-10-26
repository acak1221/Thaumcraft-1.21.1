package thaumcraft.common.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

class OriginalDisplayAspectsTest {
    @Test void theDisplayAspectsAreTheOriginalRegistryInItsOrderAndIgnoreLaterAddOns() {
        // Original displayAspects = Aspect.aspects.values().toArray() in Thaumcraft's preInit, before add-ons.
        List<Aspect> registry = new ArrayList<>(Aspect.aspects.values()).subList(0, 48);
        assertEquals(registry, List.of(OriginalDisplayAspects.ALL));
        Aspect addon = new Aspect("display_test_addon_" + System.nanoTime(), 0x123456, new Aspect[]{Aspect.VOID, Aspect.ORDER});
        try {
            assertEquals(48, OriginalDisplayAspects.ALL.length);
            assertFalse(List.of(OriginalDisplayAspects.ALL).contains(addon));
        } finally {
            Aspect.aspects.remove(addon.getTag(), addon);
        }
    }
}

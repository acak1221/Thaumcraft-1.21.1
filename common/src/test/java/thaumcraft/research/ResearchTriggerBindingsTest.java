package thaumcraft.common.lib.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The item triggers of the research entries.
 *
 * <p>Original ConfigResearch binds an item trigger to every research that a scan of that item reveals.
 * The corpus keeps the original expression; the native port binds the identity it owns.</p>
 */
class ResearchTriggerBindingsTest {
    @BeforeAll static void load() { ResearchCatalog.loadBundled(); }

    @Test void thePrimalStaffRodIsBoundToThePrimalFocus() {
        // Original ConfigResearch:414 setItemTriggers(new ItemStack(ConfigItems.itemFocusPrimal)).
        var research = ResearchCatalog.get("ROD_primal_staff");
        assertTrue(research.itemTriggerExpressions().stream()
                .anyMatch(expression -> expression.contains("itemFocusPrimal")));
        assertTrue(ResearchTriggerBindings.boundItems("ROD_primal_staff").contains("thaumcraft:focus_primal"));
    }

    @Test void theOtherOriginalItemTriggersKeepTheirNativeIdentity() {
        assertEquals(java.util.Set.of("minecraft:bow", "minecraft:bone"), ResearchTriggerBindings.boundItems("BONEBOW"));
        assertEquals(java.util.Set.of("thaumcraft:quick_silver"), ResearchTriggerBindings.boundItems("JARBRAIN"));
        assertEquals(java.util.Set.of("minecraft:ender_pearl"), ResearchTriggerBindings.boundItems("MIRROR"));
        assertEquals(java.util.Set.of("thaumcraft:primordial_pearl"), ResearchTriggerBindings.boundItems("PRIMPEARL"));
        assertTrue(ResearchTriggerBindings.boundItems("BANNERS").isEmpty());
        assertTrue(ResearchTriggerBindings.boundItems("OUTERREV").isEmpty());
    }
}

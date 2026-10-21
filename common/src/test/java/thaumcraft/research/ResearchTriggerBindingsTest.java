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
        // Original ConfigResearch:221 - the bow (Items.field_151031_f) and the bone
        // (Items.field_151103_aS) of the bone bow.
        assertEquals(java.util.Set.of("minecraft:bow", "minecraft:bone"), ResearchTriggerBindings.boundItems("BONEBOW"));
        // Original ConfigResearch:235 - itemResource metadata three, the quick silver.
        assertEquals(java.util.Set.of("thaumcraft:quick_silver"), ResearchTriggerBindings.boundItems("JARBRAIN"));
        // Original ConfigResearch:213 - the ender pearl and the three portal blocks
        // (Blocks.field_150427_aO the nether portal, Blocks.field_150384_bq the end portal
        // and Blocks.field_150378_br the end portal frame).
        assertEquals(java.util.Set.of("minecraft:ender_pearl", "minecraft:nether_portal", "minecraft:end_portal",
                "minecraft:end_portal_frame"), ResearchTriggerBindings.boundItems("MIRROR"));
        // Original ConfigResearch:400 - itemEldritchObject metadata three.
        assertEquals(java.util.Set.of("thaumcraft:primordial_pearl"), ResearchTriggerBindings.boundItems("PRIMPEARL"));
        // Original ConfigResearch:199 - the wooden device metadata eight, the banner.
        assertEquals(java.util.Set.of("thaumcraft:banner"), ResearchTriggerBindings.boundItems("BANNERS"));
        // Original ConfigResearch:399 - the eldritch block metadata five and ten.
        assertEquals(java.util.Set.of("thaumcraft:eldritch_crust", "thaumcraft:eldritch_spawner"),
                ResearchTriggerBindings.boundItems("OUTERREV"));
    }
}

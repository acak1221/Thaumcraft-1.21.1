package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;

class ResearchItemIconTest {
    @BeforeAll static void loadResearch() { ResearchCatalog.loadBundled(); }

    @Test void allNineStaffResearchIconsResolveToTheirActualStaffRods() {
        for (String material : List.of("blaze", "bone", "greatwood", "ice", "obsidian",
                "primal", "quartz", "reed", "silverwood")) {
            assertIcon("ROD_" + material + "_staff", "staff_rod_" + material);
        }
    }

    @Test void goldTravellerBootsAndHarnessKeepTheirCorpusIcons() {
        assertIcon("PUREGOLD", "cluster_gold");
        assertIcon("BOOTSTRAVELLER", "boots_traveller");
        assertIcon("HOVERHARNESS", "hover_harness");
    }

    @Test void acceptingStaffMetadataDoesNotAcceptArbitraryItemsOrUnsupportedVariants() {
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.itemWandRod, 1, 999)"));
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.itemBootsTraveller, 1, 50)"));
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.unrecognized)"));
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.itemWandRod, 65, 50)"));
    }

    private static void assertIcon(String key, String path) {
        var research = ResearchCatalog.get(key);
        assertNotNull(research, key);
        assertEquals(new ResearchIconDisplay.ItemReference(path, 1),
                ResearchIconDisplay.itemReference(research.iconExpression()), key);
    }
}

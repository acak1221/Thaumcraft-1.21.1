package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.ThaumatoriumRules;

class ThaumatoriumRulesTest {
    @Test void firstMissingAspectUsesLexicalTagOrderRatherThanRecipeInsertionOrder() {
        var costs = new java.util.LinkedHashMap<String, Integer>();
        costs.put("potentia", 3); costs.put("lux", 3); costs.put("ignis", 3);
        var stock = new AspectList();
        assertEquals(Aspect.FIRE, ThaumatoriumRules.missing(costs, stock));
        stock.add(Aspect.FIRE, 3);
        assertEquals(Aspect.LIGHT, ThaumatoriumRules.missing(costs, stock));
        stock.add(Aspect.LIGHT, 3);
        assertEquals(Aspect.ENERGY, ThaumatoriumRules.missing(costs, stock));
        stock.add(Aspect.ENERGY, 3);
        assertNull(ThaumatoriumRules.missing(costs, stock));
    }
    @Test void partialAmountsKeepRequestingTheSameAspectAndExcessStockDoesNotChangeTheCost() {
        var stock = new AspectList().add(Aspect.METAL, 1).add(Aspect.FIRE, 100);
        assertEquals(Aspect.METAL, ThaumatoriumRules.missing(Map.of("metallum", 2), stock));
        stock.add(Aspect.METAL, 1);
        assertNull(ThaumatoriumRules.missing(Map.of("metallum", 2), stock));
        assertEquals(100, stock.getAmount(Aspect.FIRE));
    }
    @Test void everyFifthHeatedUnpoweredTickWorks() {
        for (int counter = 1; counter <= 80; counter++) {
            assertEquals(counter % 5 == 0, ThaumatoriumRules.workTick(counter, true, false));
            assertFalse(ThaumatoriumRules.workTick(counter, false, false));
            assertFalse(ThaumatoriumRules.workTick(counter, true, true));
        }
    }
    @Test void originalFormationVisAndSuctionRemainExact() {
        assertEquals(Map.of("ignis", 15, "ordo", 30, "aqua", 30), ThaumatoriumRules.FORM_VIS);
        assertEquals(128, ThaumatoriumRules.SUCTION);
        assertEquals(40, ThaumatoriumRules.HEAT_INTERVAL);
    }
    @Test void anOrphanedTopBecomesTheAlchemicalConstruct() {
        // Oracle TileThaumatoriumTop:36-47 - only a top without its lower half
        // turns into the construct.
        assertTrue(ThaumatoriumRules.orphanBecomesConstruct(false));
        assertFalse(ThaumatoriumRules.orphanBecomesConstruct(true));
    }
}

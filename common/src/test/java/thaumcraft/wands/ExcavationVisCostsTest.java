package thaumcraft.wands;

import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.foci.ExcavationVisCosts;
import static org.junit.jupiter.api.Assertions.*;

class ExcavationVisCostsTest {
    @Test void firstSilkTooltipAlsoDeterminesLaterDowsingWithoutChangingBaseWandCost() {
        var cache = new ExcavationVisCosts.Tooltip();
        Map<String, Integer> silk = Map.of("aer",1,"ignis",1,"terra",16,"aqua",1,"ordo",1,"perditio",1);
        assertEquals(Map.of("terra",15), cache.cost(false, false));
        assertEquals(silk, cache.cost(true, false));
        assertEquals(silk, cache.cost(false, true));
        assertEquals(silk, cache.cost(true, true));
        assertEquals(Map.of("terra",15), cache.cost(false, false));
        assertThrows(UnsupportedOperationException.class, () -> cache.cost(true, false).put("terra",0));
    }
    @Test void firstDowsingTooltipAlsoDeterminesLaterSilkAndTheBranchesKeepSilkPriority() {
        var cache = new ExcavationVisCosts.Tooltip();
        Map<String, Integer> dowsing = Map.of("ignis",2,"ordo",2,"terra",15);
        assertEquals(dowsing, cache.cost(false, true));
        assertEquals(dowsing, cache.cost(true, false));
        assertEquals(dowsing, cache.cost(true, true));
        var bothFirst = new ExcavationVisCosts.Tooltip();
        assertEquals(Map.of("aer",1,"ignis",1,"terra",16,"aqua",1,"ordo",1,"perditio",1), bothFirst.cost(true, true));
    }
}

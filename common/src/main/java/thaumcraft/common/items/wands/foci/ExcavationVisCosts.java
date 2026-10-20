package thaumcraft.common.items.wands.foci;

import java.util.LinkedHashMap;
import java.util.Map;

/** Original Excavation's first-initialized, shared cost2 tooltip; actual casting still prices the outer wand as base15terra. */
public final class ExcavationVisCosts {
    private ExcavationVisCosts() {}
    public static final class Tooltip {
        private Map<String, Integer> special;
        public synchronized Map<String, Integer> cost(boolean silk, boolean dowsing) {
            if (!silk && !dowsing) return Map.of("terra", ItemFocusExcavation.BASE_COST_CENTIVIS);
            if (special == null) {
                var amounts = new LinkedHashMap<String, Integer>();
                if (silk) {
                    for (String primal : new String[]{"aer", "ignis", "terra", "aqua", "ordo", "perditio"}) amounts.put(primal, 1);
                } else { amounts.put("ignis", 2); amounts.put("ordo", 2); }
                amounts.merge("terra", ItemFocusExcavation.BASE_COST_CENTIVIS, Integer::sum);
                special = java.util.Collections.unmodifiableMap(amounts);
            }
            return special;
        }
    }
}

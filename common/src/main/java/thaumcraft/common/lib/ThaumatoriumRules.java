package thaumcraft.common.lib;

import java.util.Comparator;
import java.util.Map;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** TileThaumatorium: five-tick work, forty-tick heat refresh, one missing aspect in lexical tag order. */
public final class ThaumatoriumRules {
    public static final int WORK_INTERVAL = 5, HEAT_INTERVAL = 40, SUCTION = 128;
    public static final Map<String, Integer> FORM_VIS = Map.of("ignis", 15, "ordo", 30, "aqua", 30);
    private ThaumatoriumRules() {}
    public static Aspect missing(Map<String, Integer> cost, AspectList stock) {
        return cost.keySet().stream().sorted().map(Aspect::getAspect)
                .filter(aspect -> aspect != null && stock.getAmount(aspect) < cost.get(aspect.getTag())).findFirst().orElse(null);
    }
    public static boolean workTick(int counter, boolean heat, boolean powered) { return heat && !powered && counter % WORK_INTERVAL == 0; }
}

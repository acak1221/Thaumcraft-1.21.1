package thaumcraft.common.lib;

import java.util.Comparator;
import java.util.Map;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** TileThaumatorium: five-tick work, forty-tick heat refresh, one missing aspect in lexical tag order. */
public final class ThaumatoriumRules {
    public static final int WORK_INTERVAL = 5, HEAT_INTERVAL = 40, SUCTION = 128;
    public static final Map<String, Integer> FORM_VIS = Map.of("ignis", 15, "ordo", 30, "aqua", 30);
    /**
     * Oracle TileThaumatorium.java:494-516 - one formula is free and every
     * mnemonic matrix adds two. The matrix may stand on the ten cells the
     * original scans (two levels, every direction but down and the front),
     * so the machine holds at most twenty one formulas.
     */
    public static final int BASE_RECIPES = 1, RECIPES_PER_MATRIX = 2, MAX_MATRIX_SLOTS = 10, MAX_RECIPES = 21;
    private ThaumatoriumRules() {}
    /** The number of formulas of a Thaumatorium with the given number of mnemonic matrices. */
    public static int recipeCapacity(int matrices) { return BASE_RECIPES + RECIPES_PER_MATRIX * matrices; }
    public static Aspect missing(Map<String, Integer> cost, AspectList stock) {
        return cost.keySet().stream().sorted().map(Aspect::getAspect)
                .filter(aspect -> aspect != null && stock.getAmount(aspect) < cost.get(aspect.getTag())).findFirst().orElse(null);
    }
    public static boolean workTick(int counter, boolean heat, boolean powered) { return heat && !powered && counter % WORK_INTERVAL == 0; }

    /**
     * Original TileThaumatoriumTop:36-47 - the upper half watches the lower one
     * and turns into the alchemical construct when the lower half is gone.
     */
    public static boolean orphanBecomesConstruct(boolean lowerHalfIsThaumatorium) { return !lowerHalfIsThaumatorium; }
}

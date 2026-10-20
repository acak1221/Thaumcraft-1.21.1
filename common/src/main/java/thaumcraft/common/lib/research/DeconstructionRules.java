package thaumcraft.common.lib.research;

import java.util.function.IntUnaryOperator;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.foci.FocalUpgradeCosts;

/** TileDeconstructionTable:155-200 and ResearchManager:470-494, SUM branch. */
public final class DeconstructionRules {
    public static final int BREAK_TICKS = 40;
    public static final int CHANCE_BOUND = 80;
    private DeconstructionRules() {}

    private static boolean canonical(Aspect aspect) {
        return aspect != null && Aspect.getAspect(aspect.getTag()) == aspect;
    }

    /** The existing guarded SUM reducer implements this same original method.
     * Every overlapping component path contributes its full quantity. */
    public static AspectList primals(AspectList input) { return FocalUpgradeCosts.reduceToPrimals(input); }

    /** One chance roll; on success one UNWEIGHTED draw among distinct primals. */
    public static Aspect roll(AspectList primals, IntUnaryOperator random) {
        int total = 0;
        for (var entry : primals.aspects.entrySet()) {
            if (!canonical(entry.getKey()) || !entry.getKey().isPrimal() || entry.getValue() == null || entry.getValue() <= 0)
                throw new IllegalArgumentException("Invalid primal reward");
            total = Math.addExact(total, entry.getValue());
        }
        if (total == 0 || random.applyAsInt(CHANCE_BOUND) >= total) return null;
        return primals.getAspects()[random.applyAsInt(primals.size())];
    }
}

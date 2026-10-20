package thaumcraft.common.lib.research;

import java.util.function.IntUnaryOperator;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original TileNode487–544,798–821; source death and world mutation belong to TileNode. */
public final class HungryNodeRules {
    public static final double RANGE = 15D, DAMAGE_DISTANCE_SQUARED = 2D;
    public static final int BLOCK_INTERVAL = 50;
    private HungryNodeRules() {}
    /** Difference is node centre minus entity feet, not entity eye position. */
    public static Vec3 pull(Vec3 difference) {
        Vec3 scaled = difference.scale(1D / RANGE);
        double distance = scaled.length(), strength = 1D - distance;
        if (!Double.isFinite(distance) || distance <= 1e-12 || strength <= 0) return Vec3.ZERO;
        double factor = strength * strength / distance;
        return new Vec3(scaled.x * factor * .15D, scaled.y * factor * .25D, scaled.z * factor * .15D);
    }
    public static boolean breakable(float hardness) { return hardness >= 0 && hardness < 5; }
    public record Meal(Aspect aspect, boolean refill, boolean grow) {}
    /** One UNWEIGHTED distinct-primal draw, and a second draw only at/full above BASE. */
    public static Meal meal(AspectList primals, AspectList current, AspectList base, IntUnaryOperator random) {
        if (primals.size() == 0) return null;
        Aspect chosen = primals.getAspects()[random.applyAsInt(primals.size())];
        if (chosen == null || !chosen.isPrimal() || Aspect.getAspect(chosen.getTag()) != chosen || primals.getAmount(chosen) <= 0)
            throw new IllegalArgumentException("Invalid hungry-node meal");
        if (current.getAmount(chosen) < base.getAmount(chosen)) return new Meal(chosen, true, false);
        long bound = 1L + base.getAmount(chosen) * 2L;
        // Invalid imported/addon capacities must not cause original nextInt(<=0) crashes.
        boolean grows = bound > 0 && bound <= Integer.MAX_VALUE && random.applyAsInt((int)bound) < primals.getAmount(chosen);
        return new Meal(chosen, false, grows);
    }
}

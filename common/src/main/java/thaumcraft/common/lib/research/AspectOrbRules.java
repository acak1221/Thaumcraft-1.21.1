package thaumcraft.common.lib.research;

import java.util.function.IntUnaryOperator;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original TileNode213-219,726-740; EntityAspectOrb40-155,194-204. */
public final class AspectOrbRules {
    public static final int EMISSION_INTERVAL = 100, MAX_AGE = 150, STABILITY_ROLL = 42;
    public static final double RANGE = 8D;
    public static final float GRAVITY = .03F;
    private AspectOrbRules() {}
    public static int stabilityBound(int lock) { if (lock <= 0) throw new IllegalArgumentException("A stabilizer lock is required"); return 10000 / lock; }
    /** Zero-current entries participate in the draw; selecting one produces no orb. */
    public static Aspect chooseCurrentPrimal(AspectList current, IntUnaryOperator random) {
        Aspect[] primals = current.getPrimalAspects();
        if (primals.length == 0) return null; // Safe replacement for original nextInt(0) crash.
        Aspect aspect = primals[random.applyAsInt(primals.length)];
        return current.getAmount(aspect) > 0 ? aspect : null;
    }
    /** Difference is player-eye minus orb position in actual block units. */
    public static Vec3 attraction(Vec3 difference) {
        Vec3 scaled = difference.scale(1D / RANGE); double distance = scaled.length(), strength = 1D - distance;
        if (!Double.isFinite(distance) || distance <= 1e-12 || strength <= 0) return Vec3.ZERO;
        return scaled.scale(strength * strength * .1D / distance);
    }
    public static int centivis(int wholeValue) { if (wholeValue <= 0 || wholeValue > Short.MAX_VALUE) throw new IllegalArgumentException("Invalid orb value"); return wholeValue * 100; }
}

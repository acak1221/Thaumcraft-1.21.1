package thaumcraft.common.lib;

import java.util.Map;

/** Literal original Advanced furnace constants and post-debit time calculation. */
public final class AdvancedFurnaceRules {
    public static final int MAX_VIS = 500, MAX_POWER = 500, DRAW = 50;
    public static final Map<String,Integer> FORM_VIS = Map.of("ignis",50,"aqua",50,"ordo",50);
    private AdvancedFurnaceRules() {}
    public static int cooldown(int remainingHeat) { return (int)(5F + Math.max(0F,(1F - remainingHeat/500F)*100F)); }
    public static int light(int heat) { return heat > 100 ? Math.min(15,(int)(heat/500F*12F)) : 0; }
    /** Original floor(fill*14)+vis >0 ?1:0 precedence, on NOZZLES only. */
    public static int comparator(int vis) { return vis > 0 ? 1 : 0; }
    /** Original lower cross nozzle1, corner4; upper cross normal3, corner alembic2; upper center unconstrained. */
    public static int part(int x, int y, int z) {
        if (x < -1 || x > 1 || z < -1 || z > 1 || y < 0 || y > 1 || x == 0 && z == 0 && y == 1) return -1;
        if (y == 0) return x == 0 && z == 0 ? 0 : x == 0 || z == 0 ? 1 : 4;
        return x == 0 || z == 0 ? 3 : 2;
    }
}

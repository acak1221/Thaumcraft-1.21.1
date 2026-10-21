package thaumcraft.common.lib.world;

/**
 * The original numbers of the taint block family.
 *
 * <p>Original {@code BlockTaint:71-72} sets the family hardness 2 and the blast
 * resistance setter value 10; the 1.7.10 setter stores three times its argument and
 * the vanilla getter divides the stored value by five, so the effective blast
 * resistance is 10*3/5 = 6.</p>
 *
 * <p>Original {@code BlockTaint:248-259} overrides the HARDNESS per metadata: 1.75 for
 * the crust, 1.5 for the soil and 0.2 for the flesh. These are hardness values, not
 * light levels - the original block has no light at all. The taint fibres are the
 * other way round: their light is 8 for the type two and 10 for the type four
 * ({@code BlockTaintFibres:96-104}).</p>
 */
public final class TaintBlockRules {
    private TaintBlockRules() {}

    /** Original BlockTaint:71 - the hardness the constructor sets for the family. */
    public static final float TAINT_FAMILY_HARDNESS = 2.0F;
    /** Original BlockTaint:72 - the effective blast resistance of the family. */
    public static final float TAINT_RESISTANCE = 6.0F;
    /** Original BlockTaintFibres:67 - the hardness of the fibres. */
    public static final float FIBRES_HARDNESS = 1.0F;
    /** Original BlockTaintFibres:68 - the effective blast resistance of the fibres. */
    public static final float FIBRES_RESISTANCE = 3.0F;

    /** Original BlockTaint:248-259 - the hardness of one metadata value. */
    public static float taintHardness(int metadata) {
        if (metadata == 0) return 1.75F;
        if (metadata == 1) return 1.5F;
        if (metadata == 2) return 0.2F;
        return TAINT_FAMILY_HARDNESS;
    }

    /** Original BlockTaintFibres:96-104 - the light level of one metadata value. */
    public static int fibreLight(int type) {
        if (type == 2) return 8;
        if (type == 4) return 10;
        return 0;
    }
}

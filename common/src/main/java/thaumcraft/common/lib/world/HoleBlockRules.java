package thaumcraft.common.lib.world;

/**
 * The original numbers of the Portable Hole block.
 *
 * <p>Original {@code BlockHole:53} sets the blast resistance with the 1.7.10 setter value 6000000. That
 * setter stores three times its argument and the vanilla getter divides the stored value by five, so the
 * effective blast resistance is 6000000*3/5 = 3600000.</p>
 *
 * <p>Original {@code BlockHole:55} sets the light level with the 1.7.10 setter value 0.7; that setter
 * stores {@code (int)(15*0.7)} = 10.</p>
 */
public final class HoleBlockRules {
    private HoleBlockRules() {}

    /** Original BlockHole:53 - the effective blast resistance of the hole. */
    public static final float HOLE_RESISTANCE = 3600000.0F;
    /** Original BlockHole:55 - the light level of the hole. */
    public static final int HOLE_LIGHT = 10;
}

package thaumcraft.client.renderers.item;

import net.minecraft.util.Mth;

/**
 * The use pose of the wand of the original.
 *
 * <p>Original {@code thaumcraft/client/renderers/item/ItemWandRenderer.java:88-113}: the wand
 * raises itself while it is used. The pose is expressed around a pivot that is one block above
 * the model, it turns the wand by thirty three degrees around Z in the third person, by ten
 * degrees around X and ten degrees around Z in the first person, and it raises the wand by up to
 * sixty degrees around -X over the first three ticks of the use. A focus with the
 * {@code WAVE} animation adds a wave of ten degrees around Z and ten degrees around X, a focus
 * with the {@code CHARGE} animation adds a wave of one degree around Z and one degree around X.
 *
 * <p>Every number of this class is a pure function of the elapsed use time, so the values can be
 * asserted without a running client. The elapsed time is the number of ticks the item has been
 * used, which 1.21.1 counts with {@code LivingEntity#getTicksUsingItem()} exactly like the
 * 1.7.10 {@code EntityPlayer#getItemInUseDuration()} (MCP stable 12-1.7.10: {@code field_71057_bx})
 * counted it.
 */
public final class WandUsePose {
    /** Original ItemWandRenderer:90-92: the pose reaches its full raise after three ticks. */
    public static final float MAX_USE_TICKS = 3.0F;
    /** Original ItemWandRenderer:93 and :112: the pivot of the pose is one block above the model. */
    public static final float PIVOT_Y = 1.0F;
    /** Original ItemWandRenderer:95: the third person turn of the pose around Z. */
    public static final float THIRD_PERSON_ROLL = 33.0F;
    /** Original ItemWandRenderer:97: the first person turn of the pose around X. */
    public static final float FIRST_PERSON_PITCH = 10.0F;
    /** Original ItemWandRenderer:98: the first person turn of the pose around Z. */
    public static final float FIRST_PERSON_ROLL = 10.0F;
    /** Original ItemWandRenderer:100: the full raise of the wand around -X. */
    public static final float RAISE_DEGREES = 60.0F;
    /** Original ItemWandRenderer:102 and :104: the peak of the WAVE of a focus. */
    public static final float WAVE_DEGREES = 10.0F;
    /** Original ItemWandRenderer:107 and :109: the peak of the CHARGE of a focus. */
    public static final float CHARGE_DEGREES = 1.0F;

    private WandUsePose() {}

    /**
     * Original ItemWandRenderer:89-92: the elapsed use time, capped at three ticks.
     *
     * @param ticksUsingItem the ticks the item has been used
     * @param partial        the partial tick of the frame
     */
    public static float useTicks(int ticksUsingItem, float partial) {
        return Math.min(ticksUsingItem + partial, MAX_USE_TICKS);
    }

    /**
     * Original ItemWandRenderer:100: the raise of the wand, zero to sixty degrees.
     *
     * @param useTicks the capped elapsed use time
     */
    public static float raiseDegrees(float useTicks) {
        return RAISE_DEGREES * (useTicks / MAX_USE_TICKS);
    }

    /**
     * Original ItemWandRenderer:102: the WAVE of a focus around Z, ten degrees at the peak.
     *
     * <p>The original uses the <em>uncapped</em> elapsed time here, so the wave keeps running
     * after the raise has reached its full angle. The sine is the sine of the original: the
     * 1.7.10 {@code MathHelper.sin} reads the table of 65536 values with the factor 10430.378f
     * (the bytecode of the official 1.7.10 {@code client.jar}, {@code qh.java:10-11}), which is
     * the very table of the 1.21.1 {@code Mth.sin}.</p>
     */
    public static float waveRoll(int ticksUsingItem, float partial) {
        return Mth.sin((ticksUsingItem + partial) / 10.0F) * WAVE_DEGREES;
    }

    /** Original ItemWandRenderer:104: the WAVE of a focus around X, ten degrees at the peak. */
    public static float wavePitch(int ticksUsingItem, float partial) {
        return Mth.sin((ticksUsingItem + partial) / 15.0F) * WAVE_DEGREES;
    }

    /** Original ItemWandRenderer:107: the CHARGE of a focus around Z, one degree at the peak. */
    public static float chargeRoll(int ticksUsingItem, float partial) {
        return Mth.sin((ticksUsingItem + partial) / 0.8F) * CHARGE_DEGREES;
    }

    /** Original ItemWandRenderer:109: the CHARGE of a focus around X, one degree at the peak. */
    public static float chargePitch(int ticksUsingItem, float partial) {
        return Mth.sin((ticksUsingItem + partial) / 0.7F) * CHARGE_DEGREES;
    }
}

package thaumcraft.common.items.golems;

/**
 * Pure numbers of the golem dart launcher (original AIDartAttack, EntityGolemBase.attackEntityWithRangedAttack:1138-1145). The
 * shot interval is {@code 30 - 8 * Air upgrades} ticks and the spread {@code 7 - 1.75 * Water upgrades}; neither is clamped, so
 * four Air upgrades (an advanced golem) give a zero or negative interval and a dart on every tick, exactly as the original did.
 */
public final class GolemDartRules {
    /** Original EntityDart speed argument. */
    public static final float SPEED = 1.6F;
    /** The dart does this fraction of the golem's attack attribute (before the arrow's own speed factor). */
    public static final float DAMAGE_FACTOR = 0.4F;
    /** Inside three blocks the golem stops shooting and the melee goal takes over (original {@code ra < 9.0}). */
    public static final double MIN_DISTANCE_SQ = 9.0;
    /** A shot needs the target within this share of the golem's range. */
    public static final float REACH_FACTOR = 0.8F;
    /** Original upgrade stripes: Air shortens the interval, Water narrows the spread. */
    public static final int BASE_INTERVAL = 30, INTERVAL_PER_AIR = 8;
    public static final float BASE_INACCURACY = 7.0F, INACCURACY_PER_WATER = 1.75F;

    private GolemDartRules() {}

    /** Original {@code maxRangedAttackTime = 30 - upgradeAmount(Air) * 8}; may be zero or negative with four Air upgrades. */
    public static int interval(int air) { return BASE_INTERVAL - air * INTERVAL_PER_AIR; }

    /** The goal starts, and restarts after every stop, half an interval into the wait (Java integer division). */
    public static int firstDelay(int interval) { return interval / 2; }

    /** One tick of waiting while the target is in sight; the timer never goes below zero. */
    public static int wait(int timer) { return Math.max(timer - 1, 0); }

    /** Original {@code 7.0f - upgradeAmount(Water) * 1.75f}; four Water upgrades are exact, five would be negative (same spread, mirrored). */
    public static float inaccuracy(int water) { return BASE_INACCURACY - water * INACCURACY_PER_WATER; }

    /** Original {@code setDamage(attack * 0.4f)}: a float product widened to the arrow's double damage. */
    public static double damage(float attack) { return attack * DAMAGE_FACTOR; }

    /** Original: a target nearer than three blocks (squared distance below nine) is left to the melee goal. */
    public static boolean tooClose(double distanceSq) { return distanceSq < MIN_DISTANCE_SQ; }

    /** Original: {@code float r = range * 0.8f; distanceSq <= r * r} with the square taken in float. */
    public static boolean inReach(double distanceSq, float range) {
        float reach = range * REACH_FACTOR;
        reach *= reach;
        return distanceSq <= (double) reach;
    }
}

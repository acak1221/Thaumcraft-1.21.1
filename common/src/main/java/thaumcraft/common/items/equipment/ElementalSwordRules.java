package thaumcraft.common.items.equipment;

import net.minecraft.world.phys.Vec3;

/**
 * The numbers of the Sword of the Zephyr (original ItemElementalSword.onUsingTick / onLeftClickEntity). Held, it lifts its user a little
 * more than gravity takes, slows a fall and blows everything within 2.5 blocks of the user away, and every twentieth tick of that costs a
 * use. A blow also strikes every living mob within 1.2 x 1.1 x 1.2 blocks of the target.
 */
public final class ElementalSwordRules {
    /** A sword is "used" for as long as the button is held: the vanilla bow's duration. */
    public static final int USE_DURATION = 72000;
    /** Ticks between two uses worn out and two wind sounds, counted from the first tick of the hold (which counts). */
    public static final int PERIOD = 20;
    /** Added to the vertical speed every tick, the original 0.08 (what gravity takes), reset to 0.2 above 0.5. */
    public static final double LIFT = 0.08F, LIFT_CEILING = 0.5, LIFT_RESET = 0.2F;
    /** A falling user's speed and fall distance are divided by this every tick. */
    public static final float DAMPING = 1.2F;
    /** Entities this close (a box grown by this much round the user) are blown away. */
    public static final double REACH = 2.5;
    /** The strike spreads over the target's box grown by these, in blocks, along x, y and z. */
    public static final double SWEEP_X = 1.2, SWEEP_Y = 1.1, SWEEP_Z = 1.2;

    private ElementalSwordRules() {}

    /** The user's vertical speed after one tick of the hold. */
    public static double rise(double speed) {
        if (speed < 0) speed /= DAMPING;
        speed += LIFT;
        return speed > LIFT_CEILING ? LIFT_RESET : speed;
    }

    /** The user's fall distance after one tick of the hold: shortened only while the user is falling. */
    public static float fall(float distance, double speed) { return speed < 0 ? distance / DAMPING : distance; }

    /** What an entity at {@code entity} is pushed by when the user stands at {@code user}: away, a 0.4th of a unit, a hair less when far. */
    public static Vec3 push(Vec3 user, Vec3 entity) {
        Vec3 away = entity.subtract(user);
        double distance = user.distanceTo(entity) + 0.1;
        return new Vec3(away.x / REACH / distance, away.y / REACH / distance, away.z / REACH / distance);
    }
}

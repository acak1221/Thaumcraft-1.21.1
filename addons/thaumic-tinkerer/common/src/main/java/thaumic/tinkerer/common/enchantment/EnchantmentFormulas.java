/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.enchantment;

import java.util.UUID;

/**
 * The numbers of the fourteen enchantments, taken out of {@code ModEnchantmentHandler} of 1.7.10 so that they can be
 * tested without a world. Every method is the statement of the original with the Minecraft calls replaced by
 * parameters; the arithmetic is kept literally (compound assignments on a {@code float} cast back to {@code float}
 * after each step, the integer division of {@code successiveStrikes / 5}, the quirks of the successive-strike counter),
 * because the quirks are the behaviour the port has to keep.
 */
public final class EnchantmentFormulas {
    /** {@code min} of {@code onEntityUpdate}: the vertical motion of the first tick of a fall. */
    public static final double SLOW_FALL_MIN_MOTION = -0.0784000015258789;
    /** The fall distance from which Slow Fall acts, and the least it leaves. */
    public static final float SLOW_FALL_MIN_DISTANCE = 2.9F;
    /** The fall distance (exclusive) from which Shockwave hurts the creatures around. */
    public static final float SHOCKWAVE_MIN_DISTANCE = 3F;
    /** Half of the box (blocks) around the faller in which Shockwave hurts. */
    public static final double SHOCKWAVE_RADIUS = 10D;
    /** The draw time of a bow in 1.7.10 ({@code ItemBow.getMaxItemUseDuration}); the clock the quick draw handler ran on. */
    public static final int BOW_USE_DURATION = 72000;
    /** Hardness above which Shatter works at full strength. */
    public static final float SHATTER_HARD_BLOCK = 20F;
    /** The hardest block Disintegrate mines. */
    public static final float DESINTEGRATE_MAX_HARDNESS = 1.5F;
    /** Yaw difference (degrees) below which Tunnel counts the next block as "the same line". */
    public static final float TUNNEL_LINE_ANGLE = 50F;

    private EnchantmentFormulas() {}

    // ---- the strike counter of Focused and Dispersed Strikes -----------------------------------------------------

    /**
     * The new value of the counter kept on the weapon. A different target (or none yet: the stored id is zero) resets it to
     * 0; the same target gives {@code max(counter + 1, 5)}, so the counter is 5 from the second hit on, not 1 (the original
     * wrote {@code Math.max} where it meant {@code Math.min}; kept).
     */
    public static int nextSuccessiveStrikes(UUID lastTarget, int successiveStrikes, UUID target) {
        return !lastTarget.equals(target) ? 0 : Math.max(successiveStrikes + 1, 5);
    }

    // ---- the damage of an attack (LivingHurtEvent) ---------------------------------------------------------------

    /**
     * Everything {@code onEntityDamaged} reads: the levels on the leggings (Pounce) and on the held item, the dice, the health
     * of the attacker and the counter after it was updated for this hit.
     *
     * @param pounce         Pounce level on the leggings of a player attacker, 0 for none
     * @param pounceInAir    whether the block below the attacker's feet is air
     * @param finalStrike    Final Strike level on the held item
     * @param finalStrikeRoll {@code Random.nextInt(19)}, only read when {@code finalStrike > 0}
     * @param valiance       Valiance level on the held item
     * @param health         health of the attacker
     * @param maxHealth      maximum health of the attacker
     * @param focused        Focused Strikes level on the held item
     * @param dispersed      Dispersed Strikes level on the held item
     * @param successiveStrikes the counter after {@link #nextSuccessiveStrikes} for this hit
     */
    public record Attack(int pounce, boolean pounceInAir, int finalStrike, int finalStrikeRoll, int valiance, float health,
            float maxHealth, int focused, int dispersed, int successiveStrikes) {}

    /**
     * The damage of the hit after the enchantments, in the order of the original: Pounce, Final Strike, Valiance, Focused,
     * Dispersed. The casts to float are the narrowing that the compound assignments ({@code amount *= 1.25D}) of the original
     * did on their own: the arithmetic is done in double and cut to float after every step, deliberately.
     */
    public static float modifyDamage(float amount, Attack attack) {
        if (attack.pounce() > 0 && attack.pounceInAir()) {
            amount = (float) (amount * (1 + (.25 * attack.pounce())));
        }

        if (attack.finalStrike() > 0 && attack.finalStrike() > attack.finalStrikeRoll()) {
            amount *= 3;
        }

        if (attack.valiance() > 0 && attack.health() / attack.maxHealth() < .5F) {
            amount = (float) (amount * (1 + .1 * attack.valiance()));
        }

        int successiveStrikes = attack.successiveStrikes();
        if (attack.focused() > 0) {
            amount /= 2;
            amount = (float) (amount + (.5 * Math.max(successiveStrikes, 5) * amount * attack.focused()));
        }
        if (attack.dispersed() > 0) {
            amount = (float) (amount * (1 + 0.6 * attack.dispersed() + (successiveStrikes / 5)));
            amount = (float) (amount / (1 + 0.25 * attack.dispersed() + (successiveStrikes * 2.5)));
        }
        return amount;
    }

    /** What {@link #modifyDamage} does to one point of damage; the enchantments only ever multiply, so this is the factor. */
    public static float damageFactor(Attack attack) {
        return modifyDamage(1F, attack);
    }

    /** {@code attacker.heal(vampirism)}: Vampirism heals the attacker by its level, in health points. */
    public static float vampirismHeal(int level) {
        return level;
    }

    // ---- movement -------------------------------------------------------------------------------------------------

    /** {@code player.motionY *= (boost + 2) / 2D} of {@code onPlayerJump}: the factor of the jump. */
    public static double ascentBoostFactor(int level) {
        return (level + 2) / 2D;
    }

    /**
     * Whether Slow Fall acts in this tick on a client (which simulates the fall): the original condition. {@code level} is
     * the highest Slow Fall level on the armor.
     */
    public static boolean slowFallActs(int level, boolean sneaking, double motionY, float fallDistance) {
        return level > 0 && !sneaking && motionY < SLOW_FALL_MIN_MOTION && fallDistance >= 2.9;
    }

    /**
     * Whether Slow Fall acts in this tick on the server. A server does not simulate the fall of a player (the client
     * moves and reports), so there is no vertical motion to look at; a player in the air whose fall distance counts is
     * falling.
     */
    public static boolean slowFallActsOnServer(int level, boolean sneaking, boolean onGround, float fallDistance) {
        return level > 0 && !sneaking && !onGround && fallDistance >= 2.9;
    }

    /** {@code motionY /= 1 + slowfall * 0.33F}. */
    public static double slowFallMotion(double motionY, int level) {
        return motionY / (1 + level * 0.33F);
    }

    /** {@code fallDistance = Math.max(2.9F, fallDistance - slowfall / 3F)}. */
    public static float slowFallDistance(float fallDistance, int level) {
        return Math.max(SLOW_FALL_MIN_DISTANCE, fallDistance - level / 3F);
    }

    /** The damage Shockwave deals to every creature in the box around a faller: {@code .1F * shockwave * distance}. */
    public static float shockwaveDamage(int level, float distance) {
        return .1F * level * distance;
    }

    /** Whether a fall of this distance shakes the ground. */
    public static boolean shockwaveActs(int level, float distance) {
        return level > 0 && distance > SHOCKWAVE_MIN_DISTANCE;
    }

    // ---- mining ---------------------------------------------------------------------------------------------------

    /** Shatter on a block of this hardness: {@code 3 * level} on hard blocks, {@code .8} on the others. */
    public static double shatterFactor(int level, float hardness) {
        return hardness > SHATTER_HARD_BLOCK ? (3 * level) : .8;
    }

    /**
     * Tunnel: the factor of the dig speed. Without a remembered direction nothing changes; a direction within 50 degrees of
     * the remembered one gives {@code 1 + .2 * level}, any other {@code .3}. {@code angleDifference} is the difference of the
     * two yaws in degrees, already wrapped to 0..180 by the caller.
     */
    public static double tunnelFactor(int level, boolean hasDirection, float angleDifference) {
        if (!hasDirection) return 1;
        return angleDifference < TUNNEL_LINE_ANGLE ? (1 + (.2 * level)) : .3;
    }

    /** Disintegrate acts on a block when its hardness is at most 1.5 (the original compared the raw hardness field). */
    public static boolean desintegrateApplies(int level, float hardness) {
        return level > 0 && hardness <= DESINTEGRATE_MAX_HARDNESS;
    }

    /** Flaming Touch ("auto smelt") acts on wood only. */
    public static boolean autoSmeltApplies(int level, boolean woodenBlock) {
        return level > 0 && woodenBlock;
    }

    /** The dig-speed factor that stands for 1.7.10's {@code Float.MAX_VALUE}: the block breaks at once (the attribute tops out at 1024). */
    public static final double INSTANT_BREAK_FACTOR = 1024D;
    /** The factor of a block the tool cannot mine at all (1.7.10 cancelled the break-speed event). */
    public static final double NO_BREAK_FACTOR = 0D;

    /**
     * The factor of the dig speed of a tool with these enchantments on a block, in the order of {@code onGetHarvestSpeed}:
     * Shatter, Tunnel, then Disintegrate and Flaming Touch, which either make the block break at once ({@link
     * #INSTANT_BREAK_FACTOR}) or, when the block is not theirs, make it unbreakable ({@link #NO_BREAK_FACTOR}).
     */
    public static double breakSpeedFactor(int shatter, int tunnel, int desintegrate, int autoSmelt, float hardness,
            boolean woodenBlock, boolean hasTunnelDirection, float tunnelAngleDifference) {
        double factor = 1;
        if (shatter > 0) factor *= shatterFactor(shatter, hardness);
        if (tunnel > 0) factor *= tunnelFactor(tunnel, hasTunnelDirection, tunnelAngleDifference);

        boolean desintegrateApplies = desintegrateApplies(desintegrate, hardness);
        boolean autoSmeltApplies = autoSmeltApplies(autoSmelt, woodenBlock);
        if (desintegrateApplies || autoSmeltApplies) return INSTANT_BREAK_FACTOR;
        if (desintegrate > 0 || autoSmelt > 0) return NO_BREAK_FACTOR;
        return factor;
    }

    // ---- Quick Draw -----------------------------------------------------------------------------------------------

    /**
     * How many ticks the quick draw handler of 1.7.10 took off the draw clock in addition to the one tick that vanilla takes,
     * after {@code ticks} ticks of drawing a bow: per tick {@code speedup = level / 6}, plus one when the clock
     * ({@code itemInUseCount}, counting down from 72000) was a multiple of {@code 6 - level % 6}. The simulation is the
     * handler itself, run after the vanilla decrement of each tick.
     */
    public static int quickDrawBonusTicks(int level, int ticks) {
        if (level <= 0 || ticks <= 0) return 0;
        int bonus = 0;
        int count = BOW_USE_DURATION;
        for (int tick = 0; tick < ticks; tick++) {
            count--; // vanilla
            int speedup = level / 6;
            int remainder = level % 6;
            if (count % (6 - remainder) == 0) speedup++;
            int next = Math.max(2, count - speedup);
            bonus += count - next;
            count = next;
        }
        return bonus;
    }
}

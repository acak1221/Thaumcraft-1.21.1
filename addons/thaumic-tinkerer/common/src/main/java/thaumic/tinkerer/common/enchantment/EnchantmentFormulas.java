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

    // ---- the damage of an attack (LivingHurtEvent, TinkererEvents.LIVING_DAMAGE) ------------------------------------

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
     * Whether Slow Fall acts in this tick on the client that simulates the fall of its own player: the original condition
     * ({@code slowfall > 0 && !isSneaking() && motionY < min && fallDistance >= 2.9}). {@code level} is the highest Slow Fall
     * level on the armor. Only the motion and the particle of the original's block are decided by this: the fall distance
     * is the landing's ({@link #slowFallLanding}).
     */
    public static boolean slowFallActs(int level, boolean sneaking, double motionY, float fallDistance) {
        return level > 0 && !sneaking && motionY < SLOW_FALL_MIN_MOTION && fallDistance >= 2.9;
    }

    /** {@code motionY /= 1 + slowfall * 0.33F}. */
    public static double slowFallMotion(double motionY, int level) {
        return motionY / (1 + level * 0.33F);
    }

    /**
     * The fall distance of a landing (the {@code LIVING_FALL} event, both sides) of a player with Slow Fall on the armor.
     * The original ran {@code fallDistance = Math.max(2.9F, fallDistance - slowfall / 3F)} <i>every tick</i> of a fall of 2.9
     * blocks or more, while it also divided the motion; the fall distance a landing found was therefore that floor, 2.9
     * (a fall of 2.9 does no damage), however long the fall had been. The event comes once, at the landing, so the result of the
     * ticks is applied at once: a fall of 2.9 or more ends as a fall of {@link #SLOW_FALL_MIN_DISTANCE}. Sneaking turns it off, as
     * it did in the tick; a shorter fall is left alone.
     */
    public static float slowFallLanding(int level, boolean sneaking, float distance) {
        return level > 0 && !sneaking && distance >= 2.9 ? SLOW_FALL_MIN_DISTANCE : distance;
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

    /**
     * The dig speed after the handler of the original ({@code onGetHarvestSpeed}, {@code PlayerEvent.BreakSpeed} with
     * {@code newSpeed}), in its order: Shatter, Tunnel, then Disintegrate and Flaming Touch, which make the speed
     * {@link Float#MAX_VALUE} (the block breaks at once) for a block that is theirs and cancel the event, here a speed of
     * zero ("cannot dig"), for any other block. The compound assignments of the original ({@code newSpeed *= ...}) narrow to
     * {@code float} after every step, and so does this.
     *
     * @param speed the speed so far, as {@code Player.getDestroySpeed} worked it out
     * @return the new speed; {@code 0} means the player cannot dig the block with this tool
     */
    public static float breakSpeed(float speed, int shatter, int tunnel, int desintegrate, int autoSmelt, float hardness,
            boolean woodenBlock, boolean hasTunnelDirection, float tunnelAngleDifference) {
        if (shatter > 0) speed = (float) (speed * shatterFactor(shatter, hardness));
        if (tunnel > 0) speed = (float) (speed * tunnelFactor(tunnel, hasTunnelDirection, tunnelAngleDifference));

        if (desintegrateApplies(desintegrate, hardness) || autoSmeltApplies(autoSmelt, woodenBlock)) return Float.MAX_VALUE;
        if (desintegrate > 0 || autoSmelt > 0) return 0F;
        return speed;
    }

    // ---- Quick Draw -----------------------------------------------------------------------------------------------

    /**
     * The remaining ticks of a bow draw after the Quick Draw handler of the original ({@code onEntityUpdate}): per tick
     * {@code speedup = level / 6}, plus one when the clock ({@code itemInUseCount}, counting down from 72000) is a multiple
     * of {@code 6 - level % 6}, and {@code itemInUseCount = Math.max(2, itemInUseCount - speedup)}.
     *
     * <p>1.7.10 ran that after the decrement of the tick ({@code --itemInUseCount} in {@code EntityPlayer.onUpdate}, then the
     * {@code LivingUpdateEvent}); {@code USE_ITEM_TICK} comes before it, with the count of the tick before. So the rule is
     * applied to the count the decrement is about to leave ({@code remaining - 1}), and one is added back, which the game takes
     * off after the event: the clock then goes through the same values as the original's.</p>
     *
     * @param remaining the use ticks that remain, as the event gives them (before this tick's decrement)
     */
    public static int quickDrawRemaining(int level, int remaining) {
        int count = remaining - 1;
        int speedup = level / 6;
        int remainder = level % 6;
        if (count % (6 - remainder) == 0) speedup++;
        return Math.max(2, count - speedup) + 1;
    }
}

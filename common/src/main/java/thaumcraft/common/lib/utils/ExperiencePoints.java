package thaumcraft.common.lib.utils;

import net.minecraft.world.entity.player.Player;

/** Converts the player's level and current bar into spendable experience points. */
public final class ExperiencePoints {
    private ExperiencePoints() {}

    /** Total points needed to reach a level, saturated at {@link Long#MAX_VALUE}. */
    public static long atLevel(int level) {
        requireNonnegative(level);
        long levels = level;
        if (level <= 16) return levels * (levels + 6L);
        if (level <= 31) {
            long aboveSixteen = levels - 16L;
            return 352L + aboveSixteen * (5L * aboveSixteen + 79L) / 2L;
        }

        // Sum the bars above level 31. Divide the even factor before multiplying so
        // the intermediate product cannot overflow while the result still fits.
        long aboveThirtyOne = levels - 31L;
        long bars = 9L * aboveThirtyOne + 233L;
        if ((aboveThirtyOne & 1L) == 0L) aboveThirtyOne /= 2L;
        else bars /= 2L;
        if (aboveThirtyOne > (Long.MAX_VALUE - 1_507L) / bars) return Long.MAX_VALUE;
        return 1_507L + aboveThirtyOne * bars;
    }

    /**
     * Reconstructs whole points from a level and its bar. Rounding preserves points
     * whose native float progress has accumulated a small error, including a full bar.
     * Progress outside the bar is clamped; NaN contributes no partial points.
     */
    public static long available(int level, float progress) {
        long completed = atLevel(level);
        double boundedProgress = Float.isNaN(progress) ? 0.0D
                : Math.max(0.0D, Math.min(1.0D, (double) progress));
        long currentBar = Math.round(boundedProgress * pointsForNextLevel(level));
        return completed > Long.MAX_VALUE - currentBar ? Long.MAX_VALUE : completed + currentBar;
    }

    public static long available(Player player) {
        return available(player.experienceLevel, player.experienceProgress);
    }

    public static boolean canAfford(Player player, int points) {
        requireNonnegative(points);
        return available(player) >= points;
    }

    /** Creative players must afford the price, but retain their points after paying. */
    public static boolean spend(Player player, int points) {
        if (!canAfford(player, points)) return false;
        if (points > 0 && !player.isCreative()) player.giveExperiencePoints(-points);
        return true;
    }

    private static long pointsForNextLevel(int level) {
        if (level < 15) return 2L * level + 7L;
        if (level < 30) return 5L * level - 38L;
        return 9L * level - 158L;
    }

    private static void requireNonnegative(int value) {
        if (value < 0) throw new IllegalArgumentException("Experience levels and points must be nonnegative");
    }
}

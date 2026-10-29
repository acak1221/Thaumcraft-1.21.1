/**
 * Port of Thaumic Tinkerer's {@code ExperienceHelper} (taken from OpenBlocksLib,
 * https://github.com/OpenMods/OpenModsLib, in the GTNewHorizons fork of 1.7.10). Thaumic Tinkerer is distributed under a
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 License
 * (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB); it is a Derivative Work on Thaumcraft 4.
 * Thaumcraft 4 (c) Azanor 2012 (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import net.minecraft.world.entity.player.Player;

/**
 * Reads and changes the experience of a player in points. The functions are those of 1.7.10; the numbers are those of
 * the 1.21.1 experience curve (the 1.7.10 helper hard-coded that version's curve, which was 17 points a level up to
 * level 16, 3 more per level up to level 31 and 7 more beyond; the points of a level and the size of the bar that
 * {@link Player#getXpNeededForNextLevel()} reports follow the current game now, so a conversion between points and
 * levels must too, see {@code docs/deviations/BasicItems.md}).
 */
public final class ExperienceHelper {
    private ExperienceHelper() {}

    /**
     * The upper bound of the level search: far above level 21,863, the last level whose points (2,147,407,943) fit in an
     * {@code int}; level 21,864 needs more.
     */
    private static final int LEVEL_SEARCH_LIMIT = 1 << 16;

    /** The points of the player: those of the whole levels plus the part of the bar that is filled (saturated at the int range). */
    public static int getPlayerXP(Player player) {
        long points = pointsForLevel(player.experienceLevel)
                + (long) (player.experienceProgress * player.getXpNeededForNextLevel());
        return (int) Math.min(Integer.MAX_VALUE, points);
    }

    public static void drainPlayerXP(Player player, int amount) {
        addPlayerXP(player, -amount);
    }

    /**
     * Adds the points (a negative amount takes them away; the total never goes below 0 and saturates at the int range
     * instead of wrapping around).
     */
    public static void addPlayerXP(Player player, int amount) {
        int experience = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, (long) getPlayerXP(player) + amount));
        player.totalExperience = experience;
        player.experienceLevel = getLevelForExperience(experience);
        int expForLevel = getExperienceForLevel(player.experienceLevel);
        player.experienceProgress = (float) (experience - expForLevel) / (float) player.getXpNeededForNextLevel();
    }

    /**
     * The points that lead from level 0 to the level (1.21.1 curve: 2L+7, then 5L-38, then 9L-158 per level); a level whose
     * points do not fit in an {@code int} gives {@link Integer#MAX_VALUE}.
     */
    public static int getExperienceForLevel(int level) {
        return (int) Math.min(Integer.MAX_VALUE, pointsForLevel(level));
    }

    /** The highest level whose points do not exceed the amount. */
    public static int getLevelForExperience(int experience) {
        // The points grow with the level, so this is a binary search; it ends for every int (the 1.7.10 loop counted up).
        int low = 0;
        int high = LEVEL_SEARCH_LIMIT;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (pointsForLevel(middle) <= experience) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    /** The exact points from level 0 to the level, in a {@code long} so that no level overflows. */
    private static long pointsForLevel(long level) {
        if (level <= 0) return 0;
        // far past the int range already (about 1.9e10 points); keeps the squares below from overflowing a long
        level = Math.min(level, LEVEL_SEARCH_LIMIT);
        if (level <= 16) return level * level + 6 * level;
        // 2.5 L^2 - 40.5 L + 360 and 4.5 L^2 - 162.5 L + 2220, written in whole numbers (the products are even)
        if (level <= 31) return (5 * level * level - 81 * level) / 2 + 360;
        return (9 * level * level - 325 * level) / 2 + 2220;
    }
}

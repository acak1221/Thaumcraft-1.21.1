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

    /** The points of the player: those of the whole levels plus the part of the bar that is filled. */
    public static int getPlayerXP(Player player) {
        return getExperienceForLevel(player.experienceLevel)
                + (int) (player.experienceProgress * player.getXpNeededForNextLevel());
    }

    public static void drainPlayerXP(Player player, int amount) {
        addPlayerXP(player, -amount);
    }

    /** Adds the points (a negative amount takes them away; the total never goes below 0). */
    public static void addPlayerXP(Player player, int amount) {
        int experience = Math.max(0, getPlayerXP(player) + amount);
        player.totalExperience = experience;
        player.experienceLevel = getLevelForExperience(experience);
        int expForLevel = getExperienceForLevel(player.experienceLevel);
        player.experienceProgress = (float) (experience - expForLevel) / (float) player.getXpNeededForNextLevel();
    }

    /** The points that lead from level 0 to the level (1.21.1 curve: 2L+7, then 5L-38, then 9L-158 per level). */
    public static int getExperienceForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** The highest level whose points do not exceed the amount. */
    public static int getLevelForExperience(int experience) {
        int level = 0;
        while (getExperienceForLevel(level + 1) <= experience) level++;
        return level;
    }
}

package thaumcraft.common.lib.cult;

import net.minecraft.core.Direction;

/**
 * The original TC4 cultist portal room: the four crimson banners and the abandoned
 * crates that the crimson portal seeds around itself.
 *
 * <p>Source: {@code thaumcraft/common/entities/monster/boss/EntityCultistPortal.java:130-172}.
 * The geometry is pure so it can be asserted without a world or a registry.
 */
public final class CultistRoomRules {
    private CultistRoomRules() {}

    /**
     * One of the four banners of the portal room.
     *
     * @param originalFacing the original facing byte of the banner block entity
     * @param facing         the native direction the banner shows its face to
     * @param offsetX        the original offset of the banner to the portal on the x axis
     * @param offsetZ        the original offset of the banner to the portal on the z axis
     */
    public record Banner(int originalFacing, Direction facing, int offsetX, int offsetZ) {}

    /**
     * Original EntityCultistPortal.onUpdate: the four banners stand on the four cardinal
     * directions. The original ForgeDirection ordinal two is north, three south, four west
     * and five east; the banner stands six blocks away on that axis and shows its face to
     * the portal, so the banner of the north ordinal stands north of the portal and looks
     * south. The original facing byte is eight south, zero north, twelve west and four east.
     *
     * @param directionOrdinal the original ForgeDirection ordinal of the banner
     */
    public static Banner banner(int directionOrdinal) {
        return switch (directionOrdinal) {
            case 2 -> new Banner(8, Direction.SOUTH, 0, -1);
            case 3 -> new Banner(0, Direction.NORTH, 0, 1);
            case 4 -> new Banner(12, Direction.WEST, 1, 0);
            case 5 -> new Banner(4, Direction.EAST, -1, 0);
            default -> throw new IllegalArgumentException("Not a cardinal banner direction: " + directionOrdinal);
        };
    }

    /**
     * Original EntityCultistPortal.onUpdate: the abandoned crates appear in the five by
     * five ring around the portal, but never on the row or the column of the portal.
     *
     * @param originX the x column of the portal
     * @param originZ the z row of the portal
     * @param x       the x column of the crate
     * @param z       the z row of the crate
     */
    public static boolean crateSpot(int originX, int originZ, int x, int z) {
        return x != originX && z != originZ;
    }

    /**
     * Original EntityCultistPortal.onUpdate: the crate rarity is drawn from the world
     * random, five percent rare and twenty percent uncommon.
     *
     * @param roll the original world random draw
     */
    public static int crateRarity(float roll) {
        return CultistRules.lootCrateVariant(roll);
    }
}

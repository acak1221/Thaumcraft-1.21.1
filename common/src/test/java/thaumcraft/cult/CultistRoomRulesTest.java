package thaumcraft.cult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.cult.CultistRoomRules;
import thaumcraft.common.lib.cult.CultistRules;

/**
 * The original TC4 cultist portal room, asserted without a world or a registry.
 * The oracle source is {@code thaumcraft/common/entities/monster/boss/EntityCultistPortal.java:130-172}.
 */
class CultistRoomRulesTest {

    @Test void theFourBannersStandOnTheFourCardinalDirections() {
        assertEquals(Direction.SOUTH, CultistRoomRules.banner(2).facing());
        assertEquals(Direction.NORTH, CultistRoomRules.banner(3).facing());
        assertEquals(Direction.WEST, CultistRoomRules.banner(4).facing());
        assertEquals(Direction.EAST, CultistRoomRules.banner(5).facing());
    }

    @Test void theBannersKeepTheOriginalFacingBytes() {
        assertEquals(8, CultistRoomRules.banner(2).originalFacing());
        assertEquals(0, CultistRoomRules.banner(3).originalFacing());
        assertEquals(12, CultistRoomRules.banner(4).originalFacing());
        assertEquals(4, CultistRoomRules.banner(5).originalFacing());
        for (int direction = 2; direction < 6; direction++) {
            assertEquals(CultistRules.bannerFacing(direction), CultistRoomRules.banner(direction).originalFacing());
        }
    }

    @Test void everyBannerShowsItsFaceToThePortal() {
        for (int direction = 2; direction < 6; direction++) {
            CultistRoomRules.Banner banner = CultistRoomRules.banner(direction);
            // The banner stands on the axis of its ordinal and looks back at the portal.
            Direction towards = banner.offsetZ() < 0 ? Direction.NORTH
                    : banner.offsetZ() > 0 ? Direction.SOUTH
                    : banner.offsetX() < 0 ? Direction.WEST : Direction.EAST;
            assertEquals(towards.getOpposite(), banner.facing(), "banner " + direction);
        }
    }

    @Test void theBannerOffsetsKeepTheOriginalSignOfTheForgeDirection() {
        // The original places the banner at x - offsetX * 6 and z + offsetZ * 6.
        assertEquals(0, CultistRoomRules.banner(2).offsetX());
        assertEquals(-1, CultistRoomRules.banner(2).offsetZ());
        assertEquals(0, CultistRoomRules.banner(3).offsetX());
        assertEquals(1, CultistRoomRules.banner(3).offsetZ());
        assertEquals(1, CultistRoomRules.banner(4).offsetX());
        assertEquals(0, CultistRoomRules.banner(4).offsetZ());
        assertEquals(-1, CultistRoomRules.banner(5).offsetX());
        assertEquals(0, CultistRoomRules.banner(5).offsetZ());
    }

    @Test void theAbandonedCratesNeverStandOnTheRowOrTheColumnOfThePortal() {
        assertTrue(CultistRoomRules.crateSpot(10, 20, 11, 21));
        assertTrue(CultistRoomRules.crateSpot(10, 20, 9, 19));
        assertFalse(CultistRoomRules.crateSpot(10, 20, 10, 21));
        assertFalse(CultistRoomRules.crateSpot(10, 20, 11, 20));
    }

    @Test void theCrateRarityKeepsTheOriginalRolls() {
        assertEquals(2, CultistRoomRules.crateRarity(0.04F));
        assertEquals(1, CultistRoomRules.crateRarity(0.1F));
        assertEquals(0, CultistRoomRules.crateRarity(0.5F));
    }
}

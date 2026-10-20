package thaumcraft.world;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.OuterLandsWiringRules;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Oracle BlockEldritch, BlockCosmeticSolid, BlockAiry, BlockCosmeticStoneSlab, BlockWoodenDevice,
 * GenCommon, GenLibraryRoom, TeleporterThaumcraft and TileEldritchAltar.
 */
class OuterLandsWiringRulesTest {

    @Test void everyEldritchMetadataLandsOnItsOwnBlock() {
        // Oracle BlockEldritch:231-260 - the hardness and the blast resistance of a metadata
        // belong to the block in 1.21.1, so the family became six blocks.
        assertEquals("thaumcraft:eldritch", OuterLandsWiringRules.eldritchBlock(0));
        assertEquals("thaumcraft:eldritch", OuterLandsWiringRules.eldritchBlock(1));
        assertEquals("thaumcraft:eldritch", OuterLandsWiringRules.eldritchBlock(2));
        assertEquals("thaumcraft:eldritch", OuterLandsWiringRules.eldritchBlock(3));
        assertEquals("thaumcraft:eldritch_crust", OuterLandsWiringRules.eldritchBlock(4));
        assertEquals("thaumcraft:eldritch_crust", OuterLandsWiringRules.eldritchBlock(5));
        assertEquals("thaumcraft:eldritch_deco", OuterLandsWiringRules.eldritchBlock(6));
        assertEquals("thaumcraft:eldritch_doorway", OuterLandsWiringRules.eldritchBlock(7));
        assertEquals("thaumcraft:eldritch_doorway", OuterLandsWiringRules.eldritchBlock(8));
        assertEquals("thaumcraft:eldritch_spawner", OuterLandsWiringRules.eldritchBlock(9));
        assertEquals("thaumcraft:eldritch_spawner", OuterLandsWiringRules.eldritchBlock(10));
        assertEquals("thaumcraft:eldritch_crystal", OuterLandsWiringRules.eldritchBlock(11));
    }

    @Test void everyEldritchMetadataIsHostedByItsOwnBlock() {
        for (int variant = 0; variant <= 11; ++variant) {
            int group = EldritchBlockRules.eldritchGroup(variant);
            assertTrue(EldritchBlockRules.eldritchGroupVariants(group).contains(variant),
                    "the metadata " + variant + " must be hosted by the group " + group);
        }
        // Oracle BlockEldritch:151-153 - only these metadata values carry a block entity.
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_ALTAR));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_OBELISK));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_CAPSTONE));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_LOCK));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_CRAB_SPAWNER));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_RUNED));
        assertFalse(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertFalse(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_DECO));
    }

    @Test void everyCosmeticSolidMetadataLandsOnItsOwnBlock() {
        // Oracle BlockCosmeticSolid:213-253 - the obsidian tiles are thirty hard, the thaumium
        // stones four, the rest two.
        assertEquals("thaumcraft:cosmetic_solid_tough", OuterLandsWiringRules.cosmeticSolidBlock(0));
        assertEquals("thaumcraft:cosmetic_solid", OuterLandsWiringRules.cosmeticSolidBlock(1));
        assertEquals("thaumcraft:cosmetic_solid", OuterLandsWiringRules.cosmeticSolidBlock(3));
        assertEquals("thaumcraft:cosmetic_solid_thaumium", OuterLandsWiringRules.cosmeticSolidBlock(4));
        assertEquals("thaumcraft:cosmetic_solid_thaumium", OuterLandsWiringRules.cosmeticSolidBlock(6));
        assertEquals("thaumcraft:cosmetic_solid_thaumium", OuterLandsWiringRules.cosmeticSolidBlock(7));
        assertEquals("thaumcraft:cosmetic_solid_tough", OuterLandsWiringRules.cosmeticSolidBlock(8));
        assertEquals("thaumcraft:cosmetic_solid", OuterLandsWiringRules.cosmeticSolidBlock(11));
        assertEquals("thaumcraft:cosmetic_solid", OuterLandsWiringRules.cosmeticSolidBlock(15));
    }

    @Test void theRoomsOnlyUseCosmeticSolidsOfTheOrdinaryBlock() {
        // The labyrinth uses the metadata one, eleven, twelve, thirteen, fourteen and fifteen.
        for (int variant : new int[] {1, 11, 12, 13, 14, 15}) {
            assertEquals("thaumcraft:cosmetic_solid", OuterLandsWiringRules.cosmeticSolidBlock(variant),
                    "the metadata " + variant + " must stay on the ordinary cosmetic solid");
        }
    }

    @Test void everyAiryMetadataLandsOnItsOwnBlock() {
        // Oracle BlockAiry:190-213.
        assertEquals("thaumcraft:airy_node", OuterLandsWiringRules.airyBlock(0));
        assertEquals("thaumcraft:airy", OuterLandsWiringRules.airyBlock(1));
        assertEquals("thaumcraft:airy", OuterLandsWiringRules.airyBlock(4));
        assertEquals("thaumcraft:airy_node", OuterLandsWiringRules.airyBlock(5));
        assertEquals("thaumcraft:airy_hole", OuterLandsWiringRules.airyBlock(10));
        assertEquals("thaumcraft:airy_hole", OuterLandsWiringRules.airyBlock(11));
        assertEquals("thaumcraft:airy_unbreakable", OuterLandsWiringRules.airyBlock(12));
    }

    @Test void theInvisibleDoorOfTheRoomsIsTheUnbreakableAiry() {
        // Oracle GenCommon:88-92 - the void door is the airy block with the metadata twelve.
        assertEquals(EldritchBlockRules.AIRY_UNBREAKABLE, 12);
        assertEquals("thaumcraft:airy_unbreakable", OuterLandsWiringRules.airyBlock(12));
    }

    @Test void theStoneSlabKeepsItsVariantAndItsHalf() {
        // Oracle GenLibraryRoom:78-98 - the desk slab is the metadata one below and the
        // metadata nine above, which is the metadata one as the upper half.
        assertEquals("thaumcraft:cosmetic_stone_slab_eldritch", OuterLandsWiringRules.stoneSlabBlock(1));
        assertEquals("thaumcraft:cosmetic_stone_slab_eldritch", OuterLandsWiringRules.stoneSlabBlock(9));
        assertEquals("thaumcraft:cosmetic_stone_slab_arcane", OuterLandsWiringRules.stoneSlabBlock(0));
        assertEquals("thaumcraft:cosmetic_stone_slab_arcane", OuterLandsWiringRules.stoneSlabBlock(8));
        assertEquals(1, OuterLandsWiringRules.slabVariant(1));
        assertEquals(1, OuterLandsWiringRules.slabVariant(9));
        assertEquals(0, OuterLandsWiringRules.slabVariant(8));
        assertFalse(OuterLandsWiringRules.slabUpper(1));
        assertTrue(OuterLandsWiringRules.slabUpper(9));
        assertTrue(OuterLandsWiringRules.slabUpper(8));
    }

    @Test void theStairMetadataKeepsItsFacingAndItsHalf() {
        // Oracle GenCommon:151-194 - the 1.7 stair metadata holds the facing in the lower two
        // bits and the upside down half in the third bit.
        assertEquals(Direction.EAST, OuterLandsWiringRules.stairsFacing(0));
        assertEquals(Direction.WEST, OuterLandsWiringRules.stairsFacing(1));
        assertEquals(Direction.SOUTH, OuterLandsWiringRules.stairsFacing(2));
        assertEquals(Direction.NORTH, OuterLandsWiringRules.stairsFacing(3));
        assertEquals(Direction.EAST, OuterLandsWiringRules.stairsFacing(4));
        assertEquals(Direction.SOUTH, OuterLandsWiringRules.stairsFacing(6));
        assertFalse(OuterLandsWiringRules.stairsUpper(3));
        assertTrue(OuterLandsWiringRules.stairsUpper(4));
        assertTrue(OuterLandsWiringRules.stairsUpper(7));
    }

    @Test void theBannerFacingNamesTheSideOfTheCloth() {
        // Oracle BlockWoodenDevice:264-290 - the value zero hangs the cloth on the north side,
        // four on the east side, eight on the south side and twelve on the west side.
        assertEquals(Direction.NORTH, OuterLandsWiringRules.bannerDirection(0));
        assertEquals(Direction.EAST, OuterLandsWiringRules.bannerDirection(4));
        assertEquals(Direction.SOUTH, OuterLandsWiringRules.bannerDirection(8));
        assertEquals(Direction.WEST, OuterLandsWiringRules.bannerDirection(12));
    }

    @Test void theBannerOffsetsFollowTheOriginalRing() {
        // Oracle WorldGenEldritchRing:97-101 - the banners stand three blocks away from the
        // altar, in the direction of the forge direction of the loop.
        assertArrayEquals(new int[] {0, -3}, OuterLandsWiringRules.bannerOffset(2));
        assertArrayEquals(new int[] {0, 3}, OuterLandsWiringRules.bannerOffset(3));
        assertArrayEquals(new int[] {3, 0}, OuterLandsWiringRules.bannerOffset(4));
        assertArrayEquals(new int[] {-3, 0}, OuterLandsWiringRules.bannerOffset(5));
    }

    @Test void theCrystalKeepsTheOriginalDirectionNumbering() {
        // Oracle GenCommon:296-299 and GenNestRoom:66-68 - the crystal points down in the nest
        // room and into the free neighbour of a common decoration.
        assertEquals(Direction.DOWN, OuterLandsWiringRules.crystalDirection(0));
        assertEquals(Direction.UP, OuterLandsWiringRules.crystalDirection(1));
        assertEquals(Direction.NORTH, OuterLandsWiringRules.crystalDirection(2));
        assertEquals(Direction.SOUTH, OuterLandsWiringRules.crystalDirection(3));
        assertEquals(Direction.WEST, OuterLandsWiringRules.crystalDirection(4));
        assertEquals(Direction.EAST, OuterLandsWiringRules.crystalDirection(5));
    }

    @Test void theLootRarityStaysInsideTheNativeRange() {
        assertEquals(0, OuterLandsWiringRules.lootRarity(0));
        assertEquals(1, OuterLandsWiringRules.lootRarity(1));
        assertEquals(2, OuterLandsWiringRules.lootRarity(2));
        assertEquals(2, OuterLandsWiringRules.lootRarity(7));
        assertEquals(0, OuterLandsWiringRules.lootRarity(-1));
    }

    @Test void theLabyrinthOfTheAltarKeepsTheOriginalSize() {
        // Oracle TileEldritchAltar:217-226 - fifteen plus a roll of eight times two.
        assertEquals(15, OuterLandsWiringRules.altarMazeSize(0));
        assertEquals(29, OuterLandsWiringRules.altarMazeSize(7));
        for (int roll = 0; roll < 8; ++roll) {
            int size = OuterLandsWiringRules.altarMazeSize(roll);
            assertTrue(size >= 15 && size <= 29);
            assertEquals(1, size % 2);
        }
    }

    @Test void theGateSearchKeepsTheOriginalRadius() {
        // Oracle TeleporterThaumcraft:64 - the search walks a square of one hundred and
        // twenty eight blocks around the player.
        assertEquals(128, OuterLandsWiringRules.PORTAL_SEARCH_RADIUS);
    }
}

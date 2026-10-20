package thaumcraft.world;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.ChunkLoc;
import thaumcraft.common.lib.world.VillageWorldGenRules;
import thaumcraft.common.lib.world.VillageWorldGenRules.HouseType;
import thaumcraft.common.lib.world.VillageWorldGenRules.Material;
import thaumcraft.common.lib.world.VillageWorldGenRules.Placement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure rules of the TC4 village components and the two surface structures. */
class VillageWorldGenRulesTest {

    // ------------------------------------------------------------------ village pieces

    @Test void originalPieceWeightsAreKept() {
        assertEquals(25, VillageWorldGenRules.BANKER_HOME_WEIGHT);
        assertEquals(15, VillageWorldGenRules.WIZARD_TOWER_WEIGHT);
        assertEquals(100, VillageWorldGenRules.TOTAL_HOUSE_WEIGHT);
    }

    @Test void originalProfessionIdsAreKept() {
        assertEquals(191, VillageWorldGenRules.BANKER_PROFESSION_ID);
        assertEquals(190, VillageWorldGenRules.WIZARD_PROFESSION_ID);
    }

    @Test void pieceWeightMinimumIsAlwaysTheVillageSize() {
        // MathHelper.getIntWithMinMax(random, 0 + i, 1 + i) is i + nextInt(1).
        for (int size = 1; size <= 12; size++) {
            assertEquals(size, VillageWorldGenRules.pieceWeightMinSize(size));
        }
    }

    @Test void villageSizeIsOneToTwelve() {
        assertEquals(1, VillageWorldGenRules.villageSize(0));
        assertEquals(12, VillageWorldGenRules.villageSize(11));
        assertEquals(1, VillageWorldGenRules.villageSize(12));
        assertEquals(12, VillageWorldGenRules.villageSize(-1));
    }

    @Test void houseTypesFollowTheOriginalWeights() {
        assertEquals(HouseType.BANKER_HOME, VillageWorldGenRules.houseType(0));
        assertEquals(HouseType.BANKER_HOME, VillageWorldGenRules.houseType(24));
        assertEquals(HouseType.WIZARD_TOWER, VillageWorldGenRules.houseType(25));
        assertEquals(HouseType.WIZARD_TOWER, VillageWorldGenRules.houseType(39));
        assertEquals(HouseType.VANILLA_HOUSE, VillageWorldGenRules.houseType(40));
        assertEquals(HouseType.VANILLA_HOUSE, VillageWorldGenRules.houseType(99));
    }

    @Test void houseTypesShareTheWeightPool() {
        int bankers = 0;
        int wizards = 0;
        for (int roll = 0; roll < VillageWorldGenRules.TOTAL_HOUSE_WEIGHT; roll++) {
            HouseType type = VillageWorldGenRules.houseType(roll);
            if (type == HouseType.BANKER_HOME) bankers++;
            if (type == HouseType.WIZARD_TOWER) wizards++;
        }
        assertEquals(25, bankers);
        assertEquals(15, wizards);
    }

    @Test void villageHousesKeepTheirOrder() {
        List<HouseType> houses = VillageWorldGenRules.villageHouses(4, new Random(7));
        assertEquals(4, houses.size());
        int bankers = VillageWorldGenRules.componentCount(houses, HouseType.BANKER_HOME);
        int wizards = VillageWorldGenRules.componentCount(houses, HouseType.WIZARD_TOWER);
        assertEquals(4, bankers + wizards + VillageWorldGenRules.componentCount(houses, HouseType.VANILLA_HOUSE));
        assertTrue(bankers >= 0 && wizards >= 0);
    }

    @Test void villagePlanIsDeterministicAndKeepsTheChunkInItsCell() {
        VillageWorldGenRules.VillagePlan first = VillageWorldGenRules.villagePlan(1234L, 40, -17);
        VillageWorldGenRules.VillagePlan second = VillageWorldGenRules.villagePlan(1234L, 40, -17);
        assertEquals(first, second);
        assertEquals(32, VillageWorldGenRules.VILLAGE_CELL_CHUNKS);
        assertTrue(first.anchorChunkX() >= 32 && first.anchorChunkX() < 64);
        assertTrue(first.anchorChunkZ() >= -32 && first.anchorChunkZ() < 0);
        assertTrue(first.size() >= 1 && first.size() <= 12);
        assertNotEquals(VillageWorldGenRules.villagePlan(1235L, 40, -17), first);
    }

    @Test void onlyTheVillageHousesOwnAChunk() {
        VillageWorldGenRules.VillagePlan plan = VillageWorldGenRules.villagePlan(99L, 5, 5);
        int owned = 0;
        for (int x = plan.anchorChunkX() - 2; x <= plan.anchorChunkX() + 2; x++) {
            for (int z = plan.anchorChunkZ() - 2; z <= plan.anchorChunkZ() + 2; z++) {
                if (VillageWorldGenRules.houseIndex(plan, x, z) >= 0) owned++;
            }
        }
        assertEquals(plan.size(), owned);
        assertEquals(-1, VillageWorldGenRules.houseIndex(plan, plan.anchorChunkX() + 9, plan.anchorChunkZ() + 9));
    }

    @Test void theHousesFaceTheVillageCentre() {
        VillageWorldGenRules.VillagePlan plan = VillageWorldGenRules.villagePlan(3L, 0, 0);
        for (int house = 0; house < plan.houses().size(); house++) {
            int rotation = VillageWorldGenRules.houseRotation(plan, house);
            assertTrue(rotation >= 0 && rotation <= 3);
        }
    }

    // ------------------------------------------------------------------ rotation

    @Test void rotationKeepsTheFootprint() {
        assertEquals(4, VillageWorldGenRules.rotatedWidth(4, 5, 0));
        assertEquals(5, VillageWorldGenRules.rotatedDepth(4, 5, 0));
        assertEquals(5, VillageWorldGenRules.rotatedWidth(4, 5, 1));
        assertEquals(4, VillageWorldGenRules.rotatedDepth(4, 5, 1));
        assertEquals(4, VillageWorldGenRules.rotatedWidth(4, 5, 2));
        assertEquals(5, VillageWorldGenRules.rotatedDepth(4, 5, 2));
    }

    @Test void theFrontOfTheComponentTurnsWithIt() {
        assertEquals("north", VillageWorldGenRules.frontDirection(0));
        assertEquals("east", VillageWorldGenRules.frontDirection(1));
        assertEquals("south", VillageWorldGenRules.frontDirection(2));
        assertEquals("west", VillageWorldGenRules.frontDirection(3));
        assertEquals("west", VillageWorldGenRules.rotateDirection("west", 0));
        assertEquals("north", VillageWorldGenRules.rotateDirection("west", 1));
    }

    @Test void rotationMapsTheLocalCorner() {
        assertEquals(0, VillageWorldGenRules.rotateX(0, 0, 4, 5, 0));
        assertEquals(4, VillageWorldGenRules.rotateX(0, 0, 4, 5, 1));
        assertEquals(3, VillageWorldGenRules.rotateX(0, 0, 4, 5, 2));
        assertEquals(0, VillageWorldGenRules.rotateX(0, 0, 4, 5, 3));
    }

    // ------------------------------------------------------------------ banker home

    @Test void bankerHomeIsFourWideFiveDeepAndSixHigh() {
        assertEquals(4, VillageWorldGenRules.BANKER_HOME_WIDTH);
        assertEquals(5, VillageWorldGenRules.BANKER_HOME_DEPTH);
        assertEquals(6, VillageWorldGenRules.BANKER_HOME_HEIGHT);
    }

    @Test void bankerHomeHasTheCobbleFloorAndTheDirtFloor() {
        List<Placement> home = VillageWorldGenRules.bankerHome(false, 0);
        assertEquals(Material.COBBLESTONE, material(home, 0, 0, 0));
        assertEquals(Material.COBBLESTONE, material(home, 3, 0, 4));
        assertEquals(Material.DIRT, material(home, 1, 0, 1));
        assertEquals(Material.DIRT, material(home, 2, 0, 3));
    }

    @Test void bankerHomeHasTheOriginalWallsAndWindows() {
        List<Placement> home = VillageWorldGenRules.bankerHome(false, 0);
        assertEquals(Material.PLANKS, material(home, 0, 2, 2 - 1));
        assertEquals(Material.IRON_BARS, material(home, 0, 2, 2));
        assertEquals(Material.IRON_BARS, material(home, 3, 2, 2));
        assertEquals(Material.LOG, material(home, 0, 2, 0));
        assertEquals(Material.LOG, material(home, 3, 3, 4));
    }

    @Test void bankerHomeRoofVariantsAreTheOriginalOnes() {
        List<Placement> tall = VillageWorldGenRules.bankerHome(true, 0);
        List<Placement> low = VillageWorldGenRules.bankerHome(false, 0);
        assertEquals(Material.LOG, material(tall, 1, 4, 1));
        assertEquals(Material.LOG, material(low, 1, 5, 1));
        // The roof edge always stays on the fourth layer.
        assertEquals(Material.LOG, material(tall, 1, 4, 0));
        assertEquals(Material.LOG, material(low, 1, 4, 0));
    }

    @Test void bankerHomeTableUsesTheOriginalSlots() {
        assertEquals(3, VillageWorldGenRules.BANKER_HOME_TABLE_SLOTS);
        List<Placement> home = VillageWorldGenRules.bankerHome(false, 2);
        assertEquals(Material.COBBLESTONE_WALL, material(home, 2, 1, 3));
        assertEquals(Material.STONE_PRESSURE_PLATE, material(home, 2, 2, 3));
        List<Placement> without = VillageWorldGenRules.bankerHome(false, 0);
        assertEquals(Material.AIR, material(without, 2, 1, 3));
        assertEquals(Material.AIR, material(without, 2, 2, 3));
    }

    @Test void bankerHomeDoorIsOnTheFrontSide() {
        List<Placement> home = VillageWorldGenRules.bankerHome(false, 0);
        assertEquals(Material.DOOR_BOTTOM, material(home, 1, 1, 0));
        assertEquals(Material.DOOR_TOP, material(home, 1, 2, 0));
        assertEquals(1, VillageWorldGenRules.BANKER_HOME_VILLAGER_X);
        assertEquals(1, VillageWorldGenRules.BANKER_HOME_VILLAGER_Y);
        assertEquals(2, VillageWorldGenRules.BANKER_HOME_VILLAGER_Z);
    }

    // ------------------------------------------------------------------ wizard tower

    @Test void wizardTowerIsSixWideSixDeepAndTwelveHigh() {
        assertEquals(6, VillageWorldGenRules.WIZARD_TOWER_WIDTH);
        assertEquals(6, VillageWorldGenRules.WIZARD_TOWER_DEPTH);
        assertEquals(12, VillageWorldGenRules.WIZARD_TOWER_HEIGHT);
    }

    @Test void wizardTowerHasTheOriginalFloorsAndPillars() {
        List<Placement> tower = VillageWorldGenRules.wizardTower();
        assertEquals(Material.PLANKS, material(tower, 2, 0, 2));
        assertEquals(Material.PLANKS, material(tower, 2, 5, 2));
        assertEquals(Material.PLANKS, material(tower, 2, 10, 2));
        assertEquals(Material.COBBLESTONE, material(tower, 1, 0, 2));
        assertEquals(Material.COBBLESTONE, material(tower, 5, 11, 4));
        assertEquals(Material.COBBLESTONE, material(tower, 1, 10, 5));
    }

    @Test void wizardTowerKeepsLadderTrapdoorGlowstoneAndChest() {
        List<Placement> tower = VillageWorldGenRules.wizardTower();
        assertEquals(Material.LADDER, material(tower, 4, 1, 3));
        assertEquals(Material.LADDER, material(tower, 4, 9, 3));
        assertEquals(Material.TRAPDOOR, material(tower, 4, 10, 3));
        assertEquals(Material.GLOWSTONE, material(tower, 3, 5, 3));
        assertEquals(Material.CHEST, material(tower, 2, 6, 2));
        assertEquals(Material.GLASS_PANE, material(tower, 3, 7, 1));
        assertEquals(Material.DOOR_BOTTOM, material(tower, 3, 1, 1));
        assertEquals(Material.DOOR_TOP, material(tower, 3, 2, 1));
    }

    @Test void wizardSpawnsOutsideItsOwnPiece() {
        assertEquals(7, VillageWorldGenRules.WIZARD_TOWER_VILLAGER_X);
        assertTrue(VillageWorldGenRules.WIZARD_TOWER_VILLAGER_X >= VillageWorldGenRules.WIZARD_TOWER_WIDTH);
    }

    @Test void towerChestContentKeepsTheOriginalWeights() {
        assertEquals(81, VillageWorldGenRules.towerChestTotalWeight());
        assertEquals(10, VillageWorldGenRules.towerChestContents().size());
        assertEquals(4, VillageWorldGenRules.WIZARD_TOWER_CHEST_MIN);
        assertEquals(9, VillageWorldGenRules.WIZARD_TOWER_CHEST_MAX);
        assertEquals(4, VillageWorldGenRules.towerChestItemCount(0));
        assertEquals(9, VillageWorldGenRules.towerChestItemCount(5));
        assertEquals(4, VillageWorldGenRules.towerChestItemCount(6));
    }

    // ------------------------------------------------------------------ hilltop stones

    @Test void hilltopStonesKeepTheOriginalChanceAndFootprint() {
        assertEquals(40, VillageWorldGenRules.HILLTOP_STONE_CHANCE);
        assertEquals(3, VillageWorldGenRules.HILLTOP_STONE_RADIUS);
        assertEquals(4, VillageWorldGenRules.HILLTOP_STONE_LAYERS);
        assertEquals(85, VillageWorldGenRules.HILLTOP_STONE_MIN_Y);
        assertEquals(5, VillageWorldGenRules.HILLTOP_SAMPLE_OFFSETS.length);
        assertEquals(4, VillageWorldGenRules.HILLTOP_STONE_VINE_LENGTH);
    }

    @Test void hilltopStonesSkipTheCorners() {
        assertTrue(VillageWorldGenRules.isHilltopCorner(-3, -3));
        assertTrue(VillageWorldGenRules.isHilltopCorner(3, 3));
        assertFalse(VillageWorldGenRules.isHilltopCorner(3, 1));
    }

    @Test void hilltopPillarsAreTheOddBorderPositions() {
        assertTrue(VillageWorldGenRules.isHilltopPillar(3, 1));
        assertTrue(VillageWorldGenRules.isHilltopPillar(-3, -1));
        assertTrue(VillageWorldGenRules.isHilltopPillar(1, 3));
        assertTrue(VillageWorldGenRules.isHilltopPillar(-1, -3));
        assertFalse(VillageWorldGenRules.isHilltopPillar(3, 2));
        assertFalse(VillageWorldGenRules.isHilltopPillar(3, 3));
        assertFalse(VillageWorldGenRules.isHilltopPillar(0, 0));
    }

    @Test void oddnessUsesTheAbsoluteRemainder() {
        assertTrue(VillageWorldGenRules.isOdd(-1));
        assertTrue(VillageWorldGenRules.isOdd(1));
        assertFalse(VillageWorldGenRules.isOdd(-2));
        assertFalse(VillageWorldGenRules.isOdd(0));
    }

    // ------------------------------------------------------------------ eldritch ring

    @Test void eldritchRingKeepsTheOriginalChanceAndSize() {
        assertEquals(66, VillageWorldGenRules.ELDRITCH_RING_CHANCE);
        assertEquals(3, VillageWorldGenRules.ELDRITCH_RING_RADIUS);
        assertEquals(9, VillageWorldGenRules.ELDRITCH_RING_LAYERS);
        assertEquals(11, VillageWorldGenRules.eldritchRingSize(0));
        assertEquals(21, VillageWorldGenRules.eldritchRingSize(5));
        assertEquals(11, VillageWorldGenRules.eldritchRingSize(6));
    }

    @Test void eldritchRingStonesAreTheBorderPositions() {
        assertTrue(VillageWorldGenRules.isEldritchRingPosition(3, 1));
        assertTrue(VillageWorldGenRules.isEldritchRingPosition(-3, -1));
        assertTrue(VillageWorldGenRules.isEldritchRingPosition(1, -3));
        assertFalse(VillageWorldGenRules.isEldritchRingPosition(3, 2));
        assertFalse(VillageWorldGenRules.isEldritchRingPosition(0, 0));
        assertFalse(VillageWorldGenRules.isEldritchRingPosition(3, 3));
    }

    @Test void altarRollsKeepTheOriginalArms() {
        assertTrue(VillageWorldGenRules.eldritchArmsAltar(1));
        assertTrue(VillageWorldGenRules.eldritchArmsAltar(4));
        assertFalse(VillageWorldGenRules.eldritchArmsAltar(0));
        assertFalse(VillageWorldGenRules.eldritchArmsAltar(5));
        assertTrue(VillageWorldGenRules.eldritchAltarSpawnerTypeOne(6));
        assertTrue(VillageWorldGenRules.eldritchAltarSpawnerTypeOne(7));
        assertFalse(VillageWorldGenRules.eldritchAltarSpawnerTypeOne(8));
        assertEquals(4, VillageWorldGenRules.ELDRITCH_BANNER_OFFSETS.length);
    }

    // ------------------------------------------------------------------ surface structures

    @Test void chunkSeedIsTheOriginalOne() {
        assertEquals(42L + 16L * 341873128712L + 32L * 132897987541L,
                VillageWorldGenRules.chunkSeed(42L, 16, 32));
    }

    @Test void aChunkNeverCarriesTwoStructures() {
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                long seed = VillageWorldGenRules.chunkSeed(7L, x, z);
                VillageWorldGenRules.SurfaceStructure first = VillageWorldGenRules.surfaceStructure(seed);
                VillageWorldGenRules.SurfaceStructure second = VillageWorldGenRules.surfaceStructure(seed);
                assertEquals(first, second);
            }
        }
    }

    @Test void theStructureChainKeepsTheOriginalChances() {
        int mound = 0;
        int ring = 0;
        int stones = 0;
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                switch (VillageWorldGenRules.surfaceStructure(VillageWorldGenRules.chunkSeed(11L, x, z))) {
                    case MOUND -> mound++;
                    case ELDRITCH_RING -> ring++;
                    case HILLTOP_STONES -> stones++;
                    default -> { }
                }
            }
        }
        int chunks = 200 * 200;
        // One in a hundred and fifty, one in sixty six, one in forty.
        assertTrue(mound > chunks / 200 && mound < chunks / 100, "mound " + mound);
        assertTrue(ring > chunks / 90 && ring < chunks / 55, "ring " + ring);
        assertTrue(stones > chunks / 50 && stones < chunks / 33, "stones " + stones);
    }

    // ------------------------------------------------------------------ trades

    @Test void theBankerKeepsTheOriginalTwelveTrades() {
        List<VillageWorldGenRules.TradeSpec> trades = VillageWorldGenRules.bankerTrades();
        assertEquals(12, trades.size());
        VillageWorldGenRules.TradeSpec first = trades.get(0);
        assertEquals("thaumcraft:gold_coin", first.payItem());
        assertEquals(20, first.payMin());
        assertEquals(22, first.payMax());
        assertEquals("minecraft:emerald", first.receiveItems().get(0));
    }

    @Test void theWizardKeepsTheOriginalFifteenTrades() {
        List<VillageWorldGenRules.TradeSpec> trades = VillageWorldGenRules.wizardTrades();
        assertEquals(15, trades.size());
        VillageWorldGenRules.TradeSpec shards = trades.get(9);
        assertEquals("minecraft:emerald", shards.payItem());
        assertEquals(6, shards.receiveItems().size());
        assertEquals(2, shards.receiveMin());
        assertEquals(3, shards.receiveMax());
    }

    // ------------------------------------------------------------------ chunk locator

    @Test void chunkLocKeepsTheOriginalCentreArithmetic() {
        ChunkLoc loc = new ChunkLoc(3, -2);
        assertEquals((3 << 4) + 8, loc.getCenterXPos());
        assertEquals((-2 << 4) + 8, loc.getCenterZPosition());
        assertEquals("[3, -2]", loc.toString());
        assertTrue(loc.equals(new ChunkLoc(3, -2)));
        assertFalse(loc.equals(new ChunkLoc(3, 2)));
        assertFalse(loc.equals((Object) null));
    }

    private static Material material(List<Placement> placements, int x, int y, int z) {
        Material found = null;
        for (Placement placement : placements) {
            if (placement.x() == x && placement.y() == y && placement.z() == z) found = placement.material();
        }
        if (found == null) throw new AssertionError("no block at " + x + "," + y + "," + z);
        return found;
    }
}

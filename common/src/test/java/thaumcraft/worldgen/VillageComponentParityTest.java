package thaumcraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageWizardManager;
import thaumcraft.common.lib.world.VillageWorldGenRules;
import thaumcraft.common.lib.world.VillageWorldGenRules.Material;
import thaumcraft.common.lib.world.VillageWorldGenRules.Placement;

/**
 * The two TC4 village components and the piece weight of the original.
 *
 * <p>Every expectation is transcribed from the readable 1.7.10 reference
 * {@code /home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/lib/world}:
 * <ul>
 *   <li>{@code ComponentBankerHome:41-105} - the banker home,</li>
 *   <li>{@code ComponentWizardTower:29-96} - the wizard tower,</li>
 *   <li>{@code VillageBankerManager:38} and {@code VillageWizardManager:38} - the piece weight
 *       and the minimum village size draw.</li>
 * </ul>
 *
 * <p>The 1.7.10 {@code StructureComponent.placeBlockAtCurrentPosition} takes the metadata
 * first and the local coordinates after it, so the corner list of the tower is the list of
 * {@code ComponentWizardTower:53-64}.
 */
class VillageComponentParityTest {

    /**
     * The effective material of a local coordinate. The native port writes the placements in
     * the order of the original, so the last placement of a position is the one that stays.
     */
    private static Material material(List<Placement> placements, int x, int y, int z) {
        Material found = null;
        for (Placement placement : placements) {
            if (placement.x() == x && placement.y() == y && placement.z() == z) found = placement.material();
        }
        return found;
    }

    // ------------------------------------------------------------------ banker home

    @Test void theBankerHomeKeepsItsOriginalShell() {
        List<Placement> home = VillageWorldGenRules.bankerHome(false, 0);
        // Oracle ComponentBankerHome:56 - the cobble floor of the four by five footprint.
        assertEquals(Material.COBBLESTONE, material(home, 0, 0, 0));
        assertEquals(Material.COBBLESTONE, material(home, 3, 0, 4));
        // Oracle ComponentBankerHome:58 - the dirt floor of the inside.
        assertEquals(Material.DIRT, material(home, 1, 0, 1));
        assertEquals(Material.DIRT, material(home, 2, 0, 3));
        // Oracle ComponentBankerHome:62 - the low roof of the not tall house.
        assertEquals(Material.LOG, material(home, 1, 5, 1));
        assertEquals(Material.LOG, material(home, 2, 5, 3));
        // Oracle ComponentBankerHome:60 - the tall roof of the tall house.
        List<Placement> tall = VillageWorldGenRules.bankerHome(true, 0);
        assertEquals(Material.LOG, material(tall, 1, 4, 1));
        assertEquals(Material.LOG, material(tall, 2, 4, 3));
        // Oracle ComponentBankerHome:75-78 - the four log corner posts.
        for (int y = 1; y <= 3; y++) {
            assertEquals(Material.LOG, material(home, 0, y, 0));
            assertEquals(Material.LOG, material(home, 3, y, 0));
            assertEquals(Material.LOG, material(home, 0, y, 4));
            assertEquals(Material.LOG, material(home, 3, y, 4));
        }
        // Oracle ComponentBankerHome:83-84 - the two windows.
        assertEquals(Material.IRON_BARS, material(home, 0, 2, 2));
        assertEquals(Material.IRON_BARS, material(home, 3, 2, 2));
        // Oracle ComponentBankerHome:92 - the door.
        assertEquals(Material.DOOR_BOTTOM, material(home, 1, 1, 0));
        assertEquals(Material.DOOR_TOP, material(home, 1, 2, 0));
    }

    @Test void theBankerHomeKeepsItsThreeTableSlots() {
        // Oracle ComponentBankerHome:85-88 - the table only exists for the slots one and two.
        List<Placement> noTable = VillageWorldGenRules.bankerHome(false, 0);
        assertEquals(Material.AIR, material(noTable, 1, 1, 3));
        assertEquals(Material.AIR, material(noTable, 2, 1, 3));
        List<Placement> withTable = VillageWorldGenRules.bankerHome(false, 1);
        assertEquals(Material.COBBLESTONE_WALL, material(withTable, 1, 1, 3));
        assertEquals(Material.STONE_PRESSURE_PLATE, material(withTable, 1, 2, 3));
        List<Placement> secondTable = VillageWorldGenRules.bankerHome(false, 2);
        assertEquals(Material.COBBLESTONE_WALL, material(secondTable, 2, 1, 3));
        assertEquals(Material.STONE_PRESSURE_PLATE, material(secondTable, 2, 2, 3));
        assertEquals(3, VillageWorldGenRules.BANKER_HOME_TABLE_SLOTS);
    }

    // ------------------------------------------------------------------ wizard tower

    @Test void theWizardTowerKeepsTheOriginalCornersIncludingTheMissingGroundFloorPair() {
        List<Placement> tower = VillageWorldGenRules.wizardTower();
        // Oracle ComponentWizardTower:53-56 - the two corners of the ground floor at local x 1.
        assertEquals(Material.COBBLESTONE, material(tower, 1, 0, 1));
        assertEquals(Material.COBBLESTONE, material(tower, 1, 0, 5));
        // Oracle ComponentWizardTower:57-60 repeat the two corners of the middle floor at local
        // x 1, so the original never places the two ground floor corners at local x 5.
        assertNull(material(tower, 5, 0, 1), "the original leaves the ground floor corner at x 5 open");
        assertNull(material(tower, 5, 0, 5), "the original leaves the ground floor corner at x 5 open");
        // Oracle ComponentWizardTower:57-64 - the four corners of the middle and the top floor.
        assertEquals(Material.COBBLESTONE, material(tower, 1, 5, 1));
        assertEquals(Material.COBBLESTONE, material(tower, 1, 5, 5));
        assertEquals(Material.COBBLESTONE, material(tower, 5, 5, 1));
        assertEquals(Material.COBBLESTONE, material(tower, 5, 5, 5));
        assertEquals(Material.COBBLESTONE, material(tower, 1, 10, 1));
        assertEquals(Material.COBBLESTONE, material(tower, 1, 10, 5));
        assertEquals(Material.COBBLESTONE, material(tower, 5, 10, 1));
        assertEquals(Material.COBBLESTONE, material(tower, 5, 10, 5));
    }

    @Test void theWizardTowerPlacesTenCornersAndNotTwelve() {
        int corners = 0;
        for (Placement placement : VillageWorldGenRules.wizardTower()) {
            if (placement.material() != Material.COBBLESTONE) continue;
            boolean corner = (placement.x() == 1 || placement.x() == 5)
                    && (placement.z() == 1 || placement.z() == 5)
                    && (placement.y() == 0 || placement.y() == 5 || placement.y() == 10);
            if (corner) corners++;
        }
        // Twelve placeBlockAtCurrentPosition calls, two of them a repeat of the middle floor.
        assertEquals(10, corners);
    }

    @Test void theWizardTowerKeepsItsOriginalFloorsAndPillars() {
        List<Placement> tower = VillageWorldGenRules.wizardTower();
        // Oracle ComponentWizardTower:45-47 - the three plank floors.
        assertEquals(Material.PLANKS, material(tower, 2, 0, 2));
        assertEquals(Material.PLANKS, material(tower, 4, 5, 4));
        assertEquals(Material.PLANKS, material(tower, 2, 10, 4));
        // Oracle ComponentWizardTower:48-51 - the four cobble pillars.
        assertEquals(Material.COBBLESTONE, material(tower, 1, 0, 2));
        assertEquals(Material.COBBLESTONE, material(tower, 5, 11, 4));
        assertEquals(Material.COBBLESTONE, material(tower, 2, 0, 1));
        assertEquals(Material.COBBLESTONE, material(tower, 4, 11, 5));
        // Oracle ComponentWizardTower:65-70 - the six glass panes.
        assertEquals(Material.GLASS_PANE, material(tower, 3, 7, 1));
        assertEquals(Material.GLASS_PANE, material(tower, 3, 8, 5));
        assertEquals(Material.GLASS_PANE, material(tower, 3, 3, 5));
        // Oracle ComponentWizardTower:71-77 - the ladder and its trapdoor.
        assertEquals(Material.LADDER, material(tower, 4, 1, 3));
        assertEquals(Material.LADDER, material(tower, 4, 9, 3));
        assertEquals(Material.TRAPDOOR, material(tower, 4, 10, 3));
        // Oracle ComponentWizardTower:78-80 - the glowstone and the chest.
        assertEquals(Material.GLOWSTONE, material(tower, 3, 5, 3));
        assertEquals(Material.CHEST, material(tower, 2, 6, 2));
        // Oracle ComponentWizardTower:83 - the door.
        assertEquals(Material.DOOR_BOTTOM, material(tower, 3, 1, 1));
        assertEquals(Material.DOOR_TOP, material(tower, 3, 2, 1));
    }

    @Test void theWizardNeverSpawnsBecauseTheOriginalSpawnsItOutsideItsPiece() {
        // Oracle ComponentWizardTower:95 - spawnVillagers(world, bb, 7, 1, 1, 1).
        assertEquals(7, VillageWorldGenRules.WIZARD_TOWER_VILLAGER_X);
        assertTrue(VillageWorldGenRules.WIZARD_TOWER_VILLAGER_X >= VillageWorldGenRules.WIZARD_TOWER_WIDTH);
    }

    // ------------------------------------------------------------------ piece weight

    @Test void thePieceWeightDrawIsTheVillageSizeOrOneMore() {
        // Oracle VillageBankerManager:59 and VillageWizardManager:56 use
        // MathHelper.getRandomIntegerInRange(random, 0 + i, 1 + i), and the 1.7.10 helper is
        // min + random.nextInt(max - min + 1), so the draw is i + nextInt(2): the two values i
        // and i + 1. The draw of the original is 0 or 1; the port folds any other draw by its
        // parity, so an even draw is the village size and an odd draw is the village size plus one.
        for (int size = 1; size <= 12; size++) {
            assertEquals(size, VillageWorldGenRules.pieceWeightMinSize(size, 0));
            assertEquals(size + 1, VillageWorldGenRules.pieceWeightMinSize(size, 1));
            assertEquals(size, VillageWorldGenRules.pieceWeightMinSize(size, 2));
            assertEquals(size + 1, VillageWorldGenRules.pieceWeightMinSize(size, 3));
            assertEquals(size + 1, VillageWorldGenRules.pieceWeightMinSize(size, -1));
            assertEquals(size, VillageWorldGenRules.pieceWeightMinSize(size, -2));
        }
    }

    @Test void theManagersExposeTheSamePieceWeightDraw() {
        for (int size = 1; size <= 12; size++) {
            assertEquals(VillageWorldGenRules.pieceWeightMinSize(size, 0),
                    VillageBankerManager.pieceWeightMinSize(size, 0));
            assertEquals(VillageWorldGenRules.pieceWeightMinSize(size, 1),
                    VillageWizardManager.pieceWeightMinSize(size, 1));
        }
        assertEquals(25, VillageBankerManager.PIECE_WEIGHT);
        assertEquals(15, VillageWizardManager.PIECE_WEIGHT);
        assertEquals(191, VillageBankerManager.PROFESSION_ID);
        assertEquals(190, VillageWizardManager.PROFESSION_ID);
    }

    @Test void theTradeTablesKeepTheOriginalRows() {
        // Oracle VillageBankerManager:41-55 - the twelve banker trades.
        List<VillageWorldGenRules.TradeSpec> banker = VillageBankerManager.trades();
        assertEquals(12, banker.size());
        assertEquals(20, banker.get(0).payMin());
        assertEquals(22, banker.get(0).payMax());
        assertEquals(7, banker.get(4).payMin());
        assertEquals(9, banker.get(4).payMax());
        assertEquals(List.of("minecraft:book"), banker.get(4).receiveItems());
        assertEquals(25, banker.get(11).payMin());
        assertEquals(32, banker.get(11).payMax());
        // Oracle VillageWizardManager:35-52 - the fifteen wizard trades. The original pays with
        // one item and receives one item, so the pay item is a single id and not a list.
        List<VillageWorldGenRules.TradeSpec> wizard = VillageWizardManager.trades();
        assertEquals(15, wizard.size());
        // Oracle VillageWizardManager:39 - the resource metadata three, the quicksilver.
        assertEquals("thaumcraft:quick_silver", wizard.get(2).payItem());
        // Oracle VillageWizardManager:41 - the resource metadata six, the amber.
        assertEquals("thaumcraft:amber_gem", wizard.get(4).payItem());
        // Oracle VillageWizardManager:43 - the chicken nugget of the original.
        assertEquals("thaumcraft:nugget_chicken", wizard.get(6).payItem());
        // Oracle VillageWizardManager:45 - the beef nugget of the original.
        assertEquals("thaumcraft:nugget_beef", wizard.get(8).payItem());
        // Oracle VillageWizardManager:46 - the six metadata values of the aspect shard.
        assertEquals(6, wizard.get(9).receiveItems().size());
        // Oracle VillageWizardManager:51 - the six metadata values of the bauble blank.
        assertEquals(6, wizard.get(14).receiveItems().size());
        assertNotNull(wizard.get(9).receiveItems());
        assertFalse(wizard.get(9).receiveItems().isEmpty());
    }
}

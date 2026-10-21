package thaumcraft.eldritch;

import java.util.List;
import java.util.Random;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchBlockRules;
import static org.junit.jupiter.api.Assertions.*;

/** Pure rule checks for the original TC4 eldritch, cosmetic and device families. */
class EldritchBlockRulesTest {
    private static final Random RANDOM = new Random(20261020L);

    // ------------------------------------------------------------------ eldritch
    @Test void eldritchLightMatchesTheOriginalMetadataTable() {
        assertEquals(8, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_ALTAR));
        assertEquals(8, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_OBELISK));
        assertEquals(8, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_CAPSTONE));
        assertEquals(12, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertEquals(12, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_GLYPHED));
        assertEquals(5, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_DECO));
        assertEquals(12, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_DOORWAY));
        assertEquals(5, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_LOCK));
        assertEquals(4, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_CRAB_SPAWNER));
        assertEquals(0, EldritchBlockRules.eldritchLight(EldritchBlockRules.ELDRITCH_RUNED));
    }

    @Test void eldritchHardnessKeepsTheUnbreakableDoorwayAndLock() {
        assertEquals(50.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_ALTAR));
        assertEquals(2.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertEquals(2.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_GLYPHED));
        assertEquals(4.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_DECO));
        assertEquals(-1.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_DOORWAY));
        assertEquals(-1.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_LOCK));
        assertEquals(15.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_CRAB_SPAWNER));
        assertEquals(15.0F, EldritchBlockRules.eldritchHardness(EldritchBlockRules.ELDRITCH_RUNED));
    }

    @Test void eldritchExplosionResistanceKeepsTheOriginalValues() {
        assertEquals(30.0F, EldritchBlockRules.eldritchExplosionResistance(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertEquals(100.0F, EldritchBlockRules.eldritchExplosionResistance(EldritchBlockRules.ELDRITCH_DECO));
        assertEquals(Float.MAX_VALUE, EldritchBlockRules.eldritchExplosionResistance(EldritchBlockRules.ELDRITCH_LOCK));
        // The 1.7.10 setter value 20000 is stored as 60000 and the vanilla getter divides it by five.
        assertEquals(12000.0F, EldritchBlockRules.eldritchExplosionResistance(EldritchBlockRules.ELDRITCH_ALTAR));
    }

    @Test void eldritchBlockEntitiesExistForExactlyTheOriginalMetadata() {
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_ALTAR));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_OBELISK));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_CAPSTONE));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_LOCK));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_CRAB_SPAWNER));
        assertTrue(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_RUNED));
        assertFalse(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertFalse(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_GLYPHED));
        assertFalse(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_DECO));
        assertFalse(EldritchBlockRules.eldritchHasTile(EldritchBlockRules.ELDRITCH_DOORWAY));
    }

    @Test void eldritchDropsAndExperienceMatchTheOriginal() {
        assertTrue(EldritchBlockRules.eldritchDropsSelf(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertFalse(EldritchBlockRules.eldritchDropsSelf(EldritchBlockRules.ELDRITCH_GLYPHED));
        assertTrue(EldritchBlockRules.eldritchDropsResource(EldritchBlockRules.ELDRITCH_GLYPHED));
        for (int i = 0; i < 64; i++) {
            int glyphed = EldritchBlockRules.eldritchExperience(EldritchBlockRules.ELDRITCH_GLYPHED, RANDOM);
            assertTrue(glyphed >= 1 && glyphed <= 4);
            int runed = EldritchBlockRules.eldritchExperience(EldritchBlockRules.ELDRITCH_RUNED, RANDOM);
            assertTrue(runed >= 1 && runed <= 4);
            int crab = EldritchBlockRules.eldritchExperience(EldritchBlockRules.ELDRITCH_CRAB_SPAWNER, RANDOM);
            assertTrue(crab >= 6 && crab <= 10);
            assertEquals(0, EldritchBlockRules.eldritchExperience(EldritchBlockRules.ELDRITCH_ALTAR, RANDOM));
        }
    }

    @Test void breakingAStructuralEldritchPieceRemovesTheWholeAltar() {
        assertTrue(EldritchBlockRules.eldritchBreaksStructure(EldritchBlockRules.ELDRITCH_ALTAR));
        assertTrue(EldritchBlockRules.eldritchBreaksStructure(EldritchBlockRules.ELDRITCH_OBELISK));
        assertTrue(EldritchBlockRules.eldritchBreaksStructure(EldritchBlockRules.ELDRITCH_CAPSTONE));
        assertFalse(EldritchBlockRules.eldritchBreaksStructure(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertEquals(3, EldritchBlockRules.eldritchStructureRadiusX());
        assertEquals(2, EldritchBlockRules.eldritchStructureRadiusY());
    }

    // ------------------------------------------------------------------- altar
    @Test void altarTurnsIntoAGuardianSpawnerAtTheThirdEye() {
        assertEquals(EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST, EldritchBlockRules.altarSpawnTypeForEyes(0));
        assertEquals(EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST, EldritchBlockRules.altarSpawnTypeForEyes(1));
        assertEquals(EldritchBlockRules.ALTAR_SPAWN_TYPE_GUARDIAN, EldritchBlockRules.altarSpawnTypeForEyes(2));
        assertEquals(EldritchBlockRules.ALTAR_SPAWN_TYPE_GUARDIAN, EldritchBlockRules.altarSpawnTypeForEyes(3));
    }

    @Test void altarAcceptsAtMostFourEyes() {
        assertTrue(EldritchBlockRules.altarAcceptsEye(0));
        assertTrue(EldritchBlockRules.altarAcceptsEye(3));
        assertFalse(EldritchBlockRules.altarAcceptsEye(4));
        assertEquals(1, EldritchBlockRules.altarEyesAfterActivation(0));
        assertEquals(4, EldritchBlockRules.altarEyesAfterActivation(3));
        assertEquals(4, EldritchBlockRules.altarEyesAfterActivation(4));
    }

    @Test void altarMazeSizeIsAlwaysOddAndBetweenFifteenAndTwentyNine() {
        for (int i = 0; i < 64; i++) {
            int size = EldritchBlockRules.altarMazeSize(RANDOM);
            assertTrue(size >= 15 && size <= 29);
            assertEquals(1, size % 2);
        }
    }

    @Test void altarClericsStandOnTheFourCorners() {
        assertEquals(-2, EldritchBlockRules.altarClericOffsetX(0));
        assertEquals(-2, EldritchBlockRules.altarClericOffsetZ(0));
        assertEquals(-2, EldritchBlockRules.altarClericOffsetX(1));
        assertEquals(2, EldritchBlockRules.altarClericOffsetZ(1));
        assertEquals(2, EldritchBlockRules.altarClericOffsetX(2));
        assertEquals(-2, EldritchBlockRules.altarClericOffsetZ(2));
        assertEquals(2, EldritchBlockRules.altarClericOffsetX(3));
        assertEquals(2, EldritchBlockRules.altarClericOffsetZ(3));
    }

    // ----------------------------------------------------------------- obelisk
    @Test void obeliskOnlyAffectsLivingEldritchMobsInRange() {
        assertEquals(6.0D, EldritchBlockRules.OBELISK_RANGE);
        assertEquals(40, EldritchBlockRules.OBELISK_EFFECT_DURATION);
        assertEquals(1, EldritchBlockRules.OBELISK_TICK_PERIOD);
        assertTrue(EldritchBlockRules.obeliskAffects(true, true));
        assertFalse(EldritchBlockRules.obeliskAffects(false, true));
        assertFalse(EldritchBlockRules.obeliskAffects(true, false));
    }

    // ----------------------------------------------------------------- portal
    @Test void portalCooldownAndTransferGuardMatchTheOriginal() {
        assertEquals(100, EldritchBlockRules.PORTAL_COOLDOWN);
        assertEquals(5, EldritchBlockRules.PORTAL_SCAN_PERIOD);
        assertEquals(250, EldritchBlockRules.PORTAL_SOUND_PERIOD);
        assertTrue(EldritchBlockRules.portalCanTransfer(false, false));
        assertFalse(EldritchBlockRules.portalCanTransfer(true, false));
        assertFalse(EldritchBlockRules.portalCanTransfer(false, true));
    }

    // ----------------------------------------------------------------- mirror
    @Test void mirrorLinkRetryGrowsToSixHundredTicks() {
        assertEquals(40, EldritchBlockRules.MIRROR_LINK_RETRY);
        assertEquals(60, EldritchBlockRules.mirrorLinkRetryInterval(40));
        assertEquals(600, EldritchBlockRules.mirrorLinkRetryInterval(600));
        assertEquals(600, EldritchBlockRules.mirrorLinkRetryInterval(580));
    }

    @Test void mirrorEjectsEveryTickWithoutInstabilityAndSlowlyWithIt() {
        assertEquals(0, EldritchBlockRules.mirrorEjectTickRate(0));
        assertEquals(0, EldritchBlockRules.mirrorEjectTickRate(49));
        assertEquals(1, EldritchBlockRules.mirrorEjectTickRate(50));
        assertEquals(4, EldritchBlockRules.mirrorEjectTickRate(100));
        assertTrue(EldritchBlockRules.mirrorShouldEject(7, 0));
        assertTrue(EldritchBlockRules.mirrorShouldEject(4, 100));
        assertFalse(EldritchBlockRules.mirrorShouldEject(3, 100));
    }

    @Test void mirrorEssentiaOnlyMovesASingleUnitAndDrainsAtMostEight() {
        assertTrue(EldritchBlockRules.mirrorEssentiaTakeAllowed(1));
        assertFalse(EldritchBlockRules.mirrorEssentiaTakeAllowed(2));
        assertEquals(8, EldritchBlockRules.mirrorEssentiaDrainAmount(64));
        assertEquals(3, EldritchBlockRules.mirrorEssentiaDrainAmount(3));
    }

    @Test void theEssentiaMirrorPaysFromTheSlabInFrontOfItsTwin() {
        // Oracle TileMirrorEssentia:194-201 - the network that pays is the slab
        // in front of the twin, eight blocks deep in the direction it looks at.
        assertArrayEquals(new int[] {0, 0, 0}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.NORTH, 0, 0, 0));
        assertArrayEquals(new int[] {3, -2, -1}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.NORTH, 3, -2, 1));
        assertArrayEquals(new int[] {2, 4, 1}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.SOUTH, 2, 4, 1));
        assertArrayEquals(new int[] {2, 1, 5}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.EAST, 1, 5, 2));
        assertArrayEquals(new int[] {-2, 1, 5}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.WEST, 1, 5, 2));
        assertArrayEquals(new int[] {2, 6, 3}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.UP, 2, 3, 6));
        assertArrayEquals(new int[] {2, -6, 3}, EldritchBlockRules.mirrorEssentiaSlabOffset(Direction.DOWN, 2, 3, 6));
    }

    @Test void theInstabilityIsPaidWithAtMostOneOrderVis() {
        // Oracle TileMirror:314 - the payment is min(instability, 1) ORDER vis.
        assertEquals(1, EldritchBlockRules.mirrorInstabilityVisPayment(1));
        assertEquals(1, EldritchBlockRules.mirrorInstabilityVisPayment(50));
        assertEquals(0, EldritchBlockRules.mirrorInstabilityVisPayment(0));
        assertEquals(0, EldritchBlockRules.mirrorInstabilityVisPayment(-3));
    }

    @Test void mirrorLinkIsOnlyValidWhenBothSidesPointAtEachOther() {
        assertTrue(EldritchBlockRules.mirrorLinksMatch(true, true, 1, 2, 3, 0, 1, 2, 3, 0));
        assertFalse(EldritchBlockRules.mirrorLinksMatch(false, true, 1, 2, 3, 0, 1, 2, 3, 0));
        assertFalse(EldritchBlockRules.mirrorLinksMatch(true, false, 1, 2, 3, 0, 1, 2, 3, 0));
        assertFalse(EldritchBlockRules.mirrorLinksMatch(true, true, 9, 2, 3, 0, 1, 2, 3, 0));
        assertFalse(EldritchBlockRules.mirrorLinksMatch(true, true, 1, 2, 3, 1, 1, 2, 3, 0));
    }

    // --------------------------------------------------------------- magic box
    @Test void magicBoxGrowsByTwentySevenSlotsPerLinkedBox() {
        assertEquals(27, EldritchBlockRules.magicBoxSize(0));
        assertEquals(54, EldritchBlockRules.magicBoxSize(1));
        assertEquals(27 * 1025, EldritchBlockRules.magicBoxSize(1024));
        assertEquals(64, EldritchBlockRules.MAGIC_BOX_MAX_STACK);
        assertEquals(1024, EldritchBlockRules.MAGIC_BOX_MAX_LINKED);
    }

    @Test void magicBoxSortingCyclesThroughTheOriginalThreeModes() {
        assertEquals(0, EldritchBlockRules.magicBoxNextSorting(EldritchBlockRules.MAGIC_BOX_SORT_NONE));
        assertEquals(1, EldritchBlockRules.magicBoxNextSorting(0));
        assertEquals(2, EldritchBlockRules.magicBoxNextSorting(1));
        assertEquals(-1, EldritchBlockRules.magicBoxNextSorting(2));
        assertTrue(EldritchBlockRules.magicBoxSortsByName(0));
        assertTrue(EldritchBlockRules.magicBoxSortsByName(1));
        assertFalse(EldritchBlockRules.magicBoxSortsByName(2));
        assertTrue(EldritchBlockRules.magicBoxSortsByMod(2));
    }

    // ---------------------------------------------------------- warding stones
    @Test void wardingStonePushesLivingNonPlayersOnly() {
        assertTrue(EldritchBlockRules.wardingStonePushes(false, false));
        assertFalse(EldritchBlockRules.wardingStonePushes(true, false));
        assertFalse(EldritchBlockRules.wardingStonePushes(false, true));
        assertEquals(0.2F, EldritchBlockRules.WARDING_STONE_PUSH);
        assertEquals(5, EldritchBlockRules.WARDING_STONE_PUSH_PERIOD);
        assertEquals(100, EldritchBlockRules.WARDING_STONE_SEAL_PERIOD);
    }

    @Test void wardingStoneFenceNeedsAStoneBelow() {
        assertTrue(EldritchBlockRules.wardingStoneFenceSupported(true, false));
        assertTrue(EldritchBlockRules.wardingStoneFenceSupported(false, true));
        assertFalse(EldritchBlockRules.wardingStoneFenceSupported(false, false));
    }

    // ------------------------------------------------------------------ banner
    @Test void bannerKeepsItsColourAspectAndWallData() {
        assertEquals(-1, EldritchBlockRules.BANNER_COLOUR_NONE);
        assertEquals("", EldritchBlockRules.bannerAspectTag(null));
        assertEquals("aer", EldritchBlockRules.bannerAspectTag("aer"));
        assertTrue(EldritchBlockRules.bannerHasContent(3, false));
        assertTrue(EldritchBlockRules.bannerHasContent(-1, true));
        assertFalse(EldritchBlockRules.bannerHasContent(-1, false));
    }

    // ---------------------------------------------------------- pressure plate
    @Test void arcanePressurePlateCyclesThroughThreeSensitivities() {
        assertEquals(1, EldritchBlockRules.plateNextSetting(0));
        assertEquals(2, EldritchBlockRules.plateNextSetting(1));
        assertEquals(0, EldritchBlockRules.plateNextSetting(2));
    }

    @Test void arcanePressurePlateFiltersOwnerAndDeadEntities() {
        // Everything.
        assertTrue(EldritchBlockRules.plateDetects(EldritchBlockRules.PLATE_SETTING_ALL, false, false, false, false));
        assertFalse(EldritchBlockRules.plateDetects(EldritchBlockRules.PLATE_SETTING_ALL, true, false, false, false));
        // Everything except the owner.
        assertFalse(EldritchBlockRules.plateDetects(EldritchBlockRules.PLATE_SETTING_NOT_OWNER, false, true, true, false));
        assertTrue(EldritchBlockRules.plateDetects(EldritchBlockRules.PLATE_SETTING_NOT_OWNER, false, true, false, false));
        // Just the owner.
        assertFalse(EldritchBlockRules.plateDetects(EldritchBlockRules.PLATE_SETTING_OWNER_ONLY, false, true, false, false));
        assertTrue(EldritchBlockRules.plateDetects(EldritchBlockRules.PLATE_SETTING_OWNER_ONLY, false, true, true, false));
    }

    // ---------------------------------------------------------------- brainbox
    @Test void brainboxHasToFaceTheMachineHalf() {
        assertFalse(EldritchBlockRules.brainboxFacesMachine(EldritchBlockRules.BRAINBOX_FACING_UNKNOWN, 2));
        assertTrue(EldritchBlockRules.brainboxFacesMachine(2, 2));
        assertFalse(EldritchBlockRules.brainboxFacesMachine(2, 3));
        assertTrue(EldritchBlockRules.brainboxHasRoom(true));
        assertFalse(EldritchBlockRules.brainboxHasRoom(false));
    }

    // ------------------------------------------------------------ flux scrubber
    @Test void fluxScrubberConvertsFourChargesIntoOneEssentia() {
        assertTrue(EldritchBlockRules.fluxScrubberSpendsCharge(4));
        assertFalse(EldritchBlockRules.fluxScrubberSpendsCharge(3));
        assertEquals(1, EldritchBlockRules.fluxScrubberEssentiaAfterCharge(0));
        assertEquals(4, EldritchBlockRules.fluxScrubberEssentiaAfterCharge(3));
        assertEquals(4, EldritchBlockRules.fluxScrubberEssentiaAfterCharge(4));
    }

    @Test void fluxScrubberRemovesOneFluxLevelAtATime() {
        assertEquals(7, EldritchBlockRules.fluxScrubberReducedLevel(8));
        assertEquals(0, EldritchBlockRules.fluxScrubberReducedLevel(1));
        assertEquals(-1, EldritchBlockRules.fluxScrubberReducedLevel(0));
        assertEquals(16, EldritchBlockRules.FLUX_SCAN_DISTANCE);
        assertEquals(5, EldritchBlockRules.FLUX_POWER_PER_BLOCK);
    }

    @Test void theFluxScrubberOnlyCleansTheSquaredRadiusOfSixteen() {
        // Oracle TileFluxScrubber:92 - only the flux whose squared distance to
        // the scrubber is below 256 is cleaned.
        assertTrue(EldritchBlockRules.fluxScrubberInRange(0, 0, 0));
        assertTrue(EldritchBlockRules.fluxScrubberInRange(15, 0, 0));
        assertTrue(EldritchBlockRules.fluxScrubberInRange(9, 9, 9));
        assertFalse(EldritchBlockRules.fluxScrubberInRange(16, 0, 0));
        assertFalse(EldritchBlockRules.fluxScrubberInRange(0, 0, 16));
        assertFalse(EldritchBlockRules.fluxScrubberInRange(12, 12, 12));
        assertFalse(EldritchBlockRules.fluxScrubberInRange(16, 16, 16));
    }

    @Test void theItemFilterIsTheOpenAndTheClosedGrate() {
        // Oracle EntityItemGrate:26-36 - the item filter is the metal device of
        // the fifth and the sixth metadata.
        assertTrue(EldritchBlockRules.itemFilterBlock(true, false, 0));
        assertTrue(EldritchBlockRules.itemFilterBlock(false, true, EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED));
        assertTrue(EldritchBlockRules.itemFilterBlock(false, true, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN));
        assertFalse(EldritchBlockRules.itemFilterBlock(false, true, 0));
        assertFalse(EldritchBlockRules.itemFilterBlock(false, false, EldritchBlockRules.DEVICE_METAL_GRATE_OPEN));
    }

    // ---------------------------------------------------------- ethereal bloom
    @Test void etherealBloomOnlyConvertsTheThreeOriginalBiomes() {
        assertTrue(EldritchBlockRules.etherealBloomConverts(true, false, false));
        assertTrue(EldritchBlockRules.etherealBloomConverts(false, true, false));
        assertTrue(EldritchBlockRules.etherealBloomConverts(false, false, true));
        assertFalse(EldritchBlockRules.etherealBloomConverts(false, false, false));
        assertEquals(81.0D, EldritchBlockRules.ETHEREAL_BLOOM_RADIUS_SQUARED);
    }

    // --------------------------------------------------------------------- jars
    @Test void jarFillSemanticsKeepTheOriginalCapacityAndSuction() {
        assertEquals(64, EldritchBlockRules.JAR_CAPACITY);
        assertEquals(32, EldritchBlockRules.jarSuctionAmount(0, 64, false, false));
        assertEquals(64, EldritchBlockRules.jarSuctionAmount(0, 64, true, false));
        assertEquals(0, EldritchBlockRules.jarSuctionAmount(64, 64, false, false));
        assertEquals(32, EldritchBlockRules.jarSuctionAmount(0, 64, false, true));
        assertEquals(48, EldritchBlockRules.jarSuctionAmount(0, 64, true, true));
        assertEquals(32, EldritchBlockRules.jarMinimumSuction(false, false));
        assertEquals(64, EldritchBlockRules.jarMinimumSuction(true, false));
        assertEquals(48, EldritchBlockRules.jarMinimumSuction(true, true));
    }

    @Test void jarOnlyAcceptsTheStoredAspectUnlessItIsEmpty() {
        assertEquals(10, EldritchBlockRules.jarAddedAmount(10, 0, 64, false, true));
        assertEquals(10, EldritchBlockRules.jarAddedAmount(10, 5, 64, true, false));
        assertEquals(0, EldritchBlockRules.jarAddedAmount(10, 5, 64, false, false));
        assertEquals(4, EldritchBlockRules.jarAddedAmount(10, 60, 64, true, false));
        assertEquals(0, EldritchBlockRules.jarAddedAmount(0, 0, 64, false, true));
    }

    @Test void voidJarClampsTheStoredAmount() {
        assertEquals(64, EldritchBlockRules.jarVoidStoredAmount(80, 64));
        assertEquals(10, EldritchBlockRules.jarVoidStoredAmount(10, 64));
    }

    // -------------------------------------------------------------------- loot
    @Test void lootContainerDropsOneToThreePlusRarityItems() {
        for (int i = 0; i < 64; i++) {
            int common = EldritchBlockRules.lootDropCount(0, RANDOM);
            assertTrue(common >= 1 && common <= 3);
            int rare = EldritchBlockRules.lootDropCount(2, RANDOM);
            assertTrue(rare >= 3 && rare <= 5);
        }
        assertEquals(0, EldritchBlockRules.lootRarity(-1));
        assertEquals(2, EldritchBlockRules.lootRarity(2));
        assertTrue(EldritchBlockRules.lootUsesRarity(1));
        assertFalse(EldritchBlockRules.lootUsesRarity(0));
    }

    // ------------------------------------------------------------- arcane door
    @Test void arcaneDoorTogglesItsOpenBitOnTheLowerHalf() {
        assertEquals(4, EldritchBlockRules.doorToggleOpen(0));
        assertEquals(0, EldritchBlockRules.doorToggleOpen(4));
        assertTrue(EldritchBlockRules.doorIsOpen(4));
        assertFalse(EldritchBlockRules.doorIsOpen(0));
        assertEquals(2, EldritchBlockRules.doorOrientation(6));
        assertTrue(EldritchBlockRules.doorIsFlipped(16));
        assertFalse(EldritchBlockRules.doorIsFlipped(0));
        assertEquals(9, EldritchBlockRules.doorUpperMetadata(1));
        assertEquals(8, EldritchBlockRules.doorUpperMetadata(0));
        assertEquals(4, EldritchBlockRules.doorLowerMetadata(12));
    }

    @Test void arcaneDoorOnlyOpensForTheOwnerAndTheAccessList() {
        assertTrue(EldritchBlockRules.doorAllows("bob", List.of(), "bob"));
        assertTrue(EldritchBlockRules.doorAllows("bob", List.of("0alice"), "alice"));
        assertTrue(EldritchBlockRules.doorAllows("bob", List.of("1alice"), "alice"));
        assertFalse(EldritchBlockRules.doorAllows("bob", List.of(), "alice"));
        assertFalse(EldritchBlockRules.doorAllows("bob", null, "alice"));
    }

    // ------------------------------------------------------------ device family
    @Test void deviceFamiliesOwnExactlyTheirOriginalMetadata() {
        assertEquals(EldritchBlockRules.DEVICE_KIND_GRATE, EldritchBlockRules.metalDeviceKind(5));
        assertEquals(EldritchBlockRules.DEVICE_KIND_GRATE, EldritchBlockRules.metalDeviceKind(6));
        assertEquals(EldritchBlockRules.DEVICE_KIND_BRAINBOX, EldritchBlockRules.metalDeviceKind(12));
        assertEquals(EldritchBlockRules.DEVICE_KIND_NONE, EldritchBlockRules.metalDeviceKind(0));

        assertEquals(EldritchBlockRules.DEVICE_KIND_PRESSURE_PLATE, EldritchBlockRules.woodenDeviceKind(2));
        assertEquals(EldritchBlockRules.DEVICE_KIND_PRESSURE_PLATE, EldritchBlockRules.woodenDeviceKind(3));
        assertEquals(EldritchBlockRules.DEVICE_KIND_BANNER, EldritchBlockRules.woodenDeviceKind(8));

        assertEquals(EldritchBlockRules.DEVICE_KIND_WAND_PEDESTAL, EldritchBlockRules.stoneDeviceKind(5));
        assertEquals(EldritchBlockRules.DEVICE_KIND_FLUX_SCRUBBER, EldritchBlockRules.stoneDeviceKind(14));
        assertEquals(EldritchBlockRules.DEVICE_KIND_NONE, EldritchBlockRules.stoneDeviceKind(0));
    }

    @Test void deviceFamilyHardnessAndResistanceMatchTheOriginal() {
        assertEquals(3.0F, EldritchBlockRules.DEVICE_METAL_HARDNESS);
        // The 1.7.10 setter values 17, 10 and 25 are stored three times and the getter divides by five.
        assertEquals(10.2F, EldritchBlockRules.DEVICE_METAL_RESISTANCE);
        // Oracle BlockWoodenDevice:82 - the hardness of the wooden device family is 2.5.
        assertEquals(2.5F, EldritchBlockRules.DEVICE_WOOD_HARDNESS);
        assertEquals(6.0F, EldritchBlockRules.DEVICE_WOOD_RESISTANCE);
        // Oracle BlockWoodenDevice:196-212 - the arcane pressure plate has the hardness two and
        // the absolute blast resistance 999, which its own getter returns and which is therefore
        // not scaled by the 0.6 factor of the 1.7.10 setter.
        assertEquals(2.0F, EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_HARDNESS);
        assertEquals(999.0F, EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_RESISTANCE);
        assertEquals(3.0F, EldritchBlockRules.DEVICE_STONE_HARDNESS);
        assertEquals(15.0F, EldritchBlockRules.DEVICE_STONE_RESISTANCE);
    }

    @Test void theWardedGlassKeepsItsOriginalHardnessAndBlastResistance() {
        // BlockCosmeticOpaque:253-266 - the warded glass has a hardness of five (or none
        // at all with the warded stone option) and always resists an explosion with 999.
        assertEquals(5.0F, EldritchBlockRules.COSMETIC_OPAQUE_WARDED_HARDNESS);
        assertEquals(999.0F, EldritchBlockRules.COSMETIC_OPAQUE_WARDED_RESISTANCE);
        // The amber block and the amber brick keep the hardness 1.5 and the effective
        // blast resistance 3 of the setter value 5.
        assertEquals(1.5F, EldritchBlockRules.COSMETIC_OPAQUE_HARDNESS);
        assertEquals(3.0F, EldritchBlockRules.COSMETIC_OPAQUE_RESISTANCE);
    }

    @Test void theEldritchFamilyKeepsTheEffectiveResistanceOfItsSetterValue() {
        // BlockEldritch:68 - setResistance(20000) stores 60000; the getter divides it by five.
        assertEquals(20000.0F, EldritchBlockRules.ELDRITCH_FAMILY_RESISTANCE);
        assertEquals(12000.0F, EldritchBlockRules.ELDRITCH_FAMILY_NATIVE_RESISTANCE);
    }

    // ---------------------------------------------------------------- cosmetic
    @Test void cosmeticSolidKeepsItsHardnessLightAndResistance() {
        assertEquals(30.0F, EldritchBlockRules.cosmeticHardness(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE));
        assertEquals(30.0F, EldritchBlockRules.cosmeticHardness(EldritchBlockRules.COSMETIC_OBSIDIAN_TOTEM_BASE));
        assertEquals(30.0F, EldritchBlockRules.cosmeticHardness(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED));
        assertEquals(4.0F, EldritchBlockRules.cosmeticHardness(EldritchBlockRules.COSMETIC_THAUMIUM_BLOCK));
        assertEquals(4.0F, EldritchBlockRules.cosmeticHardness(EldritchBlockRules.COSMETIC_ARCANE_STONE));
        assertEquals(2.0F, EldritchBlockRules.cosmeticHardness(EldritchBlockRules.COSMETIC_ELDRITCH_TILE));
        assertEquals(9, EldritchBlockRules.cosmeticLight(EldritchBlockRules.COSMETIC_TRAVEL_STONE));
        assertEquals(4, EldritchBlockRules.cosmeticLight(EldritchBlockRules.COSMETIC_TAINT_CRUST));
        assertEquals(0, EldritchBlockRules.cosmeticLight(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE));
        assertEquals(999.0F, EldritchBlockRules.cosmeticExplosionResistance(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE));
        assertEquals(999.0F, EldritchBlockRules.cosmeticExplosionResistance(EldritchBlockRules.COSMETIC_OBSIDIAN_TOTEM_BASE));
        assertEquals(999.0F, EldritchBlockRules.cosmeticExplosionResistance(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED));
        assertEquals(20.0F, EldritchBlockRules.cosmeticExplosionResistance(EldritchBlockRules.COSMETIC_THAUMIUM_BLOCK));
        // The 1.7.10 setter value 10 is stored as 30 and the vanilla getter divides it by five.
        assertEquals(10.0F, EldritchBlockRules.COSMETIC_FAMILY_RESISTANCE);
        assertEquals(6.0F, EldritchBlockRules.COSMETIC_FAMILY_NATIVE_RESISTANCE);
        assertEquals(6.0F, EldritchBlockRules.cosmeticExplosionResistance(EldritchBlockRules.COSMETIC_ELDRITCH_TILE));
        assertEquals(2.0F, EldritchBlockRules.COSMETIC_FAMILY_HARDNESS);
    }

    @Test void cosmeticFamiliesOnlyHostTheirOriginalBlockEntities() {
        assertTrue(EldritchBlockRules.cosmeticHasTile(EldritchBlockRules.COSMETIC_WARDING_STONE));
        assertTrue(EldritchBlockRules.cosmeticHasTile(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED));
        assertFalse(EldritchBlockRules.cosmeticHasTile(EldritchBlockRules.COSMETIC_ELDRITCH_TILE));
        assertTrue(EldritchBlockRules.cosmeticOpaqueHasTile(2));
        assertFalse(EldritchBlockRules.cosmeticOpaqueHasTile(0));
        assertTrue(EldritchBlockRules.cosmeticBlocksSpawning(EldritchBlockRules.COSMETIC_TRAVEL_STONE));
        assertFalse(EldritchBlockRules.cosmeticBlocksSpawning(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE));
    }

    @Test void theVariantItemsPlaceTheirOriginalMetadataValue() {
        // ConfigRecipes:192, 194, 197, 209, 212 and 449 - the result is a block with a metadata, so every
        // one of them has its own item that places that metadata value.
        assertEquals(2, EldritchBlockRules.variantOf("paving_stone_travel"));
        assertEquals(3, EldritchBlockRules.variantOf("paving_stone_warding"));
        assertEquals(2, EldritchBlockRules.variantOf("warded_glass"));
        assertEquals(EldritchBlockRules.DEVICE_STONE_FLUX_SCRUBBER, EldritchBlockRules.variantOf("flux_scrubber"));
        // The original blockWoodenDevice metadata eight: one item that carries its colour in a
        // data component, so one entry is enough.
        assertEquals(EldritchBlockRules.DEVICE_WOOD_BANNER, EldritchBlockRules.variantOf("banner"));
        // The original blockCosmeticSolid metadata one: the obsidian tile of oracle ConfigRecipes:449.
        assertEquals(1, EldritchBlockRules.variantOf("obsidian_tile"));
        assertEquals(-1, EldritchBlockRules.variantOf("cosmetic_solid"));
        assertEquals(6, EldritchBlockRules.VARIANT_ITEMS.size());
    }

    @Test void theHardnessGroupsKeepTheOriginalPerMetadataValues() {
        // The eldritch family splits where the original hardness differs.
        assertEquals(0, EldritchBlockRules.eldritchGroup(EldritchBlockRules.ELDRITCH_ALTAR));
        assertEquals(1, EldritchBlockRules.eldritchGroup(EldritchBlockRules.ELDRITCH_GLOWING_CRUST));
        assertEquals(2, EldritchBlockRules.eldritchGroup(EldritchBlockRules.ELDRITCH_DECO));
        assertEquals(3, EldritchBlockRules.eldritchGroup(EldritchBlockRules.ELDRITCH_DOORWAY));
        assertEquals(4, EldritchBlockRules.eldritchGroup(EldritchBlockRules.ELDRITCH_CRAB_SPAWNER));
        assertEquals(50.0F, EldritchBlockRules.eldritchGroupHardness(0));
        assertEquals(2.0F, EldritchBlockRules.eldritchGroupHardness(1));
        assertEquals(4.0F, EldritchBlockRules.eldritchGroupHardness(2));
        assertEquals(-1.0F, EldritchBlockRules.eldritchGroupHardness(3));
        assertEquals(15.0F, EldritchBlockRules.eldritchGroupHardness(4));
        assertEquals(12000.0F, EldritchBlockRules.eldritchGroupResistance(0));
        assertEquals(30.0F, EldritchBlockRules.eldritchGroupResistance(1));
        assertEquals(100.0F, EldritchBlockRules.eldritchGroupResistance(2));
        assertEquals(Float.MAX_VALUE, EldritchBlockRules.eldritchGroupResistance(3));
        assertEquals(List.of(0, 1, 2, 3), EldritchBlockRules.eldritchGroupVariants(0));
        assertEquals(List.of(4, 5), EldritchBlockRules.eldritchGroupVariants(1));
        assertEquals(List.of(7, 8), EldritchBlockRules.eldritchGroupVariants(3));
        // The cosmetic family: the hardness groups of the oracle.
        assertEquals(1, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE));
        assertEquals(1, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_OBSIDIAN_TOTEM_BASE));
        assertEquals(1, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_OBSIDIAN_TILE_SHADED));
        assertEquals(2, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_THAUMIUM_BLOCK));
        assertEquals(2, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_PEDESTAL_TOP));
        assertEquals(2, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_ARCANE_STONE));
        assertEquals(0, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_TRAVEL_STONE));
        assertEquals(0, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_WARDING_STONE));
        assertEquals(0, EldritchBlockRules.cosmeticGroup(EldritchBlockRules.COSMETIC_ELDRITCH_TILE));
        assertEquals(2.0F, EldritchBlockRules.cosmeticGroupHardness(0));
        assertEquals(30.0F, EldritchBlockRules.cosmeticGroupHardness(1));
        assertEquals(4.0F, EldritchBlockRules.cosmeticGroupHardness(2));
        assertEquals(6.0F, EldritchBlockRules.cosmeticGroupResistance(0));
        assertEquals(999.0F, EldritchBlockRules.cosmeticGroupResistance(1));
        assertEquals(20.0F, EldritchBlockRules.cosmeticGroupResistance(2));
        assertEquals(List.of(0, 1, 8), EldritchBlockRules.cosmeticGroupVariants(1));
        assertEquals(List.of(4, 6, 7), EldritchBlockRules.cosmeticGroupVariants(2));
        assertEquals(List.of(2, 3, 5, 9, 10, 11, 12, 13, 14, 15), EldritchBlockRules.cosmeticGroupVariants(0));
        // The airy family.
        assertEquals(1, EldritchBlockRules.airyGroup(EldritchBlockRules.AIRY_NODE));
        assertEquals(2, EldritchBlockRules.airyGroup(EldritchBlockRules.AIRY_MAGIC_HOLE));
        assertEquals(3, EldritchBlockRules.airyGroup(EldritchBlockRules.AIRY_UNBREAKABLE));
        assertEquals(2.0F, EldritchBlockRules.airyGroupHardness(1));
        assertEquals(200.0F, EldritchBlockRules.airyGroupResistance(1));
        assertEquals(-1.0F, EldritchBlockRules.airyGroupHardness(3));
    }

    @Test void airyKeepsItsReplaceableAndAirMetadata() {
        assertTrue(EldritchBlockRules.airyReplaceable(EldritchBlockRules.AIRY_LEAVES));
        assertTrue(EldritchBlockRules.airyIsAir(EldritchBlockRules.AIRY_MAGIC_HOLE));
        assertFalse(EldritchBlockRules.airyIsAir(EldritchBlockRules.AIRY_NODE));
        assertTrue(EldritchBlockRules.airyIsLeaves(EldritchBlockRules.AIRY_LEAVES_ALT));
        assertEquals(15, EldritchBlockRules.airyLight(EldritchBlockRules.AIRY_NITOR));
        assertEquals(0, EldritchBlockRules.airyLight(EldritchBlockRules.AIRY_WARDING_FENCE));
        assertEquals(2.0F, EldritchBlockRules.airyHardness(EldritchBlockRules.AIRY_NODE));
        assertEquals(-1.0F, EldritchBlockRules.airyHardness(EldritchBlockRules.AIRY_UNBREAKABLE));
        assertEquals(200.0F, EldritchBlockRules.airyExplosionResistance(EldritchBlockRules.AIRY_NODE));
        assertEquals(Float.MAX_VALUE, EldritchBlockRules.airyExplosionResistance(EldritchBlockRules.AIRY_UNBREAKABLE));
        assertTrue(EldritchBlockRules.airyHasTile(EldritchBlockRules.AIRY_WARDING_FENCE));
    }
}

package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.TileBlocksRuntimeChecks;

/**
 * Fabric wrappers of {@link TileBlocksRuntimeChecks}, one per check. Test-only, not distributed. The timeouts are those of
 * the slow checks: the tablet's swing takes thirty ticks, and a locomotive moves only when the game time is 1 modulo 100
 * (after the relays, which look for each other every 40 ticks, have linked it).
 */
public final class TileBlocksGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationIsComplete(GameTestHelper h) {
        TileBlocksRuntimeChecks.registrationIsComplete(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void blockEntitiesPersist(GameTestHelper h) {
        TileBlocksRuntimeChecks.blockEntitiesPersist(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void tabletBreaksTheBlockInFront(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletBreaksTheBlockInFront(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 250)
    public void tabletBreaksWithoutTheRightToolDropsNothing(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletBreaksWithoutTheRightToolDropsNothing(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void tabletAttacksTheEntityInFront(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletAttacksTheEntityInFront(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void tabletPlacesBlocks(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletPlacesBlocks(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void tabletTillsWithAHoe(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletTillsWithAHoe(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void tabletUsesItemsOnEntities(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletUsesItemsOnEntities(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 250)
    public void tabletInRedstoneModeNeedsPower(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletInRedstoneModeNeedsPower(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tabletTurnsWithAWand(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletTurnsWithAWand(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tabletSpillsItsItem(GameTestHelper h) {
        TileBlocksRuntimeChecks.tabletSpillsItsItem(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void menusHaveTheirSlots(GameTestHelper h) {
        TileBlocksRuntimeChecks.menusHaveTheirSlots(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void remotePlacerPlacesItsBlock(GameTestHelper h) {
        TileBlocksRuntimeChecks.remotePlacerPlacesItsBlock(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void remotePlacerRunsOnRedstone(GameTestHelper h) {
        TileBlocksRuntimeChecks.remotePlacerRunsOnRedstone(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void remotePlacerSpillsAndRefuses(GameTestHelper h) {
        TileBlocksRuntimeChecks.remotePlacerSpillsAndRefuses(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocatorSwapsBlocksWithTheirContents(GameTestHelper h) {
        TileBlocksRuntimeChecks.dislocatorSwapsBlocksWithTheirContents(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocatorMovesEntities(GameTestHelper h) {
        TileBlocksRuntimeChecks.dislocatorMovesEntities(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void dislocatorChecksLetEveryBlockThrough(GameTestHelper h) {
        TileBlocksRuntimeChecks.dislocatorChecksLetEveryBlockThrough(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void interfaceForwardsItems(GameTestHelper h) {
        TileBlocksRuntimeChecks.interfaceForwardsItems(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void interfaceForwardsEssentia(GameTestHelper h) {
        TileBlocksRuntimeChecks.interfaceForwardsEssentia(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void binderLinksDevices(GameTestHelper h) {
        TileBlocksRuntimeChecks.binderLinksDevices(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void binderBindsGolemConnectors(GameTestHelper h) {
        TileBlocksRuntimeChecks.binderBindsGolemConnectors(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void camouflageIsSetAndCleared(GameTestHelper h) {
        TileBlocksRuntimeChecks.camouflageIsSetAndCleared(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wandTurnsTheFacingDevices(GameTestHelper h) {
        TileBlocksRuntimeChecks.wandTurnsTheFacingDevices(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void locomotiveCarriesItsPassenger(GameTestHelper h) {
        TileBlocksRuntimeChecks.locomotiveCarriesItsPassenger(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void locomotiveTurnsRoundAtTheEndOfTheTrack(GameTestHelper h) {
        TileBlocksRuntimeChecks.locomotiveTurnsRoundAtTheEndOfTheTrack(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void poweredLocomotiveStays(GameTestHelper h) {
        TileBlocksRuntimeChecks.poweredLocomotiveStays(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void relaysFindEachOther(GameTestHelper h) {
        TileBlocksRuntimeChecks.relaysFindEachOther(h);
    }
}

package thaumic.tinkerer.fabric.testing;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorInterface;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.testing.TileBlocksRuntimeChecks;

/**
 * Fabric wrappers of {@link TileBlocksRuntimeChecks}, one per check. Test-only, not distributed. The timeouts are those of
 * the slow checks: the tablet's swing takes thirty ticks, and a locomotive moves only when the game time is 1 modulo 100
 * (after the relays, which look for each other every 40 ticks, have linked it).
 */
public final class TileBlocksGameTests implements FabricGameTest {
    /** Vanilla has no fluid-holding block entity: this test gives barrels a four-bucket tank each. */
    private static final Map<BlockEntity, SingleFluidStorage> TANKS = new WeakHashMap<>();
    private static boolean tankLookupRegistered;

    private static synchronized void registerTankLookup() {
        if (tankLookupRegistered) return;
        tankLookupRegistered = true;
        FluidStorage.SIDED.registerForBlockEntity(
                (barrel, side) -> TANKS.computeIfAbsent(barrel, b -> SingleFluidStorage.withFixedCapacity(4 * FluidConstants.BUCKET, b::setChanged)),
                BlockEntityType.BARREL);
    }

    /**
     * The Transvector Interface hands the fluid storage of the block it is bound to to whoever asks it (the Fabric glue,
     * {@code TileBlocksFabric}, registered by {@code TileBlocks.ready()}): unbound it has none; bound to a tank it fills
     * and drains that tank.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void interfaceForwardsFluids(GameTestHelper h) {
        registerTankLookup();
        BlockPos ifacePos = new BlockPos(2, 1, 2);
        BlockPos tankPos = new BlockPos(4, 1, 2);
        h.setBlock(ifacePos, TileBlocks.interfaceBlock());
        h.setBlock(tankPos, Blocks.BARREL);
        BlockEntity entity = h.getBlockEntity(ifacePos);
        h.assertTrue(entity instanceof TileTransvectorInterface, "an interface at " + ifacePos + ", found " + entity);
        TileTransvectorInterface device = (TileTransvectorInterface) entity;
        BlockPos absIface = h.absolutePos(ifacePos);
        h.assertTrue(FluidStorage.SIDED.find(h.getLevel(), absIface, Direction.NORTH) == null, "an unbound interface has no fluid storage");

        device.setTarget(h.absolutePos(tankPos));
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(h.getLevel(), absIface, Direction.NORTH);
        h.assertTrue(storage != null, "a bound interface has the storage of its tank");
        try (Transaction tx = Transaction.openOuter()) {
            long in = storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
            h.assertTrue(in == FluidConstants.BUCKET, "a bucket went in through the interface: " + in);
            tx.commit();
        }
        SingleFluidStorage tank = TANKS.get(h.getBlockEntity(tankPos));
        h.assertTrue(tank != null && tank.amount == FluidConstants.BUCKET && tank.variant.isOf(Fluids.WATER), "and sits in the tank");
        try (Transaction tx = Transaction.openOuter()) {
            long out = storage.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET / 2, tx);
            h.assertTrue(out == FluidConstants.BUCKET / 2, "half a bucket came out through the interface: " + out);
            tx.commit();
        }
        h.assertTrue(tank.amount == FluidConstants.BUCKET / 2, "and left the tank: " + tank.amount);
        h.succeed();
    }

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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 450)
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
    public void menuPacketsCheckTheSender(GameTestHelper h) {
        TileBlocksRuntimeChecks.menuPacketsCheckTheSender(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void placerWandOpensTheMenuOnlyWhileSneaking(GameTestHelper h) {
        TileBlocksRuntimeChecks.placerWandOpensTheMenuOnlyWhileSneaking(h);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void fakePlayerEarnsNoAdvancements(GameTestHelper h) {
        TileBlocksRuntimeChecks.fakePlayerEarnsNoAdvancements(h);
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

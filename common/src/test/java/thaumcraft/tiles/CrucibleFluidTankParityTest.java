package thaumcraft.tiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;
import thaumcraft.common.tiles.TileCrucible;

/**
 * The fluid tank of the original crucible.
 *
 * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:61-71,373-410}: the crucible is a Forge
 * {@code IFluidHandler} with a tank of one thousand millibuckets. It accepts water only, it never takes
 * more than the free space of the tank and it never gives away more than it holds. The fill answers
 * with the amount the tank accepted, so an overflow is partly taken and partly refused. The drain of a
 * named fluid refuses a resource that is not the content of the tank; the Forge comparison of 1.7.10
 * ({@code FluidStack.isFluidEqual}) compares the fluid and its tag only, never the amounts, so a
 * one-millibucket request drains one millibucket of a full tank.</p>
 */
class CrucibleFluidTankParityTest {
    @Test void theTankHoldsOneThousandMillibuckets() {
        // Oracle TileCrucible:71 - new FluidTank(FluidRegistry.WATER, 0, 1000).
        assertEquals(1000, TileCrucible.CAPACITY);
    }

    @Test void theTankNeverTakesMoreThanItsFreeSpace() {
        // Oracle TileCrucible:373-382 - the original hands the fill over to the Forge tank
        // of 1.7.10, and that tank answers with the amount it accepted: the free space of
        // the tank and the requested amount, whichever is smaller. A thousand and one
        // millibuckets therefore fill the thousand of the tank and only the overflow is
        // refused, so the answer is a thousand and not zero.
        assertEquals(500, TileCrucible.acceptedFill(0, 500));
        assertEquals(1000, TileCrucible.acceptedFill(0, 1000));
        assertEquals(1000, TileCrucible.acceptedFill(0, 1001));
        assertEquals(250, TileCrucible.acceptedFill(750, 500));
        assertEquals(0, TileCrucible.acceptedFill(1000, 1));
        assertEquals(0, TileCrucible.acceptedFill(0, -5));
    }

    @Test void theTankNeverGivesAwayMoreThanItHolds() {
        // Oracle TileCrucible:395-397 - the original tank drains at most its content.
        assertEquals(500, TileCrucible.drained(500, 1000));
        assertEquals(1000, TileCrucible.drained(1000, 1000));
        assertEquals(0, TileCrucible.drained(0, 1000));
        assertEquals(0, TileCrucible.drained(500, -5));
    }

    @Test void aNamedDrainIgnoresTheRequestedAmount() {
        // Oracle TileCrucible:384-393 - drain(resource) only asks isFluidEqual(tank.getFluid());
        // the 1.7.10 comparison is the fluid identity and its tag, never the amount. The
        // Decanting golem probes with one millibucket, so a full tank must answer one.
        assertEquals(1, TileCrucible.drainedOf(Fluids.WATER, Fluids.WATER, 1000, 1));
        assertEquals(1000, TileCrucible.drainedOf(Fluids.WATER, Fluids.WATER, 1000, 1000));
        assertEquals(1000, TileCrucible.drainedOf(Fluids.WATER, Fluids.WATER, 1000, 5000));
        assertEquals(250, TileCrucible.drainedOf(Fluids.WATER, Fluids.WATER, 250, 1000));
        // A different fluid, the empty request and an empty tank all refuse the drain.
        assertEquals(0, TileCrucible.drainedOf(Fluids.LAVA, Fluids.WATER, 1000, 1));
        assertEquals(0, TileCrucible.drainedOf(null, Fluids.WATER, 1000, 1));
        assertEquals(0, TileCrucible.drainedOf(Fluids.WATER, Fluids.WATER, 0, 1));
    }

    @Test void onlyWaterFillsTheTank() {
        // Oracle TileCrucible:399-401 - canFill is the water of the original registry.
        assertTrue(TileCrucible.canFillWater(Fluids.WATER));
        assertFalse(TileCrucible.canFillWater(Fluids.LAVA));
        assertFalse(TileCrucible.canFillWater(Fluids.EMPTY));
        assertFalse(TileCrucible.canFillWater(null));
    }

    @Test void anyFluidMayDrainTheTank() {
        // Oracle TileCrucible:403-405 - canDrain of the original is always true.
        assertTrue(TileCrucible.canDrainWater(Fluids.WATER));
        assertTrue(TileCrucible.canDrainWater(Fluids.LAVA));
        assertTrue(TileCrucible.canDrainWater(null));
    }
}

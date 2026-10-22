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
 * more than the free space of the tank and it never gives away more than it holds. The drain of a named
 * fluid refuses a stack that is not exactly the content of the tank, because the Forge comparison of
 * 1.7.10 compares the amounts as well.</p>
 */
class CrucibleFluidTankParityTest {
    @Test void theTankHoldsOneThousandMillibuckets() {
        // Oracle TileCrucible:71 - new FluidTank(FluidRegistry.WATER, 0, 1000).
        assertEquals(1000, TileCrucible.CAPACITY);
    }

    @Test void theTankNeverTakesMoreThanItsFreeSpace() {
        // Oracle TileCrucible:381 - the original tank refuses the overflow.
        assertEquals(500, TileCrucible.acceptedFill(0, 500));
        assertEquals(1000, TileCrucible.acceptedFill(0, 1000));
        assertEquals(0, TileCrucible.acceptedFill(0, 1001));
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

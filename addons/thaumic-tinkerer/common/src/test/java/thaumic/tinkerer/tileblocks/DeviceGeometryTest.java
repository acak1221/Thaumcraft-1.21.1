package thaumic.tinkerer.tileblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.block.BlockAnimationTablet;
import thaumic.tinkerer.common.block.camo.PulseDeviceBlock;
import thaumic.tinkerer.common.block.tile.TileEntityRelay;
import thaumic.tinkerer.common.block.tile.TileRPlacer;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The small pure rules of the devices: how a placed device is turned, the turn order of the tablet's wand, where the
 * remote placer aims.</p>
 */
class DeviceGeometryTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final BlockPos POS = new BlockPos(10, 64, 10);

    @Test
    void aDeviceFacesThePlayerWhoPlacesIt() {
        // 1.7.10 yaw: 0 looks south, 90 west, 180 north, 270 east. The device faces the player: opposite to the look.
        assertEquals(Direction.NORTH, PulseDeviceBlock.determineOrientation(POS, 10.5, 64, 15.5, 0F));
        assertEquals(Direction.EAST, PulseDeviceBlock.determineOrientation(POS, 15.5, 64, 10.5, 90F));
        assertEquals(Direction.SOUTH, PulseDeviceBlock.determineOrientation(POS, 10.5, 64, 5.5, 180F));
        assertEquals(Direction.WEST, PulseDeviceBlock.determineOrientation(POS, 5.5, 64, 10.5, 270F));
        assertEquals(Direction.WEST, PulseDeviceBlock.determineOrientation(POS, 5.5, 64, 10.5, -90F));
    }

    @Test
    void aPlayerStandingOverOrUnderTheDeviceTurnsItVertically() {
        // Within two blocks horizontally; the eye is feet + 1.82.
        assertEquals(Direction.UP, PulseDeviceBlock.determineOrientation(POS, 10.5, 66.5, 10.5, 0F));
        assertEquals(Direction.DOWN, PulseDeviceBlock.determineOrientation(POS, 10.5, 60, 10.5, 0F));
        // The same height, or too far to the side, stays horizontal.
        assertEquals(Direction.NORTH, PulseDeviceBlock.determineOrientation(POS, 10.5, 64, 11.5, 0F));
        assertEquals(Direction.NORTH, PulseDeviceBlock.determineOrientation(POS, 13.5, 70, 10.5, 0F));
    }

    @Test
    void theTabletTurnsNorthSouthWestEastNorth() {
        assertEquals(Direction.SOUTH, BlockAnimationTablet.nextFacing(Direction.NORTH));
        assertEquals(Direction.WEST, BlockAnimationTablet.nextFacing(Direction.SOUTH));
        assertEquals(Direction.EAST, BlockAnimationTablet.nextFacing(Direction.WEST));
        assertEquals(Direction.NORTH, BlockAnimationTablet.nextFacing(Direction.EAST));
    }

    @Test
    void theRemotePlacerAimsTheDistanceAlongItsFacing() {
        for (int blocks = 1; blocks <= 4; blocks++) {
            assertEquals(POS.below(blocks), TileRPlacer.targetFor(POS, Direction.DOWN, blocks, true));
            assertEquals(POS.above(blocks), TileRPlacer.targetFor(POS, Direction.UP, blocks, true));
            assertEquals(POS.north(blocks), TileRPlacer.targetFor(POS, Direction.NORTH, blocks, true));
            assertEquals(POS.south(blocks), TileRPlacer.targetFor(POS, Direction.SOUTH, blocks, true));
            assertEquals(POS.west(blocks), TileRPlacer.targetFor(POS, Direction.WEST, blocks, true));
        }
    }

    @Test
    void theOriginalAimedAnEastFacingPlacerToTheWest() {
        assertEquals(POS.west(3), TileRPlacer.targetFor(POS, Direction.EAST, 3, true), "the 1.7.10 copy and paste");
        assertEquals(POS.east(3), TileRPlacer.targetFor(POS, Direction.EAST, 3, false), "what the facing says");
    }

    @Test
    void theRelayLoopsStepLikeCopySignOfOne() {
        // Math.copySign(1, difference): -1 below zero, +1 for zero and above (copySign(1, 0) is +1).
        assertEquals(1, TileEntityRelay.step(0));
        assertEquals(1, TileEntityRelay.step(9));
        assertEquals(-1, TileEntityRelay.step(-1));
        assertEquals(-1, TileEntityRelay.step(-32));
    }

    @Test
    void aRelayPartnerBeyondTheSearchRangeIsStale() {
        // checkForPartner looks from -32 to +31 along the two lines; anything else is stale data that must not make the
        // beam loops walk the rectangle to it.
        assertEquals(true, TileEntityRelay.partnerInRange(-32, 0));
        assertEquals(true, TileEntityRelay.partnerInRange(0, 31));
        assertEquals(true, TileEntityRelay.partnerInRange(31, 0));
        assertEquals(false, TileEntityRelay.partnerInRange(32, 0));
        assertEquals(false, TileEntityRelay.partnerInRange(-33, 0));
        assertEquals(false, TileEntityRelay.partnerInRange(0, 32));
        assertEquals(false, TileEntityRelay.partnerInRange(-944_868, 6_350_562));
    }

    @Test
    void theRelayLoopsAdvanceBeyondTwoToThe24() {
        // The original added the float of copySign to the int counter, which stops advancing at +-16,777,216, so a linked
        // pair out there hung the server thread. The integer step ends the walk after the 32 blocks between the relays.
        for (int start : new int[] { 16_777_216, 16_777_300, 20_000_000, -20_000_000, 29_999_960 }) {
            int partner = start + 32;
            int j = start;
            int iterations = 0;
            do {
                j += TileEntityRelay.step(partner - start);
                iterations++;
            } while (j < partner && iterations < 1000);
            assertEquals(32, iterations, "the walk from " + start);
        }
    }
}

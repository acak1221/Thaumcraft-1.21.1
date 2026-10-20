package thaumcraft.wands;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.WardingOverlay;
import static org.junit.jupiter.api.Assertions.*;

class WardingOverlayTest {
    @Test void originalConnectedAssetsKeepOneBasedNamesAndCornerRules() {
        assertEquals(1, WardingOverlay.texture(0));
        assertEquals(1, WardingOverlay.texture(1)); // A corner without either edge does not connect the tile.
        assertEquals(7, WardingOverlay.texture(2));
        assertEquals(3, WardingOverlay.texture(24));
        assertEquals(32, WardingOverlay.texture(27));
        assertEquals(47, WardingOverlay.texture(90));
        assertEquals(12, WardingOverlay.texture(255));
        Set<Integer> textures = new HashSet<>();
        for (int mask = 0; mask <= 255; mask++) textures.add(WardingOverlay.texture(mask));
        assertEquals(47, textures.size());
        assertTrue(textures.stream().allMatch(texture -> texture >= 1 && texture <= 47));
        assertThrows(IllegalArgumentException.class, () -> WardingOverlay.texture(-1));
        assertThrows(IllegalArgumentException.class, () -> WardingOverlay.texture(256));
    }

    @Test void allSixFacesUseTheOriginalVisibleSideOrientationAndEightUniqueCoplanarNeighbours() {
        BlockPos origin = new BlockPos(11, 27, -13);
        assertEquals(origin.offset(-1, 0, -1), WardingOverlay.neighbour(origin, Direction.UP, 0));
        assertEquals(origin.offset(-1, 0, -1), WardingOverlay.neighbour(origin, Direction.DOWN, 0));
        assertEquals(origin.offset(1, 1, 0), WardingOverlay.neighbour(origin, Direction.NORTH, 0));
        assertEquals(origin.offset(-1, 1, 0), WardingOverlay.neighbour(origin, Direction.SOUTH, 0));
        assertEquals(origin.offset(0, 1, -1), WardingOverlay.neighbour(origin, Direction.WEST, 0));
        assertEquals(origin.offset(0, 1, 1), WardingOverlay.neighbour(origin, Direction.EAST, 0));
        for (Direction face : Direction.values()) {
            Set<BlockPos> cells = new HashSet<>();
            for (int index = 0; index < 8; index++) {
                BlockPos cell = WardingOverlay.neighbour(origin, face, index);
                assertTrue(cells.add(cell));
                assertEquals(origin.get(face.getAxis()), cell.get(face.getAxis()));
                assertTrue(Math.abs(cell.getX() - origin.getX()) <= 1 && Math.abs(cell.getY() - origin.getY()) <= 1
                        && Math.abs(cell.getZ() - origin.getZ()) <= 1);
                assertNotEquals(origin, cell);
            }
            assertEquals(8, cells.size());
        }
    }
}

package thaumcraft.wands;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandArchitect;
import thaumcraft.common.items.wands.WandArchitect.Area;
import static org.junit.jupiter.api.Assertions.*;

class WandArchitectTest {
    @Test void defaultMaximumWrapsToZeroAndShiftSelectsAxesInOriginalOrder() {
        Area all = new Area(3, 3, 3, 0);
        assertEquals(new Area(0, 0, 0, 0), all.next(3, 3, false));
        Area x = all.next(3, 3, true);
        assertEquals(new Area(3, 3, 3, 1), x);
        assertEquals(new Area(0, 3, 3, 1), x.next(3, 3, false));
        Area z = x.next(3, 3, true);
        assertEquals(new Area(3, 3, 0, 2), z.next(3, 3, false));
        Area y = z.next(3, 3, true);
        assertEquals(new Area(3, 0, 3, 3), y.next(3, 3, false));
        assertEquals(all, y.next(3, 3, true));
    }
    @Test void tradeHasOnlyTheTwoInPlaneDimensionsAndClampsAnOlderLargerArea() {
        assertEquals(new Area(2, 2, 2, 0), new Area(8, 9, 10, 2).next(2, 2, true));
        assertEquals(new Area(0, 0, 0, 0), new Area(8, 9, 10, 3).next(2, 2, false));
    }
    @Test void theNormalToTheFaceIsDepthAndTheVerticalAxisUsesXOnWalls() {
        BlockPos root = new BlockPos(100, 64, -100);
        Area area = new Area(1, 2, 3, 0);
        for (Direction face : List.of(Direction.UP, Direction.DOWN)) {
            assertTrue(WandArchitect.within(root.offset(1, 2, 3), root, face, area, false));
            assertFalse(WandArchitect.within(root.offset(2, 0, 0), root, face, area, false));
            assertFalse(WandArchitect.within(root.offset(0, 3, 0), root, face, area, false));
        }
        for (Direction face : List.of(Direction.NORTH, Direction.SOUTH)) {
            assertTrue(WandArchitect.within(root.offset(3, 1, 2), root, face, area, false));
            assertFalse(WandArchitect.within(root.offset(0, 2, 0), root, face, area, false));
            assertFalse(WandArchitect.within(root.offset(4, 0, 0), root, face, area, false));
        }
        for (Direction face : List.of(Direction.WEST, Direction.EAST)) {
            assertTrue(WandArchitect.within(root.offset(2, 1, 3), root, face, area, false));
            assertFalse(WandArchitect.within(root.offset(3, 0, 0), root, face, area, false));
            assertFalse(WandArchitect.within(root.offset(0, 2, 0), root, face, area, false));
        }
    }
    @Test void planarTradeNeverVisitsDepthAndBothHorizontalRadiiAreIndependent() {
        BlockPos root = BlockPos.ZERO;
        Area area = new Area(1, 8, 2, 0);
        for (Direction face : Direction.values()) {
            List<BlockPos> plane = WandArchitect.planarBlocks(root, face, area, pos -> true);
            assertEquals(15, plane.size(), face.toString());
            assertTrue(plane.stream().allMatch(pos -> switch (face.getAxis()) {
                case X -> pos.getX() == 0; case Y -> pos.getY() == 0; case Z -> pos.getZ() == 0;
            }));
        }
    }
    @Test void disconnectedAndForeignCellsDoNotProvideAPathThroughTheVolume() {
        BlockPos root = BlockPos.ZERO;
        var allowed = Set.of(root, root.east(), root.east(3));
        assertEquals(List.of(root, root.east()), WandArchitect.connected(root, Direction.UP, new Area(3, 0, 0, 0), false, allowed::contains));
        assertTrue(WandArchitect.connected(root, Direction.UP, new Area(3, 3, 3, 0), false, cell -> false).isEmpty());
    }
    @Test void traversalIsOriginalDepthFirstWithoutDuplicatesOrRecursion() {
        BlockPos root = BlockPos.ZERO;
        List<BlockPos> cube = WandArchitect.connected(root, Direction.UP, new Area(1, 1, 1, 0), false, cell -> true);
        assertEquals(27, cube.size());
        assertEquals(27, new HashSet<>(cube).size());
        assertEquals(root, cube.get(0));
        assertEquals(root.below(), cube.get(1), "DOWN is the original first child");
        assertEquals(root.below().north(), cube.get(2), "UP revisits root, so NORTH is the first new grandchild");
    }
    @Test void maximumRadiusHasAFiniteScanAndEveryOfferedCellIsInsideTheBox() {
        BlockPos root = new BlockPos(-7, 123, 6);
        Area area = new Area(WandArchitect.MAX_RADIUS, WandArchitect.MAX_RADIUS, WandArchitect.MAX_RADIUS, 0);
        Set<BlockPos> offered = new HashSet<>();
        List<BlockPos> cube = WandArchitect.connected(root, Direction.UP, area, false, cell -> {
            assertTrue(WandArchitect.within(cell, root, Direction.UP, area, false));
            assertTrue(offered.add(cell));
            return true;
        });
        int diameter = WandArchitect.MAX_RADIUS * 2 + 1;
        assertEquals(diameter * diameter * diameter, cube.size());
        assertEquals(cube.size(), offered.size());
    }
    @Test void persistenceAndNetworkKeepAllDimensionsAndRejectInvalidValues() {
        Area area = new Area(1, 8, 3, 2);
        var json = Area.CODEC.encodeStart(JsonOps.INSTANCE, area).getOrThrow();
        assertEquals(area, Area.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertTrue(Area.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"x\":-1,\"y\":0,\"z\":0,\"axis\":0}")).error().isPresent());
        var buffer = Unpooled.buffer();
        try {
            Area.STREAM_CODEC.encode(buffer, area);
            assertEquals(4, buffer.readableBytes());
            assertEquals(area, Area.STREAM_CODEC.decode(buffer));
            buffer.clear(); buffer.writeByte(255).writeByte(0).writeByte(0).writeByte(0);
            assertThrows(DecoderException.class, () -> Area.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
        assertThrows(IllegalArgumentException.class, () -> new Area(0, 0, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> new Area(WandArchitect.MAX_RADIUS + 1, 0, 0, 0));
    }
}

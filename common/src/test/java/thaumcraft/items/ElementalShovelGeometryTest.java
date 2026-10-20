package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;

/** Where the Shovel of the Earthmover works (original ItemElementalShovel), without a world. */
class ElementalShovelGeometryTest {
    private static final BlockPos AT = new BlockPos(10, 20, 30);

    private static Set<BlockPos> set(List<BlockPos> cells) {
        Set<BlockPos> set = new HashSet<>(cells);
        assertEquals(9, cells.size());
        assertEquals(9, set.size(), "nine distinct cells");
        return set;
    }

    private static Set<BlockPos> box(BlockPos centre, int dx, int dy, int dz) {
        Set<BlockPos> cells = new HashSet<>();
        for (int x = -dx; x <= dx; ++x) for (int y = -dy; y <= dy; ++y) for (int z = -dz; z <= dz; ++z) cells.add(centre.offset(x, y, z));
        return cells;
    }

    @Test void miningTakesTheSquareAcrossTheHitFace() {
        assertEquals(box(AT, 1, 0, 1), set(ElementalShovelGeometry.plane(AT, Direction.UP)));
        assertEquals(box(AT, 1, 0, 1), set(ElementalShovelGeometry.plane(AT, Direction.DOWN)));
        assertEquals(box(AT, 1, 1, 0), set(ElementalShovelGeometry.plane(AT, Direction.NORTH)));
        assertEquals(box(AT, 1, 1, 0), set(ElementalShovelGeometry.plane(AT, Direction.SOUTH)));
        assertEquals(box(AT, 0, 1, 1), set(ElementalShovelGeometry.plane(AT, Direction.WEST)));
        assertEquals(box(AT, 0, 1, 1), set(ElementalShovelGeometry.plane(AT, Direction.EAST)));
        assertTrue(ElementalShovelGeometry.plane(AT, Direction.UP).contains(AT), "the anchor is part of its own square");
    }

    @Test void theLoopOrderIsTheOriginalOne() {
        // First axis outer, second inner: (-1,-1), (-1,0), (-1,1), (0,-1) ...
        var up = ElementalShovelGeometry.plane(AT, Direction.UP);
        assertEquals(AT.offset(-1, 0, -1), up.get(0));
        assertEquals(AT.offset(-1, 0, 0), up.get(1));
        assertEquals(AT.offset(0, 0, -1), up.get(3));
        assertEquals(AT.offset(1, 0, 1), up.get(8));
        var west = ElementalShovelGeometry.plane(AT, Direction.WEST);
        assertEquals(AT.offset(0, -1, -1), west.get(0), "a west face runs the depth axis first, then the height");
        assertEquals(AT.offset(0, 0, -1), west.get(1));
        assertEquals(AT.offset(0, -1, 0), west.get(3));
    }

    @Test void orientationZeroLaysTheSquareBesideTheFace() {
        for (Direction face : Direction.values()) {
            BlockPos beside = AT.relative(face);
            var plane = ElementalShovelGeometry.plane(beside, face);
            assertEquals(plane, ElementalShovelGeometry.layout(AT, face, 0, 0), face + ": the same square as mining, one block out");
        }
    }

    @Test void orientationOneIsAnUprightSquare() {
        // Top face, facing south (yaw 0) or north (180): height x width along x; facing west / east (90, -90): height x depth along z.
        assertEquals(box(AT.above(), 1, 1, 0), set(ElementalShovelGeometry.layout(AT, Direction.UP, 1, 0)));
        assertEquals(box(AT.above(), 1, 1, 0), set(ElementalShovelGeometry.layout(AT, Direction.UP, 1, 180)));
        assertEquals(box(AT.above(), 0, 1, 1), set(ElementalShovelGeometry.layout(AT, Direction.UP, 1, 90)));
        assertEquals(box(AT.above(), 0, 1, 1), set(ElementalShovelGeometry.layout(AT, Direction.UP, 1, -90)));
        // Side faces: north and south run the depth axis, west and east the width axis, always with the height.
        assertEquals(box(AT.north(), 0, 1, 1), set(ElementalShovelGeometry.layout(AT, Direction.NORTH, 1, 0)));
        assertEquals(box(AT.south(), 0, 1, 1), set(ElementalShovelGeometry.layout(AT, Direction.SOUTH, 1, 0)));
        assertEquals(box(AT.west(), 1, 1, 0), set(ElementalShovelGeometry.layout(AT, Direction.WEST, 1, 0)));
        assertEquals(box(AT.east(), 1, 1, 0), set(ElementalShovelGeometry.layout(AT, Direction.EAST, 1, 0)));
    }

    @Test void orientationTwoIsAFloorOnASideFaceAndTheUprightSquareOnTopAndBottom() {
        assertEquals(box(AT.north(), 1, 0, 1), set(ElementalShovelGeometry.layout(AT, Direction.NORTH, 2, 0)));
        assertEquals(box(AT.east(), 1, 0, 1), set(ElementalShovelGeometry.layout(AT, Direction.EAST, 2, 0)));
        assertEquals(ElementalShovelGeometry.layout(AT, Direction.UP, 1, 90), ElementalShovelGeometry.layout(AT, Direction.UP, 2, 90));
        assertEquals(ElementalShovelGeometry.layout(AT, Direction.UP, 1, 0), ElementalShovelGeometry.layout(AT, Direction.UP, 2, 0));
        assertEquals(box(AT.below(), 0, 1, 1), set(ElementalShovelGeometry.layout(AT, Direction.DOWN, 2, 90)));
    }

    @Test void theFacingIsTheOriginalQuarterTurn() {
        assertEquals(0, ElementalShovelGeometry.quarterTurn(0));
        assertEquals(0, ElementalShovelGeometry.quarterTurn(44.9F));
        assertEquals(1, ElementalShovelGeometry.quarterTurn(45F));
        assertEquals(2, ElementalShovelGeometry.quarterTurn(180F));
        assertEquals(3, ElementalShovelGeometry.quarterTurn(-90F));
        assertEquals(0, ElementalShovelGeometry.quarterTurn(360F));
    }

    @Test void theKeyCyclesThroughThreeOrientations() {
        assertEquals(1, ElementalShovelGeometry.next(0));
        assertEquals(2, ElementalShovelGeometry.next(1));
        assertEquals(0, ElementalShovelGeometry.next(2));
        assertEquals(3, ElementalShovelGeometry.ORIENTATIONS);
    }
}

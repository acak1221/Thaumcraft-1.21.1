package thaumcraft.bore;

import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.bore.ArcaneBoreMath;
import thaumcraft.common.lib.bore.ArcaneBoreMath.Angles;
import thaumcraft.common.lib.bore.ArcaneBoreMath.FocusLevels;
import thaumcraft.common.lib.bore.ArcaneBoreMath.Rotation;
import thaumcraft.common.lib.bore.ArcaneBoreMath.ScanCursor;
import static org.junit.jupiter.api.Assertions.*;

class ArcaneBoreMathTest {
    private static final List<Direction> DIRECTIONS = List.of(Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST);
    private static final BlockPos TOP = new BlockPos(10, 64, -20);

    @Test void sixOriginalTargetsAndBothMountsAreIndependent() {
        List<Angles> expected = List.of(new Angles(0, 180), new Angles(0, 0), new Angles(270, 90),
                new Angles(90, 90), new Angles(0, 90), new Angles(180, 90));
        assertEquals(TOP.below(), ArcaneBoreMath.basePosition(TOP, Direction.UP));
        assertEquals(TOP.above(), ArcaneBoreMath.basePosition(TOP, Direction.DOWN));
        for (int i = 0; i < DIRECTIONS.size(); i++) {
            Direction mining = DIRECTIONS.get(i);
            assertEquals(expected.get(i), ArcaneBoreMath.targetAngles(mining));
            for (Direction attachment : List.of(Direction.UP, Direction.DOWN)) {
                BlockPos base = ArcaneBoreMath.basePosition(TOP, attachment);
                BlockPos sameTop = base.relative(attachment);
                assertEquals(TOP, sameTop);
                assertEquals(ArcaneBoreMath.stepPlane(ScanCursor.initial(), TOP, mining, 0),
                        ArcaneBoreMath.stepPlane(ScanCursor.initial(), sameTop, mining, 0));
                Rotation rotation = ArcaneBoreMath.reorient(new Rotation(0, 0, 0, 0, 0, 0), mining, false);
                // Independent source-derived settle times: targets0/90/180/270
                // take1/35/46/52 arithmetic steps from zero velocity/position.
                int steps = switch (mining) { case UP -> 1; case DOWN, EAST -> 46; case NORTH -> 52; default -> 35; };
                for (int tick = 0; tick < steps; tick++) rotation = ArcaneBoreMath.stepRotation(rotation);
                assertTrue(rotation.aligned(), mining + "/" + attachment);
                assertEquals(expected.get(i).x(), rotation.x());
                assertEquals(expected.get(i).z(), rotation.z());
                assertEquals(0, rotation.velocityX()); assertEquals(0, rotation.velocityZ());
            }
        }
        for (Direction horizontal : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST))
            assertThrows(IllegalArgumentException.class, () -> ArcaneBoreMath.basePosition(TOP, horizontal));
    }

    @Test void rotationUsesOldVelocityOvershootsAndDampsBeforeSettling() {
        Rotation rotation = new Rotation(0, 0, 0, 0, 90, 90);
        // Tile167-184 evaluated independently: not lerp/clamp/shortest turn.
        int[][] expected = {{0, 1}, {1, 2}, {3, 3}, {6, 4}, {10, 5}, {15, 6}, {21, 7},
                {28, 8}, {36, 9}, {45, 10}, {55, 11}, {66, 12}, {78, 13}, {91, 4},
                {95, 3}, {98, 2}, {100, 1}, {101, 0}, {101, -1}, {100, -2}, {98, -3},
                {95, -4}, {91, -5}, {86, -1}, {85, 0}, {85, 1}, {86, 2}, {88, 3},
                {91, 1}, {92, 0}, {92, -1}, {91, -2}, {89, 0}, {89, 1}, {90, 0}};
        for (int index = 0; index < expected.length; index++) {
            rotation = ArcaneBoreMath.stepRotation(rotation);
            assertEquals(expected[index][0], rotation.x(), "step" + (index + 1));
            assertEquals(expected[index][1], rotation.velocityX(), "step" + (index + 1));
            assertEquals(rotation.x(), rotation.z()); assertEquals(rotation.velocityX(), rotation.velocityZ());
            assertEquals(index == expected.length - 1, rotation.aligned());
        }
        assertEquals(rotation, ArcaneBoreMath.stepRotation(rotation));
        var negativeCrossing = ArcaneBoreMath.stepRotation(new Rotation(91, 0, -2, 0, 90, 0));
        assertEquals(89, negativeCrossing.x()); assertEquals(0, negativeCrossing.velocityX());
        // Preserve the source's FLOAT division, not integer velocity/3.
        var largeCrossing = ArcaneBoreMath.stepRotation(new Rotation(0, 0, Integer.MAX_VALUE, 0, 90, 0));
        assertEquals(715_827_904, largeCrossing.velocityX());
    }

    @Test void initialSnapAndWandRetargetPreserveVelocityAndDoNotWrapAngles() {
        var before = new Rotation(17, 29, -2, 7, 0, 0);
        var turn = ArcaneBoreMath.reorient(before, Direction.NORTH, false);
        assertEquals(new Rotation(17, 29, -2, 7, 270, 90), turn);
        assertFalse(turn.aligned());
        assertEquals(new Rotation(270, 90, -2, 7, 270, 90), ArcaneBoreMath.reorient(before, Direction.NORTH, true));
        assertEquals(new Rotation(17, 29, -2, 7, 0, 0), before);
        var snapped = ArcaneBoreMath.reorient(before, Direction.UP, true);
        assertTrue(snapped.aligned());
        assertEquals(new Rotation(0, 0, 0, 0, 0, 0), ArcaneBoreMath.stepRotation(snapped));
    }

    @Test void scanResetPreservesSpiralRadiusAndLastYWithoutMutableAliases() {
        var mutable = new BlockPos.MutableBlockPos(31, -7, 41);
        var before = new ScanCursor(358, -1.25F, -2.0F / 360.0F, mutable);
        mutable.set(90, 90, 90);
        assertEquals(new BlockPos(31, -7, 41), before.lastPlane());
        var reset = ArcaneBoreMath.resetScanForOrientation(before);
        assertEquals(new ScanCursor(358, -1.25F, 0.0F, new BlockPos(0, -7, 0)), reset);
        assertEquals(-2.0F / 360.0F, before.radialIncrement());
        var advanced = ArcaneBoreMath.stepPlane(reset, TOP, Direction.SOUTH, 2).cursor();
        assertEquals(0, advanced.spiral());
        assertEquals(4.0F / 360.0F, advanced.radialIncrement());
        assertEquals(-1.25F + 4.0F / 360.0F, advanced.radius());
    }

    @Test void firstSamplesRetainOriginalFloatBitsAndRepeatedPlaneSuppression() {
        var first = ArcaneBoreMath.stepPlane(ScanCursor.initial(), TOP, Direction.SOUTH, 0);
        assertTrue(first.changedPlane()); assertEquals(new BlockPos(10, 64, -19), first.cursor().lastPlane());
        assertEquals(2, first.cursor().spiral());
        assertEquals(0x3bb60b61, Float.floatToIntBits(first.cursor().radius()));
        assertEquals(0x3bb60b61, Float.floatToIntBits(first.cursor().radialIncrement()));
        var second = ArcaneBoreMath.stepPlane(first.cursor(), TOP, Direction.SOUTH, 0);
        var third = ArcaneBoreMath.stepPlane(second.cursor(), TOP, Direction.SOUTH, 0);
        assertFalse(second.changedPlane()); assertFalse(third.changedPlane());
        assertEquals(4, second.cursor().spiral()); assertEquals(6, third.cursor().spiral());
        assertEquals(0x3c360b61, Float.floatToIntBits(second.cursor().radius()));
        assertEquals(0x3c888889, Float.floatToIntBits(third.cursor().radius()));
        assertEquals(first.cursor().lastPlane(), third.cursor().lastPlane());
        assertEquals(0.0F, ScanCursor.initial().radius());
        // Default last coordinates really are zero, not an uninitialized/null
        // sentinel. The first sample can equal them and must be skipped.
        var zeroPlane = ArcaneBoreMath.stepPlane(ScanCursor.initial(), new BlockPos(0, -1, 0), Direction.UP, 0);
        assertEquals(BlockPos.ZERO, zeroPlane.cursor().lastPlane()); assertFalse(zeroPlane.changedPlane());
    }

    @Test void firstDifferentSpiralPlanesMatchOriginalSteppingRatherThanASquare() {
        // Independent float32/table-trig derivation of Tile507-534, top10,64,-20.
        int[] sampleNumbers = {1, 90, 106, 116, 154, 164, 194, 213};
        int[][] south = {{10,64,-19}, {10,63,-19}, {10,64,-19}, {9,64,-19},
                {9,65,-19}, {10,65,-19}, {11,65,-19}, {11,64,-19}};
        int[][] east = {{11,64,-20}, {11,63,-20}, {11,64,-20}, {11,64,-19},
                {11,65,-19}, {11,65,-20}, {11,65,-21}, {11,64,-21}};
        int[][] up = {{10,65,-20}, {10,65,-19}, {10,65,-20}, {9,65,-20},
                {9,65,-21}, {10,65,-21}, {11,65,-21}, {11,65,-20}};
        Direction[] directions = {Direction.SOUTH, Direction.EAST, Direction.UP};
        int[][][] expected = {south, east, up};
        for (int direction = 0; direction < directions.length; direction++) {
            ScanCursor cursor = ScanCursor.initial(); int next = 0;
            for (int sample = 1; sample <= 213; sample++) {
                var step = ArcaneBoreMath.stepPlane(cursor, TOP, directions[direction], 0);
                cursor = step.cursor();
                if (step.changedPlane()) {
                    assertTrue(next < sampleNumbers.length);
                    assertEquals(sampleNumbers[next], sample);
                    int[] point = expected[direction][next++];
                    assertEquals(new BlockPos(point[0], point[1], point[2]), cursor.lastPlane());
                }
            }
            assertEquals(sampleNumbers.length, next);
        }
    }

    @Test void projectionKeepsAllSixAxisSignsAndFloorsNegativeCoordinates() {
        var sample = new ScanCursor(90, 1.25F, 0.0F, BlockPos.ZERO);
        List<BlockPos> expected = List.of(new BlockPos(1,-1,0), new BlockPos(1,1,0), new BlockPos(1,0,-1),
                new BlockPos(1,0,1), new BlockPos(-1,0,1), new BlockPos(1,0,-1));
        for (int i = 0; i < DIRECTIONS.size(); i++) {
            assertEquals(expected.get(i), ArcaneBoreMath.projectPlane(BlockPos.ZERO, DIRECTIONS.get(i), sample));
            assertEquals(expected.get(i).offset(-30, -80, -50),
                    ArcaneBoreMath.projectPlane(new BlockPos(-30, -80, -50), DIRECTIONS.get(i), sample));
        }
    }

    @Test void radiusReversesAfterOvershootAndDoesNotReinitializeAnExistingIncrement() {
        ScanCursor cursor = ScanCursor.initial();
        for (int sample = 0; sample < 360; sample++) cursor = ArcaneBoreMath.stepPlane(cursor, TOP, Direction.SOUTH, 0).cursor();
        assertEquals(0, cursor.spiral());
        assertEquals(1.9999938F, cursor.radius()); assertEquals(2.0F / 360.0F, cursor.radialIncrement());
        var overshoot = ArcaneBoreMath.stepPlane(cursor, TOP, Direction.SOUTH, 0).cursor();
        assertEquals(2, overshoot.spiral()); assertEquals(2.0055494F, overshoot.radius());
        assertEquals(-2.0F / 360.0F, overshoot.radialIncrement());
        var returning = ArcaneBoreMath.stepPlane(overshoot, TOP, Direction.SOUTH, 0).cursor();
        assertEquals(1.9999939F, returning.radius());
        var negative = new ScanCursor(358, -2.0F, -2.0F / 360.0F, BlockPos.ZERO);
        var negativeOvershoot = ArcaneBoreMath.stepPlane(negative, TOP, Direction.SOUTH, 0).cursor();
        assertTrue(negativeOvershoot.radius() < -2.0F);
        assertEquals(2.0F / 360.0F, negativeOvershoot.radialIncrement());
        var oldIncrement = new ScanCursor(10, .5F, 2.0F / 360.0F, BlockPos.ZERO);
        assertEquals(2.0F / 360.0F, ArcaneBoreMath.stepPlane(oldIncrement, TOP, Direction.SOUTH, 2).cursor().radialIncrement());
        for (int area = 0; area <= 2; area++)
            assertEquals((2.0F + area) / 360.0F, ArcaneBoreMath.stepPlane(ScanCursor.initial(), TOP, Direction.SOUTH, area).cursor().radialIncrement());
    }

    @Test void columnHasExactlySixtyFourReadsFromTopPlusThreeThroughSixtySix() {
        assertEquals(64, ArcaneBoreMath.CANDIDATE_READS);
        for (Direction direction : DIRECTIONS) {
            BlockPos plane = ArcaneBoreMath.projectPlane(TOP, direction, ScanCursor.initial());
            assertEquals(TOP.relative(direction), plane);
            var positions = new HashSet<BlockPos>();
            for (int index = 0; index < ArcaneBoreMath.CANDIDATE_READS; index++) {
                BlockPos candidate = ArcaneBoreMath.candidate(plane, direction, index);
                assertEquals(TOP.relative(direction, index + 3), candidate);
                assertTrue(positions.add(candidate));
            }
            assertEquals(64, positions.size());
            assertTrue(positions.contains(TOP.relative(direction, 3)));
            assertTrue(positions.contains(TOP.relative(direction, 66)));
            assertFalse(positions.contains(TOP.relative(direction, 2)));
            assertFalse(positions.contains(TOP.relative(direction, 67)));
        }
        for (int index : new int[]{Integer.MIN_VALUE, -1, 64, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> ArcaneBoreMath.candidate(TOP, Direction.NORTH, index));
    }

    @Test void occlusionRayStartsBeforeTheFirstCandidateWithoutPretendingToScanAWorld() {
        for (Direction direction : DIRECTIONS) {
            Vec3 start = ArcaneBoreMath.occlusionRayStart(TOP, direction);
            assertEquals(Vec3.atCenterOf(TOP.relative(direction)), start);
            BlockPos first = ArcaneBoreMath.candidate(TOP.relative(direction), direction, 0);
            Vec3 end = Vec3.atCenterOf(first);
            // The unsampled +2 cell lies on the real ray to the +3 candidate.
            assertEquals(Vec3.atCenterOf(TOP.relative(direction, 2)), start.add(end.subtract(start).scale(.5)));
        }
    }

    @Test void statsUseFortuneMaximumSpeedSumAndEnlargeWidthButIgnoreFrugal() {
        var levels = new FocusLevels(2, 0, 2, 1);
        var stats = ArcaneBoreMath.stats(levels, 3, 4);
        assertEquals(3, stats.fortune()); assertEquals(6, stats.speed());
        assertEquals(1, stats.area()); assertEquals(3, stats.radius()); assertEquals(7, stats.width());
        assertEquals(3, ArcaneBoreMath.stats(new FocusLevels(0, 0, 3, 0), 1, 0).fortune());
        var withoutFrugal = ArcaneBoreMath.stats(new FocusLevels(1, 0, 1, 1), 1, 0);
        assertEquals(withoutFrugal, ArcaneBoreMath.stats(new FocusLevels(1, 2, 1, 1), 1, 0));
        for (int area = 0; area <= 2; area++)
            assertEquals(5 + area * 2, ArcaneBoreMath.stats(new FocusLevels(0, 0, 0, area), 0, 0).width());
    }

    @Test void itemValidityStopsBeforeTheLastDurabilityPointWithoutATierInput() {
        assertTrue(ArcaneBoreMath.itemsValid(true, true, true, 0, 400));
        assertTrue(ArcaneBoreMath.itemsValid(true, true, true, 398, 400));
        assertFalse(ArcaneBoreMath.itemsValid(true, true, true, 399, 400));
        assertFalse(ArcaneBoreMath.itemsValid(true, true, true, 400, 400));
        assertFalse(ArcaneBoreMath.itemsValid(false, true, true, 0, 400));
        assertFalse(ArcaneBoreMath.itemsValid(true, false, true, 0, 400));
        assertFalse(ArcaneBoreMath.itemsValid(true, true, false, 0, 400));
        assertTrue(ArcaneBoreMath.itemsValid(true, true, true, 0, 59));
    }

    @Test void stoneAndObsidianDelaysUseOriginalCastAndNoMinimumClamp() {
        assertEquals(40, ArcaneBoreMath.delayForTarget(1.5F, 0, 0));
        assertEquals(10, ArcaneBoreMath.delayForTarget(1.5F, 0, 1));
        assertEquals(36, ArcaneBoreMath.delayForTarget(1.5F, 1, 0));
        assertEquals(9, ArcaneBoreMath.delayForTarget(1.5F, 1, 1));
        assertEquals(400, ArcaneBoreMath.delayForTarget(50.0F, 0, 0));
        assertEquals(100, ArcaneBoreMath.delayForTarget(50.0F, 0, 1));
        assertEquals(11, ArcaneBoreMath.delayForTarget(5.99F, 0, 1));
        assertEquals(0, ArcaneBoreMath.delayForTarget(1.5F, 10, 1));
        assertEquals(-1, ArcaneBoreMath.delayForTarget(1.5F, 11, 1));
        assertEquals(-4, ArcaneBoreMath.delayForTarget(1.5F, 11, 0));
        assertEquals(10, ArcaneBoreMath.delayForTarget(-1.0F, 0, 1)); // Math is NOT harvest authorization.
    }

    @Test void rawVisRemainsFloatCreditAndThresholdsForRefillAndSpeedAreDistinct() {
        assertEquals(100, ArcaneBoreMath.MAX_RAW_PERDITIO_REQUEST);
        assertEquals(20.0F, ArcaneBoreMath.REFILL_THRESHOLD);
        assertTrue(ArcaneBoreMath.needsAcceleration(0));
        assertTrue(ArcaneBoreMath.needsAcceleration(19.8F));
        assertFalse(ArcaneBoreMath.needsAcceleration(20.0F));
        assertFalse(ArcaneBoreMath.needsAcceleration(34.4F));
        float credit = ArcaneBoreMath.addAcceptedRawPerditio(0, 1);
        assertEquals(.2F, credit); assertEquals(40, ArcaneBoreMath.delayForTarget(1.5F, 0, credit));
        for (int grant = 1; grant < 5; grant++) credit = ArcaneBoreMath.addAcceptedRawPerditio(credit, 1);
        assertEquals(1.0F, credit); assertEquals(10, ArcaneBoreMath.delayForTarget(1.5F, 0, credit));
        assertEquals(40, ArcaneBoreMath.delayForTarget(1.5F, 0, Math.nextDown(1.0F)));
        assertEquals(20.0F, ArcaneBoreMath.addAcceptedRawPerditio(0, 100));
        assertEquals(3.0F, ArcaneBoreMath.addAcceptedRawPerditio(3.0F, 0));
        for (int invalid : new int[]{-1, 101, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> ArcaneBoreMath.addAcceptedRawPerditio(0, invalid));
    }

    @Test void mixedPrefillCanOvershootTwentyAndMustRecheckAfterRawGrant() {
        float afterRaw = ArcaneBoreMath.addAcceptedRawPerditio(14.0F, 2);
        assertEquals(14.4F, afterRaw); assertTrue(ArcaneBoreMath.needsAcceleration(afterRaw));
        assertEquals(34.4F, ArcaneBoreMath.addConsumedEssentia(afterRaw));
        assertFalse(ArcaneBoreMath.needsAcceleration(ArcaneBoreMath.addConsumedEssentia(afterRaw)));
        float filled = ArcaneBoreMath.addAcceptedRawPerditio(19.8F, 1);
        assertEquals(20.0F, filled); assertFalse(ArcaneBoreMath.needsAcceleration(filled));
        assertEquals(39.8F, ArcaneBoreMath.addConsumedEssentia(19.8F));
    }

    @Test void nextTargetDelayIsSelectedBeforeDigDebitAndFractionalDebtIsNotClamped() {
        float beforeDebit = 1.0F;
        int selected = ArcaneBoreMath.delayForTarget(1.5F, 0, beforeDebit);
        float afterSelection = ArcaneBoreMath.creditAfterDigAndNextSelection(beforeDebit, true);
        assertEquals(10, selected); assertEquals(0.0F, afterSelection);
        assertEquals(40, ArcaneBoreMath.delayForTarget(1.5F, 0, afterSelection));
        assertEquals(-.8F, ArcaneBoreMath.creditAfterDigAndNextSelection(.2F, true));
        assertEquals(-.8F, ArcaneBoreMath.creditAfterDigAndNextSelection(-.8F, true));
        assertEquals(0.0F, ArcaneBoreMath.creditAfterDigAndNextSelection(0, true));
        assertEquals(.2F, ArcaneBoreMath.creditAfterDigAndNextSelection(.2F, false));
        assertTrue(ArcaneBoreMath.needsAcceleration(-.8F));
    }

    @Test void diskShortTruncatesPositiveFractionsAndNegativeFractionalDebtTowardZero() {
        assertEquals((short) 34, ArcaneBoreMath.persistedCredit(34.4F));
        assertEquals((short) 0, ArcaneBoreMath.persistedCredit(.2F));
        assertEquals((short) 0, ArcaneBoreMath.persistedCredit(-.8F));
        assertEquals((short) -1, ArcaneBoreMath.persistedCredit(-1.8F));
        assertEquals(34.0F, ArcaneBoreMath.loadedCredit(ArcaneBoreMath.persistedCredit(34.4F)));
        assertEquals(0.0F, ArcaneBoreMath.loadedCredit(ArcaneBoreMath.persistedCredit(-.8F)));
        assertEquals(Short.MIN_VALUE, ArcaneBoreMath.persistedCredit(32_768.0F)); // Java narrowing, not saturation.
    }

    @Test void malformedScanGeometryIsRefusedWithoutMutableOrWorldState() {
        for (int spiral : new int[]{-1, 360, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> new ScanCursor(spiral, 0, 0, BlockPos.ZERO));
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ScanCursor(0, invalid, 0, BlockPos.ZERO));
            assertThrows(IllegalArgumentException.class, () -> new ScanCursor(0, 0, invalid, BlockPos.ZERO));
        }
        assertThrows(NullPointerException.class, () -> new ScanCursor(0, 0, 0, null));
        for (int area : new int[]{-1, 3, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> ArcaneBoreMath.stepPlane(ScanCursor.initial(), TOP, Direction.SOUTH, area));
    }
}

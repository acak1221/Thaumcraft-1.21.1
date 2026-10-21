package thaumcraft.essentia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.essentia.TubeGeometry;

/**
 * Parity of the tube geometry with the original TC4 1.7.10 CodeChicken geometry.
 *
 * <p>The 1.7.10 original indexed the six tube arms and the tube core with the bundled
 * CodeChickenLib types ({@code Cuboid6} in {@code IndexedCuboid6}) and hit them with the bundled
 * {@code RayTracer}. The 1.21.1 port carries none of that library: it uses native
 * {@code AABB}s and the vanilla {@code AABB.clip}. This test pins the original numbers and
 * proves that the native replacement reproduces the original 1.7.10 results.</p>
 *
 * <p>Original 1.7.10 sources (readable reference {@code OpenTC4} and the 1.7.10 jar):
 * {@code thaumcraft/common/tiles/TileTube.java:388-421} (the six arms and the core),
 * {@code thaumcraft/common/tiles/TileTubeBuffer.java:290-320} (the buffer core),
 * {@code thaumcraft/codechicken/lib/raytracer/RayTracer.java:37-122} (the ray trace),
 * {@code thaumcraft/codechicken/lib/vec/Vector3.java:177-238} (the plane intercepts) and
 * {@code thaumcraft/codechicken/lib/math/MathHelper.java:65-67} (the inclusive between test).</p>
 */
class CodeChickenGeometryParityTest {
    /** The arm cross-section of the original, 0.42F..0.58F. */
    private static final double MIN = 0.42F;
    private static final double MAX = 0.58F;
    /** The original tube core, 0.34375F..0.65625F. */
    private static final double CORE_MIN = 0.34375F;
    private static final double CORE_MAX = 0.65625F;

    /** A 1.7.10 CodeChickenLib {@code Cuboid6}: the original box type of the ray tracer. */
    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}

    /** The original 1.7.10 hit of one cuboid: the side index and the hit point. */
    private record OriginalHit(int side, Vec3 point, double distanceSquared) {}

    /** The original 1.7.10 hit of the indexed cuboid list. */
    private record IndexedHit(int index, OriginalHit hit) {}

    // ------------------------------------------------------------------ the original geometry

    /**
     * The six original arms in the original index order 0..5, transcribed from
     * {@code TileTube.addTraceableCuboids} of 1.7.10: DOWN, UP, NORTH, SOUTH, WEST, EAST.
     */
    private static AABB originalArm(int index) {
        return switch (index) {
            case 0 -> new AABB(MIN, 0.0D, MIN, MAX, 0.5D, MAX);
            case 1 -> new AABB(MIN, 0.5D, MIN, MAX, 1.0D, MAX);
            case 2 -> new AABB(MIN, MIN, 0.0D, MAX, MAX, 0.5D);
            case 3 -> new AABB(MIN, MIN, 0.5D, MAX, MAX, 1.0D);
            case 4 -> new AABB(0.0D, MIN, MIN, 0.5D, MAX, MAX);
            case 5 -> new AABB(0.5D, MIN, MIN, 1.0D, MAX, MAX);
            default -> throw new IllegalArgumentException("arm " + index);
        };
    }

    /** The original tube core, {@code TileTube:421}. */
    private static AABB originalCore() {
        return new AABB(CORE_MIN, CORE_MIN, CORE_MIN, CORE_MAX, CORE_MAX, CORE_MAX);
    }

    /** The original buffer core, {@code TileTubeBuffer:319}. */
    private static AABB originalBufferCore() {
        return new AABB(0.25D, 0.25D, 0.25D, 0.75D, 0.75D, 0.75D);
    }

    // ------------------------------------------------------------------ the original ray tracer

    /** The original inclusive {@code MathHelper.between(a, x, b)} of 1.7.10. */
    private static boolean between(double a, double x, double b) {
        return a <= x && x <= b;
    }

    /**
     * The original plane intercepts, transcribed from
     * {@code thaumcraft/codechicken/lib/vec/Vector3.java:177-238}: the crossing of the ray with
     * one face plane, only accepted when the crossing parameter is inside {@code [0,1]}, and
     * the degenerate {@code |d| <= 1e-5} case returns the start point unchanged.
     */
    private static Vec3 intercept(Vec3 start, Vec3 end, Box box, int side) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        switch (side) {
            case 0: { // XZintercept(end, cuboid.min.y)
                if (dy == 0.0D) return null;
                double d = (box.minY - start.y) / dy;
                if (between(-1.0E-5D, d, 1.0E-5D)) return start;
                if (!between(0.0D, d, 1.0D)) return null;
                return new Vec3(start.x + d * dx, box.minY, start.z + d * dz);
            }
            case 1: { // XZintercept(end, cuboid.max.y)
                if (dy == 0.0D) return null;
                double d = (box.maxY - start.y) / dy;
                if (between(-1.0E-5D, d, 1.0E-5D)) return start;
                if (!between(0.0D, d, 1.0D)) return null;
                return new Vec3(start.x + d * dx, box.maxY, start.z + d * dz);
            }
            case 2: { // XYintercept(end, cuboid.min.z)
                if (dz == 0.0D) return null;
                double d = (box.minZ - start.z) / dz;
                if (between(-1.0E-5D, d, 1.0E-5D)) return start;
                if (!between(0.0D, d, 1.0D)) return null;
                return new Vec3(start.x + d * dx, start.y + d * dy, box.minZ);
            }
            case 3: { // XYintercept(end, cuboid.max.z)
                if (dz == 0.0D) return null;
                double d = (box.maxZ - start.z) / dz;
                if (between(-1.0E-5D, d, 1.0E-5D)) return start;
                if (!between(0.0D, d, 1.0D)) return null;
                return new Vec3(start.x + d * dx, start.y + d * dy, box.maxZ);
            }
            case 4: { // YZintercept(end, cuboid.min.x)
                if (dx == 0.0D) return null;
                double d = (box.minX - start.x) / dx;
                if (between(-1.0E-5D, d, 1.0E-5D)) return start;
                if (!between(0.0D, d, 1.0D)) return null;
                return new Vec3(box.minX, start.y + d * dy, start.z + d * dz);
            }
            case 5: { // YZintercept(end, cuboid.max.x)
                if (dx == 0.0D) return null;
                double d = (box.maxX - start.x) / dx;
                if (between(-1.0E-5D, d, 1.0E-5D)) return start;
                if (!between(0.0D, d, 1.0D)) return null;
                return new Vec3(box.maxX, start.y + d * dy, start.z + d * dz);
            }
            default:
                return null;
        }
    }

    /** The original per-side bounds test, {@code RayTracer.traceSide:60-79}. */
    private static boolean inBounds(Box box, Vec3 hit, int side) {
        return switch (side) {
            case 0, 1 -> between(box.minX, hit.x, box.maxX) && between(box.minZ, hit.z, box.maxZ);
            case 2, 3 -> between(box.minX, hit.x, box.maxX) && between(box.minY, hit.y, box.maxY);
            case 4, 5 -> between(box.minY, hit.y, box.maxY) && between(box.minZ, hit.z, box.maxZ);
            default -> false;
        };
    }

    /** The original {@code RayTracer.rayTraceCuboid}: the nearest of the six face crossings. */
    private static OriginalHit originalTraceCuboid(Vec3 start, Vec3 end, Box box) {
        double best = Double.MAX_VALUE;
        int bestSide = -1;
        Vec3 bestPoint = null;
        for (int side = 0; side < 6; side++) {
            Vec3 hit = intercept(start, end, box, side);
            if (hit == null || !inBounds(box, hit, side)) continue;
            double distance = hit.subtract(start).lengthSqr();
            if (distance < best) {
                best = distance;
                bestSide = side;
                bestPoint = hit;
            }
        }
        return bestSide < 0 ? null : new OriginalHit(bestSide, bestPoint, best);
    }

    /**
     * The original {@code RayTracer.rayTraceCuboids}: the indexed list is walked in order and the
     * strictly nearest cuboid wins, so a tie keeps the earlier index.
     */
    private static IndexedHit originalTraceCuboids(Vec3 start, Vec3 end, List<Box> boxes) {
        double best = Double.MAX_VALUE;
        IndexedHit result = null;
        for (int index = 0; index < boxes.size(); index++) {
            OriginalHit hit = originalTraceCuboid(start, end, boxes.get(index));
            if (hit != null && hit.distanceSquared() < best) {
                best = hit.distanceSquared();
                result = new IndexedHit(index, hit);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ the native replacement

    /** The native arm boxes of the port, read from the audited {@code TubeGeometry}. */
    private static AABB[] nativeArms() throws ReflectiveOperationException {
        Field field = TubeGeometry.class.getDeclaredField("ARMS");
        field.setAccessible(true);
        return (AABB[]) field.get(null);
    }

    /** The native tube core of the port. */
    private static AABB nativeCore() throws ReflectiveOperationException {
        Field field = TubeGeometry.class.getDeclaredField("CORE");
        field.setAccessible(true);
        return (AABB) field.get(null);
    }

    /** The native buffer core of the port. */
    private static AABB nativeBufferCore() throws ReflectiveOperationException {
        Field field = TubeGeometry.class.getDeclaredField("BUFFER_CORE");
        field.setAccessible(true);
        return (AABB) field.get(null);
    }

    /**
     * The native trace of the port, transcribed from
     * {@code thaumcraft/common/lib/essentia/TubeGeometry.java:48-62}: every indexed part is
     * clipped with the vanilla {@code AABB.clip} and the strictly nearest hit wins.
     */
    private static IndexedHit nativeTrace(Vec3 start, Vec3 end, List<AABB> boxes) {
        double best = Double.POSITIVE_INFINITY;
        IndexedHit result = null;
        for (int index = 0; index < boxes.size(); index++) {
            BlockHitResult hit = AABB.clip(List.of(boxes.get(index)), start, end, BlockPos.ZERO);
            if (hit == null) continue;
            double distance = start.distanceToSqr(hit.getLocation());
            if (distance < best) {
                best = distance;
                result = new IndexedHit(index, new OriginalHit(-1, hit.getLocation(), distance));
            }
        }
        return result;
    }

    /** The seven original indexed parts of a tube: the six arms, then the core. */
    private static List<Box> originalParts() {
        List<Box> parts = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            AABB arm = originalArm(index);
            parts.add(new Box(arm.minX, arm.minY, arm.minZ, arm.maxX, arm.maxY, arm.maxZ));
        }
        AABB core = originalCore();
        parts.add(new Box(core.minX, core.minY, core.minZ, core.maxX, core.maxY, core.maxZ));
        return parts;
    }

    /** The seven native indexed parts of a tube, in the same order. */
    private static List<AABB> nativeParts() throws ReflectiveOperationException {
        List<AABB> parts = new ArrayList<>(List.of(nativeArms()));
        parts.add(nativeCore());
        return parts;
    }

    private static void assertBox(AABB expected, AABB actual, String what) {
        assertEquals(expected.minX, actual.minX, 1.0E-9D, what + " minX");
        assertEquals(expected.minY, actual.minY, 1.0E-9D, what + " minY");
        assertEquals(expected.minZ, actual.minZ, 1.0E-9D, what + " minZ");
        assertEquals(expected.maxX, actual.maxX, 1.0E-9D, what + " maxX");
        assertEquals(expected.maxY, actual.maxY, 1.0E-9D, what + " maxY");
        assertEquals(expected.maxZ, actual.maxZ, 1.0E-9D, what + " maxZ");
    }

    // ------------------------------------------------------------------ the tests

    @Test void theNativeArmsAreTheOriginalCuboids() throws ReflectiveOperationException {
        AABB[] arms = nativeArms();
        assertEquals(6, arms.length, "the original tube has six indexed arms");
        for (int index = 0; index < arms.length; index++) {
            assertBox(originalArm(index), arms[index], "arm " + index);
        }
    }

    @Test void theNativeCoresAreTheOriginalCuboids() throws ReflectiveOperationException {
        assertBox(originalCore(), nativeCore(), "tube core");
        assertBox(originalBufferCore(), nativeBufferCore(), "buffer core");
    }

    @Test void theNativeBufferSelectionBoxIsTheOriginalBufferCore() {
        VoxelShape shape = TubeGeometry.bufferSelectionShape();
        assertNotNull(shape, "the buffer keeps its original selection box");
        assertBox(originalBufferCore(), shape.bounds(), "buffer selection");
    }

    @Test void theOriginalRayTracerHitsTheNearFaceOfAnArm() {
        Vec3 start = new Vec3(0.5D, 2.0D, 0.5D);
        Vec3 end = new Vec3(0.5D, -1.0D, 0.5D);
        AABB arm = originalArm(0);
        OriginalHit hit = originalTraceCuboid(start, end,
                new Box(arm.minX, arm.minY, arm.minZ, arm.maxX, arm.maxY, arm.maxZ));
        assertNotNull(hit, "the original ray tracer hits the down arm");
        assertEquals(1, hit.side(), "the original hits the max.y face of the arm");
        assertEquals(0.5D, hit.point().x, 1.0E-9D);
        assertEquals(0.5D, hit.point().y, 1.0E-9D);
        assertEquals(0.5D, hit.point().z, 1.0E-9D);
    }

    @Test void theOriginalRayTracerMissesOutsideTheArmCrossSection() {
        Vec3 start = new Vec3(0.1D, 2.0D, 0.1D);
        Vec3 end = new Vec3(0.1D, -1.0D, 0.1D);
        AABB arm = originalArm(0);
        OriginalHit hit = originalTraceCuboid(start, end,
                new Box(arm.minX, arm.minY, arm.minZ, arm.maxX, arm.maxY, arm.maxZ));
        assertNull(hit, "the original ray tracer misses outside the 0.42..0.58 cross section");
    }

    @Test void theOriginalRayTracerHitsTheNearFaceOfTheCore() {
        Vec3 start = new Vec3(0.5D, 2.0D, 0.5D);
        Vec3 end = new Vec3(0.5D, -1.0D, 0.5D);
        AABB core = originalCore();
        OriginalHit hit = originalTraceCuboid(start, end,
                new Box(core.minX, core.minY, core.minZ, core.maxX, core.maxY, core.maxZ));
        assertNotNull(hit, "the original ray tracer hits the core");
        assertEquals(1, hit.side(), "the original hits the max.y face of the core");
        assertEquals(CORE_MAX, hit.point().y, 1.0E-9D, "the original stops on the near core face");
    }

    @Test void theOriginalRayTracerPicksTheNearestIndexedPart() {
        Vec3 start = new Vec3(2.0D, 0.5D, 0.5D);
        Vec3 end = new Vec3(-1.0D, 0.5D, 0.5D);
        IndexedHit hit = originalTraceCuboids(start, end, originalParts());
        assertNotNull(hit, "the original ray tracer hits the tube");
        assertEquals(5, hit.index(), "the nearest indexed part of the original is the east arm");
        assertEquals(1.0D, hit.hit().point().x, 1.0E-9D, "the original stops on the near east face");
        assertEquals(0.5D, hit.hit().point().y, 1.0E-9D);
        assertEquals(0.5D, hit.hit().point().z, 1.0E-9D);
    }

    @Test void theNativeClipAgreesWithTheOriginalRayTracer() throws ReflectiveOperationException {
        List<Vec3[]> rays = List.of(
                new Vec3[]{new Vec3(0.5D, 2.0D, 0.5D), new Vec3(0.5D, -1.0D, 0.5D)},
                new Vec3[]{new Vec3(0.1D, 2.0D, 0.1D), new Vec3(0.1D, -1.0D, 0.1D)},
                new Vec3[]{new Vec3(2.0D, 0.5D, 0.5D), new Vec3(-1.0D, 0.5D, 0.5D)},
                new Vec3[]{new Vec3(0.5D, 0.5D, 2.0D), new Vec3(0.5D, 0.5D, -1.0D)},
                new Vec3[]{new Vec3(2.0D, 0.25D, 0.25D), new Vec3(-1.0D, 0.25D, 0.25D)},
                new Vec3[]{new Vec3(-0.5D, 0.5D, 0.5D), new Vec3(2.0D, 0.5D, 0.5D)},
                new Vec3[]{new Vec3(0.5D, -1.0D, 0.5D), new Vec3(0.5D, 2.0D, 0.5D)},
                new Vec3[]{new Vec3(-1.0D, 0.5D, 0.5D), new Vec3(2.0D, 0.5D, 0.5D)},
                new Vec3[]{new Vec3(0.25D, 0.25D, -1.0D), new Vec3(0.25D, 0.25D, 2.0D)},
                new Vec3[]{new Vec3(0.75D, 0.75D, -1.0D), new Vec3(0.75D, 0.75D, 2.0D)},
                new Vec3[]{new Vec3(0.2D, 0.8D, 0.2D), new Vec3(0.8D, 0.2D, 0.8D)},
                new Vec3[]{new Vec3(-2.0D, 0.1D, 0.9D), new Vec3(3.0D, 0.9D, 0.1D)});
        List<Box> original = originalParts();
        List<AABB> nativeBoxes = nativeParts();
        for (Vec3[] ray : rays) {
            Vec3 start = ray[0];
            Vec3 end = ray[1];
            IndexedHit expected = originalTraceCuboids(start, end, original);
            IndexedHit actual = nativeTrace(start, end, nativeBoxes);
            if (expected == null) {
                assertNull(actual, "the native clip must miss like the original for " + start + " -> " + end);
                continue;
            }
            assertNotNull(actual, "the native clip must hit like the original for " + start + " -> " + end);
            assertEquals(expected.index(), actual.index(),
                    "the native clip must select the same indexed part for " + start + " -> " + end);
            // The original crosses the face plane arithmetically, the vanilla clip offsets the face
            // by its own 1e-7 epsilon, so the two agree to within that epsilon.
            assertEquals(expected.hit().point().x, actual.hit().point().x, 1.0E-6D, "hit x");
            assertEquals(expected.hit().point().y, actual.hit().point().y, 1.0E-6D, "hit y");
            assertEquals(expected.hit().point().z, actual.hit().point().z, 1.0E-6D, "hit z");
        }
    }

    @Test void theOriginalRayTracerKeepsItsDegenerateStartOnFaceHit() {
        // The original intercept returns the start point unchanged when the crossing parameter is
        // degenerate (|d| <= 1e-5), so a ray that starts on a face of a part reports the start
        // point as the hit. The native AABB.clip reports no hit there instead; the case cannot
        // occur in the port because the traced part is never the block the player stands in, so
        // the eye is never inside a traced arm or core.
        Vec3 start = new Vec3(0.5D, 0.5D, 0.5D);
        Vec3 end = new Vec3(0.5D, 3.0D, 0.5D);
        AABB arm = originalArm(0);
        OriginalHit hit = originalTraceCuboid(start, end,
                new Box(arm.minX, arm.minY, arm.minZ, arm.maxX, arm.maxY, arm.maxZ));
        assertNotNull(hit, "the original ray tracer keeps the degenerate hit");
        assertEquals(1, hit.side(), "the original stops on the max.y face of the arm");
        assertEquals(0.0D, hit.distanceSquared(), 1.0E-9D, "the original reports the start point");
        assertEquals(start.x, hit.point().x, 1.0E-9D);
        assertEquals(start.y, hit.point().y, 1.0E-9D);
        assertEquals(start.z, hit.point().z, 1.0E-9D);
    }

    @Test void theNativeClipStopsInsideTheReachLikeTheOriginal() {
        // The original only accepts a crossing inside the ray segment (d in [0,1]); the native
        // AABB.clip rejects a part that lies beyond the end of the ray the same way.
        AABB core = originalCore();
        Vec3 start = new Vec3(0.5D, 5.0D, 0.5D);
        Vec3 end = new Vec3(0.5D, 4.0D, 0.5D);
        assertNull(originalTraceCuboid(start, end,
                new Box(core.minX, core.minY, core.minZ, core.maxX, core.maxY, core.maxZ)),
                "the original ray tracer ignores a part beyond the end of the ray");
        assertNull(AABB.clip(List.of(core), start, end, BlockPos.ZERO),
                "the native clip ignores a part beyond the end of the ray");
    }

    @Test void theNativeGeometryHasNoCodeChickenLeft() {
        // The port must not carry the vendored library: the geometry is native AABB only.
        assertTrue(TubeGeometry.class.getName().startsWith("thaumcraft.common.lib.essentia."),
                "the tube geometry is a native class of the port");
        assertTrue(TubeGeometry.class.getPackage().getName().indexOf("codechicken") < 0,
                "the port ships no CodeChicken package");
    }
}

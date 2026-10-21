package thaumcraft.client.fx.bolt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

/**
 * Native port of the original {@code thaumcraft/client/fx/bolt/FXLightningBoltCommon.java} and of the float
 * vector helpers it uses, {@code thaumcraft/client/fx/WRVector3.java} and {@code thaumcraft/client/fx/WRMat4.java}
 * (oracle {@code /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle}).
 *
 * <p>The port is a line by line transcription, not an approximation:</p>
 * <ul>
 * <li>the same {@link Random} draw order, so the same seed produces the same lightning;</li>
 * <li>the same float arithmetic: {@link Vector} keeps the original {@code float} fields, and the rotation is the
 * original {@code WRMat4.rotationMat} Rodrigues matrix applied through the original {@code translate};</li>
 * <li>the same seven {@code defaultFractal()} steps with the original amounts, split chances, split lengths and
 * split angles;</li>
 * <li>the same {@code calculateCollisionAndDiffs()} pass, including the split parent map, and the same final
 * sort by descending light;</li>
 * <li>the same unnormalised {@code sin(acos(dot)/2)} joint sines, so a segment whose normalised dot product
 * leaves the unit interval keeps the original {@code NaN} and is reported as not renderable instead of being
 * silently clamped to one.</li>
 * </ul>
 *
 * <p>The original never calls {@code defaultFractal()} from its constructor; every original call site builds the
 * bolt, calls {@code defaultFractal()} and then {@code finalizeBolt()}. This port performs exactly that sequence
 * in the constructor, which is the same random stream and the same geometry.</p>
 */
public final class WispLightningShape {
    /** The original segment list, in the original order. */
    public final List<Segment> segments = new ArrayList<>();
    /** The original {@code length}: the distance between the two end points. */
    public final float length;
    /** The original {@code particleMaxAge}. */
    public final int maxAge;
    /** The original {@code numsegments0}: the number of main chain segments. */
    public int mainSegments = 1;
    /** The original {@code increment}: the age step per tick. */
    public final int increment;
    /** The original {@code multiplier}: the fractal displacement factor. */
    public final float multiplier;

    private final Random random;
    private final Map<Integer, Integer> splitParents = new HashMap<>();
    /** The original {@code numsplits}: the counter of the fork branches. */
    private int splitCount;
    private boolean finalized;

    /**
     * The original {@code FXLightningBoltCommon(World, WRVector3, WRVector3, long)}: the plain bolt, the
     * multiplier one and the age step one.
     */
    public WispLightningShape(Vec3 from, Vec3 to, long seed) {
        this(from, to, seed, 0, 1.0F, 1);
    }

    /**
     * The original {@code FXLightningBoltCommon(World, Entity, Entity, long, int)}: the entity to entity bolt with
     * the age step of the caller and the multiplier {@code 0.4f}.
     */
    public WispLightningShape(Vec3 from, Vec3 to, long seed, int speed) {
        this(from, to, seed, 0, 0.4F, speed);
    }

    /**
     * The original {@code FXLightningBoltCommon(World, double, double, double, double, double, double, long,
     * int, float)}: the point to point bolt with the duration and the multiplier of the caller.
     */
    public WispLightningShape(Vec3 from, Vec3 to, long seed, int duration, float multiplier) {
        this(from, to, seed, duration, multiplier, 1);
    }

    /**
     * The original {@code FXLightningBoltCommon(World, double, double, double, double, double, double, long,
     * int, float, int)}: the point to point bolt with the duration, the multiplier and the age step.
     */
    public WispLightningShape(Vec3 from, Vec3 to, long seed, int duration, float multiplier, int speed) {
        if (multiplier <= 0.0F || !Float.isFinite(multiplier)) throw new IllegalArgumentException("Invalid bolt multiplier");
        random = new Random(seed);
        increment = speed;
        this.multiplier = multiplier;
        Vector start = Vector.of(from);
        Vector end = Vector.of(to);
        length = end.copy().sub(start).length();
        // The original always draws the base age first and the duration age second.
        int baseAge = 3 + random.nextInt(3) - 1;
        maxAge = duration > 0 ? duration + random.nextInt(duration) - duration / 2 : baseAge;
        segments.add(new Segment(new Point(start, new Vector(0, 0, 0)), new Point(end, new Vector(0, 0, 0)), 1.0F, 0, 0));
        defaultFractal();
        finalizeBolt();
    }

    /** The original {@code defaultFractal()}: the seven fractal steps of the original, in the original order. */
    public void defaultFractal() {
        fractal(2, length * multiplier / 8.0F, 0.7F, 0.1F, 45.0F);
        fractal(2, length * multiplier / 12.0F, 0.5F, 0.1F, 50.0F);
        fractal(2, length * multiplier / 17.0F, 0.5F, 0.1F, 55.0F);
        fractal(2, length * multiplier / 23.0F, 0.5F, 0.1F, 60.0F);
        fractal(2, length * multiplier / 30.0F, 0.0F, 0.0F, 0.0F);
        fractal(2, length * multiplier / 34.0F, 0.0F, 0.0F, 0.0F);
        fractal(2, length * multiplier / 40.0F, 0.0F, 0.0F, 0.0F);
    }

    /**
     * The original {@code fractal(int splits, float amount, float splitchance, float splitlength, float splitangle)}.
     */
    public void fractal(int splits, float amount, float splitchance, float splitlength, float splitangle) {
        if (finalized) return;
        // The original replaces the segment list with a fresh one and keeps the old list for the walk.
        List<Segment> oldSegments = new ArrayList<>(segments);
        segments.clear();
        Segment previous = null;
        for (Segment segment : oldSegments) {
            previous = segment.previous;
            Vector subSegment = segment.diff.copy().scale(1.0F / (float) splits);
            Point[] newPoints = new Point[splits + 1];
            Vector startPoint = segment.start.point;
            newPoints[0] = segment.start;
            newPoints[splits] = segment.end;
            for (int i = 1; i < splits; ++i) {
                Vector randomOffset = Vector.perpendicular(segment.diff).rotate(random.nextFloat() * 360.0F, segment.diff);
                randomOffset.scale((random.nextFloat() - 0.5F) * amount);
                Vector basePoint = startPoint.copy().add(subSegment.copy().scale(i));
                newPoints[i] = new Point(basePoint, randomOffset);
            }
            for (int i = 0; i < splits; ++i) {
                Segment next = new Segment(newPoints[i], newPoints[i + 1], segment.light, segment.number * splits + i, segment.split);
                next.previous = previous;
                if (previous != null) previous.next = next;
                if (i != 0 && random.nextFloat() < splitchance) {
                    Vector splitRotation = Vector.xCross(next.diff).rotate(random.nextFloat() * 360.0F, next.diff);
                    Vector difference = next.diff.copy()
                            .rotate((random.nextFloat() * 0.66F + 0.33F) * splitangle, splitRotation)
                            .scale(splitlength);
                    ++splitCount;
                    splitParents.put(splitCount, next.split);
                    Segment split = new Segment(newPoints[i],
                            new Point(newPoints[i + 1].base, newPoints[i + 1].offset.copy().add(difference)),
                            segment.light / 2.0F, next.number, splitCount);
                    split.previous = previous;
                    segments.add(split);
                }
                previous = next;
                segments.add(next);
            }
            if (segment.next == null) continue;
            segment.next.previous = previous;
        }
        // The original multiplies the main chain count with the split count of this step.
        mainSegments *= splits;
    }

    /**
     * The original {@code finalizeBolt()}: the collision pass that removes the shadowed segments and computes the
     * joint directions, then the sort by descending light.
     */
    public void finalizeBolt() {
        if (finalized) return;
        finalized = true;
        calculateCollisionAndDiffs();
        segments.sort(Comparator.comparingDouble((Segment segment) -> -segment.light));
    }

    /**
     * The original {@code calculateCollisionAndDiffs()}. The original removes a segment when its segment number is
     * larger than the last active segment of its own split; because the map stores the largest segment number of
     * every split, that test never succeeds and the pass only computes the joint directions. The port keeps the
     * original test so the behaviour stays provably identical.
     */
    private void calculateCollisionAndDiffs() {
        Map<Integer, Integer> lastActiveSegment = new HashMap<>();
        segments.sort(Comparator.comparingInt((Segment segment) -> segment.split).thenComparingInt(segment -> segment.number));
        int lastSplitCalc = 0;
        int lastActiveSeg = 0;
        for (Segment segment : segments) {
            if (segment.split > lastSplitCalc) {
                lastActiveSegment.put(lastSplitCalc, lastActiveSeg);
                lastSplitCalc = segment.split;
                Integer parent = splitParents.get(segment.split);
                lastActiveSeg = parent == null ? 0 : lastActiveSegment.getOrDefault(parent, 0);
            }
            lastActiveSeg = segment.number;
        }
        lastActiveSegment.put(lastSplitCalc, lastActiveSeg);
        lastSplitCalc = 0;
        lastActiveSeg = lastActiveSegment.getOrDefault(0, 0);
        List<Segment> kept = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            if (lastSplitCalc != segment.split) {
                lastSplitCalc = segment.split;
                lastActiveSeg = lastActiveSegment.getOrDefault(segment.split, 0);
            }
            if (segment.number > lastActiveSeg) continue;
            segment.calcEndDiffs();
            kept.add(segment);
        }
        segments.clear();
        segments.addAll(kept);
    }

    /** One original segment of the bolt. */
    public static final class Segment {
        private final Point start;
        private final Point end;
        /** The original {@code light}: one for the main chain, a half for a fork. */
        public final float light;
        /** The original {@code segmentno}. */
        public final int number;
        /** The original {@code splitno}. */
        public final int split;
        private final Vector diff;
        private Segment previous;
        private Segment next;
        private Vector previousDiff;
        private Vector nextDiff;
        private float sinPrevious;
        private float sinNext;

        Segment(Point start, Point end, float light, int number, int split) {
            this.start = start;
            this.end = end;
            this.light = light;
            this.number = number;
            this.split = split;
            this.diff = end.point.copy().sub(start.point);
        }

        void calcEndDiffs() {
            if (previous != null) {
                Vector previousDiffNorm = previous.diff.copy().normalize();
                Vector thisDiffNorm = diff.copy().normalize();
                previousDiff = thisDiffNorm.add(previousDiffNorm).normalize();
                sinPrevious = (float) Math.sin(Vector.anglePreNorm(thisDiffNorm, previousDiffNorm.scale(-1.0F)) / 2.0F);
            } else {
                previousDiff = diff.copy().normalize();
                sinPrevious = 1.0F;
            }
            if (next != null) {
                Vector nextDiffNorm = next.diff.copy().normalize();
                Vector thisDiffNorm = diff.copy().normalize();
                nextDiff = thisDiffNorm.add(nextDiffNorm).normalize();
                sinNext = (float) Math.sin(Vector.anglePreNorm(thisDiffNorm, nextDiffNorm.scale(-1.0F)) / 2.0F);
            } else {
                nextDiff = diff.copy().normalize();
                sinNext = 1.0F;
            }
        }

        public Vec3 start() { return start.point.nativeVector(); }
        public Vec3 end() { return end.point.nativeVector(); }
        public Vec3 direction() { return diff.nativeVector(); }
        public Vec3 previousDirection() { return previousDiff.nativeVector(); }
        public Vec3 nextDirection() { return nextDiff.nativeVector(); }
        public float sinPrevious() { return sinPrevious; }
        public float sinNext() { return sinNext; }
        public boolean first() { return previous == null; }
        public boolean last() { return next == null; }
        /**
         * The original draws the ribbon with the joint sines in the divisor. A joint sine that the normalised dot
         * product pushed out of the unit interval makes the original vertex coordinates infinite or not a number, and
         * the original rasteriser drops the whole quad. The port drops the same segment instead of clamping it.
         */
        public boolean renderable() {
            return Float.isFinite(sinPrevious) && sinPrevious != 0.0F && Float.isFinite(sinNext) && sinNext != 0.0F;
        }
    }

    /** The original {@code BoltPoint}: a base point and the offset that is added to it. */
    private record Point(Vector base, Vector offset, Vector point) {
        Point(Vector base, Vector offset) { this(base, offset, base.copy().add(offset)); }
    }

    /**
     * The original {@code WRVector3} with its {@code WRMat4} rotation: the original keeps the coordinates in
     * {@code float}, and the original {@code normalize()} divides by the {@code float} length.
     */
    private static final class Vector {
        float x;
        float y;
        float z;

        Vector(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

        static Vector of(Vec3 vector) { return new Vector((float) vector.x, (float) vector.y, (float) vector.z); }

        Vector add(Vector other) { x += other.x; y += other.y; z += other.z; return this; }
        Vector sub(Vector other) { x -= other.x; y -= other.y; z -= other.z; return this; }
        Vector scale(float factor) { x *= factor; y *= factor; z *= factor; return this; }

        float length() { return (float) Math.sqrt(x * x + y * y + z * z); }

        Vector normalize() {
            float length = length();
            x /= length;
            y /= length;
            z /= length;
            return this;
        }

        Vector copy() { return new Vector(x, y, z); }

        float dot(Vector other) { return x * other.x + y * other.y + z * other.z; }

        /** The original {@code WRVector3.rotate(float, WRVector3)} through {@code WRMat4.rotationMat}. */
        Vector rotate(float angle, Vector axis) {
            Vector normal = axis.copy().normalize();
            float a = normal.x;
            float b = normal.y;
            float c = normal.z;
            double radians = angle * 0.0174532925;
            float cos = (float) Math.cos(radians);
            float ocos = 1.0F - cos;
            float sin = (float) Math.sin(radians);
            float m0 = a * a * ocos + cos;
            float m1 = b * a * ocos + c * sin;
            float m2 = a * c * ocos - b * sin;
            float m4 = a * b * ocos - c * sin;
            float m5 = b * b * ocos + cos;
            float m6 = b * c * ocos + a * sin;
            float m8 = a * c * ocos + b * sin;
            float m9 = b * c * ocos - a * sin;
            float m10 = c * c * ocos + cos;
            float newX = x * m0 + y * m1 + z * m2;
            float newY = x * m4 + y * m5 + z * m6;
            float newZ = x * m8 + y * m9 + z * m10;
            x = newX;
            y = newY;
            z = newZ;
            return this;
        }

        static Vector cross(Vector first, Vector second) {
            return new Vector(first.y * second.z - first.z * second.y,
                    first.z * second.x - first.x * second.z,
                    first.x * second.y - first.y * second.x);
        }

        static Vector xCross(Vector vector) { return new Vector(0.0F, vector.z, -vector.y); }

        static Vector zCross(Vector vector) { return new Vector(-vector.y, vector.x, 0.0F); }

        static Vector perpendicular(Vector vector) {
            return vector.z == 0.0F ? zCross(vector) : xCross(vector);
        }

        static float anglePreNorm(Vector first, Vector second) {
            return (float) Math.acos(first.dot(second));
        }

        Vec3 nativeVector() { return new Vec3(x, y, z); }
    }
}

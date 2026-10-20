package thaumcraft.common.lib.utils;


/**
 * Original TC4 {@code thaumcraft.common.lib.utils.TCVec3}: the vanilla 1.7.10 three-component vector with its own
 * pool instead of the world pool. Pure maths: no world, no entity, no registry, no rendering.
 *
 * <p>The original file marked {@link #subtract(TCVec3)} and {@link #crossProduct(TCVec3)} client-only because they
 * existed for the renderer. They are ordinary arithmetic here and are available on both sides.
 */
public class TCVec3 {
    /** The shared pool of the original. {@code new TCVec3Pool(-1, -1)} disables pooling, so every vector is its own. */
    public static final TCVec3Pool vec3dPool = new TCVec3Pool(-1, -1);

    /** The pool that owns this vector; the original keeps it for the returned vectors. */
    public final TCVec3Pool myVec3LocalPool;

    public double xCoord;
    public double yCoord;
    public double zCoord;

    /** Original {@code createVectorHelper}. */
    public static TCVec3 createVectorHelper(double x, double y, double z) {
        return new TCVec3(vec3dPool, x, y, z);
    }

    protected TCVec3(TCVec3Pool pool, double x, double y, double z) {
        // The original folds negative zero into positive zero so that two equal vectors compare equal.
        if (x == -0.0) x = 0.0;
        if (y == -0.0) y = 0.0;
        if (z == -0.0) z = 0.0;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.myVec3LocalPool = pool;
    }

    /** Original {@code setComponents}: the pool's recycle path, not a public setter in the original. */
    protected TCVec3 setComponents(double x, double y, double z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        return this;
    }

    /** Original {@code subtract}. */
    public TCVec3 subtract(TCVec3 other) {
        return this.myVec3LocalPool.getVecFromPool(other.xCoord - this.xCoord, other.yCoord - this.yCoord, other.zCoord - this.zCoord);
    }

    /** Original {@code normalize}: a vector shorter than 1.0E-4 collapses to the origin. */
    public TCVec3 normalize() {
        double length = Math.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
        return length < 1.0E-4 ? this.myVec3LocalPool.getVecFromPool(0.0, 0.0, 0.0)
                : this.myVec3LocalPool.getVecFromPool(this.xCoord / length, this.yCoord / length, this.zCoord / length);
    }

    /** Original {@code dotProduct}. */
    public double dotProduct(TCVec3 other) {
        return this.xCoord * other.xCoord + this.yCoord * other.yCoord + this.zCoord * other.zCoord;
    }

    /** Original {@code crossProduct}. */
    public TCVec3 crossProduct(TCVec3 other) {
        return this.myVec3LocalPool.getVecFromPool(
                this.yCoord * other.zCoord - this.zCoord * other.yCoord,
                this.zCoord * other.xCoord - this.xCoord * other.zCoord,
                this.xCoord * other.yCoord - this.yCoord * other.xCoord);
    }

    /** Original {@code addVector}. */
    public TCVec3 addVector(double x, double y, double z) {
        return this.myVec3LocalPool.getVecFromPool(this.xCoord + x, this.yCoord + y, this.zCoord + z);
    }

    /** Original {@code distanceTo}. */
    public double distanceTo(TCVec3 other) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Original {@code squareDistanceTo}. */
    public double squareDistanceTo(TCVec3 other) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Original {@code squareDistanceTo(double, double, double)}. */
    public double squareDistanceTo(double x, double y, double z) {
        double dx = x - this.xCoord;
        double dy = y - this.yCoord;
        double dz = z - this.zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Original {@code lengthVector}. */
    public double lengthVector() {
        return Math.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
    }

    /** Original {@code getIntermediateWithXValue}: the point at the given x on the segment, or null. */
    public TCVec3 getIntermediateWithXValue(TCVec3 other, double x) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        if (dx * dx < 1.0E-7) return null;
        double t = (x - this.xCoord) / dx;
        return t >= 0.0 && t <= 1.0 ? this.myVec3LocalPool.getVecFromPool(this.xCoord + dx * t, this.yCoord + dy * t, this.zCoord + dz * t) : null;
    }

    /** Original {@code getIntermediateWithYValue}: the point at the given y on the segment, or null. */
    public TCVec3 getIntermediateWithYValue(TCVec3 other, double y) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        if (dy * dy < 1.0E-7) return null;
        double t = (y - this.yCoord) / dy;
        return t >= 0.0 && t <= 1.0 ? this.myVec3LocalPool.getVecFromPool(this.xCoord + dx * t, this.yCoord + dy * t, this.zCoord + dz * t) : null;
    }

    /** Original {@code getIntermediateWithZValue}: the point at the given z on the segment, or null. */
    public TCVec3 getIntermediateWithZValue(TCVec3 other, double z) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        if (dz * dz < 1.0E-7) return null;
        double t = (z - this.zCoord) / dz;
        return t >= 0.0 && t <= 1.0 ? this.myVec3LocalPool.getVecFromPool(this.xCoord + dx * t, this.yCoord + dy * t, this.zCoord + dz * t) : null;
    }

    @Override public String toString() {
        return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
    }

    /** Original {@code rotateAroundX}. */
    public void rotateAroundX(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double x = this.xCoord;
        double y = this.yCoord * cos + this.zCoord * sin;
        double z = this.zCoord * cos - this.yCoord * sin;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    /** Original {@code rotateAroundY}. */
    public void rotateAroundY(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double x = this.xCoord * cos + this.zCoord * sin;
        double y = this.yCoord;
        double z = this.zCoord * cos - this.xCoord * sin;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    /** Original {@code rotateAroundZ}. */
    public void rotateAroundZ(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double x = this.xCoord * cos + this.yCoord * sin;
        double y = this.yCoord * cos - this.xCoord * sin;
        double z = this.zCoord;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }
}

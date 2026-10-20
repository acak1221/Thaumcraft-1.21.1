package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Original TC4 {@code thaumcraft.common.lib.utils.TCVec3Pool}: the recycling pool of {@link TCVec3}.
 *
 * <p>A pool created with a negative minimum size or a negative reset threshold is disabled: it never caches and
 * hands out a fresh vector every time. That is the configuration the original uses for its shared pool
 * ({@code new TCVec3Pool(-1, -1)}).
 */
public class TCVec3Pool {
    private final int truncateArrayResetThreshold;
    private final int minimumSize;
    private final List<TCVec3> vec3Cache = new ArrayList<>();
    private int nextFreeSpace = 0;
    private int maximumSizeSinceLastTruncation = 0;
    private int resetCount = 0;

    public TCVec3Pool(int truncateArrayResetThreshold, int minimumSize) {
        this.truncateArrayResetThreshold = truncateArrayResetThreshold;
        this.minimumSize = minimumSize;
    }

    /** Original {@code getVecFromPool}. */
    public TCVec3 getVecFromPool(double x, double y, double z) {
        if (isDisabled()) return new TCVec3(this, x, y, z);
        TCVec3 vec;
        if (this.nextFreeSpace >= this.vec3Cache.size()) {
            vec = new TCVec3(this, x, y, z);
            this.vec3Cache.add(vec);
        } else {
            vec = this.vec3Cache.get(this.nextFreeSpace);
            vec.setComponents(x, y, z);
        }
        ++this.nextFreeSpace;
        return vec;
    }

    /** Original {@code clear}: keeps the cache but resets the write cursor, occasionally truncating it. */
    public void clear() {
        if (isDisabled()) return;
        if (this.nextFreeSpace > this.maximumSizeSinceLastTruncation) this.maximumSizeSinceLastTruncation = this.nextFreeSpace;
        if (this.resetCount++ == this.truncateArrayResetThreshold) {
            int keep = Math.max(this.maximumSizeSinceLastTruncation, this.vec3Cache.size() - this.minimumSize);
            while (this.vec3Cache.size() > keep) this.vec3Cache.remove(keep);
            this.maximumSizeSinceLastTruncation = 0;
            this.resetCount = 0;
        }
        this.nextFreeSpace = 0;
    }

    /** Original {@code clearAndFreeCache}: drops every cached vector. */
    public void clearAndFreeCache() {
        if (isDisabled()) return;
        this.nextFreeSpace = 0;
        this.vec3Cache.clear();
    }

    /** Original {@code getPoolSize}: the number of cached vectors. */
    public int getPoolSize() {
        return this.vec3Cache.size();
    }

    /** Original {@code func_82590_d}: the number of vectors handed out since the last clear. */
    public int getNextFreeSpace() {
        return this.nextFreeSpace;
    }

    /** Original {@code func_82589_e}: a negative threshold or minimum size disables the pool. */
    private boolean isDisabled() {
        return this.minimumSize < 0 || this.truncateArrayResetThreshold < 0;
    }
}

package thaumcraft.common.lib.world;

import java.io.Serializable;

/**
 * Oracle thaumcraft.common.lib.world.ChunkLoc: the chunk pair the TC4 world
 * generator and the chunk regeneration queue use to address a chunk.
 *
 * <p>1.7.10 net.minecraft.world.ChunkCoordIntPair became
 * net.minecraft.world.level.ChunkPos in 1.21.1, but the original centre
 * arithmetic - the middle block of the chunk - is kept here because the TC4
 * structures address their chunk centre through it.
 */
public class ChunkLoc implements Serializable {
    public final int chunkXPos;
    public final int chunkZPos;

    public ChunkLoc(int chunkXPos, int chunkZPos) {
        this.chunkXPos = chunkXPos;
        this.chunkZPos = chunkZPos;
    }

    /** Oracle ChunkLoc:22 - the original compared the two chunk coordinates. */
    public boolean equals(ChunkLoc other) {
        return other != null && other.chunkXPos == this.chunkXPos && other.chunkZPos == this.chunkZPos;
    }

    /** The 1.7.10 ChunkCoordIntPair comparison, oracle ChunkLoc:26. */
    public boolean equals(int chunkX, int chunkZ) {
        return chunkX == this.chunkXPos && chunkZ == this.chunkZPos;
    }

    @Override public boolean equals(Object other) {
        return other instanceof ChunkLoc loc && equals(loc);
    }

    @Override public int hashCode() {
        return this.chunkXPos * 31 + this.chunkZPos;
    }

    /** Oracle ChunkLoc:31 - the middle block of the chunk on the x axis. */
    public int getCenterXPos() {
        return (this.chunkXPos << 4) + 8;
    }

    /** Oracle ChunkLoc:35 - the middle block of the chunk on the z axis. */
    public int getCenterZPosition() {
        return (this.chunkZPos << 4) + 8;
    }

    @Override public String toString() {
        return "[" + this.chunkXPos + ", " + this.chunkZPos + "]";
    }
}

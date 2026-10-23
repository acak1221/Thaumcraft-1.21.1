package thaumcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The original TC4 mutable block coordinate.
 *
 * <p>Original {@code thaumcraft/api/BlockCoordinates.java}: a public holder of three coordinates with
 * {@code equals}, {@code hashCode}, {@code compareWorldCoordinate}, {@code set}, the two squared distances
 * and the NBT helpers of the keys {@code b_x}, {@code b_y} and {@code b_z}. The class was the published
 * value type of the architect preview of the original, so an add-on that highlights the blocks of an
 * architect area needs it.</p>
 *
 * <p>Forced 1.21.1 change: {@code TileEntity} became {@link BlockEntity} and {@code NBTTagCompound} became
 * {@link CompoundTag}; the coordinates, the comparison, the hash and the NBT keys are the original ones.
 * The native gameplay of the architect works on {@link BlockPos} and on the bounded
 * {@code thaumcraft.common.items.wands.WandArchitect.Area}, so this class carries no native state: it is
 * the published type of the original, nothing more.</p>
 */
public class BlockCoordinates implements Comparable<BlockCoordinates> {
    /** the x coordinate */
    public int x;

    /** the y coordinate */
    public int y;

    /** the z coordinate */
    public int z;

    /** Creates a mutable block coordinate at (0,0,0), without world access. */
    public BlockCoordinates() {}

    /** Stores x/y/z block coordinates without bounds validation or world access. */
    public BlockCoordinates(int par1, int par2, int par3) {
        this.x = par1;
        this.y = par2;
        this.z = par3;
    }

    /** Copies tile's block coordinates without retaining it; null tile throws NPE. */
    public BlockCoordinates(BlockEntity tile) {
        BlockPos pos = tile.getBlockPos();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    /** Copies other's block coordinates without retaining it; null other throws NPE. */
    public BlockCoordinates(BlockCoordinates other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof BlockCoordinates coordinates)) return false;
        return this.x == coordinates.x && this.y == coordinates.y && this.z == coordinates.z;
    }

    /**
     * The original hash of the coordinate.
     *
     * <p>Original {@code thaumcraft/api/BlockCoordinates.java}: the hash is the sum of the three
     * coordinates, shifted by eight and by sixteen bits. The readable 1.12 reference uses another
     * formula; the 4.2.3.5 bytecode of the original is the one reproduced here.</p>
     */
    @Override public int hashCode() {
        return (this.x + this.y + this.z) << 24;
    }

    /**
     * Compare the coordinate with another coordinate.
     *
     * <p>Original {@code thaumcraft/api/BlockCoordinates.java}: the y coordinate first, then the z
     * coordinate, then the x coordinate.</p>
     */
    public int compareWorldCoordinate(BlockCoordinates other) {
        return this.y == other.y ? (this.z == other.z ? this.x - other.x : this.z - other.z) : this.y - other.y;
    }

    /** The original setter; the fourth argument of the original was never used. */
    public void set(int par1, int par2, int par3, int d) {
        this.x = par1;
        this.y = par2;
        this.z = par3;
    }

    /**
     * The squared distance between this coordinate and the given one.
     *
     * <p>Original {@code thaumcraft/api/BlockCoordinates.java}: the result is a {@code float}, as it was in
     * 1.7.10.</p>
     */
    public float getDistanceSquared(int par1, int par2, int par3) {
        float f = (float) (this.x - par1);
        float f1 = (float) (this.y - par2);
        float f2 = (float) (this.z - par3);
        return f * f + f1 * f1 + f2 * f2;
    }

    /** The squared distance between this coordinate and another one. */
    public float getDistanceSquaredToWorldCoordinates(BlockCoordinates other) {
        return this.getDistanceSquared(other.x, other.y, other.z);
    }

    @Override public int compareTo(BlockCoordinates other) {
        return this.compareWorldCoordinate(other);
    }

    /** Null NBT leaves coordinates unchanged; missing/wrong-type b_x/b_y/b_z fields default individually to zero. */
    public void readNBT(CompoundTag nbt) {
        if (nbt == null) return;
        this.x = nbt.getInt("b_x");
        this.y = nbt.getInt("b_y");
        this.z = nbt.getInt("b_z");
    }

    /** The original NBT keys of the class. */
    public void writeNBT(CompoundTag nbt) {
        if (nbt == null) return;
        nbt.putInt("b_x", x);
        nbt.putInt("b_y", y);
        nbt.putInt("b_z", z);
    }
}

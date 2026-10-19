package thaumcraft.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Immutable modern location. A dimension key is not a world lifetime: VisNet
 * additionally scopes every graph/cache by the exact ServerLevel identity. */
public final class WorldCoordinates implements Comparable<WorldCoordinates> {
    public final int x, y, z;
    public final ResourceKey<Level> dimension;

    public WorldCoordinates(ResourceKey<Level> dimension, BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), dimension);
    }
    public WorldCoordinates(int x, int y, int z, ResourceKey<Level> dimension) {
        if (!isCanonicalDimension(dimension) || !Level.isInSpawnableBounds(new BlockPos(x, y, z))) {
            throw new IllegalArgumentException("Canonical dimension and native world coordinates required");
        }
        this.x = x; this.y = y; this.z = z; this.dimension = dimension;
    }
    public WorldCoordinates(BlockEntity tile) {
        this(Objects.requireNonNull(tile.getLevel(), "Unbound block entity").dimension(), tile.getBlockPos());
    }
    public WorldCoordinates(WorldCoordinates other) { this(other.x, other.y, other.z, other.dimension); }
    public BlockPos pos() { return new BlockPos(x, y, z); }
    public ResourceKey<Level> dimension() { return dimension; }
    public static boolean isCanonicalDimension(ResourceKey<Level> key) {
        return key != null && key.isFor(Registries.DIMENSION)
                && ResourceKey.create(Registries.DIMENSION, key.location()) == key;
    }
    public double getDistanceSquared(int x, int y, int z) {
        double dx = (double) this.x - x, dy = (double) this.y - y, dz = (double) this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
    public double getDistanceSquaredToWorldCoordinates(WorldCoordinates other) {
        return other != null && dimension == other.dimension ? getDistanceSquared(other.x, other.y, other.z) : Double.POSITIVE_INFINITY;
    }
    @Override public boolean equals(Object other) {
        return other instanceof WorldCoordinates at && dimension == at.dimension && x == at.x && y == at.y && z == at.z;
    }
    @Override public int hashCode() { return Objects.hash(dimension, x, y, z); }
    @Override public int compareTo(WorldCoordinates other) {
        int c = dimension.location().compareTo(other.dimension.location());
        if (c == 0) c = Integer.compare(y, other.y);
        if (c == 0) c = Integer.compare(z, other.z);
        return c == 0 ? Integer.compare(x, other.x) : c;
    }
    public int compareWorldCoordinate(WorldCoordinates other) { return compareTo(other); }
    public void writeNBT(CompoundTag tag) {
        tag.putInt("w_x", x); tag.putInt("w_y", y); tag.putInt("w_z", z);
        tag.putString("w_dim", dimension.location().toString());
    }
    /** Whole-value refusal: no missing/default coordinates or numeric dim IDs. */
    public static Optional<WorldCoordinates> readNBT(CompoundTag tag) {
        if (tag == null || !tag.contains("w_x", Tag.TAG_INT) || !tag.contains("w_y", Tag.TAG_INT)
                || !tag.contains("w_z", Tag.TAG_INT) || !tag.contains("w_dim", Tag.TAG_STRING)) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("w_dim"));
        if (id == null || !id.toString().equals(tag.getString("w_dim"))) return Optional.empty();
        try { return Optional.of(new WorldCoordinates(tag.getInt("w_x"), tag.getInt("w_y"), tag.getInt("w_z"), ResourceKey.create(Registries.DIMENSION, id))); }
        catch (IllegalArgumentException malformed) { return Optional.empty(); }
    }
    @Override public String toString() { return dimension.location() + "@" + x + "," + y + "," + z; }
}

package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.common.mixin.WorldGenRegionStepAccess;

/**
 * The area a feature may write to, asked without side effects.
 *
 * <p>Minecraft 1.21.1 lets a feature of the {@code features} step write only into the chunks within
 * {@code ChunkStep#blockStateWriteRadius} (one) of the chunk it decorates. The 1.7.10 original had no
 * such area: its {@code World#setBlock} loaded or generated whatever chunk a tree reached into, so the
 * port has to know where a feature has to stop. {@code WorldGenLevel#ensureCanWrite} gives that answer,
 * but {@code WorldGenRegion} logs a "Detected setBlock in a far chunk" error each time the answer is
 * no, and a feature that probes the corners of a canopy before it commits it would flood the log with
 * errors that are not errors. This asks the same question of the same data and stays silent.
 */
public final class WorldGenWriteArea {
    private static final java.util.concurrent.atomic.AtomicLong TREES_REFUSED = new java.util.concurrent.atomic.AtomicLong();

    private WorldGenWriteArea() {}

    /** A whole magical tree was dropped because a block of it lay outside the write area (never a partial tree). */
    static void treeRefused() { TREES_REFUSED.incrementAndGet(); }

    /** Number of magical trees refused for reaching outside the write area since the game started. */
    public static long treesRefused() { return TREES_REFUSED.get(); }

    /** True when {@code pos} lies in a chunk the feature that runs in {@code level} may write to. */
    public static boolean canWrite(WorldGenLevel level, BlockPos pos) {
        if (level instanceof WorldGenRegion region) {
            int radius = ((WorldGenRegionStepAccess) region).thaumcraft$getGeneratingStep().blockStateWriteRadius();
            return inside(region.getCenter(), radius, pos);
        }
        // A live level (a command, a test) has no write area of its own.
        return level.ensureCanWrite(pos);
    }

    /** The pure rule of {@code WorldGenRegion#ensureCanWrite}: the chunk of {@code pos} is within {@code radius} of the centre. */
    public static boolean inside(ChunkPos center, int radius, BlockPos pos) {
        return Math.abs(center.x - SectionPos.blockToSectionCoord(pos.getX())) <= radius
                && Math.abs(center.z - SectionPos.blockToSectionCoord(pos.getZ())) <= radius;
    }
}

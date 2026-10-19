package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftNaturals;

/**
 * Log recognition and endpoint selection for the Axe of the Stream (original
 * Utils.isWoodLog + BlockUtils.findBlocks).
 *
 * Finite scope, deliberately narrower than "canSustainLeaves": unstripped
 * oak/spruce/birch/jungle/acacia/dark-oak logs and the native Greatwood and
 * ordinary Silverwood logs. Stripped/all-bark wood, mangrove, cherry, bamboo,
 * nether stems and third-party trees are deferred, not swept in by a LOGS tag.
 * Modern logs are one Block per species, so the original same-Block walk can no
 * longer cross mixed species that shared a legacy block; that is a narrowing.
 */
public final class TreeLogTraversal {
    private TreeLogTraversal() {}

    public static boolean isLog(ServerLevel level, BlockPos pos, BlockState state) {
        return recognised(state.getBlock()) && state.getDestroySpeed(level, pos) >= 0;
    }

    private static boolean recognised(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG
                || block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG
                || block == ThaumcraftNaturals.GREATWOOD_LOG.get() || block == ThaumcraftNaturals.SILVERWOOD_LOG.get();
    }

    /**
     * The furthest same-Block log reached by the original greedy walk from {@code anchor}.
     * Null means refusal: the anchor is stale, unloaded, or no longer the expected log.
     * It is never permission to break some other fallback cell. Unloaded neighbours are
     * treated as "not a link" (the walk ends at the loaded edge); nothing is loaded or forced.
     */
    public static BlockPos endpoint(ServerLevel level, BlockPos anchor, BlockState expected) {
        if (!level.hasChunkAt(anchor) || level.getBlockState(anchor) != expected || !isLog(level, anchor, expected)) return null;
        Block block = expected.getBlock();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        StreamAxeGeometry.Cell cell = StreamAxeGeometry.walk(anchor.getX(), anchor.getY(), anchor.getZ(), (x, y, z) -> {
            cursor.set(x, y, z);
            if (level.isOutsideBuildHeight(cursor) || !level.hasChunkAt(cursor)) return false;
            BlockState state = level.getBlockState(cursor);
            return state.getBlock() == block && isLog(level, cursor, state);
        });
        return new BlockPos(cell.x(), cell.y(), cell.z());
    }
}

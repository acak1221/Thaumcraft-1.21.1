package thaumcraft.common.lib.world;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockMagicalLeaves;
import thaumcraft.common.blocks.BlockMagicalLog;

/** Private tree transaction/canopy adapter: no remote chunk reads or partial failed trees. */
final class MagicalTreePlacement {
    final WorldGenLevel level;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private boolean writable = true;
    private final Map<BlockPos, NodeGeneration.Plan> nodes = new LinkedHashMap<>();

    MagicalTreePlacement(WorldGenLevel level) { this.level = level; }

    boolean available(BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.ensureCanWrite(pos);
    }

    BlockState state(BlockPos pos) {
        if (!available(pos)) return Blocks.BEDROCK.defaultBlockState();
        return blocks.getOrDefault(pos, level.getBlockState(pos));
    }

    void put(BlockPos pos, BlockState state) {
        if (!available(pos)) { writable = false; return; }
        blocks.put(pos.immutable(), state);
    }

    void node(BlockPos pos, NodeGeneration.Plan plan) {
        if (!available(pos)) { writable = false; return; }
        nodes.put(pos.immutable(), plan);
    }

    void reject() { writable = false; }

    static boolean soil(BlockState state) { return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND); }
    static boolean leaves(BlockState state) { return state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock; }
    static boolean replaceableByLeaves(BlockState state) { return state.isAir() || leaves(state) || state.canBeReplaced(); }

    boolean finish() {
        if (!writable || blocks.isEmpty()) return false;
        // The old metadata combined species and axis. Flattening it must not
        // lose supporting logs or mark every generated leaf persistent. Compute
        // the native 1..7 distance against the ACTUAL two-tier branches/trunks,
        // including already-present trees, before exposing any leaf to ticks.
        blocks.replaceAll((pos, state) -> {
            if (!(state.getBlock() instanceof LeavesBlock)) return state;
            state = state.setValue(LeavesBlock.DISTANCE, 7).setValue(LeavesBlock.PERSISTENT, false);
            return state.hasProperty(BlockMagicalLeaves.CHECK_DECAY) ? state.setValue(BlockMagicalLeaves.CHECK_DECAY, false) : state;
        });
        for (int pass = 0; pass < 6; pass++) {
            for (var entry : blocks.entrySet()) {
                BlockState state = entry.getValue();
                if (!(state.getBlock() instanceof LeavesBlock)) continue;
                int distance = 7;
                for (Direction direction : Direction.values()) {
                    BlockState neighbor = state(entry.getKey().relative(direction));
                    int adjacent = neighbor.is(BlockTags.LOGS) || neighbor.getBlock() instanceof BlockMagicalLog ? 0
                            : neighbor.hasProperty(LeavesBlock.DISTANCE) ? neighbor.getValue(LeavesBlock.DISTANCE) : 7;
                    distance = Math.min(distance, adjacent + 1);
                }
                entry.setValue(state.setValue(LeavesBlock.DISTANCE, Math.min(7, distance)));
            }
        }
        // Geometry/RNG were planned in the original leaves-before-trunk order.
        // Native commit deliberately writes wood before the final leaf states:
        // replacing a temporary leaf with a log would invoke onRemove and mark
        // the brand-new canopy for decay before its support state is exposed.
        // Known-shape suppresses premature native distance cascades as well.
        for (boolean leaves : new boolean[] {false, true}) for (var entry : blocks.entrySet()) {
            if ((entry.getValue().getBlock() instanceof LeavesBlock) == leaves) {
                level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        // Real knot blocks and their shared TileNode exist before initializing the prepared original generation plan.
        for (var entry : nodes.entrySet()) {
            if (!level.getBlockState(entry.getKey()).is(thaumcraft.common.config.ThaumcraftContent.SILVERWOOD_KNOT.get())
                    || !(level.getBlockEntity(entry.getKey()) instanceof thaumcraft.common.tiles.TileNode node))
                throw new IllegalStateException("Committed Silverwood knot has no actual native TileNode at " + entry.getKey());
            var plan = entry.getValue(); node.initializeGenerated(plan.aspects(), plan.type(), plan.modifier(), level);
        }
        return true;
    }
}

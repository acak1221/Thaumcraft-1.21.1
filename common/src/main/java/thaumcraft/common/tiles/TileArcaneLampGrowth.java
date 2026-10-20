package thaumcraft.common.tiles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftBiologicalLamps;
import thaumcraft.common.lib.GrowthLampTicks;
import thaumcraft.common.lib.GrowthLampPlants;
import thaumcraft.common.lib.network.GrowthLampFxNetworking;

/** Original herba1 ->100 plant updates, one extra unit in reserve and one shuffled column scan per tick. */
public final class TileArcaneLampGrowth extends TileEssentiaLamp {
    private final ArrayDeque<BlockPos> columns = new ArrayDeque<>();
    private BlockPos last = BlockPos.ZERO, preparedTarget;
    private BlockState lastState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), preparedState;
    private boolean growthCommitted;
    public TileArcaneLampGrowth(BlockPos pos, BlockState state) { super(ThaumcraftBiologicalLamps.GROWTH_TILE.get(), pos, state, -1); }
    public int getRemainingColumns() { return columns.size(); }
    @Override protected void tickLamp() {
        if (charges <= 0) {
            if (reserve) { charges = 100; reserve = false; changed(); }
            else draw(Fill.GROWTH_FUEL);
        }
        if (!current() || isQuarantined()) return;
        if (!reserve) draw(Fill.GROWTH_RESERVE);
        if (!current() || isQuarantined()) return;
        if (charges == 0) { charges = -1; changed(); }
        if (current() && charges > 0) updatePlant();
    }
    private void updatePlant() {
        if (level.hasChunkAt(last)) {
            var now = level.getBlockState(last);
            if (now != lastState) { GrowthLampFxNetworking.send(this, last); lastState = now; }
        }
        if (columns.isEmpty()) {
            var shuffled = new ArrayList<BlockPos>(169);
            for (int x = -6; x <= 6; ++x) for (int z = -6; z <= 6; ++z) shuffled.add(worldPosition.offset(x, 6, z));
            // Exactly Collections.shuffle's descending Fisher-Yates draws, using the native world RandomSource.
            for (int size = shuffled.size(); size > 1; --size) Collections.swap(shuffled, size - 1, level.random.nextInt(size));
            columns.addAll(shuffled);
        }
        BlockPos column = columns.removeFirst(); // Original next get(0) after removal crashes on the169th column; all column tops have the same Y.
        for (int y = column.getY(); y >= worldPosition.getY() - 6; --y) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (worldPosition.distSqr(pos) >= 36 || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!GrowthLampPlants.eligible(level, pos, state) || !current() || level.getBlockState(pos) != state) continue;
            preparedTarget = pos; preparedState = state; growthCommitted = false;
            try {
                if (GrowthLampTicks.schedule(this, pos, state)) { last = pos; lastState = state; if (current()) changed(); }
            } finally { preparedTarget = null; preparedState = null; growthCommitted = false; }
            return;
        }
    }
    public boolean isNativeGrowthPhase(BlockPos pos, BlockState state) {
        return ticking && !growthCommitted && charges > 0 && !isQuarantined() && preparedTarget == pos && preparedState == state && current();
    }
    public void consumePreparedGrowthCharge(BlockPos pos, BlockState state) {
        GrowthLampTicks.requirePaidSchedule(this, pos, state);
        if (!ticking || growthCommitted || charges <= 0 || preparedTarget != pos || preparedState != state) throw new IllegalStateException("No prepared native plant update");
        --charges; growthCommitted = true;
    }
}

package thaumcraft.common.testing;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/** Await an ALREADY ticketed native load, without acquiring any extra terrain/ticket.
 * GameTestServer runs simulated ticks without a 50ms wall-clock floor. Disk/light
 * executors must therefore have a real-time budget, not a fixed number of test ticks.
 */
public final class GameTestChunkAwait {
    private static final Duration LOAD_DEADLINE = Duration.ofSeconds(10);

    private GameTestChunkAwait() {}

    public static LevelChunk ticketed(GameTestHelper test, ChunkPos pos) {
        var source = test.getLevel().getChunkSource();
        long deadline = System.nanoTime() + LOAD_DEADLINE.toNanos();
        LevelChunk chunk;
        while ((chunk = source.getChunkNow(pos.x, pos.z)) == null && System.nanoTime() < deadline) {
            // The same distance-manager/generation/light/main-thread work vanilla's
            // chunk managedBlock services. Never call getChunk/getChunkFuture(true):
            // those would introduce an UNKNOWN ticket and invalidate partial-load proof.
            if (!source.pollTask()) LockSupport.parkNanos(1_000_000);
        }
        test.assertTrue(chunk != null, "ticketed native chunk " + pos + " did not reach FULL within "
                + LOAD_DEADLINE + "; state=" + source.getChunkDebugData(pos)
                + "; pendingTasks=" + source.getPendingTasksCount());
        return chunk;
    }

    /** Await entity ticking at a position whose chunk the caller ALREADY forced. A chunk reaches FULL before its entity
     * section is visible: the visibility change is a later main-thread task, and until it runs an entity added there is
     * not found by {@code ServerLevel.getEntity}. Same real-time budget, no extra ticket.
     */
    public static void entityTicking(GameTestHelper test, BlockPos pos) {
        var level = test.getLevel();
        var source = level.getChunkSource();
        long deadline = System.nanoTime() + LOAD_DEADLINE.toNanos();
        while (!level.isPositionEntityTicking(pos) && System.nanoTime() < deadline) {
            if (!source.pollTask()) LockSupport.parkNanos(1_000_000);
        }
        var chunk = new ChunkPos(pos);
        test.assertTrue(level.isPositionEntityTicking(pos), "forced chunk " + chunk + " did not tick entities within "
                + LOAD_DEADLINE + "; state=" + source.getChunkDebugData(chunk)
                + "; pendingTasks=" + source.getPendingTasksCount());
    }
}

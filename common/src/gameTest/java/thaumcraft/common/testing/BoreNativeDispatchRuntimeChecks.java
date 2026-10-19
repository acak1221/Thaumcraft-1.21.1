package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;

/** TEST-ONLY read observation during the actual native BLOCK_DESTROY call.
 * No supplied permit, dispatch, callback outcome, resource or mutation override. */
public final class BoreNativeDispatchRuntimeChecks {
    private static final ThreadLocal<Observation> ACTIVE = new ThreadLocal<>();
    private BoreNativeDispatchRuntimeChecks() {}

    public static void outerDestroyCallbackRemainsClosed(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open(); f.put(f.base, Blocks.STONE);
            f.check(BoreNativeContext.isLiveLevel(f.level) && BoreNativeContext.canDispatchOutputs(f.level),
                    "canonical native level outside work/mutation can dispatch already-owned Bore rows");
            if (ACTIVE.get() != null) throw new IllegalStateException("Overlapping native destroy callback probe");
            var observation = new Observation(f.level, f.base);
            ACTIVE.set(observation);
            boolean removed;
            try { removed = f.level.destroyBlock(f.base, false, f.player, 512); }
            finally { ACTIVE.remove(); }
            f.check(removed && f.level.getBlockState(f.base).isAir(), "one real native four-argument destroyBlock removed its declared stone without output");
            f.check(observation.failure == null && observation.events == 1 && observation.targetAlreadyAir
                    && !observation.allowed, "actual BLOCK_DESTROY callback after inner setBlock sees target air but dispatch still CLOSED until outer destroy returns");
            f.check(BoreNativeContext.canDispatchOutputs(f.level), "native outer return releases only the invocation fence, not permanent delivery denial");
            f.check(f.drops().isEmpty(), "test probe created no output, XP or paid-ownership claim");
        }
        test.succeed();
    }

    /** Non-cancelling isolated GameTest HEAD witness, never throws into Minecraft. */
    public static void observeNativeEvent(ServerLevel level, Holder<GameEvent> event, Vec3 pos) {
        Observation observation = ACTIVE.get();
        if (observation == null || observation.level != level || !event.equals(GameEvent.BLOCK_DESTROY)
                || !BlockPos.containing(pos).equals(observation.target)) return;
        try {
            ++observation.events;
            observation.targetAlreadyAir = level.getBlockState(observation.target).isAir();
            observation.allowed = BoreNativeContext.canDispatchOutputs(level);
        } catch (Throwable failure) { observation.failure = failure; }
    }
    private static final class Observation {
        private final ServerLevel level;
        private final BlockPos target;
        private int events;
        private boolean targetAlreadyAir, allowed;
        private Throwable failure;
        private Observation(ServerLevel level, BlockPos target) { this.level = level; this.target = target.immutable(); }
    }
}

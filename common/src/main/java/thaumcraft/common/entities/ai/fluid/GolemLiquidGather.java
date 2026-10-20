package thaumcraft.common.entities.ai.fluid;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemLiquidQueue;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AILiquidGather} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/fluid/AILiquidGather.java}).
 *
 * <p>The golem fills its tank from a marked water block next to it: the first pulse happens after ten ticks and it takes a
 * bucket at a time, until the tank is full or the goal's twenty ticks are over. With an Entropy upgrade the original also
 * walked the connected water body and drained it block by block; the port does the same with the native
 * {@link GolemLiquidQueue}.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemLiquidGather extends Goal {
    /** The original 1.7 goal priority of {@code AILiquidGather}. */
    public static final int PRIORITY = 2;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int count;

    public GolemLiquidGather(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemLiquidGather(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (cargo.waterMb() >= cargo.fluidCarryLimit()) return false;
        if (!golem.getNavigation().isDone()) return false;
        return source() != null;
    }

    @Override public boolean canContinueToUse() { return count < GolemAiRules.LIQUID_GATHER_MAX_TICKS && source() != null; }

    /** Original {@code func_75252_g}: the gather goal is never interrupted. */
    @Override public boolean isInterruptable() { return false; }

    @Override public void start() { count = 0; }

    @Override public void stop() { count = 0; }

    @Override public void tick() {
        if (++count < GolemAiRules.LIQUID_GATHER_FIRST_PULSE) return;
        BlockPos source = source();
        if (source == null) {
            count = GolemAiRules.LIQUID_GATHER_MAX_TICKS;
            return;
        }
        if (!(golem.level() instanceof ServerLevel server)) return;
        int limit = cargo.fluidCarryLimit();
        if (GolemAiRules.fluidNearFull(cargo.waterMb(), limit)) return;
        BlockState state = server.getBlockState(source);
        if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) return;
        if (!server.setBlock(source, Blocks.AIR.defaultBlockState(), 3)) return;
        cargo.setWaterMb(cargo.waterMb() + GolemAiRules.BUCKET_MB);
        server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL,
                0.2F, 1.0F + (server.random.nextFloat() - server.random.nextFloat()) * 0.3F);
        count = 0;
    }

    /**
     * The next water block the golem drains: the marked block itself, or with an Entropy upgrade the head of the queue of
     * the connected water body, exactly as the original did.
     */
    private BlockPos source() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos marked = null;
        double nearest = Double.MAX_VALUE;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            BlockPos pos = marker.pos();
            if (!server.hasChunkAt(pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) continue;
            double distance = golem.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance >= GolemAiRules.ADJACENT_RANGE_SQ || distance >= nearest) continue;
            nearest = distance;
            marked = pos;
        }
        if (marked == null) return null;
        if (golem.getUpgradeAmount(GolemUpgrade.ENTROPY) <= 0) return marked;
        float range = golem.getRange();
        for (BlockPos candidate : GolemLiquidQueue.build(marked, range,
                pos -> server.hasChunkAt(pos) && server.getBlockState(pos).is(Blocks.WATER),
                pos -> server.hasChunkAt(pos) && server.getBlockState(pos).is(Blocks.WATER)
                        && server.getBlockState(pos).getFluidState().isSource())) {
            if (!GolemLiquidQueue.inRange(marked, candidate, range)) continue;
            if (!server.hasChunkAt(candidate)) continue;
            BlockState state = server.getBlockState(candidate);
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) return candidate;
        }
        return marked;
    }
}

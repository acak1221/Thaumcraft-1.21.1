package thaumcraft.common.entities.ai.fluid;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;
import thaumcraft.common.tiles.TileAlembic;

/**
 * Original {@code AIEssentiaGather} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java}).
 *
 * <p>The golem stands at its home and takes the essentia of the container behind it: an essentia transport is asked from the
 * home's own face, and when there is none the alembic above the home is used, the fullest one of the six levels above the
 * home winning. A golem only takes essentia of the aspect it already carries, and never more than its carrying limit.
 * Two attempts a second at most, and a tenth of a second after a successful take.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemEssentiaGather extends Goal {
    /** The original 1.7 goal priority of {@code AIEssentiaGather}. */
    public static final int PRIORITY = 2;
    /** The original: the alembic scan looks six levels above the home. */
    public static final int ALEMBIC_LEVELS = 6;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private long delay;
    private int level;

    public GolemEssentiaGather(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemEssentiaGather(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!golem.getNavigation().isDone()) return false;
        if (delay > System.currentTimeMillis()) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos home = golem.getHomePosition();
        Direction facing = golem.getHomeFacing();
        BlockPos source = home.relative(facing.getOpposite());
        if (golem.distanceToSqr(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5) > 6.0) return false;
        level = 0;
        BlockEntity tile = server.getBlockEntity(source);
        if (tile != null) {
            if (tile instanceof IEssentiaTransport transport) {
                if (!canTake(transport, facing)) return false;
                delay = System.currentTimeMillis() + GolemAiRules.ESSENTIA_GATHER_DELAY_MS;
                return true;
            }
            // Original: no transport at the home, so the alembic above the home is scanned.
            level = -1;
            int previousTotal = -1;
            for (int up = ALEMBIC_LEVELS - 1; up >= 0; --up) {
                BlockEntity above = server.getBlockEntity(source.above(up));
                if (!(above instanceof TileAlembic alembic)) continue;
                if (cargo.essentiaAmount() != 0 && cargo.aspect() != null && cargo.aspect() != alembic.getAspect()) continue;
                if (cargo.essentiaAmount() >= cargo.carryLimit()) continue;
                if (alembic.getAmount() <= previousTotal) continue;
                delay = System.currentTimeMillis() + GolemAiRules.ESSENTIA_GATHER_DELAY_MS;
                level = up;
                previousTotal = alembic.getAmount();
            }
            return level >= 0;
        }
        return false;
    }

    /** Original: the transport has to output to the home's face and offer the aspect the golem already carries. */
    private boolean canTake(IEssentiaTransport transport, Direction facing) {
        if (transport.getEssentiaAmount(facing) <= 0) return false;
        if (!transport.canOutputTo(facing)) return false;
        if (cargo.essentiaAmount() == 0) return true;
        Aspect carried = cargo.aspect();
        Aspect offered = transport.getEssentiaType(facing);
        if (carried == null || carried == offered || offered == null) return true;
        return false;
    }

    @Override public void start() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        BlockPos home = golem.getHomePosition();
        Direction facing = golem.getHomeFacing();
        BlockPos source = home.relative(facing.getOpposite()).above(Math.max(0, level));
        BlockEntity tile = server.getBlockEntity(source);
        if (!(tile instanceof IEssentiaTransport transport)) return;
        Direction face = transport instanceof TileAlembic || tile instanceof thaumcraft.common.tiles.TileJar
                ? Direction.UP : facing;
        if (transport.getEssentiaAmount(face) == 0) return;
        if (!transport.canOutputTo(face) || transport.getEssentiaAmount(face) <= 0) return;
        if (cargo.essentiaAmount() != 0 && !GolemAiRules.essentiaAspectMatches(cargo.aspect(), transport.getEssentiaType(face))) return;
        if (cargo.essentiaAmount() >= cargo.carryLimit()) return;
        Aspect aspect = transport.getEssentiaType(face);
        if (aspect == null) aspect = transport.getEssentiaType(null);
        if (aspect == null) return;
        int available = transport.getEssentiaAmount(face);
        int wanted = GolemAiRules.essentiaGatherAmount(available, cargo.essentiaAmount(), cargo.carryLimit());
        if (wanted <= 0) return;
        int taken = transport.takeEssentia(aspect, wanted, face);
        if (taken > 0) {
            cargo.setEssentia(aspect == null ? cargo.aspect() : aspect, cargo.essentiaAmount() + taken);
            server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), net.minecraft.sounds.SoundEvents.GENERIC_SWIM,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.05F, 1.0F + (server.random.nextFloat() - server.random.nextFloat()) * 0.3F);
        } else {
            cargo.setEssentia(null, 0);
        }
        delay = System.currentTimeMillis() + GolemAiRules.ESSENTIA_GATHER_REPEAT_MS;
    }
}

package thaumcraft.common.entities.ai.fluid;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;
import thaumcraft.common.tiles.TileJar;

/**
 * Original {@code AIEssentiaEmpty} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/fluid/AIEssentiaEmpty.java}).
 *
 * <p>The golem empties the essentia it carries into the nearest marked jar that has room, as soon as it stands next to that
 * jar. A reservoir is filled through its own suction face, every other essentia transport through the marked face. The goal
 * is a single act: it never continues on its own.</p>
 *
 * <p>The original registered this goal with priority 1.</p>
 */
public final class GolemEssentiaEmpty extends Goal {
    /** The original 1.7 goal priority of {@code AIEssentiaEmpty}. */
    public static final int PRIORITY = 1;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private BlockPos jarPos;
    private GolemMarker marker;

    public GolemEssentiaEmpty(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemEssentiaEmpty(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!golem.getNavigation().isDone()) return false;
        Aspect aspect = cargo.aspect();
        if (aspect == null || cargo.essentiaAmount() == 0) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos best = null;
        GolemMarker chosen = null;
        double nearest = Double.MAX_VALUE;
        for (GolemMarker candidate : golem.getMarkers()) {
            if (!candidate.dimension().equals(server.dimension().location())) continue;
            BlockEntity tile = server.getBlockEntity(candidate.pos());
            if (!(tile instanceof IEssentiaTransport transport)) continue;
            if (!accepts(transport, candidate, aspect)) continue;
            double distance = golem.distanceToSqr(candidate.pos().getX() + 0.5, candidate.pos().getY() + 0.5, candidate.pos().getZ() + 0.5);
            if (distance > GolemAiRules.ESSENTIA_EMPTY_REACH_SQ) continue;
            if (distance >= nearest) continue;
            nearest = distance;
            best = candidate.pos();
            chosen = candidate;
        }
        if (best == null) return false;
        jarPos = best;
        marker = chosen;
        return true;
    }

    /** Original {@code AIEssentiaEmpty}: the goal never continues on its own. */
    @Override public boolean canContinueToUse() { return false; }

    @Override public void start() {
        if (jarPos == null || marker == null || !(golem.level() instanceof ServerLevel server)) return;
        BlockEntity tile = server.getBlockEntity(jarPos);
        if (!(tile instanceof IEssentiaTransport transport)) return;
        Aspect aspect = cargo.aspect();
        int amount = cargo.essentiaAmount();
        if (aspect == null || amount <= 0) return;
        int added = transport.addEssentia(aspect, amount, marker.face());
        if (added <= 0) return;
        cargo.setEssentia(cargo.essentiaAmount() - added <= 0 ? null : aspect, cargo.essentiaAmount() - added);
        server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL,
                0.2F, 1.0F + (server.random.nextFloat() - server.random.nextFloat()) * 0.3F);
        server.blockUpdated(jarPos, server.getBlockState(jarPos).getBlock());
    }

    private boolean accepts(IEssentiaTransport transport, GolemMarker candidate, Aspect aspect) {
        if (!transport.canInputFrom(candidate.face())) return false;
        if (transport.getSuctionAmount(candidate.face()) <= 0) return false;
        Aspect suction = transport.getSuctionType(candidate.face());
        if (suction != null && suction != aspect) return false;
        if (transport instanceof TileJar jar) {
            if (jar.getAmount() >= jar.getMaxAmount()) return false;
            return jar.getAmount() == 0 || jar.getAspect() == aspect;
        }
        return true;
    }
}

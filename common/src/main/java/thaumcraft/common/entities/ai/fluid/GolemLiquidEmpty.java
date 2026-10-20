package thaumcraft.common.entities.ai.fluid;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;
import thaumcraft.common.tiles.TileCrucible;

/**
 * Original {@code AILiquidEmpty} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/fluid/AILiquidEmpty.java}).
 *
 * <p>The golem pours the fluid it carries back into the container behind its home, as soon as it stands close enough to
 * that container. The goal is a single act: it never continues on its own.</p>
 *
 * <p>The original was written for every registered fluid; the native golem's tank holds water only, so the port pours
 * water into the home crucible, which is the native counterpart of the original's fluid container.</p>
 *
 * <p>The original registered this goal with priority 1.</p>
 */
public final class GolemLiquidEmpty extends Goal {
    /** The original 1.7 goal priority of {@code AILiquidEmpty}. */
    public static final int PRIORITY = 1;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;

    public GolemLiquidEmpty(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemLiquidEmpty(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (cargo.waterMb() <= 0) return false;
        if (!golem.getNavigation().isDone()) return false;
        BlockPos home = golem.getHomePosition();
        if (golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                > GolemAiRules.LIQUID_EMPTY_REACH_SQ) return false;
        return homeContainer() != null;
    }

    /** Original {@code func_75253_b}: the goal never continues on its own. */
    @Override public boolean canContinueToUse() { return false; }

    @Override public void start() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        TileCrucible home = homeContainer();
        if (home == null) return;
        int accepted = home.fillWater(cargo.waterMb(), true);
        if (accepted <= 0) return;
        cargo.setWaterMb(cargo.waterMb() - accepted);
        if (accepted > 200) server.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.GENERIC_SWIM,
                SoundSource.NEUTRAL, Math.min(0.2F, 0.2F * ((float) accepted / (float) cargo.fluidCarryLimit())),
                1.0F + (server.random.nextFloat() - server.random.nextFloat()) * 0.3F);
        server.blockUpdated(home.getBlockPos(), server.getBlockState(home.getBlockPos()).getBlock());
    }

    /** The container behind the golem's home, which is where the original put the fluid back. */
    private TileCrucible homeContainer() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        BlockPos pos = home.relative(golem.getHomeFacing().getOpposite());
        if (!server.hasChunkAt(pos)) return null;
        return server.getBlockEntity(pos) instanceof TileCrucible crucible ? crucible : null;
    }
}

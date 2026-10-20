package thaumcraft.common.entities.ai.fluid;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AILiquidGoto} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/fluid/AILiquidGoto.java}).
 *
 * <p>The golem walks to the marked water it still needs. The original picked the closest of the nine blocks around the
 * marked source that it could stand on, and gave the whole search up once its tank was nearly full. When the golem does not
 * move for two hundred ticks it takes a small detour, exactly as the original did.</p>
 *
 * <p>The original registered this goal with priority 3.</p>
 */
public final class GolemLiquidGoto extends Goal {
    /** The original 1.7 goal priority of {@code AILiquidGoto}. */
    public static final int PRIORITY = 3;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private double targetX, targetY, targetZ;
    private int count;
    private int previousX, previousY, previousZ;

    public GolemLiquidGoto(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemLiquidGoto(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (GolemAiRules.fluidNearFull(cargo.waterMb(), cargo.fluidCarryLimit())) return false;
        BlockPos source = source();
        if (source == null) return false;
        targetX = source.getX();
        targetY = source.getY();
        targetZ = source.getZ();
        // Original: the golem stands on the closest of the nine blocks around the source that it may occupy.
        double best = golem.distanceToSqr(targetX, targetY, targetZ);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                double distance = golem.distanceToSqr(source.getX() + dx, targetY, source.getZ() + dz);
                if (!(distance < best)) continue;
                if (!standable(source.getX() + dx, source.getY(), source.getZ() + dz)) continue;
                targetX = source.getX() + dx;
                targetZ = source.getZ() + dz;
                best = distance;
            }
        }
        return true;
    }

    @Override public boolean canContinueToUse() { return count > 0 && !golem.getNavigation().isDone(); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        BlockPos here = golem.blockPosition();
        previousX = here.getX();
        previousY = here.getY();
        previousZ = here.getZ();
        golem.getNavigation().moveTo(targetX, targetY, targetZ, golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() { count = 0; }

    @Override public void tick() {
        if (--count != 0) return;
        BlockPos here = golem.blockPosition();
        if (previousX != here.getX() || previousY != here.getY() || previousZ != here.getZ()) return;
        Vec3 detour = DefaultRandomPos.getPos(golem, 2, 1);
        if (detour == null) return;
        count = GolemAiRules.detourBudget();
        golem.getNavigation().moveTo(detour.x, detour.y, detour.z, golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    /** The nearest marked water source inside the golem's range. */
    private BlockPos source() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        float range = golem.getRange();
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            BlockPos pos = marker.pos();
            if (!server.hasChunkAt(pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (!state.is(Blocks.WATER) || !state.getFluidState().isSource()) continue;
            double distance = Math.sqrt(home.distSqr(pos));
            if (distance > range || distance >= nearest) continue;
            nearest = distance;
            best = pos;
        }
        return best;
    }

    /** Original {@code world.func_147445_c(x, y, z, true)}: the block may be stood on. */
    private boolean standable(int x, int y, int z) {
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos pos = new BlockPos(x, y, z);
        if (!server.hasChunkAt(pos)) return false;
        BlockState state = server.getBlockState(pos);
        return state.isAir() || !state.getCollisionShape(server, pos).isEmpty() && state.isSolidRender(server, pos);
    }
}

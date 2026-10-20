package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AISortingGoto} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AISortingGoto.java}).
 *
 * <p>The Sorting golem walks to the marked container that already holds a copy of the item it carries, so that the item can
 * be put in with its own kind. The container behind the golem's own home is never a target. After two hundred ticks without
 * moving it takes a small detour, exactly as the original did.</p>
 *
 * <p>The original registered this goal with priority 3.</p>
 */
public final class GolemSortingGoto extends Goal {
    /** The original 1.7 goal priority of {@code AISortingGoto}. */
    public static final int PRIORITY = 3;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private BlockPos destination;
    private double moveX, moveY, moveZ;
    private int count;
    private int previousX, previousY, previousZ;

    public GolemSortingGoto(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemSortingGoto(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos homeContainer = golem.getHomePosition().relative(golem.getHomeFacing().getOpposite());
        double nearest = Double.MAX_VALUE;
        BlockPos best = null;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            if (marker.pos().equals(homeContainer)) continue;
            Container container = GolemAiRules.Containers.container(server, marker.pos());
            if (container == null) continue;
            boolean holds = false;
            for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), -1)) {
                if (GolemAiRules.Containers.holds(container, side, carried)) { holds = true; break; }
            }
            if (!holds) continue;
            double distance = golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5);
            if (!GolemAiRules.nearerThan(distance, nearest)) continue;
            if (!GolemAiRules.withinRange(distance, golem.getRange())) continue;
            nearest = distance;
            best = marker.pos();
        }
        if (best == null) return false;
        destination = best;
        moveX = best.getX();
        moveY = best.getY();
        moveZ = best.getZ();
        return true;
    }

    @Override public boolean canContinueToUse() { return count > 0 && !golem.getNavigation().isDone(); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        BlockPos here = golem.blockPosition();
        previousX = here.getX();
        previousY = here.getY();
        previousZ = here.getZ();
        golem.getNavigation().moveTo(moveX, moveY, moveZ, golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() {
        count = 0;
        destination = null;
    }

    @Override public void tick() {
        if (--count != 0) return;
        BlockPos here = golem.blockPosition();
        if (previousX != here.getX() || previousY != here.getY() || previousZ != here.getZ()) return;
        Vec3 detour = DefaultRandomPos.getPos(golem, 2, 1);
        if (detour == null) return;
        count = GolemAiRules.detourBudget();
        golem.getNavigation().moveTo(detour.x, detour.y, detour.z, golem.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    /** The container the golem walks to. */
    public BlockPos destination() { return destination; }
}

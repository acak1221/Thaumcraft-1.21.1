package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
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
 * Original {@code AIEmptyGoto} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIEmptyGoto.java}).
 *
 * <p>The golem walks to the nearest marked container that still has room for the item it carries; the container behind its
 * own home is never a target. When there is no such container the golem walks to the nearest marker that is not a container
 * at all and drops the item there. After two hundred ticks without moving it takes a small detour, exactly as the original
 * did.</p>
 *
 * <p>The original registered this goal with priority 3.</p>
 */
public final class GolemEmptyGoto extends Goal {
    /** The original 1.7 goal priority of {@code AIEmptyGoto}. */
    public static final int PRIORITY = 3;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private BlockPos destination;
    private double moveX, moveY, moveZ;
    private int count;
    private int previousX, previousY, previousZ;

    public GolemEmptyGoto(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemEmptyGoto(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
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
        BlockPos home = golem.getHomePosition();
        BlockPos homeContainer = home.relative(golem.getHomeFacing().getOpposite());
        double nearest = Double.MAX_VALUE;
        BlockPos best = null;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            if (!golem.markerAccepts(marker, carried)) continue;
            Container container = GolemAiRules.Containers.container(server, marker.pos());
            if (container == null) continue;
            if (marker.pos().equals(homeContainer)) continue;
            boolean room = false;
            for (var side : GolemAiRules.Containers.markedSides(golem, marker.pos(), marker.color())) {
                if (GolemAiRules.Containers.hasRoom(container, side, carried)) { room = true; break; }
            }
            if (!room) continue;
            double distance = golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5);
            if (!GolemAiRules.nearerThan(distance, nearest)) continue;
            if (!GolemAiRules.withinRange(distance, golem.getRange())) continue;
            nearest = distance;
            best = marker.pos();
        }
        if (best != null) {
            destination = best;
            moveX = best.getX();
            moveY = best.getY();
            moveZ = best.getZ();
            return true;
        }
        // Original: without a container the golem walks to a plain marker and drops the item there.
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            if (!golem.markerAccepts(marker, carried)) continue;
            if (GolemAiRules.Containers.container(server, marker.pos()) != null) continue;
            destination = marker.pos();
            moveX = marker.pos().getX();
            moveY = marker.pos().getY();
            moveZ = marker.pos().getZ();
            return true;
        }
        return false;
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

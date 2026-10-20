package thaumcraft.common.entities.ai.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
 * Original {@code AIFillGoto} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIFillGoto.java}).
 *
 * <p>The Fill golem walks to the nearest marked container that holds one of the goods its slots still ask for. The
 * container behind its own home is never a target. After two hundred ticks without moving it takes a small detour, exactly
 * as the original did.</p>
 *
 * <p>The original registered this goal with priority 4.</p>
 */
public final class GolemFillGoto extends Goal {
    /** The original 1.7 goal priority of {@code AIFillGoto}. */
    public static final int PRIORITY = 4;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private BlockPos destination;
    private double moveX, moveY, moveZ;
    private int count;
    private int previousX, previousY, previousZ;

    public GolemFillGoto(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemFillGoto(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried != null && !carried.isEmpty()) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        List<ItemStack> missing = missingItems();
        if (missing.isEmpty()) return false;
        BlockPos homeContainer = golem.getHomePosition().relative(golem.getHomeFacing().getOpposite());
        double nearest = Double.MAX_VALUE;
        BlockPos best = null;
        for (ItemStack wanted : missing) {
            for (GolemMarker marker : golem.getMarkers()) {
                if (!marker.dimension().equals(server.dimension().location())) continue;
                if (!golem.markerAccepts(marker, wanted)) continue;
                Container container = GolemAiRules.Containers.container(server, marker.pos());
                if (container == null) continue;
                if (marker.pos().equals(homeContainer)) continue;
                boolean holds = false;
                for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), marker.color())) {
                    if (GolemAiRules.Containers.holds(container, side, wanted)) { holds = true; break; }
                }
                if (!holds) continue;
                double distance = golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5);
                if (!GolemAiRules.nearerThan(distance, nearest)) continue;
                if (!GolemAiRules.withinRange(distance, golem.getRange())) continue;
                nearest = distance;
                best = marker.pos();
            }
            if (best != null) break;
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

    /** Original {@code GolemHelper.getMissingItems}: the goods the home container still lacks. */
    private List<ItemStack> missingItems() {
        List<ItemStack> missing = new ArrayList<>();
        if (!(golem.level() instanceof ServerLevel server)) return missing;
        BlockPos home = golem.getHomePosition();
        Container homeContainer = GolemAiRules.Containers.container(server, home.relative(golem.getHomeFacing().getOpposite()));
        for (int slot = 0; slot < golem.getSlotCount(); slot++) {
            ItemStack filter = golem.getFilter(slot);
            if (filter.isEmpty()) continue;
            int wanted = golem.getWantedCount(slot);
            if (wanted <= 0) continue;
            long present = homeContainer == null ? 0
                    : GolemAiRules.Containers.visibleCount(homeContainer, golem.getHomeFacing(), filter);
            int lack = (int) Math.max(0L, wanted - present);
            if (lack <= 0) continue;
            missing.add(filter.copyWithCount(lack));
        }
        return missing;
    }
}

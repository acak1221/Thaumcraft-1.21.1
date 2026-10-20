package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIEmptyDrop} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIEmptyDrop.java}).
 *
 * <p>The golem drops the item it carries at a marked block next to it that is not a container: the marker colour has to be
 * one of the colours of the golem's slots that match the carried item, and the golem's own home is never such a place. The
 * dropped stack is thrown at the marker and waits ten ticks before it may be picked up.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemEmptyDrop extends Goal {
    /** The original 1.7 goal priority of {@code AIEmptyDrop}. */
    public static final int PRIORITY = 2;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int count;

    public GolemEmptyDrop(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemEmptyDrop(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (!golem.getNavigation().isDone()) return false;
        return dropPlace() != null;
    }

    @Override public boolean canContinueToUse() { return count > 0 && canUse(); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        BlockPos place = dropPlace();
        if (place == null) return;
        if (!(golem.level() instanceof ServerLevel server)) return;
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return;
        ItemEntity item = new ItemEntity(server, golem.getX(), golem.getY() + golem.getBbHeight() / 2.0F, golem.getZ(), carried.copy());
        double distance = Math.sqrt(golem.distanceToSqr(place.getX() + 0.5, place.getY() + 0.5, place.getZ() + 0.5));
        item.setDeltaMovement((place.getX() + 0.5 - golem.getX()) * (distance / 3.0),
                0.1 + (place.getY() + 0.5 - (golem.getY() + golem.getBbHeight() / 2.0F)) * (distance / 3.0),
                (place.getZ() + 0.5 - golem.getZ()) * (distance / 3.0));
        item.setDefaultPickUpDelay();
        server.addFreshEntity(item);
        cargo.setCarried(ItemStack.EMPTY);
    }

    @Override public void stop() { count = 0; }

    @Override public void tick() { if (count > 0) --count; }

    /** Original {@code GolemHelper.getMarkedBlocksAdjacentToGolem}: the marked blocks next to the golem that are no containers. */
    private BlockPos dropPlace() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        ItemStack carried = cargo.carried();
        for (int color : golem.colorsMatching(carried)) {
            for (GolemMarker marker : golem.getMarkers()) {
                if (!marker.dimension().equals(server.dimension().location())) continue;
                if (!GolemMarker.accepts(color, marker.color())) continue;
                if (!golem.markerAccepts(marker, carried)) continue;
                if (marker.pos().equals(home)) continue;
                Container container = GolemAiRules.Containers.container(server, marker.pos());
                if (container != null) continue;
                if (!GolemAiRules.adjacent(golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5))) continue;
                return marker.pos();
            }
        }
        return null;
    }
}

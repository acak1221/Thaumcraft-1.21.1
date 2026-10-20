package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIFillTake} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIFillTake.java}).
 *
 * <p>The Fill golem takes the goods it still needs out of a marked container next to it. The marker colour has to be one of
 * the colours of the golem's slots that hold the wanted item, and the container behind the golem's own home is never used.
 * The take happens when the goal starts, and the container then stays open for five ticks, exactly as the original did.</p>
 *
 * <p>The original registered this goal with priority 3.</p>
 */
public final class GolemFillTake extends Goal {
    /** The original 1.7 goal priority of {@code AIFillTake}. */
    public static final int PRIORITY = 3;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int count;
    private int countChest;

    public GolemFillTake(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemFillTake(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried != null && !carried.isEmpty()) return false;
        if (!golem.getNavigation().isDone()) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos homeContainer = golem.getHomePosition().relative(golem.getHomeFacing().getOpposite());
        for (ItemStack wanted : wantedItems()) {
            for (GolemMarker marker : golem.getMarkers()) {
                if (!marker.dimension().equals(server.dimension().location())) continue;
                if (!golem.markerAccepts(marker, wanted)) continue;
                if (marker.pos().equals(homeContainer)) continue;
                Container container = GolemAiRules.Containers.container(server, marker.pos());
                if (container == null) continue;
                if (!GolemAiRules.adjacent(golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5))) continue;
                for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), marker.color())) {
                    if (GolemAiRules.Containers.extract(container, side, wanted, cargo.carrySpace(), false) != null) return true;
                }
            }
        }
        return false;
    }

    @Override public boolean canContinueToUse() { return count > 0 && (!golem.getNavigation().isDone() || countChest > 0); }

    @Override public void start() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        BlockPos homeContainer = golem.getHomePosition().relative(golem.getHomeFacing().getOpposite());
        for (ItemStack wanted : wantedItems()) {
            for (GolemMarker marker : golem.getMarkers()) {
                if (!marker.dimension().equals(server.dimension().location())) continue;
                if (!golem.markerAccepts(marker, wanted)) continue;
                if (marker.pos().equals(homeContainer)) continue;
                Container container = GolemAiRules.Containers.container(server, marker.pos());
                if (container == null) continue;
                for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), marker.color())) {
                    ItemStack taken = GolemAiRules.Containers.extract(container, side, wanted, cargo.carrySpace(), true);
                    if (taken == null) continue;
                    cargo.setCarried(taken);
                    count = GolemAiRules.pathBudget();
                    countChest = GolemAiRules.CHEST_INTERACT_TICKS;
                    return;
                }
            }
        }
    }

    @Override public void stop() { countChest = 0; }

    @Override public void tick() {
        if (count > 0) --count;
        if (countChest > 0) --countChest;
    }

    /** The goods the golem's slots still ask for. */
    private java.util.List<ItemStack> wantedItems() {
        java.util.List<ItemStack> wanted = new java.util.ArrayList<>();
        for (int slot = 0; slot < golem.getSlotCount(); slot++) {
            ItemStack filter = golem.getFilter(slot);
            if (filter.isEmpty()) continue;
            if (golem.getWantedCount(slot) <= 0) continue;
            wanted.add(filter.copyWithCount(1));
        }
        return wanted;
    }
}

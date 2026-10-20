package thaumcraft.common.entities.ai.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHomeReplace} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIHomeReplace.java}).
 *
 * <p>A golem that carries an item that nobody wants any more puts it back into the container behind its home, so that the
 * item does not stay in its hands for ever. An item that is still needed, and an item that still has a marked place to go
 * to, are never replaced.</p>
 *
 * <p>The original registered this goal with priority 0, above every other inventory goal.</p>
 */
public final class GolemHomeReplace extends Goal {
    /** The original 1.7 goal priority of {@code AIHomeReplace}. */
    public static final int PRIORITY = 0;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int countChest;

    public GolemHomeReplace(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemHomeReplace(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!golem.getNavigation().isDone()) return false;
        BlockPos home = golem.getHomePosition();
        if (golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                > GolemAiRules.HOME_RANGE_SQ) return false;
        if (hasDestination(carried)) return false;
        List<ItemStack> needed = neededItems();
        if (needed == null || needed.isEmpty()) return true;
        for (ItemStack stack : needed) {
            if (ItemStack.isSameItemSameComponents(stack, carried)) return false;
        }
        return true;
    }

    @Override public boolean canContinueToUse() { return canUse() || countChest > 0; }

    @Override public void start() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        Container container = homeContainer();
        if (container == null) return;
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return;
        Direction face = golem.getHomeFacing();
        ItemStack result = GolemAiRules.Containers.place(container, face, carried, true);
        if (!GolemAiRules.placementChanged(result, carried)) return;
        countChest = GolemAiRules.CHEST_INTERACT_TICKS;
        cargo.setCarried(result);
    }

    @Override public void stop() { countChest = 0; }

    @Override public void tick() { if (countChest > 0) --countChest; }

    /**
     * Original {@code GolemHelper.findSomethingEmptyCore}/{@code findSomethingUseCore}/{@code findSomethingSortCore}: an
     * Empty golem wants a marked container with room, a Use golem wants a marked block it may use, and a Sorting golem wants
     * a marked container that already holds a copy.
     */
    private boolean hasDestination(ItemStack carried) {
        if (!(golem.level() instanceof ServerLevel server)) return false;
        int core = golem.getGolemCore();
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            if (!golem.markerAccepts(marker, carried)) continue;
            Container container = GolemAiRules.Containers.container(server, marker.pos());
            if (core == GolemData.SORTING) {
                if (container == null) continue;
                for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), marker.color())) {
                    if (GolemAiRules.Containers.holds(container, side, carried)) return true;
                }
                continue;
            }
            if (core == GolemData.USE) {
                if (container != null) continue;
                return true;
            }
            if (container == null) {
                if (core == GolemData.EMPTY_CORE) return true;
                continue;
            }
            for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), marker.color())) {
                if (GolemAiRules.Containers.hasRoom(container, side, carried)) return true;
            }
        }
        return false;
    }

    /** Original {@code GolemHelper.getItemsNeeded}: the goods the golem's slots still ask for. */
    private List<ItemStack> neededItems() {
        List<ItemStack> needed = new ArrayList<>();
        int core = golem.getGolemCore();
        if (core == GolemData.SORTING) {
            Container home = homeContainer();
            if (home == null) return null;
            for (int slot = 0; slot < home.getContainerSize(); slot++) {
                ItemStack stack = home.getItem(slot);
                if (!stack.isEmpty()) needed.add(stack.copy());
            }
            return needed;
        }
        if (core == GolemData.EMPTY_CORE || core == GolemData.USE) {
            for (int slot = 0; slot < golem.getSlotCount(); slot++) {
                ItemStack filter = golem.getFilter(slot);
                if (filter.isEmpty()) continue;
                int wanted = golem.getWantedCount(slot);
                if (wanted <= 0) continue;
                needed.add(filter.copyWithCount(wanted));
            }
            return needed;
        }
        return null;
    }

    /** The container behind the golem's home. */
    private Container homeContainer() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        return GolemAiRules.Containers.container(server, home.relative(golem.getHomeFacing().getOpposite()));
    }
}

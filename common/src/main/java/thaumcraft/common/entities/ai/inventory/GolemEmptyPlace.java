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
 * Original {@code AIEmptyPlace} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIEmptyPlace.java}).
 *
 * <p>The golem puts the item it carries into a marked container next to it. The colour of the marker has to be one of the
 * colours of the golem's slots that match the carried item, and the container behind the golem's own home is never used.
 * The placement itself happens when the goal starts, and the goal then keeps the container open for five ticks, exactly as
 * the original did.</p>
 *
 * <p>The original registered this goal with priority 1.</p>
 */
public final class GolemEmptyPlace extends Goal {
    /** The original 1.7 goal priority of {@code AIEmptyPlace}. */
    public static final int PRIORITY = 1;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private BlockPos target;
    private int count;
    private int countChest;

    public GolemEmptyPlace(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemEmptyPlace(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (!golem.getNavigation().isDone()) return false;
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos homeContainer = golem.getHomePosition().relative(golem.getHomeFacing().getOpposite());
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location())) continue;
            if (marker.pos().equals(homeContainer)) continue;
            Container container = GolemAiRules.Containers.container(server, marker.pos());
            if (container == null) continue;
            if (!GolemAiRules.adjacent(golem.distanceToSqr(marker.pos().getX() + 0.5, marker.pos().getY() + 0.5, marker.pos().getZ() + 0.5))) continue;
            for (int color : golem.colorsMatching(carried)) {
                for (Direction side : GolemAiRules.Containers.markedSides(golem, marker.pos(), color)) {
                    ItemStack result = GolemAiRules.Containers.place(container, side, carried, false);
                    if (GolemAiRules.placementChanged(result, carried)) {
                        target = marker.pos();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override public boolean canContinueToUse() { return count > 0 && (canUse() || countChest > 0); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        if (target == null || !(golem.level() instanceof ServerLevel server)) return;
        Container container = GolemAiRules.Containers.container(server, target);
        if (container == null) return;
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return;
        for (int color : golem.colorsMatching(carried)) {
            for (Direction side : GolemAiRules.Containers.markedSides(golem, target, color)) {
                ItemStack result = GolemAiRules.Containers.place(container, side, carried, true);
                countChest = GolemAiRules.CHEST_INTERACT_TICKS;
                cargo.setCarried(result);
                if (!result.isEmpty()) continue;
                break;
            }
            if (countChest == GolemAiRules.CHEST_INTERACT_TICKS) break;
        }
    }

    @Override public void stop() { countChest = 0; }

    @Override public void tick() {
        if (countChest > 0) --countChest;
        if (count > 0) --count;
    }
}

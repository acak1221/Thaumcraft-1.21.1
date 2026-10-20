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
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHomeTakeSorting} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIHomeTakeSorting.java}).
 *
 * <p>The Sorting golem takes one thing out of the container behind its home, whatever it is: it is going to put that thing
 * into the container that already holds a copy of it. The golem has to stand within five blocks of the home.</p>
 *
 * <p>The original registered this goal with priority 4.</p>
 */
public final class GolemHomeTakeSorting extends Goal {
    /** The original 1.7 goal priority of {@code AIHomeTakeSorting}. */
    public static final int PRIORITY = 4;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int count;
    private int countChest;

    public GolemHomeTakeSorting(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemHomeTakeSorting(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried != null && !carried.isEmpty()) return false;
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        if (!golem.getNavigation().isDone()) return false;
        BlockPos home = golem.getHomePosition();
        if (golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                > GolemAiRules.HOME_RANGE_SQ) return false;
        Container container = homeContainer();
        if (container == null) return false;
        Direction face = golem.getHomeFacing();
        for (ItemStack needed : neededItems(container)) {
            if (GolemAiRules.Containers.extract(container, face, needed, cargo.carrySpace(), false) != null) return true;
        }
        return false;
    }

    @Override public boolean canContinueToUse() { return count > 0 && (canUse() || countChest > 0); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        if (!(golem.level() instanceof ServerLevel server)) return;
        Container container = homeContainer();
        if (container == null) return;
        Direction face = golem.getHomeFacing();
        for (ItemStack needed : neededItems(container)) {
            ItemStack taken = GolemAiRules.Containers.extract(container, face, needed, cargo.carrySpace(), true);
            if (taken == null) continue;
            cargo.setCarried(taken);
            countChest = GolemAiRules.CHEST_INTERACT_TICKS;
            return;
        }
    }

    @Override public void stop() { countChest = 0; }

    @Override public void tick() {
        if (countChest > 0) --countChest;
        if (count > 0) --count;
    }

    /** Original {@code GolemHelper.getItemsInHomeContainer}: everything the home container holds. */
    private List<ItemStack> neededItems(Container container) {
        List<ItemStack> needed = new ArrayList<>();
        if (golem.getGolemCore() != GolemData.SORTING) return needed;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) needed.add(stack.copy());
        }
        return needed;
    }

    /** The container behind the golem's home. */
    private Container homeContainer() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        return GolemAiRules.Containers.container(server, home.relative(golem.getHomeFacing().getOpposite()));
    }
}

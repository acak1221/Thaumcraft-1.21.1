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
 * Original {@code AIHomeTake} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIHomeTake.java}).
 *
 * <p>The golem takes a thing it still needs out of the container behind its own home: the item has to be one of the goods
 * its slots ask for, and the golem has to stand within five blocks of the home. The take happens when the goal starts, and
 * the container then stays open for five ticks, exactly as the original did.</p>
 *
 * <p>The original registered this goal with priority 4.</p>
 */
public final class GolemHomeTake extends Goal {
    /** The original 1.7 goal priority of {@code AIHomeTake}. */
    public static final int PRIORITY = 4;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int countChest;

    public GolemHomeTake(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemHomeTake(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
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
        for (ItemStack needed : neededItems()) {
            if (GolemAiRules.Containers.extract(container, face, needed, cargo.carrySpace(), false) != null) return true;
        }
        return false;
    }

    @Override public boolean canContinueToUse() { return countChest > 0; }

    @Override public void start() {
        if (!(golem.level() instanceof ServerLevel server)) return;
        Container container = homeContainer();
        if (container == null) return;
        Direction face = golem.getHomeFacing();
        for (ItemStack needed : neededItems()) {
            ItemStack taken = GolemAiRules.Containers.extract(container, face, needed, cargo.carrySpace(), true);
            if (taken == null) continue;
            cargo.setCarried(taken);
            countChest = GolemAiRules.CHEST_INTERACT_TICKS;
            return;
        }
    }

    @Override public void stop() { countChest = 0; }

    @Override public void tick() { if (countChest > 0) --countChest; }

    /** Original {@code GolemHelper.getItemsNeeded}: the goods the golem's slots still ask for. */
    private List<ItemStack> neededItems() {
        List<ItemStack> needed = new ArrayList<>();
        int core = golem.getGolemCore();
        if (core == GolemData.SORTING) {
            Container home = homeContainer();
            if (home == null) return needed;
            for (int slot = 0; slot < home.getContainerSize(); slot++) {
                ItemStack stack = home.getItem(slot);
                if (!stack.isEmpty()) needed.add(stack.copy());
            }
            return needed;
        }
        for (int slot = 0; slot < golem.getSlotCount(); slot++) {
            ItemStack filter = golem.getFilter(slot);
            if (filter.isEmpty()) continue;
            int wanted = golem.getWantedCount(slot);
            if (wanted <= 0) continue;
            needed.add(filter.copyWithCount(wanted));
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

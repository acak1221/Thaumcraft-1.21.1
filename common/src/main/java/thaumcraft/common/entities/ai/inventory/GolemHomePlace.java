package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHomePlace} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIHomePlace.java}).
 *
 * <p>The golem puts the item it carries into the container behind its own home, through the home's own face. The golem has
 * to stand within five blocks of the home, and the placement happens when the goal starts; the container then stays open for
 * five ticks, exactly as the original did.</p>
 *
 * <p>The original registered this goal with priority 1.</p>
 */
public final class GolemHomePlace extends Goal {
    /** The original 1.7 goal priority of {@code AIHomePlace}. */
    public static final int PRIORITY = 1;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int countChest;

    public GolemHomePlace(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemHomePlace(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
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
        Container container = homeContainer();
        if (container == null) return false;
        Direction face = golem.getHomeFacing();
        ItemStack result = GolemAiRules.Containers.place(container, face, carried, false);
        return GolemAiRules.placementChanged(result, carried);
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

    /** The container behind the golem's home. */
    private Container homeContainer() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        return GolemAiRules.Containers.container(server, home.relative(golem.getHomeFacing().getOpposite()));
    }
}

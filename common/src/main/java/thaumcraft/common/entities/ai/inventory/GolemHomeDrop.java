package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIHomeDrop} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIHomeDrop.java}).
 *
 * <p>A golem that carries an item but has no container behind its home drops the item at its home, so that the item is not
 * lost when the home has nowhere to put it. The golem has to stand within five blocks of the home.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemHomeDrop extends Goal {
    /** The original 1.7 goal priority of {@code AIHomeDrop}. */
    public static final int PRIORITY = 2;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private int count;

    public GolemHomeDrop(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemHomeDrop(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return false;
        if (!golem.getNavigation().isDone()) return false;
        BlockPos home = golem.getHomePosition();
        if (golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                > GolemAiRules.HOME_RANGE_SQ) return false;
        return homeContainer() == null;
    }

    @Override public boolean canContinueToUse() { return count > 0 && (canUse() || count > 0); }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        if (!(golem.level() instanceof ServerLevel server)) return;
        ItemStack carried = cargo.carried();
        if (carried == null || carried.isEmpty()) return;
        BlockPos home = golem.getHomePosition();
        ItemEntity item = new ItemEntity(server, golem.getX(), golem.getY() + golem.getBbHeight() / 2.0F, golem.getZ(), carried.copy());
        double distance = Math.sqrt(golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5));
        item.setDeltaMovement((home.getX() + 0.5 - golem.getX()) * (distance / 3.0),
                0.1 + (home.getY() + 0.5 - (golem.getY() + golem.getBbHeight() / 2.0F)) * (distance / 3.0),
                (home.getZ() + 0.5 - golem.getZ()) * (distance / 3.0));
        item.setDefaultPickUpDelay();
        server.addFreshEntity(item);
        cargo.setCarried(ItemStack.EMPTY);
    }

    @Override public void stop() { count = 0; }

    @Override public void tick() { if (count > 0) --count; }

    /** The container behind the golem's home, when there is one. */
    private Container homeContainer() {
        if (!(golem.level() instanceof ServerLevel server)) return null;
        BlockPos home = golem.getHomePosition();
        return GolemAiRules.Containers.container(server, home.relative(golem.getHomeFacing().getOpposite()));
    }
}

package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityPech;

/**
 * Original {@code AIPechTradePlayer} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/pech/AIPechTradePlayer.java}).
 *
 * <p>A pech that is trading stands still: the goal starts when the pech is alive, out of the water, not tamed, on the
 * ground and not riding, and it gives up the moment the trade ends.</p>
 *
 * <p>The original registered this goal with priority 5.</p>
 */
public final class PechTradePlayer extends Goal {
    /** The original 1.7 goal priority of {@code AIPechTradePlayer}. */
    public static final int PRIORITY = 5;

    private final EntityPech villager;

    public PechTradePlayer(EntityPech villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (!villager.isAlive()) return false;
        if (villager.isInWater()) return false;
        if (villager.isTamed()) return false;
        if (!villager.onGround()) return false;
        if (villager.isPassenger()) return false;
        return villager.trading;
    }

    @Override public void start() { villager.getNavigation().stop(); }

    @Override public void stop() { villager.trading = false; }
}

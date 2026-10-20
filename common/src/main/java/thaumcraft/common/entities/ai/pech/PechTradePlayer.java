package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityPech;

/**
 * Original {@code AIPechTradePlayer} (TC4 4.2.3.5 oracle,
 * {@code thaumcraft/common/entities/ai/pech/AIPechTradePlayer.java}).
 *
 * <p>A trading pech stands still: the goal starts while the pech is alive, out of the water, TAMED, on the ground
 * and not riding, and it gives the trade up the moment the trade ends. The original pech opens its trade window
 * only when it is tamed (oracle {@code EntityPech.func_70085_c}), so the goal runs exactly while a customer trades.
 * The native port adds the 1.21.1 "hurt this tick" guard of the inline goal it replaces.</p>
 *
 * <p>The original class sets its own priority field to five ({@code func_75248_a(5)}), but the original registers
 * the goal with the selector at ONE ({@code EntityPech.java:147}); the 1.7 selector orders by the registration
 * number, exactly like the native one, so the effective original priority is ONE and that is the number this goal
 * is installed with.</p>
 */
public final class PechTradePlayer extends Goal {
    /** The original 1.7 selector priority of {@code AIPechTradePlayer}. */
    public static final int PRIORITY = 1;

    private final EntityPech villager;

    public PechTradePlayer(EntityPech villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override public boolean canUse() {
        if (!villager.isAlive()) return false;
        if (villager.isInWater()) return false;
        if (!villager.isTamed()) return false; // The original: a trading pech is a tamed one.
        if (!villager.onGround()) return false;
        if (villager.isPassenger()) return false;
        if (villager.hurtMarked) return false; // Native parity: a pech that was just hurt does not hold a trade.
        return villager.trading;
    }

    @Override public void start() { villager.getNavigation().stop(); }

    @Override public void stop() { villager.trading = false; }
}

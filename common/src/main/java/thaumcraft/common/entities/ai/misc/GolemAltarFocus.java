package thaumcraft.common.entities.ai.misc;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIAltarFocus} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/misc/AIAltarFocus.java}).
 *
 * <p>A ritualist that focuses on an altar keeps the ritual only while the altar is really there: the ritual ends when the
 * altar is more than sixteen blocks away or when the block at the altar is no longer the altar at all. The check happens
 * every fortieth tick.</p>
 *
 * <p>The cultist cleric that owned this goal in TC4 has no native counterpart in this port, so the goal is written against
 * the smallest possible contract: the ritualist flag, the altar position and the test of the altar block.</p>
 */
public final class GolemAltarFocus extends Goal {
    /** The original 1.7 goal priority of {@code AIAltarFocus}. */
    public static final int PRIORITY = 1;

    /** The ritualist flag of the entity that owns the goal. */
    public interface Ritualist {
        boolean isRitualist();
        void setRitualist(boolean value);
    }

    private final Mob entity;
    private final Ritualist ritualist;
    private final Supplier<BlockPos> altar;
    private final BooleanSupplier altarPresent;

    public GolemAltarFocus(Mob entity, Ritualist ritualist, Supplier<BlockPos> altar, BooleanSupplier altarPresent) {
        this.entity = entity;
        this.ritualist = ritualist;
        this.altar = altar;
        this.altarPresent = altarPresent;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() { return ritualist.isRitualist() && altar.get() != null; }

    @Override public boolean canContinueToUse() { return ritualist.isRitualist() && altar.get() != null; }

    @Override public void tick() {
        if (!ritualist.isRitualist()) return;
        BlockPos pos = altar.get();
        if (pos == null) return;
        if (entity.tickCount % GolemAiRules.ALTAR_FOCUS_INTERVAL != 0) return;
        if (entity.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > GolemAiRules.ALTAR_FOCUS_DISTANCE_SQ
                || !altarPresent.getAsBoolean()) {
            ritualist.setRitualist(false);
        }
    }
}

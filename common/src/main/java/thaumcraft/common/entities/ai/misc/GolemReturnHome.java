package thaumcraft.common.entities.ai.misc;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIReturnHome} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/misc/AIReturnHome.java}).
 *
 * <p>The golem walks back to its home when it strayed more than three blocks away. Every failed path adds five ticks of
 * delay, up to fifty, so a golem that cannot reach its home does not try again and again; when the golem stands still for
 * twenty ticks it takes a small detour first.</p>
 *
 * <p>The original registered this goal with priority 6, below every other golem goal.</p>
 */
public final class GolemReturnHome extends Goal {
    /** The original 1.7 goal priority of {@code AIReturnHome}. */
    public static final int PRIORITY = 6;

    private final EntityGolemBase golem;
    private double moveX, moveY, moveZ;
    private int pathingDelay;
    private int pathingDelayIncrement = GolemAiRules.RETURN_HOME_DELAY_INCREMENT;
    private int count;
    private int previousX, previousY, previousZ;

    public GolemReturnHome(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (pathingDelay > 0) --pathingDelay;
        if (pathingDelay > 0) return false;
        if (GolemAiRules.homeReached(homeDistanceSq())) return false;
        BlockPos home = golem.getHomePosition();
        moveX = home.getX() + 0.5;
        moveY = home.getY() + 0.5;
        moveZ = home.getZ() + 0.5;
        return true;
    }

    @Override public boolean canContinueToUse() {
        return pathingDelay <= 0 && count > 0 && !golem.getNavigation().isDone()
                && !GolemAiRules.homeReached(homeDistanceSq());
    }

    @Override public void start() {
        count = GolemAiRules.detourBudget();
        BlockPos here = golem.blockPosition();
        previousX = here.getX();
        previousY = here.getY();
        previousZ = here.getZ();
        pathResult(golem.getNavigation().moveTo(moveX, moveY, moveZ, golem.getAttributeValue(Attributes.MOVEMENT_SPEED)));
    }

    @Override public void stop() { golem.getNavigation().stop(); }

    @Override public void tick() {
        if (--count != 0) return;
        BlockPos here = golem.blockPosition();
        if (previousX != here.getX() || previousY != here.getY() || previousZ != here.getZ()) return;
        Vec3 detour = DefaultRandomPos.getPos(golem, 2, 1);
        if (detour == null) {
            pathResult(false);
            return;
        }
        count = GolemAiRules.detourBudget();
        pathResult(golem.getNavigation().moveTo(detour.x, detour.y, detour.z,
                golem.getAttributeValue(Attributes.MOVEMENT_SPEED)));
    }

    /** The squared distance to the golem's home; the native golem keeps its own copy private. */
    private double homeDistanceSq() {
        BlockPos home = golem.getHomePosition();
        return golem.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
    }

    /** Original: a failed path raises the delay by five ticks, up to fifty. */
    private void pathResult(boolean success) {
        if (success) {
            pathingDelayIncrement = GolemAiRules.RETURN_HOME_DELAY_INCREMENT;
            return;
        }
        pathingDelay = GolemAiRules.returnHomeDelay(pathingDelayIncrement);
        pathingDelayIncrement = GolemAiRules.returnHomeDelayIncrement(pathingDelayIncrement);
    }
}

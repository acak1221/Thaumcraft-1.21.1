package thaumcraft.common.entities.ai.misc;

import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIWander} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/misc/AIWander.java}).
 *
 * <p>The mob wanders off: it looks for a spot ten blocks wide and seven high around itself, but only when it has been idle
 * for less than a hundred ticks and only once in a hundred and twenty tries. The original also had a way to force the next
 * wander, which this port keeps as {@link #setWander()}.</p>
 *
 * <p>Documented adaptation: 1.7 counted the idle ticks inside the entity, and 1.21.1 has no such counter, so the goal
 * keeps the count itself: a mob that did not move since the last update is idle.</p>
 */
public final class GolemWander extends Goal {
    private final PathfinderMob entity;
    private final double speed;
    private double xPosition, yPosition, zPosition;
    private boolean forced;
    private int idleTicks;
    private double lastX, lastY, lastZ;

    public GolemWander(PathfinderMob entity, double speed) {
        this.entity = entity;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    /** The original {@code setWander}: the next check ignores the idle limit and the roll. */
    public void setWander() { forced = true; }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!forced) {
            if (!GolemAiRules.mayWander(idleTicks)) return false;
            if (!GolemAiRules.wanderRoll(entity.getRandom().nextInt(GolemAiRules.WANDER_CHANCE))) return false;
        }
        Vec3 pos = DefaultRandomPos.getPos(entity, GolemAiRules.WANDER_HORIZONTAL, GolemAiRules.WANDER_VERTICAL);
        if (pos == null) return false;
        xPosition = pos.x;
        yPosition = pos.y;
        zPosition = pos.z;
        forced = false;
        return true;
    }

    @Override public boolean canContinueToUse() { return !entity.getNavigation().isDone(); }

    @Override public void start() {
        entity.getNavigation().moveTo(xPosition, yPosition, zPosition, speed);
    }

    @Override public void tick() {
        if (entity.getX() == lastX && entity.getY() == lastY && entity.getZ() == lastZ) ++idleTicks;
        else idleTicks = 0;
        lastX = entity.getX();
        lastY = entity.getY();
        lastZ = entity.getZ();
    }

    /** The ticks the mob has stood still. */
    public int idleTicks() { return idleTicks; }
}

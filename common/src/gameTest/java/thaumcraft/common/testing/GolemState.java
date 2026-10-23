package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.golems.EntityGolemBase;

/**
 * The state of a golem that a GameTest prints when it fails.
 *
 * <p>A golem that does not work is not always the same defect: it can be gone (a
 * neighbouring scenario of the same batch cleaned the area), it can be unable to
 * work (its configuration, its home or a fetter holds it), it can be unable to
 * plan a route (the chunks around it are not all loaded) or it can be a golem
 * that simply never ticked. The failure message of a scenario names the state,
 * so the reason of a red scenario is readable in the log of the run instead of
 * only in a debugger.</p>
 */
final class GolemState {
    private GolemState() {}

    /** The state of one golem, in a form that fits into a failure message. */
    static String of(EntityGolemBase golem) {
        if (golem == null) return "no golem";
        boolean area = golem.loadedNavigationArea(golem.blockPosition());
        return "removed=" + golem.isRemoved()
                + " alive=" + golem.isAlive()
                + " area=" + area
                + " work=" + golem.canWork()
                + " fettered=" + golem.isFettered()
                + " noAi=" + golem.isNoAi()
                + " nav=" + (golem.getNavigation() == null ? "none" : String.valueOf(golem.getNavigation().isDone()))
                + " ticks=" + golem.tickCount + " " + golem.describeWorkState();
    }

    /** The entities that stand around a fixture, for the log of a scenario that starts. */
    static void around(ServerLevel level, BlockPos center, int radius) {
        var box = new AABB(center).inflate(radius);
        var found = level.getEntities((Entity) null, box, entity -> true);
        Thaumcraft.LOG.info("Entities around {} within {} blocks: {}", center.toShortString(), radius,
                found.stream().map(entity -> entity.getType().getDescription() + "@" + entity.blockPosition().toShortString()
                        + (entity.getName() != null ? "(" + entity.getName() + ")" : "")
                        + (entity instanceof EntityGolemBase golem ? "[" + of(golem) + "]" : "")).toList());
    }
}

package thaumcraft.common.entities.golems;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.items.golems.GolemData;

/** Shared ordinary-core safety/return/idle goals and Gather's physical-item goal. */
final class GatherGolemGoals {
    private final EntityGolemBase golem;
    private ItemEntity target;
    private int returnDelay;
    private int returnDelayIncrement = 5;

    GatherGolemGoals(EntityGolemBase golem) { this.golem = golem; }

    /**
     * The creeper flight of the original, registered FIRST (oracle EntityGolemBase:267-268).
     *
     * <p>The original registered it with the number zero, and the 1.7 goal selector walks its list in the order of the
     * registration, so the golem flees a swelling creeper before every other goal of the golem.</p>
     */
    void installCreeperFlight() {
        golem.addWorkGoal(GoalPriority.CREEPER_FLIGHT, new AvoidSwell());
    }

    /**
     * The pickup goal of the Gather golem (oracle EntityGolemBase:289-290, the number two).
     *
     * <p>It belongs to the goals of the core, so it is registered with them and before the shared goals.</p>
     */
    void installGather() {
        if (golem.getGolemCore() == GolemData.GATHER) golem.addWorkGoal(GoalPriority.ITEM_PICKUP, new Pickup());
    }

    /**
     * The shared goals of every core, registered AFTER the goals of the core (oracle EntityGolemBase:346-351).
     *
     * <p>The 1.21 selector walks its goals in the order of the registration, so the shared goals come last, exactly like
     * the original, where the goals of the core were registered first and the door, the return home, the watch and the
     * idle look after them.</p>
     *
     * <p>There is NO swimming goal: the original cleared its whole goal list ({@code tasks.taskEntries.clear()}, oracle
     * EntityGolemBase:265) and registered only its own goals, and 1.7.10 registered the swimming goal in the individual
     * mobs (the wolf, the horse, the ocelot, the sheep), never in the base classes of a living entity. A native floating
     * goal would take the MOVE flag of the number one goals of the Decanting and the Alchemy golem, so the port has
     * none.</p>
     */
    void installShared() {
        golem.addWorkGoal(GoalPriority.OPEN_DOOR, new OpenDoorGoal(golem, true) {
            @Override public boolean canUse() { return golem.canWork() && super.canUse(); }
            @Override public boolean canContinueToUse() { return golem.canWork() && super.canContinueToUse(); }
        });
        golem.addWorkGoal(GoalPriority.RETURN_HOME, new ReturnHome());
        golem.addWorkGoal(GoalPriority.WATCH_CLOSEST, new LookAtPlayerGoal(golem, Player.class, 6) {
            @Override public boolean canUse() { return golem.canWork() && super.canUse(); }
            @Override public boolean canContinueToUse() { return golem.canWork() && super.canContinueToUse(); }
        });
        golem.addWorkGoal(GoalPriority.LOOK_IDLE, new RandomLookAroundGoal(golem) {
            @Override public boolean canUse() { return golem.canWork() && super.canUse(); }
            @Override public boolean canContinueToUse() { return golem.canWork() && super.canContinueToUse(); }
        });
    }

    void reset() {
        target = null;
        returnDelay = 0;
        returnDelayIncrement = 5;
    }

    /** Actual five-server-tick cadence, independent of native goal-start throttling. */
    void serverTick() {
        if (returnDelay > 0) --returnDelay;
        if (!golem.canWork()) { target = null; return; }
        if (golem.getGolemCore() != GolemData.GATHER) return;
        if (golem.tickCount % 5 != 0) return;
        if (golem.hasCargo() && golem.getNavigation().isDone() && golem.distanceToHomeSqr() <= 5) {
            golem.placeCarriedAtHome();
        }
        if (!golem.canWork() || golem.carrySpace() <= 0) { target = null; return; }
        if (target != null && golem.eligibleItem(target)) return;
        target = null;
        double nearest = Double.MAX_VALUE;
        AABB search = new AABB(golem.getHomePosition()).inflate(golem.getRange());
        for (ItemEntity item : golem.level().getEntitiesOfClass(ItemEntity.class, search, golem::eligibleItem)) {
            double distance = golem.distanceToSqr(item);
            if (distance < nearest) { nearest = distance; target = item; }
        }
    }

    private final class Pickup extends Goal {
        private ItemEntity pursued;
        private int budget;

        Pickup() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() { return golem.canWork() && target != null && golem.eligibleItem(target); }
        @Override public boolean canContinueToUse() {
            return golem.canWork() && budget > 0 && pursued != null && golem.eligibleItem(pursued)
                    && (!golem.getNavigation().isDone() || golem.distanceToSqr(pursued) <= 2);
        }
        @Override public void start() {
            pursued = target;
            budget = 200;
            if (pursued == null || !golem.loadedNavigationArea(pursued.blockPosition())
                    || !golem.getNavigation().moveTo(pursued, 1)) budget = 0;
        }
        @Override public void tick() {
            --budget;
            if (pursued == null) return;
            golem.getLookControl().setLookAt(pursued, 30, 30);
            if (golem.distanceToSqr(pursued) <= 2 && golem.gatherItem(pursued)) {
                budget = 0;
                target = null;
                golem.getNavigation().stop();
            }
        }
        @Override public void stop() {
            if (target == pursued) target = null;
            pursued = null;
            budget = 0;
            golem.getNavigation().stop();
        }
    }

    private final class ReturnHome extends Goal {
        private int budget;
        private BlockPos previous;

        ReturnHome() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            return golem.canWork() && !golem.hasInventoryRoute() && returnDelay == 0 && golem.distanceToHomeSqr() >= 3;
        }
        @Override public boolean canContinueToUse() {
            return golem.canWork() && !golem.hasInventoryRoute() && returnDelay == 0 && budget > 0
                    && !golem.getNavigation().isDone() && golem.distanceToHomeSqr() >= 3;
        }
        @Override public void start() {
            budget = 20;
            previous = golem.blockPosition();
            BlockPos home = golem.getHomePosition();
            pathResult(golem.loadedNavigationArea(home)
                    && golem.getNavigation().moveTo(home.getX() + .5, home.getY() + .5, home.getZ() + .5, 1));
        }
        private void pathResult(boolean success) {
            if (success) returnDelayIncrement = 5;
            else {
                returnDelay = returnDelayIncrement;
                returnDelayIncrement = Math.min(50, returnDelayIncrement + 5);
            }
        }
        @Override public void tick() {
            if (--budget != 0 || !golem.blockPosition().equals(previous)) return;
            if (!golem.loadedNavigationArea(golem.blockPosition())) { pathResult(false); return; }
            Vec3 detour = DefaultRandomPos.getPos(golem, 2, 1);
            if (detour == null) { pathResult(false); return; }
            budget = 20;
            pathResult(golem.getNavigation().moveTo(detour.x, detour.y, detour.z, 1));
        }
        @Override public void stop() { golem.getNavigation().stop(); }
    }

    private final class AvoidSwell extends Goal {
        private Creeper threat;
        private Vec3 escape;
        private Path path;

        AvoidSwell() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canUse() {
            if (!golem.canWork() || !golem.loadedNavigationArea(golem.blockPosition())) return false;
            threat = null;
            for (Creeper creeper : golem.level().getEntitiesOfClass(Creeper.class, golem.getBoundingBox().inflate(5, 3, 5))) {
                if (creeper.isAlive() && creeper.getSwellDir() == 1 && golem.getSensing().hasLineOfSight(creeper)) {
                    threat = creeper;
                    break;
                }
            }
            if (threat == null) return false;
            escape = DefaultRandomPos.getPosAway(golem, 16, 7, threat.position());
            if (escape == null || threat.distanceToSqr(escape) < golem.distanceToSqr(threat)) return false;
            path = golem.getNavigation().createPath(escape.x, escape.y, escape.z, 0);
            return path != null && path.canReach();
        }
        @Override public boolean canContinueToUse() { return golem.canWork() && threat != null && threat.isAlive() && !golem.getNavigation().isDone(); }
        @Override public void start() {
            Vec3 direction = escape.subtract(golem.position()).multiply(1, 0, 1).normalize();
            Vec3 old = golem.getDeltaMovement();
            golem.setDeltaMovement(old.x * 1.2 + direction.x * .8, .3, old.z * 1.2 + direction.z * .8);
            golem.getNavigation().moveTo(path, 1.25);
        }
        @Override public void tick() { golem.getNavigation().setSpeedModifier(golem.distanceToSqr(threat) < 49 ? 1.25 : 1.125); }
        @Override public void stop() { threat = null; }
    }
}

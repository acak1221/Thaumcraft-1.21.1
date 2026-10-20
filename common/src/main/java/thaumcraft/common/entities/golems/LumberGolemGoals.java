package thaumcraft.common.entities.golems;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.lib.world.TreeLogTraversal;

/**
 * Lumber core (original AIHarvestLogs): one private anchor log, no cargo, markers or
 * inventory. A random scan finds a log in the home box, descends its column while the
 * next lower log is closer to the golem, walks to it, works for a time set by the
 * ANCHOR's hardness and the golem's strength, then removes ONE far endpoint of that tree
 * through the guarded platform (never the whole tree at once). While the anchor survives
 * it works again; when it is gone the first adjacent log inside the home box is next.
 * Leaves are never chopped; native decay does that. No saplings are planted (original).
 */
final class LumberGolemGoals extends Goal {
    private final EntityGolemBase golem;
    private BlockPos scanHome;
    private BlockPos target;
    private BlockState expected;
    private int delay = -1;
    private int maxDelay = 1;
    private int pulseMod = 1;
    private int budget;
    private boolean running;
    private boolean attempting;

    LumberGolemGoals(EntityGolemBase golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Original AIHarvestLogs:60 {@code ceil(range / 3)}, read live so a Water upgrade needs no new goal. */
    private int radius() { return LumberGeometry.radius(golem.getRange()); }

    void install() { golem.addWorkGoal(GoalPriority.HARVEST_LOGS, this); }
    boolean hasTarget() { return target != null || running; }
    void reset() {
        try { stop(); }
        finally { scanHome = null; }
    }

    /** Actual server-tick admission, independent of native new-goal throttling. */
    void serverTick() {
        if (!(golem.level() instanceof ServerLevel server) || !working()) { reset(); return; }
        BlockPos home = golem.getHomePosition();
        if (scanHome == null) scanHome = home.immutable();
        else if (!scanHome.equals(home)) { reset(); scanHome = home.immutable(); }
        if (target != null && !currentTarget(server)) clearTarget();
        // Original canUse(): idle work state, every golemDelay (5) ticks, navigation idle.
        if (running || target != null || delay >= 0 || golem.tickCount % 5 != 0 || !golem.getNavigation().isDone()) return;
        int radius = radius();
        int attempts = LumberGeometry.attempts(radius);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int[] cell = LumberGeometry.candidate(home.getX(), home.getY(), home.getZ(), radius, golem.getRandom()::nextInt);
            BlockPos pos = new BlockPos(cell[0], cell[1], cell[2]);
            if (!isLog(server, pos)) continue;
            BlockPos anchor = descend(server, pos);
            select(anchor, server.getBlockState(anchor));
            return;
        }
    }

    private boolean working() { return golem.getGolemCore() == GolemData.LUMBER && golem.canWork(); }
    private static boolean loaded(ServerLevel server, BlockPos pos) {
        return !server.isOutsideBuildHeight(pos) && server.getWorldBorder().isWithinBounds(pos) && server.hasChunkAt(pos);
    }
    private static boolean isLog(ServerLevel server, BlockPos pos) {
        return loaded(server, pos) && TreeLogTraversal.isLog(server, pos, server.getBlockState(pos));
    }

    /** Go down the column only while the next lower log is strictly closer to the golem. */
    private BlockPos descend(ServerLevel server, BlockPos start) {
        BlockPos best = start;
        double nearest = golem.distanceToSqr(start.getX() + .5, start.getY() + .5, start.getZ() + .5);
        for (int step = 1; ; step++) {
            BlockPos below = start.below(step);
            if (!isLog(server, below)) break;
            double distance = golem.distanceToSqr(below.getX() + .5, below.getY() + .5, below.getZ() + .5);
            if (!(distance < nearest)) break;
            best = below;
            nearest = distance;
        }
        return best;
    }

    private void select(BlockPos pos, BlockState state) {
        target = pos.immutable();
        expected = state;
        delay = -1;
    }
    private boolean currentTarget(ServerLevel server) {
        return working() && target != null && expected != null && loaded(server, target)
                && server.getBlockState(target) == expected && TreeLogTraversal.isLog(server, target, expected);
    }
    boolean ownsAttempt(BlockPos pos, BlockState state) {
        return running && attempting && delay == 0 && target != null && target.equals(pos) && expected == state;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        return !running && golem.level() instanceof ServerLevel server && currentTarget(server);
    }
    @Override public boolean canContinueToUse() {
        return running && budget > 0 && golem.level() instanceof ServerLevel server && currentTarget(server);
    }
    @Override public void start() { running = true; startPath(); }
    private void startPath() {
        budget = 200;
        if (!(golem.level() instanceof ServerLevel server) || !currentTarget(server) || !golem.loadedNavigationArea(target)) {
            clearTarget();
            return;
        }
        golem.getNavigation().moveTo(target.getX() + .5, target.getY() + .5, target.getZ() + .5, 1);
    }

    @Override public void tick() {
        if (!running || !(golem.level() instanceof ServerLevel server)) return;
        if (budget <= 0 || !currentTarget(server)) { clearTarget(); return; }
        --budget;
        golem.getLookControl().setLookAt(target.getX() + .5, target.getY() + .5, target.getZ() + .5, 30, 30);
        if (golem.distanceToSqr(target.getX() + .5, target.getY() + .5, target.getZ() + .5) > 4) return;
        if (delay < 0) {
            maxDelay = delay = LumberGeometry.delay(golem.getGolemStrength(), expected.getDestroySpeed(server, target));
            pulseMod = LumberGeometry.pulseMod(delay);
        }
        if (delay <= 0) return;
        --delay; // Only work time in reach; travel still consumes the pursuit budget.
        if (delay > 0 && delay % pulseMod == 0 && golem.getNavigation().isDone()) {
            golem.startActionTimer();
            SoundType sound = expected.getSoundType();
            server.playSound(null, target.getX() + .5, target.getY() + .5, target.getZ() + .5,
                    sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + .7F) / 8.0F, sound.getPitch() * .5F);
            server.destroyBlockProgress(golem.getId(), target, (int) (9.0F * (1.0F - (float) delay / maxDelay)));
        }
        if (delay != 0) return;
        BlockPos anchor = target;
        boolean chopped;
        try {
            attempting = true;
            chopped = golem.chopLog(anchor, expected); // ONE guarded native endpoint removal.
        } finally {
            attempting = false;
        }
        // Refusal, stale state or an exception retires this work; it is never retried at delay 0.
        if (!chopped || golem.level() != server || !working()) { clearTarget(); return; }
        golem.startActionTimer();
        server.destroyBlockProgress(golem.getId(), anchor, -1);
        if (isLog(server, anchor)) {
            // The anchor survived (a remote endpoint was removed): work it again.
            expected = server.getBlockState(anchor);
            delay = -1;
            startPath();
        } else {
            nextAdjacent(server, anchor);
        }
    }

    /** Original checkAdjacent order: X, then Z, then Y over -1..1, home box on every axis. */
    private void nextAdjacent(ServerLevel server, BlockPos old) {
        BlockPos home = golem.getHomePosition();
        int radius = radius();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) {
            BlockPos next = old.offset(dx, dy, dz);
            if (!LumberGeometry.withinHomeBox(home.getX(), home.getY(), home.getZ(), radius, next.getX(), next.getY(), next.getZ())
                    || !isLog(server, next)) continue;
            target = next.immutable();
            expected = server.getBlockState(next);
            delay = -1;
            startPath();
            return;
        }
        clearTarget();
    }

    private void clearTarget() {
        BlockPos old = target;
        target = null;
        expected = null;
        delay = -1;
        maxDelay = pulseMod = 1;
        budget = 0;
        attempting = false;
        try {
            if (old != null && golem.level() instanceof ServerLevel server) server.destroyBlockProgress(golem.getId(), old, -1);
        } finally {
            if (running) golem.getNavigation().stop();
        }
    }
    @Override public void stop() {
        try { clearTarget(); }
        finally { running = false; }
    }
}

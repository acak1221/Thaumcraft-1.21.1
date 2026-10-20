package thaumcraft.common.entities.golems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.golems.GolemLiquidQueue;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemWaterCargo;
import thaumcraft.common.tiles.TileCrucible;

/**
 * Ordinary marked source water -> separate mB stock -> attached crucible only. With an Entropy upgrade the golem drains, one bucket per
 * pulse, the queue {@link GolemLiquidQueue} builds from the marked block instead of the marked block itself.
 */
final class LiquidGolemGoals {
    private final EntityGolemBase golem;
    private TileCrucible home;
    private BlockPos source, route, origin;
    private ServerLevel routeLevel;
    private Path ownedPath;
    private boolean watched, moving, gathering, emptying;
    private boolean gotoReady, gatherReady, emptyReady, nudged;
    private int pathCount, gatherCount;
    private BlockPos attemptSource, attemptAnchor;
    private BlockState attemptState;
    // Per marked block: the sources still to drain (Entropy). Rebuilt when empty; targets are re-validated at every pulse.
    private final Map<BlockPos, ArrayDeque<BlockPos>> queues = new HashMap<>();
    private TileCrucible deliveryAttempt;

    private record Source(BlockPos pos, BlockState state) {}

    LiquidGolemGoals(EntityGolemBase golem) { this.golem = golem; }
    void install() {
        golem.addWorkGoal(1, new Empty());
        golem.addWorkGoal(2, new Gather());
        golem.addWorkGoal(3, new Goto());
    }
    boolean hasRoute() { return watched || route != null || moving || gathering || emptying; }
    boolean ownsCollectionAttempt(BlockPos pos, BlockState state) {
        return gathering && watched && attemptSource != null && attemptSource.equals(pos) && attemptState == state;
    }
    boolean ownsCollectionAttempt(BlockPos anchor, BlockPos pos, BlockState state) {
        return ownsCollectionAttempt(pos, state) && attemptAnchor != null && attemptAnchor.equals(anchor);
    }
    boolean ownsDeliveryAttempt(TileCrucible actualHome) { return emptying && deliveryAttempt == actualHome && actualHome != null; }
    static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
    static boolean sourceWater(ServerLevel level, BlockPos pos, BlockState state) {
        return loaded(level, pos) && state.is(Blocks.WATER) && state.getFluidState().isSource()
                && !state.hasBlockEntity() && level.getBlockEntity(pos) == null;
    }
    private boolean ready() { return golem.liquidReady() && golem.currentWaterHome(home); }
    private boolean room() { return golem.waterSpace() >= GolemWaterCargo.BUCKET; }
    private double distance(BlockPos pos) {
        return golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
    }
    private Source sourceAt(ServerLevel server, GolemMarker marker) {
        // Dimension rejection precedes ALL reads at a marker's coordinates.
        if (!marker.dimension().equals(server.dimension().location()) || !loaded(server, marker.pos())) return null;
        BlockState state = server.getBlockState(marker.pos());
        return sourceWater(server, marker.pos(), state) ? new Source(marker.pos(), state) : null;
    }
    private List<Source> nearbySources() {
        List<Source> result = new ArrayList<>();
        if (!(golem.level() instanceof ServerLevel server)) return result;
        Set<BlockPos> seen = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location()) || !seen.add(marker.pos()) || distance(marker.pos()) >= 4) continue;
            Source found = sourceAt(server, marker);
            if (found != null) result.add(found);
        }
        return result; // Marker list order; faces of one block never pay twice in a pulse.
    }
    private Source nearestSource(ServerLevel server) {
        Source best = null;
        double nearest = Double.MAX_VALUE, range = golem.getRange() * golem.getRange();
        Set<BlockPos> seen = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location()) || !seen.add(marker.pos())) continue;
            double distance = distance(marker.pos());
            if (distance > range || distance >= nearest) continue;
            Source candidate = sourceAt(server, marker);
            if (candidate != null) { best = candidate; nearest = distance; }
        }
        return best; // Inclusive actor range, strict-nearer tie; no home radius or unmarked search.
    }
    /**
     * What the next pulse drains for a marked block that is next to the golem: the marked block itself, or with Entropy the head of its
     * queue (original AILiquidGather:225-262). Entries that are no longer ordinary source water or no longer in range are skipped; when
     * the queue is spent the marked block is the target.
     */
    private Source nextTarget(ServerLevel server, Source anchor) {
        if (golem.getUpgradeAmount(GolemUpgrade.ENTROPY) <= 0) return anchor;
        Set<BlockPos> marked = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) marked.add(marker.pos());
        queues.keySet().removeIf(pos -> !marked.contains(pos));
        float range = golem.getRange();
        ArrayDeque<BlockPos> queue = queues.get(anchor.pos());
        if (queue == null || queue.isEmpty()) {
            queue = new ArrayDeque<>(GolemLiquidQueue.build(anchor.pos(), range,
                    pos -> loaded(server, pos) && server.getBlockState(pos).is(Blocks.WATER),
                    pos -> loaded(server, pos) && sourceWater(server, pos, server.getBlockState(pos))));
            queues.put(anchor.pos(), queue);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!loaded(server, pos) || !GolemLiquidQueue.inRange(anchor.pos(), pos, range)) continue;
            BlockState state = server.getBlockState(pos);
            if (sourceWater(server, pos, state)) return new Source(pos.immutable(), state);
        }
        return anchor;
    }
    private BlockPos approach(ServerLevel server, BlockPos selected) {
        BlockPos best = selected;
        double nearest = golem.distanceToSqr(selected.getX(), selected.getY(), selected.getZ());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos pos = selected.offset(dx, 0, dz); // Original SAME Y, never an invented waterY+1.
            double distance = golem.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (distance >= nearest || !loaded(server, pos)) continue;
            BlockState state = server.getBlockState(pos);
            if (state.isSolidRender(server, pos) && state.isCollisionShapeFullBlock(server, pos)) {
                best = pos;
                nearest = distance;
            }
        }
        return best;
    }
    /** Decisions are actual server ticks, not modern selector's half-rate admission checks. */
    void serverTick() {
        if (!golem.liquidReady()) { reset(); return; }
        if (golem.tickCount % 5 != 0) return;
        try {
            TileCrucible actual = golem.permittedWaterHome();
            if (actual == null) { reset(); return; }
            if (home != null && home != actual) reset();
            home = actual;
            if (!room()) {
                clearSource();
                emptyReady = canEmpty();
                return;
            }
            if (moving || gathering || emptying) return;
            emptyReady = canEmpty();
            if (emptyReady) { clearSource(); return; }
            if (watched && golem.getNavigation().isDone() && !nearbySources().isEmpty()) {
                gatherReady = true;
                gotoReady = false;
                return;
            }
            Source selected = nearestSource((ServerLevel) golem.level());
            if (selected == null) { clearSource(); return; }
            source = selected.pos();
            watched = true;
            if (golem.getNavigation().isDone() && !nearbySources().isEmpty()) {
                gatherReady = true;
                gotoReady = false;
                route = null;
            } else {
                route = approach((ServerLevel) golem.level(), source);
                gotoReady = true;
                gatherReady = false;
            }
        } catch (RuntimeException failure) {
            reset();
            Thaumcraft.LOG.warn("Golem Decanting decision failed; retaining water cargo", failure);
        }
    }
    private boolean canEmpty() {
        return ready() && golem.getWaterAmountMb() > 0 && home.getWaterAmount() < TileCrucible.CAPACITY
                && golem.getNavigation().isDone() && golem.distanceToHomeSqr() <= 5;
    }
    private void stopOwnedPath() {
        if (ownedPath != null && golem.level() == routeLevel && golem.getNavigation().getPath() == ownedPath) golem.getNavigation().stop();
        ownedPath = null;
        routeLevel = null;
    }
    private void clearSource() {
        stopOwnedPath();
        watched = moving = gathering = gotoReady = gatherReady = false;
        source = route = origin = null;
        pathCount = gatherCount = 0;
    }
    void reset() {
        clearSource(); // Never touches actual waterMb, native item cargo or paid fishing output.
        home = deliveryAttempt = null;
        emptying = emptyReady = false;
        attemptSource = attemptAnchor = null;
        attemptState = null;
        queues.clear();
    }
    private abstract class LiquidGoal extends Goal {
        LiquidGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
    }
    private final class Empty extends LiquidGoal {
        private boolean attempted;
        @Override public boolean canUse() { return emptyReady && canEmpty(); }
        @Override public boolean canContinueToUse() { return false; }
        @Override public void start() { emptyReady = false; emptying = true; attempted = false; }
        @Override public void tick() {
            if (attempted || !emptying) return;
            attempted = true; // Even the intervening tickRunningGoals pass cannot repeat a one-shot fill.
            if (!canEmpty()) return;
            TileCrucible actual = home;
            deliveryAttempt = actual;
            try { golem.deliverWaterToHome(actual); }
            finally { deliveryAttempt = null; }
        }
        @Override public void stop() { emptying = false; }
    }
    private final class Gather extends LiquidGoal {
        @Override public boolean canUse() {
            return gatherReady && watched && ready() && room() && golem.getNavigation().isDone() && !nearbySources().isEmpty();
        }
        @Override public boolean canContinueToUse() {
            return gathering && watched && gatherCount < 20 && ready() && room() && golem.getNavigation().isDone();
        }
        @Override public boolean isInterruptable() { return false; }
        @Override public void start() { gatherReady = false; gathering = true; gatherCount = 0; }
        @Override public void tick() {
            if (!gathering || !watched || !ready() || !room() || !golem.getNavigation().isDone()) { clearSource(); return; }
            if (gatherCount >= 20) { clearSource(); return; }
            if (++gatherCount < 10) return;
            for (Source candidate : nearbySources()) {
                if (!gathering || !watched || !ready() || !room()) return;
                int before = golem.getWaterAmountMb();
                Source target = nextTarget((ServerLevel) golem.level(), candidate);
                attemptAnchor = candidate.pos();
                attemptSource = target.pos();
                attemptState = target.state();
                try { golem.collectMarkedSourceWater(candidate.pos(), target.pos(), target.state()); }
                finally { attemptSource = attemptAnchor = null; attemptState = null; }
                if (golem.getWaterAmountMb() != before) {
                    gatherCount = 0;
                    if (!room()) clearSource();
                    return; // At most ONE source, including a paid operation whose feedback threw.
                }
            }
        }
        @Override public void stop() { clearSource(); }
    }
    private final class Goto extends LiquidGoal {
        @Override public boolean canUse() { return gotoReady && route != null && watched && ready() && room(); }
        @Override public boolean canContinueToUse() {
            return moving && watched && route != null && pathCount > 0 && ready() && room() && !golem.getNavigation().isDone();
        }
        @Override public void start() {
            gotoReady = false;
            moving = true;
            origin = golem.blockPosition();
            nudged = false;
            pathCount = 200;
            if (!move(route)) clearSource();
        }
        private boolean move(BlockPos pos) {
            if (pos == null || !ready() || !(golem.level() instanceof ServerLevel server)
                    || !loaded(server, pos) || !golem.loadedNavigationArea(pos)) return false;
            Path path = golem.getNavigation().createPath(pos, 0); // Integer same-Y input; native navigation supplies movement.
            if (path == null || !ready() || !moving || !watched) return false;
            routeLevel = server;
            ownedPath = path;
            return golem.getNavigation().moveTo(path, 1.0) && ready() && moving && watched;
        }
        @Override public void tick() {
            if (!moving || !ready() || !room()) { clearSource(); return; }
            if (--pathCount > 0) return;
            if (!nudged && origin != null && origin.equals(golem.blockPosition()) && golem.loadedNavigationArea(golem.blockPosition())) {
                nudged = true;
                Vec3 nudge = DefaultRandomPos.getPos(golem, 2, 1);
                if (nudge != null && move(BlockPos.containing(nudge))) { pathCount = 20; return; }
            }
            clearSource();
        }
        @Override public void stop() {
            stopOwnedPath();
            moving = gotoReady = false;
            route = null;
            pathCount = 0;
            if (!ready() || !room() || !golem.getNavigation().isDone() || nearbySources().isEmpty()) watched = false;
        }
    }
}

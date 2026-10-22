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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.items.golems.GolemFluidCargo;
import thaumcraft.common.items.golems.GolemLiquidQueue;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.lib.fluid.GolemFluidAccess;
import thaumcraft.common.lib.fluid.GolemFluidBlocks;
import thaumcraft.common.lib.fluid.GolemFluidHandler;

/**
 * The Decanting golem of the original, fluid for fluid.
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/fluid/AILiquidEmpty.java},
 * {@code AILiquidGather.java} and {@code AILiquidGoto.java} with the searches of
 * {@code thaumcraft/common/entities/golems/GolemHelper.java:331-446}:</p>
 *
 * <ul>
 *   <li>the <b>home</b> is any {@code IFluidHandler} at the home cell of the golem and the wanted
 *       fluids are the ones that it can take there ({@code getMissingLiquids}:341-369);</li>
 *   <li>the <b>source</b> is any marked {@code IFluidHandler} of the wanted fluid
 *       ({@code getMarkedFluidHandlers}:420-429) or a marked fluid block of it
 *       ({@code getMarkedFluidBlocks}:431-446);</li>
 *   <li>the <b>cargo</b> is a {@code FluidStack}, that is a fluid and an amount in millibuckets.</li>
 * </ul>
 *
 * <p>With an Entropy upgrade the golem drains, one block per pulse, the queue
 * {@link GolemLiquidQueue} builds from the marked block instead of the marked block itself.</p>
 */
final class LiquidGolemGoals {
    private final EntityGolemBase golem;
    private BlockPos home;
    private ResourceLocation wanted;
    private BlockPos source, route, origin;
    private ServerLevel routeLevel;
    private Path ownedPath;
    private boolean watched, moving, gathering, emptying;
    private boolean gotoReady, gatherReady, emptyReady, nudged;
    private int pathCount, gatherCount;
    private BlockPos attemptSource, attemptAnchor;
    private BlockState attemptState;
    private BlockPos attemptHandler;
    private Direction attemptHandlerFace;
    private ResourceLocation attemptFluid;
    // Per marked block: the sources still to drain (Entropy). Rebuilt when empty; targets are re-validated at every pulse.
    private final Map<BlockPos, ArrayDeque<BlockPos>> queues = new HashMap<>();

    private record Source(BlockPos pos, BlockState state) {}

    LiquidGolemGoals(EntityGolemBase golem) { this.golem = golem; }
    void install() {
        golem.addWorkGoal(GoalPriority.LIQUID_EMPTY, new Empty());
        golem.addWorkGoal(GoalPriority.LIQUID_GATHER, new Gather());
        golem.addWorkGoal(GoalPriority.LIQUID_GOTO, new Goto());
    }
    boolean hasRoute() { return watched || route != null || moving || gathering || emptying; }
    boolean ownsCollectionAttempt(BlockPos pos, BlockState state) {
        return gathering && watched && attemptSource != null && attemptSource.equals(pos) && attemptState == state;
    }
    boolean ownsCollectionAttempt(BlockPos anchor, BlockPos pos, BlockState state) {
        return ownsCollectionAttempt(pos, state) && attemptAnchor != null && attemptAnchor.equals(anchor);
    }
    boolean ownsCollectionAttempt(BlockPos pos, Direction face) {
        return gathering && watched && attemptHandler != null && attemptHandler.equals(pos) && attemptHandlerFace == face;
    }
    boolean ownsDeliveryAttempt(BlockPos actualHome) { return emptying && deliveryAttempt != null && deliveryAttempt.equals(actualHome); }
    private BlockPos deliveryAttempt;
    /** The fluid of the block that the current gather attempt drains. */
    ResourceLocation anchorFluid() { return attemptFluid; }
    static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
    /**
     * Whether the block is a source of the wanted fluid.
     *
     * <p>Oracle {@code GolemHelper.getMarkedFluidBlocks}:431-446: a marked block is a source of the
     * wanted fluid when it holds that fluid and the fluid of the block is a full source.</p>
     */
    static boolean sourceFluid(ServerLevel level, BlockPos pos, BlockState state, ResourceLocation fluid) {
        return loaded(level, pos) && GolemFluidBlocks.isFluid(state, fluid) && GolemFluidBlocks.isSource(state);
    }
    private boolean ready() { return golem.liquidReady(); }
    private boolean room() { return golem.fluidSpace() >= GolemFluidCargo.BUCKET; }
    private double distance(BlockPos pos) {
        return golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
    }
    private Source sourceAt(ServerLevel server, GolemMarker marker, ResourceLocation fluid) {
        // Dimension rejection precedes ALL reads at a marker's coordinates.
        if (!marker.dimension().equals(server.dimension().location()) || !loaded(server, marker.pos())) return null;
        BlockState state = server.getBlockState(marker.pos());
        return sourceFluid(server, marker.pos(), state, fluid) ? new Source(marker.pos(), state) : null;
    }
    /** The marked fluid blocks of the wanted fluid next to the golem. */
    private List<Source> nearbySources(ResourceLocation fluid) {
        List<Source> result = new ArrayList<>();
        if (!(golem.level() instanceof ServerLevel server)) return result;
        Set<BlockPos> seen = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location()) || !seen.add(marker.pos()) || distance(marker.pos()) >= 4) continue;
            Source found = sourceAt(server, marker, fluid);
            if (found != null) result.add(found);
        }
        return result; // Marker list order; faces of one block never pay twice in a pulse.
    }
    /**
     * The marked fluid handlers of the wanted fluid next to the golem.
     *
     * <p>Oracle {@code GolemHelper.getMarkedFluidHandlersAdjacentToGolem}:409-418: a marked cell
     * that holds the wanted fluid and stands next to the golem.</p>
     */
    private List<GolemMarker> nearbyHandlers(ResourceLocation fluid) {
        List<GolemMarker> result = new ArrayList<>();
        for (GolemMarker marker : GolemHelper.getMarkedFluidHandlersAdjacentToGolem(golem, fluid)) result.add(marker);
        return result;
    }
    private Source nearestSource(ServerLevel server, ResourceLocation fluid) {
        Source best = null;
        double nearest = Double.MAX_VALUE, range = golem.getRange() * golem.getRange();
        Set<BlockPos> seen = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location()) || !seen.add(marker.pos())) continue;
            double distance = distance(marker.pos());
            if (distance > range || distance >= nearest) continue;
            Source candidate = sourceAt(server, marker, fluid);
            if (candidate != null) { best = candidate; nearest = distance; }
        }
        return best; // Inclusive actor range, strict-nearer tie; no home radius or unmarked search.
    }
    /**
     * What the next pulse drains for a marked block that is next to the golem: the marked block itself, or with Entropy the head of its
     * queue (original AILiquidGather:225-262). Entries that are no longer ordinary source fluid or no longer in range are skipped; when
     * the queue is spent the marked block is the target.
     */
    private Source nextTarget(ServerLevel server, Source anchor, ResourceLocation fluid) {
        if (golem.getUpgradeAmount(GolemUpgrade.ENTROPY) <= 0) return anchor;
        Set<BlockPos> marked = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) marked.add(marker.pos());
        queues.keySet().removeIf(pos -> !marked.contains(pos));
        float range = golem.getRange();
        ArrayDeque<BlockPos> queue = queues.get(anchor.pos());
        if (queue == null || queue.isEmpty()) {
            queue = new ArrayDeque<>(GolemLiquidQueue.build(anchor.pos(), range,
                    pos -> loaded(server, pos) && server.getBlockState(pos).getFluidState().isSource(),
                    pos -> loaded(server, pos) && sourceFluid(server, pos, server.getBlockState(pos), fluid)));
            queues.put(anchor.pos(), queue);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!loaded(server, pos) || !GolemLiquidQueue.inRange(anchor.pos(), pos, range)) continue;
            BlockState state = server.getBlockState(pos);
            if (sourceFluid(server, pos, state, fluid)) return new Source(pos.immutable(), state);
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
    /** The wanted fluids of the original: the fluids the home can take. */
    private List<ResourceLocation> wantedFluids() { return GolemHelper.getMissingLiquids(golem); }
    /** The wanted fluid that the cargo of the golem is, or null. */
    private ResourceLocation carriedFluid() {
        ResourceLocation id = golem.getFluidId();
        if (id == null || golem.getFluidAmountMb() <= 0) return null;
        return id;
    }
    /** The first wanted fluid that the world offers a source of. */
    private ResourceLocation firstWantedWithSource(ServerLevel server) {
        for (ResourceLocation fluid : wantedFluids()) {
            if (!nearbySources(fluid).isEmpty() || !nearbyHandlers(fluid).isEmpty()) return fluid;
        }
        return null;
    }
    /** Decisions are actual server ticks, not modern selector's half-rate admission checks. */
    void serverTick() {
        if (!golem.liquidReady()) { reset(); return; }
        if (golem.tickCount % 5 != 0) return;
        try {
            BlockPos actual = golem.permittedFluidHome();
            if (actual == null) { reset(); return; }
            if (home != null && !home.equals(actual)) reset();
            home = actual;
            if (!room()) {
                clearSource();
                emptyReady = canEmpty();
                return;
            }
            if (moving || gathering || emptying) return;
            emptyReady = canEmpty();
            if (emptyReady) { clearSource(); return; }
            if (!(golem.level() instanceof ServerLevel server)) { clearSource(); return; }
            ResourceLocation candidate = firstWantedWithSource(server);
            if (candidate == null) { clearSource(); return; }
            wanted = candidate;
            if (watched && golem.getNavigation().isDone() && !nearbySources(candidate).isEmpty()) {
                gatherReady = true;
                gotoReady = false;
                return;
            }
            Source selected = nearestSource(server, candidate);
            if (selected == null) { clearSource(); return; }
            source = selected.pos();
            watched = true;
            if (golem.getNavigation().isDone() && !nearbySources(candidate).isEmpty()) {
                gatherReady = true;
                gotoReady = false;
                route = null;
            } else {
                route = approach(server, source);
                gotoReady = true;
                gatherReady = false;
            }
        } catch (RuntimeException failure) {
            reset();
            Thaumcraft.LOG.warn("Golem Decanting decision failed; retaining fluid cargo", failure);
        }
    }
    private boolean canEmpty() {
        // Oracle AILiquidEmpty:45-55 - a cargo, an idle navigator, the squared distance of five and the wanted set.
        ResourceLocation carried = carriedFluid();
        if (carried == null || !ready() || !golem.getNavigation().isDone()) return false;
        if (golem.distanceToHomeSqr() > 5) return false;
        return wantedFluids().contains(carried);
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
        attemptSource = attemptAnchor = attemptHandler = null;
        attemptState = null;
        attemptHandlerFace = null;
        attemptFluid = null;
    }
    void reset() {
        clearSource(); // Never touches actual fluid cargo, native item cargo or paid fishing output.
        home = deliveryAttempt = null;
        wanted = null;
        emptying = emptyReady = false;
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
            BlockPos actual = home;
            deliveryAttempt = actual;
            try { golem.deliverFluidToHome(actual); }
            finally { deliveryAttempt = null; }
        }
        @Override public void stop() { emptying = false; }
    }
    private final class Gather extends LiquidGoal {
        @Override public boolean canUse() {
            return gatherReady && watched && ready() && room() && golem.getNavigation().isDone()
                    && wanted != null && !nearbySources(wanted).isEmpty();
        }
        @Override public boolean canContinueToUse() {
            return gathering && watched && gatherCount < 20 && ready() && room() && golem.getNavigation().isDone();
        }
        @Override public boolean isInterruptable() { return false; }
        @Override public void start() { gatherReady = false; gathering = true; gatherCount = 0; }
        @Override public void tick() {
            if (!gathering || !watched || !ready() || !room() || !golem.getNavigation().isDone() || wanted == null) { clearSource(); return; }
            if (gatherCount >= 20) { clearSource(); return; }
            if (++gatherCount < 10) return;
            // Oracle AILiquidGather:141-160 - the marked fluid handlers come first, then the marked fluid blocks.
            for (GolemMarker marker : nearbyHandlers(wanted)) {
                if (!gathering || !watched || !ready() || !room()) return;
                int before = golem.getFluidAmountMb();
                attemptHandler = marker.pos();
                attemptHandlerFace = marker.face();
                attemptFluid = wanted;
                try { golem.collectMarkedHandlerFluid(marker.pos(), marker.face(), wanted, golem.fluidSpace()); }
                finally { attemptHandler = null; attemptHandlerFace = null; attemptFluid = null; }
                if (golem.getFluidAmountMb() != before) {
                    gatherCount = 0;
                    if (!room()) clearSource();
                    return; // At most ONE source, including a paid operation whose feedback threw.
                }
            }
            for (Source candidate : nearbySources(wanted)) {
                if (!gathering || !watched || !ready() || !room()) return;
                int before = golem.getFluidAmountMb();
                Source target = nextTarget((ServerLevel) golem.level(), candidate, wanted);
                attemptAnchor = candidate.pos();
                attemptSource = target.pos();
                attemptState = target.state();
                attemptFluid = wanted;
                try { golem.collectMarkedSourceFluid(candidate.pos(), target.pos(), target.state()); }
                finally { attemptSource = attemptAnchor = null; attemptState = null; attemptFluid = null; }
                if (golem.getFluidAmountMb() != before) {
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
            if (!ready() || !room() || !golem.getNavigation().isDone() || nearbySources(wanted).isEmpty()) watched = false;
        }
    }
}

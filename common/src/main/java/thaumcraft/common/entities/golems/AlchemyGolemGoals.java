package thaumcraft.common.entities.golems;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.golems.EntityGolemBase.GoalPriority;
import thaumcraft.common.items.golems.GolemAspectCargo;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileEssentiaReservoir;

/** Finite alembic/reservoir -> actual entity cargo -> directly marked normal jar/reservoir. */
final class AlchemyGolemGoals {
    record Source(BlockEntity base, BlockEntity endpoint, Aspect aspect, int limit) {}
    private final EntityGolemBase golem;
    private BlockEntity target;
    private Path ownedPath;
    private ServerLevel pathLevel;
    private BlockPos origin;
    private long gatherAfter;
    private int pathCount;
    private boolean gotoReady, emptyReady, gathering, moving, emptying, nudged;
    private BlockEntity collecting, delivering;
    private Aspect attemptedAspect;

    AlchemyGolemGoals(EntityGolemBase golem) { this.golem = golem; }
    void install() {
        golem.addWorkGoal(GoalPriority.ESSENTIA_EMPTY, new Empty());
        golem.addWorkGoal(GoalPriority.ESSENTIA_GATHER, new Gather());
        golem.addWorkGoal(GoalPriority.ESSENTIA_GOTO, new Goto());
    }
    boolean hasRoute() { return gathering || emptying || moving || target != null && (gotoReady || emptyReady); }
    boolean ownsCollectionAttempt(BlockEntity actual, Aspect aspect) {
        return gathering && collecting == actual && actual != null && attemptedAspect == aspect;
    }
    boolean ownsDeliveryAttempt(BlockEntity actual, Aspect aspect) {
        return emptying && delivering == actual && actual != null && attemptedAspect == aspect;
    }
    static boolean canonical(Aspect aspect) { return aspect != null && Aspect.getAspect(aspect.getTag()) == aspect; }
    static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
    static boolean current(ServerLevel level, BlockEntity tile) {
        return tile != null && tile.getLevel() == level && !tile.isRemoved() && loaded(level, tile.getBlockPos())
                && level.getBlockEntity(tile.getBlockPos()) == tile && tile.getType().isValid(level.getBlockState(tile.getBlockPos()));
    }
    private boolean permitted(ServerLevel server, BlockPos pos, boolean query) {
        return golem.level() == server && loaded(server, pos) && (!query || golem.mayUseAlchemyPosition(pos))
                && golem.level() == server && golem.alchemyReady();
    }
    private Source candidate(BlockEntity base, TileAlembic alembic) {
        Aspect aspect = alembic.getAspect();
        int amount = alembic.getAmount(), carried = golem.getEssentiaAmount();
        if (!canonical(aspect) || amount <= 0 || amount > 32 || !alembic.canOutputTo(Direction.UP)
                || carried > 0 && golem.getEssentiaAspect() != aspect || carried >= golem.getCarryLimit()) return null;
        return new Source(base, alembic, aspect, Math.min(amount, golem.getCarryLimit() - carried));
    }
    /** False is read-only eligibility/precommit validation, never payment authority. */
    Source findSource(boolean query) {
        if (!golem.alchemyReady() || !golem.getNavigation().isDone() || golem.getEssentiaAmount() >= golem.getCarryLimit()
                || !(golem.level() instanceof ServerLevel server)) return null;
        BlockPos pos = golem.homeInventoryPosition();
        if (golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 6 || !permitted(server, pos, query)) return null;
        BlockEntity base = server.getBlockEntity(pos);
        if (!current(server, base)) return null;
        if (base instanceof TileEssentiaReservoir reservoir && !reservoir.isQuarantined()) {
            // Original AIEssentiaGather uses the reservoir's own port and its FIRST stored aspect, not total stock.
            Aspect aspect = reservoir.getEssentiaType(null);
            int carried = golem.getEssentiaAmount(), amount = reservoir.containerContains(aspect);
            if (!canonical(aspect) || amount <= 0 || !reservoir.canOutputTo(reservoir.getFacing())
                    || carried >= golem.getCarryLimit() || carried > 0 && golem.getEssentiaAspect() != aspect) return null;
            return new Source(base, reservoir, aspect, Math.min(amount, golem.getCarryLimit() - carried));
        }
        if (base.getClass() == TileAlembic.class) {
            TileAlembic alembic = (TileAlembic) base;
            return alembic.canOutputTo(golem.getHomeFacing()) ? candidate(base, alembic) : null;
        }
        if (base.getClass() != TileAlchemyFurnace.class) return null;
        Source best = null;
        int largest = 0;
        for (int height = 1; height <= 5; height++) {
            BlockPos upper = pos.above(height);
            if (!permitted(server, upper, query)) break;
            BlockEntity tile = server.getBlockEntity(upper);
            if (!current(server, tile) || tile.getClass() != TileAlembic.class) continue; // Original AIEssentiaGather:51-67 scans the levels cY+5..cY+0 without a break: a gap in the column does not stop the search.
            TileAlembic alembic = (TileAlembic) tile;
            Source next = candidate(base, alembic);
            if (next != null && alembic.getAmount() >= largest) {
                best = next;
                largest = alembic.getAmount(); // Ascending scan: higher Y wins equal compatible amounts.
            }
        }
        return golem.level() == server && golem.alchemyReady() && current(server, base) ? best : null;
    }
    boolean currentSource(BlockEntity base, BlockEntity actual, Aspect aspect, int units) {
        Source fresh = findSource(false);
        return fresh != null && fresh.base() == base && fresh.endpoint() == actual && fresh.aspect() == aspect
                && units > 0 && units <= fresh.limit();
    }
    private double homeDistance(BlockPos pos) {
        BlockPos home = golem.getHomePosition();
        double x = pos.getX() + .5 - home.getX(), y = pos.getY() + .5 - home.getY(), z = pos.getZ() + .5 - home.getZ();
        return x * x + y * y + z * z; // INTEGER home, not home center or actor-nearest.
    }
    private int jarStage(TileJar jar, Aspect aspect) {
        if (!(golem.level() instanceof ServerLevel server) || !canonical(aspect) || jar == null || jar.getClass() != TileJar.class
                || !current(server, jar) || jar.isVoid() || homeDistance(jar.getBlockPos()) > golem.getRange() * golem.getRange()) return -1;
        int amount = jar.getAmount();
        Aspect stored = jar.getAspect(), label = jar.getFilter();
        if (jar.getMaxAmount() != 64 || amount < 0 || amount >= 64 || label != null && label != aspect
                || stored != null && stored != aspect || amount > 0 && stored != aspect) return -1;
        return label != null ? (amount > 0 ? 0 : 1) : (amount > 0 ? 2 : 3);
    }
    static int space(BlockEntity tile) {
        return tile instanceof TileJar jar ? jar.getMaxAmount() - jar.getAmount()
                : tile instanceof TileEssentiaReservoir reservoir ? reservoir.getSpace()
                // The original hands the whole cargo to a generic transport and keeps
                // what it took (AIEssentiaEmpty:110); the transport decides the amount.
                : tile instanceof IEssentiaTransport ? GolemAspectCargo.MAX_AMOUNT : 0;
    }
    /**
     * The marked side the original's payment would use.
     *
     * <p>Oracle {@code AIEssentiaEmpty:104-112}: the payment walks the marked
     * sides of the destination and uses the first one the transport accepts
     * ({@code canInputFrom}, a suction above zero and a suction type that is the
     * golem's aspect or none).</p>
     */
    private Direction deliveryFace(BlockEntity tile, IEssentiaTransport transport, Aspect aspect) {
        if (tile == null || transport == null || aspect == null) return null;
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.pos().equals(tile.getBlockPos())) continue;
            Direction face = marker.face();
            if (!transport.canInputFrom(face) || transport.getSuctionAmount(face) <= 0) continue;
            Aspect suction = transport.getSuctionType(face);
            if (suction != null && suction != aspect) continue;
            return face;
        }
        return null;
    }
    /** The original's third kind of destination: any other marked essentia transport. */
    private int transportStage(BlockEntity tile, IEssentiaTransport transport, Aspect aspect) {
        if (!canonical(aspect) || !(golem.level() instanceof ServerLevel server) || !current(server, tile)) return -1;
        if (homeDistance(tile.getBlockPos()) > golem.getRange() * golem.getRange()) return -1;
        // The original's "others" are always candidates, like the reservoir.
        return deliveryFace(tile, transport, aspect) == null ? -1 : 0;
    }
    private int destinationStage(BlockEntity tile, Aspect aspect) {
        if (tile instanceof TileJar jar) return jarStage(jar, aspect);
        if (tile instanceof TileEssentiaReservoir reservoir) {
            if (!(golem.level() instanceof ServerLevel server) || !canonical(aspect) || !current(server, tile) || reservoir.isQuarantined()
                    || homeDistance(tile.getBlockPos()) > golem.getRange() * golem.getRange()
                    || !reservoir.canInputFrom(reservoir.getFacing()) || reservoir.getSuctionAmount(reservoir.getFacing()) <= 0
                    || reservoir.getSuctionType(reservoir.getFacing()) != null && reservoir.getSuctionType(reservoir.getFacing()) != aspect
                    || reservoir.getSpace() <= 0) return -1;
            return 0; // Original GolemHelper: reservoirs share the first tier with nonempty labelled jars.
        }
        if (tile instanceof IEssentiaTransport transport) return transportStage(tile, transport, aspect);
        return -1;
    }
    /** The marked side of a generic transport destination, or null for the jar and the reservoir. */
    Direction deliveryFaceFor(BlockEntity tile, Aspect aspect) {
        return tile instanceof IEssentiaTransport transport ? deliveryFace(tile, transport, aspect) : null;
    }
    boolean currentDestination(BlockEntity jar, Aspect aspect, int units) {
        if (!golem.alchemyReady() || units <= 0 || destinationStage(jar, aspect) < 0 || units > space(jar)) return false;
        for (GolemMarker marker : golem.getMarkers()) {
            if (marker.dimension().equals(golem.level().dimension().location()) && marker.pos().equals(jar.getBlockPos())) return true;
        }
        return false;
    }
    private BlockEntity findJar() {
        if (!golem.alchemyReady() || golem.getEssentiaAmount() <= 0 || !(golem.level() instanceof ServerLevel server)) return null;
        Aspect aspect = golem.getEssentiaAspect();
        List<BlockEntity> allowed = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (GolemMarker marker : golem.getMarkers()) {
            if (!marker.dimension().equals(server.dimension().location()) || !seen.add(marker.pos())
                    || homeDistance(marker.pos()) > golem.getRange() * golem.getRange()) continue;
            if (!permitted(server, marker.pos(), true)) {
                if (golem.level() != server || !golem.alchemyReady()) return null;
                continue;
            }
            BlockEntity tile = server.getBlockEntity(marker.pos());
            if (tile instanceof TileJar jar && jar.getClass() == TileJar.class || tile instanceof TileEssentiaReservoir
                    || tile instanceof IEssentiaTransport) allowed.add(tile);
        }
        if (golem.level() != server || !golem.alchemyReady() || golem.getEssentiaAspect() != aspect) return null;
        BlockEntity best = null;
        int stage = 4;
        double nearest = Double.MAX_VALUE;
        for (BlockEntity jar : allowed) { // Fresh labels/type/room after every query callback has finished.
            int next = destinationStage(jar, aspect);
            double distance = homeDistance(jar.getBlockPos());
            if (next >= 0 && (next < stage || next == stage && distance < nearest)) {
                stage = next;
                nearest = distance;
                best = jar; // Strict distance ties preserve marker order, unlike original static HashMap.
            }
        }
        return best;
    }
    private boolean near(BlockEntity jar) {
        BlockPos pos = jar.getBlockPos();
        return golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 4;
    }
    void serverTick() {
        if (!golem.alchemyReady()) { reset(); return; }
        try {
            if (!gathering && !moving && !emptying && golem.getNavigation().isDone()) {
                BlockEntity fresh = findJar();
                emptyReady = fresh != null && near(fresh);
                if (emptyReady) target = fresh;
            }
            if (golem.tickCount % 5 != 0 || moving || gathering || emptying) return;
            if (golem.getEssentiaAmount() == 0) { clearPath(); target = null; gotoReady = emptyReady = false; return; }
            BlockEntity fresh = findJar();
            target = fresh;
            gotoReady = fresh != null && (!golem.getNavigation().isDone() || !near(fresh));
            emptyReady = fresh != null && golem.getNavigation().isDone() && near(fresh);
        } catch (RuntimeException failure) {
            reset();
            Thaumcraft.LOG.warn("Golem Alchemy decision failed; preserving actual essentia", failure);
        }
    }
    private void clearPath() {
        if (ownedPath != null && golem.level() == pathLevel && golem.getNavigation().getPath() == ownedPath) golem.getNavigation().stop();
        ownedPath = null;
        pathLevel = null;
    }
    void reset() {
        clearPath();
        target = delivering = null;
        collecting = null;
        attemptedAspect = null;
        origin = null;
        pathCount = 0;
        gotoReady = emptyReady = gathering = moving = emptying = false;
        // Never clears cargo. Keep the short/early-failure delay through menu/route resets.
    }
    private abstract class AlchemyGoal extends Goal {
        AlchemyGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
    }
    private final class Gather extends AlchemyGoal {
        private boolean attempted;
        @Override public boolean canUse() {
            // Read-only eligibility is not payment authority. It may fill compatible
            // cargo again as soon as the TWO-tick delay expires, ahead of Goto3.
            return golem.level().getGameTime() >= gatherAfter && findSource(false) != null;
        }
        @Override public boolean canContinueToUse() { return false; }
        @Override public void start() { gathering = true; attempted = false; gatherAfter = golem.level().getGameTime() + 20; }
        @Override public void tick() {
            if (attempted || !gathering) return;
            attempted = true;
            try {
                Source fresh = findSource(true);
                if (fresh == null || !gathering) return;
                collecting = fresh.endpoint();
                attemptedAspect = fresh.aspect();
                if (golem.collectAlchemyEssentia(fresh.endpoint(), fresh.aspect(), fresh.limit())) gatherAfter = golem.level().getGameTime() + 2;
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.warn("Golem Alchemy gather failed; keeping paid/previous stock and early-failure delay", failure);
            } finally { collecting = null; attemptedAspect = null; }
        }
        @Override public void stop() { gathering = false; }
    }
    private final class Empty extends AlchemyGoal {
        private boolean attempted;
        @Override public boolean canUse() { return emptyReady && golem.alchemyReady() && golem.getNavigation().isDone() && golem.getEssentiaAmount() > 0; }
        @Override public boolean canContinueToUse() { return false; }
        @Override public void start() { emptying = true; emptyReady = false; attempted = false; }
        @Override public void tick() {
            if (attempted || !emptying) return;
            attempted = true;
            try {
                BlockEntity fresh = findJar(); // NEW search, never a saved route entitlement.
                if (fresh == null || !emptying || !golem.getNavigation().isDone() || !near(fresh)) return;
                delivering = fresh;
                attemptedAspect = golem.getEssentiaAspect();
                Direction face = deliveryFaceFor(fresh, attemptedAspect);
                golem.deliverAlchemyEssentia(fresh, face, attemptedAspect, golem.getEssentiaAmount());
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.warn("Golem Alchemy deposit failed; retaining exact actual remainder", failure);
            } finally { delivering = target = null; attemptedAspect = null; gotoReady = false; }
        }
        @Override public void stop() { emptying = false; }
    }
    private final class Goto extends AlchemyGoal {
        @Override public boolean canUse() { return gotoReady && validTarget(); }
        private boolean validTarget() {
            return golem.alchemyReady() && golem.getEssentiaAmount() > 0 && currentDestination(target, golem.getEssentiaAspect(), 1);
        }
        @Override public boolean canContinueToUse() { return moving && pathCount > 0 && validTarget() && !golem.getNavigation().isDone(); }
        @Override public void start() {
            moving = true;
            gotoReady = false;
            origin = golem.blockPosition();
            nudged = false;
            pathCount = 200;
            if (!validTarget() || !move(target.getBlockPos())) stop();
        }
        private boolean move(BlockPos pos) {
            if (!validTarget() || !(golem.level() instanceof ServerLevel server) || !loaded(server, pos) || !golem.loadedNavigationArea(pos)) return false;
            Path path = golem.getNavigation().createPath(pos, 0); // Original integer jar XYZ; no 3x3/Y+1 approach.
            if (path == null || !moving || !validTarget()) return false;
            ownedPath = path;
            pathLevel = server;
            return golem.getNavigation().moveTo(path, 1.0) && moving && validTarget();
        }
        @Override public void tick() {
            if (!moving || !validTarget()) { stop(); return; }
            if (--pathCount > 0) return;
            if (!nudged && origin != null && origin.equals(golem.blockPosition()) && golem.loadedNavigationArea(golem.blockPosition())) {
                nudged = true;
                Vec3 nudge = DefaultRandomPos.getPos(golem, 2, 1);
                if (nudge != null && move(BlockPos.containing(nudge))) { pathCount = 20; return; }
            }
            stop();
        }
        @Override public void stop() { clearPath(); moving = gotoReady = false; target = null; pathCount = 0; }
    }
}

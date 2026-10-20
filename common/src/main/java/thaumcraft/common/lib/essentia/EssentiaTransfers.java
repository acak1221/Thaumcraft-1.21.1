package thaumcraft.common.lib.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemAspectCargo;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.relics.ItemResonator;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileEssentiaReservoir;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.lib.bore.BoreNativeContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.ArrayList;
import java.util.List;

/** One-unit native transfer. Pressure and minimum-suction quirks belong to the caller. */
public final class EssentiaTransfers {
    private static final ThreadLocal<Mutation> MUTATION = new ThreadLocal<>();

    private EssentiaTransfers() {}

    /** Read-only lifecycle fence for native stock restoration, not permission
     * to begin/nest a resource mutation. Retiring paid labels stay separate. */
    public static boolean isMutationActive() { return MUTATION.get() != null; }

    /** A nested retirement scope must not hide the held native worker mutation. */
    public static boolean isGolemMutationActive(EntityGolemBase golem) {
        if (golem == null) return false;
        for (Mutation mutation = MUTATION.get(); mutation != null; mutation = mutation.parent) {
            if (mutation.golem == golem) return true;
        }
        return false;
    }

    public enum BoreOperation { PREFILL, FINISH, EVACUATE, RECONCILE, REPAIR }
    public enum BoreOwnership { ARM, WITNESS, UNKNOWN, EVACUATE, DROP }

    /** Only this executor can create a scope. No callback/debit/output supplier. */
    public static final class BoreScope {
        private final TileArcaneBore top;
        private final BoreOperation operation;
        private BoreWork.Frame frame;
        private BoreScope(TileArcaneBore top, BoreOperation operation) { this.top = top; this.operation = operation; }
        public BoreOperation operation() { return operation; }
    }

    public static void workBore(TileArcaneBore top, BoreOperation operation) {
        if (top == null || operation == null) return;
        if (operation == BoreOperation.EVACUATE) BoreNativeContext.requireRetirement(top);
        else if (!BoreNativeContext.isCurrentOwner(top) || !BoreNativeContext.isNativeTick(top)) return;
        Mutation parent = MUTATION.get();
        if (parent != null && operation != BoreOperation.EVACUATE) return;
        for (Mutation ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor.raw || ancestor.boreScope != null && ancestor.receiver == top
                    && ancestor.boreScope.operation == BoreOperation.EVACUATE) return;
        }
        Mutation mutation = new Mutation(top, top);
        mutation.boreScope = new BoreScope(top, operation);
        MUTATION.set(mutation);
        try { BoreWork.run(top, mutation.boreScope); }
        finally { finishTubeFilterMutation(mutation); }
    }

    public static void requireBoreScope(BoreScope scope, TileArcaneBore top) {
        Mutation mutation = MUTATION.get();
        if (scope == null || mutation == null || mutation.boreScope != scope || scope.top != top || mutation.receiver != top)
            throw new IllegalStateException("Bore work requires this executor's exact scope");
    }
    public static void bindBoreFrame(BoreScope scope, TileArcaneBore top, BoreWork.Frame frame) {
        requireBoreScope(scope, top);
        if (scope.frame != null || frame == null) throw new IllegalStateException("Bore scope already claimed");
        scope.frame = frame;
    }
    public static BoreWork.Frame currentBoreFrame(TileArcaneBore top) {
        Mutation mutation = MUTATION.get();
        return mutation != null && mutation.boreScope != null && mutation.receiver == top ? mutation.boreScope.frame : null;
    }
    public static void requireBoreFrame(BoreWork.Frame frame) {
        if (frame == null) throw new IllegalStateException("Missing Bore frame");
        requireBoreScope(frame.scope(), frame.top());
        if (frame.scope().frame != frame) throw new IllegalStateException("Wrong Bore frame");
    }
    public static void lockBoreActor(BoreWork.Frame frame, BlockEntity actor) {
        requireBoreFrame(frame);
        if (MUTATION.get().raw || !BoreWork.mayLockActor(frame, actor)) throw new IllegalStateException("Bore actor lock outside exact native preparation");
        if (!MUTATION.get().additionalActors.contains(actor)) MUTATION.get().additionalActors.add(actor);
    }
    public static RuntimeException commitBore(BoreWork.Frame frame, BoreWork.Change change) {
        requireBoreFrame(frame);
        BoreWork.requirePrepared(frame, change);
        Mutation mutation = MUTATION.get();
        mutation.boreChange = change;
        mutation.boreEscrow = change.escrow();
        mutation.boreOwnership = change.ownership();
        var debit = change.sourceDebit();
        if (debit != null) {
            mutation.source = (BlockEntity) debit.owner();
            lockBoreActor(frame, mutation.source);
        }
        try {
            return commitPrepared(mutation, frame.top(), null, null, mutation.source,
                    debit == null ? null : debit.owner(), debit);
        } finally { mutation.raw = false; mutation.boreChange = null; mutation.boreOwnership = null; mutation.boreEscrow = null; }
    }
    public static void requireBoreRaw(BoreWork.Frame frame, BoreWork.Change change) {
        requireBoreFrame(frame);
        Mutation mutation = MUTATION.get();
        if (!mutation.raw || mutation.boreChange != change) throw new IllegalStateException("Wrong prepared Bore raw change");
    }
    public static void commitBoreRetention(BoreWork.Frame frame, BoreWork.RetirementRetention retention) {
        requireBoreFrame(frame);
        BoreWork.requireRetentionPrepared(frame, retention);
        Mutation mutation = MUTATION.get();
        if (mutation.raw || mutation.boreChange != null || mutation.boreRetention != null)
            throw new IllegalStateException("Nested Bore retention raw step");
        mutation.boreRetention = retention;
        try { commitPrepared(mutation, frame.top(), null, null, mutation.source, null, null); }
        finally { mutation.raw = false; mutation.boreRetention = null; }
    }
    public static void requireBoreRetentionRaw(BoreWork.Frame frame, BoreWork.RetirementRetention retention) {
        requireBoreFrame(frame);
        Mutation mutation = MUTATION.get();
        if (!mutation.raw || mutation.boreRetention != retention || retention == null)
            throw new IllegalStateException("Wrong opaque Bore retention raw scope");
    }
    public static void requireBoreEscrow(DeferredItemDrops.BoreEscrow escrow, BoreOwnership operation) {
        Mutation mutation = MUTATION.get();
        if (mutation == null || mutation.boreScope == null || mutation.boreScope.frame == null
                || !mutation.raw || mutation.boreEscrow != escrow || mutation.boreOwnership != operation)
            throw new IllegalStateException("Bore escrow requires its exact prepared ownership phase");
    }
    public static void requireBoreEscrowScope(DeferredItemDrops.BoreEscrow escrow) {
        Mutation mutation = MUTATION.get();
        if (mutation == null || mutation.boreScope == null || !BoreWork.ownsEscrow(mutation.boreScope.frame, escrow))
            throw new IllegalStateException("Wrong Bore escrow frame");
    }
    public static void routeBoreOutput(BoreWork.Frame frame, DeferredItemDrops.BoreEscrow escrow) {
        requireBoreFrame(frame);
        if (!BoreWork.ownsEscrow(frame, escrow)) throw new IllegalStateException("Unowned Bore route");
        Mutation mutation = MUTATION.get();
        mutation.boreEscrow = escrow; mutation.boreOwnership = BoreOwnership.DROP;
        try { commitPrepared(mutation, frame.top(), null, null, mutation.source, null, null); }
        finally { mutation.raw = false; mutation.boreEscrow = null; mutation.boreOwnership = null; }
    }

    /** TC4 ApiHelper:155-168 checks the NEIGHBOR's opposite side, not the origin's. Client-safe. */
    public static BlockEntity connectableNeighbor(BlockEntity origin, Direction face) {
        Level level = origin.getLevel();
        if (face == null || level == null || !current(origin, level)) return null;
        BlockEntity neighbor = neighbor(origin, face, level);
        if (!(neighbor instanceof IEssentiaTransport transport) || !transport.isConnectable(face.getOpposite())) return null;
        return current(origin, level) && current(neighbor, level) ? neighbor : null;
    }

    public static boolean isLocked(BlockEntity tile) {
        // Retirement can be required DURING another actor's notification. Its
        // nested cleanup must not temporarily unlock the already committed pair.
        for (Mutation mutation = MUTATION.get(); mutation != null; mutation = mutation.parent) {
            if (mutation.receiver == tile || mutation.source == tile || mutation.additionalActors.contains(tile)) return true;
        }
        return false;
    }

    public static void requireMutation(BlockEntity tile) {
        Mutation mutation = MUTATION.get();
        if (mutation == null || !mutation.raw || mutation.receiver != tile && mutation.source != tile) {
            throw new IllegalStateException("Raw essentia storage writes require the active transfer's raw phase");
        }
    }

    public static void requireAtomicChange(BlockEntity tile, EssentiaAtomicEndpoint.Change change) {
        requireMutation(tile);
        Mutation mutation = MUTATION.get();
        if (mutation.tubeFilter || mutation.tubeSides || change.owner() != tile
                || !(mutation.receiver == tile && mutation.receiverChange == change || mutation.source == tile && mutation.sourceChange == change)) {
            throw new IllegalStateException("Atomic stock writes require this executor's exact prepared change");
        }
    }

    /** Only the exact prepared native worker replacement can use this raw phase. */
    public static void requireGolemCargoChange(EntityGolemBase golem, GolemAspectCargo before, GolemAspectCargo after) {
        Mutation mutation = MUTATION.get();
        if (golem == null || before == null || after == null || mutation == null || !mutation.raw
                || mutation.golem != golem || mutation.beforeCargo != before || mutation.afterCargo != after) {
            throw new IllegalStateException("Golem cargo writes require this executor's exact prepared raw change");
        }
    }

    public static void requireTubeSideMutation(BlockEntity tile) {
        requireMutation(tile);
        if (!MUTATION.get().tubeSides) throw new IllegalStateException("Raw tube sides require the paired-side mutation");
    }

    public enum CrystalOwnership { PUBLISH, INVENTORY, DROP }
    public static void requireCrystalEscrowScope(DeferredItemDrops.CrystalEscrow escrow) {
        Mutation mutation = MUTATION.get();
        if (mutation == null || mutation.crystalEscrow != escrow || !(mutation.receiver instanceof TileEssentiaCrystalizer)
                || mutation.tubeFilter || mutation.tubeSides) throw new IllegalStateException("Paid crystal escrow requires its exact native completion scope");
    }
    public static void requireCrystalEscrowMutation(DeferredItemDrops.CrystalEscrow escrow, CrystalOwnership operation) {
        requireCrystalEscrowScope(escrow);
        if (!MUTATION.get().raw || MUTATION.get().crystalOwnership != operation) throw new IllegalStateException("Crystal ownership requires this prepared raw phase");
    }
    public static void requireCrystalInventoryMutation(BlockEntity owner, CrystalInventoryTarget.Plan plan) {
        Mutation mutation = MUTATION.get();
        if (mutation == null || !mutation.raw || mutation.crystalOwnership != CrystalOwnership.INVENTORY || mutation.inventoryPlan != plan
                || mutation.crystalEscrow == null || !mutation.additionalActors.contains(owner) || !plan.owns(owner)) throw new IllegalStateException("Native inventory writes require the executor's exact paid crystal plan");
    }

    /** A storage transfer's raw scope does not authorize unpaid filter writes. */
    public static void requireTubeFilterMutation(TileTube tube) {
        requireMutation(tube);
        if (!MUTATION.get().tubeFilter || !tube.isFilter()) {
            throw new IllegalStateException("Tube filter writes require a paid label or retiring-owner mutation");
        }
    }

    /** Compatibility preflight retains the public sided query used by existing
     * addon/adversarial fixtures. Production profiles below use original query
     * sites instead. ALL profiles delegate to the SAME raw accounting executor. */
    public static int pullOne(BlockEntity receiver, Direction inputFace, Aspect wanted) {
        if (!EssentiaAtomicEndpoint.validAspect(wanted)) return 0;
        Attempt result = transport(receiver, inputFace, Profile.COMPAT, wanted);
        if (result.failure() != null) throw result.failure();
        return result.committedUnits();
    }

    public record Attempt(boolean selected, int committedUnits, RuntimeException failure) {}
    private enum Profile { COMPAT, TUBE, JAR, BUFFER, CENTRIFUGE, CRYSTALLIZER, RESERVOIR, LAMP }

    public static Attempt pullTubeOne(TileTube tube, Direction inputFace) { return transport(tube, inputFace, Profile.TUBE, null); }
    public static Attempt pullJarOne(TileJar jar) { return transport(jar, Direction.UP, Profile.JAR, null); }
    public static Attempt pullBufferOne(TileTubeBuffer buffer, Direction inputFace) { return transport(buffer, inputFace, Profile.BUFFER, null); }
    public static Attempt pullCentrifugeOne(TileCentrifuge centrifuge) { return transport(centrifuge, Direction.DOWN, Profile.CENTRIFUGE, null); }
    public static Attempt pullCrystallizerOne(TileEssentiaCrystalizer tile) { return transport(tile, tile.getFacing(), Profile.CRYSTALLIZER, null); }
    public static Attempt pullReservoirOne(TileEssentiaReservoir tile) { return transport(tile, tile.getFacing(), Profile.RESERVOIR, null); }
    /** Closed native lamp draw. No general transport callback or arbitrary subclass can acquire this profile. */
    public static Attempt pullLampOne(thaumcraft.common.tiles.TileEssentiaLamp tile) {
        if (!(tile instanceof thaumcraft.common.tiles.TileArcaneLampGrowth || tile instanceof thaumcraft.common.tiles.TileArcaneLampFertility)
                || !tile.isNativeDrawPhase()) return new Attempt(false, 0, null);
        return transport(tile, tile.getFacing(), Profile.LAMP, tile.fuelAspect());
    }

    /** Bulk native alembic debit paired with the actual private Alchemy job's cargo credit. */
    public static Attempt collectAlembic(EntityGolemBase golem, TileAlembic source, Direction output, Aspect aspect, int limit) {
        if (golem == null || source == null || output != Direction.UP || limit <= 0
                || !EssentiaAtomicEndpoint.validAspect(aspect) || !(golem.level() instanceof ServerLevel level)) return new Attempt(false, 0, null);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Golem essentia collection requires the server thread");
        if (MUTATION.get() != null || !currentGolem(golem, level) || !currentGolemAlembic(source, level, output)) return new Attempt(false, 0, null);
        Mutation mutation = new Mutation(source, source);
        mutation.golem = golem;
        MUTATION.set(mutation);
        boolean selected = false;
        try {
            GolemAspectCargo beforeCargo = golem.getAspectCargo();
            int capacity = golem.getCarryLimit();
            var before = source.snapshotAtomicState();
            if (!matchesGolemCargo(golem, level, beforeCargo, capacity)
                    || beforeCargo.amount() > 0 && beforeCargo.aspect() != aspect
                    || !(before instanceof EssentiaStorageEndpoint.SingletonState stock) || !stock.valid()
                    || stock.overflow() || stock.type() != aspect) return new Attempt(false, 0, null);
            int units = Math.min(limit, Math.min(stock.amount(), capacity - beforeCargo.amount()));
            if (units <= 0 || !golem.canCollectEssentia(source, aspect, units)) return new Attempt(false, 0, null);
            selected = true;
            if (!matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemAlembic(source, level, output)
                    || !source.matchesAtomicState(before) || !source.preflightTake(aspect, output)) return new Attempt(true, 0, null);
            // Arbitration can call out. Neither the old singleton snapshot nor UP
            // proves the current home-facing/job permission after that callback.
            if (!matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemAlembic(source, level, output)
                    || !source.matchesAtomicState(before) || !golem.canCollectEssentia(source, aspect, units)
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemAlembic(source, level, output)
                    || !source.matchesAtomicState(before)) return new Attempt(true, 0, null);
            GolemAspectCargo afterCargo = new GolemAspectCargo(aspect, beforeCargo.amount() + units);
            if (afterCargo.amount() > capacity) return new Attempt(true, 0, null);
            var debit = source.prepareExtract(before, aspect, units);
            if (!prepared(debit, source, before, units) || !debit.changed()
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemAlembic(source, level, output)
                    || !source.matchesAtomicState(before) || !golem.canCollectEssentia(source, aspect, units)
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemAlembic(source, level, output)
                    || !source.matchesAtomicState(before)) return new Attempt(true, 0, null);
            mutation.beforeCargo = beforeCargo;
            mutation.afterCargo = afterCargo;
            RuntimeException failure = commitPrepared(mutation, source, null, null, source, source, debit);
            return new Attempt(true, units, failure);
        } catch (RuntimeException failure) {
            // Native raw application is callback-free after full preparation.
            // A violated exact-change invariant is a bug, not an unpaid retry.
            if (mutation.raw) throw failure;
            return new Attempt(selected, 0, failure);
        } finally { finishTubeFilterMutation(mutation); }
    }

    /** Bulk actual cargo debit into one normal native jar; no overflow/transport profile. */
    public static Attempt deliverNormalJar(EntityGolemBase golem, TileJar sink, Aspect aspect, int limit) {
        if (golem == null || sink == null || limit <= 0 || !EssentiaAtomicEndpoint.validAspect(aspect)
                || !(golem.level() instanceof ServerLevel level)) return new Attempt(false, 0, null);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Golem essentia delivery requires the server thread");
        if (MUTATION.get() != null || !currentGolem(golem, level) || !currentGolemJar(sink, level, aspect)) return new Attempt(false, 0, null);
        Mutation mutation = new Mutation(sink, sink);
        mutation.golem = golem;
        MUTATION.set(mutation);
        boolean selected = false;
        try {
            GolemAspectCargo beforeCargo = golem.getAspectCargo();
            int capacity = golem.getCarryLimit();
            var before = sink.snapshotAtomicState();
            if (!matchesGolemCargo(golem, level, beforeCargo, capacity) || beforeCargo.aspect() != aspect
                    || !(before instanceof EssentiaStorageEndpoint.SingletonState stock) || !stock.valid() || stock.overflow()
                    || stock.amount() > 0 && stock.type() != aspect) return new Attempt(false, 0, null);
            int units = Math.min(limit, Math.min(beforeCargo.amount(), stock.capacity() - stock.amount()));
            if (units <= 0 || !golem.canDeliverEssentia(sink, aspect, units)) return new Attempt(false, 0, null);
            selected = true;
            // Jar labels are not in SingletonState and prepareInsert ignores them.
            if (!matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemJar(sink, level, aspect)
                    || !sink.matchesAtomicState(before)) return new Attempt(true, 0, null);
            int remaining = beforeCargo.amount() - units;
            GolemAspectCargo afterCargo = remaining == 0 ? GolemAspectCargo.EMPTY : new GolemAspectCargo(aspect, remaining);
            if (afterCargo.amount() > capacity) return new Attempt(true, 0, null);
            var credit = sink.prepareInsert(before, aspect, units);
            if (!prepared(credit, sink, before, units) || !credit.changed()
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemJar(sink, level, aspect)
                    || !sink.matchesAtomicState(before) || !golem.canDeliverEssentia(sink, aspect, units)
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentGolemJar(sink, level, aspect)
                    || !sink.matchesAtomicState(before)) return new Attempt(true, 0, null);
            mutation.beforeCargo = beforeCargo;
            mutation.afterCargo = afterCargo;
            RuntimeException failure = commitPrepared(mutation, sink, sink, credit, sink, null, null);
            return new Attempt(true, units, failure);
        } catch (RuntimeException failure) {
            if (mutation.raw) throw failure;
            return new Attempt(selected, 0, failure);
        } finally { finishTubeFilterMutation(mutation); }
    }

    /** Original reservoir branches: first-aspect collection and partial delivery, paired with actual cargo in one native commit. */
    public static Attempt collectReservoir(EntityGolemBase golem, TileEssentiaReservoir reservoir, Aspect aspect, int limit) {
        return transferGolemReservoir(golem, reservoir, aspect, limit, true);
    }

    public static Attempt deliverReservoir(EntityGolemBase golem, TileEssentiaReservoir reservoir, Aspect aspect, int limit) {
        return transferGolemReservoir(golem, reservoir, aspect, limit, false);
    }

    private static Attempt transferGolemReservoir(EntityGolemBase golem, TileEssentiaReservoir reservoir, Aspect aspect, int limit, boolean collect) {
        if (golem == null || reservoir == null || limit <= 0 || !EssentiaAtomicEndpoint.validAspect(aspect)
                || !(golem.level() instanceof ServerLevel level)) return new Attempt(false, 0, null);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Golem reservoir transfer requires the server thread");
        if (MUTATION.get() != null || !currentGolem(golem, level) || !currentReservoir(reservoir, level)) return new Attempt(false, 0, null);
        Mutation mutation = new Mutation(reservoir, reservoir);
        mutation.golem = golem;
        MUTATION.set(mutation);
        boolean selected = false;
        try {
            GolemAspectCargo beforeCargo = golem.getAspectCargo();
            int capacity = golem.getCarryLimit();
            var before = reservoir.snapshotAtomicState();
            if (!matchesGolemCargo(golem, level, beforeCargo, capacity)
                    || beforeCargo.amount() > 0 && beforeCargo.aspect() != aspect
                    || collect && reservoir.getEssentiaType(null) != aspect) return new Attempt(false, 0, null);
            int units = Math.min(limit, collect
                    ? Math.min(reservoir.containerContains(aspect), capacity - beforeCargo.amount())
                    : Math.min(beforeCargo.amount(), reservoir.getSpace()));
            if (units <= 0 || !(collect ? golem.canCollectEssentia(reservoir, aspect, units)
                    : golem.canDeliverEssentia(reservoir, aspect, units))) return new Attempt(false, 0, null);
            selected = true;
            var change = collect ? reservoir.prepareExtract(before, aspect, units) : reservoir.prepareInsert(before, aspect, units);
            if (!prepared(change, reservoir, before, units) || !change.changed()
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentReservoir(reservoir, level)
                    || !reservoir.matchesAtomicState(before)
                    || !(collect ? golem.canCollectEssentia(reservoir, aspect, units) : golem.canDeliverEssentia(reservoir, aspect, units))
                    || !matchesGolemCargo(golem, level, beforeCargo, capacity) || !currentReservoir(reservoir, level)
                    || !reservoir.matchesAtomicState(before)) return new Attempt(true, 0, null);
            int after = beforeCargo.amount() + (collect ? units : -units);
            mutation.beforeCargo = beforeCargo;
            mutation.afterCargo = after == 0 ? GolemAspectCargo.EMPTY : new GolemAspectCargo(aspect, after);
            RuntimeException failure = collect
                    ? commitPrepared(mutation, reservoir, null, null, reservoir, reservoir, change)
                    : commitPrepared(mutation, reservoir, reservoir, change, reservoir, null, null);
            return new Attempt(true, units, failure);
        } catch (RuntimeException failure) {
            if (mutation.raw) throw failure;
            return new Attempt(selected, 0, failure);
        } finally { finishTubeFilterMutation(mutation); }
    }

    private static boolean currentReservoir(TileEssentiaReservoir reservoir, ServerLevel level) {
        return current(reservoir, level) && reservoir.getType().isValid(level.getBlockState(reservoir.getBlockPos()))
                && !reservoir.isQuarantined();
    }

    private static boolean currentGolem(EntityGolemBase golem, ServerLevel level) {
        return golem.getClass() == EntityGolemBase.class && golem.level() == level && golem.isAlive() && !golem.isRemoved()
                && level.hasChunkAt(golem.blockPosition()) && level.getEntity(golem.getUUID()) == golem && level.getEntity(golem.getId()) == golem;
    }

    private static boolean matchesGolemCargo(EntityGolemBase golem, ServerLevel level, GolemAspectCargo before, int capacity) {
        return currentGolem(golem, level) && before != null && capacity > 0 && capacity <= GolemAspectCargo.MAX_AMOUNT && before.amount() <= capacity
                && golem.getCarryLimit() == capacity && before.equals(golem.getAspectCargo());
    }

    private static boolean currentGolemAlembic(TileAlembic source, ServerLevel level, Direction output) {
        return output == Direction.UP && current(source, level) && source.getBlockState().getBlock().getClass() == BlockAlembic.class
                && source.canOutputTo(output);
    }

    private static boolean currentGolemJar(TileJar sink, ServerLevel level, Aspect aspect) {
        return current(sink, level) && sink.getBlockState().getBlock().getClass() == BlockJar.class && !sink.isVoid()
                && (sink.getFilter() == null || sink.getFilter() == aspect);
    }

    private static Attempt transport(BlockEntity receiver, Direction inputFace, Profile profile, Aspect requested) {
        if (inputFace == null || !(receiver.getLevel() instanceof ServerLevel level)) return new Attempt(false, 0, null);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Essentia transfers require the server thread");
        if (MUTATION.get() != null || !(receiver instanceof EssentiaAtomicEndpoint into) || !current(receiver, level)) return new Attempt(false, 0, null);
        if (profile == Profile.CENTRIFUGE && !((TileCentrifuge) receiver).isNativeDrawPhase()) return new Attempt(false, 0, null);
        if (profile == Profile.CRYSTALLIZER && !((TileEssentiaCrystalizer) receiver).isNativeFillPhase()) return new Attempt(false, 0, null);
        if (profile == Profile.LAMP && (!(receiver instanceof thaumcraft.common.tiles.TileArcaneLampGrowth || receiver instanceof thaumcraft.common.tiles.TileArcaneLampFertility)
                || !((thaumcraft.common.tiles.TileEssentiaLamp) receiver).isNativeDrawPhase())) return new Attempt(false, 0, null);
        BlockEntity source = neighbor(receiver, inputFace, level);
        if (!(source instanceof IEssentiaTransport from) || source == receiver) return new Attempt(false, 0, null);
        EssentiaAtomicEndpoint stockFrom = source instanceof EssentiaAtomicEndpoint endpoint ? endpoint : null;
        if (profile == Profile.COMPAT && stockFrom == null) return new Attempt(false, 0, null);
        Mutation mutation = new Mutation(receiver, source);
        MUTATION.set(mutation);
        boolean selected = false;
        try {
            var beforeInto = into.snapshotAtomicState(); var beforeFrom = stockFrom == null ? null : stockFrom.snapshotAtomicState();
            Direction output = inputFace.getOpposite();
            if (!from.isConnectable(output) || profile != Profile.BUFFER && (!into.isConnectable(inputFace) || !into.canInputFrom(inputFace))) return new Attempt(false, 0, null);
            Aspect wanted = requested;
            switch (profile) {
                case LAMP -> {
                    // Original growth/fertility lamps ask ONLY output and strictly lower pressure, then take fixed herba/victus.
                    // No representative mixed-store type, stock-count or minimum-suction query belongs to this policy.
                    if (!from.canOutputTo(output) || from.getSuctionAmount(output) >= into.getSuctionAmount(inputFace)) return new Attempt(false, 0, null);
                }
                case COMPAT -> {
                    if (stockFrom.prepareExtract(beforeFrom, wanted, 1) == null || into.prepareInsert(beforeInto, wanted, 1) == null) return new Attempt(false, 0, null);
                    if (!from.canOutputTo(output)) return new Attempt(false, 0, null);
                    if (stockFrom instanceof EssentiaStorageEndpoint) {
                        Aspect offered = from.getEssentiaType(output);
                        if (offered != null && offered != wanted || from.getEssentiaAmount(output) < 1) return new Attempt(false, 0, null);
                    }
                    // A mixed store has no representative singleton identity.
                    // Its prepared per-aspect debit above is the stock proof.
                }
                case TUBE -> {
                    if (!from.canOutputTo(output)) return new Attempt(false, 0, null);
                    Aspect suctionType = into.getSuctionType(null);
                    // Original short circuit deliberately makes TWO random
                    // buffer queries on a typed mismatch, one on a match.
                    if (suctionType != null && suctionType != from.getEssentiaType(output) && from.getEssentiaType(output) != null
                            || into.getSuctionAmount(null) <= from.getSuctionAmount(output)
                            || into.getSuctionAmount(null) < from.getMinimumSuction()) return new Attempt(false, 0, null);
                    wanted = suctionType;
                    if (wanted == null) wanted = from.getEssentiaType(output);
                    if (wanted == null) wanted = from.getEssentiaType(null);
                }
                case JAR -> {
                    if (!from.canOutputTo(output)) return new Attempt(false, 0, null);
                    TileJar jar = (TileJar) receiver;
                    if (jar.getFilter() != null) wanted = jar.getFilter();
                    else if (jar.getAspect() != null && jar.getAmount() > 0) wanted = jar.getAspect();
                    else if (from.getEssentiaAmount(output) > 0 && from.getSuctionAmount(output) < jar.getSuctionAmount(inputFace)
                            && jar.getSuctionAmount(inputFace) >= from.getMinimumSuction()) wanted = from.getEssentiaType(output);
                    if (wanted == null || from.getSuctionAmount(output) >= jar.getSuctionAmount(inputFace)) return new Attempt(false, 0, null);
                }
                case BUFFER -> {
                    if (from.getEssentiaAmount(output) <= 0 || from.getSuctionAmount(output) >= into.getSuctionAmount(inputFace)
                            || into.getSuctionAmount(inputFace) < from.getMinimumSuction()) return new Attempt(false, 0, null);
                    // The first eligible candidate STOPS this caller even if
                    // type query, source port, arbitration or notification fails.
                    selected = true;
                    wanted = from.getEssentiaType(output);
                }
                case CENTRIFUGE -> {
                    if (!from.canOutputTo(output) || from.getEssentiaAmount(output) <= 0
                            || from.getSuctionAmount(output) >= into.getSuctionAmount(inputFace)
                            || into.getSuctionAmount(inputFace) < from.getMinimumSuction()) return new Attempt(false, 0, null);
                    selected = true;
                    // Original TileCentrifuge:225-235 selects exactly ONCE,
                    // refuses a selected primal, then repeats only pressure.
                    // In particular a mixed buffer is never resampled here.
                    wanted = from.getEssentiaType(output);
                    if (!EssentiaAtomicEndpoint.validAspect(wanted) || wanted.isPrimal()
                            || from.getSuctionAmount(output) >= into.getSuctionAmount(inputFace)) return new Attempt(true, 0, null);
                }
                // Original TileEssentiaReservoir.fillReservoir asks the same questions as the crystallizer's draw: output side, stock,
                // pressure and minimum suction, ONE type query, then the pressure again; any aspect, primal or compound, is taken.
                case CRYSTALLIZER, RESERVOIR -> {
                    if (!from.canOutputTo(output) || from.getEssentiaAmount(output) <= 0
                            || from.getSuctionAmount(output) >= into.getSuctionAmount(inputFace)
                            || into.getSuctionAmount(inputFace) < from.getMinimumSuction()) return new Attempt(false, 0, null);
                    selected = true;
                    wanted = from.getEssentiaType(output); // Original one query; all real primal/compound inputs accepted.
                    if (!EssentiaAtomicEndpoint.validAspect(wanted) || from.getSuctionAmount(output) >= into.getSuctionAmount(inputFace)) return new Attempt(true, 0, null);
                }
            }
            selected = true;
            // Unsupported transports are never debited via callback take/add,
            // but an eligible buffer candidate still terminates its scan.
            if (!EssentiaAtomicEndpoint.validAspect(wanted) || !from.canOutputTo(output) || stockFrom == null || !stockFrom.preflightTake(wanted, output)) return new Attempt(true, 0, null);
            var debit = stockFrom.prepareExtract(beforeFrom, wanted, 1);
            var credit = into.prepareInsert(beforeInto, wanted, 1);
            if (!prepared(debit, stockFrom, beforeFrom, 1) || !prepared(credit, into, beforeInto, 1)) return new Attempt(true, 0, null);
            if (!from.isConnectable(output) || !from.canOutputTo(output)
                    || profile != Profile.BUFFER && (!into.isConnectable(inputFace) || !into.canInputFrom(inputFace))
                    || !current(receiver, level) || !current(source, level)
                    || level.getBlockEntity(receiver.getBlockPos().relative(inputFace)) != source
                    || !into.matchesAtomicState(beforeInto) || !stockFrom.matchesAtomicState(beforeFrom)) return new Attempt(true, 0, null);
            RuntimeException failure = commitPrepared(mutation, receiver, into, credit, source, stockFrom, debit);
            return new Attempt(true, 1, failure);
        } catch (RuntimeException failure) {
            return new Attempt(selected, 0, failure);
        } finally { finishTubeFilterMutation(mutation); }
    }

    private static boolean prepared(EssentiaAtomicEndpoint.Change change, EssentiaAtomicEndpoint owner, EssentiaAtomicEndpoint.State before, int units) {
        return change != null && change.owner() == owner && change.before() == before && change.units() == units;
    }

    /** The single raw field/notification executor, for paired and local stock changes. */
    private static RuntimeException commitPrepared(Mutation mutation, BlockEntity receiver, EssentiaAtomicEndpoint into, EssentiaAtomicEndpoint.Change credit,
                                                     BlockEntity source, EssentiaAtomicEndpoint from, EssentiaAtomicEndpoint.Change debit) {
        return commitPrepared(mutation, receiver, into, credit, source, from, debit, null, null);
    }

    /** Same raw executor, with only CLOSED paid-output ownership transitions.
     * No arbitrary callbacks/plans can be supplied through any public entry. */
    private static RuntimeException commitPrepared(Mutation mutation, BlockEntity receiver, EssentiaAtomicEndpoint into, EssentiaAtomicEndpoint.Change credit,
                                                     BlockEntity source, EssentiaAtomicEndpoint from, EssentiaAtomicEndpoint.Change debit,
                                                     CrystalOwnership ownership, CrystalInventoryTarget.Plan inventory) {
        mutation.receiverChange = credit; mutation.sourceChange = debit;
        mutation.crystalOwnership = ownership; mutation.inventoryPlan = inventory;
        mutation.raw = true;
        if (debit != null && debit.changed()) from.applyAtomicRaw(debit);
        if (credit != null && credit.changed()) into.applyAtomicRaw(credit);
        if (mutation.golem != null) mutation.golem.applyAspectCargoRaw(mutation.beforeCargo, mutation.afterCargo);
        if (ownership != null) switch (ownership) {
            case PUBLISH -> mutation.crystalEscrow.publishRaw();
            case INVENTORY -> { inventory.applyRaw(); mutation.crystalEscrow.inventoryRaw(); }
            case DROP -> mutation.crystalEscrow.dropRaw();
        }
        if (mutation.boreRetention != null) BoreWork.applyRetentionRaw(mutation.boreScope.frame, mutation.boreRetention);
        else if (mutation.boreChange != null) BoreWork.applyPreparedRaw(mutation.boreScope.frame, mutation.boreChange);
        else if (mutation.boreOwnership == BoreOwnership.DROP) mutation.boreEscrow.dropRaw();
        mutation.raw = false;
        RuntimeException failure = null;
        try {
            if (debit != null && debit.changed()) from.notifyEssentiaStorageChanged();
        } catch (RuntimeException exception) { failure = exception; }
        finally {
            try { if (credit != null && credit.changed()) into.notifyEssentiaStorageChanged(); }
            catch (RuntimeException exception) { if (failure == null) failure = exception; else if (failure != exception) failure.addSuppressed(exception); }
        }
        if (inventory != null) {
            try { inventory.notifyChanged(); }
            catch (RuntimeException exception) { failure = combine(failure, exception); }
        }
        if (mutation.golem != null) {
            try { mutation.golem.notifyGolemEssentiaChanged(); }
            catch (RuntimeException exception) { failure = combine(failure, exception); }
        }
        return failure;
    }

    private static RuntimeException combine(RuntimeException first, RuntimeException next) {
        if (first == null) return next;
        if (next != null && next != first) first.addSuppressed(next);
        return first;
    }

    /** Local native buffer APIs share the same lock/preparation/commit machinery. */
    public static int changeBuffer(TileTubeBuffer buffer, Aspect aspect, int amount, boolean insert, Direction outputFace, boolean exact) {
        if (!EssentiaAtomicEndpoint.validAspect(aspect) || amount <= 0 || insert && amount != 1 || !(buffer.getLevel() instanceof ServerLevel level)) return 0;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Buffer changes require the server thread");
        Attempt attempt = changeLocal(buffer, new LocalRequest(insert ? LocalOperation.BUFFER_INSERT : LocalOperation.BUFFER_EXTRACT, aspect, amount, outputFace, exact));
        if (attempt.failure() != null) {
            // Preserve the accepted buffer wrapper's pre-commit throw vs
            // post-commit log-and-return behavior after factoring its scope.
            if (attempt.committedUnits() == 0) throw attempt.failure();
            thaumcraft.common.Thaumcraft.LOG.warn("Committed buffer change notification failed at {}", buffer.getBlockPos(), attempt.failure());
        }
        return attempt.committedUnits();
    }

    /** The reservoir's own container calls: units that moved (an insert takes what fits, an extract is all or nothing). */
    public static int changeReservoir(TileEssentiaReservoir reservoir, Aspect aspect, int amount, boolean insert) {
        if (!EssentiaAtomicEndpoint.validAspect(aspect) || amount <= 0 || !(reservoir.getLevel() instanceof ServerLevel level)) return 0;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Reservoir changes require the server thread");
        Attempt attempt = changeLocal(reservoir, new LocalRequest(insert ? LocalOperation.RESERVOIR_INSERT : LocalOperation.RESERVOIR_EXTRACT, aspect, amount, null, true));
        if (attempt.failure() != null) {
            if (attempt.committedUnits() == 0) throw attempt.failure();
            thaumcraft.common.Thaumcraft.LOG.warn("Committed reservoir change notification failed at {}", reservoir.getBlockPos(), attempt.failure());
        }
        return attempt.committedUnits();
    }

    /** Bounded native raw facades, not a public arbitrary credit-plan capability.
     * The transport/container facade owns side/exact-quantity distinctions. */
    public enum CentrifugeMutation { INPUT, OUTPUT_INSERT, OUTPUT_EXTRACT }

    public static Attempt changeCentrifuge(TileCentrifuge centrifuge, CentrifugeMutation operation, Aspect aspect, int amount) {
        if (operation == null || !EssentiaAtomicEndpoint.validAspect(aspect) || amount <= 0) return new Attempt(false, 0, null);
        LocalOperation local = switch (operation) {
            case INPUT -> LocalOperation.CENTRIFUGE_INPUT;
            case OUTPUT_INSERT -> LocalOperation.CENTRIFUGE_OUTPUT_INSERT;
            case OUTPUT_EXTRACT -> LocalOperation.CENTRIFUGE_OUTPUT_EXTRACT;
        };
        return changeLocal(centrifuge, new LocalRequest(local, aspect, amount, null, false));
    }

    /** No result aspect/index/plan parameter. The tile's private actual native
     * completion phase is mandatory; this method alone cannot force conversion. */
    public static Attempt completeCentrifuge(TileCentrifuge centrifuge) {
        return changeLocal(centrifuge, new LocalRequest(LocalOperation.CENTRIFUGE_COMPLETE, null, 1, null, false));
    }

    public static Attempt changeCrystallizer(TileEssentiaCrystalizer tile, Aspect aspect, int amount, boolean insert) {
        if (!EssentiaAtomicEndpoint.validAspect(aspect) || amount <= 0 || !insert && amount != 1) return new Attempt(false, 0, null);
        return changeLocal(tile, new LocalRequest(insert ? LocalOperation.CRYSTALLIZER_INSERT : LocalOperation.CRYSTALLIZER_EXTRACT, aspect, amount, null, true));
    }

    /** One paid input -> one typed, level-owned output BEFORE any inventory
     * callbacks. Native-tick permission is private to the actual machine. */
    public static Attempt finishCrystallizer(TileEssentiaCrystalizer tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) return new Attempt(false, 0, null);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Crystal completion requires the server thread");
        if (MUTATION.get() != null || !current(tile, level) || tile.isQuarantined()) return new Attempt(false, 0, null);
        Mutation mutation = new Mutation(tile, tile); MUTATION.set(mutation);
        DeferredItemDrops.CrystalEscrow escrow = null;
        boolean paid = false;
        try {
            var before = tile.snapshotAtomicState(); Aspect aspect = tile.getNativeCompletionAspect(before);
            if (aspect == null) return new Attempt(false, 0, null);
            Direction facing = tile.getFacing();
            ItemStack output = ItemCrystalEssence.create(aspect);
            escrow = DeferredItemDrops.prepareCrystal(level, tile.getBlockPos(), facing, output);
            var debit = tile.prepareCompletion(before);
            if (!prepared(debit, tile, before, 1) || !current(tile, level) || !tile.matchesAtomicState(before)) return new Attempt(false, 0, null);
            mutation.crystalEscrow = escrow;
            RuntimeException failure = commitPrepared(mutation, tile, tile, debit, tile, tile, null, CrystalOwnership.PUBLISH, null);
            paid = true;
            boolean residual = false;
            try {
                CrystalInventoryTarget.Group group = current(tile, level) ? CrystalInventoryTarget.discover(level, tile.getBlockPos().relative(facing.getOpposite())) : null;
                if (group != null) {
                    mutation.additionalActors.addAll(group.actors());
                    CrystalInventoryTarget.Plan plan = group.prepare(output, facing);
                    if (plan != null && current(tile, level) && plan.matches()) failure = combine(failure, commitPrepared(mutation, tile, null, null, tile, null, null, CrystalOwnership.INVENTORY, plan));
                }
            } catch (RuntimeException exception) { failure = combine(failure, exception); }
            finally {
                if (escrow.needsRoute()) {
                    commitPrepared(mutation, tile, null, null, tile, null, null, CrystalOwnership.DROP, null);
                    residual = true;
                }
            }
            // Event only for residual physical ejection, fizz for BOTH routes.
            // Retried ADD never repeats these effects or their world-RNG draws.
            try { if (residual && current(tile, level)) level.blockEvent(tile.getBlockPos(), tile.getBlockState().getBlock(), 0, 0); }
            finally {
                level.playSound(null, tile.getBlockPos().getX() + .5, tile.getBlockPos().getY() + .5, tile.getBlockPos().getZ() + .5,
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .25F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * .8F);
            }
            return new Attempt(true, 1, failure);
        } catch (RuntimeException failure) { return new Attempt(true, paid ? 1 : 0, failure); }
        finally {
            try {
                if (escrow != null && mutation.crystalEscrow == escrow) {
                    if (escrow.needsRoute()) commitPrepared(mutation, tile, null, null, tile, null, null, CrystalOwnership.DROP, null);
                    escrow.finishFrame();
                }
            } finally { finishTubeFilterMutation(mutation); }
        }
    }

    private enum LocalOperation { BUFFER_INSERT, BUFFER_EXTRACT, CENTRIFUGE_INPUT, CENTRIFUGE_OUTPUT_INSERT, CENTRIFUGE_OUTPUT_EXTRACT, CENTRIFUGE_COMPLETE, CRYSTALLIZER_INSERT, CRYSTALLIZER_EXTRACT, RESERVOIR_INSERT, RESERVOIR_EXTRACT }
    private record LocalRequest(LocalOperation operation, Aspect aspect, int amount, Direction outputFace, boolean exact) {}

    /** One local scope for accepted buffers and the separate two-slot machine.
     * Only these private requests can reach the SAME prepared raw executor. */
    private static Attempt changeLocal(BlockEntity owner, LocalRequest request) {
        if (!(owner.getLevel() instanceof ServerLevel level)) return new Attempt(false, 0, null);
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Native essentia changes require the server thread");
        if (MUTATION.get() != null || !current(owner, level)) return new Attempt(false, 0, null);
        boolean bufferOperation = request.operation() == LocalOperation.BUFFER_INSERT || request.operation() == LocalOperation.BUFFER_EXTRACT;
        if (bufferOperation) {
            if (!(owner instanceof TileTubeBuffer buffer) || buffer.isQuarantined()) return new Attempt(false, 0, null);
        } else if (request.operation() == LocalOperation.CRYSTALLIZER_INSERT || request.operation() == LocalOperation.CRYSTALLIZER_EXTRACT) {
            if (!(owner instanceof TileEssentiaCrystalizer tile) || tile.isQuarantined()) return new Attempt(false, 0, null);
        } else if (request.operation() == LocalOperation.RESERVOIR_INSERT || request.operation() == LocalOperation.RESERVOIR_EXTRACT) {
            if (!(owner instanceof TileEssentiaReservoir tile) || tile.isQuarantined()) return new Attempt(false, 0, null);
        } else if (!(owner instanceof TileCentrifuge centrifuge) || centrifuge.isQuarantined()) return new Attempt(false, 0, null);
        EssentiaAtomicEndpoint endpoint = (EssentiaAtomicEndpoint) owner;
        Mutation mutation = new Mutation(owner, owner); MUTATION.set(mutation);
        boolean selected = false;
        try {
            var before = endpoint.snapshotAtomicState();
            if (request.outputFace() != null && (!endpoint.canOutputTo(request.outputFace()) || !endpoint.preflightTake(request.aspect(), request.outputFace()))) return new Attempt(false, 0, null);
            int units = 1;
            EssentiaAtomicEndpoint.Change change;
            switch (request.operation()) {
                case BUFFER_INSERT, BUFFER_EXTRACT -> {
                    TileTubeBuffer buffer = (TileTubeBuffer) owner;
                    units = request.operation() == LocalOperation.BUFFER_INSERT || request.exact() ? request.amount() : Math.min(request.amount(), buffer.containerContains(request.aspect()));
                    if (units <= 0) return new Attempt(false, 0, null);
                    change = request.operation() == LocalOperation.BUFFER_INSERT ? buffer.prepareInsert(before, request.aspect(), units) : buffer.prepareExtract(before, request.aspect(), units);
                }
                case RESERVOIR_INSERT, RESERVOIR_EXTRACT -> {
                    TileEssentiaReservoir reservoir = (TileEssentiaReservoir) owner;
                    // Original addToContainer takes what fits and returns the rest; takeFromContainer is all or nothing.
                    boolean insert = request.operation() == LocalOperation.RESERVOIR_INSERT;
                    units = insert ? Math.min(request.amount(), reservoir.getSpace()) : request.amount();
                    if (units <= 0) return new Attempt(false, 0, null);
                    change = insert ? reservoir.prepareInsert(before, request.aspect(), units) : reservoir.prepareExtract(before, request.aspect(), units);
                }
                case CENTRIFUGE_INPUT -> change = endpoint.prepareInsert(before, request.aspect(), 1);
                case CENTRIFUGE_OUTPUT_INSERT -> change = ((TileCentrifuge) owner).prepareOutputInsert(before, request.aspect());
                case CENTRIFUGE_OUTPUT_EXTRACT -> change = endpoint.prepareExtract(before, request.aspect(), 1);
                case CRYSTALLIZER_INSERT -> change = endpoint.prepareInsert(before, request.aspect(), 1);
                case CRYSTALLIZER_EXTRACT -> change = endpoint.prepareExtract(before, request.aspect(), 1);
                case CENTRIFUGE_COMPLETE -> {
                    TileCentrifuge centrifuge = (TileCentrifuge) owner;
                    Aspect[] components = centrifuge.getNativeCompletionComponents(before);
                    if (components == null || !current(owner, level) || !endpoint.matchesAtomicState(before)) return new Attempt(false, 0, null);
                    selected = true;
                    // Ready IN1/OUT0/process0 + TWO canonical immediate
                    // components checked before the ONE actual world-RNG draw.
                    change = centrifuge.prepareCompletion(before, components[level.random.nextInt(2)]);
                }
                default -> throw new IllegalStateException("Unrecognized native stock operation");
            }
            selected = true;
            if (!prepared(change, endpoint, before, units) || !current(owner, level) || !endpoint.matchesAtomicState(before)
                    || request.outputFace() != null && !endpoint.canOutputTo(request.outputFace())) return new Attempt(true, 0, null);
            RuntimeException failure = commitPrepared(mutation, owner, endpoint, change, owner, endpoint, null);
            return new Attempt(true, units, failure);
        } catch (RuntimeException failure) {
            return new Attempt(selected, 0, failure);
        } finally { finishTubeFilterMutation(mutation); }
    }

    /** Raw open flags for BOTH basic-family and buffer tubes commit before callbacks. */
    public static boolean setTubeSideOpen(BlockEntity tube, Direction face, boolean open) {
        if (face == null || !(tube instanceof TileTube || tube instanceof TileTubeBuffer) || !(tube.getLevel() instanceof ServerLevel level)) return false;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Tube sides require the server thread");
        if (MUTATION.get() != null || !current(tube, level) || tube instanceof TileTubeBuffer buffer && buffer.isQuarantined()) return false;
        BlockEntity adjacent = neighbor(tube, face, level);
        BlockEntity paired = adjacent instanceof TileTube || adjacent instanceof TileTubeBuffer ? adjacent : null;
        if (paired instanceof TileTubeBuffer buffer && buffer.isQuarantined()) return false;
        Mutation mutation = new Mutation(tube, paired == null ? tube : paired); mutation.tubeSides = true; MUTATION.set(mutation);
        try {
            if (!current(tube, level) || paired != null && !current(paired, level)) return false;
            mutation.raw = true;
            rawSide(tube, face, open);
            if (paired != null) rawSide(paired, face.getOpposite(), open);
            mutation.raw = false;
            try { ((EssentiaAtomicEndpoint) tube).notifyEssentiaStorageChanged(); }
            finally { if (paired != null) ((EssentiaAtomicEndpoint) paired).notifyEssentiaStorageChanged(); }
            return true;
        } finally { finishTubeFilterMutation(mutation); }
    }

    private static void rawSide(BlockEntity tile, Direction face, boolean open) {
        if (tile instanceof TileTube tube) tube.setSideOpenRaw(face, open);
        else ((TileTubeBuffer) tile).setSideOpenRaw(face, open);
    }

    /**
     * Validated native hand exchange (phial or held jar), not sided transport.
     * Caller preflights the gameplay price/filter and prepares ONE output item.
     * Storage and the detached paid hand commit before any notification or ADD.
     * There is no take/add/refund window, including a full void-jar drain.
     */
    public static boolean exchangeItem(BlockEntity endpoint, Player player, InteractionHand hand, ItemStack held,
                                       Aspect beforeType, int beforeAmount, Aspect afterType, int afterAmount,
                                       ItemStack output, boolean replaceHand) {
        if (!(endpoint.getLevel() instanceof ServerLevel level)) return false;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Essentia item exchanges require the server thread");
        if (MUTATION.get() != null || !(endpoint instanceof EssentiaStorageEndpoint storage)
                || !current(endpoint, level) || player.level() != level || player.isSpectator()
                || player.getItemInHand(hand) != held || held.isEmpty() || output.isEmpty() || output.getCount() != 1
                || replaceHand && held.getCount() != 1 || afterAmount < 0 || afterAmount > storage.getEssentiaCapacity()
                || afterAmount > 0 && (afterType == null || Aspect.getAspect(afterType.getTag()) != afterType)) return false;
        ItemStack beforeHand = held.copy(), paid = held.copy(), delivery = output.copy();
        paid.shrink(1);
        Mutation mutation = new Mutation(endpoint, endpoint);
        MUTATION.set(mutation);
        try {
            if (!current(endpoint, level) || storage.getStoredEssentiaType() != beforeType
                    || storage.getStoredEssentiaAmount() != beforeAmount || beforeAmount < 0
                    || beforeAmount > storage.getEssentiaCapacity() || player.getItemInHand(hand) != held
                    || !ItemStack.matches(held, beforeHand)) return false;
            mutation.raw = true;
            storage.setEssentiaStorageRaw(afterAmount == 0 ? null : afterType, afterAmount);
            // Native Inventory.setItem is field-only, unlike equipment hooks on
            // LivingEntity.setItemInHand. Both resources commit before callbacks.
            // Detachment also defeats creative useOn's passed-stack restoration.
            held.shrink(1);
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            player.getInventory().setItem(slot, replaceHand ? delivery : paid.isEmpty() ? ItemStack.EMPTY : paid);
            mutation.raw = false;
            try {
                // Inventory.add silently discards undeliverable leftovers for
                // instabuild players. A paid TC4 exchange must instead keep its
                // output, so do not enter that branch without actual room.
                if (!replaceHand && (!hasRoomForOne(player, delivery) || !player.getInventory().add(delivery)))
                    DeferredItemDrops.enqueue(level, endpoint.getBlockPos(), List.of(delivery));
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
            } finally {
                storage.notifyEssentiaStorageChanged();
            }
            return true;
        } finally {
            mutation.raw = false;
            MUTATION.remove();
        }
    }

    /**
     * Bound-label attachment, never a storage exchange. The native hand list
     * and filter commit before even Inventory.setChanged/equipment callbacks.
     * The detached remainder also defeats creative Item.useOn count restoration.
     */
    public static boolean attachTubeFilter(TileTube tube, Player player, InteractionHand hand, ItemStack held) {
        if (!(tube.getLevel() instanceof ServerLevel level)) return false;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Tube labels require the server thread");
        if (MUTATION.get() != null || !tube.isFilter() || !current(tube, level) || tube.getFilter() != null
                || player.level() != level || player.isSpectator() || player.getItemInHand(hand) != held
                || held.isEmpty() || !(held.getItem() instanceof ItemJarLabel)) return false;
        Aspect chosen = ItemJarLabel.aspect(held);
        if (chosen == null || Aspect.getAspect(chosen.getTag()) != chosen) return false;
        Inventory inventory = player.getInventory();
        var handSlots = hand == InteractionHand.MAIN_HAND ? inventory.items : inventory.offhand;
        int slot = hand == InteractionHand.MAIN_HAND ? inventory.selected : 0;
        if (slot < 0 || slot >= handSlots.size() || hand == InteractionHand.MAIN_HAND && !Inventory.isHotbarSlot(slot)
                || handSlots.get(slot) != held) return false;
        ItemStack beforeHand = held.copy(), paid = held.copy(); paid.shrink(1);
        Mutation mutation = new Mutation(tube, tube, true);
        MUTATION.set(mutation);
        try {
            if (player.level() != level || player.isSpectator() || player.getItemInHand(hand) != held || player.getInventory() != inventory
                    || !current(tube, level) || tube.getFilter() != null || handSlots.get(slot) != held
                    || hand == InteractionHand.MAIN_HAND && inventory.selected != slot
                    || !ItemStack.matches(held, beforeHand) || ItemJarLabel.aspect(held) != chosen) return false;
            mutation.raw = true;
            tube.setFilterRaw(chosen);
            held.shrink(1);
            // Direct native lists, not virtual setItem/setItemInHand: no observer
            // may see a configured filter while the ACTUAL hand is still unpaid.
            handSlots.set(slot, paid.isEmpty() ? ItemStack.EMPTY : paid);
            mutation.raw = false;
            try {
                inventory.setChanged();
                player.containerMenu.broadcastChanges();
            } finally {
                if (current(tube, level)) tube.notifyEssentiaStorageChanged();
            }
            return true;
        } finally {
            finishTubeFilterMutation(mutation);
        }
    }

    /** Sneak removal clears ownership before the durable return or any notification. */
    public static boolean removeTubeFilter(TileTube tube, Player player, InteractionHand hand, ItemStack held, Aspect expected) {
        if (!(tube.getLevel() instanceof ServerLevel level)) return false;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Tube labels require the server thread");
        if (MUTATION.get() != null || expected == null || !tube.isFilter() || !current(tube, level) || tube.getFilter() != expected
                || player.level() != level || player.isSpectator() || !player.isShiftKeyDown() || player.getItemInHand(hand) != held
                || held.getItem() instanceof ItemWandCasting || held.getItem() instanceof ItemResonator) return false;
        List<ItemStack> returned = List.of(ItemJarLabel.blank());
        Mutation mutation = new Mutation(tube, tube, true);
        MUTATION.set(mutation);
        try {
            if (player.level() != level || player.isSpectator() || player.getItemInHand(hand) != held || !player.isShiftKeyDown()
                    || !current(tube, level) || tube.getFilter() != expected) return false;
            mutation.raw = true;
            tube.setFilterRaw(null);
            mutation.raw = false;
            DeferredItemDrops.enqueue(level, tube.getBlockPos(), returned);
            if (current(tube, level)) tube.notifyEssentiaStorageChanged();
            return true;
        } finally {
            finishTubeFilterMutation(mutation);
        }
    }

    /**
     * Native onRemove runs AFTER replacement is installed but BEFORE the old BE
     * is removed. Strict ordinary current() must stay false in that interval.
     * Accept only that exact old owner/replacement pair, even if its retirement
     * was caused by an attach/transfer notification holding the global lock.
     */
    public static boolean returnTubeFilterOnRemove(Level level, BlockPos pos, BlockState oldState, BlockState replacement) {
        if (!(level instanceof ServerLevel server)) return false;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Tube label retirement requires the server thread");
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof TileTube tube)
                || !retiringTube(tube, level, pos, oldState, replacement)) return false;
        Aspect owned = tube.getFilter();
        if (owned == null) return false;
        List<ItemStack> returned = List.of(ItemJarLabel.blank());
        Mutation mutation = new Mutation(tube, tube, true);
        MUTATION.set(mutation);
        try {
            if (!retiringTube(tube, level, pos, oldState, replacement) || tube.getFilter() != owned) return false;
            mutation.raw = true;
            tube.setFilterRaw(null);
            mutation.raw = false;
            // No dying-BE notification: block replacement already owns that
            // update. Clear FIRST, so reentry/ADD cancellation cannot return twice.
            DeferredItemDrops.enqueue(server, pos, returned);
            return true;
        } finally {
            finishTubeFilterMutation(mutation);
        }
    }

    private static boolean retiringTube(TileTube tube, Level level, BlockPos pos, BlockState oldState, BlockState replacement) {
        return oldState.getBlock() instanceof BlockTube block && block.kind() == BlockTube.Kind.FILTER
                && oldState.getBlock() != replacement.getBlock() && tube.isFilter() && !tube.isRemoved()
                && tube.getLevel() == level && tube.getBlockPos().equals(pos) && tube.getBlockState() == oldState
                && level.hasChunkAt(pos) && level.getBlockState(pos) == replacement && level.getBlockEntity(pos) == tube;
    }

    private static void finishTubeFilterMutation(Mutation mutation) {
        mutation.raw = false;
        if (mutation.parent == null) MUTATION.remove();
        else MUTATION.set(mutation.parent);
    }

    private static boolean hasRoomForOne(Player player, ItemStack output) {
        // Native add searches the 36 main slots; an already matching offhand
        // can also merge, but an empty offhand is not a free insertion slot.
        for (int slot = 0; slot < 36; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current.isEmpty() || canMergeOne(current, output)) return true;
        }
        return canMergeOne(player.getOffhandItem(), output);
    }

    private static boolean canMergeOne(ItemStack current, ItemStack output) {
        return !current.isEmpty() && ItemStack.isSameItemSameComponents(current, output)
                && current.getCount() < Math.min(64, current.getMaxStackSize());
    }

    /**
     * Infusion wireless consumption is NOT a sided tube pull: labels, suction
     * and the jar's UP port do not participate. The source and the matrix's
     * outstanding demand pay the SAME unit before any notification can reenter.
     */
    public static int consumeForInfusion(BlockEntity consumer, BlockEntity source, Aspect wanted) {
        if (wanted == null || source == consumer || !(consumer.getLevel() instanceof ServerLevel level)) return 0;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Infusion consumption requires the server thread");
        if (MUTATION.get() != null || !(consumer instanceof InfusionDemandEndpoint demand)
                || !(source instanceof IAspectSource) || !(source instanceof EssentiaStorageEndpoint || source instanceof TileEssentiaReservoir)
                || !current(consumer, level) || !current(source, level)) return 0;
        int dx = source.getBlockPos().getX() - consumer.getBlockPos().getX();
        int dy = source.getBlockPos().getY() - consumer.getBlockPos().getY();
        int dz = source.getBlockPos().getZ() - consumer.getBlockPos().getZ();
        // Original UNKNOWN/range12 search: dy -12..11, not a sphere or LOS.
        if (Math.abs(dx) > 12 || Math.abs(dz) > 12 || dy < -12 || dy >= 12) return 0;
        if (source instanceof TileEssentiaReservoir reservoir) return consumeFromReservoir(level, demand, consumer, reservoir, wanted);
        EssentiaStorageEndpoint storage = (EssentiaStorageEndpoint) source;
        Mutation mutation = new Mutation(consumer, source);
        MUTATION.set(mutation);
        try {
            Aspect storedType = storage.getStoredEssentiaType();
            int stored = storage.getStoredEssentiaAmount(), capacity = storage.getEssentiaCapacity();
            int pending = demand.getPendingEssentia(wanted);
            if (storedType != wanted || stored <= 0 || stored > capacity || pending <= 0) return 0;
            if (!current(consumer, level) || !current(source, level)
                    || storage.getStoredEssentiaType() != storedType || storage.getStoredEssentiaAmount() != stored
                    || storage.getEssentiaCapacity() != capacity || demand.getPendingEssentia(wanted) != pending) return 0;
            mutation.raw = true;
            storage.setEssentiaStorageRaw(stored == 1 ? null : storedType, stored - 1);
            demand.consumeInfusionEssentiaRaw(wanted);
            mutation.raw = false;
            try {
                storage.notifyEssentiaStorageChanged();
            } finally {
                demand.notifyInfusionEssentiaChanged();
            }
            return 1;
        } finally {
            mutation.raw = false;
            MUTATION.remove();
        }
    }

    /** The reservoir holds many aspects, so its unit comes out as a prepared per-aspect change, not as a singleton rewrite. */
    private static int consumeFromReservoir(ServerLevel level, InfusionDemandEndpoint demand, BlockEntity consumer, TileEssentiaReservoir reservoir, Aspect wanted) {
        Mutation mutation = new Mutation(consumer, reservoir);
        MUTATION.set(mutation);
        try {
            int pending = demand.getPendingEssentia(wanted);
            var before = reservoir.snapshotAtomicState();
            var change = reservoir.prepareExtract(before, wanted, 1);
            if (pending <= 0 || !prepared(change, reservoir, before, 1)) return 0;
            if (!current(consumer, level) || !current(reservoir, level) || !reservoir.matchesAtomicState(before) || demand.getPendingEssentia(wanted) != pending) return 0;
            mutation.sourceChange = change;
            mutation.raw = true;
            reservoir.applyAtomicRaw(change);
            demand.consumeInfusionEssentiaRaw(wanted);
            mutation.raw = false;
            try {
                reservoir.notifyEssentiaStorageChanged();
            } finally {
                demand.notifyInfusionEssentiaChanged();
            }
            return 1;
        } finally {
            mutation.raw = false;
            MUTATION.remove();
        }
    }

    private static boolean current(BlockEntity tile, Level level) {
        BlockPos pos = tile.getBlockPos();
        return !tile.isRemoved() && tile.getLevel() == level && level.hasChunkAt(pos) && level.getBlockEntity(pos) == tile
                && level.getBlockState(pos) == tile.getBlockState();
    }

    private static BlockEntity neighbor(BlockEntity origin, Direction face, Level level) {
        BlockPos pos = origin.getBlockPos().relative(face);
        if (!level.hasChunkAt(pos)) return null;
        BlockEntity tile = level.getBlockEntity(pos);
        return tile != null && current(tile, level) ? tile : null;
    }

    private static final class Mutation {
        final BlockEntity receiver;
        BlockEntity source;
        final Mutation parent;
        final boolean tubeFilter;
        boolean tubeSides;
        EssentiaAtomicEndpoint.Change receiverChange, sourceChange;
        EntityGolemBase golem;
        GolemAspectCargo beforeCargo, afterCargo;
        final List<BlockEntity> additionalActors = new ArrayList<>();
        DeferredItemDrops.CrystalEscrow crystalEscrow;
        CrystalOwnership crystalOwnership;
        CrystalInventoryTarget.Plan inventoryPlan;
        BoreScope boreScope;
        BoreWork.Change boreChange;
        BoreWork.RetirementRetention boreRetention;
        DeferredItemDrops.BoreEscrow boreEscrow;
        BoreOwnership boreOwnership;
        boolean raw;

        Mutation(BlockEntity receiver, BlockEntity source) {
            this(receiver, source, false);
        }

        Mutation(BlockEntity receiver, BlockEntity source, boolean tubeFilter) {
            this.receiver = receiver;
            this.source = source;
            this.parent = MUTATION.get();
            this.tubeFilter = tubeFilter;
        }
    }
}

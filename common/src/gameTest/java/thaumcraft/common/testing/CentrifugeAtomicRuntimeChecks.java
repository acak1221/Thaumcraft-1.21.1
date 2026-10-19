package thaumcraft.common.testing;

import static thaumcraft.common.testing.CentrifugeRuntimeChecks.centrifuge;
import static thaumcraft.common.testing.CentrifugeRuntimeChecks.component;
import static thaumcraft.common.testing.CentrifugeRuntimeChecks.tick;
import static thaumcraft.common.testing.CentrifugeRuntimeChecks.ticks;
import static thaumcraft.common.testing.CentrifugeRuntimeChecks.units;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.ordered;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** Third M06d wrapper. Native gameplay uses genuine separate devices; raw stock,
 * labels and pressures are explicit SERVER INPUTS. Only adversarial query-count
 * cases use the isolated atomic probe. Native comparator callbacks observe real
 * dirty notifications; they NEVER invoke completion/notification themselves or
 * write a production timer, clock, RandomSource or completion permission. */
public final class CentrifugeAtomicRuntimeChecks {
    private CentrifugeAtomicRuntimeChecks() {}

    public static void nativeDrawsConservationAndCallbackFences(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); nativeMixedAndOutput(f); genuineBufferStarvation(f); querySitesAndRefusals(f);
            snapshotsAndReplacement(f); pairedNativeDirtyFailuresAndRetirement(f); completionDirtyFailures(f);
        }
        test.succeed();
    }

    private static void nativeMixedAndOutput(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base); TileTubeBuffer source = TubeBufferRuntimeChecks.buffer(f, f.base.below());
        seed(f, source, ordered(Aspect.FIRE, 7, Aspect.WATER, 1)); ticks(f, tile, 20);
        f.check(tile.getTickCount() == 20 && units(tile) == 0 && source.getAmount() == 8 && source.containerContains(Aspect.FIRE) == 7 && source.containerContains(Aspect.WATER) == 1,
                "genuine MIXED primal-only native buffer always refuses centrifuge input, regardless of which real held primal RNG selects; no source debit, recursive split or free input");
        f.reset(); tile = centrifuge(f, f.base); source = TubeBufferRuntimeChecks.buffer(f, f.base.below()); seed(f, source, ordered(Aspect.FIRE, 7, Aspect.MAGIC, 1));
        for (int call = 1; call <= 25 && tile.getInputAspect() == null; call++) {
            tick(f, tile);
            f.check(source.getAmount() + units(tile) == 8 && source.containerContains(Aspect.FIRE) == 7
                    && (tile.getInputAspect() == null ? source.containerContains(Aspect.MAGIC) == 1 && tile.getProcess() == 0
                    : tile.getInputAspect() == Aspect.MAGIC && tile.getProcess() == 38 && source.containerContains(Aspect.MAGIC) == 0),
                    "actual primal+compound buffer draw either refuses the selected primal UNCHANGED or pays its held compound1; never deletes the other seven real units");
        }
        // No flaky requirement that either choice occurs within25 calls. The
        // deterministic native-caller probe below proves the single query/no
        // resampling branch with a primal THEN compound program.
        f.reset(); TileCentrifuge below = centrifuge(f, f.base.below()); TileTubeBuffer receiver = TubeBufferRuntimeChecks.buffer(f, f.base);
        below.addEssentia(Aspect.MAGIC, 1, null); below.addToContainer(Aspect.WATER, 1); TubeBufferRuntimeChecks.ticks(f, receiver, 5);
        f.check(receiver.containerContains(Aspect.WATER) == 1 && receiver.getAmount() == 1 && below.getInputAspect() == Aspect.MAGIC
                && below.getOutputAspect() == null && below.getProcess() == 39 && units(below) + receiver.getAmount() == 2,
                "real CF OUTPUT_UP->buffer transfers only OUT1, preserving separately paid IN/process39 and exact two-unit conservation");
        TubeBufferRuntimeChecks.ticks(f, receiver, 10);
        f.check(receiver.getAmount() == 1 && below.getInputAspect() == Aspect.MAGIC && below.getProcess() == 39, "processing input is NEVER a second public source unit when OUT is empty");
    }

    /** NEW genuine counterexample; accepted M06c probe tests are not edited. */
    private static void genuineBufferStarvation(Fixture f) {
        f.reset(); TileTubeBuffer buffer = TubeBufferRuntimeChecks.buffer(f, f.base); TileCentrifuge above = centrifuge(f, f.base.above());
        above.addEssentia(Aspect.MAGIC, 1, null); above.addToContainer(Aspect.ENERGY, 1);
        for (Direction face : List.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST)) TubeBufferRuntimeChecks.bellows(f, f.base, face, true);
        f.put(f.base.west(), ThaumcraftAlembics.ALEMBIC.get()); var later = (TileAlembic) f.level.getBlockEntity(f.base.west()); later.addToContainer(Aspect.ENERGY, 1);
        TubeBufferRuntimeChecks.ticks(f, buffer, 5);
        f.check(buffer.getBellowsCount() == 4 && buffer.getSuctionAmount(Direction.UP) == 128 && above.isConnectable(Direction.DOWN)
                && above.getEssentiaAmount(Direction.DOWN) == 1 && above.getSuctionAmount(Direction.DOWN) == 64 && !above.canOutputTo(Direction.DOWN)
                && buffer.getAmount() == 0 && above.getInputAspect() == Aspect.MAGIC && above.getOutputAspect() == Aspect.ENERGY && above.getProcess() == 39 && later.getAmount() == 1,
                "GENUINE original failed-first candidate: buffer128 selects CF ABOVE with DOWNamount1/suction64, attempts its forbidden DOWN output ONCE, then STOPS; later WEST real stock remains untouched");
        f.check(above.takeEssentia(Aspect.ENERGY, 1, Direction.UP) == 1, "explicitly account/remove the one genuine CF output through its LEGAL opposite port");
        TubeBufferRuntimeChecks.ticks(f, buffer, 5);
        f.check(buffer.getAmount() == 1 && buffer.containerContains(Aspect.ENERGY) == 1 && later.getAmount() == 0 && above.getInputAspect() == Aspect.MAGIC
                && above.getOutputAspect() == null && above.getProcess() == 39 && buffer.getAmount() + units(above) + later.getAmount() + 1 == 3,
                "with CF public OUT now empty (IN still present), the actual buffer scan may reach WEST; this proves the untouched later source really WAS eligible, and all three supplied units remain accounted");
    }

    private static void querySitesAndRefusals(Fixture f) {
        for (String branch : List.of("compound", "primal-no-resample", "minimum-equality", "minimum-too-high", "pressure-tie", "second-pressure-tie", "output-closed", "opposite-closed")) {
            f.reset(); TileCentrifuge tile = centrifuge(f, f.base);
            var source = probe(f, f.base.below(), ordered(Aspect.MAGIC, 7, Aspect.FIRE, 1), branch.equals("primal-no-resample") ? Aspect.FIRE : Aspect.MAGIC, Aspect.MAGIC);
            if (branch.equals("minimum-equality")) source.minimum = 128;
            if (branch.equals("minimum-too-high")) source.minimum = 129;
            if (branch.equals("pressure-tie")) source.pressure = 128;
            if (branch.equals("second-pressure-tie")) source.query = () -> source.pressure = 128;
            if (branch.equals("output-closed")) source.output = false;
            if (branch.equals("opposite-closed")) source.open = false;
            f.check(EssentiaTransfers.pullCentrifugeOne(tile).committedUnits() == 0 && tile.getTickCount() == 0 && source.typeQueries == 0,
                    "public production profile cannot externally force its private native draw phase");
            ticks(f, tile, 4); f.check(source.typeQueries == 0 && source.rawWrites == 0, "no public representative query/extraction before the genuine fifth idle increment"); tick(f, tile);
            boolean accepted = branch.equals("compound") || branch.equals("minimum-equality");
            boolean queried = accepted || branch.equals("primal-no-resample") || branch.equals("second-pressure-tie");
            f.check(source.unexpected == null && source.typeQueries == (queried ? 1 : 0) && source.typeFaces.stream().allMatch(face -> face == Direction.UP)
                    && source.rawWrites == (accepted ? 1 : 0) && source.total() + units(tile) == 8 && tile.getTickCount() == 5
                    && (accepted ? tile.getInputAspect() == Aspect.MAGIC && tile.getProcess() == 38 && source.stock.get(Aspect.MAGIC) == 6 && source.stock.get(Aspect.FIRE) == 1
                    : units(tile) == 0 && tile.getProcess() == 0 && source.total() == 8),
                    "ACTUAL native centrifuge ticker preserves ONE original type query, zero raw-executor queries/public take-add, strict pressure, minimum equality and no selected-primal resampling: " + branch);
            if (branch.equals("second-pressure-tie")) f.check(source.pressureQueries == 2, "original centrifuge really repeats pressure AFTER the one type selection, not just snapshot refusal");
        }
    }

    private static void snapshotsAndReplacement(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base); var empty = tile.snapshotAtomicState(); var insert = tile.prepareInsert(empty, Aspect.MAGIC, 1);
        f.check(insert != null && insert.owner() == tile && insert.before() == empty && tile.prepareInsert(empty, Aspect.FIRE, 1) == null
                && tile.prepareInsert(empty, Aspect.MAGIC, 2) == null && tile.prepareExtract(empty, Aspect.MAGIC, 1) == null && denied(() -> tile.applyAtomicRaw(insert)),
                "callback-free exact IN planner accepts a compound1 only; prepared changes cannot bypass shared executor authority or expose absent input as output");
        tile.addToContainer(Aspect.FIRE, 1); f.check(!tile.matchesAtomicState(empty), "full native snapshot includes OUT independently of still-empty INPUT");
        var output = tile.snapshotAtomicState(); tile.addEssentia(Aspect.MAGIC, 1, null); f.check(!tile.matchesAtomicState(output), "full native snapshot includes IN/process, not merely public OUT1");
        var processing = tile.snapshotAtomicState(); tick(f, tile);
        f.check(!tile.matchesAtomicState(processing) && tile.getProcess() == 38 && tile.getOutputAspect() == Aspect.FIRE,
                "full native snapshot fences the honestly decremented processing timer even while OUT stays unchanged");
        f.check(EssentiaTransfers.completeCentrifuge(tile).committedUnits() == 0 && tile.prepareCompletion(tile.snapshotAtomicState(), Aspect.VOID) == null,
                "external local completion/plans cannot mint a native completion permission or choose a result aspect");

        for (String changed : List.of("other-aspect", "source-port", "source-replaced", "receiver-replaced")) {
            f.reset(); TileCentrifuge receiver = centrifuge(f, f.base); var source = probe(f, f.base.below(), ordered(Aspect.ENERGY, 7, Aspect.MAGIC, 1), Aspect.ENERGY);
            source.query = () -> {
                f.check(EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(receiver), "both shared source/CF locks precede the actual original type-selection call-out");
                switch (changed) {
                    case "other-aspect" -> source.stock = immutable(ordered(Aspect.ENERGY, 7, Aspect.VOID, 1));
                    case "source-port" -> source.output = false;
                    case "source-replaced" -> f.level.setBlock(source.getBlockPos(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                    case "receiver-replaced" -> replace(f, receiver);
                    default -> throw new AssertionError(changed);
                }
            };
            ticks(f, receiver, 5);
            f.check(source.unexpected == null && source.total() == 8 && source.stock.get(Aspect.ENERGY) == 7 && source.rawWrites == 0 && source.notices == 0
                    && units(receiver) == 0 && source.typeQueries == 1 && !EssentiaTransfers.isLocked(source) && !EssentiaTransfers.isLocked(receiver),
                    "pre-commit callback changes invalidate full stock/ports/current partner identity, WITHOUT debit, IN credit, process39 or hidden refund: " + changed);
            if (changed.equals("receiver-replaced")) f.check(f.level.getBlockEntity(f.base) instanceof TileCentrifuge fresh && fresh != receiver && units(fresh) == 0 && fresh.getProcess() == 0, "replacement is a genuine fresh native BE, not an old prepared input recipient");
        }
    }

    private static void pairedNativeDirtyFailuresAndRetirement(Fixture f) {
        f.reset(); TileCentrifuge tile = centrifuge(f, f.base); TileTubeBuffer source = TubeBufferRuntimeChecks.buffer(f, f.base.below());
        seed(f, source, ordered(Aspect.ENERGY, 7, Aspect.MAGIC, 1));
        // The genuine machine occludes the old north-filter fixture's south
        // approach. Prove that refusal instead of weakening strict native LOS,
        // then spend the SAME held label on the reachable southern filter.
        TileTube occluded = f.tube(f.base.north(), BlockTube.Kind.FILTER);
        ItemStack label = ItemJarLabel.bound("aqua"); f.player.setItemInHand(InteractionHand.MAIN_HAND, label);
        f.aim(Vec3.atCenterOf(occluded.getBlockPos()));
        f.check(!f.use(occluded.getBlockPos()).consumesAction() && occluded.getFilter() == null
                && f.player.getMainHandItem() == label && label.getCount() == 1,
                "actual intervening centrifuge outline refuses behind-machine attachment without spending the genuine bound label");
        TileTube paid = f.tube(f.base.south(), BlockTube.Kind.FILTER); f.aim(Vec3.atCenterOf(paid.getBlockPos()));
        f.check(f.use(paid.getBlockPos()).consumesAction() && paid.getFilter() == Aspect.WATER && f.player.getMainHandItem().isEmpty(),
                "the SAME refused label really pays ONE accessible native filter, later retired during shared callback mutation");
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3));
        var sourceDirty = comparator(f, source.getBlockPos().west(), Direction.EAST); var receiverDirty = comparator(f, f.base.east(), Direction.WEST);
        Aspect[] selected = {null};
        sourceDirty.action = () -> {
            selected[0] = tile.getInputAspect();
            f.check((selected[0] == Aspect.ENERGY || selected[0] == Aspect.MAGIC) && tile.getProcess() == 39 && tile.getOutputAspect() == null
                    && source.getAmount() == 7 && source.containerContains(selected[0]) == (selected[0] == Aspect.ENERGY ? 6 : 0)
                    && EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(tile), "FIRST REAL source-buffer setChanged/comparator callback sees BOTH source-1 and CF IN+process39 atomically, before same-tick decrement38");
            fence(f, tile, source);
            f.level.setBlock(paid.getBlockPos(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.level.setBlock(paid.getBlockPos(), ThaumcraftTubes.ESSENTIA_FILTER.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            var replacement = (TileTube) f.level.getBlockEntity(paid.getBlockPos()); f.aim(Vec3.atCenterOf(paid.getBlockPos()));
            f.check(paid.isRemoved() && paid.getFilter() == null && replacement.getFilter() == null && f.pendingRows().size() == 1
                    && EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(tile)
                    && BlockTube.tryLabelInteraction(f.context(paid.getBlockPos())) == InteractionResult.FAIL && f.player.getMainHandItem().getCount() == 3,
                    "nested native paid-filter retirement clears/enqueues ONE blank without temporarily unlocking the committed buffer/CF parents or spending another label on replacement");
        };
        sourceDirty.failure = "centrifuge-source-dirty-fixture";
        receiverDirty.action = () -> {
            f.check(selected[0] != null && sourceDirty.observations == 1 && tile.getInputAspect() == selected[0] && tile.getProcess() == 39
                    && source.getAmount() == 7 && f.pendingRows().size() == 1, "REAL receiver setChanged callback is attempted in FINALLY after source dirty throws, and still sees both paid fields/process39/one blank");
            fence(f, tile, source);
        };
        receiverDirty.failure = "centrifuge-receiver-dirty-fixture";
        ticks(f, tile, 5);
        f.check(sourceDirty.unexpected == null && receiverDirty.unexpected == null && sourceDirty.observations == 1 && receiverDirty.observations == 1
                && tile.getInputAspect() == selected[0] && tile.getProcess() == 38 && tile.getTickCount() == 5 && source.getAmount() + units(tile) == 8
                && !EssentiaTransfers.isLocked(source) && !EssentiaTransfers.isLocked(tile), "both genuine native dirty failures keep one committed draw, THEN perform original39->38 once; no retry/debit-only state/lock leak");
        f.flushFresh(); f.check(f.blankDrops() == 1 && f.pendingRows().isEmpty(), "actual durable delivery returns exactly ONE blank from the independently PAID retired filter");
        f.check(f.use(paid.getBlockPos()).consumesAction() && ((TileTube) f.level.getBlockEntity(paid.getBlockPos())).getFilter() == Aspect.WATER
                && f.player.getMainHandItem().getCount() == 2 && f.blankDrops() == 1, "all nested mutation frames unwind; a later independent native attachment pays one NEW label normally");
    }

    private static void fence(Fixture f, TileCentrifuge tile, TileTubeBuffer source) {
        var before = tile.saveWithFullMetadata(f.level.registryAccess()); int process = tile.getProcess(), count = tile.getTickCount(), sourceCount = source.getTickCount();
        f.check(tile.addEssentia(Aspect.MAGIC, 1, null) == 0 && tile.addToContainer(Aspect.FIRE, 1) == 1 && !tile.takeFromContainer(Aspect.FIRE, 1)
                && tile.takeEssentia(Aspect.FIRE, 1, Direction.UP) == 0 && source.addToContainer(Aspect.FIRE, 1) == 1 && !source.takeFromContainer(Aspect.ENERGY, 1)
                && !source.setSideOpen(Direction.NORTH, false) && !source.cycleChoke(Direction.NORTH)
                && denied(() -> tile.loadWithComponents(before, f.level.registryAccess())) && EssentiaTransfers.completeCentrifuge(tile).committedUnits() == 0,
                "shared notification lock fences local two-slot/whole-pool stock, reload, raw policy and completion reentry");
        tick(f, tile); TubeBufferRuntimeChecks.tick(f, source);
        f.check(tile.getProcess() == process && tile.getTickCount() == count && source.getTickCount() == sourceCount && before.equals(tile.saveWithFullMetadata(f.level.registryAccess())), "reentrant ACTUAL tickers cannot consume another processing step/cache phase or alter committed IN/OUT while callbacks run");
    }

    private static void completionDirtyFailures(Fixture f) {
        for (boolean replacement : new boolean[] {false, true}) {
            f.reset(); TileCentrifuge tile = centrifuge(f, f.base); tile.addEssentia(Aspect.MAGIC, 1, null); ticks(f, tile, 38);
            f.check(tile.getProcess() == 1 && tile.getInputAspect() == Aspect.MAGIC && tile.getOutputAspect() == null, "only actual native ticker calls reach process1; no clock/counter/reflection forcing");
            var pending = tile.saveWithFullMetadata(f.level.registryAccess()); var dirty = comparator(f, f.base.east(), Direction.WEST); Aspect[] chosen = {null};
            dirty.action = () -> {
                chosen[0] = tile.getOutputAspect();
                f.check(tile.getInputAspect() == null && component(Aspect.MAGIC, chosen[0]) && !chosen[0].isPrimal() && tile.getProcess() == 0 && units(tile) == 1
                        && EssentiaTransfers.isLocked(tile), "FIRST REAL completion dirty callback sees ONE immediate COMPOUND output with IN already cleared, under the SAME local executor lock");
                var extract = tile.prepareExtract(tile.snapshotAtomicState(), chosen[0], 1);
                f.check(extract != null && denied(() -> tile.applyAtomicRaw(extract)) && tile.takeEssentia(chosen[0], 99, Direction.UP) == 0
                        && tile.addEssentia(Aspect.MAGIC, 1, null) == 0 && denied(() -> tile.loadWithComponents(pending, f.level.registryAccess()))
                        && EssentiaTransfers.completeCentrifuge(tile).committedUnits() == 0, "completion notification cannot refund/reload IN, reuse raw plans, extract another unit or reenter the native RNG/completion phase");
                tick(f, tile);
                if (replacement) {
                    replace(f, tile); var fresh = (TileCentrifuge) f.level.getBlockEntity(f.base);
                    f.check(units(fresh) == 0 && fresh.addEssentia(Aspect.MAGIC, 1, null) == 0, "replacement cannot escape active parent scope to accept another unpaid input during completed-stock retirement");
                }
            };
            dirty.failure = "centrifuge-completion-dirty-fixture"; int reads = dirty.reads; tick(f, tile);
            f.check(dirty.unexpected == null && dirty.observations == 1 && chosen[0] != null && tile.getInputAspect() == null && tile.getOutputAspect() == chosen[0]
                    && units(tile) == 1 && !EssentiaTransfers.isLocked(tile), "throwing native completion notification leaves one already-converted unit, no input refund, no reroll or leaked scope");
            if (replacement) {
                var fresh = (TileCentrifuge) f.level.getBlockEntity(f.base); ticks(f, fresh, 5);
                f.check(tile.isRemoved() && fresh != tile && units(fresh) == 0 && fresh.getTickCount() == 5, "native replacement legitimately retires the converted unit, does NOT recover old IN/OUT, and remains independently tickable");
            } else {
                ticks(f, tile, 8);
                f.check(tile.getOutputAspect() == chosen[0] && tile.getTickCount() == 0 && tile.getProcess() == 0 && dirty.reads == reads + 1,
                        "later actual ticks neither retry completion/RNG nor emit another completion notification after its post-commit failure");
            }
        }
    }

    private static void seed(Fixture f, TileTubeBuffer buffer, Map<Aspect, Integer> contents) { contents.forEach((type, count) -> { for (int i = 0; i < count; i++) f.check(buffer.addToContainer(type, 1) == 0, "declared bounded native raw SERVER stock input"); }); TubeBufferRuntimeChecks.assertStock(f, buffer, contents, "actual ordered mixed native stock, not an acquisition claim"); }
    private static boolean denied(Runnable action) { try { action.run(); return false; } catch (IllegalStateException expected) { return true; } }
    private static void replace(Fixture f, TileCentrifuge old) { f.level.setBlock(old.getBlockPos(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); f.level.setBlock(old.getBlockPos(), ThaumcraftTubes.CENTRIFUGE.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); }
    private static DirtyComparator comparator(Fixture f, BlockPos pos, Direction input) {
        f.put(pos.below(), Blocks.STONE); f.level.setBlock(pos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, input), TubeVariantsRuntimeChecks.QUIET); f.level.removeBlockEntity(pos);
        var comparator = new DirtyComparator(pos, f.level.getBlockState(pos)); f.level.setBlockEntity(comparator); return comparator;
    }
    /** Actual vanilla BlockEntity.setChanged -> updateNeighbourForOutputSignal
     * -> ComparatorBlock.checkTickOnNeighbor -> virtual getOutputSignal. This
     * ordinary call-out is observed, NEVER invoked manually. Vanilla may wrap
     * the deliberate RuntimeException in "Exception while updating neighbours".
     * Unexpected assertions are retained for rethrow/check OUTSIDE production's
     * intentionally caught callback-failure path, never swallowed as a pass. */
    private static final class DirtyComparator extends ComparatorBlockEntity {
        Runnable action; String failure; Throwable unexpected; int observations, reads;
        DirtyComparator(BlockPos pos, BlockState state) { super(pos, state); }
        @Override public int getOutputSignal() {
            reads++; if (action != null) {
                Runnable once = action; action = null; observations++;
                try { once.run(); } catch (RuntimeException | Error error) { unexpected = error; throw error; }
                if (failure != null) throw new IllegalStateException(failure);
            }
            return super.getOutputSignal();
        }
    }

    private static SourceProbe probe(Fixture f, BlockPos pos, Map<Aspect, Integer> stock, Aspect... program) { f.put(pos, ThaumcraftTubes.ESSENTIA_TUBE.get()); f.level.removeBlockEntity(pos); var source = new SourceProbe(pos, f.level.getBlockState(pos), stock, program); f.level.setBlockEntity(source); return source; }
    private static Map<Aspect, Integer> immutable(Map<Aspect, Integer> stock) { return Collections.unmodifiableMap(new LinkedHashMap<>(stock)); }
    /** Synchronous adversarial endpoint, NOT a replacement world RNG/native
     * buffer. It exercises the actual CENTRIFUGE ticker caller and same executor,
     * counts public query sites, and trips on ANY public take/add/refund loop. */
    private static final class SourceProbe extends BlockEntity implements EssentiaAtomicEndpoint {
        Map<Aspect, Integer> stock; final Aspect[] program; final List<Direction> typeFaces = new ArrayList<>();
        int typeQueries, pressureQueries, minimum, pressure, rawWrites, notices; boolean open = true, output = true; Runnable query; Throwable unexpected;
        SourceProbe(BlockPos pos, BlockState state, Map<Aspect, Integer> stock, Aspect[] program) { super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state); this.stock = immutable(stock); this.program = program.clone(); }
        private record Stock(Map<Aspect, Integer> contents, boolean open, boolean output, int pressure, int minimum) implements State {}
        int total() { return stock.values().stream().mapToInt(Integer::intValue).sum(); }
        @Override public State snapshotAtomicState() { return new Stock(stock, open, output, pressure, minimum); }
        @Override public boolean matchesAtomicState(State before) { return before instanceof Stock old && old.contents() == stock && old.equals(snapshotAtomicState()); }
        @Override public Change prepareExtract(State before, Aspect type, int units) {
            if (!(before instanceof Stock old) || units != 1 || !EssentiaAtomicEndpoint.validAspect(type) || old.contents().getOrDefault(type, 0) < 1) return null;
            var after = new LinkedHashMap<>(old.contents()); int left = after.get(type) - 1; if (left == 0) after.remove(type); else after.put(type, left);
            return new Change(this, before, new Stock(immutable(after), old.open(), old.output(), old.pressure(), old.minimum()), 1, true);
        }
        @Override public Change prepareInsert(State before, Aspect type, int units) { return null; }
        @Override public void applyAtomicRaw(Change change) { EssentiaTransfers.requireAtomicChange(this, change); stock = ((Stock) change.after()).contents(); rawWrites++; }
        @Override public void notifyEssentiaStorageChanged() { notices++; }
        @Override public boolean isConnectable(Direction face) { return open && face != null; }
        @Override public boolean canInputFrom(Direction face) { return false; }
        @Override public boolean canOutputTo(Direction face) { return output && isConnectable(face); }
        @Override public void setSuction(Aspect type, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { pressureQueries++; return pressure; }
        @Override public Aspect getEssentiaType(Direction face) { typeQueries++; typeFaces.add(face); if (query != null) { Runnable once = query; query = null; try { once.run(); } catch (RuntimeException | Error error) { unexpected = error; throw error; } } return program[(typeQueries - 1) % program.length]; }
        @Override public int getEssentiaAmount(Direction face) { return total(); }
        @Override public int getMinimumSuction() { return minimum; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public int takeEssentia(Aspect type, int units, Direction face) { throw new AssertionError("CF accounting must NEVER call public take/add/refund"); }
        @Override public int addEssentia(Aspect type, int units, Direction face) { throw new AssertionError("CF accounting must NEVER call public take/add/refund"); }
    }
}

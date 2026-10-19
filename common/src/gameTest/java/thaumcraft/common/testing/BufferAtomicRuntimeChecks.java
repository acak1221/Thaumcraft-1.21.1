package thaumcraft.common.testing;

import static thaumcraft.common.testing.TubeBufferRuntimeChecks.assertStock;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.bellows;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.buffer;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.choke;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.ordered;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.tick;
import static thaumcraft.common.testing.TubeBufferRuntimeChecks.ticks;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** M06c's third wrapper. Raw stock/pressure/labels are declared SERVER INPUTS,
 * not survival acquisition. Ordinary cases use real native devices/tickers;
 * deterministic callback probes are isolated below and never survive this
 * synchronous fixture. They count original query sites, NOT world RNG draws:
 * production RandomSource is never replaced, seeded, rewound or written. */
public final class BufferAtomicRuntimeChecks {
    private BufferAtomicRuntimeChecks() {}

    public static void nativeCallersArbitrationAndFences(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); nativeConservation(f); arbitration(f); stoppedCandidates(f);
            callerTypeQuerySites(f); snapshotsAndReplacement(f); mixedReceiverFences(f);
            bothNotificationsAndOverflow(f); paidFilterRetirement(f);
        }
        test.succeed();
    }

    private static void nativeConservation(Fixture f) {
        f.reset(); TileTubeBuffer source = buffer(f, f.base); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1));
        TileJar fire = jar(f, f.base.below(), false); bindJar(f, fire, Aspect.FIRE); jarTicks(f, fire, 5);
        assertStock(f, source, ordered(Aspect.WATER, 7), "real labelled jar drains the actually requested ignis1 out of mixed aqua7/ignis1, without requiring a random representative match");
        f.check(fire.getAmount() == 1 && fire.getAspect() == Aspect.FIRE, "actual native jar ticker receives that ONE real mixed-pool unit");
        TileTube tube = f.tube(f.base.west(), BlockTube.Kind.BASIC); TileJar water = jar(f, f.base.west().below(), false); bindJar(f, water, Aspect.WATER);
        for (int call = 0; call < 100; call++) {
            f.tick(tube); conserve(f, source, tube, fire, water);
            jarTicks(f, water, 1); conserve(f, source, tube, fire, water);
            jarTicks(f, fire, 1); conserve(f, source, tube, fire, water);
        }
        f.check(source.getAmount() == 0 && tube.getEssentiaAmount(null) == 0 && water.getAmount() == 7 && fire.getAmount() == 1
                && f.pendingRows().isEmpty(), "actual production tube/jar callers deliver the remaining seven aqua through native phases; no public take/add refund or hidden pending stock");

        f.reset(); TileTubeBuffer receiver = buffer(f, f.base); source = buffer(f, f.base.below());
        seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1)); choke(source, Direction.UP, 2);
        for (int call = 1; call <= 45; call++) {
            tick(f, receiver);
            f.check(receiver.getAmount() == Math.min(8, call / 5) && source.getAmount() + receiver.getAmount() == 8
                    && source.containerContains(Aspect.WATER) + receiver.containerContains(Aspect.WATER) == 7
                    && source.containerContains(Aspect.FIRE) + receiver.containerContains(Aspect.FIRE) == 1,
                    "real buffer-to-buffer ticker moves one native randomly selected HELD type every fifth call, preserving both per-type totals and the shared eight-unit sum after EVERY call");
        }
        f.check(source.getAmount() == 0 && receiver.containerContains(Aspect.WATER) == 7 && receiver.containerContains(Aspect.FIRE) == 1,
                "draining all eight guarantees exact final membership, without any probabilistic demand that both random choices appear in a sample");
    }

    private static void conserve(Fixture f, TileTubeBuffer source, TileTube tube, TileJar fire, TileJar water) {
        int buffered = tube.getStoredEssentiaAmount(); Aspect held = tube.getStoredEssentiaType();
        f.check(source.getAmount() + buffered + fire.getAmount() + water.getAmount() == 8
                && source.containerContains(Aspect.FIRE) + (held == Aspect.FIRE ? buffered : 0) + fire.getAmount() == 1
                && source.containerContains(Aspect.WATER) + (held == Aspect.WATER ? buffered : 0) + water.getAmount() == 7
                && (buffered == 0 || held == Aspect.WATER) && fire.getAspect() == Aspect.FIRE && water.getFilter() == Aspect.WATER,
                "native mixed buffer->tube->typed jars conserves all eight real units/per-type balances after each individual actor tick");
    }

    private record Demand(String why, Aspect competingType, int requested, int competing, boolean closedSource, boolean valveStem, boolean allowed) {}
    private static void arbitration(Fixture f) {
        for (Demand demand : List.of(
                new Demand("stronger matching typed competitor", Aspect.WATER, 10, 11, false, false, false),
                new Demand("stronger UNTYPED competitor", null, 10, 11, false, false, false),
                new Demand("wrong typed competitor is ignored", Aspect.FIRE, 10, 100, false, false, true),
                new Demand("equal requested demand is not stronger", Aspect.WATER, 10, 10, false, false, true),
                new Demand("weaker matching demand cannot steal", Aspect.WATER, 10, 9, false, false, true),
                new Demand("demand equal to source's own pressure1 is not stronger", null, 0, 1, false, false, true),
                new Demand("source-closed competing raw side is ignored", Aspect.WATER, 10, 100, true, false, true),
                new Demand("neighbor's valve-stem opposite is nonconnectable despite open raw flags", Aspect.WATER, 10, 100, false, true, true))) {
            f.reset(); TileTubeBuffer source = buffer(f, f.base); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1));
            TileTube requested = f.tube(f.base.east(), BlockTube.Kind.BASIC); requested.setSuction(Aspect.FIRE, demand.requested());
            TileTube competing = f.tube(f.base.north(), demand.valveStem() ? BlockTube.Kind.VALVE : BlockTube.Kind.BASIC);
            if (demand.valveStem()) competing.setFacing(Direction.SOUTH);
            competing.setSuction(demand.competingType(), demand.competing());
            if (demand.closedSource()) source.setSideOpen(Direction.NORTH, false);
            var before = source.saveWithFullMetadata(f.level.registryAccess()); int took = source.takeEssentia(Aspect.WATER, 1, Direction.EAST);
            f.check(took == (demand.allowed() ? 1 : 0) && source.containerContains(Aspect.WATER) == 7 - took && source.containerContains(Aspect.FIRE) == 1
                    && source.getAmount() + took == 8 && (demand.allowed() || before.equals(source.saveWithFullMetadata(f.level.registryAccess()))),
                    "original source arbitration (requester's own FIRE type is NOT a raw aqua-extraction veto): " + demand.why());
        }
        f.reset(); TileTubeBuffer source = buffer(f, f.base); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1));
        TileTube competing = f.tube(f.base.north(), BlockTube.Kind.BASIC); competing.setSuction(null, 2);
        f.check(source.takeEssentia(Aspect.WATER, 1, Direction.EAST) == 0 && source.getAmount() == 8, "ABSENT requested neighbor means demand0, not cached pressure, while the stronger untyped neighbor2 blocks");
        source.setSideOpen(Direction.EAST, false);
        f.check(source.takeEssentia(Aspect.WATER, 99, Direction.EAST) == 0 && source.getAmount() == 8, "closed requested raw output refuses even a valid oversized positive take");

        f.reset(); source = buffer(f, f.base); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1));
        f.tube(f.base.east(), BlockTube.Kind.BASIC).setSuction(null, 1);
        competing = f.tube(f.base.north(), BlockTube.Kind.BASIC); competing.setSuction(Aspect.WATER, 32);
        bellows(f, f.base, Direction.DOWN, true); tick(f, source);
        f.check(source.getBellowsCount() == 1 && source.takeEssentia(Aspect.WATER, 1, Direction.EAST) == 1,
                "competing32 > requested1 does NOT win when equal to the source's own cached bellows32");
        competing.setSuction(Aspect.WATER, 33);
        f.check(source.takeEssentia(Aspect.WATER, 1, Direction.EAST) == 0 && source.getAmount() == 7, "competing33 is strictly above BOTH requested1 and source32, and blocks without removing any type");
        competing.setSuction(Aspect.WATER, 32); choke(source, Direction.NORTH, 1);
        f.check(source.takeEssentia(Aspect.WATER, 1, Direction.EAST) == 0, "the competing side's actual choke1 lowers the source threshold to1; no global max-suction approximation");

        f.reset(); source = buffer(f, f.base); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1));
        f.tube(f.base.east(), BlockTube.Kind.BASIC).setSuction(null, 1);
        var noInput = parent(f, f.base.north(), Map.of()); noInput.pressure = 2; noInput.input = false;
        f.check(source.takeEssentia(Aspect.WATER, 1, Direction.EAST) == 0 && noInput.pressureQueries == 1,
                "arbitration considers an opposite-CONNECTABLE compatible demand even when that neighbor canInput=false; no invented capacity/input gate");
    }

    private static void stoppedCandidates(Fixture f) {
        // Close BEFORE placing its source: a legitimately asymmetric pair of raw
        // flags can result from ordinary later placement, without raw flag writes.
        f.reset(); TileTubeBuffer receiver = buffer(f, f.base); receiver.setSideOpen(Direction.DOWN, false);
        TileTubeBuffer source = buffer(f, f.base.below()); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1)); choke(source, Direction.UP, 2);
        bellows(f, f.base, Direction.NORTH, true);
        TileTube competing = f.tube(f.base.below().west(), BlockTube.Kind.BASIC); competing.setSuction(null, 2);
        TileAlembic later = alembic(f, f.base.west(), Aspect.ORDER, 1);
        ticks(f, receiver, 5);
        f.check(receiver.getSuctionAmount(Direction.DOWN) == 32 && !receiver.isConnectable(Direction.DOWN) && source.canOutputTo(Direction.UP)
                && source.getAmount() == 8 && receiver.getAmount() == 0 && later.getAmount() == 1,
                "closed receiving BUFFER is eligible to fill, but source lookup sees requested demand0: competitor2 blocks despite being WEAKER than caller32; first eligible ZERO take stops before later real stock");
        receiver.setSideOpen(Direction.DOWN, true); ticks(f, receiver, 5);
        f.check(source.getAmount() == 7 && receiver.getAmount() == 1 && later.getAmount() == 1
                && source.containerContains(Aspect.WATER) + receiver.containerContains(Aspect.WATER) == 7
                && source.containerContains(Aspect.FIRE) + receiver.containerContains(Aspect.FIRE) == 1,
                "reopening makes the genuine requested demand32 visible and permits exactly ONE original first-source unit, without trying the later alembic");

        for (String reason : List.of("output-refusal", "minimum-refusal", "pressure-tie", "closed-opposite")) {
            f.reset(); receiver = buffer(f, f.base); var probe = parent(f, f.base.below(), ordered(Aspect.WATER, 1), Aspect.WATER);
            later = alembic(f, f.base.west(), Aspect.ORDER, 1);
            switch (reason) {
                case "output-refusal" -> probe.output = false;
                case "minimum-refusal" -> probe.minimum = 2;
                case "pressure-tie" -> probe.pressure = 1;
                case "closed-opposite" -> probe.open &= ~(1 << Direction.UP.get3DDataValue());
                default -> throw new AssertionError(reason);
            }
            ticks(f, receiver, 5); boolean selected = reason.equals("output-refusal");
            f.check(receiver.getAmount() == (selected ? 0 : 1) && probe.total() == 1 && later.getAmount() == (selected ? 1 : 0)
                    && probe.typeQueries == (selected ? 1 : 0) && probe.rawWrites == 0 && probe.notices == 0,
                    "real buffer driver distinguishes first selected zero-attempt STOP from genuinely ineligible source SKIP, with original type-query placement: " + reason);
        }
        f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.BASIC); TileTube pressure = f.tube(f.base.west(), BlockTube.Kind.BASIC);
        pressure.setSuction(Aspect.WATER, 64); f.phase(tube, 10, 4); pressure.setSuction(null, 0);
        source = buffer(f, f.base.below()); seed(f, source, ordered(Aspect.WATER, 7, Aspect.FIRE, 1)); choke(source, Direction.UP, 2);
        f.tube(f.base.below().north(), BlockTube.Kind.BASIC).setSuction(Aspect.WATER, 100);
        later = alembic(f, f.base.east(), Aspect.WATER, 1); f.tick(tube);
        f.check(source.getAmount() == 8 && tube.getStoredEssentiaAmount() == 1 && tube.getStoredEssentiaType() == Aspect.WATER && later.getAmount() == 0,
                "unlike buffer's zero-attempt STOP, the REAL tube driver CONTINUES after its first buffer source is refused by a typed mismatch or stronger arbitration, and drains only later real stock; no RNG sequence required");
    }

    private static void callerTypeQuerySites(Fixture f) {
        for (boolean sink : new boolean[] {false, true}) for (boolean labelled : new boolean[] {true, false}) {
            f.reset(); TileJar jar = jar(f, f.base, sink);
            if (labelled) bindJar(f, jar, Aspect.WATER); else jar.addToContainer(Aspect.WATER, 1);
            var source = parent(f, f.base.above(), ordered(Aspect.WATER, labelled ? 7 : 6, Aspect.FIRE, 1), Aspect.FIRE, Aspect.WATER);
            source.minimum = 1000; jarTicks(f, jar, 5);
            f.check(source.typeQueries == 0 && source.minimumQueries == 0 && source.rawWrites == 1 && source.total() + jar.getAmount() == 8
                    && source.stock.get(Aspect.FIRE) == 1 && jar.getAspect() == Aspect.WATER && jar.getAmount() == (labelled ? 1 : 2),
                    "REAL ordinary/void jar tick uses its label/filled type with ZERO representative queries, ZERO minimum checks and ZERO raw-executor type queries, regardless of an adversarial ignis representative: labelled=" + labelled + "/void=" + sink);
        }
        for (String branch : List.of("minimum-equality", "minimum-too-high", "pressure-tie")) {
            f.reset(); TileJar empty = jar(f, f.base, false);
            var source = parent(f, f.base.above(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.FIRE, Aspect.WATER);
            source.minimum = branch.equals("minimum-too-high") ? 33 : 32; source.pressure = branch.equals("pressure-tie") ? 32 : 31;
            jarTicks(f, empty, 5); boolean accepted = branch.equals("minimum-equality");
            f.check(source.typeQueries == (accepted ? 1 : 0) && source.minimumQueries == (branch.equals("pressure-tie") ? 0 : 1)
                    && source.total() + empty.getAmount() == 8 && empty.getAmount() == (accepted ? 1 : 0)
                    && (!accepted || empty.getAspect() == Aspect.FIRE && source.typeFaces.equals(List.of(Direction.DOWN))),
                    "actual EMPTY unlabelled jar fallback alone checks source minimum (equality passes), strict pressure, then ONE type selection without accounting resample: " + branch);
        }
        f.reset(); TileJar jar = jar(f, f.base, false); bindJar(f, jar, Aspect.WATER);
        var singleton = singleton(f, f.base.above(), Aspect.WATER, 1, 1, false); jarTicks(f, jar, 5);
        f.check(singleton.typeQueries == 0 && singleton.amount == 0 && jar.getAmount() == 1 && singleton.rawWrites == 1,
                "default SINGLETON adapter also snapshots/prepares/commits with ZERO public type queries in the actual typed jar caller");

        for (String branch : List.of("typed-match", "typed-mismatch", "untyped")) {
            f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.BASIC); TileTube pressure = f.tube(f.base.west(), BlockTube.Kind.BASIC);
            pressure.setSuction(branch.equals("untyped") ? null : Aspect.WATER, 64); f.phase(tube, 10, 4);
            f.check(tube.getSuctionAmount(null) == 63 && tube.getSuctionType(null) == (branch.equals("untyped") ? null : Aspect.WATER), "real native phase4 has the required prior suction; no counter/NBT rewrite");
            pressure.setSuction(null, 0);
            var source = parent(f, f.base.below(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), branch.equals("typed-match") ? Aspect.WATER : Aspect.FIRE, Aspect.WATER);
            f.tick(tube); boolean refused = branch.equals("typed-mismatch"); Aspect selected = branch.equals("untyped") ? Aspect.FIRE : Aspect.WATER;
            f.check(source.typeQueries == (refused ? 2 : 1) && source.typeFaces.stream().allMatch(face -> face == Direction.UP)
                    && source.total() + tube.getStoredEssentiaAmount() == 8 && source.rawWrites == (refused ? 0 : 1)
                    && (refused ? tube.getStoredEssentiaAmount() == 0 && source.total() == 8 : tube.getStoredEssentiaType() == selected && source.stock.getOrDefault(selected, 0) == (selected == Aspect.FIRE ? 0 : 6)),
                    "ACTUAL production tube tick preserves original sided-query short circuit and does not requery the chosen type in accounting: " + branch);
        }
        for (String branch : List.of("minimum-equality", "minimum-too-high", "pressure-tie", "source-output-closed")) {
            f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.BASIC); TileTube pressure = f.tube(f.base.west(), BlockTube.Kind.BASIC);
            pressure.setSuction(Aspect.WATER, 64); f.phase(tube, 10, 4); pressure.setSuction(null, 0);
            var candidate = parent(f, f.base.below(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.WATER);
            candidate.minimum = branch.equals("minimum-too-high") ? 64 : 63; candidate.pressure = branch.equals("pressure-tie") ? 63 : 0;
            candidate.output = !branch.equals("source-output-closed"); f.tick(tube); boolean accepted = branch.equals("minimum-equality");
            f.check(candidate.typeQueries == (candidate.output ? 1 : 0) && candidate.rawWrites == (accepted ? 1 : 0)
                    && candidate.total() + tube.getStoredEssentiaAmount() == 8 && tube.getStoredEssentiaAmount() == (accepted ? 1 : 0),
                    "native tube ticker honors original type-query order, output port, STRICT pressure and minimum equality rather than compatibility-wrapper policy: " + branch);
        }
        f.reset(); TileTubeBuffer receiver = buffer(f, f.base);
        var source = parent(f, f.base.below(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.FIRE, Aspect.WATER); ticks(f, receiver, 5);
        f.check(source.typeQueries == 1 && source.typeFaces.equals(List.of(Direction.UP)) && source.total() == 7
                && source.stock.get(Aspect.WATER) == 7 && receiver.containerContains(Aspect.FIRE) == 1 && receiver.getAmount() == 1,
                "ACTUAL buffer tick resolves one candidate type ONCE; raw executor uses exact held ignis, never a second representative aqua or public take/add/refund");
    }

    /** Deliberately malicious field/identity changes in adversarial callbacks,
     * not gameplay APIs and never a claim of legitimate native stock creation. */
    private static void snapshotsAndReplacement(Fixture f) {
        for (String changed : List.of("unselected-aspect", "unrelated-source-side", "receiver-port", "source-output", "source-replaced", "receiver-replaced")) {
            f.reset(); var receiver = singleton(f, f.base, null, 0, 1, false);
            var source = parent(f, f.base.east(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.WATER);
            source.preflight = () -> {
                f.check(EssentiaTransfers.isLocked(receiver) && EssentiaTransfers.isLocked(source), "both shared endpoint locks already hold before the source's callback-bearing take-policy hook");
                switch (changed) {
                    case "unselected-aspect" -> source.stock = immutable(ordered(Aspect.WATER, 7, Aspect.EARTH, 1));
                    case "unrelated-source-side" -> source.open &= ~(1 << Direction.NORTH.get3DDataValue());
                    case "receiver-port" -> receiver.open = false;
                    case "source-output" -> source.output = false;
                    case "source-replaced" -> f.put(source.getBlockPos(), Blocks.STONE);
                    case "receiver-replaced" -> {
                        f.level.setBlock(receiver.getBlockPos(), Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                        f.level.setBlock(receiver.getBlockPos(), ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
                        f.check(f.level.getBlockEntity(receiver.getBlockPos()) instanceof TileTube && f.level.getBlockEntity(receiver.getBlockPos()) != receiver, "genuine replacement installs a DIFFERENT current native BE");
                    }
                    default -> throw new AssertionError(changed);
                }
            };
            f.check(EssentiaTransfers.pullOne(receiver, Direction.EAST, Aspect.WATER) == 0 && receiver.amount == 0 && source.total() == 8
                    && source.stock.get(Aspect.WATER) == 7 && source.rawWrites == 0 && receiver.rawWrites == 0 && source.notices == 0 && receiver.notices == 0
                    && source.typeQueries == 0 && source.preflights == 1 && !EssentiaTransfers.isLocked(source) && !EssentiaTransfers.isLocked(receiver),
                    "shared known-aspect mixed accounting rejects FULL stale stock/policy/partner identity, without debit/credit/refund or ANY representative query: " + changed);
        }
    }

    private static void mixedReceiverFences(Fixture f) {
        f.reset(); TileTubeBuffer receiver = buffer(f, f.base); seed(f, receiver, ordered(Aspect.FIRE, 7));
        var source = parent(f, f.base.below(), ordered(Aspect.WATER, 1), Aspect.WATER);
        TileTube filter = f.tube(f.base.east(), BlockTube.Kind.FILTER);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3)); f.aim(Vec3.atCenterOf(filter.getBlockPos()));
        int[] observed = {0}; Runnable fence = () -> {
            observed[0]++; int count = receiver.getTickCount(); var saved = receiver.saveWithFullMetadata(f.level.registryAccess());
            f.check(EssentiaTransfers.isLocked(receiver) && EssentiaTransfers.isLocked(source) && receiver.addToContainer(Aspect.WATER, 1) == 1
                    && !receiver.takeFromContainer(Aspect.FIRE, 1) && receiver.takeEssentia(Aspect.FIRE, 1, Direction.UP) == 0
                    && !receiver.setSideOpen(Direction.NORTH, false) && !receiver.cycleChoke(Direction.NORTH), "real mixed buffer refuses add/take/paired-side/choke reentry under the SAME endpoint lock");
            tick(f, receiver);
            f.check(receiver.getTickCount() == count && saved.equals(receiver.saveWithFullMetadata(f.level.registryAccess()))
                    && BlockTube.tryLabelInteraction(f.context(filter.getBlockPos())) == InteractionResult.FAIL
                    && filter.getFilter() == null && f.player.getMainHandItem().getCount() == 3 && f.pendingRows().isEmpty(), "query/notification callbacks cannot tick the mixed pool, mutate saved policy, spend a label through another endpoint, or create a hidden return");
        };
        source.query = fence;
        source.notice = () -> {
            f.check(receiver.getAmount() == 8 && receiver.containerContains(Aspect.FIRE) == 7 && receiver.containerContains(Aspect.WATER) == 1 && source.total() == 0,
                    "FIRST native source notification already sees BOTH fully committed mixed receiver/source fields, including unrelated ignis7 and cap8");
            fence.run(); throw new IllegalStateException("buffer-notification-failure-fixture");
        };
        ticks(f, receiver, 5);
        f.check(observed[0] == 2 && receiver.getTickCount() == 5 && receiver.getAmount() == 8 && source.total() == 0 && source.typeQueries == 1 && source.notices == 1
                && !EssentiaTransfers.isLocked(receiver) && !EssentiaTransfers.isLocked(source) && f.pendingRows().isEmpty(),
                "real buffer driver treats a post-COMMIT throwing callback as one paid attempt, never rolls back/retries or leaks either shared lock");
        f.check(receiver.takeFromContainer(Aspect.FIRE, 1) && receiver.addToContainer(Aspect.FIRE, 1) == 0 && receiver.getAmount() == 8,
                "ordinary independent native stock operations work again after failed notification unwinds the global scope");

        f.reset(); TileTubeBuffer otherReceiver = buffer(f, f.base); var failing = parent(f, f.base.below(), ordered(Aspect.WATER, 1), Aspect.WATER);
        TileAlembic later = alembic(f, f.base.west(), Aspect.FIRE, 1);
        failing.query = () -> { throw new IllegalStateException("buffer-query-failure-fixture"); };
        ticks(f, otherReceiver, 5);
        f.check(otherReceiver.getAmount() == 0 && failing.total() == 1 && later.getAmount() == 1 && failing.typeQueries == 1 && failing.rawWrites == 0
                && !EssentiaTransfers.isLocked(otherReceiver) && !EssentiaTransfers.isLocked(failing), "first selected candidate's PRE-commit query failure still stops native buffer fill before later stock, without a paid/debit-only state");
    }

    private static void bothNotificationsAndOverflow(Fixture f) {
        f.reset(); var receiver = singleton(f, f.base, null, 0, 1, false);
        var source = parent(f, f.base.east(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.WATER);
        var debit = source.prepareExtract(source.snapshotAtomicState(), Aspect.WATER, 1);
        var credit = receiver.prepareInsert(receiver.snapshotAtomicState(), Aspect.WATER, 1);
        source.notice = () -> {
            f.check(source.total() == 7 && source.stock.get(Aspect.FIRE) == 1 && receiver.amount == 1 && receiver.type == Aspect.WATER
                    && receiver.notices == 0 && EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(receiver), "mixed->default-singleton BOTH fields precede source notification, with source-before-receiver notification ordering");
            f.check(EssentiaTransfers.pullOne(receiver, Direction.EAST, Aspect.WATER) == 0 && rawRefused(() -> source.applyAtomicRaw(debit))
                    && rawRefused(() -> receiver.applyAtomicRaw(credit)), "notification scope cannot reuse even a valid owner-bound prepared change or enter another pair transaction");
            throw new IllegalStateException("buffer-source-notification-fixture");
        };
        receiver.notice = () -> {
            f.check(source.notices == 1 && receiver.amount == 1 && source.total() == 7 && EssentiaTransfers.isLocked(source) && EssentiaTransfers.isLocked(receiver),
                    "receiver notification is genuinely attempted in FINALLY despite source failure, still seeing both paid fields under both locks");
            throw new IllegalStateException("buffer-receiver-notification-fixture");
        };
        RuntimeException failed = null;
        // Compatibility wrapper remains distinct from the production callers.
        // A mixed known-aspect debit proves stock without a representative;
        // the old SINGLETON-only callback query remains covered by M06b.
        try { EssentiaTransfers.pullOne(receiver, Direction.EAST, Aspect.WATER); } catch (RuntimeException expected) { failed = expected; }
        f.check(failed != null && "buffer-source-notification-fixture".equals(failed.getMessage()) && failed.getSuppressed().length == 1
                && "buffer-receiver-notification-fixture".equals(failed.getSuppressed()[0].getMessage()) && source.notices == 1 && receiver.notices == 1
                && source.rawWrites == 1 && receiver.rawWrites == 1 && source.typeQueries == 0 && receiver.typeQueries == 0
                && source.total() + receiver.amount == 8 && !EssentiaTransfers.isLocked(source) && !EssentiaTransfers.isLocked(receiver),
                "both notification failures preserve the committed eighth unit, report both failures, and release the shared scope without public take/add or hidden refund");

        f.reset(); var sink = singleton(f, f.base, Aspect.WATER, 64, 64, true);
        var stock = parent(f, f.base.east(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.WATER);
        f.check(EssentiaTransfers.pullOne(sink, Direction.EAST, Aspect.WATER) == 1 && sink.amount == 64 && sink.notices == 0 && sink.rawWrites == 0
                && stock.total() == 7 && stock.stock.get(Aspect.FIRE) == 1 && stock.notices == 1 && stock.rawWrites == 1,
                "shared default singleton full-void plan intentionally consumes ONE real unit, stores NONE, and does not raw-write/notify an unchanged full sink; not an implicit refund reservoir");
    }

    private static void paidFilterRetirement(Fixture f) {
        f.reset(); TileTube paid = f.tube(f.base, BlockTube.Kind.FILTER); f.bind(paid, Aspect.WATER);
        var marker = ((IAspectContainer) paid).getAspects();
        f.check(marker.getAmount(Aspect.WATER) == -1 && paid.getStoredEssentiaAmount() == 0, "actual PAID filter marker is not one physical unit in the shared singleton adapter");
        var source = parent(f, f.base.east(), ordered(Aspect.WATER, 7, Aspect.FIRE, 1), Aspect.WATER);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound("aqua").copyWithCount(3)); f.aim(Vec3.atCenterOf(f.base));
        source.notice = () -> {
            f.check(paid.getStoredEssentiaAmount() == 1 && paid.getFilter() == Aspect.WATER && source.total() == 7
                    && EssentiaTransfers.isLocked(paid) && EssentiaTransfers.isLocked(source), "mixed->paid-filter callback begins with genuine buffered stock and an independently owned paid marker under the original pair locks");
            f.level.setBlock(f.base, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.level.setBlock(f.base, ThaumcraftTubes.ESSENTIA_FILTER.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            var replacement = (TileTube) f.level.getBlockEntity(f.base);
            f.check(paid.isRemoved() && paid.getFilter() == null && replacement.getFilter() == null && f.pendingRows().size() == 1
                    && EssentiaTransfers.isLocked(paid) && EssentiaTransfers.isLocked(source), "native onRemove nested retirement clears the old paid owner and queues ONE blank, but never temporarily unlocks its already committed mixed/singleton parents");
            f.check(BlockTube.tryLabelInteraction(f.context(f.base)) == InteractionResult.FAIL && replacement.getFilter() == null
                    && f.player.getMainHandItem().getCount() == 3 && f.pendingRows().size() == 1,
                    "replacement identity does not escape the outer global mutation to pay/attach another label or rediscover a second old-label return");
            throw new IllegalStateException("buffer-paid-filter-retirement-fixture");
        };
        boolean failed = false;
        try { EssentiaTransfers.pullOne(paid, Direction.EAST, Aspect.WATER); }
        catch (IllegalStateException expected) { failed = "buffer-paid-filter-retirement-fixture".equals(expected.getMessage()); }
        var replacement = (TileTube) f.level.getBlockEntity(f.base);
        f.check(failed && source.total() == 7 && paid.getStoredEssentiaAmount() == 1 && replacement.getStoredEssentiaAmount() == 0
                && f.pendingRows().size() == 1 && !EssentiaTransfers.isLocked(paid) && !EssentiaTransfers.isLocked(source),
                "post-commit block replacement legitimately retires/destroys the old buffered unit: it is NOT secretly refunded to mixed stock or copied to the replacement; one blank survives callback failure");
        f.flushFresh(); f.check(f.blankDrops() == 1 && f.pendingRows().isEmpty(), "actual durable queue delivers precisely ONE original blank for the retired PAID label");
        f.check(f.use(f.base).consumesAction() && replacement.getFilter() == Aspect.WATER && f.player.getMainHandItem().getCount() == 2
                && f.blankDrops() == 1 && f.pendingRows().isEmpty(), "all nested frames unwind: the next independent native attachment pays exactly one NEW label, not the retired blank or an old transfer credit");
    }

    private static boolean rawRefused(Runnable action) { try { action.run(); return false; } catch (IllegalStateException expected) { return true; } }
    private static void seed(Fixture f, TileTubeBuffer buffer, Map<Aspect, Integer> contents) {
        contents.forEach((type, amount) -> { for (int i = 0; i < amount; i++) f.check(buffer.addToContainer(type, 1) == 0, "declared native server stock fixture fits in the real mixed pool"); });
        assertStock(f, buffer, contents, "explicit raw SERVER INPUT is exact ordered native stock, never a survival-acquisition claim");
    }
    private static TileAlembic alembic(Fixture f, BlockPos pos, Aspect type, int amount) {
        f.put(pos, ThaumcraftAlembics.ALEMBIC.get()); var source = (TileAlembic) f.level.getBlockEntity(pos);
        f.check(source.addToContainer(type, amount) == 0, "declared later REAL alembic stock fixture is bounded and genuine"); return source;
    }
    private static TileJar jar(Fixture f, BlockPos pos, boolean sink) { f.put(pos, (sink ? ThaumcraftContent.VOID_JAR : ThaumcraftContent.JAR).get()); return (TileJar) f.level.getBlockEntity(pos); }
    private static void bindJar(Fixture f, TileJar jar, Aspect type) {
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound(type.getTag())); f.player.setShiftKeyDown(false); f.aim(Vec3.atCenterOf(jar.getBlockPos()));
        f.check(f.use(jar.getBlockPos()).consumesAction() && jar.getFilter() == type && f.player.getMainHandItem().isEmpty(), "actual native survival actor pays the declared raw bound-label input to the real jar");
    }
    private static void jarTicks(Fixture f, TileJar jar, int calls) {
        for (int i = 0; i < calls; i++) {
            var pos = jar.getBlockPos(); var state = f.level.getBlockState(pos); var ticker = state.getTicker(f.level, ThaumcraftContent.JAR_BLOCK_ENTITY.get());
            if (ticker == null) throw new IllegalStateException("Missing real jar ticker"); ticker.tick(f.level, pos, state, jar);
        }
    }
    private static ParentProbe parent(Fixture f, BlockPos pos, Map<Aspect, Integer> stock, Aspect... offered) {
        f.put(pos, ThaumcraftTubes.ESSENTIA_TUBE.get()); f.level.removeBlockEntity(pos);
        var probe = new ParentProbe(pos, f.level.getBlockState(pos), stock, offered); f.level.setBlockEntity(probe); return probe;
    }
    private static SingletonProbe singleton(Fixture f, BlockPos pos, Aspect type, int amount, int capacity, boolean sink) {
        f.put(pos, ThaumcraftTubes.ESSENTIA_TUBE.get()); f.level.removeBlockEntity(pos);
        var probe = new SingletonProbe(pos, f.level.getBlockState(pos), type, amount, capacity, sink); f.level.setBlockEntity(probe); return probe;
    }
    private static Map<Aspect, Integer> immutable(Map<Aspect, Integer> stock) { return Collections.unmodifiableMap(new LinkedHashMap<>(stock)); }

    /** Adversarial native-level endpoint using the NEW parent, deliberately NOT
     * singleton-compatible. No external gameplay, RNG or production field edits.
     * Public take/add are tripwires; only the authorized executor can raw-apply. */
    private static final class ParentProbe extends BlockEntity implements EssentiaAtomicEndpoint {
        Map<Aspect, Integer> stock; final Aspect[] offered; final List<Direction> typeFaces = new ArrayList<>();
        int open = 63, pressure, minimum, typeQueries, pressureQueries, minimumQueries, preflights, rawWrites, notices;
        Aspect suction; boolean input = true, output = true; Runnable query, preflight, notice;
        ParentProbe(BlockPos pos, BlockState state, Map<Aspect, Integer> stock, Aspect[] offered) { super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state); this.stock = immutable(stock); this.offered = offered.clone(); }
        private record MixedState(Map<Aspect, Integer> stock, int open, int pressure, int minimum, Aspect suction, boolean input, boolean output) implements State {}
        int total() { return stock.values().stream().mapToInt(Integer::intValue).sum(); }
        @Override public State snapshotAtomicState() { return new MixedState(stock, open, pressure, minimum, suction, input, output); }
        @Override public boolean matchesAtomicState(State before) { return before instanceof MixedState saved && saved.stock() == stock && saved.equals(snapshotAtomicState()); }
        @Override public Change prepareExtract(State before, Aspect type, int units) {
            if (!(before instanceof MixedState saved) || units <= 0 || !EssentiaAtomicEndpoint.validAspect(type) || saved.stock().getOrDefault(type, 0) < units) return null;
            var after = new LinkedHashMap<>(saved.stock()); int left = after.get(type) - units; if (left == 0) after.remove(type); else after.put(type, left);
            return change(saved, after, units);
        }
        @Override public Change prepareInsert(State before, Aspect type, int units) {
            if (!(before instanceof MixedState saved) || units != 1 || !EssentiaAtomicEndpoint.validAspect(type) || saved.stock().values().stream().mapToInt(Integer::intValue).sum() >= 8) return null;
            var after = new LinkedHashMap<>(saved.stock()); after.merge(type, 1, Integer::sum); return change(saved, after, units);
        }
        private Change change(MixedState before, Map<Aspect, Integer> after, int units) { return new Change(this, before, new MixedState(immutable(after), before.open(), before.pressure(), before.minimum(), before.suction(), before.input(), before.output()), units, true); }
        @Override public void applyAtomicRaw(Change change) { EssentiaTransfers.requireAtomicChange(this, change); stock = ((MixedState) change.after()).stock(); rawWrites++; }
        @Override public boolean preflightTake(Aspect type, Direction face) { preflights++; if (preflight != null) { Runnable once = preflight; preflight = null; once.run(); } return true; }
        @Override public void notifyEssentiaStorageChanged() { notices++; if (notice != null) notice.run(); }
        @Override public boolean isConnectable(Direction face) { return face != null && (open & 1 << face.get3DDataValue()) != 0; }
        @Override public boolean canInputFrom(Direction face) { return input && isConnectable(face); }
        @Override public boolean canOutputTo(Direction face) { return output && isConnectable(face); }
        @Override public void setSuction(Aspect type, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return suction; }
        @Override public int getSuctionAmount(Direction face) { pressureQueries++; return pressure; }
        @Override public Aspect getEssentiaType(Direction face) {
            typeQueries++; typeFaces.add(face); if (query != null) { Runnable once = query; query = null; once.run(); }
            return offered.length > 0 ? offered[(typeQueries - 1) % offered.length] : stock.isEmpty() ? null : stock.keySet().iterator().next();
        }
        @Override public int getEssentiaAmount(Direction face) { return total(); }
        @Override public int getMinimumSuction() { minimumQueries++; return minimum; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public int takeEssentia(Aspect type, int units, Direction face) { throw new AssertionError("Shared mixed accounting must NEVER call public take/add/refund"); }
        @Override public int addEssentia(Aspect type, int units, Direction face) { throw new AssertionError("Shared mixed accounting must NEVER call public take/add/refund"); }
    }

    /** Existing native storage shape: relies on the production DEFAULT adapters,
     * not copied snapshot/preparation logic. Removed before ordinary world ticks. */
    private static final class SingletonProbe extends BlockEntity implements EssentiaStorageEndpoint {
        Aspect type; int amount, rawWrites, notices, typeQueries; final int capacity; final boolean sink; boolean open = true; Runnable notice;
        SingletonProbe(BlockPos pos, BlockState state, Aspect type, int amount, int capacity, boolean sink) { super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state); this.type = type; this.amount = amount; this.capacity = capacity; this.sink = sink; }
        @Override public Aspect getStoredEssentiaType() { return type; }
        @Override public int getStoredEssentiaAmount() { return amount; }
        @Override public int getEssentiaCapacity() { return capacity; }
        @Override public boolean allowsEssentiaOverflow() { return sink; }
        @Override public void setEssentiaStorageRaw(Aspect type, int amount) { EssentiaTransfers.requireMutation(this); this.type = type; this.amount = amount; rawWrites++; }
        @Override public void notifyEssentiaStorageChanged() { notices++; if (notice != null) notice.run(); }
        @Override public boolean isConnectable(Direction face) { return face != null && open; }
        @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
        @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
        @Override public void setSuction(Aspect type, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { return 0; }
        @Override public Aspect getEssentiaType(Direction face) { typeQueries++; return amount > 0 ? type : null; }
        @Override public int getEssentiaAmount(Direction face) { return amount; }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public int takeEssentia(Aspect type, int units, Direction face) { throw new AssertionError("Default singleton accounting must NEVER call public take/add/refund"); }
        @Override public int addEssentia(Aspect type, int units, Direction face) { throw new AssertionError("Default singleton accounting must NEVER call public take/add/refund"); }
    }
}

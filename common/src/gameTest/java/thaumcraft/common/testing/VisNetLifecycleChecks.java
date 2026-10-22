package thaumcraft.common.testing;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.common.tiles.TileVisRelay;
import thaumcraft.api.nodes.NodeType;

/** Native current-owner/callback tests plus the real EARTH consumer integration.
 * Probe subclasses are SERVER-only installed registered-type API adversaries,
 * NEVER a public fake-grant API or evidence that an arbitrary addon is atomic.
 * Same-location loadStatic/graph lifetime clearing is labelled honestly: it is
 * NOT a full chunk unload, server restart, cross-file power loss or client run. */
public final class VisNetLifecycleChecks {
    private VisNetLifecycleChecks() {}

    /** Suggested timeout240, separate batch with no other M09b level owner. */
    public static void callbacksReloadAndIsolation(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open(); committedCallbacks(f); providerAndConsumerIdentity(f); currentReloadAndIsolation(f); nativeVisibilityAbsence(f);
        }
        test.succeed();
    }

    private static void committedCallbacks(Fixture f) {
        f.reset();
        var source = source(f, f.base.west(7));
        var other = source(f, f.base.west(7).south(7)); // outside both consumer/probe reach
        var relay = pulseRelay(f, f.base); graphTick(f, relay);
        f.check(VisNetRuntimeChecks.parent(relay) == source, "actual built-in source with installed test-only pulse callback in native graph");
        BlockPos consumer = f.base.east(7);
        long epoch = source.getRefillEpoch(), otherEpoch = other.getRefillEpoch();
        int phase = source.getNodeCounter();
        var saved = source.saveWithFullMetadata(f.level.registryAccess());
        relay.pulse = () -> {
            f.check(source.getAvailableVis().getAmount(Aspect.EARTH) == 4 && VisNetHandler.isConsumeActive(f.level),
                    "FIRST post-debit callback ALREADY sees committed4 of8 and whole-level consume frame");
            f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 8) == 0 && source.consumeVis(Aspect.EARTH, 8) == 0,
                    "nested drainer/direct-source entry cannot spend the same stock snapshot");
            NodeTransductionRuntimeChecks.sourceTick(f, source);
            NodeTransductionRuntimeChecks.sourceTick(f, other);
            f.check(source.getRefillEpoch() == epoch && source.getNodeCounter() == phase && other.getRefillEpoch() == otherEpoch
                            && source.getAvailableVis().getAmount(Aspect.EARTH) == 4,
                    "reentrant ACTUAL source tick is fenced BEFORE graph/RNG/refill for this AND another root in SAME level");
            boolean refused = false;
            try { source.loadWithComponents(saved, f.level.registryAccess()); } catch (IllegalStateException expected) { refused = true; }
            f.check(refused && source.getAvailableVis().getAmount(Aspect.EARTH) == 4, "installed source reload cannot reset an in-flight paid frame");
            relay.setParent(null);
            f.check(VisNetRuntimeChecks.parent(relay) == source, "consume callback cannot detach/reassign route to escape its debit frame");
            throw new PulseFailure("m09b-post-commit-pulse-fixture");
        };
        f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 4) == 4
                        && source.getAvailableVis().getAmount(Aspect.EARTH) == 4 && relay.pulses == 1
                        && !VisNetHandler.isConsumeActive(f.level),
                "throwing post-COMMIT pulse returns actual4, not0/refund/retry; finally releases shared frame");
        f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 1) == 1 && source.getAvailableVis().getAmount(Aspect.EARTH) == 3,
                "next independent drain works with remaining3, not a leaked lock or free replenishment");

        var replacement = new TileNodeEnergized[1];
        relay.pulse = () -> {
            f.check(source.getAvailableVis().getAmount(Aspect.EARTH) == 1, "second callback sees actual2 debited BEFORE identity replacement");
            f.level.removeBlockEntity(source.getBlockPos());
            var loaded = ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get().create(source.getBlockPos(), source.getBlockState());
            f.check(loaded instanceof TileNodeEnergized, "callback uses REAL registered factory, not a stock grant");
            f.level.setBlockEntity(loaded); replacement[0] = (TileNodeEnergized) loaded;
            boolean refused = false;
            try { replacement[0].loadWithComponents(saved, f.level.registryAccess()); } catch (IllegalStateException expected) { refused = true; }
            NodeTransductionRuntimeChecks.sourceTick(f, replacement[0]);
            f.check(refused && replacement[0].getAvailableVis().visSize() == 0 && replacement[0].getRefillEpoch() == 0,
                    "simultaneous same-coordinate current replacement has ZERO pulse and cannot reload/refill inside old consume frame");
            throw new PulseFailure("m09b-post-commit-replacement-fixture");
        };
        int actual = VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 2);
        f.check(actual == 2 && replacement[0] != null && NodeTransductionRuntimeChecks.existing(f, source.getBlockPos()) == replacement[0]
                        && source.isRemoved() && replacement[0].getAvailableVis().visSize() == 0,
                "committed2 remains actual despite retirement/FX failure, NEVER accidentally returns new replacement stock");
        var current = NodeTransductionRuntimeChecks.reload(f, replacement[0], saved, TileNodeEnergized.class);
        NodeTransductionRuntimeChecks.sourceTick(f, current);
        graphTick(f, relay);
        source.clearRemoved(); // explicit detached-but-unmarked owner INPUT
        source.removeThisNode(); NodeTransductionRuntimeChecks.sourceTick(f, source); source.setRemoved();
        f.check(VisNetHandler.getSources(f.level).stream().anyMatch(ref -> ref.get() == current)
                        && current.getAvailableVis().getAmount(Aspect.EARTH) == 8
                        && VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 1) == 1,
                "stale SAME-coordinate owner removes ONLY its identity, cannot unregister/debit CURRENT replacement root");
        relay.pulse = () -> {
            int remaining = current.getAvailableVis().getAmount(Aspect.EARTH);
            long beforeEpoch = current.getRefillEpoch();
            VisNetHandler.clearLevel(f.level);
            NodeTransductionRuntimeChecks.sourceTick(f, current);
            f.check(VisNetHandler.isConsumeActive(f.level) && current.getRefillEpoch() == beforeEpoch
                            && current.getAvailableVis().getAmount(Aspect.EARTH) == remaining
                            && VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 99) == 0,
                    "native level-clear INSIDE paid callback keeps closed consume frame fenced; cannot reopen source refill/stock via a new lifetime");
        };
        f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 2) == 2 && !VisNetHandler.isConsumeActive(f.level),
                "actual committed2 survives lifetime-clear callback, finally unwinds without an accidental refund0");
    }

    private static void providerAndConsumerIdentity(Fixture f) {
        // Invalid-provider handling tests the PUBLIC native integration boundary,
        // not a claim that readback can roll back arbitrary addon mutations.
        for (int grant : new int[] {-1, 5, 0, 2, 4}) {
            f.reset(); var provider = grantProbe(f, f.base.west(7), Aspect.ENERGY, grant);
            graphTick(f, provider);
            int actual = VisNetHandler.drainVis(f.level, f.base, Aspect.ENERGY, 4);
            f.check(provider.calls == 1 && actual == (grant < 0 || grant > 4 ? 0 : grant)
                            && !VisNetHandler.isConsumeActive(f.level),
                    "native boundary returns0 for WHOLE invalid grant, real0/partial2/full4 otherwise; canonical compound provider is NOT built-in-primal-whitelisted: " + grant);
        }
        for (boolean replace : new boolean[] {false, true}) {
            f.reset(); var beforeSource = source(f, f.base.west(7)); var beforeRelay = pulseRelay(f, f.base); graphTick(f, beforeRelay);
            var replacement = new TileNodeEnergized[1];
            beforeRelay.query = () -> {
                f.level.removeBlockEntity(beforeSource.getBlockPos());
                if (replace) {
                    var next = ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get().create(beforeSource.getBlockPos(), beforeSource.getBlockState());
                    f.level.setBlockEntity(next); replacement[0] = next;
                    NodeTransductionRuntimeChecks.sourceTick(f, next);
                }
            };
            f.check(VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 4) == 0 && beforeSource.isRemoved()
                            && (replace ? NodeTransductionRuntimeChecks.existing(f, beforeSource.getBlockPos()) == replacement[0]
                            && replacement[0].getAvailableVis().visSize() == 0 : NodeTransductionRuntimeChecks.existing(f, beforeSource.getBlockPos()) == null),
                    "metadata callback deletes/replaces CURRENT source BEFORE debit: no old-owner access, new-stock return or lazy absent-BE manufacture; replace=" + replace);
        }
        f.reset();
        var source = source(f, f.base.west(7)); var relay = pulseRelay(f, f.base); graphTick(f, relay);
        BlockPos consumer = f.base.east(7);
        relay.query = () -> f.level.setBlock(consumer, Blocks.STONE.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        f.check(VisNetHandler.drainVis(f.level, consumer, Aspect.EARTH, 4) == 0
                        && source.getAvailableVis().getAmount(Aspect.EARTH) == 8 && f.level.getBlockState(consumer).is(Blocks.STONE),
                "actual consumer identity/state captured BEFORE provider metadata call-out; synchronous replacement refuses BEFORE native source debit");

        // Public setters reject cycles and duplicate edges using identity-safe
        // production walks, without corrupting private graph fields by reflection.
        f.put(consumer, Blocks.AIR);
        var child = VisNetRuntimeChecks.relay(f, f.base.east(6), (byte) -1); VisNetRuntimeChecks.relayTick(f, child);
        relay.setParent(new WeakReference<>(child));
        f.check(VisNetRuntimeChecks.parent(relay) == source && child.getRootSource().get() == source,
                "attempted descendant->ancestor cycle is refused, valid root chain stays finite");
        for (int i = 0; i < 64; i++) child.setParent(new WeakReference<>(relay));
        f.check(relay.getChildren().stream().filter(ref -> ref.get() == child).count() == 1
                        && child.consumeVis(Aspect.EARTH, 99) == 8,
                "repeated same-parent setter cannot create duplicate edges or multiply same-root budget; bounded walk, no overflow");
        var expired = new WeakReference<TileVisNode>(relay); expired.clear();
        f.check(!VisNetHandler.isNodeValid(expired), "explicitly expired weak INPUT is dereferenced/refused without null race");

        f.reset(); source = source(f, f.base.west(4)); relay = pulseRelay(f, f.base); relay.range = 2;
        graphTick(f, relay);
        f.check(VisNetRuntimeChecks.parent(relay) == null, "attachment uses CHILD range2, not the source's wider range8");
        relay.range = 8; relay.nodeRefresh = true; graphTick(f, relay);
        f.check(VisNetRuntimeChecks.parent(relay) == source, "declared API child-range policy8 now permits SAME actual four-block attachment");
    }

    private static void currentReloadAndIsolation(Fixture f) {
        f.reset(); var source = source(f, f.base.west(7));
        var relay = VisNetRuntimeChecks.relay(f, f.base, (byte) 1); VisNetRuntimeChecks.relayTick(f, relay);
        var relaySave = relay.saveWithFullMetadata(f.level.registryAccess());
        f.check(!relaySave.contains("VisParent") && !relaySave.contains("HasVisParent") && relay.getParent() != null,
                "SERVER disk relay has no persisted Java pointer or trusted parent coordinates; live graph exists only in memory");
        var oldRelay = relay;
        relay = NodeTransductionRuntimeChecks.reload(f, relay, relaySave, TileVisRelay.class);
        f.check(relay.getParent() == null && relay.getNodeCounter() == 0 && relay.getAttunement() == 1
                        && oldRelay.consumeVis(Aspect.EARTH, 8) == 0,
                "actual current-BE replacement preserves policy but rebuilds fresh graph, refuses stale old relay; NOT full chunk unload");
        VisNetRuntimeChecks.relayTick(f, relay);
        f.check(VisNetRuntimeChecks.parent(relay) == source && relay.consumeVis(Aspect.EARTH, 3) == 3,
                "first actual native reload tick rebuilds registered source route, no persisted-pointer authority");
        var sourceSave = source.saveWithFullMetadata(f.level.registryAccess());
        source = NodeTransductionRuntimeChecks.reload(f, source, sourceSave, TileNodeEnergized.class);
        f.check(source.getAvailableVis().visSize() == 0 && relay.consumeVis(Aspect.EARTH, 1) == 0,
                "source native replacement loses unused pulse and invalidates downstream route BEFORE fresh source tick");
        NodeTransductionRuntimeChecks.sourceTick(f, source); VisNetRuntimeChecks.relayTick(f, relay);
        f.check(relay.consumeVis(Aspect.EARTH, 1) == 1, "source first actual refill plus relay rebuild restores live output only once");

        int loaded = f.level.getChunkSource().getLoadedChunksCount();
        BlockPos unknown = new BlockPos(20_000_000, f.base.getY(), -20_000_000);
        f.check(!f.level.hasChunkAt(unknown), "explicit UNKNOWN consumer terrain input is genuinely not already loaded");
        for (int i = 0; i < 8; i++) f.check(VisNetHandler.drainVis(f.level, unknown, Aspect.EARTH, 99) == 0, "unloaded target cannot route or force a terrain read");
        f.check(!f.level.hasChunkAt(unknown) && loaded == f.level.getChunkSource().getLoadedChunksCount(),
                "actual repeated unknown-target requests force ZERO native terrain loads (not a simulated full unload)");

        var failure = new AtomicReference<Throwable>(); var checkedSource = source;
        Thread offThread = new Thread(() -> {
            try { VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 1); failure.set(new AssertionError("Off-thread mutation accepted")); }
            catch (IllegalStateException expected) { /* native confinement refusal */ }
            catch (Throwable unexpected) { failure.set(unexpected); }
        }, "m09b-off-thread-refusal");
        int stock = source.getAvailableVis().getAmount(Aspect.EARTH); offThread.start();
        try { offThread.join(5000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
        f.check(!offThread.isAlive() && failure.get() == null && checkedSource.getAvailableVis().getAmount(Aspect.EARTH) == stock,
                "actual off-thread call is REFUSED before native stock/graph mutation, no leaked worker or stock loss");

        long oldLifetime = VisNetHandler.getLevelLifetime(f.level);
        f.check(VisNetHandler.getSources(f.level).size() == 1, "level-clear case owns the ONLY registered root; separate wrapper batch required");
        VisNetHandler.clearLevel(f.level);
        f.check(VisNetHandler.getSources(f.level).isEmpty() && relay.getParent() == null
                        && VisNetHandler.drainVis(f.level, f.base.east(7), Aspect.EARTH, 1) == 0,
                "actual native level-lifetime clear removes all source/parent/cache authority, not just a numeric dimension source map");
        NodeTransductionRuntimeChecks.sourceTick(f, source); VisNetRuntimeChecks.relayTick(f, relay);
        f.check(VisNetHandler.getLevelLifetime(f.level) > oldLifetime && relay.consumeVis(Aspect.EARTH, 1) == 1,
                "same native level creates NEW network lifetime and rebuilds ONLY from actual registered ticks; not an actual new-server claim");
        var other = f.level.getServer().getLevel(f.level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER);
        f.check(other != null && other != f.level && VisNetHandler.drainVis(other, f.base.east(7), Aspect.EARTH, 1) == 0
                        && VisNetHandler.getSources(other).stream().noneMatch(ref -> ref.get() == checkedSource),
                "actual other server dimension cannot see this live root at the SAME reachable consumer coordinate");
    }

    /** Explicit, restored INPUT at the actual native ChunkMap visibility field
     * AND its four-entry ServerChunkCache lookup cache. Neither is a real unload.
     * This tests UNKNOWN lookups and ray admission, NOT full unload/save/reload,
     * chunk ticket removal, IO or a new ServerLevel. It never swaps a fake world
     * into the server and never feeds detached BEs to production. */
    private static void nativeVisibilityAbsence(Fixture f) {
        f.reset();
        BlockPos start = null;
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            BlockPos candidate = f.base.offset(x, 0, z);
            if (Math.floorMod(candidate.getX(), 16) == 15 && Math.floorMod(candidate.getZ(), 16) == 14) start = candidate;
        }
        f.check(start != null, "owned loaded footprint contains the native chunk-corner ray input");
        var source = source(f, start);
        BlockPos target = start.east(2).south(3);
        var relay = VisNetRuntimeChecks.relay(f, target, (byte) -1); VisNetRuntimeChecks.relayTick(f, relay);
        f.check(VisNetHandler.canNodeBeSeen(source, relay) && VisNetRuntimeChecks.parent(relay) == source,
                "actual known source/relay endpoints have clear native center ray across the third chunk at the corner");
        long epoch = source.getRefillEpoch();
        var converter = (thaumcraft.common.tiles.TileNodeConverter) NodeTransductionRuntimeChecks.existing(f, start.above());
        int count = converter.getCount();
        BlockPos missingRay = start.east(); // east chunk, still north of z16; neither endpoint's chunk
        try (var absent = new VisibilityAbsence(f, missingRay)) {
            f.check(f.level.getChunkSource().getChunkNow(start.getX() >> 4, start.getZ() >> 4) != null
                            && f.level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) != null,
                    "BOTH actual endpoints remain loaded/current while ONLY an intermediate ray/power chunk is UNKNOWN");
            int loaded = f.level.getChunkSource().getLoadedChunksCount();
            f.check(!VisNetHandler.canNodeBeSeen(source, relay)
                            && VisNetHandler.drainVis(f.level, target.south(), Aspect.EARTH, 1) == 0,
                    "unknown INTERMEDIATE ray chunk refuses LOS AND cached routing without forcing terrain; no extra opaque consumer-LOS rule");
            NodeTransductionRuntimeChecks.sourceTick(f, source); converter.checkStatus();
            f.check(!source.isRemoved() && !source.isQuarantined() && source.getRefillEpoch() == epoch
                            && converter.getCount() == count && source.getBlockState().is(ThaumcraftNodeDevices.ENERGIZED_NODE.get())
                            && source.consumeVis(Aspect.EARTH, 1) == 0
                            && f.level.getChunkSource().getLoadedChunksCount() == loaded,
                    "UNKNOWN support-power neighborhood is NOT destruction: no explosion/debit/new epoch or implicit terrain load");
        }
        f.check(VisNetHandler.canNodeBeSeen(source, relay)
                        && VisNetHandler.drainVis(f.level, target.south(), Aspect.EARTH, 1) == 1,
                "restoring EXACT native visibility owner invalidates cached negative route, preserves coherent tower and prior remaining pulse");
    }

    /** Suggested timeout240. Real source/relay/consumer tickers; no progress or
     * counter writes. Declared RAW essentia1 is the only material crystal input. */
    public static void crystallizer(GameTestHelper test) {
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open(); acceleratedFillAndDurableOutput(f); baselineAndReload(f); competingConsumers(f); consumerCallbackReplacement(f);
        }
        test.succeed();
    }

    private static void acceleratedFillAndDurableOutput(Fixture f) {
        f.reset(); var source = source(f, f.base.west(7));
        var relay = VisNetRuntimeChecks.relay(f, f.base, (byte) 1); VisNetRuntimeChecks.relayTick(f, relay);
        var machine = CrystallizerRuntimeChecks.machine(f, f.base.east(7), Direction.DOWN);
        f.put(machine.getBlockPos().below(), ThaumcraftAlembics.ALEMBIC.get());
        var raw = (TileAlembic) f.level.getBlockEntity(machine.getBlockPos().below());
        f.check(raw.addToContainer(Aspect.MAGIC, 1) == 0, "declared RAW essentiaSERVER input1 in REAL native alembic, not a finished crystal");
        int totalRawVis = 0;
        for (int tick = 1; tick <= 65; tick++) {
            NodeTransductionRuntimeChecks.sourceTick(f, source); VisNetRuntimeChecks.relayTick(f, relay);
            int before = source.getAvailableVis().getAmount(Aspect.EARTH);
            CrystallizerRuntimeChecks.tick(f, machine);
            int grant = before - source.getAvailableVis().getAmount(Aspect.EARTH); totalRawVis += grant;
            if (tick < 5) f.check(machine.getAspect() == null && machine.getProgress() == 0 && raw.getAmount() == 1, "native ticks1..4 do not fill/advance");
            if (tick == 5) f.check(machine.getAspect() == Aspect.MAGIC && machine.getProgress() == 0 && raw.getAmount() == 0 && grant == 0,
                    "native fill tick5 debits EXACT raw1 and resets progress0, no same-fill acceleration");
            if (tick >= 10 && tick < 65 && tick % 5 == 0) f.check(grant == 8 && machine.getProgress() == (tick / 5 - 1) * 17,
                    "real EARTH8 gives+17 ONLY on fifth production phases, via REQUIRED fourteen-block relay path");
            if (tick == 64) f.check(machine.getProgress() == 187 && machine.getAspect() == Aspect.MAGIC && f.pendingRows().isEmpty(),
                    "eleven actual accelerated steps reach187, NO early crystal at tick64");
            if (tick == 65) f.check(grant == 6 && machine.getAspect() == null && machine.getProgress() == 0
                            && machine.getTickCount() == 65 && f.pendingRows().size() == 1,
                    "step12 requests MIN20/MAX1/(200-187)/2=6, adds13 and completes EXACT tick65 into ONE durable escrow");
        }
        f.check(totalRawVis == 94 && raw.getAmount() == 0 && CrystallizerRuntimeChecks.crystalDrops(f) == 0,
                "eleven8+final6 actual raw grants94, paid input1 retired BEFORE physical output callback; no x100 units or free output");
        var row = f.pendingRows().getFirst();
        f.flushFresh(); var output = CrystallizerOutputRuntimeChecks.onlyCrystal(f);
        CrystallizerRuntimeChecks.assertTyped(f, output.getItem(), Aspect.MAGIC, 1); CrystallizerRuntimeChecks.roundTrip(f, output.getItem());
        f.check(output.getUUID().equals(row.getUUID("Id")) && f.pendingRows().isEmpty(), "SAME durable escrow UUID delivered exactly once, unchanged accepted typed1 adapter");
        f.flushFresh(); f.check(CrystallizerRuntimeChecks.crystalDrops(f) == 1, "another delivery tick cannot duplicate paid accelerated output");
        output.discard();
    }

    private static void baselineAndReload(Fixture f) {
        f.reset(); var machine = CrystallizerRuntimeChecks.machine(f, f.base.east(7), Direction.DOWN);
        f.check(machine.addToContainer(Aspect.FIRE, 1) == 0, "declared direct pending RAW input1, not output/progress");
        CrystallizerRuntimeChecks.ticks(f, machine, 999);
        f.check(machine.getProgress() == 199 && machine.getAspect() == Aspect.FIRE && f.pendingRows().isEmpty(),
                "NO source/graph still EXACT baseline1000:999 ticks produce199, no opportunistic stale source acceleration");
        CrystallizerRuntimeChecks.tick(f, machine);
        f.check(machine.getAspect() == null && machine.getTickCount() == 1000 && f.pendingRows().size() == 1, "actual baseline tick1000 pays SAME typed1 durable output");
        f.flushFresh(); var output = CrystallizerOutputRuntimeChecks.onlyCrystal(f); CrystallizerRuntimeChecks.assertTyped(f, output.getItem(), Aspect.FIRE, 1); output.discard();

        f.reset(); var source = source(f, f.base.west(7));
        var relay = VisNetRuntimeChecks.relay(f, f.base, (byte) -1); VisNetRuntimeChecks.relayTick(f, relay);
        machine = CrystallizerRuntimeChecks.machine(f, f.base.east(7), Direction.DOWN); machine.addToContainer(Aspect.ENERGY, 1);
        driver(f, source, relay, machine, 5);
        f.check(machine.getProgress() == 17, "pending direct input has actual accelerated first step at5");
        BlockPos power = machine.getBlockPos().north(); f.put(power, Blocks.REDSTONE_BLOCK);
        f.check(machine.isPowered(), "actual adjacent consumer redstone power, not an injected pause flag");
        driver(f, source, relay, machine, 8);
        f.check(machine.getTickCount() == 13 && machine.getProgress() == 17, "POWER pauses accelerated progress but native count continues5->13");
        f.put(power, Blocks.AIR); driver(f, source, relay, machine, 2);
        f.check(machine.getTickCount() == 15 && machine.getProgress() == 34, "unpower inherits original fifth phase, no fake timer restart");
        var tag = machine.saveWithFullMetadata(f.level.registryAccess());
        machine = NodeTransductionRuntimeChecks.reload(f, machine, tag, TileEssentiaCrystalizer.class);
        f.check(machine.getAspect() == Aspect.ENERGY && machine.getProgress() == 0 && machine.getTickCount() == 0,
                "ACTUAL native pending-machine reload keeps paid input only, transient accelerated progress/count reset0");
        driver(f, source, relay, machine, 59);
        f.check(machine.getProgress() == 187 && machine.getAspect() == Aspect.ENERGY && f.pendingRows().isEmpty(), "direct pending reload still needs sixty actual assisted ticks, not an immediate old-progress crystal");
        driver(f, source, relay, machine, 1);
        f.check(machine.getTickCount() == 60 && machine.getAspect() == null && f.pendingRows().size() == 1, "actual assisted pending-input output at60, separate from native empty-fill65");
        f.flushFresh(); output = CrystallizerOutputRuntimeChecks.onlyCrystal(f); CrystallizerRuntimeChecks.assertTyped(f, output.getItem(), Aspect.ENERGY, 1); output.discard();
    }

    private static void competingConsumers(Fixture f) {
        f.reset(); var source = source(f, f.base.west(7));
        var relay = VisNetRuntimeChecks.relay(f, f.base, (byte) -1); VisNetRuntimeChecks.relayTick(f, relay);
        var a = CrystallizerRuntimeChecks.machine(f, f.base.east(7), Direction.DOWN);
        var b = CrystallizerRuntimeChecks.machine(f, f.base.east(7).south(), Direction.DOWN);
        a.addToContainer(Aspect.FIRE, 1); b.addToContainer(Aspect.WATER, 1); // two declared raw inputs
        for (int tick = 1; tick <= 10; tick++) {
            NodeTransductionRuntimeChecks.sourceTick(f, source); VisNetRuntimeChecks.relayTick(f, relay);
            int before = source.getAvailableVis().getAmount(Aspect.EARTH);
            if (tick <= 5) { CrystallizerRuntimeChecks.tick(f, a); CrystallizerRuntimeChecks.tick(f, b); }
            else { CrystallizerRuntimeChecks.tick(f, b); CrystallizerRuntimeChecks.tick(f, a); }
            int actual = before - source.getAvailableVis().getAmount(Aspect.EARTH);
            f.check(actual == (tick % 5 == 0 ? 8 : 0), "TWO actual consumers share source epoch8, never each receive an independent8 grant");
            if (tick == 5) f.check(a.getProgress() == 17 && b.getProgress() == 1, "first actual ordering yields17+1, not fictitious17+17");
        }
        f.check(a.getProgress() == 18 && b.getProgress() == 18 && a.getAspect() == Aspect.FIRE && b.getAspect() == Aspect.WATER && f.pendingRows().isEmpty(),
                "reverse next native fixture order shares second epoch honestly: both progress18, original raw types retained/no premature output");
    }

    private static void consumerCallbackReplacement(Fixture f) {
        f.reset(); var source = source(f, f.base.west(7)); var relay = pulseRelay(f, f.base); graphTick(f, relay);
        var machine = CrystallizerRuntimeChecks.machine(f, f.base.east(7), Direction.DOWN); machine.addToContainer(Aspect.MAGIC, 1);
        CrystallizerRuntimeChecks.ticks(f, machine, 4);
        var replacement = new TileEssentiaCrystalizer[1];
        relay.pulse = () -> {
            f.check(source.getAvailableVis().getAmount(Aspect.EARTH) == 0 && machine.getProgress() == 0
                            && !machine.takeFromContainer(Aspect.MAGIC, 1),
                    "production consumer callback sees source8 ALREADY paid; drawingVis refuses swapping original input during in-flight credit");
            BlockPos pos = machine.getBlockPos(); f.put(pos, Blocks.STONE);
            replacement[0] = CrystallizerRuntimeChecks.machine(f, pos, Direction.DOWN);
            throw new PulseFailure("m09b-replaced-crystallizer-pulse-fixture");
        };
        CrystallizerRuntimeChecks.tick(f, machine);
        f.check(replacement[0] != null && replacement[0].getAspect() == null && replacement[0].getProgress() == 0
                        && replacement[0].getTickCount() == 0 && machine.getProgress() == 0
                        && source.getAvailableVis().getAmount(Aspect.EARTH) == 0 && f.pendingRows().isEmpty(),
                "actual consumer replacement after debit cannot inherit +17/old input/output or refund source8; old destroyed resource is not resurrected");

        // Actual consumer integration with malformed provider reports remains
        // baseline+1, not clamped/free bonus. This is API-adversary coverage ONLY.
        for (int reported : new int[] {-1, 21}) {
            f.reset(); var provider = grantProbe(f, f.base.west(7), Aspect.EARTH, reported); graphTick(f, provider);
            var route = pulseRelay(f, f.base); graphTick(f, route);
            var into = CrystallizerRuntimeChecks.machine(f, f.base.east(7), Direction.DOWN); into.addToContainer(Aspect.FIRE, 1);
            CrystallizerRuntimeChecks.ticks(f, into, 5);
            f.check(into.getProgress() == 1 && into.getAspect() == Aspect.FIRE && provider.calls == 1 && f.pendingRows().isEmpty(),
                    "native crystallizer rejects WHOLE invalid provider grant and earns only baseline1: " + reported);
        }
    }

    private static TileNodeEnergized source(Fixture f, BlockPos pos) {
        var source = NodeTransductionRuntimeChecks.energize(f, pos, new AspectList().add(Aspect.EARTH, 64), null, NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source); return source;
    }
    private static void driver(Fixture f, TileNodeEnergized source, TileVisRelay relay, TileEssentiaCrystalizer machine, int ticks) {
        for (int i = 0; i < ticks; i++) { NodeTransductionRuntimeChecks.sourceTick(f, source); VisNetRuntimeChecks.relayTick(f, relay); CrystallizerRuntimeChecks.tick(f, machine); }
    }
    private static PulseRelay pulseRelay(Fixture f, BlockPos pos) {
        f.put(pos.below(), Blocks.STONE); f.put(pos, Blocks.AIR);
        var state = ThaumcraftNodeDevices.VIS_RELAY.get().defaultBlockState().setValue(BlockVisRelay.FACING, Direction.UP);
        f.level.setBlock(pos, state, TubeVariantsRuntimeChecks.QUIET); f.level.removeBlockEntity(pos);
        var probe = new PulseRelay(pos, state); f.level.setBlockEntity(probe); return probe;
    }
    private static GrantProbe grantProbe(Fixture f, BlockPos pos, Aspect type, int grant) {
        f.put(pos, Blocks.AIR); f.level.setBlock(pos, ThaumcraftNodeDevices.ENERGIZED_NODE.get().defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        f.level.removeBlockEntity(pos); var probe = new GrantProbe(pos, f.level.getBlockState(pos), type, grant); f.level.setBlockEntity(probe); return probe;
    }
    private static void graphTick(Fixture f, TileVisNode node) { TileVisNode.tick(f.level, node.getBlockPos(), node.getBlockState(), node); }

    /** Native registered-type installed graph participant. Only callbacks differ;
     * production final consume/attachment/debit logic is never overridden. */
    private static final class PulseRelay extends TileVisNode {
        Runnable pulse, query; int pulses, range = 8;
        PulseRelay(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), pos, state); }
        @Override public int getRange() {
            if (query != null) {
                var once = query; query = null;
                try { once.run(); }
                catch (RuntimeException unexpected) { throw new AssertionError("M09b metadata callback assertion/action failed", unexpected); }
            }
            return range;
        }
        @Override public boolean isSource() { return false; }
        @Override public void triggerConsumeEffect(Aspect aspect) {
            ++pulses;
            if (pulse == null) return;
            var once = pulse; pulse = null;
            try { once.run(); }
            catch (PulseFailure expected) { throw expected; } // ONLY intentional FX failure may be swallowed by production.
            catch (RuntimeException unexpected) { throw new AssertionError("M09b pulse callback assertion/action failed", unexpected); }
        }
    }
    private static final class PulseFailure extends IllegalStateException { PulseFailure(String message) { super(message); } }
    /** Deliberately untrusted API provider; never stands in for built-in source
     * conversion, source lifecycle, generation or client acceptance evidence. */
    private static final class GrantProbe extends TileVisNode {
        final Aspect type; final int report; int calls;
        GrantProbe(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, Aspect type, int report) { super(ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get(), pos, state); this.type = type; this.report = report; }
        @Override public int getRange() { return 8; }
        @Override public boolean isSource() { return true; }
        @Override protected int consumeFromSource(Aspect aspect, int amount) { ++calls; return aspect == type ? report : 0; }
    }

    private static final class VisibilityAbsence implements AutoCloseable {
        final Fixture f; final Object chunkMap; final Field field;
        final it.unimi.dsi.fastutil.longs.Long2ObjectMap<Object> visible;
        final long key; final Object owner;
        final Object chunkSource; final Field positionsField, statusesField, chunksField;
        final long[] positions, savedPositions;
        final Object[] statuses, savedStatuses, chunks, savedChunks;
        @SuppressWarnings("unchecked") VisibilityAbsence(Fixture f, BlockPos pos) {
            this.f = f; key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            chunkSource = f.level.getChunkSource();
            try {
                Field chunkMapField = chunkSource.getClass().getDeclaredField("chunkMap"); chunkMapField.setAccessible(true);
                chunkMap = chunkMapField.get(chunkSource);
                field = chunkMap.getClass().getDeclaredField("visibleChunkMap"); field.setAccessible(true);
                visible = (it.unimi.dsi.fastutil.longs.Long2ObjectMap<Object>) field.get(chunkMap);
                // Native1.21.1 getChunkNow checks these BEFORE visibleChunkMap.
                // Removing only the holder does not actually make terrain unknown.
                positionsField = chunkSource.getClass().getDeclaredField("lastChunkPos"); positionsField.setAccessible(true);
                statusesField = chunkSource.getClass().getDeclaredField("lastChunkStatus"); statusesField.setAccessible(true);
                chunksField = chunkSource.getClass().getDeclaredField("lastChunk"); chunksField.setAccessible(true);
                positions = (long[]) positionsField.get(chunkSource); savedPositions = positions.clone();
                statuses = (Object[]) statusesField.get(chunkSource); savedStatuses = statuses.clone();
                chunks = (Object[]) chunksField.get(chunkSource); savedChunks = chunks.clone();
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot locate actual native visibility field for restored INPUT", failure); }
            owner = visible.remove(key);
            for (int i = 0; i < positions.length; i++) if (positions[i] == key) {
                positions[i] = ChunkPos.INVALID_CHUNK_POS; statuses[i] = null; chunks[i] = null;
            }
            try {
                f.check(owner != null && f.level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null,
                        "real native getChunkNow observes explicit visibility absence; not a stubbed hasChunkAt return");
            } catch (RuntimeException | Error failure) {
                if (owner != null) visible.put(key, owner);
                restoreCache(); throw failure;
            }
        }
        private void restoreCache() {
            System.arraycopy(savedPositions, 0, positions, 0, positions.length);
            System.arraycopy(savedStatuses, 0, statuses, 0, statuses.length);
            System.arraycopy(savedChunks, 0, chunks, 0, chunks.length);
        }
        @Override public void close() {
            boolean owned;
            try { owned = field.get(chunkMap) == visible && !visible.containsKey(key)
                    && positionsField.get(chunkSource) == positions && statusesField.get(chunkSource) == statuses && chunksField.get(chunkSource) == chunks; }
            catch (IllegalAccessException failure) { visible.put(key, owner); restoreCache(); throw new IllegalStateException(failure); }
            visible.put(key, owner); // Restore BEFORE asserting, even on fixture failure.
            restoreCache();
            f.check(owned && visible.get(key) == owner && java.util.Arrays.equals(positions, savedPositions)
                            && java.util.Arrays.equals(statuses, savedStatuses) && java.util.Arrays.equals(chunks, savedChunks),
                    "finally restores EXACT original native ChunkHolder/map entry AND lookup-cache arrays/entries; no fake full-unload claim");
        }
    }
}

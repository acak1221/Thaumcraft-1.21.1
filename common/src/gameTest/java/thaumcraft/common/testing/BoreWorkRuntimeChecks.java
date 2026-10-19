package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockArcaneBore;
import thaumcraft.common.blocks.BlockArcaneBoreBase;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.mixin.PersistentEntitySectionManagerAccess;
import thaumcraft.common.mixin.ServerLevelEntityManagerAccess;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileJar;

/** NEW unexecuted native helpers. Root registers isolated wrappers after real
 * origin/lifecycle/platform wiring. No manual ticker, permission booleans, clock,
 * RNG, target/count/credit setters, loot grant or synthetic native result.
 * Fixtures are explicit native-test inputs, NOT survival recipe acquisition. */
public final class BoreWorkRuntimeChecks {
    private BoreWorkRuntimeChecks() {}
    private static final int UPDATE = Block.UPDATE_ALL;

    public static void externalRequestsCannotBorrowNativeOrigin(GameTestHelper test) {
        try (Fixture f = new Fixture(test)) {
            f.tools();
            ItemStack focus = f.top.getItem(0), pick = f.top.getItem(1);
            long revision = f.top.revision();
            test.assertTrue(BoreNativeContext.isCurrentOwner(f.top) && !BoreNativeContext.isNativeTick(f.top),
                    "a real installed BE is not an actual native ticker invocation");
            BoreWork.prefill(f.top); BoreWork.finish(f.top); BoreWork.reconcileLoaded(f.top);
            test.assertTrue(denied(() -> BoreWork.evacuate(f.top)) && denied(f.top::workSnapshot)
                            && denied(f.top::retirementSnapshot), "public request/read entry points cannot manufacture native phase or retirement");
            test.assertTrue(f.top.revision() == revision && f.top.getAccelerationCredit() == 0
                            && ItemStack.matches(focus, f.top.getItem(0)) && ItemStack.matches(pick, f.top.getItem(1))
                            && f.top.workReference() == null && !f.top.wasEvacuated(),
                    "denied external work preserves exact real slots, credit, sequence effects and ownership");
            test.assertTrue(DeferredItemDrops.boreJournalView(test.getLevel()).stream().noneMatch(row -> row.getUUID("Machine").equals(f.top.machineIdentity())),
                    "external requests never publish a candidate or witnessed output");
        } // Cleanup must finish BEFORE the native PASS.
        test.succeed();
    }

    /** Root wrapper needs >=200 genuine world ticks and a naturally loaded lane. */
    public static void ordinaryOreUsesRealNativeTicksAndNoXp(GameTestHelper test) {
        Fixture f = new Fixture(test); f.tools();
        ItemStack originalFocus = f.top.getItem(0);
        f.put(f.target, Blocks.DIAMOND_ORE.defaultBlockState());
        f.power(true);
        // The scheduler, spiral/ray and countdown choose/complete the actual
        // +3 UP target. This poll never invokes TileArcaneBore.tick or BoreWork.
        test.succeedWhen(() -> {
            f.requireOpen();
            test.assertTrue(f.top.getItem(1).getDamageValue() == 1, "one native low-tier pick completion has occurred: current="
                    + f.top.isCurrent() + ", powered=" + f.top.isPowered() + ", fenced=" + f.top.isQuarantined()
                    + ", bound=" + (f.top.accelerationBase() != null) + ", count=" + f.top.getCountdown()
                    + ", target=" + f.top.selectedTarget() + ", plane=" + f.top.scanCursor()
                    + ", rotation=" + f.top.rotation() + ", revision=" + f.top.revision());
            f.pauseOnce(); // Do not repeat no-op state requests while awaiting level-post delivery.
            test.assertTrue(test.getLevel().getBlockState(f.target).isAir(), "the actual ordinary ore was natively removed");
            test.assertTrue(f.top.getAccelerationCredit() == 0 && ItemStack.matches(originalFocus, f.top.getItem(0)),
                    "unassisted completion neither charges focus/vis nor fabricates acceleration");
            List<ItemEntity> physical = test.getLevel().getEntitiesOfClass(ItemEntity.class, f.area);
            test.assertTrue(physical.size() == 1 && physical.getFirst().getItem().is(Items.DIAMOND)
                            && physical.getFirst().getItem().getCount() == 1,
                    "one durable native diamond output exists; wooden-pick tier is not a Bore gate");
            test.assertTrue(test.getLevel().getEntitiesOfClass(ExperienceOrb.class, f.area).isEmpty(),
                    "ore native spawnAfterBreak/playerDestroy XP was never invoked");
            List<CompoundTag> journals = DeferredItemDrops.boreJournalView(test.getLevel()).stream()
                    .filter(row -> row.getUUID("Machine").equals(f.top.machineIdentity()) && row.getString("Mode").equals("HARVEST")).toList();
            test.assertTrue(journals.size() == 1, "exactly one real completion journal");
            CompoundTag journal = journals.getFirst();
            test.assertTrue(journal.getString("Phase").equals("WITNESSED") && !journal.getBoolean("Fenced")
                            && journal.getLong("Target") == f.target.asLong() && journal.getInt("CreditBits") == Float.floatToIntBits(0)
                            && journal.getInt("Nozzle") == Direction.EAST.get3DDataValue()
                            && journal.getInt("Attachment") == Direction.UP.get3DDataValue()
                            && journal.getList("Rows", Tag.TAG_COMPOUND).size() == 1,
                    "durable exact true witness binds real machine/target/reservation/nozzle/attachment");
            CompoundTag segment = journal.getList("Rows", Tag.TAG_COMPOUND).getCompound(0);
            test.assertTrue(segment.getInt("Count") == 1 && segment.getCompound("Bore").getInt("Member") == 0
                            && segment.getCompound("Bore").getInt("Segment") == 0
                            && segment.getUUID("Id").equals(physical.getFirst().getUUID()),
                    "the one ordinary drop is a fixed-UUID native-save-safe physical segment, not an over99 inventory-item workaround");
            ItemStack before = ItemStack.parse(test.getLevel().registryAccess(), journal.getCompound("PickBefore")).orElseThrow();
            ItemStack after = ItemStack.parse(test.getLevel().registryAccess(), journal.getCompound("PickAfter")).orElseThrow();
            test.assertTrue(before.getDamageValue() == 0 && after.getDamageValue() == 1 && ItemStack.matches(after, f.top.getItem(1)),
                    "the installed actual tool is the already-worn reservation, not an unworn escaped preparation copy");
            journal.putString("Phase", "ARMED"); journal.getList("Rows", Tag.TAG_COMPOUND).clear();
            test.assertTrue(DeferredItemDrops.boreJournalView(test.getLevel()).stream().anyMatch(row -> row.getUUID("Machine").equals(f.top.machineIdentity())
                            && row.getString("Phase").equals("WITNESSED") && row.getList("Rows", Tag.TAG_COMPOUND).size() == 1),
                    "read-only diagnostics cannot mutate queue ownership");
            f.close(); // BEFORE succeedWhen's implicit native PASS.
        });
    }

    public static void baseEssentiaNeedsLazyBindingAndThenPrefillsWhileOff(GameTestHelper test) {
        Fixture f = new Fixture(test);
        BlockPos jarPos = f.jar;
        f.put(jarPos, ThaumcraftContent.JAR.get().defaultBlockState());
        TileJar jar = (TileJar) test.getLevel().getBlockEntity(jarPos);
        test.assertTrue(jar != null && jar.addToContainer(Aspect.ENTROPY, 1) == 0, "explicit real native fixture input Perditio1");
        test.runAfterDelay(5, () -> {
            if (f.closed) return; // Failed/timed-out/rerun fixtures must never write again.
            test.assertTrue(jar.getAmount() == 1 && f.top.getAccelerationCredit() == 0,
                    "an installed but not lazily bound base cannot supply initial off/invalid-tool prefill");
            f.tools(); f.put(f.target, Blocks.STONE.defaultBlockState()); f.power(true);
        });
        test.succeedWhen(() -> {
            f.requireOpen();
            if (f.top.accelerationBase() != null && jar.getAmount() == 1) f.pauseOnce();
            test.assertTrue(!f.top.isPowered() && jar.getAmount() == 0 && f.top.getAccelerationCredit() == 20,
                    "after observed lazy binding, native REDSTONE-OFF prefill paid one real source unit for credit20");
            test.assertTrue(test.getLevel().getBlockState(f.target).is(Blocks.STONE) && f.top.getItem(1).getDamageValue() == 0,
                    "prefill is independent of completed mining and does not wear a pick");
            test.assertTrue(jar.getAmount() + (int) (f.top.getAccelerationCredit() / 20) == 1,
                    "closed source debit/top credit conserve the one declared physical essentia unit");
            f.close();
        });
    }

    private static boolean denied(Runnable action) {
        try { action.run(); return false; } catch (IllegalStateException expected) { return true; }
    }
    /** Explicit saved-input boundary fixture, then GENUINE native reconciliation
     * reaches MAX. Never manually set a live counter/phase or invoke its ticker. */
    public static void exhaustedRevisionRetainsOneOpaqueOwner(GameTestHelper test) {
        Fixture f = new Fixture(test); f.tools();
        ItemStack expectedFocus = f.top.getItem(0), expectedPick = f.top.getItem(1);
        CompoundTag saved = f.top.saveWithoutMetadata(f.level.registryAccess());
        saved.putLong("BoreRevision", Long.MAX_VALUE - 1);
        saved.put("BoreWorkReference", new CompoundTag()); // Missing witness: reconciliation must FENCE, not grant output.
        f.top.loadWithComponents(saved, f.level.registryAccess());
        test.assertTrue(f.top.revision() == Long.MAX_VALUE - 1 && f.top.needsLoadedReconciliation(),
                "explicit saved fixture retains tools and schedules native reference reconciliation");
        test.succeedWhen(() -> {
            f.requireOpen();
            test.assertTrue(f.top.revision() == Long.MAX_VALUE && !f.top.needsLoadedReconciliation(),
                    "actual native reconciliation consumes the final revision without restoring or refunding stock");
            var queue = f.level.getDataStorage().computeIfAbsent(Fixture.QUEUE, Fixture.QUEUE_ID);
            int before = queue.boreRetirementCustodyCount();
            test.assertTrue(f.level.setBlock(f.pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE),
                    "actual native onRemove at exhausted revision");
            test.assertTrue(f.top.wasEvacuated() && f.top.isEmpty() && queue.boreRetirementCustodyCount() == before + 1,
                    "MAX retirement has ONE opaque actual-stock owner and no dying BE copy");
            CompoundTag checkpoint = queue.save(new CompoundTag(), f.level.registryAccess());
            for (Tag raw : checkpoint.getList("Pending", Tag.TAG_COMPOUND))
                test.assertTrue(((CompoundTag) raw).getLong("Source") != f.pos.asLong(),
                        "revision exhaustion never publishes duplicate physical tool returns");
            int owned = 0;
            for (Tag raw : checkpoint.getList("Quarantined", Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) raw;
                if (!row.getString("Kind").equals("BORE_RETIREMENT_CUSTODY") || row.getLong("Source") != f.pos.asLong()
                        || !row.getUUID("Machine").equals(f.top.machineIdentity())) continue;
                ++owned;
                var focus = ItemStack.parse(f.level.registryAccess(), row.getCompound("Focus").getCompound("Stack")).orElseThrow();
                focus.setCount(row.getCompound("Focus").getInt("Count"));
                var pick = ItemStack.parse(f.level.registryAccess(), row.getCompound("Pick").getCompound("Stack")).orElseThrow();
                pick.setCount(row.getCompound("Pick").getInt("Count"));
                test.assertTrue(ItemStack.matches(focus, expectedFocus) && ItemStack.matches(pick, expectedPick),
                        "the sole opaque owner retains both complete actual tools");
            }
            test.assertTrue(owned == 1, "exactly one whole quarantine checkpoint for MAX retirement");
            f.close();
        });
    }
    static final class Fixture implements AutoCloseable {
        private static final String QUEUE_ID = "thaumcraft_pending_inventory_drops";
        private static final SavedData.Factory<DeferredItemDrops> QUEUE = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
        private final GameTestHelper test;
        private final GameTestInfo info;
        private final ServerLevel level;
        private final BlockPos pos, base, power, jar;
        final BlockPos target;
        TileArcaneBore top;
        final AABB area;
        private final Map<BlockPos, Cell> cells = new LinkedHashMap<>();
        private final Set<UUID> machines = new HashSet<>(), transactions = new HashSet<>(), outputs = new HashSet<>();
        private final Set<UUID> initialIds = new HashSet<>(), initialTransactions = new HashSet<>();
        private final Map<UUID, CompoundTag> outputRows = new HashMap<>();
        private final Map<UUID, Entity> observed = new HashMap<>();
        private final Map<UUID, ItemStack> observedItems = new HashMap<>();
        private final Map<UUID, CompoundTag> observedXp = new HashMap<>();
        private EntityEvent.Add emissionListener;
        private boolean observing;
        private boolean closed, closing, paused;
        private IllegalStateException cleanupFailure;
        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); info = testInfo(test); pos = test.absolutePos(new BlockPos(3, 4, 3));
            base = pos.below(); target = pos.above(3); power = base.west(); jar = base.below();
            area = new AABB(Vec3.atLowerCornerOf(pos.offset(-2, -3, -2)), Vec3.atLowerCornerOf(target.offset(2, 2, 2)));
            test.assertTrue(level.hasChunksAt(pos.offset(-2, -3, -2), target.offset(2, 2, 2)),
                    "fixture/native vacuum/ejection cells must already be loaded; no tickets or generated chunks");
            // Strong bounded refusal: never overwrite, detach, serialize/reload
            // or later "restore" somebody else's original BE. Capture all FIVE
            // potential inputs before the first write; unused cells stay untouched.
            for (BlockPos cell : List.of(pos, base, power, target, jar)) {
                requireLoaded(cell);
                BlockState state = level.getBlockState(cell);
                test.assertTrue(state.isAir() && !state.hasBlockEntity() && level.getBlockEntity(cell) == null,
                        "finite Bore fixture cell must initially be AIR/noBE: " + cell);
                cells.put(cell, new Cell(state));
            }
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty()
                            && level.getEntitiesOfClass(ExperienceOrb.class, area).isEmpty(),
                    "fixture starts without ambient item/XP ownership ambiguity; this is read-only, never an entity sweep");
            DeferredItemDrops queue = level.getDataStorage().get(QUEUE, QUEUE_ID);
            if (queue != null) protectInitial(queue.save(new CompoundTag(), level.registryAccess()).copy());
            synchronized (liveReceipts()) { initialIds.addAll(liveReceipts().getOrDefault(level, Set.of())); }
            // Native listener covers actual FAIL/TIMEOUT and rerun even when a
            // predicate never reaches its success cleanup. No mapped field name.
            info.addListener(new GameTestListener() {
                @Override public void testStructureLoaded(GameTestInfo actual) {}
                @Override public void testPassed(GameTestInfo actual, GameTestRunner runner) { checkInfo(actual); close(); }
                @Override public void testFailed(GameTestInfo actual, GameTestRunner runner) { checkInfo(actual); close(); }
                @Override public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) { checkInfo(previous); close(); }
            });
            emissionListener = (entity, world) -> {
                // Observe only NEW emissions in THIS machine's actual work
                // invocation at its finite target. Never claim ambient entities
                // merely because they happen to be inside a test bounding box.
                if (world == level && top != null && EssentiaTransfers.currentBoreFrame(top) != null
                        && entity.blockPosition().equals(target) && level.getEntity(entity.getUUID()) == null
                        && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
                    observed.put(entity.getUUID(), entity);
                    if (entity instanceof ItemEntity item) observedItems.put(entity.getUUID(), item.getItem().copy());
                    if (entity instanceof ExperienceOrb orb) observedXp.put(entity.getUUID(), xpMass(orb));
                }
                return EventResult.pass();
            };
            try {
                observing = true; EntityEvent.ADD.register(emissionListener);
                put(base, ThaumcraftArcaneBore.ARCANE_BORE_BASE.get().defaultBlockState().setValue(BlockArcaneBoreBase.NOZZLE, Direction.EAST));
                put(pos, ThaumcraftArcaneBore.ARCANE_BORE.get().defaultBlockState()
                        .setValue(BlockArcaneBore.MINING, Direction.UP).setValue(BlockArcaneBore.ATTACHMENT, Direction.UP));
                top = (TileArcaneBore) level.getBlockEntity(pos); rememberMachine(top);
                test.assertTrue(top != null && top.isCurrent(), "real registered installed native top/base, no detached synthetic owner");
            } catch (RuntimeException | Error failure) {
                try { close(); } catch (RuntimeException cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
                throw failure;
            }
        }
        void tools() {
            requireOpen();
            top.setItem(0, new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get()));
            top.setItem(1, new ItemStack(Items.WOODEN_PICKAXE));
            test.assertTrue(top.hasFocus() && top.hasPickaxe(), "native guarded fixture inventory accepts actual focus and pick");
        }
        void power(boolean enabled) { put(power, (enabled ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState()); }
        private void pauseOnce() { if (!paused) { power(false); paused = true; } }
        void requireOpen() {
            if (cleanupFailure != null) throw cleanupFailure;
            if (closed || closing) throw new IllegalStateException("Closed Bore fixture cannot resume");
        }
        private void requireLoaded(BlockPos cell) {
            if (!level.isInWorldBounds(cell) || !level.hasChunkAt(cell)) throw new IllegalStateException("Unavailable fixture cell; no force load: " + cell);
        }
        void put(BlockPos cell, BlockState desired) {
            requireOpen(); requireLoaded(cell);
            Cell owned = cells.get(cell);
            if (owned == null) throw new IllegalStateException("Write outside five-cell Bore fixture");
            BlockEntity current = level.getBlockEntity(cell);
            if (current != null && current != owned.created) throw new IllegalStateException("Unexpected BE in fixture input");
            BlockState state = level.getBlockState(cell);
            if (!owned.written && state != owned.original) throw new IllegalStateException("Fixture baseline changed before write");
            if (state == desired) return; // No repeated AIR/power state request.
            owned.written = true; owned.states.add(desired);
            try { level.setBlock(cell, desired, UPDATE); }
            finally {
                // Also record a native owner created before an exceptional setBlock
                // return; cleanup's later removals require THIS exact identity.
                if (level.hasChunkAt(cell) && level.getBlockState(cell) == desired && desired.hasBlockEntity()) {
                    BlockEntity created = level.getBlockEntity(cell);
                    if (owned.created == null) owned.created = created;
                    if (cell.equals(pos) && created instanceof TileArcaneBore bore) rememberMachine(bore);
                }
            }
            if (level.getBlockState(cell) != desired) throw new IllegalStateException("Native fixture placement did not install requested state");
        }
        private void rememberMachine(TileArcaneBore bore) {
            if (bore == null) return;
            UUID machine = bore.machineIdentity();
            if (!bore.getBlockPos().equals(pos)) throw new IllegalStateException("Wrong fixture machine position");
            machines.add(machine);
        }
        private void checkInfo(GameTestInfo actual) {
            if (actual != info) throw new IllegalStateException("Cleanup listener belongs to another GameTestInfo");
        }
        private void protectInitial(CompoundTag saved) {
            for (String collection : List.of("Pending", "Settled")) for (Tag raw : saved.getList(collection, Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) raw; if (row.hasUUID("Id")) initialIds.add(row.getUUID("Id"));
            }
            for (String collection : List.of("BoreJournals", "Quarantined")) for (Tag raw : saved.getList(collection, Tag.TAG_COMPOUND)) {
                CompoundTag row = (CompoundTag) raw;
                if (row.hasUUID("Id")) { initialTransactions.add(row.getUUID("Id")); initialIds.add(row.getUUID("Id")); }
                for (Tag candidate : row.getList("Rows", Tag.TAG_COMPOUND)) if (((CompoundTag) candidate).hasUUID("Id")) initialIds.add(((CompoundTag) candidate).getUUID("Id"));
            }
        }
        private boolean ownJournal(CompoundTag row) {
            return row.hasUUID("Machine") && machines.contains(row.getUUID("Machine")) && row.contains("Source", Tag.TAG_LONG)
                    && row.getLong("Source") == pos.asLong() && row.hasUUID("Id") && !initialTransactions.contains(row.getUUID("Id"));
        }
        private void collectOwned(CompoundTag saved) {
            for (String collection : List.of("BoreJournals", "Quarantined")) for (Tag raw : saved.getList(collection, Tag.TAG_COMPOUND)) {
                CompoundTag journal = (CompoundTag) raw;
                if (!ownJournal(journal)) continue;
                UUID transaction = journal.getUUID("Id"); transactions.add(transaction);
                for (Tag candidate : journal.getList("Rows", Tag.TAG_COMPOUND)) {
                    CompoundTag row = (CompoundTag) candidate;
                    if (!row.hasUUID("Id") || !row.contains("Source", Tag.TAG_LONG) || row.getLong("Source") != pos.asLong() || !row.getCompound("Bore").hasUUID("Transaction")
                            || !row.getCompound("Bore").getUUID("Transaction").equals(transaction)) throw new IllegalStateException("Unattributable fixture output row");
                    UUID id = row.getUUID("Id");
                    if (initialIds.contains(id)) throw new IllegalStateException("Fixture output collides with protected initial ownership");
                    outputs.add(id); outputRows.put(id, row.copy());
                }
            }
        }
        private boolean ownPending(CompoundTag row) {
            return row.hasUUID("Id") && outputs.contains(row.getUUID("Id")) && !initialIds.contains(row.getUUID("Id"))
                    && row.contains("Source", Tag.TAG_LONG) && row.getLong("Source") == pos.asLong() && row.getCompound("Bore").hasUUID("Transaction")
                    && transactions.contains(row.getCompound("Bore").getUUID("Transaction"));
        }
        @Override public void close() {
            if (closing) return;
            if (closed) { if (cleanupFailure != null) throw cleanupFailure; return; }
            closing = true; Throwable failure = null;
            try { if (observing) { EntityEvent.ADD.unregister(emissionListener); observing = false; } }
            catch (RuntimeException | Error problem) { failure = combine(failure, problem); }
            // Replace each exact installed owner in ONE native operation; do
            // not detach then look up a still-BE block (which can recreate it).
            // Declared final tools/essentia are disposable test inputs. Native
            // cleanup retirement may add an own-machine journal; collect it
            // below without counting this housekeeping as evacuation evidence.
            for (BlockPos cell : List.of(power, pos, base, jar, target)) {
                Cell owned = cells.get(cell); if (!owned.written) continue;
                try {
                    requireLoaded(cell);
                    BlockState current = level.getBlockState(cell);
                    BlockEntity actual = level.getBlockEntity(cell);
                    if ((actual != null && actual != owned.created) || !owned.states.contains(current))
                        throw new IllegalStateException("Refuse to overwrite unexpected cleanup cell: " + cell);
                    if (cell.equals(base) && (level.getBlockState(pos) != cells.get(pos).original || level.getBlockEntity(pos) != null))
                        throw new IllegalStateException("Retain base support under an unrestored/foreign top");
                    if (top != null && actual == top && top.inventoryAvailable()) {
                        top.removeItemNoUpdate(0); top.removeItemNoUpdate(1);
                    }
                    // Recheck after guarded slot-transfer notifications.
                    if (level.getBlockState(cell) != current || level.getBlockEntity(cell) != actual)
                        throw new IllegalStateException("Fixture owner/state changed during cleanup transfer");
                    if (level.getBlockState(cell) != owned.original) level.setBlock(cell, owned.original, UPDATE);
                    if (level.getBlockState(cell) != owned.original || level.getBlockEntity(cell) != null) throw new IllegalStateException("Fixture state restoration failed: " + cell);
                } catch (RuntimeException | Error problem) { failure = combine(failure, problem); }
            }
            try { cleanOutputs(); } catch (RuntimeException | Error problem) { failure = combine(failure, problem); }
            closing = false; closed = true;
            if (failure != null) {
                cleanupFailure = new IllegalStateException("Bore fixture cleanup failed (never a PASS)", failure);
                Throwable original = info.getError();
                if (original != null && original != cleanupFailure) { original.addSuppressed(cleanupFailure); info.fail(original); }
                else info.fail(cleanupFailure);
                throw cleanupFailure;
            }
        }
        private void cleanOutputs() {
            if (BoreWork.isMutationActive() || !BoreNativeContext.canDispatchOutputs(level)) throw new IllegalStateException("Cleanup cannot replace in-flight native ownership");
            DeferredItemDrops queue = level.getDataStorage().get(QUEUE, QUEUE_ID);
            if (queue != null && ((boolean) field(queue, "flushing") || field(queue, "inFlight") != null || !((Set<?>) field(queue, "routingFrames")).isEmpty()))
                throw new IllegalStateException("Cleanup cannot touch an in-flight deferred queue");
            if (queue != null) discardRetirementInputs(queue);
            if (queue != null) collectOwned(queue.save(new CompoundTag(), level.registryAccess()).copy()); // Capture UUIDs BEFORE deleting any journal.
            for (UUID id : outputs) {
                Entity entity = exactEntity(id); if (entity == null) continue;
                CompoundTag row = outputRows.get(id);
                ItemStack expected = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElseThrow(); expected.setCount(row.getInt("Count"));
                if (!(entity instanceof ItemEntity item) || !ItemStack.matches(expected, item.getItem()))
                    throw new IllegalStateException("Refuse ambiguous/mixed ambient ownership for output " + id);
                entity.discard();
                if (!entity.isRemoved() || exactEntity(id) != null) throw new IllegalStateException("Owned output removal was refused");
            }
            for (var entry : observed.entrySet()) {
                UUID id = entry.getKey(); Entity entity = exactEntity(id);
                if (outputs.contains(id) || entity == null) continue;
                if (initialIds.contains(id) || entity != entry.getValue()
                        || entity instanceof ItemEntity item && !ItemStack.matches(observedItems.get(id), item.getItem())
                        || entity instanceof ExperienceOrb orb && !xpMass(orb).equals(observedXp.get(id)))
                    throw new IllegalStateException("Refuse ambiguous observed work-emission ownership");
                entity.discard();
                if (!entity.isRemoved() || exactEntity(id) != null) throw new IllegalStateException("Observed work emission removal was refused");
            }
            // Snapshot AFTER native entity-removal callbacks, so unrelated work
            // performed by those callbacks is never restored from an old copy.
            queue = level.getDataStorage().get(QUEUE, QUEUE_ID);
            if (queue == null) return;
            if ((boolean) field(queue, "flushing") || field(queue, "inFlight") != null || !((Set<?>) field(queue, "routingFrames")).isEmpty())
                throw new IllegalStateException("Queue became active during cleanup callbacks");
            Set<UUID> known = Set.copyOf(outputs);
            CompoundTag before = queue.save(new CompoundTag(), level.registryAccess()).copy(); collectOwned(before);
            if (!known.equals(outputs)) throw new IllegalStateException("New fixture emissions appeared during entity cleanup; retain their journals for diagnosis");
            CompoundTag expected = before.copy();
            removeRows(expected.getList("Pending", Tag.TAG_COMPOUND), this::ownPending);
            removeRows(expected.getList("BoreJournals", Tag.TAG_COMPOUND), this::ownJournal);
            removeRows(expected.getList("Quarantined", Tag.TAG_COMPOUND), row -> ownJournal(row) || ownPending(row));
            removeRows(expected.getList("Settled", Tag.TAG_COMPOUND), row -> row.hasUUID("Id") && outputs.contains(row.getUUID("Id")) && !initialIds.contains(row.getUUID("Id")));
            if (expected.getList("BoreJournals", Tag.TAG_COMPOUND).isEmpty()) expected.remove("BoreJournals");
            // TEST ONLY exact-ID deletion, NOT a production permit. Keep the
            // SAME canonical queue/opaque unrelated Pending objects, retryAfter,
            // receipt/admission and routing state rather than resetting SavedData.
            // In particular, do NOT reload unrelated ARMED journals through the
            // V3 loader (which correctly changes those to UNKNOWN on real load).
            Collection<?> pending = (Collection<?>) field(queue, "pending");
            pending.removeIf(entry -> {
                UUID id = (UUID) field(entry, "id"); if (!outputs.contains(id) || initialIds.contains(id)) return false;
                Object bore = field(entry, "bore");
                if (!pos.equals(field(entry, "source")) || bore == null || !transactions.contains((UUID) field(bore, "transaction")))
                    throw new IllegalStateException("Refuse unrelated Pending sharing a fixture ID");
                return true;
            });
            @SuppressWarnings("unchecked") Map<UUID, CompoundTag> journals = (Map<UUID, CompoundTag>) field(queue, "boreJournals");
            journals.entrySet().removeIf(entry -> transactions.contains(entry.getKey()) && ownJournal(entry.getValue()));
            removeRows((ListTag) field(queue, "quarantined"), row -> ownJournal(row) || ownPending(row));
            @SuppressWarnings("unchecked") Set<UUID> settled = (Set<UUID>) field(queue, "settled");
            settled.removeIf(id -> outputs.contains(id) && !initialIds.contains(id));
            Map<ServerLevel, Set<UUID>> receipts = liveReceipts();
            synchronized (receipts) {
                Set<UUID> live = receipts.get(level);
                if (live != null) live.removeIf(id -> outputs.contains(id) && !initialIds.contains(id));
            }
            queue.setDirty();
            if (!sameQueue(expected, queue.save(new CompoundTag(), level.registryAccess()).copy())) throw new IllegalStateException("Cleanup changed unrelated queue data");
        }
        private void discardRetirementInputs(DeferredItemDrops queue) {
            // Root's follow-up may retain failed native retirement as opaque
            // live custody rather than an encodable journal. Remove ONLY this
            // fixture's new ID/machine/source-owned declared test inputs BEFORE
            // asking codecs to serialize them. No reload/empty-row fallback.
            Object depth = optionalField(queue, "saveDepth");
            if (depth instanceof Integer value && value != 0) throw new IllegalStateException("Cleanup during queue checkpoint");
            Object owners = optionalField(queue, "boreRetirements");
            if (owners == null) return; // Absent in the original frozen queue.
            Collection<?> custody = (Collection<?>) owners;
            boolean removed = custody.removeIf(owner -> {
                UUID id = (UUID) field(owner, "id");
                return !initialTransactions.contains(id) && !initialIds.contains(id) && machines.contains((UUID) field(owner, "machine"))
                        && pos.equals(field(owner, "source"));
            });
            if (removed) queue.setDirty();
        }
        private Entity exactEntity(UUID id) {
            Entity visible = level.getEntity(id);
            if (visible != null) return visible;
            var manager = ((ServerLevelEntityManagerAccess) level).thaumcraft$getEntityManager();
            if (!manager.isLoaded(id)) return null;
            // Exact-ID lookup in EXISTING sections includes hidden native owners
            // without chunk loads or a region/entity deletion sweep.
            var sections = ((PersistentEntitySectionManagerAccess) manager).thaumcraft$getSectionStorage();
            Entity found = null;
            for (long chunk : sections.getAllChunksWithExistingSections()) {
                try (var stream = sections.getExistingSectionsInChunk(chunk)) {
                    for (Entity entity : stream.flatMap(EntitySection::getEntities).filter(entity -> entity.getUUID().equals(id)).toList()) {
                        if (found != null && found != entity) throw new IllegalStateException("Duplicate hidden owners for fixture UUID");
                        found = entity;
                    }
                }
            }
            if (found == null) throw new IllegalStateException("Unattached UUID reservation retained for diagnosis: " + id);
            return found;
        }
        private static final class Cell {
            private final BlockState original;
            private final Set<BlockState> states = new HashSet<>();
            private BlockEntity created;
            private boolean written;
            private Cell(BlockState original) { this.original = original; states.add(original); states.add(Blocks.AIR.defaultBlockState()); }
        }
    }

    static GameTestInfo testInfo(GameTestHelper helper) {
        List<Field> candidates = new ArrayList<>();
        for (Class<?> type = helper.getClass(); type != null; type = type.getSuperclass())
            for (Field field : type.getDeclaredFields()) if (field.getType() == GameTestInfo.class) candidates.add(field);
        if (candidates.size() != 1) throw new IllegalStateException("Expected sole GameTestInfo-typed helper field");
        try { Field field = candidates.getFirst(); field.setAccessible(true); return (GameTestInfo) java.util.Objects.requireNonNull(field.get(helper)); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot install native fixture cleanup listener", failure); }
    }
    private static Object field(Object owner, String name) {
        try { Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Test-only scoped queue access failed: " + name, failure); }
    }
    private static Object optionalField(Object owner, String name) {
        try { Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
        catch (NoSuchFieldException absent) { return null; }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Test-only queue compatibility read failed: " + name, failure); }
    }
    @SuppressWarnings("unchecked") private static Map<ServerLevel, Set<UUID>> liveReceipts() {
        try { Field field = DeferredItemDrops.class.getDeclaredField("LIVE_RECEIPTS"); field.setAccessible(true); return (Map<ServerLevel, Set<UUID>>) field.get(null); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot preserve exact deferred receipts", failure); }
    }
    private static Throwable combine(Throwable first, Throwable next) {
        if (first == null) return next; if (first != next) first.addSuppressed(next); return first;
    }
    private static CompoundTag xpMass(ExperienceOrb orb) {
        CompoundTag saved = orb.saveWithoutId(new CompoundTag()), mass = new CompoundTag();
        for (String name : List.of("Value", "Count")) {
            if (!saved.contains(name, Tag.TAG_ANY_NUMERIC)) throw new IllegalStateException("Missing native XP mass field");
            mass.put(name, saved.get(name).copy());
        }
        return mass;
    }
    private static void removeRows(ListTag rows, java.util.function.Predicate<CompoundTag> remove) {
        for (int index = rows.size() - 1; index >= 0; index--) if (remove.test(rows.getCompound(index))) rows.remove(index);
    }
    private static boolean sameQueue(CompoundTag first, CompoundTag second) {
        CompoundTag a = first.copy(), b = second.copy();
        for (String name : List.of("Settled", "BoreJournals")) {
            Map<UUID, CompoundTag> left = indexed(a.getList(name, Tag.TAG_COMPOUND)), right = indexed(b.getList(name, Tag.TAG_COMPOUND));
            if (!left.equals(right)) return false;
            a.remove(name); b.remove(name);
        }
        return a.equals(b); // Pending order, all legacy/crystal payloads and raw quarantine stay exact.
    }
    private static Map<UUID, CompoundTag> indexed(ListTag rows) {
        Map<UUID, CompoundTag> result = new HashMap<>();
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.hasUUID("Id") || result.put(row.getUUID("Id"), row) != null) throw new IllegalStateException("Ambiguous queue record IDs during cleanup");
        }
        return result;
    }
}

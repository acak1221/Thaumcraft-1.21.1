package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.lib.bore.BorePlatform;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.testing.BoreWorkRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileJar;

/** STAGED / UNEXECUTED. Reuses the existing five-cell native fixture and its
 * exact-ID PASS/FAIL/TIMEOUT cleanup. No ticker, completion, credit, RNG or loot
 * setter. Probe callbacks are real loader/native entry observations, not a
 * replacement BorePlatform bridge. Root supplies isolated <=240-tick wrappers. */
public final class BoreProtectionRuntimeChecks {
    private BoreProtectionRuntimeChecks() {}
    private static final String QUEUE_ID = "thaumcraft_pending_inventory_drops";
    private static final SavedData.Factory<DeferredItemDrops> QUEUE =
            new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);

    public enum Mode { ARCH_DENY, FABRIC_DENY, ALLOW, TARGET_ABA, BEFORE_TOP, AFTER_TOP, AFTER_THROW, UNKNOWN_FALSE, UNKNOWN_THROW }
    public enum Fault { NONE, FALSE, THROW }
    /** Implemented ONLY in Fabric gameTest sources. No production install API. */
    public interface Probe extends AutoCloseable {
        void bind(Attempt attempt);
        @Override void close();
    }
    public static final class DeclaredAfterFailure extends RuntimeException {
        public DeclaredAfterFailure() { super("declared per-fixture Bore AFTER failure"); }
    }
    public static final class DeclaredRemovalFailure extends RuntimeException {
        public DeclaredRemovalFailure() { super("declared per-fixture native removeBlock failure after ARM"); }
    }

    // Six bounded families; denial, AFTER and UNKNOWN have two isolated wrappers.
    public static void denialOrder(GameTestHelper test, Probe probe, boolean fabric) { start(test, probe, fabric ? Mode.FABRIC_DENY : Mode.ARCH_DENY); }
    public static void allowedRemoval(GameTestHelper test, Probe probe) { start(test, probe, Mode.ALLOW); }
    public static void targetAba(GameTestHelper test, Probe probe) { start(test, probe, Mode.TARGET_ABA); }
    public static void beforeRemovesTop(GameTestHelper test, Probe probe) { start(test, probe, Mode.BEFORE_TOP); }
    public static void afterOutcome(GameTestHelper test, Probe probe, boolean throwAfter) { start(test, probe, throwAfter ? Mode.AFTER_THROW : Mode.AFTER_TOP); }
    public static void unknownOutcome(GameTestHelper test, Probe probe, boolean throwNative) { start(test, probe, throwNative ? Mode.UNKNOWN_THROW : Mode.UNKNOWN_FALSE); }

    private static void start(GameTestHelper test, Probe probe, Mode mode) {
        GameTestInfo info = BoreWorkRuntimeChecks.testInfo(test);
        // Register BEFORE constructing Fixture: release ACTIVE even if a later
        // Fixture failure-listener throws while restoring cells/queue ownership.
        info.addListener(new GameTestListener() {
            private void release() {
                try { probe.close(); }
                catch (RuntimeException | Error failure) { info.fail(failure); throw failure; }
            }
            @Override public void testStructureLoaded(GameTestInfo actual) {}
            @Override public void testPassed(GameTestInfo actual, GameTestRunner runner) { release(); }
            @Override public void testFailed(GameTestInfo actual, GameTestRunner runner) { release(); }
            @Override public void testAddedForRerun(GameTestInfo old, GameTestInfo next, GameTestRunner runner) { release(); }
        });
        Fixture fixture = new Fixture(test);
        try {
            fixture.tools();
            fixture.put(fixture.target, Blocks.DIAMOND_ORE.defaultBlockState());
            Attempt attempt = new Attempt(test, fixture, mode);
            if (attempt.unknown()) {
                // One declared physical unit, never a live credit/prefill setter.
                BlockPos jarPos = fixture.top.getBlockPos().below(2);
                fixture.put(jarPos, ThaumcraftContent.JAR.get().defaultBlockState());
                attempt.jar = (TileJar) test.getLevel().getBlockEntity(jarPos);
                test.assertTrue(attempt.jar != null && attempt.jar.addToContainer(Aspect.ENTROPY, 1) == 0,
                        "declared Perditio1 is acquired by genuine native lazy-bound prefill only");
            }
            probe.bind(attempt);
            fixture.power(true);
            test.succeedWhen(() -> {
                fixture.requireOpen();
                test.assertTrue(attempt.poll(), "awaiting real native attempt/settlement: " + attempt.trace + " / " + attempt.problems);
                attempt.verify(); // All ownership/loot/XP assertions BEFORE cleanup.
                probe.close();
                fixture.close(); // BEFORE succeedWhen's implicit native PASS.
            });
        } catch (RuntimeException | Error failure) {
            try { probe.close(); } catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            try { fixture.close(); } catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /** One fixture's typed test probe state, NOT a new fixture/transaction engine.
     * Fabric forwards actual callbacks here; there is no supplied Runnable. */
    public static final class Attempt {
        private final GameTestHelper test;
        private final Fixture f;
        private final UUID machine;
        private final BlockPos source;
        private final ItemStack focus, pick;
        private final List<String> trace = new ArrayList<>(), problems = new ArrayList<>();
        private final Mode mode;
        private TileJar jar;
        private ServerPlayer actor;
        private float creditAtBefore;
        private boolean paused, faultFired, witnessInAfter, declaredAfterThrown, reloaded;
        private int nativeEntries, nativeReturns, actions;
        private long holdUntil = -1, frozenRevision;
        private CompoundTag frozenReference;

        private Attempt(GameTestHelper test, Fixture f, Mode mode) {
            this.test = test; this.f = f; this.mode = mode;
            machine = f.top.machineIdentity(); source = f.top.getBlockPos(); focus = f.top.getItem(0); pick = f.top.getItem(1);
        }
        private boolean unknown() { return mode == Mode.UNKNOWN_FALSE || mode == Mode.UNKNOWN_THROW; }
        private void note(boolean value, String reason) { if (!value) problems.add(reason); }
        private void check(boolean value, String reason) { test.assertTrue(value, reason + ": " + trace + " / " + problems); }
        public boolean applies(Level world, BlockPos target) {
            var frame = EssentiaTransfers.currentBoreFrame(f.top);
            return world == test.getLevel() && target.equals(f.target) && machine.equals(f.top.machineIdentity())
                    && frame != null && frame.top() == f.top && frame.scope().operation() == EssentiaTransfers.BoreOperation.FINISH
                    && BoreNativeContext.isNativeTick(f.top) && f.top.isNativePhase(TileArcaneBore.WorkPhase.COMPLETE);
        }
        private void attribution(Player player, BlockState old, BlockEntity oldEntity) {
            note(player instanceof ServerPlayer server && BorePlatform.required().isNativeMachineActor(server), "actual native machine fake-player attribution");
            if (player instanceof ServerPlayer server) {
                if (actor == null) actor = server;
                note(actor == server && server.serverLevel() == test.getLevel() && server.getMainHandItem().isEmpty()
                        && server.getOffhandItem().isEmpty(), "same native actor/level and no equipped tool copy");
            }
            note(old.is(Blocks.DIAMOND_ORE) && oldEntity == null, "captured ordinary native old state/noBE");
        }
        /** Return means veto only for the declared Architectury-denial phase. */
        public boolean architectury(Player player, BlockState old) {
            attribution(player, old, null); trace.add("ARCH");
            if (mode == Mode.TARGET_ABA && actions++ == 0) {
                f.put(f.target, Blocks.AIR.defaultBlockState());
                f.put(f.target, old); // Same value restored, but native watch must remain invalid.
            }
            return mode != Mode.ARCH_DENY;
        }
        public boolean before(Player player, BlockState old, BlockEntity oldEntity) {
            attribution(player, old, oldEntity); trace.add("BEFORE"); creditAtBefore = f.top.getAccelerationCredit();
            if (mode == Mode.BEFORE_TOP && actions++ == 0) f.put(source, Blocks.AIR.defaultBlockState());
            return mode != Mode.FABRIC_DENY;
        }
        public void canceled(Player player, BlockState old, BlockEntity oldEntity) {
            attribution(player, old, oldEntity); trace.add("CANCELED");
        }
        public void after(Player player, BlockState old, BlockEntity oldEntity) {
            attribution(player, old, oldEntity); trace.add("AFTER");
            witnessInAfter = journals("HARVEST").size() == 1 && journals("HARVEST").getFirst().getString("Phase").equals("WITNESSED");
            note(witnessInAfter && test.getLevel().getBlockState(f.target).isAir(), "true durable witness precedes actual AFTER");
            if (mode == Mode.AFTER_TOP && actions++ == 0) f.put(source, Blocks.AIR.defaultBlockState());
            if (mode == Mode.AFTER_THROW) { declaredAfterThrown = true; throw new DeclaredAfterFailure(); }
        }
        /** Called ONLY from actual Level.removeBlock HEAD; otherwise NONE. */
        public Fault nativeEntry(Level world, BlockPos target, boolean moving) {
            if (!applies(world, target)) return Fault.NONE;
            ++nativeEntries; trace.add("NATIVE_ENTER");
            CompoundTag reference = f.top.workReference();
            boolean armed = reference != null && reference.getString("Phase").equals("ARMED") && reference.hasUUID("Machine") && reference.hasUUID("Id")
                    && reference.getUUID("Machine").equals(machine) && reference.getLong("Source") == source.asLong()
                    && reference.getLong("Target") == f.target.asLong() && f.top.getItem(1).getDamageValue() == 1;
            List<CompoundTag> harvests = journals("HARVEST");
            armed = armed && harvests.size() == 1 && harvests.getFirst().getString("Phase").equals("ARMED")
                    && harvests.getFirst().getUUID("Id").equals(reference.getUUID("Id"));
            note(!moving && armed && nativeEntries == 1, "sole actual native removeBlock entry AFTER raw ARM/worn real pick");
            note(f.top.getAccelerationCredit() == (unknown() ? 19 : 0), "installed reserved credit at native entry");
            if (moving || !armed || nativeEntries != 1 || !unknown() || faultFired || creditAtBefore != 20
                    || f.top.getAccelerationCredit() != 19 || jar == null || jar.getAmount() != 0) return Fault.NONE;
            faultFired = true;
            trace.add(mode == Mode.UNKNOWN_FALSE ? "NATIVE_FALSE" : "NATIVE_THROW");
            return mode == Mode.UNKNOWN_FALSE ? Fault.FALSE : Fault.THROW;
        }
        public void nativeReturn(Level world, BlockPos target, boolean removed) {
            if (!applies(world, target)) return;
            ++nativeReturns;
            if (faultFired) { note(!removed && mode == Mode.UNKNOWN_FALSE, "no true return from injected unknown removal"); return; }
            trace.add(removed ? "NATIVE_TRUE" : "NATIVE_FALSE_RETURN");
            note(removed && test.getLevel().getBlockState(f.target).isAir(), "actual native true return observes removed target");
        }

        private boolean terminalSeen() {
            return switch (mode) {
                case ARCH_DENY, TARGET_ABA -> trace.contains("ARCH");
                case FABRIC_DENY -> trace.contains("CANCELED");
                case BEFORE_TOP -> trace.contains("BEFORE");
                case UNKNOWN_FALSE, UNKNOWN_THROW -> faultFired;
                default -> trace.contains("AFTER");
            };
        }
        private boolean poll() {
            if (!terminalSeen()) return false;
            if (!paused) { f.power(false); paused = true; } // ONCE after each observed real attempt.
            if (mode == Mode.AFTER_THROW && actions == 0) {
                check(declaredAfterThrown && witnessInAfter && f.top.getItem(1).getDamageValue() == 1,
                        "real AFTER threw, but native ticking returned with witnessed already-worn tool");
                ++actions; f.put(source, Blocks.AIR.defaultBlockState()); // Real later native retirement, not a refund.
            }
            if (unknown() && !reloaded) {
                verifyUnknown();
                // UNKNOWN is NEVER promoted by present AIR. Change only the
                // declared target, load a detached copied queue, NEVER install or
                // flush it, then restore the ore for a powered native fence hold.
                f.put(f.target, Blocks.AIR.defaultBlockState());
                CompoundTag saved = queueTag();
                DeferredItemDrops loaded = DeferredItemDrops.load(saved.copy(), test.getLevel().registryAccess());
                CompoundTag decoded = loaded.save(new CompoundTag(), test.getLevel().registryAccess());
                assertUnknownTag(decoded);
                f.put(f.target, Blocks.DIAMOND_ORE.defaultBlockState());
                frozenRevision = f.top.revision(); frozenReference = f.top.workReference();
                reloaded = true; f.power(true); // Fence, NOT an OFF signal, must stop >40 further real ticks.
                holdUntil = test.getLevel().getGameTime() + 50;
            }
            if (holdUntil < 0) holdUntil = test.getLevel().getGameTime() + 3;
            return test.getLevel().getGameTime() >= holdUntil;
        }
        private void verify() {
            check(problems.isEmpty(), "all real callback/attribution/reservation observations agree");
            switch (mode) {
                case ARCH_DENY -> verifyRefused(List.of("ARCH"));
                case FABRIC_DENY -> verifyRefused(List.of("ARCH", "BEFORE", "CANCELED"));
                case TARGET_ABA -> { check(actions == 1, "one actual native target AIR/ore ABA"); verifyRefused(List.of("ARCH")); }
                case BEFORE_TOP -> {
                    check(trace.equals(List.of("ARCH", "BEFORE")) && nativeEntries == 0, "BEFORE top removal invalidates harvest without AFTER/native target removal");
                    check(test.getLevel().getBlockState(f.target).is(Blocks.DIAMOND_ORE), "BEFORE-retired machine never harvested target");
                    assertRetiredTools(0); assertSettled(0, true);
                }
                case ALLOW, AFTER_TOP, AFTER_THROW -> {
                    check(trace.equals(List.of("ARCH", "BEFORE", "NATIVE_ENTER", "NATIVE_TRUE", "AFTER"))
                            && nativeEntries == 1 && nativeReturns == 1 && witnessInAfter, "ordered once-only native protected true completion");
                    check(test.getLevel().getBlockState(f.target).isAir(), "true native removal still owns its output");
                    if (mode == Mode.ALLOW) {
                        ItemStack worn = pick.copy(); worn.setDamageValue(1);
                        check(ItemStack.matches(f.top.getItem(0), focus) && ItemStack.matches(f.top.getItem(1), worn)
                                && f.top.getAccelerationCredit() == 0, "one actual wear, unchanged focus, no invented credit");
                    } else {
                        check(mode != Mode.AFTER_THROW || declaredAfterThrown, "AFTER really threw from its registered native callback");
                        assertRetiredTools(1);
                    }
                    assertSettled(1, mode != Mode.ALLOW);
                }
                case UNKNOWN_FALSE, UNKNOWN_THROW -> {
                    verifyUnknown();
                    check(reloaded && f.top.isPowered() && f.top.revision() == frozenRevision
                            && f.top.workReference().equals(frozenReference), "powered native >40-tick fence hold cannot refund/retry/reset UNKNOWN");
                }
                default -> throw new IllegalStateException("Incomplete declared scenario");
            }
            check(test.getLevel().getEntitiesOfClass(ExperienceOrb.class, f.area).isEmpty(), "no native ore XP/spawnAfterBreak/playerDestroy path");
        }
        private void verifyRefused(List<String> expected) {
            check(trace.equals(expected) && nativeEntries == 0 && nativeReturns == 0, "exact denial/ABA callback order, no native target delegate");
            check(ItemStack.matches(f.top.getItem(0), focus) && ItemStack.matches(f.top.getItem(1), pick)
                    && f.top.getAccelerationCredit() == 0 && f.top.workReference() == null, "pre-entry refusal has no wear/debit/reference");
            check(test.getLevel().getBlockState(f.target).is(Blocks.DIAMOND_ORE) && journals(null).isEmpty()
                    && test.getLevel().getEntitiesOfClass(ItemEntity.class, f.area).isEmpty(), "refused target/whole stock unchanged, no journal or physical output");
        }
        private void verifyUnknown() {
            check(trace.equals(List.of("ARCH", "BEFORE", "NATIVE_ENTER", mode == Mode.UNKNOWN_FALSE ? "NATIVE_FALSE" : "NATIVE_THROW"))
                    && nativeEntries == 1 && (mode == Mode.UNKNOWN_THROW ? nativeReturns == 0 : nativeReturns <= 1)
                    && faultFired, "actual one false/throw delegate, no AFTER or retry");
            ItemStack worn = pick.copy(); worn.setDamageValue(1);
            check(creditAtBefore == 20 && jar.getAmount() == 0 && f.top.getAccelerationCredit() == 19
                    && ItemStack.matches(f.top.getItem(1), worn) && ItemStack.matches(f.top.getItem(0), focus)
                    && f.top.isQuarantined(), "genuine Perditio1 paid credit20; UNKNOWN retains worn pick/credit19, no refund");
            check(test.getLevel().getBlockState(f.target).is(Blocks.DIAMOND_ORE)
                    && test.getLevel().getEntitiesOfClass(ItemEntity.class, f.area).isEmpty(), "failed delegate has no target mutation or physical candidate");
            assertUnknownTag(queueTag());
        }
        private void assertUnknownTag(CompoundTag saved) {
            var owned = new ArrayList<CompoundTag>();
            for (Tag raw : saved.getList("BoreJournals", Tag.TAG_COMPOUND)) if (ours((CompoundTag) raw)) owned.add((CompoundTag) raw);
            check(owned.size() == 1, "one durable unknown journal, never another successful completion");
            CompoundTag journal = owned.getFirst();
            check(journal.getString("Mode").equals("HARVEST") && journal.getString("Phase").equals("UNKNOWN") && journal.getBoolean("Fenced")
                    && journal.getInt("CreditBits") == Float.floatToIntBits(19) && journal.getList("Rows", Tag.TAG_COMPOUND).size() == 1,
                    "save/native reload preserves UNKNOWN exact reserved credit/candidate, never infers success from AIR");
            UUID candidate = journal.getList("Rows", Tag.TAG_COMPOUND).getCompound(0).getUUID("Id");
            check(test.getLevel().getEntity(candidate) == null, "UNKNOWN stable candidate UUID has no admitted physical owner anywhere in the loaded level");
            for (String name : List.of("Pending", "Settled")) for (Tag raw : saved.getList(name, Tag.TAG_COMPOUND))
                check(!((CompoundTag) raw).getUUID("Id").equals(candidate), "UNKNOWN candidate is neither spendable Pending nor acknowledged output");
        }
        private void assertRetiredTools(int damage) {
            check(f.top.wasEvacuated() && f.top.isEmpty() && f.top.getAccelerationCredit() == 0, "old machine is cleared/retired without re-admission");
            List<CompoundTag> evacs = journals("EVACUATE");
            check(evacs.size() == 1 && evacs.getFirst().getString("Phase").equals("EVACUATED"), "one actual physical tool evacuation journal");
            var rows = evacs.getFirst().getList("Rows", Tag.TAG_COMPOUND);
            check(rows.size() == 2, "exactly two tool return members");
            ItemStack worn = pick.copy(); worn.setDamageValue(damage);
            check(ItemStack.matches(stack(rows.getCompound(0)), focus) && ItemStack.matches(stack(rows.getCompound(1)), worn),
                    "actual complete focus and already-worn pick are returned once, not stale unworn preparation");
        }
        private void assertSettled(int diamonds, boolean evacuated) {
            List<CompoundTag> harvests = journals("HARVEST");
            check(harvests.size() == diamonds && journals(null).size() == diamonds + (evacuated ? 1 : 0), "no extra owned completion/evacuation journals");
            if (diamonds == 1) {
                CompoundTag harvest = harvests.getFirst(); ItemStack worn = pick.copy(); worn.setDamageValue(1);
                ItemStack before = ItemStack.parse(test.getLevel().registryAccess(), harvest.getCompound("PickBefore")).orElseThrow();
                ItemStack after = ItemStack.parse(test.getLevel().registryAccess(), harvest.getCompound("PickAfter")).orElseThrow();
                check(harvest.getString("Phase").equals("WITNESSED") && harvest.getInt("CreditBits") == Float.floatToIntBits(0)
                        && ItemStack.matches(before, pick) && ItemStack.matches(after, worn), "durable true witness keeps exact pre/post native tool wear and reserved credit");
            }
            CompoundTag saved = queueTag(); Set<UUID> ids = new HashSet<>(); int diamondCount = 0;
            for (CompoundTag journal : journals(null)) {
                check(!journal.getBoolean("Fenced"), "true output is witnessed/evacuated, never unknown candidate stock");
                for (Tag raw : journal.getList("Rows", Tag.TAG_COMPOUND)) {
                    CompoundTag row = (CompoundTag) raw; UUID id = row.getUUID("Id");
                    check(ids.add(id), "no duplicate stable physical UUID across own journals");
                    ItemStack expected = stack(row);
                    if (expected.is(Items.DIAMOND)) diamondCount += expected.getCount();
                    check(test.getLevel().getEntity(id) instanceof ItemEntity physical && ItemStack.matches(physical.getItem(), expected),
                            "native physical entity exactly matches its acknowledged durable candidate");
                    boolean acknowledged = false;
                    for (Tag receipt : saved.getList("Settled", Tag.TAG_COMPOUND)) if (((CompoundTag) receipt).getUUID("Id").equals(id)) acknowledged = true;
                    check(acknowledged, "one stable native entity owns an acknowledged output");
                }
            }
            check(diamondCount == diamonds && ids.size() == diamonds + (evacuated ? 2 : 0), "whole output mass is exact, including native evacuation tools");
            var physical = test.getLevel().getEntitiesOfClass(ItemEntity.class, f.area);
            check(physical.size() == ids.size() && physical.stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet()).equals(ids),
                    "all actual nearby output ownership is exactly the journal UUID set, with no unjournalled duplicate tools or diamonds");
            for (Tag raw : saved.getList("Pending", Tag.TAG_COMPOUND)) check(!ids.contains(((CompoundTag) raw).getUUID("Id")), "no acknowledged fixture output remains retryable");
            for (Tag raw : saved.getList("Quarantined", Tag.TAG_COMPOUND)) check(!ours((CompoundTag) raw), "ordinary callback retirement must physically return tools, not hide them in opaque fallback");
        }
        private ItemStack stack(CompoundTag row) {
            ItemStack value = ItemStack.parse(test.getLevel().registryAccess(), row.getCompound("Stack")).orElseThrow(); value.setCount(row.getInt("Count")); return value;
        }
        private boolean ours(CompoundTag row) { return row.hasUUID("Machine") && row.getUUID("Machine").equals(machine) && row.getLong("Source") == source.asLong(); }
        private List<CompoundTag> journals(String mode) {
            return DeferredItemDrops.boreJournalView(test.getLevel()).stream().filter(row -> ours(row) && (mode == null || row.getString("Mode").equals(mode))).toList();
        }
        private CompoundTag queueTag() {
            DeferredItemDrops queue = test.getLevel().getDataStorage().get(QUEUE, QUEUE_ID);
            return queue == null ? new CompoundTag() : queue.save(new CompoundTag(), test.getLevel().registryAccess()).copy();
        }
    }
}

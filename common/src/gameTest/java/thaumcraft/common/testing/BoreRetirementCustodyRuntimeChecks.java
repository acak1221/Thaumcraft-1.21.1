package thaumcraft.common.testing;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneBore;

/** Three UNEXECUTED source checks. Real native onRemove and the actual builtin
 * text component codec; no component registration, production hook, reflection,
 * synthetic retirement scope, supplied success permit or manual machine tick. */
public final class BoreRetirementCustodyRuntimeChecks {
    private BoreRetirementCustodyRuntimeChecks() {}

    public static void stableNativeRetirementQueuesToolsOnce(GameTestHelper test) {
        exercise(test, false, false);
    }
    public static void componentMismatchRetainsActualSlots(GameTestHelper test) {
        exercise(test, true, false);
    }
    public static void codecFailureAfterOwnerRemovalRetainsAndSaves(GameTestHelper test) {
        exercise(test, true, true);
    }

    private static void exercise(GameTestHelper test, boolean mutate, boolean detachAndThrow) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        try (var f = new Fixture(test)) {
            f.open();
            var queue = f.level.getDataStorage().computeIfAbsent(Fixture.QUEUE, Fixture.QUEUE_ID);
            var original = queue.save(new CompoundTag(), f.level.registryAccess());
            Set<UUID> originalJournals = ids(original, "BoreJournals"), originalQuarantine = ids(original, "Quarantined");
            TextCodecWitness witness = null;
            try {
                f.put(f.base.below(), ThaumcraftArcaneBore.ARCANE_BORE_BASE.get());
                f.put(f.base, ThaumcraftArcaneBore.ARCANE_BORE.get());
                var top = (TileArcaneBore) f.level.getBlockEntity(f.base);
                var focus = new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get());
                focus.set(DataComponents.CUSTOM_NAME, Component.literal("retirement-focus"));
                var pick = new ItemStack(Items.WOODEN_PICKAXE); pick.setDamageValue(7);
                witness = new TextCodecWitness(f, top, detachAndThrow);
                pick.set(DataComponents.CUSTOM_NAME, mutate ? MutableComponent.create(witness) : Component.literal(witness.value));
                top.setItem(0, focus); top.setItem(1, pick);
                f.check(!top.isEmpty() && top.getItem(1).getDamageValue() == 7, "actual guarded installed finite tools");
                int beforeCustody = queue.boreRetirementCustodyCount();
                witness.armed = mutate; // No native setup packet/save can trigger the fixture early.
                f.check(f.level.setBlock(f.base, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE),
                        "real Level/Chunk onRemove, not a direct BoreWork/retirement call");
                f.check(top.wasEvacuated() && top.isEmpty() && f.level.getBlockEntity(f.base) == null,
                        "native dying owner has no second copy of tools");
                f.check(!mutate || witness.fired, "actual builtin text codec visited the callback witness");
                f.check(queue.boreRetirementCustodyCount() == beforeCustody + (mutate ? 1 : 0),
                        "stable path stays physical; callback failure/mismatch owns exactly one opaque quarantine");

                if (detachAndThrow) {
                    witness.failSave = true;
                    boolean refused = false;
                    try { queue.save(new CompoundTag(), f.level.registryAccess()); }
                    catch (RuntimeException expected) { refused = true; }
                    f.check(refused && queue.boreRetirementCustodyCount() == beforeCustody + 1
                                    && queue.boreRetirementSaveFailures().stream().anyMatch(value -> !value.isEmpty()),
                            "unserializable actual custody aborts checkpoint, retains stock and records a diagnostic");
                    witness.failSave = false;
                }
                var saved = queue.save(new CompoundTag(), f.level.registryAccess());
                var expectedFocus = new ItemStack(ThaumcraftFoci.EXCAVATION_FOCUS.get());
                var expectedName = Component.literal("retirement-focus");
                if (mutate) expectedName.append("-changed");
                expectedFocus.set(DataComponents.CUSTOM_NAME, expectedName);
                var expectedPick = new ItemStack(Items.WOODEN_PICKAXE); expectedPick.setDamageValue(7);
                expectedPick.set(DataComponents.CUSTOM_NAME, Component.literal(witness.value));
                if (mutate) {
                    CompoundTag retained = null; int matches = 0;
                    for (Tag raw : saved.getList("Quarantined", Tag.TAG_COMPOUND)) {
                        var row = (CompoundTag) raw;
                        if (row.getString("Kind").equals("BORE_RETIREMENT_CUSTODY") && row.getLong("Source") == f.base.asLong()
                                && row.hasUUID("Id") && !originalQuarantine.contains(row.getUUID("Id"))) { retained = row; matches++; }
                    }
                    f.check(matches == 1 && retained != null && retained.getBoolean("Unsendable"), "one whole unsendable dying-stock owner");
                    f.check(ItemStack.matches(decode(f, retained.getCompound("Focus")), expectedFocus)
                                    && ItemStack.matches(decode(f, retained.getCompound("Pick")), expectedPick),
                            "custody saves ACTUAL post-callback focus components and currently worn pick, not stale preparation");
                    for (Tag raw : saved.getList("Pending", Tag.TAG_COMPOUND))
                        f.check(((CompoundTag) raw).getLong("Source") != f.base.asLong(), "abandoned prepared escrow never became physical output");
                    var loaded = DeferredItemDrops.load(saved.copy(), f.level.registryAccess());
                    f.check(loaded.pendingCount() == queue.pendingCount() && loaded.quarantinedCount() == queue.quarantinedCount(),
                            "actual V3 reader keeps opaque whole payload in quarantine, never restores a source or Pending row");
                    f.check(saved.getList("Quarantined", Tag.TAG_COMPOUND).equals(loaded.save(new CompoundTag(), f.level.registryAccess())
                            .getList("Quarantined", Tag.TAG_COMPOUND)), "whole quarantine survives native save/load without slot normalization");
                } else {
                    int focusCount = 0, pickCount = 0;
                    for (Tag raw : saved.getList("Pending", Tag.TAG_COMPOUND)) {
                        var row = (CompoundTag) raw;
                        if (row.getLong("Source") != f.base.asLong() || !row.contains("Bore", Tag.TAG_COMPOUND)) continue;
                        ItemStack value = decode(f, row);
                        if (ItemStack.matches(value, expectedFocus)) focusCount++;
                        else if (ItemStack.matches(value, expectedPick)) pickCount++;
                        else f.check(false, "no extra/stale retirement tool output");
                    }
                    f.check(focusCount == 1 && pickCount == 1, "stable native retirement queues both complete tools exactly once");
                    int pending = queue.pendingCount();
                    thaumcraft.common.lib.bore.BoreWork.evacuate(top);
                    f.check(queue.pendingCount() == pending && queue.boreRetirementCustodyCount() == beforeCustody,
                            "already evacuated stale owner cannot duplicate a return or acquire opaque custody");
                }
            } finally {
                if (witness != null) { witness.armed = false; witness.failSave = false; }
                // Synchronous TEST-ONLY cleanup: remove only this source's NEW
                // exact transaction/custody IDs, preserving all other queue data.
                var saved = queue.save(new CompoundTag(), f.level.registryAccess());
                var journalIds = new HashSet<UUID>();
                var journals = saved.getList("BoreJournals", Tag.TAG_COMPOUND);
                for (int index = journals.size() - 1; index >= 0; index--) {
                    var row = journals.getCompound(index);
                    if (row.hasUUID("Id") && row.getLong("Source") == f.base.asLong() && !originalJournals.contains(row.getUUID("Id"))) {
                        journalIds.add(row.getUUID("Id")); journals.remove(index);
                    }
                }
                var rows = saved.getList("Pending", Tag.TAG_COMPOUND);
                for (int index = rows.size() - 1; index >= 0; index--) {
                    var row = rows.getCompound(index);
                    if (row.contains("Bore", Tag.TAG_COMPOUND) && row.getCompound("Bore").hasUUID("Transaction")
                            && journalIds.contains(row.getCompound("Bore").getUUID("Transaction"))) rows.remove(index);
                }
                var quarantine = saved.getList("Quarantined", Tag.TAG_COMPOUND);
                for (int index = quarantine.size() - 1; index >= 0; index--) {
                    var row = quarantine.getCompound(index);
                    if (row.getString("Kind").equals("BORE_RETIREMENT_CUSTODY") && row.hasUUID("Id")
                            && row.getLong("Source") == f.base.asLong() && !originalQuarantine.contains(row.getUUID("Id"))) quarantine.remove(index);
                }
                f.level.getDataStorage().set(Fixture.QUEUE_ID, DeferredItemDrops.load(saved, f.level.registryAccess()));
            }
        }
        test.succeed();
    }

    private static ItemStack decode(Fixture f, CompoundTag row) {
        ItemStack stack = ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack")).orElseThrow();
        stack.setCount(row.getInt("Count")); return stack;
    }
    private static Set<UUID> ids(CompoundTag saved, String collection) {
        var ids = new HashSet<UUID>();
        for (Tag raw : saved.getList(collection, Tag.TAG_COMPOUND)) if (((CompoundTag) raw).hasUUID("Id")) ids.add(((CompoundTag) raw).getUUID("Id"));
        return ids;
    }

    /** The existing native PlainTextContents codec really invokes text(). It
     * encodes/decodes as ordinary literal text: no new component type/bootstrap.
     * The only side effects live here in gameTest source, not production. */
    private static final class TextCodecWitness implements PlainTextContents {
        private final Fixture fixture;
        private final TileArcaneBore top;
        private final boolean detachAndThrow;
        // Distinct native codec-cache key: no earlier unrelated literal can
        // satisfy the encoder without visiting this actual fixture's text().
        private final String value = "retirement-pick-" + UUID.randomUUID();
        private boolean armed, fired, failSave;
        private TextCodecWitness(Fixture fixture, TileArcaneBore top, boolean detachAndThrow) {
            this.fixture = fixture; this.top = top; this.detachAndThrow = detachAndThrow;
        }
        @Override public String text() {
            if (failSave) throw new IllegalStateException("owned-retirement-codec-not-serializable-yet");
            if (armed) {
                armed = false; fired = true;
                var name = top.getItem(0).get(DataComponents.CUSTOM_NAME);
                fixture.check(name instanceof MutableComponent, "fixture has a genuine mutable native text component");
                ((MutableComponent) name).append("-changed");
                if (detachAndThrow) {
                    fixture.level.removeBlockEntity(top.getBlockPos());
                    throw new IllegalStateException("native-retirement-owner-removed-during-initial-codec-capture");
                }
            }
            return value;
        }
        @Override public boolean equals(Object other) {
            return other == this || other instanceof PlainTextContents contents && value.equals(contents.text());
        }
        @Override public int hashCode() { return value.hashCode(); }
    }
}

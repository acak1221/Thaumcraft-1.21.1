package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.utils.RejectedEntitySpawn;
import thaumcraft.common.mixin.PersistentEntitySectionManagerAccess;
import thaumcraft.common.mixin.ServerLevelEntityManagerAccess;
import thaumcraft.common.tiles.TileResearchTable;

/** Scoped, real Architectury13.0.11 EntityEvent.ADD cancellation and callback checks. */
public final class TableDropCancellationRuntimeChecks {
    private static final String MARKER = "thaumcraft:test_table_drop_scope";
    // Read the actual production SavedData, never replace it or call its flush.
    private static final SavedData.Factory<DeferredItemDrops> QUEUE_READER = new SavedData.Factory<>(
            DeferredItemDrops::new, DeferredItemDrops::load, null);
    private TableDropCancellationRuntimeChecks() {}

    public static void canceledTableOutputRetriesAfterCooldown(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        var scope = new AddScope(fixture.level, fixture.marker, true, null);
        long startedAt = test.getTick();
        long startedWorldAt = fixture.level.getGameTime();
        boolean[] closed = {false};
        Runnable cleanup = cleanup(fixture, scope, closed);
        try {
            var note = markedNote(fixture.marker);
            fixture.placePair();
            var original = fixture.tile();
            original.setItem(1, note.copy());
            fixture.breakMain(scope);
            test.assertTrue(original.isEmpty() && original.isRemoved() && scope.attempts == 0,
                    "actual break clears and removes physical BE without invoking item ADD inside onRemove");
            test.assertTrue(fixture.pendingMarked().size() == 1 && ItemStack.matches(fixture.pendingMarked().getFirst(), note),
                    "production durable queue owns the exact component stack before any spawn callback");
            test.runAfterDelay(2, () -> {
                if (closed[0]) return;
                try {
                    assertCallbackHealthy(test, scope);
                    test.assertTrue(scope.attempts == 1 && scope.first != null && scope.first.isRemoved()
                            && fixture.level.getEntity(scope.first.getUUID()) == null,
                            "real first marked item ADD is canceled and its rejected native entity never becomes a world member");
                    test.assertTrue(!((ServerLevelEntityManagerAccess)fixture.level).thaumcraft$getEntityManager().isLoaded(scope.first.getUUID()),
                            "rejected native ADD leaves no poisoned known-UUID reservation before retry");
                    test.assertTrue(fixture.pendingMarked().size() == 1 && ItemStack.matches(fixture.pendingMarked().getFirst(), note)
                            && fixture.pendingIds().contains(scope.first.getUUID()),
                            "addFreshEntity rejection preserves exact material/components and stable output UUID in the real queue");
                    // A new unrelated output must wake only eligible work. It
                    // must not bypass the failed entry's own20-tick cooldown.
                    DeferredItemDrops.enqueue(fixture.level, fixture.main,
                            List.of(markedNote(fixture.marker + "-wake")));
                } catch (RuntimeException | Error error) {
                    cleanup.run(); throw error;
                }
            });
            test.runAfterDelay(4, () -> {
                if (closed[0]) return;
                try {
                    test.assertTrue(scope.attempts == 1 && fixture.pendingMarked().size() == 1,
                            "unrelated enqueue cannot retry the canceled stable output before its20-tick cooldown");
                    var wake = fixture.dropsFor(fixture.marker + "-wake");
                    test.assertTrue(wake.size() == 1 && ItemStack.matches(wake.getFirst().getItem(), markedNote(fixture.marker + "-wake")),
                            "native level-post delivers the newly enqueued unrelated component stack while the failed entry waits");
                } catch (RuntimeException | Error error) {
                    cleanup.run(); throw error;
                }
            });
            test.onEachTick(() -> {
                if (closed[0] || scope.attempts < 2) return;
                try {
                    assertCallbackHealthy(test, scope);
                    test.assertTrue(scope.attempts == 2 && scope.last != scope.first
                            && scope.last.getUUID().equals(scope.first.getUUID()),
                            "native retry constructs one fresh entity for the same durable output");
                    test.assertTrue(scope.lastAt - scope.firstAt >= 20L,
                            "failed first output waits the production cooldown before retry");
                    test.assertTrue(fixture.level.getEntity(scope.last.getUUID()) == scope.last
                            && !scope.last.isRemoved() && ItemStack.matches(scope.last.getItem(), note),
                            "native safe-level-post retry delivers the intact stack through the real entity manager");
                    test.assertTrue(fixture.pendingMarked().isEmpty() && fixture.markedDrops().size() == 1,
                            "successful retry removes only its pending record and leaves exactly one component stack");
                    fixture.assertPlayerUnchanged();
                    cleanup.run();
                    test.succeed();
                } catch (RuntimeException | Error error) {
                    cleanup.run(); throw error;
                }
            });
            test.runAfterDelay(70, () -> {
                if (closed[0]) return;
                String diagnostic = " attempts=" + scope.attempts + ", firstAt=" + scope.firstAt
                        + ", lastAt=" + scope.lastAt + ", worldNow=" + fixture.level.getGameTime()
                        + ", startedWorldAt=" + startedWorldAt + ", testTick=" + test.getTick()
                        + ", startedTestTick=" + startedAt + ", pendingMarked=" + fixture.pendingMarked().size()
                        + ", pendingIds=" + fixture.pendingIds() + ", chunkLoaded=" + fixture.level.hasChunkAt(fixture.main);
                cleanup.run();
                test.fail("canceled table output did not retry through native level-post within its relative70-tick deadline;" + diagnostic);
            });
        } catch (RuntimeException | Error error) {
            cleanup.run(); throw error;
        }
    }

    public static void tableSpawnCallbackPreservesReplacementInventory(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        var replacementTool = new ItemStack(ThaumcraftContent.INKWELL.get());
        replacementTool.setDamageValue(83);
        replacementTool.set(DataComponents.CUSTOM_NAME, Component.literal("replacement-tool-" + fixture.marker));
        var replacementNote = markedNote(fixture.marker + "-replacement");
        TileResearchTable[] replacement = {null};
        var scope = new AddScope(fixture.level, fixture.marker, false, () -> {
            fixture.placePair();
            replacement[0] = fixture.tile();
            replacement[0].setItem(0, replacementTool.copy());
            replacement[0].setItem(1, replacementNote.copy());
        });
        boolean[] closed = {false};
        Runnable cleanup = cleanup(fixture, scope, closed);
        try {
            var note = markedNote(fixture.marker);
            fixture.placePair();
            var original = fixture.tile();
            original.setItem(1, note.copy());
            fixture.breakMain(scope);
            test.assertTrue(original.isEmpty() && original.isRemoved() && scope.attempts == 0
                            && fixture.level.getBlockState(fixture.main).isAir(),
                    "outer native setBlock/onRemove has finished before the marked output callback runs");
            test.runAfterDelay(2, () -> {
                try {
                    assertCallbackHealthy(test, scope);
                    test.assertTrue(scope.attempts == 1 && !scope.calledDuringBreak && replacement[0] != null,
                            "one actual native item ADD callback creates replacement table after the outer removal frame");
                    test.assertTrue(fixture.level.getBlockEntity(fixture.main) == replacement[0]
                            && !replacement[0].isRemoved()
                            && ItemStack.matches(replacement[0].getItem(0), replacementTool)
                            && ItemStack.matches(replacement[0].getItem(1), replacementNote),
                            "completed outer teardown cannot erase the callback-created BE or its component-bearing inventory");
                    test.assertTrue(scope.last != null && fixture.level.getEntity(scope.last.getUUID()) == scope.last
                            && ItemStack.matches(scope.last.getItem(), note) && fixture.pendingMarked().isEmpty()
                            && fixture.markedDrops().size() == 1,
                            "replacement callback neither loses nor duplicates the original queued stack");
                    fixture.assertPlayerUnchanged();
                    cleanup.run();
                    test.succeed();
                } catch (RuntimeException | Error error) {
                    cleanup.run(); throw error;
                }
            });
        } catch (RuntimeException | Error error) {
            cleanup.run(); throw error;
        }
    }

    /** Direct helper guard check; the separate cancellation case still uses the full native queue path. */
    public static void rejectedSpawnPreservesExistingHiddenUuidOwner(GameTestHelper test) {
        var level = test.getLevel();
        var manager = ((ServerLevelEntityManagerAccess)level).thaumcraft$getEntityManager();
        var reference = test.absolutePos(new BlockPos(1, 2, 1));
        var remote = new BlockPos((reference.getX() & ~15) + 2_097_156,
                reference.getY(), (reference.getZ() & ~15) + 2_097_156);
        var chunkPos = new ChunkPos(remote);
        // Synchronous isolated fixture: no player or other test is moved here,
        // and no global/player tickets or shared GameTest chunk visibility change.
        var chunk = level.getChunkAt(remote);
        var originalStatus = chunk.getFullStatus();
        var id = UUID.randomUUID();
        var ownerStack = markedNote(UUID.randomUUID() + "-hidden-owner");
        var owner = new ItemEntity(level, remote.getX() + 0.5, remote.getY() + 0.5, remote.getZ() + 0.5, ownerStack.copy());
        owner.setUUID(id);
        var attempted = new ItemEntity(level, remote.getX() + 0.5, remote.getY() + 0.5, remote.getZ() + 0.5,
                new ItemStack(ThaumcraftContent.INKWELL.get()));
        attempted.setUUID(id);
        var preexistingAttempt = new ItemEntity(level, remote.getX() + 0.5, remote.getY() + 0.5, remote.getZ() + 0.5,
                new ItemStack(ThaumcraftContent.INKWELL.get()));
        preexistingAttempt.setUUID(id);
        try {
            // Owner is deliberately inserted AFTER the snapshot. knownBefore is
            // false, so preserving it requires the helper's all-sections guard.
            var beforeOwner = RejectedEntitySpawn.snapshotBefore(level, attempted);
            test.assertTrue(!beforeOwner.knownBefore(), "fresh attempted UUID starts unreserved");
            test.assertTrue(level.addFreshEntity(owner), "actual native owner enters its isolated chunk");
            manager.updateChunkStatus(chunkPos, Visibility.HIDDEN);
            test.assertTrue(manager.isLoaded(id) && level.getEntity(id) == null && !owner.isRemoved(),
                    "native owner is registered but absent from the visible level lookup");
            var sections = ((PersistentEntitySectionManagerAccess)manager).thaumcraft$getSectionStorage();
            try (var existing = sections.getExistingSectionsInChunk(chunkPos.toLong())) {
                test.assertTrue(existing.flatMap(EntitySection::getEntities).anyMatch(entity -> entity == owner),
                        "owner remains a real member of a hidden native entity section");
            }
            boolean nativeAdded = level.addFreshEntity(attempted);
            test.assertTrue(!nativeAdded, "actual duplicate attempted output is rejected while owner is hidden");
            attempted.discard();
            test.assertTrue(!RejectedEntitySpawn.releaseRejected(level, attempted, beforeOwner, nativeAdded)
                            && manager.isLoaded(id) && !owner.isRemoved() && ItemStack.matches(owner.getItem(), ownerStack),
                    "post-snapshot hidden owner prevents UUID release and retains its intact component stack");

            var alreadyOwned = RejectedEntitySpawn.snapshotBefore(level, preexistingAttempt);
            test.assertTrue(alreadyOwned.knownBefore(), "second snapshot sees the existing hidden owner reservation");
            boolean secondAdded = level.addFreshEntity(preexistingAttempt);
            preexistingAttempt.discard();
            test.assertTrue(!secondAdded && !RejectedEntitySpawn.releaseRejected(level, preexistingAttempt, alreadyOwned, secondAdded)
                            && manager.isLoaded(id) && !owner.isRemoved(),
                    "known-before guard cannot unreserve or discard an existing native owner");
            manager.updateChunkStatus(chunkPos, Visibility.TRACKED);
            test.assertTrue(level.getEntity(id) == owner && ItemStack.matches(owner.getItem(), ownerStack),
                    "revealing the same native section recovers the original owner identity and components");
        } finally {
            // The real owner's assigned native callback removes its own UUID;
            // no accessor mutation or manual cleanup of knownUuids is used.
            attempted.discard(); preexistingAttempt.discard(); owner.discard();
            manager.updateChunkStatus(chunkPos, originalStatus);
        }
        test.succeed();
    }

    private static Runnable cleanup(Fixture fixture, AddScope scope, boolean[] closed) {
        return () -> {
            if (closed[0]) return;
            closed[0] = true;
            try {
                scope.close();
            } finally {
                fixture.close();
            }
        };
    }

    private static void assertCallbackHealthy(GameTestHelper test, AddScope scope) {
        test.assertTrue(scope.callbackFailure == null, "scoped native callback completes: " + scope.callbackFailure);
        test.assertTrue(!scope.calledDuringBreak, "marked inventory entity ADD never reenters the unfinished native break frame");
    }

    private static ItemStack markedNote(String marker) {
        var stack = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("scoped-table-output"));
        var tag = new CompoundTag(); tag.putString(MARKER, marker);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static boolean hasMarker(ItemStack stack, String marker) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && marker.equals(data.copyTag().getString(MARKER));
    }

    private static final class AddScope implements AutoCloseable {
        final ServerLevel level;
        final String marker;
        final boolean cancelFirst;
        final Runnable onFirst;
        final EntityEvent.Add listener;
        boolean active = true, breaking, calledDuringBreak;
        int attempts;
        long firstAt, lastAt;
        ItemEntity first, last;
        Throwable callbackFailure;
        AddScope(ServerLevel level, String marker, boolean cancelFirst, Runnable onFirst) {
            this.level = level; this.marker = marker; this.cancelFirst = cancelFirst; this.onFirst = onFirst;
            // Architectury13.0.11 Add(Entity,Level): interruptFalse cancels actual
            // Fabric entity-manager insertion / NeoForge EntityJoinLevelEvent.
            listener = (entity, world) -> {
                if (!active || world != level || !(entity instanceof ItemEntity item) || !hasMarker(item.getItem(), marker)) {
                    return EventResult.pass();
                }
                attempts++; last = item; lastAt = level.getGameTime(); calledDuringBreak |= breaking;
                if (attempts == 1) {
                    first = item; firstAt = lastAt;
                    try {
                        if (onFirst != null) onFirst.run();
                    } catch (RuntimeException | Error error) {
                        // Queue intentionally catches native spawn exceptions.
                        // Record failures and assert outside that production catch.
                        callbackFailure = error;
                    }
                    if (cancelFirst) return EventResult.interruptFalse();
                }
                return EventResult.pass();
            };
            EntityEvent.ADD.register(listener);
        }
        @Override public void close() {
            active = false;
            EntityEvent.ADD.unregister(listener);
            if (EntityEvent.ADD.isRegistered(listener)) throw new IllegalStateException("Scoped item ADD listener was not removed");
        }
    }

    private record SavedBlock(BlockState state, CompoundTag tag) {}

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos main;
        final String marker = UUID.randomUUID().toString();
        final Map<BlockPos, SavedBlock> blocks = new LinkedHashMap<>();
        final List<ItemEntity> rawDrops = new ArrayList<>();
        final List<ItemStack> inventory = new ArrayList<>();
        final CompoundTag knowledge;
        final thaumcraft.common.lib.research.PlayerKnowledge knowledgeSnapshot;
        final Vec3 playerPosition;
        Fixture(GameTestHelper test, ServerPlayer player) {
            this.test = test; this.level = test.getLevel(); this.player = player;
            main = test.absolutePos(new BlockPos(4, 2, 4));
            knowledgeSnapshot = KnowledgeAccess.get(player).copy();
            knowledge = knowledgeSnapshot.save(); playerPosition = player.position();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
            var safe = test.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
            remember(main); remember(main.north());
        }
        void remember(BlockPos pos) {
            var be = level.getBlockEntity(pos);
            blocks.put(pos, new SavedBlock(level.getBlockState(pos), be == null ? null : be.saveWithFullMetadata(level.registryAccess())));
        }
        void placePair() {
            level.setBlock(main, ThaumcraftContent.TABLE.get().state(2), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(main.north(), ThaumcraftContent.TABLE.get().state(7), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        TileResearchTable tile() { return (TileResearchTable)level.getBlockEntity(main); }
        void breakMain(AddScope scope) {
            Set<UUID> before = new HashSet<>();
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(main).inflate(2))) before.add(entity.getUUID());
            scope.breaking = true;
            try {
                test.assertTrue(level.destroyBlock(main, true, player), "actual native break removes research main");
            } finally {
                scope.breaking = false;
            }
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(main).inflate(2))) {
                if (!before.contains(entity.getUUID()) && entity.getItem().is(ThaumcraftContent.TABLE_ITEM.get())) rawDrops.add(entity);
            }
        }
        DeferredItemDrops queue() { return level.getDataStorage().get(QUEUE_READER, "thaumcraft_pending_inventory_drops"); }
        List<ItemStack> pendingMarked() {
            var result = new ArrayList<ItemStack>();
            var queue = queue();
            if (queue == null) return result;
            for (Tag raw : queue.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag)raw;
                var stack = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
                if (hasMarker(stack, marker)) { stack.setCount(row.getInt("Count")); result.add(stack); }
            }
            return result;
        }
        List<UUID> pendingIds() { return queue() == null ? List.of() : queue().pendingIds(); }
        List<ItemEntity> markedDrops() {
            return dropsFor(marker);
        }
        List<ItemEntity> dropsFor(String ownedMarker) {
            return level.getEntitiesOfClass(ItemEntity.class, new AABB(main).inflate(8), entity -> hasMarker(entity.getItem(), ownedMarker));
        }
        void assertPlayerUnchanged() {
            test.assertValueEqual(KnowledgeAccess.get(player).save(), knowledge, "native drop cancellation/callback does not change actor knowledge or warp");
            for (int slot = 0; slot < inventory.size(); slot++) {
                test.assertTrue(ItemStack.matches(player.getInventory().getItem(slot), inventory.get(slot)), "drop proof does not debit or pick up player items at slot " + slot);
            }
        }
        @Override public void close() {
            for (BlockPos pos : blocks.keySet()) if (level.getBlockEntity(pos) instanceof TileResearchTable table) table.clearContent();
            for (var entry : blocks.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue().state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var be = level.getBlockEntity(entry.getKey());
                if (be != null && entry.getValue().tag != null) be.loadWithComponents(entry.getValue().tag, level.registryAccess());
            }
            for (var entity : markedDrops()) entity.discard();
            for (var entity : dropsFor(marker + "-wake")) entity.discard();
            for (var entity : rawDrops) if (!entity.isRemoved()) entity.discard();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            KnowledgeAccess.get(player).copyFrom(knowledgeSnapshot);
            player.setPos(playerPosition);
        }
    }
}

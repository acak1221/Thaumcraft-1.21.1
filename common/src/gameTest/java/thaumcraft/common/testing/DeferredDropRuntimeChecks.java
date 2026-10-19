package thaumcraft.common.testing;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.entities.EntityInventoryItem;
import thaumcraft.common.items.ItemResearchNotes;

/** Actual native spawn rejection, SavedData round-trip and stable-ID delivery. */
public final class DeferredDropRuntimeChecks {
    private DeferredDropRuntimeChecks() {}

    public static void pendingDropSurvivesCollisionAndPersistence(GameTestHelper test) {
        var level = test.getLevel();
        BlockPos source = test.absolutePos(new BlockPos(1, 2, 1));
        UUID id = UUID.randomUUID();
        var blocker = new ItemEntity(level, source.getX() + 0.5, source.getY() + 0.5,
                source.getZ() + 0.5, new ItemStack(Items.COBBLESTONE));
        blocker.setUUID(id);
        test.assertTrue(level.addFreshEntity(blocker), "real UUID collision fixture enters the native world");
        var tool = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
        tool.setCount(127);
        CompoundTag encoded = queued(source, id, tool, level.registryAccess());
        try {
            var queue = DeferredItemDrops.load(encoded, level.registryAccess());
            queue.flush(level);
            test.assertTrue(queue.pendingCount() == 1 && level.getEntity(id) == blocker,
                    "conflicting actual world entity keeps output pending without replacement or material loss");
            var persisted = queue.save(new CompoundTag(), level.registryAccess());
            var restored = DeferredItemDrops.load(persisted, level.registryAccess());
            test.assertTrue(restored.pendingCount() == 1 && restored.pendingIds().equals(queue.pendingIds()),
                    "pending identity/components/count survive native SavedData codec round-trip");
            blocker.discard();
            test.runAfterDelay(2, () -> {
                try {
                    restored.flush(level);
                    var actual = level.getEntity(id);
                    test.assertTrue(restored.pendingCount() == 0 && actual instanceof ItemEntity
                                    && ItemStack.matches(((ItemEntity) actual).getItem(), tool),
                            "retry delivers one intact native item after the conflicting entity is removed");
                    test.assertTrue(actual instanceof EntityInventoryItem, "over99 output uses native-count serialization adapter");
                    CompoundTag entitySaved = new CompoundTag();
                    test.assertTrue(actual.save(entitySaved), "actual output entity saves count127 without native codec rejection");
                    var entityReloaded = EntityType.loadEntityRecursive(entitySaved, level, java.util.function.Function.identity());
                    test.assertTrue(entityReloaded instanceof EntityInventoryItem
                                    && ItemStack.matches(((ItemEntity) entityReloaded).getItem(), tool)
                                    && ItemStack.matches(((ItemEntity) actual).getItem(), tool),
                            "native entity round-trip retains one count127 discovery and does not mutate the live stack");
                    restored.flush(level);
                    test.assertTrue(level.getEntity(id) == actual && restored.pendingCount() == 0,
                            "repeated flush cannot duplicate a delivered output");
                    var replay = DeferredItemDrops.load(encoded, level.registryAccess());
                    replay.flush(level);
                    test.assertTrue(replay.pendingCount() == 0 && level.getEntity(id) == actual,
                            "stable output ID recognizes an already present identical delivery");
                    test.assertTrue(restored.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND).isEmpty(),
                            "successful delivery removes the durable pending record");
                    test.succeed();
                } finally {
                    var entity = level.getEntity(id);
                    if (entity != null) entity.discard();
                }
            });
        } catch (RuntimeException | Error error) {
            blocker.discard();
            throw error;
        }
    }

    public static void quarantineRetainsUnknownData(GameTestHelper test) {
        var source = test.absolutePos(new BlockPos(1, 2, 1));
        var unknown = new CompoundTag();
        unknown.putInt("Version", 777);
        unknown.putString("UnknownPayload", "preserve this data");
        var future = DeferredItemDrops.load(unknown, test.getLevel().registryAccess());
        CompoundTag futureSaved = future.save(new CompoundTag(), test.getLevel().registryAccess());
        test.assertTrue(future.pendingCount() == 0 && future.quarantinedCount() == 1
                        && futureSaved.getList("Quarantined", Tag.TAG_COMPOUND).getCompound(0).equals(unknown),
                "unknown queue schema is retained rather than erased after a vanilla SavedData read failure");
        var malformed = queued(source, UUID.randomUUID(), new ItemStack(Items.PAPER), test.getLevel().registryAccess());
        malformed.getList("Pending", Tag.TAG_COMPOUND).getCompound(0).putInt("Count", 0);
        var invalid = DeferredItemDrops.load(malformed, test.getLevel().registryAccess());
        var invalidSaved = invalid.save(new CompoundTag(), test.getLevel().registryAccess());
        test.assertTrue(invalid.pendingCount() == 0 && invalid.quarantinedCount() == 1
                        && invalidSaved.getList("Quarantined", Tag.TAG_COMPOUND).getCompound(0)
                        .equals(malformed.getList("Pending", Tag.TAG_COMPOUND).getCompound(0)),
                "malformed stored item is retained intact for recovery, never silently discarded or spawned");
        test.succeed();
    }

    private static CompoundTag queued(BlockPos source, UUID id, ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        var row = new CompoundTag();
        row.putUUID("Id", id);
        row.putLong("Source", source.asLong());
        row.put("Stack", stack.copyWithCount(1).save(registries));
        row.putInt("Count", stack.getCount());
        row.putDouble("X", source.getX() + 0.5); row.putDouble("Y", source.getY() + 0.5); row.putDouble("Z", source.getZ() + 0.5);
        row.putDouble("DX", 0); row.putDouble("DY", 0.2); row.putDouble("DZ", 0);
        var entries = new ListTag();
        entries.add(row);
        var tag = new CompoundTag();
        tag.putInt("Version", 1);
        tag.put("Pending", entries);
        return tag;
    }
}

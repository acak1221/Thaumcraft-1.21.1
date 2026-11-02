package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileMirror;

/** Actual stationary-mirror output through native admission, including one bounded recursive observer. */
public final class ComprehensiveMirrorReviewRuntimeChecks {
    private static final String QUEUE_ID = "thaumcraft_pending_inventory_drops";
    private static final SavedData.Factory<DeferredItemDrops> QUEUE = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
    private ComprehensiveMirrorReviewRuntimeChecks() {}

    public static void ejectionOwnsOnlyOneItemAcrossRecursiveAdmission(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 180, 3);
        var mirror = mirror(lane);
        String marker = "mirror-ejection-review-" + UUID.randomUUID();
        mirror.addStack(marked(marker));
        int[] calls = {0};
        CompoundTag[] callbackSource = {null};
        Throwable[] recursiveFailure = {null};
        EntityEvent.Add observer = (entity, level) -> {
            if (level != lane.level || !(entity instanceof ItemEntity item) || !marked(item.getItem(), marker)) return EventResult.pass();
            if (++calls[0] == 1) {
                callbackSource[0] = mirror.saveWithoutMetadata(lane.level.registryAccess());
                try { mirror.eject(); }
                catch (RuntimeException failure) { recursiveFailure[0] = failure; }
            }
            return EventResult.pass();
        };
        EntityEvent.ADD.register(observer);
        try {
            Throwable outerFailure = null;
            try { mirror.eject(); }
            catch (RuntimeException failure) { outerFailure = failure; }
            // A queued implementation may emit immediately or at the normal level-post flush.
            DeferredItemDrops.tick(lane.level);
            int emitted = drops(lane, marker).stream().mapToInt(item -> item.getItem().getCount()).sum();
            test.assertTrue(outerFailure == null && recursiveFailure[0] == null && calls[0] == 1 && emitted == 1,
                    "one buffered item must emit once without a recursive tick crash; actual calls=" + calls[0]
                            + ", emitted=" + emitted + ", outer=" + outerFailure + ", inner=" + recursiveFailure[0]);
            test.assertTrue(callbackSource[0] != null && callbackSource[0].getList("Items", Tag.TAG_COMPOUND).isEmpty()
                            && mirror.getOutputStacks().isEmpty(),
                    "a save from the real ADD callback must see the already-paid empty source buffer");
            var item = drops(lane, marker).getFirst();
            test.assertTrue(item.getPortalCooldown() == 20 && item.hasPickUpDelay(),
                    "stationary output preserves its native portal cooldown and default pickup delay");
            test.succeed();
        } finally {
            EntityEvent.ADD.unregister(observer);
            drops(lane, marker).forEach(ItemEntity::discard);
            lane.clean();
        }
    }

    public static void canceledEjectionRetainsPaidCustodyAndOriginalPhysics(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 210, 3);
        var mirror = mirror(lane);
        String marker = "mirror-canceled-review-" + UUID.randomUUID();
        mirror.addStack(marked(marker));
        List<ItemEntity> attempts = new ArrayList<>();
        EntityEvent.Add observer = (entity, level) -> {
            if (level == lane.level && entity instanceof ItemEntity item && marked(item.getItem(), marker)) {
                attempts.add(item);
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        };
        EntityEvent.ADD.register(observer);
        try {
            mirror.eject();
            DeferredItemDrops.tick(lane.level);
            var queue = lane.level.getDataStorage().get(QUEUE, QUEUE_ID);
            test.assertTrue(mirror.getOutputStacks().isEmpty() && queue != null && drops(lane, marker).isEmpty(),
                    "native refusal leaves no physical item and one durable paid destination, never a retryable source copy");
            var saved = queue.save(new CompoundTag(), lane.level.registryAccess());
            var owned = owned(saved, lane.level, marker);
            test.assertTrue(attempts.size() == 1 && owned.size() == 1 && owned.getFirst().getInt("Count") == 1
                            && owned.getFirst().getInt("PickupDelay") == 10 && owned.getFirst().getInt("PortalCooldown") == 20,
                    "the canceled stationary output retains one exact stack, native default pickup delay10 and portal cooldown20");
            var restored = DeferredItemDrops.load(saved, lane.level.registryAccess());
            test.assertTrue(owned(restored.save(new CompoundTag(), lane.level.registryAccess()), lane.level, marker).size() == 1,
                    "paid custody survives the real SavedData codec");
            EntityEvent.ADD.unregister(observer);
            // Reinstall only this fixture's canonical queue snapshot to exercise a real recovery retry.
            lane.level.getDataStorage().set(QUEUE_ID, restored);
            restored.flush(lane.level);
            test.assertTrue(drops(lane, marker).stream().mapToInt(item -> item.getItem().getCount()).sum() == 1
                            && mirror.getOutputStacks().isEmpty(),
                    "a native retry emits exactly the single paid output and does not refund its source");
            test.succeed();
        } finally {
            EntityEvent.ADD.unregister(observer);
            drops(lane, marker).forEach(ItemEntity::discard);
            lane.clean();
        }
    }

    private static TileMirror mirror(WaveLane lane) {
        lane.put(lane.origin.below(), Blocks.STONE.defaultBlockState());
        lane.put(lane.origin, ThaumcraftEldritchBlocks.MIRROR.get().defaultBlockState().setValue(BlockMirror.FACING, Direction.UP));
        var mirror = (TileMirror) lane.level.getBlockEntity(lane.origin);
        // Fixture only: the original ejection delay has already elapsed; all ejection code is production.
        try {
            var count = TileMirror.class.getDeclaredField("count"); count.setAccessible(true); count.setInt(mirror, 21);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        return mirror;
    }
    private static ItemStack marked(String marker) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(marker));
        return stack;
    }
    private static boolean marked(ItemStack stack, String marker) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null && name.getString().equals(marker);
    }
    private static List<ItemEntity> drops(WaveLane lane, String marker) {
        return lane.level.getEntitiesOfClass(ItemEntity.class, lane.area(4), item -> marked(item.getItem(), marker));
    }
    private static List<CompoundTag> owned(CompoundTag saved, ServerLevel level, String marker) {
        List<CompoundTag> result = new ArrayList<>();
        for (Tag raw : saved.getList("Pending", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            ItemStack item = ItemStack.parse(level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (marked(item, marker)) result.add(row);
        }
        return result;
    }
}

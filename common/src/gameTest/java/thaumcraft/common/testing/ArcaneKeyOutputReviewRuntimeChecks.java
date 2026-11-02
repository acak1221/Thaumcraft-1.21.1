package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.items.ItemKey;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileOwned;

/** Full native inventory, real key linking and scoped ADD callbacks expose delivery ownership failures. */
public final class ArcaneKeyOutputReviewRuntimeChecks {
    private ArcaneKeyOutputReviewRuntimeChecks() {}

    public static void canceledLinkedOutputRetainsOnePaidKey(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); prepare(f);
            ItemStack source = fill(f, 2);
            var attempts = new ArrayList<ItemEntity>();
            boolean[] paidBeforeCallback = {false};
            EntityEvent.Add cancel = (entity, world) -> {
                if (world == f.level && entity instanceof ItemEntity item && bound(f, item.getItem())) {
                    attempts.add(item); paidBeforeCallback[0] = source.getCount() == 1;
                    return EventResult.interruptFalse();
                }
                return EventResult.pass();
            };
            EntityEvent.ADD.register(cancel);
            try {
                var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                DeferredItemDrops.tick(f.level);
                int pending = pending(f);
                test.assertTrue(result.consumesAction() && source.getCount() == 1 && physical(f) == 0
                                && pending == 1 && attempts.size() == 1 && paidBeforeCallback[0],
                        "refused linked-key admission must retain exactly one already-paid key; pending=" + pending
                                + ", source=" + source.getCount() + ", attempts=" + attempts.size() + ", paidBefore=" + paidBeforeCallback[0]);
                var queue = f.level.getDataStorage().get(TubeVariantsRuntimeChecks.Fixture.QUEUE, TubeVariantsRuntimeChecks.Fixture.QUEUE_ID);
                var saved = queue.save(new CompoundTag(), f.level.registryAccess());
                var restored = DeferredItemDrops.load(saved, f.level.registryAccess());
                EntityEvent.ADD.unregister(cancel);
                f.level.getDataStorage().set(TubeVariantsRuntimeChecks.Fixture.QUEUE_ID, restored);
                restored.flush(f.level);
                test.assertTrue(pending(f) == 0 && physical(f) == 1 && source.getCount() == 1,
                        "SavedData recovery emits the single bound key without refunding or recharging the blank source");
            } finally {
                EntityEvent.ADD.unregister(cancel);
                f.drops(); f.pendingRows();
            }
        }
        test.succeed();
    }

    public static void recursiveLinkedOutputDoesNotDuplicateOrDoubleCharge(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            for (int count : new int[]{1, 2}) {
                f.reset(); prepare(f);
                ItemStack source = fill(f, count);
                int[] calls = {0};
                List<Throwable> nestedErrors = new ArrayList<>();
                EntityEvent.Add reenter = (entity, world) -> {
                    if (world == f.level && entity instanceof ItemEntity item && bound(f, item.getItem()) && ++calls[0] == 1) {
                        try { f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND); }
                        catch (RuntimeException failure) { nestedErrors.add(failure); }
                    }
                    return EventResult.pass();
                };
                EntityEvent.ADD.register(reenter);
                try {
                    var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                    DeferredItemDrops.tick(f.level);
                    int output = inventory(f) + physical(f) + pending(f);
                    test.assertTrue(result.consumesAction() && nestedErrors.isEmpty() && output == 1
                                    && source.getCount() == count - 1 && calls[0] <= 1,
                            "one real key use must own one linked output and spend one blank despite ADD reentry; count=" + count
                                    + ", output=" + output + ", source=" + source.getCount() + ", calls=" + calls[0] + ", errors=" + nestedErrors);
                } finally {
                    EntityEvent.ADD.unregister(reenter);
                    f.drops(); f.pendingRows();
                }
            }
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                f.reset(); prepare(f); f.mode(mode);
                ItemStack source = new ItemStack(ThaumcraftLegacyItems.KEY_GOLD.get(), 2);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, source);
                var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                test.assertTrue(result.consumesAction() && inventory(f) == 1 && physical(f) == 0 && pending(f) == 0
                                && source.getCount() == (mode == GameType.CREATIVE ? 2 : 1),
                        "ordinary inventory delivery preserves the survival debit and creative blank key; mode=" + mode);
            }
            f.reset(); prepare(f); f.mode(GameType.CREATIVE);
            ItemStack creativeFull = fill(f, 2);
            var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
            test.assertTrue(result.consumesAction() && creativeFull.getCount() == 2
                            && inventory(f) + physical(f) + pending(f) == 0,
                    "native creative full-inventory add retains its original discard behavior without spending a blank");
        }
        test.succeed();
    }

    private static void prepare(TubeVariantsRuntimeChecks.Fixture f) {
        f.put(f.base, ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE.get());
        var owned = (TileOwned) f.level.getBlockEntity(f.base);
        owned.setOwner(f.player.getGameProfile().getName()); owned.setChanged();
    }
    private static ItemStack fill(TubeVariantsRuntimeChecks.Fixture f, int count) {
        for (int slot = 0; slot < f.player.getInventory().items.size(); slot++)
            f.player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
        var source = new ItemStack(ThaumcraftLegacyItems.KEY_GOLD.get(), count);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, source);
        return source;
    }
    private static boolean bound(TubeVariantsRuntimeChecks.Fixture f, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ThaumcraftLegacyItems.KEY_GOLD.get())) return false;
        var link = ItemKey.link(stack);
        return link != null && link.type() == 1
                && link.location().equals(EquipmentRules.keyLocation(f.base.getX(), f.base.getY(), f.base.getZ()));
    }
    private static int physical(TubeVariantsRuntimeChecks.Fixture f) {
        return f.drops().stream().filter(item -> bound(f, item.getItem())).mapToInt(item -> item.getItem().getCount()).sum();
    }
    private static int inventory(TubeVariantsRuntimeChecks.Fixture f) {
        return f.player.getInventory().items.stream().filter(stack -> bound(f, stack)).mapToInt(ItemStack::getCount).sum();
    }
    private static int pending(TubeVariantsRuntimeChecks.Fixture f) {
        int result = 0;
        for (var row : f.pendingRows()) {
            ItemStack stack = ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (bound(f, stack)) result += stack.getCount();
        }
        return result;
    }
}

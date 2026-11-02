package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileWandPedestal;

/** Real full-inventory actions, scoped native ADD refusal/reentry and existing durable queue. */
public final class TailBlockOutputRuntimeChecks {
    private TailBlockOutputRuntimeChecks() {}

    public static void pedestalRetainsCanceledRetrieval(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); f.put(f.base, ThaumcraftLegacyTiles.WAND_PEDESTAL.get());
            var pedestal = (TileWandPedestal) f.level.getBlockEntity(f.base);
            var wand = new ItemStack(ThaumcraftContent.WAND.get());
            pedestal.setItem(0, wand.copy());
            full(f);
            Predicate<ItemStack> matches = stack -> ItemStack.matches(stack, wand);
            int[] attempts = {0};
            EntityEvent.Add cancel = (entity, world) -> {
                if (world == f.level && entity instanceof ItemEntity item && matches.test(item.getItem())) {
                    attempts[0]++;
                    return EventResult.interruptFalse();
                }
                return EventResult.pass();
            };
            EntityEvent.ADD.register(cancel);
            try {
                var result = f.use(f.base);
                DeferredItemDrops.tick(f.level);
                int pending = pending(f, matches), physical = physical(f, matches);
                test.assertTrue(result.consumesAction() && pedestal.isEmpty() && physical == 0 && pending == 1 && attempts[0] == 1,
                        "refused native pedestal retrieval retains exactly one removed wand; slot=" + pedestal.getItem(0)
                                + ", physical=" + physical + ", pending=" + pending + ", attempts=" + attempts[0]);
                EntityEvent.ADD.unregister(cancel);
                recover(f);
                test.assertTrue(pending(f, matches) == 0 && physical(f, matches) == 1,
                        "existing SavedData recovery delivers the removed wand exactly once");
            } finally {
                EntityEvent.ADD.unregister(cancel); f.drops(); f.pendingRows();
            }
            f.reset(); f.put(f.base, ThaumcraftLegacyTiles.WAND_PEDESTAL.get());
            pedestal = (TileWandPedestal) f.level.getBlockEntity(f.base);
            pedestal.setItem(0, wand.copy());
            test.assertTrue(f.use(f.base).consumesAction() && pedestal.isEmpty()
                            && f.player.getInventory().countItem(wand.getItem()) == 1 && physical(f, matches) == 0 && pending(f, matches) == 0,
                    "ordinary retrieval still returns the wand directly into native inventory");
            f.reset(); f.put(f.base, ThaumcraftLegacyTiles.WAND_PEDESTAL.get()); f.mode(GameType.CREATIVE);
            pedestal = (TileWandPedestal) f.level.getBlockEntity(f.base); pedestal.setItem(0, wand.copy()); full(f);
            test.assertTrue(f.use(f.base).consumesAction() && pedestal.isEmpty()
                            && f.player.getInventory().countItem(wand.getItem()) == 0 && physical(f, matches) == 0 && pending(f, matches) == 0,
                    "native creative full-inventory retrieval retains its original discard behavior");
        }
        test.succeed();
    }

    public static void mirrorBindingRetainsCanceledOutput(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            for (boolean essentia : new boolean[]{false, true}) {
                f.reset(); prepareMirror(f, essentia);
                ItemStack source = mirrorSource(f, essentia, 2);
                Predicate<ItemStack> matches = stack -> bound(f, stack, essentia);
                int[] attempts = {0}; boolean[] paid = {false};
                EntityEvent.Add cancel = (entity, world) -> {
                    if (world == f.level && entity instanceof ItemEntity item && matches.test(item.getItem())) {
                        attempts[0]++; paid[0] = source.getCount() == 1;
                        return EventResult.interruptFalse();
                    }
                    return EventResult.pass();
                };
                EntityEvent.ADD.register(cancel);
                try {
                    var result = f.use(f.base, Direction.SOUTH, InteractionHand.MAIN_HAND);
                    DeferredItemDrops.tick(f.level);
                    int pending = pending(f, matches);
                    test.assertTrue(result.consumesAction() && source.getCount() == 1 && physical(f, matches) == 0
                                    && pending == 1 && attempts[0] == 1 && paid[0],
                            "refused binding retains one already-paid mirror; essentia=" + essentia + ", source=" + source.getCount()
                                    + ", pending=" + pending + ", attempts=" + attempts[0] + ", paidBefore=" + paid[0]);
                    EntityEvent.ADD.unregister(cancel); recover(f);
                    test.assertTrue(physical(f, matches) == 1 && pending(f, matches) == 0 && source.getCount() == 1,
                            "saved mirror binding recovery retains its link and exactly one source debit");
                } finally {
                    EntityEvent.ADD.unregister(cancel); f.drops(); f.pendingRows();
                }
            }
        }
        test.succeed();
    }

    public static void mirrorBindingCannotReenterUnpaidSource(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            for (boolean essentia : new boolean[]{false, true}) for (int count : new int[]{1, 2}) {
                f.reset(); prepareMirror(f, essentia);
                ItemStack source = mirrorSource(f, essentia, count);
                Predicate<ItemStack> matches = stack -> bound(f, stack, essentia);
                int[] calls = {0}; var failures = new ArrayList<Throwable>();
                EntityEvent.Add reenter = (entity, world) -> {
                    if (world == f.level && entity instanceof ItemEntity item && matches.test(item.getItem()) && ++calls[0] == 1) {
                        try { f.use(f.base, Direction.SOUTH, InteractionHand.MAIN_HAND); }
                        catch (RuntimeException failure) { failures.add(failure); }
                    }
                    return EventResult.pass();
                };
                EntityEvent.ADD.register(reenter);
                try {
                    var result = f.use(f.base, Direction.SOUTH, InteractionHand.MAIN_HAND);
                    DeferredItemDrops.tick(f.level);
                    int outputs = physical(f, matches) + pending(f, matches)
                            + f.player.getInventory().items.stream().filter(matches).mapToInt(ItemStack::getCount).sum();
                    test.assertTrue(result.consumesAction() && failures.isEmpty() && outputs == 1
                                    && source.getCount() == count - 1 && calls[0] <= 1,
                            "one mirror binding pays one source before native recursive admission; essentia=" + essentia
                                    + ", count=" + count + ", output=" + outputs + ", source=" + source.getCount()
                                    + ", callbacks=" + calls[0] + ", errors=" + failures);
                } finally {
                    EntityEvent.ADD.unregister(reenter); f.drops(); f.pendingRows();
                }
            }
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                f.reset(); prepareMirror(f, false); f.mode(mode);
                var ordinary = new ItemStack(ThaumcraftEldritchBlocks.MIRROR_ITEM.get(), 2);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, ordinary);
                test.assertTrue(f.use(f.base).consumesAction() && ordinary.getCount() == (mode == GameType.CREATIVE ? 2 : 1)
                                && f.player.getInventory().items.stream().filter(stack -> bound(f, stack, false)).mapToInt(ItemStack::getCount).sum() == 1,
                        "ordinary inventory binding retains native immediate delivery and source charge; mode=" + mode);
            }
            f.reset(); prepareMirror(f, false); f.mode(GameType.CREATIVE);
            var creativeFull = mirrorSource(f, false, 2);
            test.assertTrue(f.use(f.base).consumesAction() && creativeFull.getCount() == 2
                            && physical(f, stack -> bound(f, stack, false)) == 0 && pending(f, stack -> bound(f, stack, false)) == 0,
                    "native creative full-inventory mirror binding retains its original output discard and blank source");
        }
        test.succeed();
    }

    private static void full(TubeVariantsRuntimeChecks.Fixture f) {
        for (int slot = 0; slot < f.player.getInventory().items.size(); slot++)
            f.player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
    }
    private static void prepareMirror(TubeVariantsRuntimeChecks.Fixture f, boolean essentia) {
        f.put(f.base.south(), net.minecraft.world.level.block.Blocks.STONE);
        f.level.setBlock(f.base, ThaumcraftEldritchBlocks.MIRROR.get().defaultBlockState()
                .setValue(BlockMirror.FACING, Direction.NORTH).setValue(BlockMirror.ESSENTIA, essentia), TubeVariantsRuntimeChecks.QUIET);
    }
    private static ItemStack mirrorSource(TubeVariantsRuntimeChecks.Fixture f, boolean essentia, int count) {
        full(f);
        var source = new ItemStack(essentia ? ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_ITEM.get() : ThaumcraftEldritchBlocks.MIRROR_ITEM.get(), count);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, source); return source;
    }
    private static boolean bound(TubeVariantsRuntimeChecks.Fixture f, ItemStack stack, boolean essentia) {
        if (!stack.is(essentia ? ThaumcraftEldritchBlocks.MIRROR_ESSENTIA_ITEM.get() : ThaumcraftEldritchBlocks.MIRROR_ITEM.get())) return false;
        var link = stack.get(ThaumcraftLegacyItems.MIRROR_LINK.get());
        return link != null && link.x() == f.base.getX() && link.y() == f.base.getY() && link.z() == f.base.getZ()
                && link.dimension().equals(f.level.dimension().location().toString());
    }
    private static int physical(TubeVariantsRuntimeChecks.Fixture f, Predicate<ItemStack> matches) {
        return f.drops().stream().filter(item -> matches.test(item.getItem())).mapToInt(item -> item.getItem().getCount()).sum();
    }
    private static int pending(TubeVariantsRuntimeChecks.Fixture f, Predicate<ItemStack> matches) {
        int result = 0;
        for (var row : f.pendingRows()) {
            var stack = ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (matches.test(stack)) result += stack.getCount();
        }
        return result;
    }
    private static void recover(TubeVariantsRuntimeChecks.Fixture f) {
        var queue = f.level.getDataStorage().get(TubeVariantsRuntimeChecks.Fixture.QUEUE, TubeVariantsRuntimeChecks.Fixture.QUEUE_ID);
        var restored = DeferredItemDrops.load(queue.save(new CompoundTag(), f.level.registryAccess()), f.level.registryAccess());
        f.level.getDataStorage().set(TubeVariantsRuntimeChecks.Fixture.QUEUE_ID, restored); restored.flush(f.level);
    }
}

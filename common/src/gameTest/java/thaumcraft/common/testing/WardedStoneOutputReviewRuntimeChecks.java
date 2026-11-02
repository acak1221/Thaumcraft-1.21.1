package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Half;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileOwned;

/** Real native wand reclaim and scoped ADD hooks exercise physical block/output ownership. */
public final class WardedStoneOutputReviewRuntimeChecks {
    private WardedStoneOutputReviewRuntimeChecks() {}

    public static void canceledReclaimRetainsOneRemovedBlock(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); prepare(f);
            List<ItemEntity> attempts = new ArrayList<>();
            boolean[] removedBeforeCallback = {false};
            EntityEvent.Add cancel = (entity, world) -> {
                if (world == f.level && entity instanceof ItemEntity item && reclaimed(item.getItem())) {
                    attempts.add(item);
                    removedBeforeCallback[0] = f.level.getBlockState(f.base).isAir();
                    return EventResult.interruptFalse();
                }
                return EventResult.pass();
            };
            EntityEvent.ADD.register(cancel);
            try {
                var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                DeferredItemDrops.tick(f.level);
                int pending = pending(f);
                test.assertTrue(result.consumesAction() && f.level.getBlockState(f.base).isAir()
                                && physical(f) == 0 && pending == 1 && attempts.size() == 1 && removedBeforeCallback[0],
                        "refused warded reclaim admission must retain one removed physical block; pending=" + pending
                                + ", attempts=" + attempts.size() + ", removedBefore=" + removedBeforeCallback[0]);
                var queue = f.level.getDataStorage().get(TubeVariantsRuntimeChecks.Fixture.QUEUE, TubeVariantsRuntimeChecks.Fixture.QUEUE_ID);
                var saved = queue.save(new CompoundTag(), f.level.registryAccess());
                var restored = DeferredItemDrops.load(saved, f.level.registryAccess());
                EntityEvent.ADD.unregister(cancel);
                f.level.getDataStorage().set(TubeVariantsRuntimeChecks.Fixture.QUEUE_ID, restored);
                restored.flush(f.level);
                test.assertTrue(pending(f) == 0 && physical(f) == 1 && f.level.getBlockState(f.base).isAir(),
                        "SavedData recovery returns the one already-removed plate without restoring the block");
            } finally {
                EntityEvent.ADD.unregister(cancel);
                f.drops(); f.pendingRows();
            }
        }
        test.succeed();
    }

    public static void recursiveReclaimCannotDuplicateTheSameBlock(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); prepare(f);
            int[] calls = {0};
            List<Throwable> nestedErrors = new ArrayList<>();
            EntityEvent.Add reenter = (entity, world) -> {
                if (world == f.level && entity instanceof ItemEntity item && reclaimed(item.getItem()) && ++calls[0] == 1) {
                    try { f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND); }
                    catch (RuntimeException failure) { nestedErrors.add(failure); }
                }
                return EventResult.pass();
            };
            EntityEvent.ADD.register(reenter);
            try {
                var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                DeferredItemDrops.tick(f.level);
                int output = physical(f) + pending(f);
                test.assertTrue(result.consumesAction() && nestedErrors.isEmpty() && f.level.getBlockState(f.base).isAir()
                                && output == 1 && calls[0] <= 1,
                        "recursive native wand use cannot reclaim one plate twice; output=" + output
                                + ", calls=" + calls[0] + ", errors=" + nestedErrors);
            } finally {
                EntityEvent.ADD.unregister(reenter);
                f.drops(); f.pendingRows();
            }
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                f.reset(); prepare(f); f.mode(mode);
                var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                test.assertTrue(result.consumesAction() && f.level.getBlockState(f.base).isAir()
                                && physical(f) == 1 && pending(f) == 0 && f.player.getMainHandItem().getCount() == 1,
                        "ordinary reclaim returns one plate without consuming the wand; mode=" + mode);
            }
            for (Half clicked : Half.values()) {
                f.reset();
                f.put(f.base, ThaumcraftEldritchBlocks.ARCANE_DOOR.get());
                f.put(f.base.above(), ThaumcraftEldritchBlocks.ARCANE_DOOR.get());
                f.level.setBlock(f.base.above(), f.level.getBlockState(f.base.above()).setValue(BlockArcaneDoor.HALF, Half.TOP), Block.UPDATE_CLIENTS);
                for (BlockPos pos : List.of(f.base, f.base.above())) {
                    var owned = (TileOwned) f.level.getBlockEntity(pos);
                    owned.setOwner(f.player.getGameProfile().getName()); owned.setChanged();
                }
                f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
                var result = f.use(clicked == Half.TOP ? f.base.above() : f.base, Direction.UP, InteractionHand.MAIN_HAND);
                int doors = f.drops().stream().filter(item -> item.getItem().is(ThaumcraftEldritchBlocks.ARCANE_DOOR_ITEM.get()))
                        .mapToInt(item -> item.getItem().getCount()).sum();
                int expected = thaumcraft.common.config.ThaumcraftSettings.current().wardedStone() || clicked == Half.BOTTOM ? 1 : 0;
                test.assertTrue(result.consumesAction() && f.level.getBlockState(f.base).isAir()
                                && f.level.getBlockState(f.base.above()).isAir() && doors == expected && f.pendingRows().isEmpty(),
                        "reclaiming either half removes the native pair and preserves its one-item rule; clicked=" + clicked + ", doors=" + doors);
            }
            f.reset(); prepare(f);
            var dropsRule = f.level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
            boolean previous = dropsRule.get();
            dropsRule.set(false, f.level.getServer());
            try {
                var result = f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND);
                test.assertTrue(result.consumesAction() && f.level.getBlockState(f.base).isAir() && physical(f) + pending(f) == 0,
                        "doTileDrops=false still permits reclaim removal while suppressing the returned item");
            } finally { dropsRule.set(previous, f.level.getServer()); }
        }
        test.succeed();
    }

    private static void prepare(TubeVariantsRuntimeChecks.Fixture f) {
        f.put(f.base, ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE.get());
        var owned = (TileOwned) f.level.getBlockEntity(f.base);
        owned.setOwner(f.player.getGameProfile().getName()); owned.setChanged();
        f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.WAND.get()));
    }
    private static boolean reclaimed(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ThaumcraftEldritchBlocks.ARCANE_PRESSURE_PLATE_ITEM.get());
    }
    private static int physical(TubeVariantsRuntimeChecks.Fixture f) {
        return f.drops().stream().filter(item -> reclaimed(item.getItem())).mapToInt(item -> item.getItem().getCount()).sum();
    }
    private static int pending(TubeVariantsRuntimeChecks.Fixture f) {
        int result = 0;
        for (var row : f.pendingRows()) {
            ItemStack stack = ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (reclaimed(stack)) result += stack.getCount();
        }
        return result;
    }
}

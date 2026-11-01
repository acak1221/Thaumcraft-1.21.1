package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftFocalManipulation;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.container.ContainerHandMirror;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.relics.ItemHandMirror;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ScribingInk;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileFocalManipulator;
import thaumcraft.common.tiles.TileResearchTable;

/** Declared item fixtures, real native menus/clicks and a scoped actual entity ADD cancellation. */
public final class HandMirrorMenuReviewRuntimeChecks {
    private static final String MARKER = "review:hand_mirror";
    private static final SavedData.Factory<DeferredItemDrops> QUEUE = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
    private HandMirrorMenuReviewRuntimeChecks() {}

    public static void restrictedSlotsKeepOneFocusAndOneInkwell(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            KnowledgeAccess.get(f.player).addResearch("FOCALMANIPULATION");
            f.put(f.base, ThaumcraftFocalManipulation.FOCAL_MANIPULATOR.get());
            test.assertTrue(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerFocalManipulator,
                    "actual focal-manipulator block interaction opens its native menu");
            var focal = (ContainerFocalManipulator)f.player.containerMenu;
            f.player.getInventory().setItem(9, new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()));
            f.player.getInventory().setItem(10, new ItemStack(ThaumcraftFoci.FIRE_FOCUS.get()));
            focal.clicked(1, 0, ClickType.PICKUP, f.player); focal.clicked(0, 0, ClickType.PICKUP, f.player);
            focal.clicked(2, 0, ClickType.PICKUP, f.player); focal.clicked(0, 0, ClickType.PICKUP, f.player);
            var tile = (TileFocalManipulator)f.level.getBlockEntity(f.base);
            test.assertTrue(tile.getItem(0).getCount() == 1 && focal.getCarried().getCount() == 1,
                    "a second identical unstackable focus stays on the cursor instead of producing a count-two invalid upgrade input");
            focal.clicked(2, 0, ClickType.PICKUP, f.player); f.player.closeContainer();

            f.put(f.base, ThaumcraftContent.TABLE.get()); f.put(f.base.north(), ThaumcraftContent.TABLE.get());
            f.level.setBlock(f.base, ThaumcraftContent.TABLE.get().defaultBlockState().setValue(BlockTable.VARIANT, 2), TubeVariantsRuntimeChecks.QUIET);
            f.level.setBlock(f.base.north(), ThaumcraftContent.TABLE.get().defaultBlockState().setValue(BlockTable.VARIANT, 7), TubeVariantsRuntimeChecks.QUIET);
            test.assertTrue(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerResearchTable,
                    "actual research-table block interaction opens its native input slots");
            var research = (ContainerResearchTable)f.player.containerMenu;
            f.player.getInventory().setItem(9, new ItemStack(ThaumcraftContent.INKWELL.get()));
            f.player.getInventory().setItem(10, new ItemStack(ThaumcraftContent.INKWELL.get()));
            research.clicked(2, 0, ClickType.PICKUP, f.player); research.clicked(0, 0, ClickType.PICKUP, f.player);
            research.clicked(3, 0, ClickType.PICKUP, f.player); research.clicked(0, 0, ClickType.PICKUP, f.player);
            ItemStack ink = ((TileResearchTable)f.level.getBlockEntity(f.base)).getItem(0);
            test.assertTrue(ink.getCount() == 1 && research.getCarried().getCount() == 1, "a second inkwell is not merged by the real native pickup path");
            test.assertTrue(ScribingInk.consumeFromTable(ink, true) && ink.getDamageValue() == 1 && research.getCarried().getDamageValue() == 0,
                    "one table ink charge changes only the one tool in its physical slot");
            research.clicked(3, 0, ClickType.PICKUP, f.player);
        }
        test.succeed();
    }

    public static void handMirrorOwnsOneOutputAcrossCloseCancelAndRetry(GameTestHelper test) {
        var f = new TubeVariantsRuntimeChecks.Fixture(test); f.open();
        String token = UUID.randomUUID().toString();
        int[] attempts = {0}; boolean[] done = {false}; Throwable[] callbackFailure = {null};
        ContainerHandMirror[] source = {null};
        EntityEvent.Add observer = (entity, world) -> {
            if (world != f.level || !(entity instanceof ItemEntity item) || !marked(item.getItem(), token)) return EventResult.pass();
            try {
                ++attempts[0];
                test.assertTrue(source[0].inputStack().isEmpty(), "source input was already relinquished before native ADD");
                test.assertTrue(item.getPortalCooldown() == 20 && !item.hasPickUpDelay(), "original portal cooldown20 and pickup delay0 exist before ADD");
                f.player.closeContainer();
                source[0].clicked(0, 0, ClickType.PICKUP, f.player);
                source[0].getSlot(0).set(item.getItem().copy()); // A closed/reentrant menu cannot acquire another copy.
                source[0].removed(f.player); // Idempotent duplicate close.
                test.assertTrue(source[0].inputStack().isEmpty() && playerCount(f, token) == 0, "close/reentry cannot refund the destination's already-owned stock");
            } catch (RuntimeException | Error failure) { callbackFailure[0] = failure; }
            return attempts[0] == 1 ? EventResult.interruptFalse() : EventResult.pass();
        };
        Runnable cleanup = () -> {
            if (done[0]) return; done[0] = true; EntityEvent.ADD.unregister(observer);
            f.drops().stream().filter(item -> marked(item.getItem(), token)).forEach(ItemEntity::discard); f.close();
        };
        BoreWorkRuntimeChecks.testInfo(test).addListener(new GameTestListener() {
            @Override public void testStructureLoaded(GameTestInfo info) {}
            @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { cleanup.run(); }
            @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { cleanup.run(); }
            @Override public void testAddedForRerun(GameTestInfo previous, GameTestInfo next, GameTestRunner runner) { cleanup.run(); }
        });
        try {
            f.put(f.base, ThaumcraftEldritchBlocks.MIRROR.get());
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftLegacyItems.HAND_MIRROR.get()));
            test.assertTrue(f.use(f.base).consumesAction() && ItemHandMirror.link(f.player.getMainHandItem()) != null, "native item-on-mirror interaction binds the hand mirror");
            test.assertTrue(f.player.gameMode.useItem(f.player, f.level, f.player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction()
                    && f.player.containerMenu instanceof ContainerHandMirror, "native held-item use opens the registered hand-mirror menu");
            source[0] = (ContainerHandMirror)f.player.containerMenu;
            var tag = new CompoundTag(); tag.putString(MARKER, token);
            var stock = new ItemStack(Items.DIAMOND, 7); stock.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            f.player.getInventory().setItem(9, stock);
            EntityEvent.ADD.register(observer);
            source[0].clicked(1, 0, ClickType.QUICK_MOVE, f.player);
            if (callbackFailure[0] != null) throw new AssertionError("mirror ADD callback", callbackFailure[0]);
            test.assertTrue(attempts[0] == 1 && playerCount(f, token) == 0 && source[0].inputStack().isEmpty(), "canceled native admission neither returns nor duplicates the paid input");
            var queue = f.level.getDataStorage().get(QUEUE, "thaumcraft_pending_inventory_drops");
            test.assertTrue(queue != null, "the destination owns a production durable queue");
            var saved = queue.save(new CompoundTag(), f.level.registryAccess());
            var owned = markedRows(saved, f, token);
            test.assertTrue(owned.size() == 1 && owned.getFirst().getInt("Count") == 7 && owned.getFirst().getInt("PortalCooldown") == 20,
                    "canceled output retains exactly seven items and original cooldown in saved custody");
            var roundTrip = DeferredItemDrops.load(saved, f.level.registryAccess()).save(new CompoundTag(), f.level.registryAccess());
            test.assertTrue(markedRows(roundTrip, f, token).size() == 1 && markedRows(roundTrip, f, token).getFirst().getInt("PortalCooldown") == 20,
                    "the optional portal cooldown survives the real queue codec (detached read only; not replayed into the live world)");
            test.onEachTick(() -> {
                if (done[0]) return;
                if (callbackFailure[0] != null) throw new AssertionError("mirror ADD callback", callbackFailure[0]);
                if (attempts[0] < 2) return;
                test.assertTrue(attempts[0] == 2 && playerCount(f, token) == 0
                        && f.drops().stream().filter(item -> marked(item.getItem(), token)).mapToInt(item -> item.getItem().getCount()).sum() == 7,
                        "ordinary server ticks retry once into exactly one seven-item output, with no source refund");
                cleanup.run(); test.succeed();
            });
            test.runAfterDelay(80, () -> { if (!done[0]) test.fail("canceled mirror output was not retried by the native queue ticker"); });
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }

    public static void handMirrorMissingDestinationReturnsInputOnce(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            String token = UUID.randomUUID().toString();
            f.put(f.base, ThaumcraftEldritchBlocks.MIRROR.get());
            f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftLegacyItems.HAND_MIRROR.get()));
            test.assertTrue(f.use(f.base).consumesAction() && f.player.gameMode.useItem(f.player, f.level,
                    f.player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction()
                    && f.player.containerMenu instanceof ContainerHandMirror, "native linked hand mirror opens before its destination disappears");
            var menu = (ContainerHandMirror)f.player.containerMenu;
            f.put(f.base, net.minecraft.world.level.block.Blocks.AIR);
            var data = new CompoundTag(); data.putString(MARKER, token);
            var stock = new ItemStack(Items.DIAMOND, 7); stock.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            f.player.getInventory().setItem(9, stock);
            menu.clicked(1, 0, ClickType.QUICK_MOVE, f.player);
            test.assertTrue(menu.inputStack().getCount() == 7 && playerCount(f, token) == 0,
                    "refusal before queue publication restores the sole actual input to the still-open menu");
            f.player.closeContainer(); menu.removed(f.player);
            test.assertTrue(menu.inputStack().isEmpty() && playerCount(f, token) == 7,
                    "ordinary close returns that input exactly once, including a repeated close callback");
            test.assertTrue(f.drops().stream().noneMatch(item -> marked(item.getItem(), token)), "failed-link input was never also emitted");
        }
        test.succeed();
    }

    private static boolean marked(ItemStack stack, String token) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(MARKER).equals(token);
    }
    private static int playerCount(TubeVariantsRuntimeChecks.Fixture f, String token) {
        return f.player.getInventory().items.stream().filter(stack -> marked(stack, token)).mapToInt(ItemStack::getCount).sum();
    }
    private static List<CompoundTag> markedRows(CompoundTag saved, TubeVariantsRuntimeChecks.Fixture f, String token) {
        var result = new java.util.ArrayList<CompoundTag>();
        for (Tag entry : saved.getList("Pending", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag)entry;
            var stack = ItemStack.parse(f.level.registryAccess(), row.getCompound("Stack")).orElse(ItemStack.EMPTY);
            if (marked(stack, token)) result.add(row);
        }
        return result;
    }
}

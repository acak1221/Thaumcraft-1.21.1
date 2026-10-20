package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.TableTopology;
import thaumcraft.common.tiles.TileResearchTable;
import thaumcraft.common.tiles.TileTable;

/** Native world/menu interactions; the explicit far-chunk case also waits for actual unload/reload. */
public final class ResearchTableRuntimeChecks {
    private static final TicketType<BlockPos> LIFECYCLE_TICKET = TicketType.create(
            "thaumcraft-research-table-test", Comparator.comparingLong(BlockPos::asLong));
    private ResearchTableRuntimeChecks() {}

    public static void originalTableCraftPlacementAndDrops(GameTestHelper test, ServerPlayer player) {
        try (var fixture = new Fixture(test, player)) {
            List<ItemStack> grid = List.of(new ItemStack(Items.OAK_SLAB), new ItemStack(Items.BIRCH_SLAB),
                    new ItemStack(Items.SPRUCE_SLAB), new ItemStack(Items.OAK_PLANKS), ItemStack.EMPTY,
                    new ItemStack(Items.BIRCH_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            var input = CraftingInput.of(3, 3, grid);
            var recipe = fixture.level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, fixture.level).orElseThrow();
            test.assertValueEqual(recipe.id().toString(), "thaumcraft:table", "original SSS/W W recipe accepts mixed wooden variants");
            var crafted = recipe.value().assemble(input, fixture.level.registryAccess());
            test.assertTrue(crafted.is(ThaumcraftContent.TABLE_ITEM.get()) && crafted.getCount() == 1, "original recipe yields one table");
            var pos = fixture.center;
            for (int yaw : new int[] {0, 90}) {
                fixture.clear(pos);
                fixture.set(pos.below(), Blocks.STONE.defaultBlockState());
                player.setYRot(yaw);
                player.setItemInHand(InteractionHand.MAIN_HAND, crafted.copy());
                var result = use(player, pos.below(), InteractionHand.MAIN_HAND);
                test.assertTrue(result.consumesAction() && fixture.level.getBlockState(pos).is(ThaumcraftContent.TABLE.get()), "native block-item interaction places a table");
                test.assertTrue(variant(fixture.level, pos) == (yaw == 0 ? 0 : 1), "placement retains original horizontal axis metadata");
                test.assertTrue(fixture.level.getBlockEntity(pos) instanceof TileTable, "base placement creates dummy table BE");
                test.assertTrue(player.getMainHandItem().isEmpty(), "survival placement consumes its one crafted table");
            }
            for (int state = 0; state <= 9; state++) {
                fixture.set(pos, table().state(state));
                var blockState = fixture.level.getBlockState(pos);
                var drops = Block.getDrops(blockState, fixture.level, pos, fixture.level.getBlockEntity(pos));
                test.assertTrue(drops.size() == 1 && drops.getFirst().is(ThaumcraftContent.TABLE_ITEM.get())
                        && drops.getFirst().getCount() == 1, "every source table state has one raw base-table drop: " + state);
                var pick = table().getCloneItemStack(fixture.level, pos, blockState);
                test.assertTrue(pick.is(state >= 2 ? ThaumcraftContent.RESEARCH_TABLE_ITEM.get() : ThaumcraftContent.TABLE_ITEM.get()),
                        "pick identity preserves source base/research distinction: " + state);
            }
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    public static void tableConversionDirectionsAndCosts(GameTestHelper test, ServerPlayer player) {
        try (var fixture = new Fixture(test, player)) {
            var center = fixture.center;
            for (Direction direction : TableTopology.SEARCH_ORDER) {
                fixture.clearCross(center);
                fixture.set(center, table().state(1));
                fixture.set(center.relative(direction), table().state(0));
                var tool = ink(100);
                tool.set(DataComponents.CUSTOM_NAME, Component.literal("exhausted-original-tool"));
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                var result = use(player, center, InteractionHand.MAIN_HAND);
                int main = TableTopology.mainVariant(direction);
                test.assertTrue(result.consumesAction() && variant(fixture.level, center) == main
                        && variant(fixture.level, center.relative(direction)) == TableTopology.partnerVariant(main), "native conversion preserves exact directional states: " + direction);
                test.assertTrue(fixture.level.getBlockEntity(center) instanceof TileResearchTable
                        && fixture.level.getBlockEntity(center.relative(direction)) instanceof TileTable, "main/secondary use distinct native BE classes");
                var stored = research(fixture.level, center).getItem(0);
                test.assertTrue(stored.getDamageValue() == 100 && stored.getCount() == 1
                        && Component.literal("exhausted-original-tool").equals(stored.get(DataComponents.CUSTOM_NAME)), "conversion accepts and copies exhausted tool/components without damage");
                test.assertTrue(player.getMainHandItem().isEmpty(), "survival conversion transfers exactly one held tool");
            }
            fixture.clearCross(center);
            fixture.set(center, table().state(0)); fixture.set(center.north(), table().state(1));
            player.setGameMode(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, ink(99));
            var creativeBefore = player.getMainHandItem().copy();
            test.assertTrue(use(player, center, InteractionHand.MAIN_HAND).consumesAction(), "creative converts through native item/block interaction");
            test.assertTrue(ItemStack.matches(player.getMainHandItem(), creativeBefore)
                    && ItemStack.matches(research(fixture.level, center).getItem(0), creativeBefore), "creative retains original tool and puts an identical copy in the table");

            fixture.clearCross(center);
            fixture.set(center, table().state(0)); fixture.set(center.east(), table().state(0));
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            player.setItemInHand(InteractionHand.OFF_HAND, ink(23));
            test.assertTrue(use(player, center, InteractionHand.OFF_HAND).consumesAction(), "sneaking offhand reaches the native ItemInkwell.useOn fallback");
            test.assertTrue(player.getOffhandItem().isEmpty() && player.getMainHandItem().is(Items.STICK)
                    && research(fixture.level, center).getItem(0).getDamageValue() == 23, "actual offhand tool is transferred and main hand is untouched");
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    public static void tableSecondaryQuirksAndSearchOrder(GameTestHelper test, ServerPlayer player) {
        try (var fixture = new Fixture(test, player)) {
            var center = fixture.center;
            fixture.clearCross(center);
            fixture.set(center, table().state(0));
            for (Direction direction : TableTopology.SEARCH_ORDER) fixture.set(center.relative(direction), table().state(0));
            player.setItemInHand(InteractionHand.MAIN_HAND, ink(0));
            use(player, center, InteractionHand.MAIN_HAND);
            test.assertTrue(variant(fixture.level, center) == 2 && variant(fixture.level, center.north()) == 7
                    && variant(fixture.level, center.south()) == 0, "conversion searches north before south/west/east");
            for (int secondary : new int[] {7, 8, 9}) {
                fixture.clearCross(center);
                fixture.set(center, table().state(secondary)); fixture.set(center.north(), table().state(0));
                player.setItemInHand(InteractionHand.MAIN_HAND, ink(0));
                test.assertTrue(use(player, center, InteractionHand.MAIN_HAND).consumesAction()
                        && variant(fixture.level, center) == 2 && player.getMainHandItem().isEmpty(), "source permits reconverting clicked secondary " + secondary);
            }
            fixture.clearCross(center);
            fixture.set(center, table().state(6)); fixture.set(center.south(), table().state(0));
            player.setItemInHand(InteractionHand.MAIN_HAND, ink(0));
            use(player, center, InteractionHand.MAIN_HAND);
            test.assertTrue(variant(fixture.level, center) == 6 && variant(fixture.level, center.south()) == 0
                    && player.getMainHandItem().getCount() == 1, "source excludes only clicked secondary6 from conversion");

            fixture.clearCross(center);
            fixture.set(center, table().state(7));
            fixture.set(center.north(), table().state(3));
            fixture.set(center.south(), table().state(2));
            test.assertTrue(BlockTable.findOpeningMain(fixture.level, center) == research(fixture.level, center.north()),
                    "secondary opening uses first adjacent main even when its own link points south");
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    public static void sharedTableMenusFiltersAndQuickMove(GameTestHelper test, ServerPlayer first, ServerPlayer second) {
        try (var fixture = new Fixture(test, first, second)) {
            var pos = fixture.center;
            fixture.pair(pos, Direction.NORTH);
            var tile = research(fixture.level, pos);
            first.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            second.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            test.assertTrue(use(first, pos, InteractionHand.MAIN_HAND).consumesAction(), "native main activation opens extended menu");
            test.assertTrue(use(second, pos.north(), InteractionHand.MAIN_HAND).consumesAction(), "native secondary activation opens shared extended menu");
            test.assertTrue(first.containerMenu instanceof ContainerResearchTable && second.containerMenu instanceof ContainerResearchTable,
                    "both native server menu openings create registered research menu");
            var a = (ContainerResearchTable)first.containerMenu;
            var b = (ContainerResearchTable)second.containerMenu;
            test.assertTrue(a != b && a.tile() == tile && b.tile() == tile && a.mainPos().equals(pos) && b.mainPos().equals(pos)
                    && a.slots.size() == 38 && b.slots.size() == 38, "two players share one physical two-slot BE through separate 38-slot menus");
            var normal = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), false);
            var discovery = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            var tool = ink(100);
            test.assertTrue(a.getSlot(0).mayPlace(tool) && !a.getSlot(0).mayPlace(normal)
                    && a.getSlot(1).mayPlace(normal) && a.getSlot(1).mayPlace(discovery)
                    && !a.getSlot(1).mayPlace(new ItemStack(Items.PAPER)), "GUI filters use source item classes and accept exhausted tools/discoveries");
            test.assertTrue(tile.canPlaceItem(0, tool) && tile.canPlaceItem(1, normal)
                    && !tile.canPlaceItem(1, discovery), "automation intentionally excludes discovery presentation");
            tile.setItem(0, tool);
            test.assertTrue(!a.quickMoveStack(first, 0).isEmpty() && tile.getItem(0).isEmpty()
                    && first.getInventory().getItem(8).getDamageValue() == 100, "table-to-player shift transfer starts at reverse hotbar slot8");
            test.assertTrue(!a.quickMoveStack(first, 37).isEmpty() && !tile.getItem(0).isEmpty()
                    && first.getInventory().getItem(8).isEmpty(), "player-to-table transfer returns tool to slot0");
            discovery.set(DataComponents.CUSTOM_NAME, Component.literal("shared-discovery"));
            first.getInventory().setItem(9, discovery.copy());
            test.assertTrue(!a.quickMoveStack(first, 2).isEmpty() && ItemStack.matches(b.getSlot(1).getItem(), discovery), "discovery shift transfer is immediately visible through the second physical view");
            first.getInventory().setItem(10, new ItemStack(Items.PAPER, 3));
            test.assertTrue(a.quickMoveStack(first, 3).isEmpty() && first.getInventory().getItem(10).getCount() == 3,
                    "invalid paper has no fallback player-inventory rearrangement");
            test.assertTrue(a.clickMenuButton(first, 1) && !a.clickMenuButton(first, 5), "source no-op button1 remains while copy button5 is not implemented by R07");
            tile.setChanged(); a.broadcastChanges(); b.broadcastChanges();
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    public static void tableMenuDistanceAndStaleIdentity(GameTestHelper test, ServerPlayer player) {
        try (var fixture = new Fixture(test, player)) {
            var pos = fixture.center;
            fixture.pair(pos, Direction.NORTH);
            var tile = research(fixture.level, pos);
            var menu = new ContainerResearchTable(77, player.getInventory(), tile);
            player.setPos(pos.getX() + 8.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            test.assertTrue(menu.stillValid(player), "source distance squared64 is inclusive");
            player.setPos(pos.getX() + 8.501, pos.getY() + 0.5, pos.getZ() + 0.5);
            test.assertTrue(!menu.stillValid(player), "distance beyond squared64 refuses menu interaction");
            player.setPos(Vec3.atCenterOf(pos));
            fixture.level.removeBlockEntity(pos);
            fixture.level.setBlockEntity(new TileResearchTable(pos, fixture.level.getBlockState(pos)));
            test.assertTrue(!menu.stillValid(player) && menu.quickMoveStack(player, 0).isEmpty(), "same-state replacement invalidates stale BE identity");
            var replacement = research(fixture.level, pos);
            test.assertTrue(replacement.stillValid(player) && !tile.stillValid(player), "replacement menu identity is valid independently of stale instance");
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    public static void tableInventoryComponentsAndCountsPersist(GameTestHelper test, ServerPlayer player) {
        try (var fixture = new Fixture(test, player)) {
            var pos = fixture.center;
            fixture.pair(pos, Direction.EAST);
            var tile = research(fixture.level, pos);
            var tool = ink(99); tool.set(DataComponents.CUSTOM_NAME, Component.literal("persistent-ink"));
            var note = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            note.set(DataComponents.CUSTOM_NAME, Component.literal("persistent-note"));
            note.setCount(70); tile.setItem(1, note);
            test.assertTrue(tile.getItem(1).getCount() == 70, "R10 whole-stack return preserves copied-note counts above64; ordinary tools retain their setter limit");
            tile.setItem(0, tool);
            tile.getItem(1).setCount(127); // Model direct original load/R10 state, not a legal setter insertion.
            var expected = tile.getItem(1).copy();
            var saved = tile.saveWithFullMetadata(fixture.level.registryAccess());
            var restored = new TileResearchTable(pos, tile.getBlockState());
            restored.loadWithComponents(saved, fixture.level.registryAccess());
            test.assertTrue(ItemStack.matches(restored.getItem(0), tool) && ItemStack.matches(restored.getItem(1), expected),
                    "native BE persistence keeps exact components and direct-loaded count127 without setter truncation");
            var update = new TileResearchTable(pos, tile.getBlockState());
            update.loadWithComponents(tile.getUpdateTag(fixture.level.registryAccess()), fixture.level.registryAccess());
            test.assertTrue(ItemStack.matches(update.getItem(0), tool) && ItemStack.matches(update.getItem(1), expected), "native update tag carries both physical stacks/components/count");
            var packet = tile.getUpdatePacket();
            var packetView = new TileResearchTable(pos, tile.getBlockState());
            packetView.loadWithComponents(packet.getTag(), fixture.level.registryAccess());
            test.assertTrue(ItemStack.matches(packetView.getItem(1), expected), "native block-entity update packet contains full note component/count");
            test.assertTrue(!tile.isValidBlockState(table().state(8)) && !new TileTable(pos, table().state(8)).isValidBlockState(table().state(5)),
                    "client chunk state checks reject the opposite BE class during same-block conversion");
            var removed = restored.removeItem(1, 2);
            test.assertTrue(removed.getCount() == 2 && restored.getItem(1).getCount() == 125, "native partial extraction conserves direct-loaded count");
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    public static void tableBreakEvacuatesEachHalfOnce(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return;
            closed[0] = true;
            fixture.close();
        };
        // Native queue output has no fixture pickup protection. Keep the actual
        // player away until assertions run after the safe server-level-post.
        player.setPos(fixture.center.getX() + 0.5, fixture.center.getY() + 6, fixture.center.getZ() + 0.5);
        breakHalfAfterSafeDelivery(test, fixture, player, true, () ->
                breakHalfAfterSafeDelivery(test, fixture, player, false, () -> {
                    fixture.assertKnowledgeUnchanged();
                    cleanup.run();
                    test.succeed();
                }, cleanup), cleanup);
    }

    private static void breakHalfAfterSafeDelivery(GameTestHelper test, Fixture fixture, ServerPlayer player,
                                                  boolean breakMain, Runnable next, Runnable cleanup) {
        try {
            var pos = fixture.center;
            fixture.clearCross(pos);
            fixture.discardNewDrops();
            fixture.pair(pos, Direction.NORTH);
            var tile = research(fixture.level, pos);
            var tool = ink(73); tool.set(DataComponents.CUSTOM_NAME, Component.literal("break-tool"));
            var note = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            note.set(DataComponents.CUSTOM_NAME, Component.literal("break-note"));
            tile.setItem(0, tool.copy()); tile.setItem(1, note.copy());
            var broken = breakMain ? pos : pos.north();
            test.assertTrue(fixture.level.destroyBlock(broken, true, player), "native destroy removes selected half");
            var remaining = breakMain ? pos.north() : pos;
            table().validateTopology(fixture.level, remaining);
            table().validateTopology(fixture.level, remaining);
            tile.evacuate(); tile.evacuate();
            test.assertTrue(tile.isEmpty() && variant(fixture.level, remaining) == 0
                    && fixture.level.getBlockEntity(remaining) instanceof TileTable,
                    "native teardown immediately clears physical slots and leaves an empty base half");
            var immediate = fixture.newDrops();
            test.assertTrue(matching(immediate, tool) == 0 && matching(immediate, note) == 0,
                    "physical contents cannot spawn reentrant entity callbacks inside onRemove/setBlock");
            test.runAfterDelay(2, () -> {
                try {
                    var drops = fixture.newDrops();
                    test.assertTrue(drops.size() == 3 && matching(drops, tool) == 1 && matching(drops, note) == 1
                            && drops.stream().filter(e -> e.getItem().is(ThaumcraftContent.TABLE_ITEM.get())).mapToInt(e -> e.getItem().getCount()).sum() == 1,
                            "after actual server-level-post, breaking either half delivers one raw table and each physical stack exactly once: " + breakMain);
                    next.run();
                } catch (RuntimeException | Error error) {
                    cleanup.run(); throw error;
                }
            });
        } catch (RuntimeException | Error error) {
            cleanup.run(); throw error;
        }
    }

    public static void tablePairAcrossChunkBoundaryPersists(GameTestHelper test, ServerPlayer player) {
        try (var fixture = new Fixture(test, player)) {
            var pos = new BlockPos((fixture.center.getX() & ~15) + 15, fixture.center.getY(), fixture.center.getZ());
            var partner = pos.east();
            fixture.level.getChunkAt(pos); fixture.level.getChunkAt(partner);
            fixture.clearCross(pos);
            fixture.set(pos, table().state(1)); fixture.set(partner, table().state(0));
            player.setPos(Vec3.atCenterOf(pos));
            player.setItemInHand(InteractionHand.MAIN_HAND, ink(17));
            test.assertTrue(use(player, pos, InteractionHand.MAIN_HAND).consumesAction(), "native conversion works across two loaded chunk objects");
            test.assertTrue((pos.getX() >> 4) != (partner.getX() >> 4)
                    && fixture.level.getChunkAt(pos) != fixture.level.getChunkAt(partner), "fixture actually straddles a native chunk boundary");
            var tile = research(fixture.level, pos);
            var note = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), false);
            tile.setItem(1, note.copy());
            var mainSaved = tile.saveWithFullMetadata(fixture.level.registryAccess());
            var secondarySaved = fixture.level.getBlockEntity(partner).saveWithFullMetadata(fixture.level.registryAccess());
            var mainState = fixture.level.getBlockState(pos); var secondaryState = fixture.level.getBlockState(partner);
            // Recreate BEs from native NBT without claiming that either chunk was unloaded.
            fixture.level.removeBlockEntity(pos); fixture.level.removeBlockEntity(partner);
            var main = new TileResearchTable(pos, mainState); main.loadWithComponents(mainSaved, fixture.level.registryAccess());
            var secondary = new TileTable(partner, secondaryState); secondary.loadWithComponents(secondarySaved, fixture.level.registryAccess());
            fixture.level.setBlockEntity(main); fixture.level.setBlockEntity(secondary);
            table().validateTopology(fixture.level, pos); table().validateTopology(fixture.level, partner);
            test.assertTrue(variant(fixture.level, pos) == 5 && variant(fixture.level, partner) == 8
                    && main.getItem(0).getDamageValue() == 17 && ItemStack.matches(main.getItem(1), note)
                    && BlockTable.findOpeningMain(fixture.level, partner) == main, "cross-chunk pair survives native BE data restoration with shared contents/link intact");
            fixture.assertKnowledgeUnchanged();
        }
        test.succeed();
    }

    /** Uses a remote chunk in this generated GameTest world, never a private/user fixture world. */
    public static void tablePairActuallyUnloadsAndReloads(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        var pos = new BlockPos((fixture.center.getX() & ~15) + 1_048_580,
                fixture.center.getY(), (fixture.center.getZ() & ~15) + 1_048_580);
        var chunkPos = new ChunkPos(pos);
        var source = fixture.level.getChunkSource();
        boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return;
            closed[0] = true;
            try {
                fixture.close();
            } finally {
                source.removeRegionTicket(LIFECYCLE_TICKET, chunkPos, 0, pos);
                source.removeRegionTicket(LIFECYCLE_TICKET, chunkPos, 2, pos);
            }
        };
        try {
            // Radius0 grants FULL access without keeping the chunk entity-ticking.
            // No player is moved here, so removing our own ticket permits unload.
            source.addRegionTicket(LIFECYCLE_TICKET, chunkPos, 0, pos);
            var oldChunk = fixture.level.getChunkAt(pos);
            fixture.pair(pos, Direction.NORTH);
            var oldTile = research(fixture.level, pos);
            var tool = ink(46); tool.set(DataComponents.CUSTOM_NAME, Component.literal("disk-reloaded-tool"));
            var note = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            note.set(DataComponents.CUSTOM_NAME, Component.literal("disk-reloaded-note"));
            oldTile.setItem(0, tool.copy()); oldTile.setItem(1, note.copy());
            source.removeRegionTicket(LIFECYCLE_TICKET, chunkPos, 0, pos);
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    // getChunkNow does not load. A null alone can mean demotion;
                    // removed old BE additionally proves ServerLevel.unload ran.
                    if (source.getChunkNow(chunkPos.x, chunkPos.z) != null || !oldTile.isRemoved()) return;
                    source.addRegionTicket(LIFECYCLE_TICKET, chunkPos, 2, pos);
                    var reloadedChunk = fixture.level.getChunkAt(pos);
                    var reloaded = research(fixture.level, pos);
                    test.assertTrue(reloadedChunk != oldChunk && reloaded != oldTile,
                            "actual server unload is followed by a fresh native chunk and BE, not manual NBT recreation");
                    test.assertTrue(variant(fixture.level, pos) == 2 && variant(fixture.level, pos.north()) == 7
                            && ItemStack.matches(reloaded.getItem(0), tool) && ItemStack.matches(reloaded.getItem(1), note),
                            "native chunk disk save/reload preserves directional pair, complete note and item components");
                    test.assertTrue(fixture.level.getBlockEntity(pos.north()) instanceof TileTable
                            && BlockTable.findOpeningMain(fixture.level, pos.north()) == reloaded,
                            "native chunk reload restores secondary BE and shared menu target");
                    fixture.assertKnowledgeUnchanged();
                    cleanup.run();
                    test.succeed();
                } catch (RuntimeException | Error error) {
                    cleanup.run();
                    throw error;
                }
            });
            test.runAtTickTime(540, () -> {
                if (closed[0]) return;
                cleanup.run();
                test.fail("isolated chunk did not actually unload before the bounded lifecycle deadline");
            });
        } catch (RuntimeException | Error error) {
            cleanup.run();
            throw error;
        }
    }

    /** Actual native BE ticks repair loaded orphans without onPlace or a saved scheduled tick. */
    public static void tableFirstNativeTickRepairsReloadedOrphans(GameTestHelper test, ServerPlayer player) {
        var fixture = new Fixture(test, player);
        boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return;
            closed[0] = true;
            fixture.close();
        };
        try {
            var mainPos = fixture.center;
            var otherMain = mainPos.east(2);
            var secondaryPos = otherMain.north();
            fixture.pair(mainPos, Direction.NORTH);
            fixture.pair(otherMain, Direction.NORTH);
            var tool = ink(52); tool.set(DataComponents.CUSTOM_NAME, Component.literal("orphan-repair-tool"));
            var note = ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(), true);
            research(fixture.level, mainPos).setItem(0, tool.copy());
            research(fixture.level, mainPos).setItem(1, note.copy());
            // Keep native pickup from hiding the output before our next-tick assertion.
            player.setPos(mainPos.getX() + 0.5, mainPos.getY() + 6, mainPos.getZ() + 0.5);
            test.runAfterDelay(3, () -> {
                try {
                    test.assertTrue(!fixture.level.getBlockTicks().hasScheduledTick(mainPos, table())
                            && !fixture.level.getBlockTicks().hasScheduledTick(secondaryPos, table()),
                            "initial onPlace ticks have drained before the cold-load fixture is introduced");
                    var oldMain = research(fixture.level, mainPos);
                    var mainSaved = oldMain.saveWithFullMetadata(fixture.level.registryAccess());
                    var secondarySaved = fixture.level.getBlockEntity(secondaryPos).saveWithFullMetadata(fixture.level.registryAccess());
                    // Flags18 suppress neighbour notifications; main state never
                    // changes, so no main onPlace callback schedules the repair.
                    fixture.clear(mainPos.north()); fixture.clear(otherMain);
                    fixture.level.removeBlockEntity(mainPos); fixture.level.removeBlockEntity(secondaryPos);
                    var loadedMain = new TileResearchTable(mainPos, fixture.level.getBlockState(mainPos));
                    loadedMain.loadWithComponents(mainSaved, fixture.level.registryAccess());
                    var loadedSecondary = new TileTable(secondaryPos, fixture.level.getBlockState(secondaryPos));
                    loadedSecondary.loadWithComponents(secondarySaved, fixture.level.registryAccess());
                    fixture.level.setBlockEntity(loadedMain); fixture.level.setBlockEntity(loadedSecondary);
                    test.assertTrue(variant(fixture.level, mainPos) == 2 && variant(fixture.level, secondaryPos) == 7
                            && !fixture.level.getBlockTicks().hasScheduledTick(mainPos, table())
                            && !fixture.level.getBlockTicks().hasScheduledTick(secondaryPos, table()),
                            "recreated native BEs begin orphaned with no scheduled block tick");
                    test.runAfterDelay(3, () -> {
                        try {
                            test.assertTrue(variant(fixture.level, mainPos) == 0 && variant(fixture.level, secondaryPos) == 0
                                    && fixture.level.getBlockEntity(mainPos) instanceof TileTable
                                    && fixture.level.getBlockEntity(secondaryPos) instanceof TileTable,
                                    "first actual server BE tick repairs both main and secondary orphans without manual validation");
                            loadedMain.evacuate(); loadedMain.evacuate();
                            var drops = fixture.newDrops();
                            test.assertTrue(loadedMain.isEmpty() && drops.size() == 2 && matching(drops, tool) == 1 && matching(drops, note) == 1,
                                    "after actual server-level-post, cold-load main repair delivers each physical stack exactly once");
                            fixture.assertKnowledgeUnchanged();
                            cleanup.run();
                            test.succeed();
                        } catch (RuntimeException | Error error) {
                            cleanup.run(); throw error;
                        }
                    });
                } catch (RuntimeException | Error error) {
                    cleanup.run(); throw error;
                }
            });
        } catch (RuntimeException | Error error) {
            cleanup.run(); throw error;
        }
    }

    private static BlockTable table() { return ThaumcraftContent.TABLE.get(); }
    private static int variant(ServerLevel level, BlockPos pos) { return level.getBlockState(pos).getValue(BlockTable.VARIANT); }
    private static TileResearchTable research(ServerLevel level, BlockPos pos) { return (TileResearchTable)level.getBlockEntity(pos); }
    private static ItemStack ink(int damage) { var result = new ItemStack(ThaumcraftContent.INKWELL.get()); result.setDamageValue(damage); return result; }
    private static long matching(List<ItemEntity> entities, ItemStack expected) { return entities.stream().filter(entity -> ItemStack.matches(entity.getItem(), expected)).count(); }
    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        var hit = new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), Direction.UP, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getItemInHand(hand), hand, hit);
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos center;
        final Map<BlockPos, SavedBlock> blocks = new LinkedHashMap<>();
        final Set<UUID> originalDrops = new HashSet<>();
        final List<PlayerState> players = new ArrayList<>();
        Fixture(GameTestHelper test, ServerPlayer... players) {
            this.test = test; level = test.getLevel(); center = test.absolutePos(new BlockPos(3, 2, 3));
            for (ServerPlayer player : players) {
                this.players.add(new PlayerState(player));
                player.getInventory().clearContent(); player.getInventory().selected = 0;
                player.setGameMode(GameType.SURVIVAL); player.setShiftKeyDown(false);
                player.setPos(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5);
            }
        }
        void remember(BlockPos pos) {
            pos = pos.immutable();
            if (!blocks.containsKey(pos)) {
                var be = level.getBlockEntity(pos);
                blocks.put(pos, new SavedBlock(level.getBlockState(pos), be == null ? null : be.saveWithFullMetadata(level.registryAccess())));
                for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) originalDrops.add(entity.getUUID());
            }
        }
        void set(BlockPos pos, BlockState state) {
            remember(pos);
            if (level.getBlockEntity(pos) instanceof TileResearchTable tile) tile.clearContent();
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        void clear(BlockPos pos) { set(pos, Blocks.AIR.defaultBlockState()); }
        void clearCross(BlockPos pos) { clear(pos); for (Direction direction : TableTopology.SEARCH_ORDER) clear(pos.relative(direction)); }
        void pair(BlockPos pos, Direction direction) {
            clearCross(pos);
            set(pos, table().state(TableTopology.mainVariant(direction)));
            set(pos.relative(direction), table().state(TableTopology.partnerVariant(TableTopology.mainVariant(direction))));
        }
        List<ItemEntity> newDrops() {
            Map<UUID, ItemEntity> result = new LinkedHashMap<>();
            for (BlockPos pos : blocks.keySet()) for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) {
                if (!originalDrops.contains(entity.getUUID())) result.put(entity.getUUID(), entity);
            }
            return new ArrayList<>(result.values());
        }
        void discardNewDrops() { for (ItemEntity entity : newDrops()) entity.discard(); }
        void assertKnowledgeUnchanged() { for (PlayerState player : players) test.assertValueEqual(KnowledgeAccess.get(player.player).save(), player.knowledge.save(), "table lifecycle/menu does not change knowledge, pools, clues, scans or warp"); }
        @Override public void close() {
            for (PlayerState player : players) player.player.closeContainer();
            for (BlockPos pos : blocks.keySet()) if (level.getBlockEntity(pos) instanceof TileResearchTable tile) tile.clearContent();
            for (var entry : blocks.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue().state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                var be = level.getBlockEntity(entry.getKey());
                if (be != null && entry.getValue().tag != null) be.loadWithComponents(entry.getValue().tag, level.registryAccess());
            }
            discardNewDrops();
            for (PlayerState player : players) player.restore();
        }
    }
    private record SavedBlock(BlockState state, CompoundTag tag) {}
    private static final class PlayerState {
        final ServerPlayer player;
        final List<ItemStack> inventory = new ArrayList<>();
        final thaumcraft.common.lib.research.PlayerKnowledge knowledge;
        final Vec3 position;
        final int selected;
        final GameType gameType;
        final boolean creative, sneaking;
        final float yaw, pitch;
        PlayerState(ServerPlayer player) {
            this.player = player; knowledge = KnowledgeAccess.get(player).copy(); position = player.position();
            selected = player.getInventory().selected; creative = player.getAbilities().instabuild;
            gameType = player.gameMode.getGameModeForPlayer();
            sneaking = player.isShiftKeyDown(); yaw = player.getYRot(); pitch = player.getXRot();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        }
        void restore() {
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = selected; player.setGameMode(gameType); player.getAbilities().instabuild = creative;
            player.setShiftKeyDown(sneaking); player.setYRot(yaw); player.setXRot(pitch); player.setPos(position);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
    }
}

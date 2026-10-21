package thaumcraft.common.testing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.common.blocks.BlockCrystal;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCrystalClusters;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileCrystal;

/** Declared inputs: seven of each primal shard and one balance shard. Every placed cluster is earned through the actual result slot. */
public final class CrystalClustersRuntimeChecks {
    private static final TicketType<BlockPos> TICKET = TicketType.create("thaumcraft-crystal-lifecycle-test", Comparator.comparingLong(BlockPos::asLong));
    private CrystalClustersRuntimeChecks() {}

    public static void sevenRecipesPlaceOnSixFacesAndRecoverTheirSixShards(GameTestHelper test) {
        try (var actor = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 260, 4))) {
            actor.clearLane(); actor.player.getInventory().clearContent();
            for (int metadata = 0; metadata < 6; metadata++) actor.player.getInventory().setItem(9 + metadata, new ItemStack(shard(metadata), 7));
            actor.player.getInventory().setItem(15, new ItemStack(ThaumcraftContent.SHARDS.get("balanced").get()));
            var beforeKnowledge = KnowledgeAccess.get(actor.player).save();
            BlockPos table = actor.root.north(3); actor.put(table, Blocks.CRAFTING_TABLE.defaultBlockState());
            var enchantments = actor.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            var silk = new ItemStack(Items.IRON_PICKAXE); silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            var fortune = new ItemStack(Items.IRON_PICKAXE); fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
            for (int metadata = 0; metadata < 7; metadata++) {
                Block cluster = ThaumcraftCrystalClusters.blocks().get(metadata);
                ItemStack earned = craft(actor, table, metadata, cluster);
                // Another valid support must not permit placing on the clicked fence's unsupported side.
                actor.put(actor.root.below(), Blocks.STONE.defaultBlockState()); actor.put(actor.root.east(), Blocks.OAK_FENCE.defaultBlockState());
                actor.player.setItemInHand(InteractionHand.OFF_HAND, earned);
                actor.require(!actor.player.gameMode.useItemOn(actor.player, actor.level, earned, InteractionHand.OFF_HAND,
                                new BlockHitResult(Vec3.atCenterOf(actor.root.east()).add(-.5, 0, 0), Direction.WEST, actor.root.east(), false)).consumesAction()
                                && earned.getCount() == 1 && actor.level.isEmptyBlock(actor.root),
                        "clicked unsupported fence face refuses the paid cluster even when another adjacent stone could support it");
                actor.put(actor.root.below(), Blocks.AIR.defaultBlockState()); actor.put(actor.root.east(), Blocks.AIR.defaultBlockState());
                int faceIndex = 0;
                for (Direction face : Direction.values()) {
                    if (faceIndex++ > 0) { earned = craft(actor, table, metadata, cluster); actor.player.setItemInHand(InteractionHand.OFF_HAND, earned); }
                    else {
                        var conflictingItemNbt = new CompoundTag(); conflictingItemNbt.putString("id", "thaumcraft:crystal_cluster");
                        conflictingItemNbt.putShort("orientation", (short) 5);
                        earned.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(conflictingItemNbt));
                    }
                    BlockPos support = actor.root.relative(face.getOpposite()); actor.put(support, Blocks.STONE.defaultBlockState());
                    actor.require(actor.player.gameMode.useItemOn(actor.player, actor.level, earned, InteractionHand.OFF_HAND,
                                    new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, support, false)).consumesAction()
                                    && actor.player.getOffhandItem().isEmpty() && actor.level.getBlockState(actor.root).is(cluster),
                            "actual native BlockCrystalItem spends one earned identity on clicked face " + metadata + "/" + face);
                    var state = actor.level.getBlockState(actor.root); var tile = (TileCrystal) actor.level.getBlockEntity(actor.root);
                    actor.require(tile != null && tile.orientation == face.get3DDataValue() && ThaumcraftCrystalClusters.metadata(state) == metadata,
                            "all seven actual native tiles retain clicked orientation even after native item BE-data merge, independently of color");
                    actor.require(state.getShape(actor.level, actor.root).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                                    && state.getCollisionShape(actor.level, actor.root).bounds().equals(new AABB(0, 0, 0, 1, 1, 1))
                                    && state.getBlockSupportShape(actor.level, actor.root).isEmpty() && state.getLightEmission() == 7
                                    && state.getDestroySpeed(actor.level, actor.root) == .7F && cluster.getExplosionResistance() == .6F
                                    && cluster instanceof IInfusionStabiliser stabiliser && stabiliser.canStabaliseInfusion(actor.level, actor.root),
                            "original fullcube outline/collision, non-solid support, light7, hardness.7/effective blast.6 and unconditional stabilisation are real");
                    var saved = tile.saveWithFullMetadata(actor.level.registryAccess());
                    actor.require(saved.contains("orientation", Tag.TAG_SHORT) && saved.getShort("orientation") == face.get3DDataValue(), "orientation is original SHORT NBT rather than byte, blockstate or item component");
                    var clone = BlockEntity.loadStatic(actor.root, state, saved, actor.level.registryAccess());
                    actor.require(clone instanceof TileCrystal crystal && crystal.orientation == face.get3DDataValue(), "native block entity registry reload accepts every original cluster identity and orientation");
                    var empty = new TileCrystal(actor.root, state); actor.require(empty.orientation == 1, "original constructor orientation defaults UP1");
                    empty.loadWithComponents(new CompoundTag(), actor.level.registryAccess()); actor.require(empty.orientation == 0, "original missing saved orientation reads DOWN0");
                    var unusual = new CompoundTag(); unusual.putShort("orientation", Short.MIN_VALUE); empty.loadWithComponents(unusual, actor.level.registryAccess());
                    actor.require(empty.saveWithoutMetadata(actor.level.registryAccess()).getShort("orientation") == Short.MIN_VALUE, "native NBT roundtrip preserves original signed SHORT without clamping");
                    actor.require(cluster.getCloneItemStack(actor.level, actor.root, state).is(cluster.asItem()), "native pick-block gives own cluster item while break loot gives shards");
                    for (ItemStack tool : List.of(ItemStack.EMPTY, silk, fortune)) assertDrops(actor, metadata, Block.getDrops(state, actor.level, actor.root, tile, actor.player, tool));

                    Direction other = face.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
                    BlockPos secondary = actor.root.relative(other); actor.put(secondary, Blocks.STONE.defaultBlockState());
                    actor.level.setBlock(secondary, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    actor.require(actor.level.getBlockState(actor.root).is(cluster) && actor.items().isEmpty(), "losing an unrelated neighbor does not pop a supported crystal");
                    actor.put(secondary, Blocks.STONE.defaultBlockState());
                    actor.level.setBlock(support, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    actor.require(actor.level.isEmptyBlock(actor.root), "losing selected support pops the crystal even while another solid face remains");
                    var drops = actor.items(); assertDrops(actor, metadata, drops.stream().map(ItemEntity::getItem).toList());
                    for (var drop : drops) { drop.setNoPickUpDelay(); drop.playerTouch(actor.player); }
                    actor.require(actor.items().isEmpty(), "actual native pickup recovers the six earned shards (test clears only vanilla pickup delay)");
                    for (int primal = 0; primal < 6; primal++) actor.require(actor.player.getInventory().countItem(shard(primal)) == 7, "craft/place/support-loss/pickup cycle conserves every supplied primal shard");
                    actor.put(secondary, Blocks.AIR.defaultBlockState());
                }
            }
            actor.require(KnowledgeAccess.get(actor.player).save().equals(beforeKnowledge), "ordinary original Clusters recipes require no RP, research purchase, wand or vis");
        }
        test.succeed();
    }

    private static ItemStack craft(FocusUpgradeRuntimeChecks.Actor actor, BlockPos table, int metadata, Block cluster) {
        actor.require(actor.player.gameMode.useItemOn(actor.player, actor.level, actor.player.getMainHandItem(), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(table).add(0, .5, 0), Direction.UP, table, false)).consumesAction()
                        && actor.player.containerMenu instanceof CraftingMenu, "real crafting-table interaction opens the native3x3 menu");
        var menu = (CraftingMenu) actor.player.containerMenu;
        for (int cell = 0; cell < 6; cell++) menu.getSlot(cell + 1).set(take(actor, shard(metadata == 6 ? 5 - cell : metadata)));
        menu.getSlot(9).set(take(actor, ThaumcraftContent.SHARDS.get("balanced").get())); menu.slotsChanged(menu.getSlot(1).container);
        actor.require(menu.getSlot(0).getItem().isEmpty(), "seven inputs including a balanced shard cannot substitute for original six-shard clusters");
        click(actor, 0); actor.require(menu.getCarried().isEmpty() && menu.getSlot(9).getItem().getCount() == 1, "real refused result-slot click preserves the extra input");
        actor.player.getInventory().add(menu.getSlot(9).remove(1)); menu.slotsChanged(menu.getSlot(1).container);
        actor.require(menu.getSlot(0).getItem().is(cluster.asItem()) && menu.getSlot(0).getItem().getCount() == 1,
                "actual registered shapeless recipe selects one own cluster, including all six distinct primals for mixed");
        click(actor, 0); ItemStack earned = menu.getCarried();
        actor.require(earned.is(cluster.asItem()) && earned.getCount() == 1, "real native result-slot extraction produces one paid cluster");
        for (int cell = 1; cell <= 9; cell++) actor.require(menu.getSlot(cell).getItem().isEmpty(), "one consumed from every one of the SIX occupied cells");
        menu.setCarried(ItemStack.EMPTY); actor.player.closeContainer(); return earned;
    }
    private static void click(FocusUpgradeRuntimeChecks.Actor actor, int slot) {
        var menu = actor.player.containerMenu;
        actor.player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slot, 0, ClickType.PICKUP, ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
    }
    private static ItemStack take(FocusUpgradeRuntimeChecks.Actor actor, Item item) {
        for (int slot = 0; slot < 36; slot++) if (actor.player.getInventory().getItem(slot).is(item)) return actor.player.getInventory().removeItem(slot, 1);
        throw new AssertionError("Missing conserved shard input " + item);
    }
    private static Item shard(int metadata) { return ThaumcraftContent.SHARDS.get(ThaumcraftCrystalClusters.TAGS.get(metadata)).get(); }
    private static void assertDrops(FocusUpgradeRuntimeChecks.Actor actor, int metadata, List<ItemStack> drops) {
        Map<Item, Integer> totals = new HashMap<>(); for (var stack : drops) totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
        Map<Item, Integer> expected = new HashMap<>(); for (int index = 0; index < 6; index++) expected.merge(shard(metadata == 6 ? index : metadata), 1, Integer::sum);
        actor.require(drops.size() == 6 && drops.stream().allMatch(stack -> stack.getCount() == 1) && totals.equals(expected),
                "original getDrops returns SIX individual shards: own primal6 or mixed one of EACH primal, ignoring Silk/Fortune/balance shard");
    }

    public static void mixedOrientationActuallyUnloadsAndReloadsFromChunkNbt(GameTestHelper test) {
        var level = test.getLevel(); var origin = test.absolutePos(new BlockPos(4, 10, 4));
        BlockPos position = new BlockPos((origin.getX() & ~15) + 1_048_580, origin.getY() + 100, (origin.getZ() & ~15) + 1_048_580);
        BlockPos support = position.west(); var chunk = new ChunkPos(position); var source = level.getChunkSource();
        source.addRegionTicket(TICKET, chunk, 0, position); var oldChunk = level.getChunkAt(position);
        var oldBlock = level.getBlockState(position); var oldSupport = level.getBlockState(support);
        var oldTile = level.getBlockEntity(position); var oldNbt = oldTile == null ? null : oldTile.saveWithFullMetadata(level.registryAccess());
        boolean[] closed = {false};
        long startedWallNanos = System.nanoTime();
        net.minecraft.world.level.chunk.LevelChunk[] loadedOwner = {null};
        TileCrystal[] loadedTile = {null};
        Runnable cleanup = () -> {
            if (closed[0]) return; closed[0] = true;
            try {
                source.addRegionTicket(TICKET, chunk, 2, position);
                level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2)).forEach(ItemEntity::discard);
                level.removeBlockEntity(position); level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(support, oldSupport, 2); level.setBlock(position, oldBlock, 2);
                if (oldNbt != null && level.getBlockEntity(position) != null) level.getBlockEntity(position).loadWithComponents(oldNbt, level.registryAccess());
            } finally { source.removeRegionTicket(TICKET, chunk, 0, position); source.removeRegionTicket(TICKET, chunk, 2, position); }
        };
        try {
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2)).isEmpty(), "private far-chunk fixture has no pre-existing item to merge or discard");
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(position, ThaumcraftCrystalClusters.MIXED.get().defaultBlockState(), 3);
            var crystal = (TileCrystal) level.getBlockEntity(position); test.assertTrue(crystal != null, "real mixed crystal has its native inert tile"); crystal.setOrientation(Direction.EAST);
            source.removeRegionTicket(TICKET, chunk, 0, position);
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    if (loadedOwner[0] == null) {
                        if (source.getChunkNow(chunk.x, chunk.z) != null || !crystal.isRemoved()) return;
                        source.addRegionTicket(TICKET, chunk, 2, position); var reloadedChunk = level.getChunkAt(position); var reloaded = level.getBlockEntity(position);
                        test.assertTrue(reloadedChunk != oldChunk && reloaded != crystal && reloaded instanceof TileCrystal,
                                "actual ServerLevel unload is followed by a fresh native disk-loaded chunk and tile");
                        var tile = (TileCrystal) reloaded; var state = level.getBlockState(position);
                        test.assertTrue(state.is(ThaumcraftCrystalClusters.MIXED.get()) && tile.orientation == 5
                                        && tile.saveWithoutMetadata(level.registryAccess()).contains("orientation", Tag.TAG_SHORT)
                                        && ThaumcraftCrystalClusters.MIXED.get().getTicker(level, state, ThaumcraftCrystalClusters.CRYSTAL_TILE.get()) == null,
                                "real chunk NBT retains mixed identity/SHORT EAST5 and has no invented growth ticker");
                        loadedOwner[0] = reloadedChunk; loadedTile[0] = tile;
                        return;
                    }
                    // FULL block access precedes native entity-section visibility after reload. Wait for the actual lifecycle,
                    // rather than breaking in the reload call and querying an entity section that is still HIDDEN.
                    if (!level.areEntitiesLoaded(chunk.toLong()) || !level.isPositionEntityTicking(position)) return;
                    test.assertTrue(source.getChunkNow(chunk.x, chunk.z) == loadedOwner[0] && level.getBlockEntity(position) == loadedTile[0],
                            "the same disk-loaded crystal remains current when its native entity section starts ticking");
                    level.setBlock(support, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(2));
                    test.assertTrue(level.isEmptyBlock(position) && drops.size() == 6 && drops.stream().allMatch(drop -> drop.getItem().getCount() == 1),
                            "disk-loaded selected support loss still creates six individual drops once; state=" + level.getBlockState(position)
                                    + ", drops=" + drops.stream().map(ItemEntity::getItem).toList());
                    for (int index = 0; index < 6; index++) {
                        Item expected = shard(index); test.assertTrue(drops.stream().filter(drop -> drop.getItem().is(expected)).count() == 1, "disk-loaded mixed drop retains each distinct primal exactly once");
                    }
                    cleanup.run(); test.succeed();
                } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
            });
            test.runAfterDelay(1100, () -> { if (!closed[0]) {
                // Snapshot before cleanup, which deliberately re-adds a ticket and loads/restores the fixture.
                var currentChunk = source.getChunkNow(chunk.x, chunk.z);
                String phase = loadedOwner[0] == null ? "await-original-unload" : "await-reloaded-entity-section";
                String diagnostic = "isolated crystal chunk did not complete actual unload/reload/entity ticking within1100 world ticks"
                        + "; phase=" + phase + ", position=" + position + ", currentChunk=" + currentChunk
                        + ", currentIsOld=" + (currentChunk == oldChunk) + ", currentIsReloaded=" + (currentChunk == loadedOwner[0])
                        + ", oldBEremoved=" + crystal.isRemoved() + ", reloadedBEremoved=" + (loadedTile[0] == null ? "absent" : loadedTile[0].isRemoved())
                        + ", entitiesLoaded=" + level.areEntitiesLoaded(chunk.toLong()) + ", entityTicking=" + level.isPositionEntityTicking(position)
                        + ", ticketDebug=" + source.getChunkDebugData(chunk) + ", elapsedWallMs=" + (System.nanoTime() - startedWallNanos) / 1_000_000;
                cleanup.run(); test.fail(diagnostic);
            } });
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }
}

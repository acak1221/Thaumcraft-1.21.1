package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** One native content check, bound/run by the central loader test integration. No test recipes are injected. */
public final class ArcaneWorkbenchRuntimeChecks {
    private ArcaneWorkbenchRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        var oldState = level.getBlockState(pos);
        var oldBlockEntity = level.getBlockEntity(pos);
        var oldTag = oldBlockEntity == null ? null : oldBlockEntity.saveWithFullMetadata(level.registryAccess());
        var knowledge = KnowledgeAccess.get(player).copy();
        var inventory = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventory.add(player.getInventory().getItem(slot).copy());
        var oldPosition = player.position();
        var oldGameType = player.gameMode.getGameModeForPlayer();
        int oldSelected = player.getInventory().selected;
        boolean oldSneaking = player.isShiftKeyDown();
        AABB dropsBox = new AABB(pos).inflate(3);
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, dropsBox).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            player.closeContainer();
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            level.setBlock(pos, ThaumcraftContent.TABLE.get().state(1), Block.UPDATE_ALL);
            ItemStack charged = wand(110);
            charged.set(DataComponents.CUSTOM_NAME, Component.literal("arcane-transfer-components"));
            player.setItemInHand(InteractionHand.MAIN_HAND, charged.copy());
            test.assertTrue(use(player, pos).consumesAction() && level.getBlockEntity(pos) instanceof TileArcaneWorkbench,
                    "native wand use converts original base variant1 into the registered workbench");
            var tile = (TileArcaneWorkbench) level.getBlockEntity(pos);
            test.assertTrue(player.getMainHandItem().isEmpty() && ItemStack.matches(charged, tile.getItem(10)),
                    "conversion transfers the entire named/charged wand without vis cost");
            test.assertTrue(use(player, pos).consumesAction() && player.containerMenu instanceof ContainerArcaneWorkbench,
                    "native block interaction opens the real extended workbench menu");
            var menu = (ContainerArcaneWorkbench) player.containerMenu;
            test.assertTrue(menu.slots.size() == 47 && !menu.getSlot(0).mayPlace(charged)
                    && menu.getSlot(1).mayPlace(charged) && !menu.getSlot(1).mayPlace(new ItemStack(Items.STICK)), "original 47 slots and wand/result filters");

            fabric(tile, 2);
            KnowledgeAccess.get(player).removeResearch("ENCHFABRIC");
            menu.broadcastChanges();
            test.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.quickMoveStack(player, 0).isEmpty()
                    && ItemStack.matches(tile.getItem(10), charged), "missing original research refuses without consuming ingredients/vis");
            KnowledgeAccess.get(player).addResearch("ENCHFABRIC");
            menu.broadcastChanges();
            Item fabric = ThaumcraftContent.MATERIALS.get("enchanted_fabric").get();
            test.assertTrue(menu.getSlot(0).getItem().is(fabric) && menu.craftable(), "actual registered Enchanted Fabric JSON matches colored wool and string");
            for (String tag : WandData.PRIMALS) test.assertTrue(menu.requiredVis(tag) == 110, "one vis times original iron1.1 modifier: " + tag);
            menu.clicked(0, 1, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().is(fabric) && menu.getCarried().getCount() == 1 && tile.getItem(4).getCount() == 1,
                    "right-click takes exactly one full real output and one item from every ingredient");
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), tag) == 0, "all six centivis debits applied atomically: " + tag);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(menu.getCarried().getCount() == 1 && tile.getItem(4).getCount() == 1, "replayed result click with exhausted wand grants nothing");

            ItemStack shortWand = wand(110);
            shortWand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortWand).withAmount("perditio", 109));
            tile.setItem(10, shortWand);
            menu.broadcastChanges();
            var beforeRefusal = tile.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(tile.saveWithoutMetadata(level.registryAccess()).equals(beforeRefusal) && !menu.craftable(),
                    "one insufficient primal refuses the whole transaction, not a partial debit");
            tile.setItem(10, wand(220));
            menu.broadcastChanges();
            KnowledgeAccess.get(player).removeResearch("ENCHFABRIC");
            test.assertTrue(menu.quickMoveStack(player, 0).isEmpty() && ItemWandCasting.getVis(tile.getItem(10), "aer") == 220,
                    "research is checked again at take, not trusted from a stale preview");
            KnowledgeAccess.get(player).addResearch("ENCHFABRIC");
            player.getInventory().add(menu.getCarried());
            menu.setCarried(ItemStack.EMPTY);
            int fabricBefore = player.getInventory().countItem(fabric);
            fabric(tile, 2);
            menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            test.assertTrue(player.getInventory().countItem(fabric) == fabricBefore + 2 && tile.getItem(4).isEmpty(),
                    "native shift-click repeats only the two paid recipes that actually fit the matrix/vis");
            for (String tag : WandData.PRIMALS) test.assertTrue(ItemWandCasting.getVis(tile.getItem(10), tag) == 0, "shift crafting debits each recipe once: " + tag);

            tile.clearContent();
            Item[] cake = {Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG,
                    Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT};
            for (int slot = 0; slot < 9; slot++) tile.setItem(slot, new ItemStack(cake[slot]));
            menu.broadcastChanges();
            int bucketsBefore = player.getInventory().countItem(Items.BUCKET);
            test.assertTrue(menu.getSlot(0).getItem().is(Items.CAKE) && menu.craftable() && menu.requiredVis("aer") == 0,
                    "real native vanilla 3x3 recipe works without any wand/research/vis");
            test.assertTrue(menu.quickMoveStack(player, 0).is(Items.CAKE) && player.getInventory().countItem(Items.BUCKET) == bucketsBefore + 3,
                    "native cake remainders return all three buckets exactly once");
            for (int slot = 0; slot < 9; slot++) test.assertTrue(tile.getItem(slot).isEmpty(), "cake matrix consumed with inventory-first TC4 remainders");

            fabric(tile, 3);
            tile.getItem(1).set(DataComponents.CUSTOM_NAME, Component.literal("persisted-string"));
            tile.setItem(10, charged.copy());
            menu.broadcastChanges();
            var expected = new ArrayList<ItemStack>();
            for (int slot = 0; slot < 11; slot++) expected.add(tile.getItem(slot).copy());
            var saved = tile.saveWithFullMetadata(level.registryAccess());
            level.removeBlockEntity(pos);
            var loaded = new TileArcaneWorkbench(pos, level.getBlockState(pos));
            loaded.loadWithComponents(saved, level.registryAccess());
            level.setBlockEntity(loaded);
            for (int slot = 0; slot < 11; slot++) test.assertTrue(ItemStack.matches(expected.get(slot), loaded.getItem(slot)), "native BE round trip preserves physical slot/components " + slot);
            test.assertTrue(loaded.getItem(9).isEmpty() && !menu.stillValid(player) && menu.quickMoveStack(player, 0).isEmpty(),
                    "computed output never persists; old menu cannot take after BE replacement");
            var beforeStale = loaded.saveWithoutMetadata(level.registryAccess());
            menu.clicked(0, 0, ClickType.PICKUP, player);
            test.assertTrue(beforeStale.equals(loaded.saveWithoutMetadata(level.registryAccess())), "stale session leaves the replacement inventory untouched");
            player.closeContainer();
            test.assertTrue(beforeStale.equals(loaded.saveWithoutMetadata(level.registryAccess())), "closing retains every input and the inserted wand");
            var rawDrops = Block.getDrops(level.getBlockState(pos), level, pos, loaded);
            test.assertTrue(rawDrops.size() == 1 && rawDrops.getFirst().is(ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get()), "source metadata15 drops one workbench, not a base table/preview");
            long dropsBefore = level.getEntitiesOfClass(ItemEntity.class, dropsBox).size();
            loaded.evacuate();
            loaded.evacuate();
            // We are outside a block mutation here, so the existing level-post
            // delivery adapter can safely exercise its native entity delivery.
            DeferredItemDrops.tick(level);
            test.assertTrue(loaded.isEmpty() && level.getEntitiesOfClass(ItemEntity.class, dropsBox).size() == dropsBefore + 6,
                    "five matrix stacks plus wand evacuate once; no phantom crafted output is dropped");
        } finally {
            player.closeContainer();
            if (level.getBlockEntity(pos) instanceof TileArcaneWorkbench table) table.clearContent();
            level.setBlock(pos, oldState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (oldTag != null && level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(oldTag, level.registryAccess());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, dropsBox)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
            player.getInventory().clearContent();
            for (int slot = 0; slot < inventory.size(); slot++) player.getInventory().setItem(slot, inventory.get(slot));
            player.getInventory().selected = oldSelected;
            player.setGameMode(oldGameType);
            player.setShiftKeyDown(oldSneaking);
            player.setPos(oldPosition);
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static ItemStack wand(int centivis) {
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        WandData data = WandData.EMPTY;
        for (String tag : WandData.PRIMALS) data = data.withAmount(tag, centivis);
        wand.set(ThaumcraftContent.WAND_DATA.get(), data);
        return wand;
    }

    private static void fabric(TileArcaneWorkbench tile, int count) {
        for (int slot = 0; slot < 9; slot++) tile.setItem(slot, ItemStack.EMPTY);
        for (int slot : new int[] {1, 3, 5, 7}) tile.setItem(slot, new ItemStack(Items.STRING, count));
        tile.setItem(4, new ItemStack(Items.RED_WOOL, count));
    }

    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(new Vec3(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5), Direction.UP, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }
}

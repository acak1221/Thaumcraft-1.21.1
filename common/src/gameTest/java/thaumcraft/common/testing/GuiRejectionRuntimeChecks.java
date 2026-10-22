package thaumcraft.common.testing;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.container.ContainerMagicBox;
import thaumcraft.common.tiles.TileMagicBox;

/** Real menu/connected actor; malformed live reload is an explicit fixture, not a player exploit. */
public final class GuiRejectionRuntimeChecks {
    private GuiRejectionRuntimeChecks() {}

    public static void magicBox(GameTestHelper test) {
        try (var actor = new FocusUpgradeRuntimeChecks.Actor(test)) {
            actor.put(actor.root, ThaumcraftEldritchBlocks.MAGIC_BOX.get().defaultBlockState());
            var box = (TileMagicBox) actor.level.getBlockEntity(actor.root);
            var hit = new BlockHitResult(Vec3.atCenterOf(actor.root), Direction.UP, actor.root, false);
            test.assertTrue(actor.player.gameMode.useItemOn(actor.player, actor.level, ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, hit).consumesAction()
                    && actor.player.containerMenu instanceof ContainerMagicBox, "ordinary block use opens real magic-box menu");
            var menu = (ContainerMagicBox) actor.player.containerMenu;
            test.assertTrue(menu.slots.size() == 63 && menu.getSlot(0).container == box, "27 real box slots plus 36 player slots");
            actor.player.getInventory().setItem(9, new ItemStack(Items.DIRT, 7));
            menu.clicked(27, 0, ClickType.QUICK_MOVE, actor.player);
            test.assertTrue(box.getItem(0).getCount() == 7 && actor.player.getInventory().getItem(9).isEmpty(), "healthy quick move inserts actual seven dirt");
            menu.clicked(0, 0, ClickType.QUICK_MOVE, actor.player);
            test.assertTrue(box.getItem(0).isEmpty() && actor.player.getInventory().getItem(8).getCount() == 7, "reverse extraction preserves seven dirt");
            actor.player.getInventory().setItem(8, ItemStack.EMPTY);
            actor.player.getInventory().setItem(9, new ItemStack(Items.DIRT, 7));

            // Simulate a live persisted-data rejection after admission, retaining the same BE identity.
            CompoundTag malformed = new CompoundTag();
            malformed.putString("Items", "not a list");
            box.loadWithComponents(malformed, actor.level.registryAccess());
            test.assertTrue(box.isDataRejected() && !box.canPlaceItem(0, new ItemStack(Items.DIRT))
                    && !box.stillValid(actor.player), "backend explicitly rejects input and viewer after malformed reload fixture");
            menu.clicked(27, 0, ClickType.QUICK_MOVE, actor.player);
            int total = actor.player.getInventory().items.stream().filter(stack -> stack.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum()
                    + box.getContents().stream().filter(stack -> stack.is(Items.DIRT)).mapToInt(ItemStack::getCount).sum()
                    + actor.items().stream().filter(entity -> entity.getItem().is(Items.DIRT)).mapToInt(entity -> entity.getItem().getCount()).sum()
                    + (menu.getCarried().is(Items.DIRT) ? menu.getCarried().getCount() : 0);
            test.assertTrue(total == 7, "rejected shift-click must preserve all seven dirt across player, backend, cursor and drops; actual=" + total);
            test.assertTrue(!menu.stillValid(actor.player) && !menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT))
                    && !menu.getSlot(0).mayPickup(actor.player), "menu agrees with backend rejection in both slot directions");
            actor.player.closeContainer();
        }
        test.succeed();
    }
}

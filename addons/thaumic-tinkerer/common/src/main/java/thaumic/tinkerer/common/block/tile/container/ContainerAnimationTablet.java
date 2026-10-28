package thaumic.tinkerer.common.block.tile.container;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;
import thaumic.tinkerer.common.content.TileBlocks;

/** The menu of the Animation Tablet: one slot for the item and the player's inventory. */
public class ContainerAnimationTablet extends ContainerPlayerInv {

    public final TileAnimationTablet tablet;

    public ContainerAnimationTablet(int containerId, Inventory playerInv, TileAnimationTablet tablet) {
        super(TileBlocks.animationTabletMenu(), containerId, playerInv);

        this.tablet = tablet;

        addSlot(new Slot(tablet, 0, 80, 15));

        initPlayerInv();
    }

    /** The client side: the block position comes from the packet, the block entity from the client's world. */
    public static ContainerAnimationTablet fromNetwork(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity entity = playerInv.player.level().getBlockEntity(pos);
        if (entity instanceof TileAnimationTablet tablet) return new ContainerAnimationTablet(containerId, playerInv, tablet);
        // The block entity has not reached this client (or is gone): a detached tablet keeps the menu consistent.
        TileAnimationTablet detached = new TileAnimationTablet(pos, TileBlocks.animationTabletBlock().defaultBlockState());
        detached.setLevel(playerInv.player.level());
        return new ContainerAnimationTablet(containerId, playerInv, detached);
    }

    @Override
    public boolean stillValid(Player player) {
        return tablet.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index < 1) {
                if (!moveItemStackTo(stack, 1, 37, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return original;
    }
}

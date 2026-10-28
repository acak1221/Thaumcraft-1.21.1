package thaumic.tinkerer.common.block.tile.container;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumic.tinkerer.common.block.tile.TileRPlacer;
import thaumic.tinkerer.common.content.TileBlocks;

/** The menu of the Remote Placer: one slot for the block and the player's inventory. */
public class ContainerRemotePlacer extends ContainerPlayerInv {

    public final TileRPlacer placer;

    public ContainerRemotePlacer(int containerId, Inventory playerInv, TileRPlacer placer) {
        super(TileBlocks.remotePlacerMenu(), containerId, playerInv);

        this.placer = placer;

        addSlot(new Slot(placer, 0, 20, 30));

        initPlayerInv();
    }

    /** The client side: the block position comes from the packet, the block entity from the client's world. */
    public static ContainerRemotePlacer fromNetwork(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity entity = playerInv.player.level().getBlockEntity(pos);
        if (entity instanceof TileRPlacer placer) return new ContainerRemotePlacer(containerId, playerInv, placer);
        TileRPlacer detached = new TileRPlacer(pos, TileBlocks.remotePlacerBlock().defaultBlockState());
        detached.setLevel(playerInv.player.level());
        return new ContainerRemotePlacer(containerId, playerInv, detached);
    }

    @Override
    public boolean stillValid(Player player) {
        return placer.stillValid(player);
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

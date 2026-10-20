package thaumcraft.common.container;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileMagicBox;

/**
 * Original ContainerMagicBox: the shared inventory of a chain of magic boxes.
 * Every box of the chain shows the same content, the owner box stores it.
 */
public final class ContainerMagicBox extends AbstractContainerMenu {
    private final TileMagicBox box;

    public ContainerMagicBox(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public ContainerMagicBox(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, resolve(inventory, pos));
    }

    public ContainerMagicBox(int id, Inventory inventory, TileMagicBox box) {
        super(ThaumcraftEldritchBlocks.MAGIC_BOX_MENU.get(), id);
        this.box = box;
        int size = box == null ? 27 : box.getContainerSize();
        int rows = (size + 8) / 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            addSlot(new Slot(box, slot, 8 + column * 18, 18 + row * 18));
        }
        int playerRows = 3;
        int startY = 18 + rows * 18 + 14;
        for (int row = 0; row < playerRows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, startY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, startY + playerRows * 18 + 4));
        }
    }

    /** The magic box of the opened position. */
    private static TileMagicBox resolve(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof TileMagicBox box ? box : null;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < boxSlots()) {
            if (!moveItemStackTo(stack, boxSlots(), slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, boxSlots(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** The number of slots that belong to the box. */
    private int boxSlots() {
        return box == null ? 0 : box.getContainerSize();
    }

    @Override public boolean stillValid(Player player) {
        return box != null && box.stillValid(player);
    }
}

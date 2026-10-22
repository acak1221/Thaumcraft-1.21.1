package thaumcraft.common.container;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.common.config.ThaumcraftDeviceMenus;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * Original ContainerMagicBox: three rows of box slots above the player
 * inventory, whatever the real box size is.
 *
 * <p>The original quirk is preserved: a linked box owns twenty-seven slots per
 * box, but the container binds only three rows and the quick move boundary
 * uses the WHOLE linked size. A linked box therefore shifts a box item out of
 * reach exactly as it did in 1.7.10.</p>
 */
public final class ContainerMagicBox extends AbstractContainerMenu {
    /** Original ContainerMagicBox:34 - the creative sort helper of the original. */
    public static ContainerMagicBox tc;

    private final Container box;
    private final BlockPos pos;
    private final Player viewer;
    private final int size;
    private final int numRows;

    public ContainerMagicBox(int id, Inventory inventory, Container box, BlockPos pos, int size) {
        super(ThaumcraftDeviceMenus.MAGIC_BOX.get(), id);
        this.box = box;
        this.pos = pos.immutable();
        this.viewer = inventory.player;
        this.size = Math.max(0, size);
        this.numRows = DeviceContainerRules.magicBoxRows(this.size);
        box.startOpen(inventory.player);
        bindBoxInventory(0);
        bindPlayerInventory(inventory);
        // Original ContainerMagicBox:33-35 - the client keeps the open box in
        // a static field so the creative sort key can reach it.
        if (viewer.level().isClientSide) tc = this;
    }

    public ContainerMagicBox(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, new SimpleContainer(DeviceContainerRules.MAGIC_BOX_SLOTS), buffer.readBlockPos(), buffer.readVarInt());
    }

    /** Original ContainerMagicBox:38-46 - only three rows are ever bound. */
    void bindBoxInventory(int row) {
        for (int line = 0; line < DeviceContainerRules.MAGIC_BOX_ROWS; line++) {
            for (int column = 0; column < DeviceContainerRules.MAGIC_BOX_COLUMNS; column++) {
                addSlot(new Slot(box, column + (line + row) * DeviceContainerRules.MAGIC_BOX_COLUMNS,
                        DeviceContainerRules.magicBoxSlotX(column), DeviceContainerRules.magicBoxSlotY(line)) {
                    @Override public boolean mayPlace(ItemStack stack) { return box.canPlaceItem(getContainerSlot(), stack); }
                    @Override public boolean mayPickup(Player player) { return ContainerMagicBox.this.stillValid(player); }
                });
            }
        }
    }

    /** Original ContainerMagicBox:48-56. */
    void bindPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, DeviceContainerRules.magicBoxPlayerX(column), DeviceContainerRules.magicBoxPlayerY(row)));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, DeviceContainerRules.magicBoxPlayerX(column), DeviceContainerRules.magicBoxHotbarY()));
        }
    }

    /**
     * Original ContainerMagicBox:58-63. The 1.21.1 slot list is fixed when the
     * menu is built, so the original rebind is replaced by a content refresh.
     */
    public void refreshInventory() { broadcastChanges(); }

    public BlockPos boxPos() { return pos; }
    public int numRows() { return numRows; }
    public int boxSize() { return size; }
    public Container boxContents() { return box; }

    @Override public boolean stillValid(Player player) {
        if (player != viewer || !player.isAlive() || player.isSpectator()) return false;
        // The BE helper only checks identity/proximity, not the backend's
        // rejected-data gate. A refused setter cannot safely accept a transfer.
        if (!box.stillValid(player)) return false;
        if (box instanceof BlockEntity blockEntity) return Container.stillValidBlockEntity(blockEntity, player);
        return true;
    }

    private boolean active(Player player) {
        return player == viewer && player instanceof ServerPlayer server && server.server.isSameThread() && viewer.containerMenu == this && stillValid(player);
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (viewer.level().isClientSide ? stillValid(player) : active(player)) super.clicked(index, button, type, player);
    }

    /** Original ContainerMagicBox:44-56 - the linked size decides the boundary. */
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int boundary = DeviceContainerRules.magicBoxQuickMoveBoundary(size);
        boolean moved = index < boundary
                ? moveItemStackTo(stack, boundary, slots.size(), true)
                : moveItemStackTo(stack, 0, boundary, false);
        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    /** Original ContainerMagicBox:69-72 - the box inventory is closed again. */
    @Override public void removed(Player player) {
        super.removed(player);
        if (player == viewer) box.stopOpen(player);
    }
}

package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityTravelingTrunk;

/**
 * Original ContainerTravelingTrunk: the trunk's {@code rows * 9} slots at {@code (8 + 18 i, 15 + 23 j)}, then the player's inventory.
 * Button 1 toggles "stay". The server menu sets the trunk open and plays the lid sounds; the client copy only mirrors the layout.
 */
public final class ContainerTravelingTrunk extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 200, STAY_BUTTON = 1;
    private final EntityTravelingTrunk trunk; // null on the client
    private final int entityId, rows;
    private final Player viewer;

    public ContainerTravelingTrunk(int id, Inventory inventory, EntityTravelingTrunk trunk) {
        this(id, inventory, trunk, trunk.inventory, trunk.getId(), trunk.getRows());
        trunk.setOpen(true);
        trunk.level().playSound(null, trunk.getX(), trunk.getY(), trunk.getZ(), SoundEvents.CHEST_OPEN, SoundSource.NEUTRAL, 0.5F, trunk.level().random.nextFloat() * 0.1F + 0.9F);
    }

    public ContainerTravelingTrunk(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt(), buffer.readVarInt());
    }

    private ContainerTravelingTrunk(int id, Inventory inventory, int entityId, int rows) {
        this(id, inventory, null, new SimpleContainer(rows * 9), entityId, rows);
    }

    private ContainerTravelingTrunk(int id, Inventory inventory, EntityTravelingTrunk trunk, Container container, int entityId, int rows) {
        super(ThaumcraftGolems.TRUNK_MENU.get(), id);
        this.trunk = trunk; this.entityId = entityId; this.rows = rows; viewer = inventory.player;
        for (int row = 0; row < rows; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(container, column + row * 9, 8 + column * 18, 15 + row * 23));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 118 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 176));
    }

    public int rows() { return rows; }

    /** The trunk as the viewer's own level knows it (the client reads its tracked copy for the health bar and the "stay" mark). */
    public EntityTravelingTrunk trunk() {
        if (trunk != null) return trunk;
        return viewer.level().getEntity(entityId) instanceof EntityTravelingTrunk found ? found : null;
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != STAY_BUTTON || trunk == null || !stillValid(player)) return false;
        trunk.setStay(!trunk.getStay());
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            int own = rows * 9;
            if (index < own ? !moveItemStackTo(stack, own, slots.size(), true) : !moveItemStackTo(stack, 0, own, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return moved;
    }

    @Override public boolean stillValid(Player player) {
        return trunk == null || trunk.isAlive() && player.level() == trunk.level() && player.distanceToSqr(trunk) <= 100;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (trunk != null) {
            trunk.setOpen(false);
            trunk.level().playSound(null, trunk.getX(), trunk.getY(), trunk.getZ(), SoundEvents.CHEST_CLOSE, SoundSource.NEUTRAL, 0.5F, trunk.level().random.nextFloat() * 0.1F + 0.9F);
        }
    }
}

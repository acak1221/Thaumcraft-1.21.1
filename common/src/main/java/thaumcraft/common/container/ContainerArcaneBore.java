package thaumcraft.common.container;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.items.wands.foci.ItemFocusExcavation;
import thaumcraft.common.lib.bore.ArcaneBoreMath;
import thaumcraft.common.tiles.TileArcaneBore;

/** Original two device slots +36 player slots; GUI176x141. No research, XP,
 * operator proximity-to-target or paid-start button is invented for the bore. */
public final class ContainerArcaneBore extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176, IMAGE_HEIGHT = 141;
    private final TileArcaneBore serverTile;
    private final BlockPos pos;
    private final Player viewer;
    private final UUID lifetime;
    private final ContainerData stats;
    private boolean clicking, quickMoving;

    public ContainerArcaneBore(int id, Inventory inventory, TileArcaneBore tile) {
        this(id, inventory, tile, tile.getBlockPos(), tile, tile.lifetimeId(), serverData(tile));
    }
    public ContainerArcaneBore(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(2), data.readBlockPos(), null, data.readUUID(), new SimpleContainerData(3));
    }
    private ContainerArcaneBore(int id, Inventory inventory, Container contents, BlockPos pos,
                               TileArcaneBore tile, UUID lifetime, ContainerData stats) {
        super(ThaumcraftArcaneBore.ARCANE_BORE_MENU.get(), id);
        this.pos = pos.immutable(); serverTile = tile; viewer = inventory.player; this.lifetime = lifetime; this.stats = stats;
        checkContainerSize(contents, 2); checkContainerDataCount(stats, 3);
        for (int index = 0; index < 2; index++) {
            final int deviceSlot = index;
            addSlot(new Slot(contents, index, index == 0 ? 26 : 74, 18) {
                @Override public boolean mayPlace(ItemStack stack) { return TileArcaneBore.validSlot(deviceSlot, stack); }
                @Override public boolean mayPickup(Player player) { return serverTile == null || serverTile.inventoryAvailable(); }
                @Override public int getMaxStackSize() { return 1; }
                @Override public int getMaxStackSize(ItemStack stack) { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 59 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 117));
        addDataSlots(stats);
    }
    private static ContainerData serverData(TileArcaneBore tile) {
        return new ContainerData() {
            @Override public int get(int index) {
                var values = tile.stats();
                return switch (index) { case 0 -> values.fortune(); case 1 -> values.speed(); case 2 -> values.area(); default -> 0; };
            }
            @Override public void set(int index, int value) {} // Native S2C display only, never a server stat setter.
            @Override public int getCount() { return 3; }
        };
    }
    public BlockPos borePos() { return pos; }
    public ArcaneBoreMath.Stats stats() { return new ArcaneBoreMath.Stats(stats.get(0), stats.get(1), stats.get(2)); }
    public TileArcaneBore tile() {
        if (serverTile != null) return serverTile;
        return viewer.level().hasChunkAt(pos) && viewer.level().getBlockEntity(pos) instanceof TileArcaneBore top ? top : null;
    }
    @Override public boolean stillValid(Player player) {
        if (player != viewer || !player.isAlive() || player.isSpectator()) return false;
        if (serverTile != null) return lifetime.equals(serverTile.lifetimeId()) && serverTile.stillValid(player);
        return viewer.level().hasChunkAt(pos) && viewer.level().getBlockState(pos).is(ThaumcraftArcaneBore.ARCANE_BORE.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
    private boolean active(Player player) {
        return serverTile != null && player instanceof ServerPlayer sender && player == viewer
                && sender.server.isSameThread() && sender.server.getPlayerList().getPlayer(sender.getUUID()) == sender
                && sender.containerMenu == this && stillValid(player) && serverTile.inventoryAvailable();
    }
    @Override public boolean clickMenuButton(Player player, int button) { return false; }
    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (clicking || !active(player)) return;
        clicking = true;
        try { super.clicked(index, button, type, player); }
        finally { clicking = false; }
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (quickMoving || !active(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        quickMoving = true;
        try {
            Slot slot = getSlot(index);
            if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
            ItemStack current = slot.getItem(), original = current.copy();
            boolean moved;
            if (index < 2) moved = moveItemStackTo(current, 2, 38, true);
            else if (current.getItem() instanceof ItemFocusExcavation && TileArcaneBore.validSlot(0, current))
                moved = moveItemStackTo(current, 0, 1, false);
            else if (current.getItem() instanceof PickaxeItem && TileArcaneBore.validSlot(1, current))
                moved = moveItemStackTo(current, 1, 2, false);
            else return ItemStack.EMPTY;
            if (!moved || current.getCount() == original.getCount()) return ItemStack.EMPTY;
            // Device getters deliberately return copies: commit the resulting
            // slot through its guarded setter, not only setChanged on a copy.
            slot.set(current.isEmpty() ? ItemStack.EMPTY : current);
            slot.onTake(player, current);
            return original;
        } finally { quickMoving = false; }
    }
}

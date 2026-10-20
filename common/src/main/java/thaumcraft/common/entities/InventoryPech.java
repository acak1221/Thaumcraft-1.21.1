package thaumcraft.common.entities;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * TC4 InventoryPech, the merchant inventory of the Pech trade.
 * <p>
 * The original offers five slots: the first one receives the item the player
 * offers, the other four show what the Pech returns. Only a tamed Pech may be
 * traded with, the stack limit is 64 and every change is reported to the
 * open container so the menu refreshes.
 * <p>
 * Declared native adaptation: the original notified its container through the
 * 1.7 inventory change callback, which has no native counterpart, so the
 * change notification is a listener that the owning menu installs.
 */
public class InventoryPech implements Container {
    private final EntityPech theMerchant;
    private final ItemStack[] theInventory = new ItemStack[ProjectileRules.INVENTORY_PECH_SLOTS];
    private final Player thePlayer;
    private Runnable changeListener = () -> {};

    public InventoryPech(Player player, EntityPech merchant) {
        this.thePlayer = player;
        this.theMerchant = merchant;
    }

    public EntityPech getMerchant() { return theMerchant; }
    public Player getPlayer() { return thePlayer; }
    public void setChangeListener(Runnable listener) { changeListener = listener == null ? () -> {} : listener; }

    @Override public int getContainerSize() { return theInventory.length; }

    @Override public boolean isEmpty() {
        for (ItemStack stack : theInventory) if (stack != null && !stack.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getItem(int slot) {
        return slot >= 0 && slot < theInventory.length && theInventory[slot] != null ? theInventory[slot] : ItemStack.EMPTY;
    }

    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= theInventory.length || theInventory[slot] == null) return ItemStack.EMPTY;
        ItemStack stored = theInventory[slot];
        ItemStack taken = stored.split(amount);
        if (stored.isEmpty()) theInventory[slot] = null;
        setChanged();
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= theInventory.length || theInventory[slot] == null) return ItemStack.EMPTY;
        ItemStack stored = theInventory[slot];
        theInventory[slot] = null;
        return stored;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= theInventory.length) return;
        if (stack != null && stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        theInventory[slot] = stack;
        setChanged();
    }

    @Override public int getMaxStackSize() { return ProjectileRules.inventoryPechStackLimit(); }

    @Override public void setChanged() { changeListener.run(); }

    @Override public boolean stillValid(Player player) {
        // The original only lets a player trade with a tamed Pech.
        return theMerchant != null && theMerchant.isTamed();
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return ProjectileRules.inventoryPechAccepts(slot);
    }

    @Override public void clearContent() {
        for (int slot = 0; slot < theInventory.length; slot++) theInventory[slot] = null;
        setChanged();
    }
}

package thaumcraft.common.container;

import java.util.function.BooleanSupplier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.ItemFocusPouch;

/** Private menu mirror, persisted to its exact physical pouch after each native
 * mutation. No detached-on-close snapshot is allowed to overwrite a new owner. */
public final class InventoryFocusPouch extends SimpleContainer {
    private final BooleanSupplier writable;
    private final Runnable persist;
    public InventoryFocusPouch(ItemStack[] initial,BooleanSupplier writable,Runnable persist) {
        super(ItemFocusPouch.SIZE);this.writable=writable;this.persist=persist;
        if(initial.length!=ItemFocusPouch.SIZE)throw new IllegalArgumentException("Expected eighteen pouch slots");
        for(int slot=0;slot<initial.length;slot++)getItems().set(slot,initial[slot].copy());
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return stack.getItem() instanceof ItemFocusBasic; }
    @Override public void setItem(int slot,ItemStack stack) {
        if(!writable.getAsBoolean())return;
        // Native Slot/Container insertion limits are one. Initial client slot
        // synchronization must retain a complete foreign saved entry instead
        // of throwing or SimpleContainer silently clamping it to one.
        getItems().set(slot,stack);setChanged();
    }
    @Override public ItemStack removeItem(int slot,int amount) { return writable.getAsBoolean()?super.removeItem(slot,amount):ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if(!writable.getAsBoolean())return ItemStack.EMPTY;ItemStack out=super.removeItemNoUpdate(slot);if(!out.isEmpty())setChanged();return out;
    }
    @Override public void clearContent() { if(writable.getAsBoolean())super.clearContent(); }
    @Override public void setChanged() { if(writable.getAsBoolean()){persist.run();super.setChanged();} }
    public ItemStack[] snapshot() { ItemStack[] out=new ItemStack[ItemFocusPouch.SIZE];for(int i=0;i<out.length;i++)out[i]=getItem(i).copy();return out; }
}

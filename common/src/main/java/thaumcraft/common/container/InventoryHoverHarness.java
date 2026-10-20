package thaumcraft.common.container;

import java.util.function.BooleanSupplier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.armor.HarnessData;

/** Native one-slot mirror. Insertions are limited by Slot/Container; native
 * synchronization and recovered foreign saved entries must never be clamped. */
public final class InventoryHoverHarness extends SimpleContainer {
    private final BooleanSupplier writable;
    private final Runnable persist;
    public InventoryHoverHarness(ItemStack initial,BooleanSupplier writable,Runnable persist) {super(1);this.writable=writable;this.persist=persist;getItems().set(0,initial.copy());}
    @Override public int getMaxStackSize() {return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return HarnessData.validFuelJar(stack);}
    @Override public void setItem(int slot,ItemStack stack) {if(writable.getAsBoolean()){getItems().set(slot,stack);setChanged();}}
    @Override public ItemStack removeItem(int slot,int amount) {return writable.getAsBoolean()?super.removeItem(slot,amount):ItemStack.EMPTY;}
    @Override public ItemStack removeItemNoUpdate(int slot) {if(!writable.getAsBoolean())return ItemStack.EMPTY;var removed=super.removeItemNoUpdate(slot);if(!removed.isEmpty())setChanged();return removed;}
    @Override public void clearContent() {if(writable.getAsBoolean())super.clearContent();}
    @Override public void setChanged() {if(writable.getAsBoolean()){persist.run();super.setChanged();}}
}

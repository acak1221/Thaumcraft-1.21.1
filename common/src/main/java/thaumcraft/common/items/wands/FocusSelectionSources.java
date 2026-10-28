package thaumcraft.common.items.wands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.baubles.PlayerAccessories;

/** Physical addresses only: the server resolves all stacks and contents again.
 * Worn pouches precede the36 original main slots; armor/offhand are not scanned.
 *
 * <p>A pouch is any {@link IFocusPouch}: the native eighteen-cell one and an add-on's, of any size. Cells 0..17 keep the
 * compact addresses of the original pouch (1000+slot*18+cell for the main inventory, 2000+slot*18+cell for the worn
 * slots), so every native address is the one it always was. Cells 18..{@link #MAX_CELLS}-1 of a larger pouch have a band of
 * their own after those (3000+slot*110+cell-18 for the main inventory, then the same for the worn slots). Cells from
 * {@link #MAX_CELLS} on have no address: they are neither listed nor given a returned focus.</p> */
public final class FocusSelectionSources {
    /** The cells of a pouch that have the compact addresses of the original pouch. */
    public static final int NATIVE_CELLS=18;
    /** The cells of one pouch that have an address at all; see {@link IFocusPouch#MAX_SELECTABLE_CELLS}. */
    public static final int MAX_CELLS=IFocusPouch.MAX_SELECTABLE_CELLS;
    private static final int EXTRA_CELLS=MAX_CELLS-NATIVE_CELLS;
    private static final int INVENTORY_POUCH=1000,WORN_POUCH=2000,INVENTORY_EXTRA=3000,WORN_EXTRA=INVENTORY_EXTRA+36*EXTRA_CELLS;
    public record Choice(int address,ItemStack focus) {}
    private FocusSelectionSources() {}
    public static int inventoryPouch(int slot,int cell) {
        if(slot<0||slot>=36||cell<0||cell>=MAX_CELLS)throw new IllegalArgumentException("Invalid physical pouch slot");
        return cell<NATIVE_CELLS?INVENTORY_POUCH+slot*NATIVE_CELLS+cell:INVENTORY_EXTRA+slot*EXTRA_CELLS+cell-NATIVE_CELLS;
    }
    public static int wornPouch(int slot,int cell) {
        if(slot<0||slot>=4||cell<0||cell>=MAX_CELLS)throw new IllegalArgumentException("Invalid worn pouch slot");
        return cell<NATIVE_CELLS?WORN_POUCH+slot*NATIVE_CELLS+cell:WORN_EXTRA+slot*EXTRA_CELLS+cell-NATIVE_CELLS;
    }
    public static boolean valid(int address) { return address>=-1&&address<36||isInventoryPouch(address)||isWornPouch(address); }
    public static boolean isInventoryPouch(int address) { return offset(address,INVENTORY_POUCH,36,NATIVE_CELLS)>=0||offset(address,INVENTORY_EXTRA,36,EXTRA_CELLS)>=0; }
    public static boolean isWornPouch(int address) { return offset(address,WORN_POUCH,4,NATIVE_CELLS)>=0||offset(address,WORN_EXTRA,4,EXTRA_CELLS)>=0; }
    public static int outerSlot(int address) {
        int d;
        if((d=offset(address,INVENTORY_POUCH,36,NATIVE_CELLS))>=0)return d/NATIVE_CELLS;
        if((d=offset(address,INVENTORY_EXTRA,36,EXTRA_CELLS))>=0)return d/EXTRA_CELLS;
        if((d=offset(address,WORN_POUCH,4,NATIVE_CELLS))>=0)return d/NATIVE_CELLS;
        if((d=offset(address,WORN_EXTRA,4,EXTRA_CELLS))>=0)return d/EXTRA_CELLS;
        return -1;
    }
    public static int pouchSlot(int address) {
        int d;
        if((d=offset(address,INVENTORY_POUCH,36,NATIVE_CELLS))>=0)return d%NATIVE_CELLS;
        if((d=offset(address,INVENTORY_EXTRA,36,EXTRA_CELLS))>=0)return NATIVE_CELLS+d%EXTRA_CELLS;
        if((d=offset(address,WORN_POUCH,4,NATIVE_CELLS))>=0)return d%NATIVE_CELLS;
        if((d=offset(address,WORN_EXTRA,4,EXTRA_CELLS))>=0)return NATIVE_CELLS+d%EXTRA_CELLS;
        return -1;
    }
    /** The distance of the address from the start of a band of slots*perSlot addresses, or -1 outside it. */
    private static int offset(int address,int base,int slots,int perSlot) { int d=address-base;return d>=0&&d<slots*perSlot?d:-1; }
    /**
     * The cells of a one-pouch stack as independent stacks (a null entry of an add-on pouch reads as empty), or null for
     * anything else. A pouch whose payload it cannot read (it throws {@link IllegalArgumentException}) stays intact and inactive.
     */
    public static ItemStack[] contents(ItemStack stack) {
        if(stack.getCount()!=1||!(stack.getItem() instanceof IFocusPouch pouch))return null;
        ItemStack[] read;
        try { read=pouch.getInventory(stack); }catch(IllegalArgumentException malformed){return null;}
        if(read==null)return null;
        ItemStack[] cells=new ItemStack[read.length];
        for(int cell=0;cell<cells.length;cell++)cells[cell]=read[cell]==null?ItemStack.EMPTY:read[cell].copy();
        return cells;
    }
    /** How many of the cells have an address: the first {@link #MAX_CELLS}. */
    public static int addressable(ItemStack[] contents) { return Math.min(contents.length,MAX_CELLS); }
    public static List<Choice> choices(Player player) {
        List<Choice> choices=new ArrayList<>();var worn=PlayerAccessories.get(player);
        if(!worn.isRejected())for(int slot=0;slot<4;slot++)add(choices,worn.getItem(slot),slot,true);
        for(int slot=0;slot<36;slot++) {
            ItemStack stack=player.getInventory().getItem(slot);
            if(FocusData.isSupportedFocus(stack))choices.add(new Choice(slot,stack.copy()));
            add(choices,stack,slot,false);
        }
        // Retain the native selector's established ability to display physical
        // equal-key duplicates, rather than erase one preview in a TreeMap.
        choices.sort(Comparator.comparing((Choice choice)->((ItemFocusBasic)choice.focus().getItem()).getSortingHelper(choice.focus())).thenComparingInt(Choice::address));
        return List.copyOf(choices);
    }
    private static void add(List<Choice> choices,ItemStack pouch,int slot,boolean worn) {
        ItemStack[] contents=contents(pouch);if(contents==null)return;
        for(int cell=0,cells=addressable(contents);cell<cells;cell++)if(FocusData.isSupportedFocus(contents[cell]))
            choices.add(new Choice(worn?wornPouch(slot,cell):inventoryPouch(slot,cell),contents[cell].copy()));
    }
}

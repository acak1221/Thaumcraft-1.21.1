package thaumcraft.common.items.wands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.baubles.PlayerAccessories;

/** Physical addresses only: the server resolves all stacks and contents again.
 * Worn pouches precede the36 original main slots; armor/offhand are not scanned. */
public final class FocusSelectionSources {
    private static final int INVENTORY_POUCH=1000,WORN_POUCH=2000;
    public record Choice(int address,ItemStack focus) {}
    private FocusSelectionSources() {}
    public static int inventoryPouch(int slot,int cell) {
        if(slot<0||slot>=36||cell<0||cell>=18)throw new IllegalArgumentException("Invalid physical pouch slot");
        return INVENTORY_POUCH+slot*18+cell;
    }
    public static int wornPouch(int slot,int cell) {
        if(slot<0||slot>=4||cell<0||cell>=18)throw new IllegalArgumentException("Invalid worn pouch slot");
        return WORN_POUCH+slot*18+cell;
    }
    public static boolean valid(int address) { return address>=-1&&address<36||isInventoryPouch(address)||isWornPouch(address); }
    public static boolean isInventoryPouch(int address) { return address>=INVENTORY_POUCH&&address<INVENTORY_POUCH+36*18; }
    public static boolean isWornPouch(int address) { return address>=WORN_POUCH&&address<WORN_POUCH+4*18; }
    public static int outerSlot(int address) { return isInventoryPouch(address)?(address-INVENTORY_POUCH)/18:isWornPouch(address)?(address-WORN_POUCH)/18:-1; }
    public static int pouchSlot(int address) { return isInventoryPouch(address)?(address-INVENTORY_POUCH)%18:isWornPouch(address)?(address-WORN_POUCH)%18:-1; }
    /** Invalid externally stored native payloads remain intact and inactive. */
    public static ItemStack[] contents(ItemStack stack) {
        if(stack.getCount()!=1||!(stack.getItem() instanceof ItemFocusPouch pouch))return null;
        try { return pouch.getInventory(stack); }catch(IllegalArgumentException malformed){return null;}
    }
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
        for(int cell=0;cell<contents.length;cell++)if(FocusData.isSupportedFocus(contents[cell]))
            choices.add(new Choice(worn?wornPouch(slot,cell):inventoryPouch(slot,cell),contents[cell].copy()));
    }
}

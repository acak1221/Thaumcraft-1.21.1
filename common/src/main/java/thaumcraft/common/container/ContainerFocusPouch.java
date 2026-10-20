package thaumcraft.common.container;

import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.config.ThaumcraftFocusPouches;
import thaumcraft.common.items.wands.ItemFocusPouch;

/** Original18+36 slots and reverse return order, with actual hand/stack authority
 * and a locked source slot. Stale close can never copy a physical pouch. */
public final class ContainerFocusPouch extends AbstractContainerMenu {
    private final Player viewer;
    private final InteractionHand hand;
    private final ItemStack pouch;
    private ItemStack observed;
    private final InventoryFocusPouch input;
    private final boolean server;
    private final int lockedSlot,hotbar;
    private boolean writing;
    public ContainerFocusPouch(int id,Inventory inventory,InteractionHand hand) { this(id,inventory,hand,true); }
    public ContainerFocusPouch(int id,Inventory inventory,FriendlyByteBuf buffer) { this(id,inventory,buffer.readBoolean()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,false); }
    private ContainerFocusPouch(int id,Inventory inventory,InteractionHand hand,boolean server) {
        super(ThaumcraftFocusPouches.MENU.get(),id);viewer=inventory.player;this.hand=hand;this.server=server;
        pouch=viewer.getItemInHand(hand);observed=pouch.copy();hotbar=inventory.selected;lockedSlot=hand==InteractionHand.MAIN_HAND?45+hotbar:-1;
        ItemStack[] contents=new ItemStack[ItemFocusPouch.SIZE];Arrays.fill(contents,ItemStack.EMPTY);
        if(server)contents=((ItemFocusPouch)pouch.getItem()).getInventory(pouch);
        input=new InventoryFocusPouch(contents,()->!server||viewer.containerMenu==this&&bound(),this::persist);
        for(int slot=0;slot<18;slot++)addSlot(new Slot(input,slot,37+slot%6*18,51+slot/6*18){
            @Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof ItemFocusBasic;}
        });
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(inventory,column+row*9+9,8+column*18,151+row*18));
        for(int column=0;column<9;column++) {
            final boolean locked=hand==InteractionHand.MAIN_HAND&&column==hotbar;
            addSlot(new Slot(inventory,column,8+column*18,209){
                @Override public boolean mayPlace(ItemStack stack){return !locked&&super.mayPlace(stack);}
                @Override public boolean mayPickup(Player player){return !locked&&super.mayPickup(player);}
            });
        }
    }
    public int lockedSlot() { return lockedSlot; }
    private boolean bound() {
        return !writing&&viewer instanceof ServerPlayer player&&player.server.isSameThread()&&player.isAlive()&&!player.isRemoved()&&!player.hasDisconnected()
                &&!player.isSpectator()&&player.server.getPlayerList().getPlayer(player.getUUID())==player
                &&player.getItemInHand(hand)==pouch&&pouch.getCount()==1&&pouch.getItem() instanceof ItemFocusPouch&&ItemStack.matches(pouch,observed);
    }
    private boolean active(Player player) { return player==viewer&&(!server||viewer.containerMenu==this&&bound()); }
    private void persist() {
        if(!server||!bound())return;
        writing=true;
        try { ((ItemFocusPouch)pouch.getItem()).setInventory(pouch,input.snapshot());observed=pouch.copy();viewer.getInventory().setChanged(); }
        finally { writing=false; }
    }
    @Override public boolean stillValid(Player player) { return player==viewer&&(!server||bound()); }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!active(player)||slot==lockedSlot||type==ClickType.SWAP&&(hand==InteractionHand.MAIN_HAND&&button==hotbar||hand==InteractionHand.OFF_HAND&&button==40))return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) {
        if(!active(player)||slot<0||slot>=slots.size()||slot==lockedSlot)return ItemStack.EMPTY;
        Slot source=slots.get(slot);if(!source.hasItem()||!source.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=source.getItem(),before=stack.copy();
        if(!(stack.getItem() instanceof ItemFocusBasic))return ItemStack.EMPTY;
        boolean moved=slot<18?moveItemStackTo(stack,18,54,true):moveItemStackTo(stack,0,18,false);
        if(!moved||stack.getCount()==before.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();source.onTake(player,stack);return before;
    }
    @Override public void removed(Player player) {
        // Also records the original empty Inventory-list presence after a valid
        // open/close, while every actual content change was already persisted.
        if(server&&player==viewer&&bound())persist();
        super.removed(player);
    }
}

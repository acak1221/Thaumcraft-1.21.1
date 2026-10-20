package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftTravelGear;
import thaumcraft.common.items.armor.HarnessData;

/** Original one jar+36 player slots and reverse return. Persist every physical
 * change to the exact source item; never overwrite a new item on stale close. */
public final class ContainerHoverHarness extends AbstractContainerMenu {
    private final Player viewer;
    private final InteractionHand hand;
    private final ItemStack armor;
    private final InventoryHoverHarness input;
    private final boolean server;
    private final int hotbar,lockedSlot;
    private ItemStack observed;
    private boolean writing;
    public ContainerHoverHarness(int id,Inventory inventory,InteractionHand hand) {this(id,inventory,hand,true);}
    public ContainerHoverHarness(int id,Inventory inventory,FriendlyByteBuf buffer) {this(id,inventory,buffer.readBoolean()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,false);}
    private ContainerHoverHarness(int id,Inventory inventory,InteractionHand hand,boolean server) {
        super(ThaumcraftTravelGear.HARNESS_MENU.get(),id);viewer=inventory.player;this.hand=hand;this.server=server;armor=viewer.getItemInHand(hand);observed=armor.copy();
        hotbar=inventory.selected;lockedSlot=hand==InteractionHand.MAIN_HAND?28+hotbar:-1;
        input=new InventoryHoverHarness(server?HarnessData.jar(armor):ItemStack.EMPTY,()->!server||viewer.containerMenu==this&&bound(),this::persist);
        addSlot(new Slot(input,0,80,32){@Override public boolean mayPlace(ItemStack stack){return HarnessData.validFuelJar(stack);}});
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(inventory,9+row*9+column,8+column*18,84+row*18));
        for(int column=0;column<9;column++) {final boolean locked=hand==InteractionHand.MAIN_HAND&&column==hotbar;addSlot(new Slot(inventory,column,8+column*18,142){
            @Override public boolean mayPickup(Player player){return !locked&&super.mayPickup(player);}
            @Override public boolean mayPlace(ItemStack stack){return !locked&&super.mayPlace(stack);}
        });}
    }
    public int lockedSlot() {return lockedSlot;}
    private boolean bound() {return !writing&&viewer instanceof ServerPlayer player&&player.server.isSameThread()&&player.server.getPlayerList().getPlayer(player.getUUID())==player
            &&player.isAlive()&&!player.isRemoved()&&!player.hasDisconnected()&&!player.isSpectator()&&player.getItemInHand(hand)==armor
            &&HarnessData.isHarness(armor)&&ItemStack.matches(armor,observed);}
    private boolean active(Player player) {return player==viewer&&(!server||viewer.containerMenu==this&&bound());}
    private void persist() {if(!server||!bound())return;writing=true;try {HarnessData.setJar(armor,input.getItem(0));observed=armor.copy();viewer.getInventory().setChanged();}finally {writing=false;}}
    @Override public boolean stillValid(Player player) {return player==viewer&&(!server||bound());}
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!active(player)||slot==lockedSlot||type==ClickType.SWAP&&(hand==InteractionHand.MAIN_HAND&&button==hotbar||hand==InteractionHand.OFF_HAND&&button==40))return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) {
        if(!active(player)||slot<0||slot>=slots.size()||slot==lockedSlot)return ItemStack.EMPTY;
        Slot source=slots.get(slot);if(!source.hasItem()||!source.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=source.getItem(),before=stack.copy();if(!HarnessData.validFuelJar(stack))return ItemStack.EMPTY;
        if(!(slot==0?moveItemStackTo(stack,1,37,true):moveItemStackTo(stack,0,1,false))||stack.getCount()==before.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();source.onTake(player,stack);return before;
    }
    @Override public void removed(Player player) {if(server&&player==viewer&&bound())persist();super.removed(player);}
}

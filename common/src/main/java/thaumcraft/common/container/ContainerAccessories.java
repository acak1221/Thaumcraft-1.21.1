package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;

/** Actual native menu slots, carried stack, state ids and click synchronization are vanilla-owned. */
public final class ContainerAccessories extends AbstractContainerMenu {
    private final Player owner;
    private final PlayerAccessoryInventory accessories;
    public ContainerAccessories(int id,Inventory inventory){this(id,inventory,(FriendlyByteBuf)null);}
    public ContainerAccessories(int id,Inventory inventory,FriendlyByteBuf ignored){
        super(ThaumcraftBaubles.MENU.get(),id);owner=inventory.player;accessories=PlayerAccessories.get(owner);
        for(int slot=0;slot<4;slot++) {
            final int index=slot;
            addSlot(new Slot(accessories,slot,53+slot*18,35){
                @Override public boolean mayPlace(ItemStack stack){return accessories.canPlaceItem(index,stack);}
                @Override public boolean mayPickup(Player player){ItemStack stack=getItem();return player==owner&&(stack.isEmpty()||stack.getItem() instanceof IBauble bauble&&bauble.canUnequip(stack,player));}
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(inventory,column+row*9+9,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(inventory,column,8+column*18,142));
    }
    @Override public boolean stillValid(Player player){return player==owner&&accessories.stillValid(player);}
    private boolean active(Player player){return player==owner&&stillValid(player)&&(!(player instanceof ServerPlayer server)||server.containerMenu==this&&server.server.isSameThread());}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(active(player))super.clicked(slot,button,type,player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!active(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack source=slot.getItem(),original=source.copy();boolean moved;
        if(index<4)moved=moveItemStackTo(source,4,40,true);
        else {
            moved=moveItemStackTo(source,0,4,false);
            if(!moved)moved=index<31?moveItemStackTo(source,31,40,false):moveItemStackTo(source,4,31,false);
        }
        if(!moved||source.getCount()==original.getCount())return ItemStack.EMPTY;
        if(source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            // Vanilla quick-move shrinks the original slot object before set(); preserve the original unequip callback.
            if(index<4 && original.getItem() instanceof IBauble bauble)bauble.onUnequipped(original,player);
        } else slot.setChanged();
        slot.onTake(player,source);return original;
    }
}

package thaumcraft.common.container;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.items.ItemBathSalts;
import thaumcraft.common.tiles.TileSpa;

/** Original real salt slot +36 inventory slots, original reverse extraction
 * and ingredient-only quick move. Tank/components also use the real BE packet;
 * native menu data carries exact display amount, type and mix without client
 * mutation of the live server tile. Menu button1 toggles, then returns false. */
public final class ContainerSpa extends AbstractContainerMenu {
    private static final int AMOUNT=0,FLUID_LOW=1,FLUID_HIGH=2,MIX=3,DATA_COUNT=4;
    private final TileSpa tile;
    private final Player viewer;
    private final BlockPos pos;
    private final ContainerData data;
    public ContainerSpa(int id,Inventory inventory,TileSpa tile) {
        this(id,inventory,tile,tile.getBlockPos(),tile,serverData(tile));
    }
    public ContainerSpa(int id,Inventory inventory,FriendlyByteBuf buffer) {
        this(id,inventory,new SimpleContainer(1),buffer.readBlockPos(),null,new SimpleContainerData(DATA_COUNT));
    }
    private ContainerSpa(int id,Inventory inventory,Container contents,BlockPos pos,TileSpa tile,ContainerData data) {
        super(ThaumcraftSpa.SPA_MENU.get(),id);this.tile=tile;this.viewer=inventory.player;this.pos=pos.immutable();this.data=data;
        checkContainerSize(contents,1);checkContainerDataCount(data,DATA_COUNT);
        addSlot(new Slot(contents,0,65,31){
            @Override public boolean mayPlace(ItemStack stack){return stack.getItem() instanceof ItemBathSalts&&(tile==null||active(viewer)&&tile.canPlaceItem(0,stack));}
            @Override public boolean mayPickup(Player player){return tile==null?player==viewer:active(player);}
        });
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(inventory,column+row*9+9,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(inventory,column,8+column*18,142));
        addDataSlots(data);
    }
    private static ContainerData serverData(TileSpa tile) {
        return new ContainerData(){
            @Override public int get(int index) {
                return switch(index){
                    case AMOUNT->tile.getFluidAmountMb();
                    case FLUID_LOW->BuiltInRegistries.FLUID.getId(tile.fluid().getFluid())&65535;
                    case FLUID_HIGH->BuiltInRegistries.FLUID.getId(tile.fluid().getFluid())>>>16;
                    case MIX->tile.getMix()?1:0;
                    default->0;
                };
            }
            @Override public void set(int index,int value) {} // No client setter for live tile state.
            @Override public int getCount(){return DATA_COUNT;}
        };
    }
    public BlockPos spaPos(){return pos;}
    public int getFluidAmountMb(){return data.get(AMOUNT)&65535;}
    public int getCapacityMb(){return 5000;}
    public boolean getMix(){return data.get(MIX)!=0;}
    private Fluid displayFluid(){
        int id=(data.get(FLUID_LOW)&65535)|((data.get(FLUID_HIGH)&65535)<<16);
        Fluid fluid=BuiltInRegistries.FLUID.byId(id);return fluid==null?Fluids.EMPTY:fluid;
    }
    public FluidStack fluid(){
        if(tile!=null)return tile.fluid();
        int amount=getFluidAmountMb();Fluid fluid=displayFluid();if(amount<=0||fluid==Fluids.EMPTY)return FluidStack.empty();
        // Real client BE updates preserve every native fluid component. Menu
        // fields remain authoritative for amount/type while its packet arrives.
        if(viewer.level().isClientSide&&viewer.level().hasChunkAt(pos)&&viewer.level().getBlockEntity(pos) instanceof TileSpa client) {
            FluidStack contents=client.fluid();if(!contents.isEmpty()&&contents.getFluid()==fluid)return contents.copyWithAmount((long)amount*FluidStack.bucketAmount()/1000);
        }
        return FluidStack.create(fluid,(long)amount*FluidStack.bucketAmount()/1000);
    }
    @Override public boolean stillValid(Player player){
        if(player!=viewer||!player.isAlive()||player.isSpectator())return false;
        if(tile!=null)return player.level()==tile.getLevel()&&tile.current()&&tile.stillValid(player);
        return player.level().hasChunkAt(pos)&&player.level().getBlockState(pos).is(ThaumcraftSpa.ARCANE_SPA.get())
            &&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;
    }
    private boolean active(Player player){
        return tile!=null&&tile.canInteract()&&player instanceof ServerPlayer server&&server.server.isSameThread()&&viewer.containerMenu==this&&stillValid(player);
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(button==1&&active(player)){tile.toggleMix();broadcastChanges();}
        return false; // Original side effect deliberately returns false.
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(tile!=null?active(player):viewer.level().isClientSide&&stillValid(player))super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!active(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack source=slot.getItem();if(!(source.getItem() instanceof ItemBathSalts))return ItemStack.EMPTY;
        ItemStack original=source.copy();
        // Original tests ingredient class for BOTH source and target paths;
        // non-salt player inventory does not shuffle between main/hotbar.
        if(!(index==0?moveItemStackTo(source,1,37,true):moveItemStackTo(source,0,1,false))||source.getCount()==original.getCount())return ItemStack.EMPTY;
        if(source.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,source);return original;
    }
}

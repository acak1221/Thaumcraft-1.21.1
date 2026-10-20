package thaumcraft.common.container;

import java.util.Arrays;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.pech.PechTrade;
import thaumcraft.common.lib.pech.PechTradeTables;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original one-input/four-output Pech menu. Server identity, live customer,
 * distance and complete stack snapshots authorize one atomic paid trade.
 * Original untaming-on-reward and unchanged output components are retained;
 * rejected/overflowing/broken-provider plans cannot spend input or pack stock. */
public final class ContainerPech extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH=175,IMAGE_HEIGHT=232;
    private final EntityPech pech;
    private final Player viewer;
    private final int entityId;
    private final SimpleContainer inventory=new SimpleContainer(5);
    private boolean executing,closed;
    public ContainerPech(int id,Inventory playerInventory,EntityPech pech){this(id,playerInventory,pech,pech.getId());if(!pech.isCustomer(viewer))throw new IllegalStateException("Pech menu requires its admitted customer");}
    public ContainerPech(int id,Inventory playerInventory,FriendlyByteBuf buffer){this(id,playerInventory,null,buffer.readVarInt());}
    private ContainerPech(int id,Inventory playerInventory,EntityPech pech,int entityId){
        super(ThaumcraftPech.PECH_MENU.get(),id);this.pech=pech;this.entityId=entityId;viewer=playerInventory.player;
        addSlot(new Slot(inventory,0,36,29));
        for(int row=0;row<2;row++)for(int column=0;column<2;column++)addSlot(new Slot(inventory,1+column+row*2,106+18*column,20+18*row){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int column=0;column<9;column++)addSlot(new Slot(playerInventory,column+row*9+9,8+column*18,84+row*18));
        for(int column=0;column<9;column++)addSlot(new Slot(playerInventory,column,8+column*18,142));
    }
    public Container getMerchantInventory(){return inventory;}
    public EntityPech pech(){return pech!=null?pech:viewer.level().getEntity(entityId) instanceof EntityPech found?found:null;}
    public boolean canTrade(){EntityPech owner=pech();if(owner==null||inventory.getItem(0).isEmpty()||!owner.isValued(inventory.getItem(0)))return false;for(int i=1;i<5;i++)if(!inventory.getItem(i).isEmpty())return false;return true;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(button!=0||pech==null||!(player instanceof ServerPlayer server)||executing||!stillValid(player)||!canTrade())return false;
        if(!server.server.isSameThread())throw new IllegalStateException("Pech trading requires the server thread");
        executing=true;
        try{
            ItemStack input=inventory.getItem(0),before=input.copy();List<ItemStack> oldPack=Arrays.stream(pech.loot).map(ItemStack::copy).toList();
            int value=pech.getValue(before),type=pech.getPechType();
            PechTrade.Plan plan=PechTrade.prepare(value,oldPack,PechTradeTables.entries(server.serverLevel(),type),()->ThaumcraftLoot.dungeonEntries(server.serverLevel()),server.serverLevel().random);
            if(plan==null||!stillValid(player)||!canTrade()||inventory.getItem(0)!=input||!ItemStack.matches(before,input)||value!=pech.getValue(input)||type!=pech.getPechType())return false;
            for(int i=0;i<9;i++)if(!ItemStack.matches(oldPack.get(i),pech.loot[i]))return false;
            var pack=plan.pack();var output=plan.outputs();
            // Raw private state commit before external callbacks/network/sound.
            input.shrink(1);if(input.isEmpty())inventory.setItem(0,ItemStack.EMPTY);
            for(int i=0;i<9;i++)pech.loot[i]=pack.get(i);
            for(int i=0;i<4;i++)inventory.setItem(i+1,output.get(i));
            if(plan.untame()){pech.setTamed(false);pech.updateAINextTick=true;}
            inventory.setChanged();
            // Original ItemStack.onCrafting(...,0), NOT Forge recipe CRAFT_ITEM.
            // ManaBean's contents already rolled on its private reward copy.
            for(ItemStack reward:plan.craftedRewards())reward.onCraftedBy(server.level(),server,0);
            if(plan.untame())pech.playSound(EntityPech.sound("pech_trade"),.4F,1);
            broadcastChanges();return true;
        }finally{executing=false;}
    }
    @Override public boolean stillValid(Player player){
        if(player!=viewer||closed)return false;
        return pech==null?player.level().isClientSide:player instanceof ServerPlayer server&&server.server.isSameThread()&&player.containerMenu==this&&player.isAlive()&&!player.isSpectator()
                &&pech.isAlive()&&pech.isTamed()&&pech.isCustomer(player)&&player.level()==pech.level()&&player.distanceToSqr(pech)<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||pech!=null&&!stillValid(player)||executing)return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;ItemStack stack=slot.getItem(),before=stack.copy();
        if(index<5?!moveItemStackTo(stack,5,41,true):!moveItemStackTo(stack,0,1,true))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return before;
    }
    @Override public void removed(Player player){
        if(closed)return;closed=true;super.removed(player);
        if(pech!=null){
            pech.endTrading(player);
            if(player.level() instanceof ServerLevel level)for(int i=0;i<5;i++){
                ItemStack stack=inventory.getItem(i);if(stack.isEmpty())continue;
                DeferredItemDrops.enqueuePechReturn(level,player,stack);
                inventory.removeItemNoUpdate(i); // Relinquish only after durable ownership was published.
                player.awardStat(Stats.DROP);
            }
        }
    }
}

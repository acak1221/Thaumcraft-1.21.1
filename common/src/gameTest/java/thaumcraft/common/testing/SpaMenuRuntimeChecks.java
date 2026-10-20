package thaumcraft.common.testing;

import dev.architectury.fluid.FluidStack;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.tiles.TileSpa;

/** Actual connected native menu clicks/button packet, salt conservation and
 * live data propagation. Inputs/tank fill are declared fixtures; client art,
 * native fluid rendering and actual remote client packets are not seen here. */
public final class SpaMenuRuntimeChecks {
    private SpaMenuRuntimeChecks() {}
    public static void run(GameTestHelper test){
        try(var actor=new FocusUpgradeRuntimeChecks.Actor(test);
            var foreign=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(40,4,4))){
            actor.put(actor.root,ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState());
            test.assertTrue(actor.level.getBlockEntity(actor.root) instanceof TileSpa,"actual registered spa block creates its real backend BE");
            TileSpa spa=(TileSpa)actor.level.getBlockEntity(actor.root);
            Set<UUID> priorPending=pendingIds(actor);
            try {
                actor.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                var hit=new BlockHitResult(Vec3.atCenterOf(actor.root),Direction.UP,actor.root,false);
                test.assertTrue(actor.player.gameMode.useItemOn(actor.player,actor.level,actor.player.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction()
                    &&actor.player.containerMenu instanceof ContainerSpa,"ordinary connected survival block-RMB opens actual registered extended spa menu");
                var menu=(ContainerSpa)actor.player.containerMenu;
                test.assertTrue(menu.slots.size()==37&&menu.getSlot(0).container==spa&&menu.getSlot(0).x==65&&menu.getSlot(0).y==31,"original one real salt slot +36 native player slots and exact salt coordinates");
                test.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ThaumcraftSpa.BATH_SALTS.get()))&&!menu.getSlot(0).mayPlace(new ItemStack(Items.SUGAR)),"native salt slot accepts only the actual original ingredient class");
                actor.player.getInventory().setItem(9,new ItemStack(Items.DIRT,7));
                menu.clicked(1,0,ClickType.QUICK_MOVE,actor.player);
                test.assertTrue(spa.getItem(0).isEmpty()&&actor.player.getInventory().getItem(9).getCount()==7,"original non-salt quick move does not shuffle or consume player inventory");
                actor.player.getInventory().setItem(10,new ItemStack(ThaumcraftSpa.BATH_SALTS.get(),10));
                menu.clicked(2,0,ClickType.QUICK_MOVE,actor.player);
                test.assertTrue(spa.getItem(0).getCount()==10&&actor.player.getInventory().getItem(10).isEmpty(),"actual native quick move transfers exactly10 declared salts into real backend inventory");
                menu.clicked(0,0,ClickType.PICKUP,actor.player);
                test.assertTrue(spa.getItem(0).isEmpty()&&menu.getCarried().is(ThaumcraftSpa.BATH_SALTS.get())&&menu.getCarried().getCount()==10,"actual normal slot click takes the real backend stack to native carried ownership");
                menu.clicked(0,0,ClickType.PICKUP,actor.player);
                test.assertTrue(menu.getCarried().isEmpty()&&spa.getItem(0).getCount()==10,"normal placement returns the same10 salts without duplication");
                menu.clicked(0,0,ClickType.QUICK_MOVE,actor.player);
                test.assertTrue(spa.getItem(0).isEmpty()&&actor.player.getInventory().getItem(8).is(ThaumcraftSpa.BATH_SALTS.get())&&actor.player.getInventory().getItem(8).getCount()==10,
                    "original reverse extraction starts at last hotbar slot8, not first inventory slot");
                menu.clicked(36,0,ClickType.QUICK_MOVE,actor.player);
                actor.player.getInventory().setItem(11,new ItemStack(ThaumcraftSpa.BATH_SALTS.get(),60));
                menu.clicked(3,0,ClickType.QUICK_MOVE,actor.player);
                test.assertTrue(spa.getItem(0).getCount()==64&&actor.player.getInventory().getItem(11).getCount()==6,"native merge fills original64-stack limit and retains6 actual remainder (70 total)");
                menu.clicked(3,0,ClickType.QUICK_MOVE,actor.player);
                test.assertTrue(spa.getItem(0).getCount()==64&&actor.player.getInventory().getItem(11).getCount()==6,"full native salt slot refuses additional quick move without losing remainder");
                actor.player.getInventory().setItem(12,new ItemStack(Items.SUGAR,3));
                menu.clicked(4,0,ClickType.PICKUP,actor.player);menu.clicked(0,0,ClickType.PICKUP,actor.player);
                test.assertTrue(menu.getCarried().is(Items.SUGAR)&&menu.getCarried().getCount()==3&&spa.getItem(0).getCount()==64,"normal carried sugar cannot swap into the class-limited salt slot");
                menu.clicked(4,0,ClickType.PICKUP,actor.player);
                var buffer=new FriendlyByteBuf(Unpooled.buffer());ContainerSpa shadow;
                try {buffer.writeBlockPos(actor.root);shadow=new ContainerSpa(menu.containerId,actor.player.getInventory(),buffer);}finally{buffer.release();}
                List<Integer> changed=new ArrayList<>();
                var listener=new ContainerListener(){
                    @Override public void slotChanged(AbstractContainerMenu source,int slot,ItemStack stack) {}
                    @Override public void dataChanged(AbstractContainerMenu source,int index,int value){changed.add(index);shadow.setData(index,(short)value);}
                };
                menu.addSlotListener(listener);
                try {
                    long unit=FluidStack.bucketAmount();
                    test.assertTrue(spa.fill(Direction.UP,FluidStack.create(Fluids.WATER,unit*9/2),true)==unit*9/2,"real direct backend fill accepts declared4500mB fixture even on UP, preserving original direct/advertised-side distinction");
                    menu.broadcastChanges();
                    test.assertTrue(menu.getFluidAmountMb()==4500&&shadow.getFluidAmountMb()==4500&&shadow.getCapacityMb()==5000
                        &&shadow.fluid().getFluid()==Fluids.WATER&&shadow.fluid().getAmount()==unit*9/2,"actual native ContainerListener data propagates real fluid type/amount/5bucket capacity into read-only display state");
                    FluidStack display=menu.fluid();display.setAmount(1);
                    test.assertTrue(spa.getFluidAmountMb()==4500,"menu fluid accessor returns defensive display ownership, never a live writable backend tank");
                    boolean initial=spa.getMix();changed.clear();
                    test.assertTrue(!menu.clickMenuButton(actor.player,1)&&spa.getMix()!=initial&&shadow.getMix()==spa.getMix()&&changed.contains(3),
                        "original button1 mutates+native-syncs mix yet deliberately returns false");
                    actor.player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(menu.containerId,1));
                    test.assertTrue(spa.getMix()==initial&&shadow.getMix()==initial,"actual connected serverbound native menu button packet reaches the same authoritative toggle and sync");
                    actor.player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(menu.containerId+1,1));
                    test.assertTrue(spa.getMix()==initial&&!menu.clickMenuButton(actor.player,0)&&!menu.clickMenuButton(actor.player,2)&&spa.getMix()==initial,"foreign container ID and undefined button indices cannot toggle mix");
                    test.assertTrue(!shadow.clickMenuButton(actor.player,1)&&shadow.quickMoveStack(actor.player,0).isEmpty()&&spa.getMix()==initial,"read-only menu shadow cannot acquire server mutation authority");
                    shadow.setData(0,1);shadow.setData(3,initial?0:1);
                    menu.setData(0,1);menu.setData(3,initial?0:1);
                    test.assertTrue(spa.getFluidAmountMb()==4500&&spa.getMix()==initial,"client-shadow and live-menu data setters cannot mutate backend tank or mixing");
                    var lease=spa.beginFluidTransaction(new Object());test.assertTrue(lease!=null,"actual backend accepts one native external transaction lease");
                    try {
                        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(ThaumcraftSpa.BATH_SALTS.get()))&&!menu.getSlot(0).mayPickup(actor.player),"native salt Slot refuses both directions while real fluid lease owns the backend");
                        menu.clicked(0,0,ClickType.QUICK_MOVE,actor.player);
                        test.assertTrue(!menu.clickMenuButton(actor.player,1)&&menu.quickMoveStack(actor.player,0).isEmpty()&&spa.getMix()==initial,"actual menu button/quick move cannot bypass the native fluid lease");
                    } finally {lease.finish(false);}
                    test.assertTrue(spa.getItem(0).getCount()==64&&spa.getFluidAmountMb()==4500&&spa.getMix()==initial,"releasing actual lease retains original salts/tank/mix after refused menu operations");
                    foreign.player.teleportTo(actor.root.getX()+2.5,actor.root.getY(),actor.root.getZ()+.5);
                    test.assertTrue(!menu.stillValid(foreign.player)&&!menu.clickMenuButton(foreign.player,1),"another actual connected nearby survival player cannot control this viewer's menu");
                    menu.clicked(0,0,ClickType.QUICK_MOVE,foreign.player);
                    test.assertTrue(spa.getItem(0).getCount()==64&&spa.getMix()==initial&&foreign.player.getInventory().items.stream().allMatch(ItemStack::isEmpty),"foreign native clicks preserve real salt ownership and foreign inventory");
                    actor.player.teleportTo(actor.root.getX()+20.5,actor.root.getY(),actor.root.getZ()+.5);
                    test.assertTrue(!menu.stillValid(actor.player)&&!menu.clickMenuButton(actor.player,1)&&menu.quickMoveStack(actor.player,0).isEmpty(),"out-of-range owner cannot mutate salt slot or toggle old menu");
                    actor.aim();actor.player.closeContainer();
                    test.assertTrue(!menu.clickMenuButton(actor.player,1)&&menu.quickMoveStack(actor.player,0).isEmpty()&&spa.getItem(0).getCount()==64,"closed native menu retains machine stock and rejects stale callbacks");
                    test.assertTrue(actor.player.gameMode.useItemOn(actor.player,actor.level,actor.player.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction()
                        &&actor.player.containerMenu instanceof ContainerSpa reopened&&reopened!=menu&&reopened.getSlot(0).container==spa,"actual reopen binds one new native session to the same preserved backend slot");
                    test.assertTrue(!menu.clickMenuButton(actor.player,1)&&spa.getMix()==initial,"stale previous session cannot toggle after actual reopen");
                } finally {menu.removeSlotListener(listener);}
            } finally {
                actor.player.closeContainer();spa.evacuate();
                try {flushOwnedRetirement(test,actor,priorPending);}
                finally {actor.put(actor.root,Blocks.AIR.defaultBlockState());actor.purgeOutputs();}
            }
        }
        test.succeed();
    }
    private static DeferredItemDrops queue(FocusUpgradeRuntimeChecks.Actor actor){
        return actor.level.getDataStorage().get(new net.minecraft.world.level.saveddata.SavedData.Factory<>(DeferredItemDrops::new,DeferredItemDrops::load,null),"thaumcraft_pending_inventory_drops");
    }
    private static Set<UUID> pendingIds(FocusUpgradeRuntimeChecks.Actor actor){
        var result=new HashSet<UUID>();var queue=queue(actor);if(queue!=null)for(Tag raw:queue.save(new CompoundTag(),actor.level.registryAccess()).getList("Pending",Tag.TAG_COMPOUND))result.add(((CompoundTag)raw).getUUID("Id"));return result;
    }
    private static boolean ownsPending(FocusUpgradeRuntimeChecks.Actor actor,Set<UUID> before){
        var queue=queue(actor);if(queue==null)return false;
        for(Tag raw:queue.save(new CompoundTag(),actor.level.registryAccess()).getList("Pending",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)raw;if(row.getLong("Source")==actor.root.asLong()&&!before.contains(row.getUUID("Id")))return true;
        }
        return false;
    }
    private static void flushOwnedRetirement(GameTestHelper test,FocusUpgradeRuntimeChecks.Actor actor,Set<UUID> before){
        var queue=queue(actor);if(queue==null)return;
        int passes=Math.max(1,(queue.pendingCount()+63)/64+1);
        for(int i=0;i<passes&&ownsPending(actor,before);i++)queue.flush(actor.level);
        test.assertTrue(!ownsPending(actor,before),"fixture retires/admits its actual backend salt ownership before generic Actor cleanup");
    }
}

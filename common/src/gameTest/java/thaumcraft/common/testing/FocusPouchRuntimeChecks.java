package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.config.ThaumcraftFocusPouches;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.container.ContainerFocusPouch;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.items.wands.FocusSelectionSources;
import thaumcraft.common.items.wands.ItemFocusPouch;
import thaumcraft.common.items.wands.ItemFocusPouchBauble;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.AccessoryNetworking;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;

/** Real connected actor, actual native click packets, saved/native-wire contents
 * and actual worn belt selection. Direct selection receiver calls are not a
 * claim that a physical client's custom payload was dispatched. */
public final class FocusPouchRuntimeChecks {
    private FocusPouchRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        try(var a=new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,240,4))) {
            recipeAndStorage(a);nativeMenu(a);selection(a);
        }
        test.succeed();
    }
    private static ItemStack[] empty() { ItemStack[] out=new ItemStack[18];Arrays.fill(out,ItemStack.EMPTY);return out; }
    private static ItemStack focus(FocusUpgradeRuntimeChecks.Actor a,String name) {
        ItemStack stack=a.focus(ThaumcraftFoci.FIRE_FOCUS.get(),FocusUpgradeType.frugal,1,FocusUpgradeType.potency,2);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack;
    }
    private static ItemStack pouch() { return new ItemStack(ThaumcraftFocusPouches.FOCUS_POUCH.get()); }
    private static ItemFocusPouchBauble item() { return ThaumcraftFocusPouches.FOCUS_POUCH.get(); }
    private static void click(ServerPlayer player,int slot,int button,ClickType type) {
        var menu=player.containerMenu;
        player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),slot,button,type,ItemStack.EMPTY,new Int2ObjectOpenHashMap<>()));
    }
    private static ContainerFocusPouch open(FocusUpgradeRuntimeChecks.Actor a,InteractionHand hand) {
        a.require(a.player.gameMode.useItem(a.player,a.level,a.player.getItemInHand(hand),hand).consumesAction()
                        &&a.player.containerMenu instanceof ContainerFocusPouch,"actual pouch RMB opens its registered native eighteen-slot menu");
        return (ContainerFocusPouch)a.player.containerMenu;
    }
    private static void recipeAndStorage(FocusUpgradeRuntimeChecks.Actor a) {
        var recipe=(NativeArcaneRecipe)a.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/focus_pouch")).orElseThrow().value();
        var grid=new java.util.ArrayList<ItemStack>();
        for(int slot=0;slot<9;slot++)grid.add(new ItemStack(slot==1?Items.GOLD_INGOT:slot==4?ThaumcraftBaubles.MUNDANE_BELT.get():Items.LEATHER));
        var input=new ArcaneRecipeInput(grid);KnowledgeAccess.get(a.player).removeResearch("FOCUSPOUCH");
        a.require(recipe.matches(input,a.level)&&recipe.vis().equals(Map.of("terra",10,"ordo",10,"perditio",10))
                        &&recipe.assemble(input,a.level.registryAccess()).is(item())&&!recipe.hasResearch(input,a.player),
                "original real arcane leather/gold/mundane-belt matrix costs10terra10ordo10perditio and requires FOCUSPOUCH");
        KnowledgeAccess.get(a.player).addResearch("FOCUSPOUCH");a.require(recipe.hasResearch(input,a.player),"actual pouch research satisfies the native recipe gate");
        ItemStack pouch=pouch(),named=focus(a,"physical saved focus");ItemStack[] contents=empty();contents[17]=named;
        item().setInventory(pouch,contents);contents[17].setCount(2);
        a.require(item().getInventory(pouch)[17].getCount()==1&&item().getInventory(pouch).length==18&&item().getBaubleType(pouch)==BaubleType.BELT,
                "single registered Bauble subclass is a belt and storage never aliases caller entries");
        ItemStack[] exposed=item().getInventory(pouch);exposed[17].set(DataComponents.CUSTOM_NAME,Component.literal("changed copy"));
        a.require(item().getInventory(pouch)[17].getHoverName().getString().equals("physical saved focus"),"getInventory exposes no mutable native item aliases");
        ItemStack restored=ItemStack.parse(a.level.registryAccess(),pouch.save(a.level.registryAccess())).orElseThrow();
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),a.level.registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer,restored);var decoded=ItemStack.STREAM_CODEC.decode(buffer);
            a.require(ItemStack.matches(decoded,pouch)&&ItemStack.matches(item().getInventory(decoded)[17],item().getInventory(pouch)[17]),
                    "actual pouch NBT and native ItemStack stream retain slot17, name and rank components once");
            var extended=new FocusSelectionPayload(InteractionHand.OFF_HAND,FocusSelectionSources.wornPouch(3,17));
            FocusSelectionPayload.STREAM_CODEC.encode(buffer,extended);
            a.require(buffer.readableBytes()==4&&FocusSelectionPayload.STREAM_CODEC.decode(buffer).equals(extended),"pouch source stream carries only validated hand/physical address intent");
            buffer.writeByte(0);buffer.writeByte(127);buffer.writeShort(1800);
            boolean refused=false;try{FocusSelectionPayload.STREAM_CODEC.decode(buffer);}catch(io.netty.handler.codec.DecoderException expected){refused=true;}
            a.require(refused,"address gap cannot name another server inventory or item");
        }finally {buffer.release();}
    }
    private static void nativeMenu(FocusUpgradeRuntimeChecks.Actor a) {
        a.player.getInventory().clearContent();a.player.getInventory().selected=0;
        ItemStack pouch=pouch(),first=focus(a,"native menu focus");a.player.setItemInHand(InteractionHand.MAIN_HAND,pouch);a.player.getInventory().setItem(9,first.copy());
        var menu=open(a,InteractionHand.MAIN_HAND);a.require(menu.slots.size()==54&&menu.lockedSlot()==45,"original18+36 native slots lock the actual source hotbar cell");
        click(a.player,18,0,ClickType.QUICK_MOVE);
        a.require(a.player.getInventory().getItem(9).isEmpty()&&ItemStack.matches(item().getInventory(pouch)[0],first)&&menu.getCarried().isEmpty(),
                "native quick-move removes one actual focus and persists its first pouch cell immediately before close");
        click(a.player,45,0,ClickType.PICKUP);click(a.player,0,0,ClickType.SWAP);
        a.require(a.player.getMainHandItem()==pouch&&ItemStack.matches(item().getInventory(pouch)[0],first)&&menu.getCarried().isEmpty(),
                "native pickup and number-key swap cannot remove the open physical pouch");
        click(a.player,0,0,ClickType.QUICK_MOVE);
        a.require(item().getInventory(pouch)[0].isEmpty()&&ItemStack.matches(a.player.getInventory().getItem(8),first),
                "pouch quick-move returns through the original reverse inventory order");
        a.player.getInventory().setItem(11,new ItemStack(Items.STICK));click(a.player,20,0,ClickType.PICKUP);click(a.player,0,0,ClickType.PICKUP);
        a.require(menu.getCarried().is(Items.STICK)&&item().getInventory(pouch)[0].isEmpty(),"ordinary native insertion refuses non-focus material without consuming its cursor stack");
        click(a.player,20,0,ClickType.PICKUP);click(a.player,53,0,ClickType.QUICK_MOVE);
        ItemStack replacement=pouch.copy();a.player.setItemInHand(InteractionHand.MAIN_HAND,replacement);
        menu.clicked(0,0,ClickType.PICKUP,a.player);a.player.closeContainer();
        a.require(menu.getCarried().isEmpty()&&ItemStack.matches(item().getInventory(replacement)[0],first)
                        &&ItemStack.matches(item().getInventory(pouch)[0],first),"a detached menu cannot withdraw or overwrite its replacement physical pouch on stale click/close");
        // The declared replacement is an independent fixture input, not an item
        // copied by gameplay; retain only it for subsequent physical operations.
        pouch=replacement;a.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);a.player.setItemInHand(InteractionHand.OFF_HAND,pouch);
        menu=open(a,InteractionHand.OFF_HAND);click(a.player,0,40,ClickType.SWAP);
        a.require(a.player.getOffhandItem()==pouch&&ItemStack.matches(item().getInventory(pouch)[0],first),"offhand swap40 cannot remove the active offhand pouch");
        click(a.player,0,0,ClickType.PICKUP);
        a.require(ItemStack.matches(menu.getCarried(),first)&&item().getInventory(pouch)[0].isEmpty(),"ordinary withdrawal saves the empty physical slot before carried-stack ownership changes");
        a.player.closeContainer();menu.clicked(0,0,ClickType.PICKUP,a.player);
        a.require(item().getInventory(pouch)[0].isEmpty()&&a.player.getInventory().items.stream().filter(s->ItemStack.matches(s,first)).count()==1,
                "native close returns its one carried focus and stale menu replay cannot restore it");
        menu=open(a,InteractionHand.OFF_HAND);ItemStack external=focus(a,"external server replacement");ItemStack[] changed=empty();changed[0]=external;item().setInventory(pouch,changed);
        menu.clicked(0,0,ClickType.PICKUP,a.player);a.player.closeContainer();
        a.require(menu.getCarried().isEmpty()&&ItemStack.matches(item().getInventory(pouch)[0],external),"changed full native pouch component invalidates the old mirror and cannot be overwritten on close");
        ItemStack[] foreign=empty();foreign[0]=new ItemStack(Items.STICK,64);item().setInventory(pouch,foreign);
        menu=open(a,InteractionHand.OFF_HAND);
        var foreignWire=new RegistryFriendlyByteBuf(Unpooled.buffer(),a.level.registryAccess());
        var opening=new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
        try {
            ItemStack.STREAM_CODEC.encode(foreignWire,pouch);var decoded=ItemStack.STREAM_CODEC.decode(foreignWire);
            opening.writeBoolean(true);var mirror=new ContainerFocusPouch(menu.containerId,a.player.getInventory(),opening);
            mirror.getSlot(0).set(item().getInventory(decoded)[0]);
            a.require(mirror.getSlot(0).getItem().is(Items.STICK)&&mirror.getSlot(0).getItem().getCount()==64
                            &&mirror.getSlot(0).getMaxStackSize()==1&&!mirror.getSlot(0).mayPlace(new ItemStack(Items.STICK))
                            &&item().getInventory(pouch)[0].getCount()==64,
                    "native full-count item stream and client-constructor Slot.set preserve foreign64 while actual insertion stays one and focus-only");
        }finally {foreignWire.release();opening.release();}
        click(a.player,0,0,ClickType.QUICK_MOVE);
        a.require(item().getInventory(pouch)[0].getCount()==64,"original foreign saved entries remain intact and cannot quick-move through a focus-only transfer");
        click(a.player,0,0,ClickType.PICKUP);a.require(menu.getCarried().is(Items.STICK)&&menu.getCarried().getCount()==64&&item().getInventory(pouch)[0].isEmpty(),
                "ordinary native withdrawal recovers the whole foreign saved stack without truncation");a.player.closeContainer();
        var oversized=new java.util.ArrayList<ItemStack>();for(int slot=0;slot<19;slot++)oversized.add(focus(a,"oversized declared "+slot));
        pouch.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(oversized));ItemStack before=pouch.copy();
        a.require(!a.player.gameMode.useItem(a.player,a.level,pouch,InteractionHand.OFF_HAND).consumesAction()
                        &&a.player.containerMenu==a.player.inventoryMenu&&ItemStack.matches(pouch,before),"oversized native payload is retained inactive rather than truncated into eighteen slots");
        a.player.getInventory().clearContent();
    }
    private static boolean select(FocusUpgradeRuntimeChecks.Actor a,int address) { return FocusSelectionNetworking.handle(a.player,new FocusSelectionPayload(InteractionHand.MAIN_HAND,address)); }
    private static void selection(FocusUpgradeRuntimeChecks.Actor a) {
        a.player.getInventory().clearContent();ItemStack belt=pouch(),beltBefore=belt.copy();a.player.getInventory().setItem(9,belt);
        a.require(AccessoryNetworking.open(a.player)&&a.player.containerMenu instanceof ContainerAccessories,"real accessories receiver opens the native wearable menu");
        click(a.player,4,0,ClickType.QUICK_MOVE);a.player.closeContainer();
        a.require(ItemStack.matches(PlayerAccessories.get(a.player).getItem(3),beltBefore)&&a.player.getInventory().getItem(9).isEmpty(),"actual native quick-move equips the one pouch in the real belt slot");
        belt=PlayerAccessories.get(a.player).getItem(3);ItemStack inventoryPouch=pouch(),old=focus(a,"old installed focus"),next=focus(a,"selected worn focus");
        ItemStack[] worn=empty(),stored=empty();for(int slot=0;slot<18;slot++){worn[slot]=focus(a,"worn filler "+slot);stored[slot]=focus(a,"inventory filler "+slot);}worn[5]=next;
        item().setInventory(belt,worn);item().setInventory(inventoryPouch,stored);a.equip(old,"gold",137);
        for(int slot=1;slot<36;slot++)a.player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));a.player.getInventory().setItem(9,inventoryPouch);
        a.require(FocusSelectionSources.choices(a.player).stream().anyMatch(c->c.address()==FocusSelectionSources.wornPouch(3,5)&&ItemStack.matches(c.focus(),next)),
                "real worn pouch contributes its actual component-bearing cell to common selector previews");
        a.require(select(a,FocusSelectionSources.wornPouch(3,5))&&ItemStack.matches(FocusData.get(a.wand),next)
                        &&ItemStack.matches(item().getInventory(belt)[5],old)&&a.vis("ignis")==137,
                "full-storage swap extracts its worn source before returning the old focus to that freed pouch cell without vis payment");
        ItemStack unchanged=a.wand.copy(),unchangedBelt=belt.copy(),unchangedPouch=inventoryPouch.copy();
        a.require(!select(a,-1)&&ItemStack.matches(a.wand,unchanged)&&ItemStack.matches(belt,unchangedBelt)&&ItemStack.matches(inventoryPouch,unchangedPouch),
                "full native inventory and pouches refuse removal atomically with no drop, overflow or deletion");
        stored=item().getInventory(inventoryPouch);stored[7]=ItemStack.EMPTY;item().setInventory(inventoryPouch,stored);
        a.require(select(a,-1)&&FocusData.get(a.wand).isEmpty()&&ItemStack.matches(item().getInventory(inventoryPouch)[7],next),
                "removal scans full worn storage before the original first empty main-inventory pouch cell");
        a.require(select(a,FocusSelectionSources.inventoryPouch(9,7))&&item().getInventory(inventoryPouch)[7].isEmpty()&&ItemStack.matches(FocusData.get(a.wand),next),
                "actual inventory pouch cell moves its whole physical focus into the real wand once");
        ItemStack loose=focus(a,"loose native focus");a.player.getInventory().setItem(10,loose);
        a.require(select(a,10)&&ItemStack.matches(FocusData.get(a.wand),loose)&&a.player.getInventory().getItem(10).isEmpty()
                        &&ItemStack.matches(item().getInventory(inventoryPouch)[7],next),"loose selection returns the previous focus to available pouch storage before its just-freed main slot");
        a.player.inventoryMenu.setCarried(new ItemStack(Items.STICK));unchanged=a.wand.copy();
        a.require(!select(a,FocusSelectionSources.wornPouch(3,5))&&ItemStack.matches(a.wand,unchanged),"actual native carried material blocks selection during an unfinished inventory operation");
        a.player.inventoryMenu.setCarried(ItemStack.EMPTY);a.player.getInventory().setItem(9,ItemStack.EMPTY);a.player.setItemInHand(InteractionHand.OFF_HAND,inventoryPouch);
        open(a,InteractionHand.OFF_HAND);a.require(!select(a,FocusSelectionSources.wornPouch(3,5)),"active real pouch menu cannot race its mirror against a wand selection");a.player.closeContainer();
        a.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));unchangedBelt=belt.copy();
        a.require(!select(a,FocusSelectionSources.wornPouch(3,5))&&ItemStack.matches(belt,unchangedBelt)&&a.items().isEmpty(),"late real hand change refuses extraction and all declared focus transfers produce no item drops");
    }
}

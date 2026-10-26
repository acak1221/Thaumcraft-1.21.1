package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.config.ThaumcraftTravelGear;
import thaumcraft.common.container.ContainerHoverHarness;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.items.armor.HarnessData;
import thaumcraft.common.items.armor.Hover;
import thaumcraft.common.items.armor.TravelGearTick;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;
import thaumcraft.common.lib.network.HoverTogglePayload;
import thaumcraft.common.lib.runic.RunicArmor;
import thaumcraft.common.mixin.ServerGamePacketListenerFloatAccess;

/** Owned actual connected player, native armor clicks/movement and native living
 * steps. Declared resources are not natural-acquisition or physical-client wire
 * acceptance. Root owns native wrappers/build. */
public final class TravelGearRuntimeChecks {
    private TravelGearRuntimeChecks() {}
    private static FocusUpgradeRuntimeChecks.Actor actor(GameTestHelper test) {return new FocusUpgradeRuntimeChecks.Actor(test,new BlockPos(4,200,4));}
    private static void click(FocusUpgradeRuntimeChecks.Actor a,int slot,int button,ClickType type) {
        var menu=a.player.containerMenu;a.player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),slot,button,type,ItemStack.EMPTY,new Int2ObjectOpenHashMap<>()));
    }
    private static void equip(FocusUpgradeRuntimeChecks.Actor a,ItemStack stack,int armorSlot) {
        a.require(a.player.containerMenu==a.player.inventoryMenu&&a.player.inventoryMenu.getCarried().isEmpty(),"native equipment fixture starts with its actual empty cursor");
        a.player.getInventory().setItem(9,stack.copy());click(a,9,0,ClickType.PICKUP);click(a,armorSlot,0,ClickType.PICKUP);
        a.require(a.player.inventoryMenu.getCarried().isEmpty()&&a.player.getInventory().getItem(9).isEmpty(),"actual native inventory clicks move one physical armor item to its real equipment slot");
    }
    private static ItemStack jar(int amount) {
        ItemStack jar=ItemJar.withContents(new ItemStack(ThaumcraftContent.JAR.get()),Aspect.ENERGY,amount);
        jar.set(DataComponents.CUSTOM_NAME,Component.literal("actual flight fuel"));jar.set(ThaumcraftLabels.JAR_LABEL_DATA.get(),new JarLabelData("potentia"));return jar;
    }
    private static ItemStack harness(int fuel) {ItemStack armor=new ItemStack(ThaumcraftTravelGear.HOVER_HARNESS.get());HarnessData.setJar(armor,jar(fuel));return armor;}
    private static void nativeStep(FocusUpgradeRuntimeChecks.Actor a) {a.player.setDeltaMovement(Vec3.ZERO);a.player.zza=0;a.player.aiStep();}
    private static InfusionStackRecipe recipe(FocusUpgradeRuntimeChecks.Actor a,String id) {return (InfusionStackRecipe)a.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion/"+id)).orElseThrow().value();}
    public static void boots(GameTestHelper test) {
        try(var a=actor(test)) {
            a.clearLane();var item=ThaumcraftTravelGear.BOOTS_TRAVELLER.get();var recipe=recipe(a,"boots_traveller");
            a.require(recipe.research().equals("BOOTSTRAVELLER")&&recipe.instability()==1&&recipe.aspects().equals(Map.of("volatus",25,"iter",25))
                            &&recipe.input().test(new ItemStack(Items.LEATHER_BOOTS))&&recipe.inputDamage().orElse(-1)==0&&recipe.result().is(item),"actual original pristine leather-boots infusion has Flight25/Travel25, instability1 and real research/output");
            a.require(recipe.components().size()==6&&recipe.components().get(0).test(new ItemStack(ThaumcraftContent.SHARDS.get("aer").get()))
                            &&recipe.components().get(1).test(new ItemStack(ThaumcraftContent.SHARDS.get("aer").get()))&&recipe.components().get(2).test(new ItemStack(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()))
                            &&recipe.components().get(3).test(new ItemStack(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()))&&recipe.components().get(4).test(new ItemStack(Items.FEATHER)),"actual six original boots components retain duplicate shard/fabric counts and feather");
            for(Item fish:List.of(Items.COD,Items.SALMON,Items.TROPICAL_FISH,Items.PUFFERFISH))a.require(recipe.components().get(5).test(new ItemStack(fish)),"original ItemFishFood wildcard includes raw "+fish);
            for(Item cooked:List.of(Items.COOKED_COD,Items.COOKED_SALMON))a.require(!recipe.components().get(5).test(new ItemStack(cooked)),"cooked fish cannot replace the old raw fish wildcard");
            ItemStack boots=new ItemStack(item);a.require(boots.getMaxDamage()==350&&boots.getRarity()==Rarity.RARE&&item.getDefense()==1&&item.getEnchantmentValue()==25&&RunicArmor.finalCharge(boots)==0
                            &&RunicArmor.finalCharge(RunicArmor.augment(boots))==1,"actual SPECIAL boots keep350 durability/1armor/25enchantability/rare and genuine runic eligibility without free base charge");
            for(Item repair:List.of(Items.LEATHER,Items.GOLD_INGOT,ThaumcraftContent.MATERIALS.get("enchanted_fabric").get()))a.require(!item.isValidRepairItem(boots,new ItemStack(repair)),"original SPECIAL boots have no invented ordinary repair material");
            a.player.setSprinting(false);a.player.setOnGround(true);a.player.setDeltaMovement(Vec3.ZERO);a.player.jumpFromGround();double baseline=a.player.getDeltaMovement().y;
            a.player.setItemInHand(InteractionHand.MAIN_HAND,boots);a.require(a.player.gameMode.useItem(a.player,a.level,boots,InteractionHand.MAIN_HAND).consumesAction()
                            &&a.player.getMainHandItem().isEmpty()&&a.player.getItemBySlot(EquipmentSlot.FEET).is(item),"actual native ArmorItem RMB equips the physical boots once");
            a.player.setDeltaMovement(Vec3.ZERO);a.player.jumpFromGround();a.require(a.player.getDeltaMovement().y==baseline+(double).275F,"actual native Player jump preserves vanilla jump and adds exact original double-promoted.275F");
            a.player.setYRot(0);a.player.zza=1;a.player.setOnGround(true);a.player.setDeltaMovement(Vec3.ZERO);a.player.fallDistance=.125F;
            double step=a.player.getAttributeValue(Attributes.STEP_HEIGHT);TravelGearTick.tick(a.player);
            a.require(a.player.getDeltaMovement().z==(double).055F&&a.player.getDeltaMovement().x==0&&a.player.fallDistance==-.125F
                            &&a.player.getAttributeValue(Attributes.STEP_HEIGHT)==step,"real worn tick adds original ground acceleration/unclamped fall reduction while server step-height stays unchanged");
            var haste=a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EquipmentEnchantments.HASTE);a.player.getItemBySlot(EquipmentSlot.FEET).enchant(haste,1);
            a.player.hasImpulse=false;a.player.setDeltaMovement(Vec3.ZERO);EquipmentEnchantments.playerLivingTick(a.player);
            a.require(Math.abs(a.player.getDeltaMovement().z-((double).055F+(double).015F))<1e-12,"the actual shared tick applies boots acceleration and separate Haste exactly once");
            a.player.zza=-1;a.player.setDeltaMovement(Vec3.ZERO);TravelGearTick.tick(a.player);a.require(a.player.getDeltaMovement().equals(Vec3.ZERO),"backward input receives no boots forward acceleration");
            a.player.zza=1;a.player.getAbilities().flying=true;TravelGearTick.tick(a.player);a.require(a.player.getDeltaMovement().equals(Vec3.ZERO),"native flight receives no boots ground boost");a.player.getAbilities().flying=false;
            a.player.setOnGround(false);a.player.setDeltaMovement(Vec3.ZERO);a.player.travel(new Vec3(0,0,1));double fast=a.player.getDeltaMovement().z;
            a.require(fast>.04549&&fast<.04551,"actual native air travel uses boots.05 rather than vanilla.02");
            equip(a,harness(2),6);a.require(Hover.toggleHover(a.player)&&Hover.getHover(a.player)&&!a.player.getAbilities().flying,"real funded harness toggle precedes its first paid native flight frame");
            a.player.setOnGround(false);a.player.zza=1;a.player.setDeltaMovement(Vec3.ZERO);a.player.travel(new Vec3(0,0,1));double hover=a.player.getDeltaMovement().z;
            a.require(hover>.02729&&hover<.02731,"actual native boots air.03 branch queries the real funded Harness controller");Hover.forget(a.player);
            a.player.getInventory().clearContent();a.player.getAbilities().flying=false;
            a.put(a.root,Blocks.WATER.defaultBlockState());a.player.setPos(a.root.getX()+.5,a.root.getY(),a.root.getZ()+.5);a.player.baseTick();a.require(a.player.isInWater(),"declared physical water actually surrounds the native actor");
            a.player.setItemSlot(EquipmentSlot.FEET,new ItemStack(item));a.player.zza=1;a.player.setOnGround(true);a.player.setDeltaMovement(Vec3.ZERO);TravelGearTick.tick(a.player);
            a.require(a.player.getDeltaMovement().z==(double)(.055F/4F),"actual water state quarters the original boots ground acceleration");
        }test.succeed();
    }
    public static void harnessFuelAndPermissions(GameTestHelper test) {
        try(var a=actor(test)) {
            a.clearLane();equip(a,harness(2),6);ItemStack armor=a.player.getItemBySlot(EquipmentSlot.CHEST),originalJar=HarnessData.jar(armor);
            var recipe=recipe(a,"hover_harness");a.require(recipe.research().equals("HOVERHARNESS")&&recipe.instability()==6&&recipe.input().test(new ItemStack(Items.LEATHER_CHESTPLATE))
                            &&recipe.inputDamage().orElse(-1)==0&&recipe.aspects().equals(Map.of("volatus",32,"potentia",32,"machina",32,"iter",16))&&recipe.components().size()==9
                            &&recipe.components().get(2).test(new ItemStack(ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem()))&&recipe.components().get(3).test(new ItemStack(ThaumcraftNaturals.GREATWOOD_PLANKS.get().asItem()))
                            &&recipe.result().is(ThaumcraftTravelGear.HOVER_HARNESS.get()),"real Harness infusion retains original pristine center,9components, duplicate greatwood planks, exact costs/research/output");
            var item=ThaumcraftTravelGear.HOVER_HARNESS.get();a.require(armor.getMaxDamage()==400&&armor.getRarity()==Rarity.EPIC&&item.getDefense()==3&&item.getEnchantmentValue()==25
                            &&item.getVisDiscount(armor,a.player,Aspect.AIR)==5&&item.getVisDiscount(armor,a.player,Aspect.FIRE)==2&&item.isValidRepairItem(armor,new ItemStack(Items.GOLD_INGOT))
                            &&!item.isValidRepairItem(armor,new ItemStack(Items.LEATHER))&&RunicArmor.finalCharge(armor)==0,"real SPECIAL Harness has400durability/3armor/25enchant/epic, Aer5/other2discount,gold repair,runic0");
            var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),a.level.registryAccess());try {wire.writeInt(a.player.getId());wire.writeBoolean(true);boolean rejected=false;
                try{HoverTogglePayload.STREAM_CODEC.decode(wire);}catch(io.netty.handler.codec.DecoderException expected){rejected=true;}a.require(rejected,"old arbitrary player-id/flight boolean cannot enter the native empty toggle-intent codec");}finally{wire.release();}
            var floats=(ServerGamePacketListenerFloatAccess)a.player.connection;floats.thaumcraft$setAboveGroundTickCount(79);a.player.fallDistance=8;
            a.require(Hover.toggleHover(a.player),"actual sender toggles its actual funded worn harness");nativeStep(a);
            a.require(a.player.getAbilities().mayfly&&a.player.getAbilities().flying&&HarnessData.charge(armor)==1&&HarnessData.fuel(armor)==2&&a.player.fallDistance==0&&floats.thaumcraft$getAboveGroundTickCount()==0,
                    "one actual native living step pays one counter, grants flight and resets its real float/fall counters once");
            for(int i=1;i<360;i++)nativeStep(a);a.require(HarnessData.charge(armor)==360&&HarnessData.fuel(armor)==2,"360actual native living steps do not debit a unit before the original boundary");
            nativeStep(a);a.require(HarnessData.charge(armor)==0&&HarnessData.fuel(armor)==1&&Hover.getHover(a.player),"361stactual step pays precisely one physical Potentia and continues on its second unit");
            a.require(Hover.toggleHover(a.player),"actual paid flight can toggle off");nativeStep(a);a.require(!a.player.getAbilities().mayfly&&!a.player.getAbilities().flying&&HarnessData.fuel(armor)==1,"deactivation restores native survival rights without consuming idle fuel");
            a.require(Hover.toggleHover(a.player),"remaining unit permits reactivation");for(int i=0;i<360;i++)nativeStep(a);nativeStep(a);
            a.require(HarnessData.fuel(armor)==0&&HarnessData.charge(armor)==0&&!Hover.getHover(a.player)&&a.player.getAbilities().flying
                            &&ItemStack.matches(HarnessData.jar(armor),ItemJar.withContents(originalJar,Aspect.ENERGY,0)),"last actual unit preserves its complete named/labelled empty jar and the original one paid closing flight frame");
            nativeStep(a);a.require(!a.player.getAbilities().flying&&!a.player.getAbilities().mayfly&&!Hover.toggleHover(a.player),"following native frame revokes the temporary grant and an empty actual jar cannot restart flight");
            Hover.forget(a.player);a.player.setItemSlot(EquipmentSlot.CHEST,harness(2));armor=a.player.getItemBySlot(EquipmentSlot.CHEST);PlayerAccessories.get(a.player).setItem(3,new ItemStack(ThaumcraftBaubles.HOVER_GIRDLE.get()));
            a.require(Hover.toggleHover(a.player),"real worn native Girdle/Harness pair starts");for(int i=0;i<288;i++)nativeStep(a);
            a.require(HarnessData.charge(armor)==288&&HarnessData.fuel(armor)==2,"real belt identity gives the original288 successful counter steps");nativeStep(a);
            a.require(HarnessData.charge(armor)==0&&HarnessData.fuel(armor)==1,"289thstep with actual Girdle consumes exactly one unit");
            armor.set(ThaumcraftTravelGear.HOVER_CHARGE.get(),173);var saved=a.player.saveWithoutId(new CompoundTag());
            a.require(saved.contains("abilities",10)&&!saved.getCompound("abilities").getBoolean("mayfly")&&!saved.getCompound("abilities").getBoolean("flying")&&a.player.getAbilities().mayfly&&a.player.getAbilities().flying,
                    "actual player save strips only the temporary output-NBT permission while preserving the live paid flight frame");
            ItemStack gearBefore=armor.copy();Hover.forget(a.player);a.player.load(saved);armor=a.player.getItemBySlot(EquipmentSlot.CHEST);
            a.require(ItemStack.matches(armor,gearBefore)&&HarnessData.charge(armor)==173&&!a.player.getAbilities().mayfly,"actual native reload retains physical gear/jar/counter/mode but cannot persist free flight rights");nativeStep(a);
            a.require(HarnessData.charge(armor)==174&&a.player.getAbilities().flying,"saved hover mode reacquires flight through one newly paid real worn tick after reload");
            click(a,6,0,ClickType.PICKUP);click(a,9,0,ClickType.PICKUP);nativeStep(a);a.require(!Hover.getHover(a.player)&&!a.player.getAbilities().mayfly&&!a.player.getAbilities().flying,
                    "native unequip removes only the gear-owned permission and cannot keep flight alive from an inventory harness");
            ItemStack detached=a.player.getInventory().getItem(9),before=detached.copy();a.require(!Hover.toggleHover(a.player)&&ItemStack.matches(detached,before),"detached actual inventory fuel cannot authorize another activation");
            Hover.forget(a.player);a.player.setItemSlot(EquipmentSlot.CHEST,harness(2));a.player.getAbilities().mayfly=true;a.player.getAbilities().flying=true;
            a.require(Hover.toggleHover(a.player),"declared preexisting intrinsic flight can wear the same real harness");nativeStep(a);a.require(Hover.toggleHover(a.player),"intrinsic flight test turns only the harness mode off");nativeStep(a);
            a.require(a.player.getAbilities().mayfly&&a.player.getAbilities().flying,"deactivation does not revoke flight that existed before this equipment's grant");
            Hover.forget(a.player);a.player.getAbilities().mayfly=false;a.player.getAbilities().flying=false;a.player.setItemSlot(EquipmentSlot.CHEST,harness(2));a.player.setGameMode(GameType.CREATIVE);
            a.require(Hover.toggleHover(a.player),"real creative actor can toggle the original worn mode");nativeStep(a);
            a.require(HarnessData.fuel(a.player.getItemBySlot(EquipmentSlot.CHEST))==2&&HarnessData.charge(a.player.getItemBySlot(EquipmentSlot.CHEST))==0&&a.player.getAbilities().mayfly,
                    "original creative armor-tick exclusion spends no material and preserves native creative permissions");
            a.player.setGameMode(GameType.SURVIVAL);nativeStep(a);a.require(HarnessData.charge(a.player.getItemBySlot(EquipmentSlot.CHEST))==1&&a.player.getAbilities().flying,"survival transition starts paying the actual preserved mode again");
            a.player.setGameMode(GameType.SPECTATOR);nativeStep(a);a.require(a.player.getAbilities().mayfly&&a.player.getAbilities().flying,"retiring a survival grant preserves newly intrinsic spectator rights");Hover.forget(a.player);
        }test.succeed();
    }
    /**
     * Original Hover: a hovering player mines as fast as on the ground, the native airborne fifth is
     * undone. Measured through the native break progress, which NeoForge computes through its own
     * {@code getDigSpeed} and the break speed event rather than {@code Player.getDestroySpeed}.
     */
    public static void harnessMining(GameTestHelper test) {
        try(var a=actor(test)) {
            var stone=Blocks.STONE.defaultBlockState();
            equip(a,harness(2),6);
            a.player.setOnGround(true);float ground=stone.getDestroyProgress(a.player,a.level,a.root);
            a.player.setOnGround(false);float air=stone.getDestroyProgress(a.player,a.level,a.root);
            a.require(ground>0&&Math.abs(air-ground/5F)<1e-7F,"without hover the native airborne player mines at a fifth: "+ground+" "+air);
            a.require(Hover.toggleHover(a.player)&&Hover.getHover(a.player),"the funded worn harness hovers");
            a.player.setOnGround(false);float hover=stone.getDestroyProgress(a.player,a.level,a.root);
            a.require(Math.abs(hover-ground)<1e-7F,"a hovering player mines at the ground speed of the original: "+ground+" "+hover);
            a.player.setOnGround(true);
            a.require(stone.getDestroyProgress(a.player,a.level,a.root)==ground,"on the ground the hover changes nothing");
            Hover.forget(a.player);
        }test.succeed();
    }
    public static void harnessMenu(GameTestHelper test) {
        try(var a=actor(test)) {
            a.clearLane();ItemStack armor=new ItemStack(ThaumcraftTravelGear.HOVER_HARNESS.get()),fuel=jar(3);a.player.setItemInHand(InteractionHand.MAIN_HAND,armor);a.player.getInventory().setItem(9,fuel.copy());
            a.player.getInventory().setItem(10,ItemJar.withContents(new ItemStack(ThaumcraftContent.JAR.get()),Aspect.EARTH,2));
            var menu=open(a,InteractionHand.MAIN_HAND);a.require(menu.slots.size()==37&&menu.lockedSlot()==28&&a.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"native Harness RMB opens1+36 fuel menu rather than automatically equipping it");
            click(a,1,0,ClickType.QUICK_MOVE);a.require(a.player.getInventory().getItem(9).isEmpty()&&ItemStack.matches(HarnessData.jar(armor),fuel),"native jar quick-move pays one actual jar into immediate physical embedded storage");
            click(a,28,0,ClickType.PICKUP);click(a,0,0,ClickType.SWAP);a.require(a.player.getMainHandItem()==armor&&ItemStack.matches(HarnessData.jar(armor),fuel)&&menu.getCarried().isEmpty(),"native source-slot and number-key locks keep the open harness in its actual hand");
            click(a,0,0,ClickType.QUICK_MOVE);a.require(HarnessData.jar(armor).isEmpty()&&ItemStack.matches(a.player.getInventory().getItem(8),fuel),"jar removal uses original reverse inventory transfer order");
            click(a,2,0,ClickType.PICKUP);click(a,0,0,ClickType.PICKUP);a.require(ItemJar.getAspect(menu.getCarried())==Aspect.EARTH&&HarnessData.jar(armor).isEmpty(),"native cursor insertion refuses a real non-Potentia jar without spending it");click(a,2,0,ClickType.PICKUP);click(a,36,0,ClickType.QUICK_MOVE);
            armor.set(ThaumcraftTravelGear.HOVER_CHARGE.get(),17);menu.clicked(0,0,ClickType.PICKUP,a.player);a.player.closeContainer();
            a.require(menu.getCarried().isEmpty()&&ItemStack.matches(HarnessData.jar(armor),fuel)&&HarnessData.charge(armor)==17,"external full-component change invalidates stale fuel mirror and close cannot overwrite the counter or duplicate a jar");
            a.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);a.player.setItemInHand(InteractionHand.OFF_HAND,armor);menu=open(a,InteractionHand.OFF_HAND);click(a,0,40,ClickType.SWAP);
            a.require(a.player.getOffhandItem()==armor&&ItemStack.matches(HarnessData.jar(armor),fuel),"offhand40 native swap cannot detach the live fuel-menu source");click(a,0,0,ClickType.PICKUP);a.player.closeContainer();
            a.require(HarnessData.jar(armor).isEmpty()&&a.player.getInventory().items.stream().filter(s->ItemStack.matches(s,fuel)).count()==1,"ordinary native withdrawal/close returns one full component-bearing jar exactly once");
            HarnessData.setJar(armor,new ItemStack(Items.STICK,64));menu=open(a,InteractionHand.OFF_HAND);var opening=new FriendlyByteBuf(Unpooled.buffer());var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),a.level.registryAccess());
            try {ItemStack.STREAM_CODEC.encode(wire,armor);var decoded=ItemStack.STREAM_CODEC.decode(wire);opening.writeBoolean(true);var mirror=new ContainerHoverHarness(menu.containerId,a.player.getInventory(),opening);mirror.getSlot(0).set(HarnessData.jar(decoded));
                a.require(mirror.getSlot(0).getItem().getCount()==64&&mirror.getSlot(0).getMaxStackSize()==1&&!mirror.getSlot(0).mayPlace(new ItemStack(Items.STICK)),"actual native item stream/client Slot.set retains foreign64 without weakening one-jar insertion");}
            finally{opening.release();wire.release();}
            click(a,0,0,ClickType.QUICK_MOVE);a.require(HarnessData.jar(armor).getCount()==64,"original invalid saved fuel entry cannot quick-move");click(a,0,0,ClickType.PICKUP);
            a.require(menu.getCarried().is(Items.STICK)&&menu.getCarried().getCount()==64&&HarnessData.jar(armor).isEmpty(),"ordinary native withdrawal recovers the whole retained foreign stack");a.player.closeContainer();
            armor.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(jar(1),jar(1))));ItemStack before=armor.copy();
            a.require(!a.player.gameMode.useItem(a.player,a.level,armor,InteractionHand.OFF_HAND).consumesAction()&&ItemStack.matches(armor,before)&&a.player.containerMenu==a.player.inventoryMenu,
                    "oversized physical jar payload refuses activation intact rather than deleting extra fuel");a.require(a.items().isEmpty(),"native harness menu operations produce no duplicate item drops");
        }test.succeed();
    }
    private static ContainerHoverHarness open(FocusUpgradeRuntimeChecks.Actor a,InteractionHand hand) {
        a.require(a.player.gameMode.useItem(a.player,a.level,a.player.getItemInHand(hand),hand).consumesAction()&&a.player.containerMenu instanceof ContainerHoverHarness,"actual native held-harness RMB opens the registered fuel menu");return (ContainerHoverHarness)a.player.containerMenu;
    }
}

package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftAdvancedFurnace;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.AdvancedFurnaceFormation;
import thaumcraft.common.lib.AdvancedFurnaceRules;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedNozzle;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvancedPart;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.api.nodes.NodeType;

/** Real recipes/menu, paid17-cell formation, renewable source epochs, dropped input and all four shared output ports.
 * Raw materials, late crafting wand, ordinary node capacities and physical pearls are explicit SERVER INPUT fixtures. */
public final class AdvancedFurnaceRuntimeChecks {
    private AdvancedFurnaceRuntimeChecks() {}
    public static void worldScheduled(GameTestHelper test) {
        var f = new Fixture(test);
        DeviceWorldRuntimeChecks.cleanup(test, f::close);
        f.open(); rememberBox(f,f.base.offset(-8,-3,-3),f.base.offset(8,3,3));
        var core = form(f,f.base,false);
        NodeTransductionRuntimeChecks.energize(f,f.base.west(6),NodeTransductionRuntimeChecks.primals(2500),null,NodeType.NORMAL);
        test.runAfterDelay(300, () -> {
            f.check(core.getHeat() >= 8 && core.getPower1() >= 4 && core.getPower2() >= 4,
                    "formed world advanced furnace actually draws native source power without manual machine ticks");
            int heat=core.getHeat(), entropy=core.getPower1(), water=core.getPower2();
            var item=physical(f,f.base,new ItemStack(Items.COAL));
            // Native collision entry on an actual registered built multiblock and actual physical item.
            f.level.getBlockState(f.base).entityInside(f.level,f.base,item);
            f.check(item.isRemoved() && core.getVis()==4 && core.getHeat()==heat-8
                            && core.getPower1()==entropy-4 && core.getPower2()==water-4,
                    "actual collision pays coal1+ignis8+perditio4+aqua4 for essentia ignis2+potentia2");
            var nozzle=(TileAlchemyFurnaceAdvancedNozzle)f.level.getBlockEntity(f.base.east());
            f.check(nozzle.takeEssentia(Aspect.FIRE,2,Direction.EAST)==2
                            && nozzle.takeEssentia(Aspect.ENERGY,2,Direction.EAST)==2 && core.getVis()==0,
                    "all four generated units are actually recoverable through registered output nozzle");
            f.close(); test.succeed();
        });
    }
    public static void run(GameTestHelper test) {
        var forced=GolemChunks.hold(test.getLevel(),List.of(test.absolutePos(new BlockPos(-9,3,-3)),test.absolutePos(new BlockPos(12,3,9))));
        try(var f=new Fixture(test)) {
            f.open(); rememberBox(f,f.base.offset(-8,-3,-3),f.base.offset(8,3,3));
            recipe(f); f.reset();
            var core=form(f,f.base,true);
            var upper=(ChestBlockEntity)f.level.getBlockEntity(f.base.above()); var upperBefore=upper.saveWithoutMetadata(f.level.registryAccess());
            var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(6),NodeTransductionRuntimeChecks.primals(2500),null,NodeType.NORMAL);
            warm(f,core,source,50);
            f.check(core.getHeat()==550 && core.getPower1()==550 && core.getPower2()==550 && core.getVis()==0
                            && core.getBlockState().getValue(BlockAdvancedAlchemyFurnace.LIGHT)==13,
                    "actual native three50-CV draws overshoot to550; heat idle decay is real and original550 light is13");
            boolean denied=false; try { core.acceptPreparedPowerGrant(Aspect.FIRE,1); } catch(IllegalStateException expected) { denied=true; }
            f.check(denied && core.getHeat()==550,"public raw-power entry cannot forge an unpaid native grant");
            f.put(f.base.below(),Blocks.REDSTONE_BLOCK);
            var sample=new ItemStack(Items.DIAMOND_SWORD); sample.set(DataComponents.CUSTOM_NAME,Component.literal("actual-full-object-tags"));
            var item=physical(f,core.getBlockPos(),sample.copyWithCount(3)); var tags=ObjectAspectResolver.get(sample,f.level); int size=tags.visSize();
            f.level.getBlockState(f.base).entityInside(f.level,f.base,item);
            f.check(item.getItem().getCount()==2 && core.getAspects().aspects.equals(tags.aspects) && core.getHeat()==550-size*2
                            && core.getPower1()==550-size && core.getPower2()==550-size && core.getProcessed()==AdvancedFurnaceRules.cooldown(550-size*2),
                    "real registered core collision consumes ONE physical input, adds full resolver base+bonus tags and exact2a/a/a before post-debit cooldown");
            var paid=core.saveWithoutMetadata(f.level.registryAccess()); f.level.getBlockState(f.base).entityInside(f.level,f.base,item);
            f.check(item.getItem().getCount()==2 && paid.equals(core.saveWithoutMetadata(f.level.registryAccess())),"processing cooldown refuses a second real collision without any debit/credit");
            portsAndPersistence(f,core,tags);
            f.check(f.level.getBlockEntity(f.base.above())==upper && upper.saveWithoutMetadata(f.level.registryAccess()).equals(upperBefore),
                    "unconstrained upper-center physical chest BE and complete inventory/components survive formation, operation and native reload");
            item.discard(); f.reset();
            capacityAndEnergy(f); f.reset();
            negativeHeatAndRestart(f); f.reset();
            dismantle(f);
            standaloneCreative(f);
        } finally { for(var chunk:forced) test.getLevel().setChunkForced(chunk.x,chunk.z,false); }
        test.succeed();
    }
    private static void recipe(Fixture f) {
        f.put(f.base,ThaumcraftArcane.ARCANE_WORKBENCH.get()); f.aim(Vec3.atCenterOf(f.base)); f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench,"native workbench menu opens");
        var table=(TileArcaneWorkbench)f.level.getBlockEntity(f.base); var menu=(ContainerArcaneWorkbench)f.player.containerMenu;
        var definition=(ArcaneShapedRecipe)f.level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","arcane/advanced_alchemical_construct")).orElseThrow().value();
        f.check(definition.research().equals("ADVALCHEMYFURNACE") && definition.vis().equals(Map.of("aqua",10,"ordo",30,"terra",10))
                        && definition.getResultItem(f.level.registryAccess()).getCount()==4,"original advanced construct recipe gate,three costs and FOUR output");
        ItemStack pearl=new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get()); pearl.set(DataComponents.CUSTOM_NAME,Component.literal("exact-retained-center-pearl"));
        for(int slot=0;slot<9;slot++) table.setItem(slot,slot==4?pearl:new ItemStack(slot%2==0?ThaumcraftContent.MATERIALS.get("void_ingot").get():ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT_ITEM.get(),2));
        var payer=wand(); table.setItem(TileArcaneWorkbench.WAND_SLOT,payer);
        KnowledgeAccess.get(f.player).removeResearch("ADVALCHEMYFURNACE"); menu.broadcastChanges();
        f.check(menu.getSlot(0).getItem().isEmpty(),"missing actual actor research exposes no craftable advanced construct");
        KnowledgeAccess.get(f.player).addResearch("ADVALCHEMYFURNACE");
        var poor=wand(); poor.set(ThaumcraftContent.WAND_DATA.get(),ItemWandCasting.data(poor).withAmount("ordo",2399)); table.setItem(TileArcaneWorkbench.WAND_SLOT,poor); menu.broadcastChanges();
        var unchanged=table.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0,0,ClickType.PICKUP,f.player);
        f.check(menu.getCarried().isEmpty() && table.saveWithoutMetadata(f.level.registryAccess()).equals(unchanged),"one centivis short refuses output without ingredients, retained catalyst or wand debit");
        table.setItem(TileArcaneWorkbench.WAND_SLOT,payer); menu.broadcastChanges(); menu.clicked(0,0,ClickType.PICKUP,f.player);
        f.check(menu.getCarried().is(ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT_ITEM.get()) && menu.getCarried().getCount()==4
                        && table.getItem(4)==pearl && pearl.getCount()==1 && pearl.getHoverName().getString().equals("exact-retained-center-pearl")
                        && f.player.getInventory().countItem(ThaumcraftContent.PRIMORDIAL_PEARL.get())==0,
                "one paid four-output craft retains SAME physical center pearl/count/components in matrix, never an inventory-first/general remainder");
        menu.setCarried(ItemStack.EMPTY); menu.clicked(0,0,ClickType.QUICK_MOVE,f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftAdvancedFurnace.ADVANCED_CONSTRUCT_ITEM.get())==4 && table.getItem(4)==pearl && pearl.getCount()==1
                        && ItemWandCasting.getVis(table.getItem(TileArcaneWorkbench.WAND_SLOT),"ordo")==5200
                        && ItemWandCasting.getVis(table.getItem(TileArcaneWorkbench.WAND_SLOT),"aqua")==8400
                        && ItemWandCasting.getVis(table.getItem(TileArcaneWorkbench.WAND_SLOT),"terra")==8400,
                "native quick-move pays second exact craft only; same original pearl is retained and exact void-cap vis costs paid twice");
        f.player.closeContainer(); f.level.setBlock(f.base,Blocks.AIR.defaultBlockState(),TubeVariantsRuntimeChecks.QUIET); f.flushFresh(); f.drops();
    }
    static ItemStack wand() {
        var values=new LinkedHashMap<String,Integer>(); for(String tag:WandData.PRIMALS) values.put(tag,10000);
        var wand=new ItemStack(ThaumcraftContent.WAND.get()); wand.set(ThaumcraftContent.WAND_DATA.get(),new WandData("silverwood","void",values)); return wand;
    }
    static void rememberBox(Fixture f,BlockPos from,BlockPos to) {
        for(BlockPos mutable:BlockPos.betweenClosed(from,to)) { var pos=mutable.immutable(); f.states.putIfAbsent(pos,f.level.getBlockState(pos)); if(f.level.getBlockEntity(pos)!=null) f.originals.putIfAbsent(pos,f.level.getBlockEntity(pos)); }
    }
    static TileAlchemyFurnaceAdvanced form(Fixture f,BlockPos center,boolean occupiedInputs) {
        rememberBox(f,center.offset(-2,-2,-2),center.offset(2,3,2));
        for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
            int part=AdvancedFurnaceRules.part(x,y,z); if(part>=0) f.put(center.offset(x,y,z),AdvancedFurnaceFormation.original(part).getBlock());
        }
        f.put(center.above(),Blocks.CHEST); var upper=(ChestBlockEntity)f.level.getBlockEntity(center.above());
        ItemStack marker=new ItemStack(Items.DIAMOND,3); marker.set(DataComponents.CUSTOM_NAME,Component.literal("untouched-upper-center")); upper.setItem(0,marker);
        var upperTag=upper.saveWithoutMetadata(f.level.registryAccess());
        if(occupiedInputs) {
            var normal=(TileAlchemyFurnace)f.level.getBlockEntity(center); normal.setItem(0,new ItemStack(Items.COAL,3)); normal.setItem(1,new ItemStack(Items.STICK,7));
            var alembic=(TileAlembic)f.level.getBlockEntity(center.offset(1,1,1)); alembic.addToContainer(Aspect.FIRE,5); alembic.setFilter(Aspect.FIRE);
        }
        ItemStack wand=wand(); f.player.setItemInHand(InteractionHand.MAIN_HAND,wand); f.aim(Vec3.atCenterOf(center.west()));
        KnowledgeAccess.get(f.player).addResearch("ADVALCHEMYFURNACE");
        BlockPos earlier=center.offset(-2,0,-1); f.put(earlier,ThaumcraftDistillation.ALCHEMY_FURNACE.get());
        var before=ItemWandCasting.data(wand);
        f.check(!f.use(center.west()).consumesAction() && ItemWandCasting.data(wand).equals(before),"FIRST earlier invalid normal-furnace candidate terminates search without consuming vis");
        f.level.setBlock(earlier,Blocks.AIR.defaultBlockState(),TubeVariantsRuntimeChecks.QUIET);
        f.check(f.use(center.west()).consumesAction() && f.level.getBlockEntity(center) instanceof TileAlchemyFurnaceAdvanced,"actual native wand trigger pays and forms registered17-piece machine");
        var core=(TileAlchemyFurnaceAdvanced)f.level.getBlockEntity(center);
        f.check(AdvancedFurnaceFormation.structure(core)==AdvancedFurnaceFormation.Status.COMPLETE
                        && ItemWandCasting.getVis(wand,"ignis")==6000 && ItemWandCasting.getVis(wand,"aqua")==6000 && ItemWandCasting.getVis(wand,"ordo")==6000
                        && f.level.getBlockEntity(center.above())==upper && upper.saveWithoutMetadata(f.level.registryAccess()).equals(upperTag),
                "exact three50-whole-vis void-cap prices paid once;17 native owner-linked parts valid; upper center original chest stays exact");
        for(Direction face:Direction.Plane.HORIZONTAL) { var nozzle=(TileAlchemyFurnaceAdvancedNozzle)f.level.getBlockEntity(center.relative(face)); TileAlchemyFurnaceAdvancedNozzle.tick(f.level,nozzle.getBlockPos(),nozzle.getBlockState(),nozzle); }
        if(occupiedInputs) {
            f.flushFresh(); var drops=f.drops();
            f.check(drops.stream().filter(drop->drop.getItem().is(Items.COAL)).mapToInt(drop->drop.getItem().getCount()).sum()==3
                            && drops.stream().filter(drop->drop.getItem().is(Items.STICK)).mapToInt(drop->drop.getItem().getCount()).sum()==7
                            && drops.stream().filter(drop->drop.getItem().is(ThaumcraftContent.MATERIALS.get("jar_label").get())).mapToInt(drop->drop.getItem().getCount()).sum()==1
                            && core.getVis()==0,
                    "original destructive nonempty conversion accepts occupied devices; native removal returns physical furnace input/fuel and alembic label once, never copies old slurry");
        }
        return core;
    }
    static void ticks(Fixture f,TileAlchemyFurnaceAdvanced core,int count) { for(int tick=0;tick<count;tick++) TileAlchemyFurnaceAdvanced.tick(f.level,core.getBlockPos(),core.getBlockState(),core); }
    static void warm(Fixture f,TileAlchemyFurnaceAdvanced core,TileNodeEnergized source,int cycles) { for(int cycle=0;cycle<cycles;cycle++) { NodeTransductionRuntimeChecks.sourceTick(f,source); ticks(f,core,5); } }
    static ItemEntity physical(Fixture f,BlockPos pos,ItemStack stack) {
        var item=new ItemEntity(f.level,pos.getX()+.5,pos.getY()+.75,pos.getZ()+.5,stack); item.setNoGravity(true); item.setPickUpDelay(1000);
        f.check(f.level.addFreshEntity(item),"declared actual physical input entity spawns natively"); f.ownedDrops.add(item.getUUID()); return item;
    }
    private static void portsAndPersistence(Fixture f,TileAlchemyFurnaceAdvanced core,AspectList original) {
        var ports=new ArrayList<TileAlchemyFurnaceAdvancedNozzle>();
        for(Direction face:Direction.Plane.HORIZONTAL) {
            var nozzle=(TileAlchemyFurnaceAdvancedNozzle)f.level.getBlockEntity(core.getBlockPos().relative(face)); ports.add(nozzle);
            f.check(nozzle.getFacing()==face && nozzle.canOutputTo(face) && !nozzle.canOutputTo(face.getOpposite()) && !nozzle.canInputFrom(face)
                            && nozzle.getSuctionAmount(face)==0 && nozzle.getMinimumSuction()==0 && !nozzle.renderExtendedTube(),"original one outward output/zero suction port: "+face);
        }
        var copy=ports.getFirst().getAspects(); copy.add(Aspect.FIRE,999);
        f.check(core.getAspects().aspects.equals(original.aspects) && !ports.getFirst().takeFromContainer(Aspect.FIRE,-1),"defensive nozzle snapshot and negative extraction cannot mint shared slurry");
        var remaining=original.copy();
        for(var nozzle:ports) {
            Aspect first=remaining.size()==0?null:remaining.getAspects()[0];
            if(first==null) break;
            f.check(nozzle.getEssentiaType(null)==first && nozzle.getEssentiaAmount(null)==remaining.getAmount(first)
                            && nozzle.takeEssentia(first,1,nozzle.getFacing())==1,"each of four native nozzles debits the SAME current first-insertion aspect pool");
            remaining.remove(first,1); f.check(core.getAspects().aspects.equals(remaining.aspects),"exact one-unit shared pool conservation across nozzles");
        }
        var saved=core.saveWithFullMetadata(f.level.registryAccess()); var old=core;
        f.level.removeBlockEntity(core.getBlockPos()); core=(TileAlchemyFurnaceAdvanced)BlockEntity.loadStatic(core.getBlockPos(),core.getBlockState(),saved,f.level.registryAccess()); f.level.setBlockEntity(core);
        f.check(core!=old && core.getAspects().aspects.equals(remaining.aspects) && core.getHeat()==old.getHeat() && core.getPower1()==old.getPower1() && core.getPower2()==old.getPower2()
                        && core.machineId().equals(old.machineId()) && core.getProcessed()==0 && core.getTickCount()==0,
                "native NBT replacement retains exact shared stock/three SHORT pools/owner while original cooldown/count remain transient");
        f.check(!ports.getFirst().takeFromContainer(remaining.size()>0?remaining.getAspects()[0]:Aspect.FIRE,1),"cached retired core cannot be drained through an old nozzle after native replacement");
        var unbound=new TileAlchemyFurnaceAdvancedNozzle(core.getBlockPos().east(),core.getBlockState().setValue(BlockAdvancedAlchemyFurnace.PART,1));
        f.check(unbound.getEssentiaAmount(null)==0 && unbound.getEssentiaType(null)==null,"original unbound int-unboxing NPE is bounded to safe zero/null without fabricated stock");
    }
    private static void capacityAndEnergy(Fixture f) {
        var core=form(f,f.base,false); var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(6),NodeTransductionRuntimeChecks.primals(2500),null,NodeType.NORMAL);
        var missing=physical(f,f.base,new ItemStack(Items.COAL)); var before=core.saveWithoutMetadata(f.level.registryAccess());
        f.level.getBlockState(f.base).entityInside(f.level,f.base,missing);
        f.check(missing.getItem().getCount()==1 && before.equals(core.saveWithoutMetadata(f.level.registryAccess())),"real input with missing three energy pools is refused unchanged"); missing.discard();
        for(int input=0;input<5;input++) { warm(f,core,source,50); var pearl=physical(f,f.base,new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get())); f.level.getBlockState(f.base).entityInside(f.level,f.base,pearl); f.check(pearl.isRemoved() && core.getVis()==96*(input+1),"physical96-essentia pearl processed once into mixed capacity: "+input); ticks(f,core,core.getProcessed()); }
        warm(f,core,source,50); var excess=physical(f,f.base,new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get())); before=core.saveWithoutMetadata(f.level.registryAccess());
        f.check(core.getHeat()>=192 && core.getPower1()>=96 && core.getPower2()>=96,"capacity-only refusal fixture has sufficient REAL renewable energy");
        f.level.getBlockState(f.base).entityInside(f.level,f.base,excess);
        f.check(!excess.isRemoved() && excess.getItem().getCount()==1 && before.equals(core.saveWithoutMetadata(f.level.registryAccess())),"480+96 exceeds500: no sixth physical debit/energy debit/partial slurry credit"); excess.discard();
    }
    private static void negativeHeatAndRestart(Fixture f) {
        var core=form(f,f.base,false); ticks(f,core,5);
        f.check(core.getHeat()==-1 && core.getPower1()==0 && core.getPower2()==0,"unpowered original fifth tick heat decays BELOW zero while other pools do not decay");
        var saved=core.saveWithFullMetadata(f.level.registryAccess()); f.check(saved.getShort("heat")==-1,"original negative heat is a native signed SHORT");
        saved.putShort("heat",Short.MIN_VALUE); // Explicit original short-boundary storage INPUT; not a claim of elapsed nine-hour idle gameplay.
        f.level.removeBlockEntity(f.base); core=(TileAlchemyFurnaceAdvanced)BlockEntity.loadStatic(f.base,core.getBlockState(),saved,f.level.registryAccess()); f.level.setBlockEntity(core); ticks(f,core,5);
        f.check(core.getHeat()==Short.MIN_VALUE-1 && core.saveWithoutMetadata(f.level.registryAccess()).getShort("heat")==Short.MAX_VALUE,
                "original unbounded live negative heat and signed-short save wrap are preserved, not silently clamped");
    }
    private static void standaloneCreative(Fixture f) {
        f.reset(); f.mode(net.minecraft.world.level.GameType.CREATIVE);
        var gift=new ItemStack(ThaumcraftAdvancedFurnace.FURNACE_ITEM.get()); f.player.setItemInHand(InteractionHand.MAIN_HAND,gift); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.level.getBlockEntity(f.base) instanceof TileAlchemyFurnaceAdvanced,
                "original creative metadata0 item places ONE real working core through native BlockItem/game-mode placement");
        var core=(TileAlchemyFurnaceAdvanced)f.level.getBlockEntity(f.base); int formed=0;
        for(BlockPos pos:BlockPos.betweenClosed(f.base.offset(-1,0,-1),f.base.offset(1,1,1))) if(f.level.getBlockState(pos).is(ThaumcraftAdvancedFurnace.FURNACE.get())) formed++;
        f.check(core.isStandaloneCreative() && AdvancedFurnaceFormation.structure(core)==AdvancedFurnaceFormation.Status.COMPLETE && formed==1
                        && gift.getCount()==1 && f.player.getMainHandItem()==gift,
                "persisted standaloneCreative path operates with only native core; never auto-spawns17 components and preserves ordinary creative item counts");
        var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(6),NodeTransductionRuntimeChecks.primals(2500),null,NodeType.NORMAL);
        warm(f,core,source,50); var input=physical(f,f.base,new ItemStack(Items.COAL,2));
        f.level.getBlockState(f.base).entityInside(f.level,f.base,input);
        f.check(core.getVis()==ObjectAspectResolver.get(new ItemStack(Items.COAL),f.level).visSize() && input.getItem().getCount()==1,
                "lone original creative core draws real50-CV power and consumes one real item into slurry, not an inert display/catalogue block"); input.discard();
        var saved=core.saveWithFullMetadata(f.level.registryAccess()); f.level.removeBlockEntity(f.base);
        core=(TileAlchemyFurnaceAdvanced)BlockEntity.loadStatic(f.base,core.getBlockState(),saved,f.level.registryAccess()); f.level.setBlockEntity(core);
        f.check(core.isStandaloneCreative() && AdvancedFurnaceFormation.structure(core)==AdvancedFurnaceFormation.Status.COMPLETE && core.getVis()>0,
                "native encode/decode preserves standalone mode, exact stock and real operation independently of17-part survival formation");
        f.mode(net.minecraft.world.level.GameType.SURVIVAL); f.level.destroyBlock(f.base,true);
        f.check(f.drops().stream().filter(drop->drop.getItem().is(ThaumcraftDistillation.ALCHEMY_FURNACE_ITEM.get())).mapToInt(drop->drop.getItem().getCount()).sum()==1
                        && f.drops().stream().noneMatch(drop->drop.getItem().is(ThaumcraftAdvancedFurnace.FURNACE_ITEM.get())),
                "breaking standalone core returns original NORMAL furnace once, never a reusable charged core item or copied slurry");
        var inherited=new ItemStack(ThaumcraftAdvancedFurnace.FURNACE_ITEM.get()); f.player.setItemInHand(InteractionHand.MAIN_HAND,inherited); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && inherited.isEmpty() && ((TileAlchemyFurnaceAdvanced)f.level.getBlockEntity(f.base)).isStandaloneCreative(),
                "a real creative-sourced core item handed to survival still follows original native one-item placement consumption; no invented creative-only permission gate");
        f.reset();
    }
    private static void dismantle(Fixture f) {
        for(BlockPos offset:List.of(new BlockPos(1,0,0),new BlockPos(1,0,1),new BlockPos(1,1,1),new BlockPos(1,1,0),BlockPos.ZERO)) {
            var core=form(f,f.base,false); var upper=f.level.getBlockEntity(f.base.above());
            f.check(f.level.destroyBlock(f.base.offset(offset),true),"actual native break removes chosen formed part: "+offset);
            if(!offset.equals(BlockPos.ZERO)) {
                // Original destroy is transient; install a real saved current core before its next update.
                var saved=core.saveWithFullMetadata(f.level.registryAccess()); f.level.removeBlockEntity(f.base); core=(TileAlchemyFurnaceAdvanced)BlockEntity.loadStatic(f.base,core.getBlockState(),saved,f.level.registryAccess()); f.level.setBlockEntity(core); ticks(f,core,1);
            }
            f.check(f.level.getBlockEntity(f.base.above())==upper,"all original break/reversion paths preserve unconstrained upper-center BE");
            for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
                int part=AdvancedFurnaceRules.part(x,y,z); BlockPos pos=f.base.offset(x,y,z); if(part<0 || pos.equals(f.base.offset(offset))) continue;
                f.check(f.level.getBlockState(pos).is(AdvancedFurnaceFormation.original(part).getBlock()),"loaded native broken/save/reload sequence restores original material, not a free formed machine");
            }
            f.drops(); f.reset();
        }
    }
}

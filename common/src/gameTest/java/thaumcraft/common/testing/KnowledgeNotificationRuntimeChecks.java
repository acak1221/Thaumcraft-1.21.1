package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.items.ItemManaBean;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.WarpRules;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.*;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchClues;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.lib.research.ResearchNoteEditing;
import thaumcraft.common.lib.research.ScanManager;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileResearchTable;

/** Actual native committed actions/outbound codec bodies; seeded food/refund/event RNG are explicit branch INPUTS.
 * No headless test claims an inspected client HUD, socket transport, puzzle solving or survival acquisition. */
public final class KnowledgeNotificationRuntimeChecks {
    private KnowledgeNotificationRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        ResearchSettings original=ThaumcraftSettings.server();
        try(var f=new Fixture(test)) {
            f.open();ThaumcraftSettings.configureServer(new ResearchSettings(true,true,true,true,false,0,100));
            protocol(f);scan(f);table(f);deconstruction(f);purchaseAndSyncOnly(f);food(f);warp(f);
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalStateException("Native knowledge notification command failed",failure); }
        finally { ThaumcraftSettings.configureServer(original); }
        test.succeed();
    }
    private static void protocol(Fixture f) {
        var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setAspectPool("aer",33000);feedback(f);
        KnowledgeEvents.sync(f.player);KnowledgeNotificationNetworking.pool(f.player,"aer",1);
        var actual=packets(f);
        f.check(actual.size()==2&&actual.get(0) instanceof KnowledgeSyncPayload snapshot&&actual.get(1).equals(new AspectPoolPayload("aer",(short)1,(short)-32536)),
                "actual native full snapshot precedes old requested SHORT1/queried total−32536 feedback for raw33000; actual="+actual);
        var restored=new thaumcraft.common.lib.research.PlayerKnowledge();restored.load(((KnowledgeSyncPayload)actual.getFirst()).state());
        f.check(restored.getAspectPoolFor("aer")==33000&&knowledge.getAspectPoolFor("aer")==33000,
                "full native snapshot/server raw33000 remains authoritative; legacy feedback cannot mutate either storage into−32536");
        var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.level.registryAccess());
        try {
            var payload=new AspectPoolPayload("aer",(short)-32768,(short)-32536);AspectPoolPayload.STREAM_CODEC.encode(wire,payload);
            f.check(wire.readUtf(128).equals("aer")&&wire.readShort()==Short.MIN_VALUE&&wire.readShort()==-32536&&!wire.isReadable(),"actual native RP wire preserves original ordered key/SHORT delta/SHORT total");
        } finally {wire.release();}
    }
    private static void scan(Fixture f) {
        var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);feedback(f);
        f.check(ScanManager.completeScan(f.player,"#m00-light",new AspectList().add(Aspect.LIGHT,1),ScanManager.ScanKind.ITEM),"actual native scan reward accepts a first known-parent light aspect");
        f.check(feedback(f).equals(List.of(new AspectDiscoveryPayload("lux"),new AspectPoolPayload("lux",(short)3,(short)3)))&&knowledge.getAspectPoolFor("lux")==3,
                "first compound scan emits actual discovery before exact1+2 RP gain, not a fabricated generic snapshot toast");
        var saved=knowledge.save();f.check(!ScanManager.completeScan(f.player,"#m00-light",new AspectList().add(Aspect.LIGHT,1),ScanManager.ScanKind.ITEM)
                        &&knowledge.save().equals(saved)&&feedback(f).isEmpty(),"duplicate native scan cannot replay RP/discovery packets or state");
        f.check(!ScanManager.completeScan(f.player,"#m00-flight",new AspectList().add(Aspect.FLIGHT,1),ScanManager.ScanKind.ITEM)
                        &&knowledge.save().equals(saved)&&feedback(f).isEmpty(),"missing compound parent refuses without a free notification or mutation");
        prepare(f,"JARBRAIN");
        f.check(ScanManager.completeScan(f.player,"#m00-real-clue",new AspectList().add(Aspect.AIR,1),ScanManager.ScanKind.ITEM,
                        ResearchClues.Context.item(new ItemStack(ThaumcraftContent.MATERIALS.get("quick_silver").get()))),"actual server-observed quicksilver clue path admits its original hidden research candidate");
        var clue=feedback(f);
        f.check(clue.equals(List.of(new AspectPoolPayload("aer",(short)1,(short)101),new ResearchNoticePayload("@JARBRAIN")))
                        &&knowledge.isResearchComplete("@JARBRAIN")&&!knowledge.isResearchComplete("JARBRAIN"),"actual clue follows gain and marks only @KEY; no normal research toast/warp grant manufactured");
    }
    private static void table(Fixture f) {
        var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);knowledge.setAspectPool("aer",2);knowledge.setAspectPool("ignis",2);
        f.level.setBlock(f.base,ThaumcraftContent.TABLE.get().state(2),TubeVariantsRuntimeChecks.QUIET);
        f.level.setBlock(f.base.north(),ThaumcraftContent.TABLE.get().state(7),TubeVariantsRuntimeChecks.QUIET);
        f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.aim(net.minecraft.world.phys.Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction()&&f.player.containerMenu instanceof ContainerResearchTable,"actual native two-piece research table opens its real menu");
        var menu=(ContainerResearchTable)f.player.containerMenu;var table=(TileResearchTable)f.level.getBlockEntity(f.base);feedback(f);
        var combine=new ResearchTableCombinePayload(menu.containerId,menu.sessionToken(),table.tableRevision(),"aer","ignis");
        f.check(ResearchTableCombineNetworking.handle(f.player,combine),"actual registered combine receiver pays both native RP inputs");
        f.check(feedback(f).equals(List.of(new AspectPoolPayload("aer",(short)0,(short)1),new AspectPoolPayload("ignis",(short)0,(short)1),new AspectDiscoveryPayload("lux"),new AspectPoolPayload("lux",(short)3,(short)3))),
                "actual combination sends silent input corrections then first output discovery3; no input-spend flight");
        var saved=knowledge.save();f.check(!ResearchTableCombineNetworking.handle(f.player,combine)&&knowledge.save().equals(saved)&&feedback(f).isEmpty(),"stale native combine revision produces only snapshot correction, never replayed reward packets");
        knowledge.setAspectPool("lux",20);
        table.setItem(0,new ItemStack(ThaumcraftContent.INKWELL.get()));
        table.setItem(1,ItemResearchNotes.create(new ResearchNoteData("NITOR",0x123456,false,0,List.of(
                new ResearchNoteData.Cell(0,0,1,"aer"),new ResearchNoteData.Cell(2,0,1,"ignis"),new ResearchNoteData.Cell(1,0,0,null),new ResearchNoteData.Cell(-1,0,0,null))),false));feedback(f);
        f.check(ResearchTableEditNetworking.handle(f.player,new ResearchTableEditPayload(menu.containerId,menu.sessionToken(),table.tableRevision(),ResearchNoteEditing.Action.PLACE,-1,0,"lux")),"actual paid research edit places a disconnected mutable cell");
        f.check(knowledge.getAspectPoolFor("lux")==19&&feedback(f).equals(List.of(new AspectPoolPayload("lux",(short)0,(short)19))),"native placement's original amount0 correction cannot look like gained RP");
        knowledge.addResearch("RESEARCHER1");long seed=refundSeed();f.level.random.setSeed(seed);
        f.check(ResearchTableEditNetworking.handle(f.player,new ResearchTableEditPayload(menu.containerId,menu.sessionToken(),table.tableRevision(),ResearchNoteEditing.Action.ERASE,-1,0,null)),"actual native erase consumes the declared successful original refund RNG input");
        f.check(knowledge.getAspectPoolFor("lux")==20&&feedback(f).equals(List.of(new AspectPoolPayload("lux",(short)0,(short)20))),"real refund+1 still sends original delta0, retaining server-owned refund sound without an extra reward flight");
        knowledge.addResearch("RESEARCHDUPE");knowledge.setAspectPool("lux",100);knowledge.setAspectPool("ignis",100);
        table.setItem(1,ItemResearchNotes.create(ResearchNoteRuntimeChecks.solvedNitor(),true)); // Explicit solved-note INPUT; puzzle solving is separately covered.
        f.player.getInventory().setItem(3,new ItemStack(Items.PAPER));f.player.getInventory().setItem(4,new ItemStack(Items.BLACK_DYE));feedback(f);
        f.check(ResearchTableCopyNetworking.handle(f.player,new ResearchTableCopyPayload(menu.containerId,menu.sessionToken(),table.tableRevision())),"actual paid copy consumes physical paper/black dye and canonical RP costs");
        f.check(feedback(f).equals(List.of(new AspectPoolPayload("lux",(short)0,(short)97),new AspectPoolPayload("ignis",(short)0,(short)99)))&&table.getItem(1).getCount()==2,
                "actual copied note sends two original silent corrections, not gained-RP/discovery/research notices");
        f.player.closeContainer();
    }
    private static void deconstruction(Fixture f) {
        f.put(f.base,thaumcraft.common.config.ThaumcraftDeconstruction.TABLE.get());
        var table=(thaumcraft.common.tiles.TileDeconstructionTable)f.level.getBlockEntity(f.base);
        table.setItem(0,new ItemStack(Items.IRON_INGOT,2));
        var primals=thaumcraft.common.lib.research.DeconstructionRules.primals(thaumcraft.common.lib.research.ObjectAspectResolver.get(new ItemStack(Items.IRON_INGOT),f.level));
        long seed=0;for(;seed<100000;seed++)if(RandomSource.create(seed).nextInt(80)<primals.visSize())break;
        f.check(seed<100000&&primals.size()>0,"declared deconstruction RNG input has a real possible paid primal reward");
        var expected=RandomSource.create(seed);expected.nextInt(80);Aspect reward=primals.getAspects()[expected.nextInt(primals.size())];f.level.random.setSeed(seed);
        for(int tick=0;tick<40;tick++)thaumcraft.common.tiles.TileDeconstructionTable.tick(f.level,f.base,table.getBlockState(),table);
        f.check(table.getItem(0).getCount()==1&&table.reward()==reward,"forty actual native work updates consume ONE physical iron ingot and produce original stored reward");
        f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.aim(net.minecraft.world.phys.Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction()&&f.player.containerMenu instanceof thaumcraft.common.container.ContainerDeconstructionTable,"actual native table interaction opens its real claim menu");
        var menu=(thaumcraft.common.container.ContainerDeconstructionTable)f.player.containerMenu;int before=KnowledgeAccess.get(f.player).getAspectPoolFor(reward.getTag());feedback(f);
        f.check(menu.clickMenuButton(f.player,1)&&table.reward()==null&&KnowledgeAccess.get(f.player).getAspectPoolFor(reward.getTag())==before+1
                        &&feedback(f).equals(List.of(new AspectPoolPayload(reward.getTag(),(short)1,(short)(before+1)))),
                "actual paid deconstruction menu claim publishes one original+1 RP packet after clearing the sole stored reward");
        f.check(!menu.clickMenuButton(f.player,1)&&table.getItem(0).getCount()==1&&feedback(f).isEmpty(),"repeated actual claim cannot duplicate stored RP, packets or physical input debit");f.player.closeContainer();
    }
    private static long refundSeed() {for(long seed=0;seed<100000;seed++)if(RandomSource.create(seed).nextFloat()<.25F)return seed;throw new AssertionError("No original refund branch seed");}
    private static void purchaseAndSyncOnly(Fixture f) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        prepare(f,"COREBUTCHER");var knowledge=KnowledgeAccess.get(f.player);var bought=ResearchPurchaseNetworking.handle(f.player,"COREBUTCHER");
        var actual=feedback(f);
        f.check(bought.success()&&actual.equals(List.of(new AspectPoolPayload("telum",(short)-3,(short)97),new AspectPoolPayload("bestia",(short)-3,(short)97),new AspectPoolPayload("sensus",(short)-3,(short)97),new AspectPoolPayload("meto",(short)-3,(short)97),new ResearchNoticePayload("COREBUTCHER"),new WarpMessagePayload(1,(byte)0))),
                "real paid purchase emits ordered negative costs, actual key notice, then its original warp feedback; actual="+actual);
        var before=knowledge.save();f.check(!ResearchPurchaseNetworking.handle(f.player,"COREBUTCHER").success()&&knowledge.save().equals(before)&&feedback(f).isEmpty(),"refused repeated purchase cannot publish free completion or RP messages");
        prepare(f,"NITOR");var dispatcher=f.level.getServer().getCommands().getDispatcher();var source=f.player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        dispatcher.execute("tc research @s NITOR",source);f.check(feedback(f).isEmpty()&&knowledge.isResearchComplete("NITOR"),"original admin grant is full-sync-only, never a fabricated normal research popup");
        dispatcher.execute("tc aspect @s aer 5",source);f.check(feedback(f).isEmpty(),"original administrator aspect grant has no per-point scan popup channel");
        prepare(f,"NITOR");var book=new ItemStack(ThaumcraftContent.THAUMONOMICON_CHEAT.get());f.player.setItemInHand(InteractionHand.MAIN_HAND,book);
        f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.MAIN_HAND);f.check(knowledge.isResearchComplete("NITOR")&&feedback(f).isEmpty(),"real cheat-sheet all-research pass syncs state but does not invent research or RP notifications");
    }
    private static void food(Fixture f) {
        var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);
        var meal=ItemManaBean.create(Aspect.LIGHT);meal.setCount(2);f.player.setItemInHand(InteractionHand.MAIN_HAND,meal);f.player.getFoodData().setFoodLevel(10);f.player.getFoodData().setSaturation(0);feedback(f);
        long seed=0;for(;seed<100000;seed++){var random=RandomSource.create(seed);if(random.nextInt(40)==0&&random.nextFloat()<.25F)break;}f.check(seed<100000,"declared real native food branch seed selects null effect and original RP gain");f.level.random.setSeed(seed);
        f.check(f.player.gameMode.useItem(f.player,f.level,meal,InteractionHand.MAIN_HAND).consumesAction(),"native mana-bean RMB begins actual ten-tick food use");
        for(int tick=0;tick<10;tick++){f.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);f.player.doTick();}
        f.check(meal.getCount()==1&&knowledge.getAspectPoolFor("lux")==1&&feedback(f).equals(List.of(new AspectPoolPayload("lux",(short)1,(short)1))),"actual completed physical mana bean sends one original+1 pool message without inventing a separate discovery notification");
    }
    private static void warp(Fixture f) {
        var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);knowledge.setWarpPerm(51);knowledge.setWarpCounter(100);feedback(f);
        long seed=warpSeed(51,WarpRules.Event.RESEARCH);var expected=RandomSource.create(seed);var decision=WarpRules.decide(51,100,0,false,expected::nextInt);
        var totals=new LinkedHashMap<String,Integer>();var ordered=new ArrayList<CustomPacketPayload>();points(expected,1,totals,ordered);
        ordered.add(new ResearchNoticePayload("@BATHSALTS"));points(expected,10,totals,ordered);ordered.add(new ResearchNoticePayload("ELDRITCHMINOR"));points(expected,20,totals,ordered);ordered.add(new ResearchNoticePayload("ELDRITCHMAJOR"));
        var actual=RandomSource.create(seed);var accepted=WarpEvents.checkWarpEvent(f.player,actual);
        f.check(accepted.equals(decision)&&feedback(f).equals(ordered)&&actual.nextLong()==expected.nextLong(),"actual warp event emits EVERY original repeated primal draw with momentary total and interleaved three new gates, retaining exact RNG order");
        for(var row:totals.entrySet())f.check(knowledge.getAspectPoolFor(row.getKey())==row.getValue(),"actual warp RP state equals independently drawn original per-primal total "+row.getKey());
        knowledge.reset();knowledge.setInitializedAspects(true);knowledge.addResearch("@BATHSALTS");knowledge.addResearch("ELDRITCHMINOR");knowledge.addResearch("ELDRITCHMAJOR");knowledge.setWarpPerm(99);knowledge.setWarpSticky(1);knowledge.setWarpCounter(100);feedback(f);
        WarpEvents.checkWarpEvent(f.player,RandomSource.create(warpSeed(100,WarpRules.Event.CLARITY)));
        f.check(knowledge.getWarpSticky()==0&&feedback(f).equals(List.of(new WarpMessagePayload(-1,(byte)1))),"real original clarity event's actual sticky loss emits its original negative warp popup once, with no duplicate known gate notices");
    }
    private static long warpSeed(int warp,WarpRules.Event event) {for(long seed=0;seed<100000;seed++)if(WarpRules.decide(warp,100,0,false,RandomSource.create(seed)::nextInt).event()==event)return seed;throw new AssertionError("Missing declared warp input "+event);}
    private static void points(RandomSource random,int bound,Map<String,Integer> totals,List<CustomPacketPayload> ordered) {
        int count=1+random.nextInt(bound);var primals=List.of("aer","terra","ignis","aqua","ordo","perditio");
        for(int draw=0;draw<count;draw++){String tag=primals.get(random.nextInt(6));int total=totals.merge(tag,1,Integer::sum);ordered.add(new AspectPoolPayload(tag,(short)1,(short)total));}
    }
    private static void prepare(Fixture f,String absent) {
        var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);
        for(var research:ResearchCatalog.all())if(research.isEnabled()&&!research.key().equals(absent))knowledge.addResearch(research.key());
        for(Aspect aspect:Aspect.aspects.values())knowledge.setAspectPool(aspect.getTag(),100);feedback(f);
    }
    static List<CustomPacketPayload> feedback(Fixture f) { return packets(f).stream().filter(value->!(value instanceof KnowledgeSyncPayload)).toList(); }
    private static List<CustomPacketPayload> packets(Fixture f) {
        f.connection.flushChannel();f.channel.runPendingTasks();Object packet;var result=new ArrayList<CustomPacketPayload>();
        while((packet=f.channel.readOutbound())!=null) {
            try {
                if(!(packet instanceof ClientboundCustomPayloadPacket custom))continue;
                var id=custom.payload().type().id();
                if(!id.equals(AspectPoolPayload.TYPE.id())&&!id.equals(AspectDiscoveryPayload.TYPE.id())&&!id.equals(ResearchNoticePayload.TYPE.id())&&!id.equals(KnowledgeSyncPayload.TYPE.id())&&!id.equals(WarpMessagePayload.TYPE.id()))continue;
                if(!(custom.payload() instanceof dev.architectury.impl.NetworkAggregator.BufCustomPacketPayload wrapped)){result.add(custom.payload());continue;}
                var body=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(wrapped.payload()),f.level.registryAccess());
                try {
                    CustomPacketPayload decoded=id.equals(AspectPoolPayload.TYPE.id())?AspectPoolPayload.STREAM_CODEC.decode(body):id.equals(AspectDiscoveryPayload.TYPE.id())?AspectDiscoveryPayload.STREAM_CODEC.decode(body):id.equals(ResearchNoticePayload.TYPE.id())?ResearchNoticePayload.STREAM_CODEC.decode(body):id.equals(KnowledgeSyncPayload.TYPE.id())?KnowledgeSyncPayload.STREAM_CODEC.decode(body):WarpMessagePayload.STREAM_CODEC.decode(body);
                    f.check(!body.isReadable(),"actual native outbound knowledge payload has no ignored trailing bytes: "+id);result.add(decoded);
                } finally {body.release();}
            } finally {ReferenceCountUtil.release(packet);}
        }
        return List.copyOf(result);
    }
}

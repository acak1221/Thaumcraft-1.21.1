package thaumcraft.common.testing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileVisRelay;

/** Declared synthetic native entity/game-listener cycles drive genuine player
 * living ticks; earned source, original relay cache and paid-before-pulse
 * removal remain actual gameplay. No physical packet-client claim. */
public final class VisAmuletRuntimeChecks {
    private static PulseRelayBlock probeBlock;
    private static BlockEntityType<PulseRelay> probeType;
    private VisAmuletRuntimeChecks() {}
    /** Test-mod initializer only: actual compatible native block/type/ticker, never a production relay impostor. */
    public static synchronized void registerTestProbes(){
        if(probeBlock!=null)return;
        var id=ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest","amulet_pulse_relay");
        probeBlock=Registry.register(BuiltInRegistries.BLOCK,id,new PulseRelayBlock(BlockBehaviour.Properties.of().noOcclusion()));
        probeType=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,BlockEntityType.Builder.of(PulseRelay::new,probeBlock).build(null));
    }
    public static void nativeWornTransferAndRelayPayment(GameTestHelper test) {
        var f=NodeTransductionRuntimeChecks.fixture(test,false);
        EmbeddedPlayerTickDriver[] clock={null};
        Runnable close=()->{try{if(clock[0]!=null)clock[0].close();}finally{f.level.getServer().getConnection().getConnections().remove(f.connection);f.close();}};
        var info=BoreWorkRuntimeChecks.testInfo(test);
        info.addListener(new GameTestListener(){
            @Override public void testStructureLoaded(GameTestInfo actual){}
            @Override public void testPassed(GameTestInfo actual,GameTestRunner runner){close.run();}
            @Override public void testFailed(GameTestInfo actual,GameTestRunner runner){close.run();}
            @Override public void testAddedForRerun(GameTestInfo previous,GameTestInfo next,GameTestRunner runner){close.run();}
        });
        try {
            f.open();
            test.assertTrue(probeBlock!=null&&probeType!=null,"test mod registered the native amulet pulse probe before registry freeze");
            var source=NodeTransductionRuntimeChecks.energize(f,f.base.west(6),NodeTransductionRuntimeChecks.primals(64),null,TileNode.NodeType.NORMAL);
            NodeTransductionRuntimeChecks.sourceTick(f,source);
            f.put(f.base.below(),Blocks.STONE);
            f.level.setBlock(f.base,probeBlock.defaultBlockState(),TubeVariantsRuntimeChecks.QUIET);
            var middle=(PulseRelay)f.level.getBlockEntity(f.base);
            test.assertTrue(middle!=null&&middle.getType()==probeType&&probeType.isValid(middle.getBlockState()),"actual installed pulse probe owns its compatible native type and ticker");
            TileVisNode.tick(f.level,f.base,middle.getBlockState(),middle);
            BlockPos at=f.base.east(6);f.put(at.below(),Blocks.STONE);
            f.level.setBlock(at,ThaumcraftNodeDevices.VIS_RELAY.get().defaultBlockState().setValue(BlockVisRelay.FACING,Direction.UP),TubeVariantsRuntimeChecks.QUIET);
            var relay=(TileVisRelay)f.level.getBlockEntity(at);
            f.player.setPos(at.getX()+.5,at.getY()+.5,at.getZ()+.5);f.player.setDeltaMovement(0,0,0);
            for(int tick=0;tick<20;tick++)VisNetRuntimeChecks.relayTick(f,relay);
            test.assertTrue(relay.getParent()!=null&&relay.getParent().get()==middle&&middle.getParent().get()==source&&TileVisRelay.nearbyPlayer(f.player)==relay,
                    "actual twentieth relay update associates the real player with source-6 -> middle0 -> relay+6 nearest path");
            ItemStack amulet=new ItemStack(ThaumcraftBaubles.VIS_AMULET_LESSER.get());var item=ThaumcraftBaubles.VIS_AMULET_LESSER.get();
            for(Aspect aspect:Aspect.getPrimalAspects())item.storeVis(amulet,aspect,10);
            PlayerAccessories.get(f.player).setItem(0,amulet);
            ItemStack wand=new ItemStack(ThaumcraftContent.WAND.get());var amounts=new java.util.LinkedHashMap<String,Integer>();
            for(String tag:WandData.PRIMALS)amounts.put(tag,2498);wand.set(ThaumcraftContent.WAND_DATA.get(),new WandData("wood","gold",amounts));f.player.getInventory().setItem(0,wand);
            test.assertTrue(f.level.getServer().getConnection().getConnections().add(f.connection),"owned embedded fixture transport is registered without assuming it supplies physical client packets");
            clock[0]=EmbeddedPlayerTickDriver.open(f.level,f.player);
            int[] stage={0};long[] moved={0};ItemStack[] ejecting={null};ItemEntity[] drop={null};boolean[] paidBeforePulse={false};
            test.onEachTick(()->{
                if(f.closed)return;
                try {
                    clock[0].checkHealthy();
                    if(stage[0]==0) {
                        if(item.getVis(amulet,Aspect.AIR)==10)return;
                        for(Aspect aspect:Aspect.getPrimalAspects())test.assertTrue(item.getVis(amulet,aspect)==13&&ItemWandCasting.getVis(wand,aspect.getTag())==2500,
                                "native worn phase transfers only two raw CV to the almost-full wand, THEN draws five raw CV into the amulet: "+aspect.getTag());
                        f.player.setPos(at.getX()+6.5,at.getY()+.5,at.getZ()+.5);moved[0]=f.player.tickCount;stage[0]=1;
                    } else if(stage[0]==1) {
                        if(f.player.tickCount-moved[0]<10)return;
                        for(Aspect aspect:Aspect.getPrimalAspects())test.assertTrue(item.getVis(amulet,aspect)==13,"distance squared36 prevents original <26 relay recharge");
                        test.assertTrue(TileVisRelay.nearbyPlayer(f.player)==null,"outside cached association is removed instead of pulling distant vis");
                        ItemStack empty=new ItemStack(item);PlayerAccessories.get(f.player).setItem(0,empty);
                        f.player.setPos(at.getX()+.5,at.getY()+.5,at.getZ()+.5);
                        middle.action=()->{
                            test.assertTrue(item.getVis(empty,Aspect.AIR)==5&&source.getAvailableVis().getAmount(Aspect.AIR)==3,
                                    "closed native amulet receives exactly the committed five from eight source units BEFORE route pulse callbacks");
                            ejecting[0]=empty.copy();
                            // Native removeItem splits a NEW physical stack and
                            // shrinks the previous equipped alias to count0.
                            ItemStack removed=PlayerAccessories.get(f.player).removeItem(0,1);
                            test.assertTrue(ItemStack.matches(removed,ejecting[0])&&empty.isEmpty(),"callback detaches the actual paid amulet and empties its old slot alias");
                            drop[0]=f.player.drop(removed,false);paidBeforePulse[0]=true;
                            boolean refused=false;try{PlayerAccessories.acceptPreparedAmuletGrant(relay,Aspect.AIR,5);}catch(IllegalStateException expected){refused=true;}
                            test.assertTrue(refused&&item.getVis(removed,Aspect.AIR)==5,"post-payment callback cannot forge or replay another exact amulet grant");
                        };
                        stage[0]=2;
                    } else if(paidBeforePulse[0]) {
                        test.assertTrue(clock[0].serverPostTicks()>0&&clock[0].nativeConnectionTicks()>0&&clock[0].actualPlayerPostTicks()>=3,
                                "declared synthetic world-POST driver reaches real native player ticks across transfer, distance and paid detach phases");
                        var amuletDrops=f.drops().stream().filter(entity->entity.getItem().is(item)).toList();
                        test.assertTrue(PlayerAccessories.get(f.player).getItem(0).isEmpty()&&drop[0]!=null&&amuletDrops.size()==1&&amuletDrops.getFirst()==drop[0]&&ItemStack.matches(drop[0].getItem(),ejecting[0])
                                &&item.getVis(drop[0].getItem(),Aspect.AIR)==5,"native physical drop retains one paid five-CV component snapshot with no equipped duplicate");
                        for(Aspect aspect:Aspect.getPrimalAspects())if(aspect!=Aspect.AIR)test.assertTrue(item.getVis(drop[0].getItem(),aspect)==0,"detached item cannot receive the later per-aspect source grants");
                        close.run();test.succeed();
                    }
                }catch(RuntimeException|Error failure){close.run();throw failure;}
            });
            test.runAfterDelay(160,()->{if(!f.closed){close.run();test.fail("native worn amulet scenario timed out; stage="+stage[0]+", playerTicks="+f.player.tickCount+", relayPhase="+relay.getNodeCounter());}});
        }catch(RuntimeException|Error failure){close.run();throw failure;}
    }
    private static final class PulseRelay extends TileVisNode {
        Runnable action;
        PulseRelay(BlockPos pos,BlockState state){super(probeType,pos,state);}
        @Override public int getRange(){return 8;}
        @Override public boolean isSource(){return false;}
        @Override public void triggerConsumeEffect(Aspect aspect){if(action!=null){Runnable once=action;action=null;once.run();}}
    }
    private static final class PulseRelayBlock extends BaseEntityBlock {
        private static final MapCodec<PulseRelayBlock> CODEC=simpleCodec(PulseRelayBlock::new);
        private PulseRelayBlock(Properties properties){super(properties);}
        @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PulseRelay(pos,state);}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return createTickerHelper(type,probeType,TileVisNode::tick);
        }
    }
}

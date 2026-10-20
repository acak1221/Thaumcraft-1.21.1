package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.fluid.FluidPure;

/** Native Classic-fluid states, real scheduled propagation/collision and real200-tick salt expiry. */
public final class PureFluidRuntimeChecks {
    private static final int QUIET=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private PureFluidRuntimeChecks(){}
    public static void run(GameTestHelper test){
        var check=new Check(test);try{check.prepare();check.states();check.flow();check.ward();check.ordinaryTicks();}
        catch(RuntimeException|Error failure){check.finish();throw failure;}
    }
    private static final class Check {
        final GameTestHelper test;final ServerLevel level;final BlockPos base;
        final Map<BlockPos,BlockState> originals=new LinkedHashMap<>();final List<ChunkPos> forced=new ArrayList<>();final List<ItemEntity> items=new ArrayList<>();
        final Connection connection=new Connection(PacketFlow.SERVERBOUND);final EmbeddedChannel channel=new EmbeddedChannel(connection);
        ServerPlayer player;TickEvent.ServerLevelTick observer;int oldRandomTicks,lastSourceSaltAge,lastFlowSaltAge;long start,firstDown=-1,sourceExpired=-1,flowExpired=-1;
        ItemEntity salt,flowSalt;BlockPos timerSource,saltSource,saltFlow;boolean finished;
        Check(GameTestHelper test){this.test=test;level=test.getLevel();base=test.absolutePos(new BlockPos(3,100,3));}
        void set(BlockPos pos,BlockState state){originals.putIfAbsent(pos.immutable(),level.getBlockState(pos));level.setBlock(pos,state,QUIET);}
        BlockState pure(int metadata){return ThaumcraftSpa.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL,metadata);}
        void tick(BlockPos pos){level.getFluidState(pos).tick(level,pos);}
        void cell(BlockPos pos){
            for(BlockPos target:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,3,2)))set(target.immutable(),Blocks.AIR.defaultBlockState());
            set(pos.below(),Blocks.STONE.defaultBlockState());for(Direction side:Direction.Plane.HORIZONTAL)set(pos.relative(side),Blocks.STONE.defaultBlockState());
        }
        void prepare(){
            oldRandomTicks=level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,level.getServer());
            forced.addAll(GolemChunks.hold(level,List.of(base.offset(-10,-4,-8),base.offset(50,6,8))));
            player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"pure-"+UUID.randomUUID().toString().substring(0,8)),ClientInformation.createDefault());
            level.getServer().getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false));player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setPos(Vec3.atCenterOf(base.above(5)));
        }
        void states(){
            cell(base);set(base,pure(0));var source=level.getFluidState(base);
            test.assertTrue(source.getType() instanceof FluidPure.Source&&source.isSource()&&source.is(FluidTags.WATER)&&source.getAmount()==8&&source.getOwnHeight()==1F&&source.getType().getTickDelay(level)==5,"registered SOURCE is an actual native water-tag fluid with original8 quanta/full physical source/tick5");
            for(int meta=1;meta<=7;meta++){
                set(base,pure(meta));var fluid=level.getFluidState(base);
                test.assertTrue(fluid.getType() instanceof FluidPure.Flowing&&!fluid.isSource()&&fluid.is(FluidTags.WATER)&&fluid.getAmount()==8-meta&&!fluid.getValue(FlowingFluid.FALLING)&&fluid.createLegacyBlock()==pure(meta),"native flowing state maps exactly metadata1..7 without modern falling8: "+meta);
            }
            int[] light={8,7,6,5,3,2,1,0};for(int meta=0;meta<8;meta++)test.assertTrue(pure(meta).getLightEmission()==light[meta],"original Classic extra-quantum luminosity "+meta);
            set(base,pure(0));var captured=ThaumcraftSpa.PURE_WATER.get().pickupBlock(player,level,base,pure(0));test.assertTrue(captured.is(ThaumcraftSpa.BUCKET_PURE.get())&&captured.getCount()==1&&level.getBlockState(base).isAir()&&ThaumcraftSpa.PURE_FLUID_SOURCE.get().getBucket()==ThaumcraftSpa.BUCKET_PURE.get(),"actual native BucketPickup captures one original source into its real registered filled container");set(base,pure(0));
            test.assertTrue(pure(0).getCollisionShape(level,base).isEmpty()&&pure(0).isRandomlyTicking(),"real liquid has no solid collision and retains original random ticking");
            var eye=new ItemEntity(level,base.getX()+.5,base.getY()+.5,base.getZ()+.5,new ItemStack(Items.IRON_INGOT),0,0,0);eye.getItem().set(DataComponents.CUSTOM_NAME,Component.literal("native-pure-eye-"+UUID.randomUUID()));eye.setNoGravity(true);eye.setPickUpDelay(32767);items.add(eye);test.assertTrue(level.addFreshEntity(eye),"actual native item for physical eye-immersion boundary is admitted");
            int[] metas={0,1,2,7};float[] physical={1F,1F,.875F,.25F};
            for(int i=0;i<metas.length;i++){
                set(base,pure(metas[i]));test.assertTrue(level.getFluidState(base).getHeight(level,base)==physical[i],"actual fluid physical height uses Forge filledPercentage, separately from renderer: "+metas[i]);
                eye.setDeltaMovement(Vec3.ZERO);eye.setPos(base.getX()+.5,base.getY()+physical[i]-.01-eye.getEyeHeight(),base.getZ()+.5);eye.tick();test.assertTrue(eye.isEyeInFluid(FluidTags.WATER),"actual native entity tick sees eyes just inside original Forge filledPercentage: "+metas[i]);
                eye.setDeltaMovement(Vec3.ZERO);eye.setPos(base.getX()+.5,base.getY()+physical[i]+.01-eye.getEyeHeight(),base.getZ()+.5);eye.tick();test.assertTrue(!eye.isEyeInFluid(FluidTags.WATER)&&level.getBlockState(base)==pure(metas[i]),"actual native entity tick sees eyes just above original Forge filledPercentage and non-player collision preserves fluid: "+metas[i]);
            }
            eye.discard();set(base,pure(0));
            set(base.west(),Blocks.AIR.defaultBlockState());set(base.east(),Blocks.AIR.defaultBlockState());
            var from=new Vec3(base.getX()-1.5,base.getY()+.95,base.getZ()+.5);var to=from.add(4,0,0);
            var hit=level.clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
            test.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(base)&&source.getHeight(level,base)==1F,"actual native SOURCE_ONLY ray sees original full-cube source bounds above the separate Classic .875 visual surface");
            set(base,pure(1));hit=level.clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
            test.assertTrue(hit.getType()==HitResult.Type.MISS,"original fullHit/source-only fluid targeting ignores a real flowing cell");
            var pureSource=ThaumcraftSpa.PURE_FLUID_SOURCE.get();var pureFlow=ThaumcraftSpa.PURE_FLUID_FLOWING.get();
            test.assertTrue(Fluids.WATER.isSame(pureSource)&&Fluids.FLOWING_WATER.isSame(pureFlow)&&!pureSource.isSame(Fluids.WATER)&&!pureFlow.isSame(Fluids.FLOWING_WATER)&&!Fluids.WATER.isSame(Fluids.LAVA)&&!Fluids.LAVA.isSame(pureSource),"native water's original Material.WATER adapter is asymmetric and leaves Classic and lava identities separate");
            test.assertTrue(!source.canBeReplacedWith(level,base,Fluids.WATER,Direction.DOWN)&&source.canBeReplacedWith(level,base,Fluids.LAVA,Direction.DOWN)&&!source.canBeReplacedWith(level,base,Fluids.LAVA,Direction.EAST),"native incoming replacement preserves exact downward/non-water gate without broadening horizontal or same-material flow");
        }
        void flow(){
            var at=base;cell(at);set(at,pure(0));set(at.below(),Blocks.AIR.defaultBlockState());tick(at);
            test.assertTrue(level.getBlockState(at)==pure(0)&&level.getBlockState(at.below())==pure(1),"actual native fluid tick falls SOURCE0 into metadata1, never modern falling8");
            cell(at);set(at,pure(1));set(at.west(),pure(0));set(at.east(),pure(0));tick(at);
            test.assertTrue(level.getBlockState(at)==pure(1)&&!level.getFluidState(at).isSource(),"two actual adjacent sources do not generate an infinite source");
            cell(at);set(at,pure(7));set(at.above().west(),pure(0));tick(at);
            test.assertTrue(level.getBlockState(at)==pure(1),"an upper horizontal feeder gives the exact original seven quanta");
            cell(at);set(at,pure(7));set(at.below(),Blocks.AIR.defaultBlockState());tick(at);
            test.assertTrue(level.getBlockState(at).isAir()&&level.getBlockState(at.below())==pure(1),"Classic starved-cell removal still continues its original downward metadata1 attempt");
            cell(at);set(at,pure(0));set(at.below(),Blocks.WATER.defaultBlockState());
            test.assertTrue(BlockFluidPure.legacyDensity(level.getBlockState(at.below()))==Integer.MAX_VALUE&&BlockFluidPure.canDisplace(level,at.below())&&!BlockFluidPure.canFlowInto(level,at.below()),"vanilla water has original unknown MAX density but the separate lateral WATER-material veto");tick(at);
            test.assertTrue(level.getBlockState(at.below())==pure(1),"actual Classic downward displacement overwrites vanilla water instead of assuming modern water rules");
            cell(at);set(at,pure(0));set(at.west(),Blocks.WATER.defaultBlockState());set(at.east(),Blocks.AIR.defaultBlockState());set(at.east().below(),Blocks.AIR.defaultBlockState());tick(at);
            test.assertTrue(level.getBlockState(at.west()).is(Blocks.WATER)&&level.getBlockState(at.east())==pure(1),"a real immediate eastern hole selects only that route and preserves lateral vanilla water");
            cell(at);set(at,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,7));set(at.west(),pure(0));tick(at);
            test.assertTrue(level.getBlockState(at).is(Blocks.WATER)&&level.getBlockState(at).getValue(LiquidBlock.LEVEL)==1&&level.getBlockState(at.west())==pure(0),"actual native vanilla flowing water is fed by adjacent original Material.WATER Pure source without consuming/converting that source; water="+level.getBlockState(at)+", feeder="+level.getBlockState(at.west())+", fluid="+level.getFluidState(at));
            set(at.above(),pure(0));test.assertTrue(level.getFluidState(at).getHeight(level,at)==1F,"actual native vanilla-water surface fills beneath an original same-water-material Pure cell");
            cell(at);set(at,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,7));set(at.west(),pure(3));tick(at);
            test.assertTrue(level.getBlockState(at).is(Blocks.WATER)&&level.getBlockState(at).getValue(LiquidBlock.LEVEL)==4&&level.getBlockState(at.west())==pure(3),"native vanilla water reads original Pure flowing metadata3 as a same-material feeder with decay4, preserving its exact block; water="+level.getBlockState(at)+", feeder="+level.getBlockState(at.west()));
            cell(at);set(at,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,7));set(at.above(),pure(0));tick(at);
            test.assertTrue(level.getBlockState(at).is(Blocks.WATER)&&level.getFluidState(at).getValue(FlowingFluid.FALLING)&&level.getBlockState(at.above())==pure(0),"native vanilla water receives the original above-water falling state from Pure without changing that source; water="+level.getBlockState(at)+", above="+level.getBlockState(at.above()));
            cell(at);set(at,pure(7));set(at.west(),Blocks.WATER.defaultBlockState());tick(at);
            test.assertTrue(level.getBlockState(at).isAir()&&level.getBlockState(at.west()).is(Blocks.WATER),"the reverse actual Classic tick still refuses a vanilla-water feeder and removes its starved cell; pure="+level.getBlockState(at)+", water="+level.getBlockState(at.west()));
            cell(at);set(at,pure(0));set(at.below(),ThaumcraftFlux.FLUX_GOO.get().defaultBlockState());tick(at);
            test.assertTrue(level.getBlockState(at.below()).is(ThaumcraftFlux.FLUX_GOO.get()),"original MaterialTaint blocks movement even though native goo has an empty collision");
            for(var obstacle:List.of(Blocks.OAK_PRESSURE_PLATE.defaultBlockState(),Blocks.CAKE.defaultBlockState().setValue(net.minecraft.world.level.block.CakeBlock.BITES,6),Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN,true))){
                set(at.below(),obstacle);test.assertTrue(!BlockFluidPure.canDisplace(level,at.below())&&BlockFluidPure.legacySolid(obstacle),"original material of plate/eaten cake/open gate blocks displacement independently of modern thin collision: "+obstacle);
            }
            for(var obstacle:List.of(Blocks.LADDER.defaultBlockState(),Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS,8))){
                set(at.below(),obstacle);test.assertTrue(BlockFluidPure.canDisplace(level,at.below())&&!BlockFluidPure.legacySolid(obstacle),"original circuits/snow material remains displaceable independently of modern tall collision: "+obstacle);
            }
            cell(at);set(at.above(),pure(0));set(at,Blocks.DANDELION.defaultBlockState());set(at.below(),Blocks.DIRT.defaultBlockState());
            set(at.west(),pure(0));set(at.west().below(),Blocks.STONE.defaultBlockState());for(Direction side:List.of(Direction.WEST,Direction.NORTH,Direction.SOUTH))set(at.west().relative(side),Blocks.STONE.defaultBlockState());
            int[] attempts={0};EntityEvent.Add reenter=(entity,world)->{
                if(world==level&&entity instanceof ItemEntity item&&item.getItem().is(Items.DANDELION)){attempts[0]++;tick(at.west());}
                return EventResult.pass();
            };EntityEvent.ADD.register(reenter);try{tick(at.above());}finally{EntityEvent.ADD.unregister(reenter);}
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(2));int flowers=drops.stream().filter(item->item.getItem().is(Items.DANDELION)).mapToInt(item->item.getItem().getCount()).sum();
            test.assertTrue(level.getBlockState(at)==pure(1)&&flowers==1&&attempts[0]==1,"actual unknown-density plant displacement publishes exactly one native drop even when ADD reenters a second real source");for(var drop:drops){items.add(drop);drop.discard();}
            cell(at);set(at,pure(1));set(at.east(),pure(7));player.removeEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD));player.setPos(at.getX()+.5,at.getY()+.1,at.getZ()+.5);player.setDeltaMovement(Vec3.ZERO);
            boolean wet=player.updateFluidHeightAndDoFluidPushing(FluidTags.WATER,.014D);player.move(MoverType.SELF,Vec3.ZERO);
            test.assertTrue(wet&&player.getFluidHeight(FluidTags.WATER)>.6&&player.getDeltaMovement().x>0&&!player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD))&&level.getBlockState(at)==pure(1),"actual native water physics sees Classic flowing height/direction, while flowing collision grants no Ward and consumes no fluid");
        }
        void ward(){
            var at=base;var ward=ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD);
            int[] warps={0,400,Integer.MAX_VALUE},durations={32000,10000,4};
            for(int i=0;i<warps.length;i++){
                cell(at);set(at,pure(0));player.removeEffect(ward);KnowledgeAccess.get(player).setWarpPerm(warps[i]);player.setPos(at.getX()+.5,at.getY()+.05,at.getZ()+.5);player.setDeltaMovement(Vec3.ZERO);
                test.assertTrue(player.isAlive()&&!player.noPhysics&&player.level()==level&&level.getEntity(player.getUUID())==player&&level.getServer().getLevel(level.dimension())==level&&level.getServer().isSameThread()&&level.getBlockState(at)==pure(0)&&player.getBoundingBox().intersects(new AABB(at)),"native source collision prerequisites: alive="+player.isAlive()+", noPhysics="+player.noPhysics+", sameLevel="+(player.level()==level)+", nativeUuid="+(level.getEntity(player.getUUID())==player)+", canonical="+(level.getServer().getLevel(level.dimension())==level)+", sameThread="+level.getServer().isSameThread()+", state="+level.getBlockState(at)+", at="+at+", player="+player.position()+", box="+player.getBoundingBox());
                player.move(MoverType.SELF,Vec3.ZERO);
                var effect=player.getEffect(ward);test.assertTrue(effect!=null&&effect.getDuration()==durations[i]&&effect.getAmplifier()==0&&effect.isAmbient()&&level.getBlockState(at).isAir(),"actual native player collision grants exact source-only original WarpWard and consumes one source: "+warps[i]+"; effect="+effect+", duration="+(effect==null?-1:effect.getDuration())+", amp="+(effect==null?-1:effect.getAmplifier())+", ambient="+(effect!=null&&effect.isAmbient())+", state="+level.getBlockState(at)+", warp="+KnowledgeAccess.get(player).getWarpPerm()+", pos="+player.position()+", box="+player.getBoundingBox());
            }
            cell(at);set(at,pure(0));player.removeEffect(ward);player.addEffect(new MobEffectInstance(ward,77,2,true,true));player.move(MoverType.SELF,Vec3.ZERO);
            test.assertTrue(level.getBlockState(at)==pure(0)&&player.getEffect(ward).getDuration()==77&&player.getEffect(ward).getAmplifier()==2,"already-Warded actual player preserves SOURCE and existing effect exactly");
            player.removeEffect(ward);player.setGameMode(GameType.CREATIVE);KnowledgeAccess.get(player).setWarpPerm(0);player.move(MoverType.SELF,Vec3.ZERO);
            test.assertTrue(level.getBlockState(at).isAir()&&player.getEffect(ward)!=null&&player.getEffect(ward).getDuration()==32000,"actual creative player still receives original Ward and consumes its source");player.setGameMode(GameType.SURVIVAL);
            player.removeEffect(ward);player.setPos(Vec3.atCenterOf(base.above(5)));cell(base);
        }
        ItemEntity salt(BlockPos pos,String name){
            var stack=new ItemStack(ThaumcraftSpa.BATH_SALTS.get(),64);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));
            var entity=new ItemEntity(level,pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5,stack,0,0,0);entity.setNoGravity(true);entity.setPickUpDelay(32767);items.add(entity);
            test.assertTrue(level.addFreshEntity(entity),"actual native named64 Bath Salts stack is admitted");return entity;
        }
        void ordinaryTicks(){
            timerSource=base.offset(10,0,0);cell(timerSource);set(timerSource.below(),Blocks.AIR.defaultBlockState());set(timerSource.below(2),Blocks.STONE.defaultBlockState());set(timerSource,pure(1));set(timerSource,pure(0));
            test.assertTrue(level.getBlockTicks().hasScheduledTick(timerSource,ThaumcraftSpa.PURE_WATER.get()),"Classic native BLOCK scheduling survives an actual same-cell flowing1 to SOURCE0 replacement");
            saltSource=base.offset(20,0,0);cell(saltSource);set(saltSource.above(),Blocks.STONE.defaultBlockState());set(saltSource,Blocks.WATER.defaultBlockState());salt=salt(saltSource,"source-whole64");
            saltFlow=base.offset(30,0,0);cell(saltFlow);set(saltFlow.above(),Blocks.STONE.defaultBlockState());set(saltFlow,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1));set(saltFlow.east(),Blocks.WATER.defaultBlockState());set(saltFlow.east().below(),Blocks.STONE.defaultBlockState());set(saltFlow.east().above(),Blocks.STONE.defaultBlockState());
            for(Direction side:Direction.Plane.HORIZONTAL)if(side!=Direction.WEST)set(saltFlow.east().relative(side),Blocks.STONE.defaultBlockState());flowSalt=salt(saltFlow,"flowing-whole64");
            start=level.getGameTime();observer=world->{if(world!=level)return;
                if(firstDown<0&&level.getBlockState(timerSource.below()).is(ThaumcraftSpa.PURE_WATER.get()))firstDown=level.getGameTime();
                if(!salt.isRemoved())lastSourceSaltAge=salt.getAge();else if(sourceExpired<0)sourceExpired=level.getGameTime();
                if(!flowSalt.isRemoved())lastFlowSaltAge=flowSalt.getAge();else if(flowExpired<0)flowExpired=level.getGameTime();
            };TickEvent.SERVER_LEVEL_POST.register(observer);
            test.runAfterDelay(4,()->guard(()->test.assertTrue(firstDown<0&&level.getBlockState(timerSource.below()).isAir(),"ordinary native world ticks do not propagate before original tick5")));
            test.runAfterDelay(7,()->guard(()->test.assertTrue(firstDown-start==5&&level.getBlockState(timerSource.below())==pure(1),"real scheduled native fluid consumer first flows at exactly five world ticks; observed="+(firstDown-start))));
            test.runAfterDelay(190,()->guard(()->test.assertTrue(!salt.isRemoved()&&!flowSalt.isRemoved()&&salt.getAge()>=185&&salt.getAge()<200&&level.getBlockState(saltSource).is(Blocks.WATER)&&!level.getBlockState(saltFlow).is(ThaumcraftSpa.PURE_WATER.get()),"ordinary200-tick salt lifespan keeps both actual whole stacks before expiry")));
            test.runAfterDelay(207,()->guard(()->{
                test.assertTrue(salt.isRemoved()&&salt.getAge()==200&&lastSourceSaltAge==199&&sourceExpired-start==200&&level.getBlockState(saltSource)==pure(0),"real native salt ticks retire the whole named64 stack on tick200 and replace exactly one actual WATER source; age="+salt.getAge()+", elapsed="+(sourceExpired-start));
                test.assertTrue(flowSalt.isRemoved()&&flowSalt.getAge()==200&&lastFlowSaltAge==199&&flowExpired-start==200&&level.getBlockState(saltFlow).is(Blocks.WATER)&&level.getBlockState(saltFlow).getValue(LiquidBlock.LEVEL)>0,"real native flowing WATER fails original source-only expiry and yields no purifying fluid");
                int sources=0;for(BlockPos pos:BlockPos.betweenClosed(saltSource.offset(-2,-1,-2),saltSource.offset(2,1,2)))if(level.getBlockState(pos)==pure(0))sources++;
                test.assertTrue(sources==1&&salt.getItem().getCount()==64,"stack64 does not manufacture64 source blocks or refund/replace its retired physical item");finish();test.succeed();
            }));
        }
        void guard(Runnable work){try{work.run();}catch(RuntimeException|Error failure){finish();throw failure;}}
        void finish(){
            if(finished)return;finished=true;if(observer!=null)TickEvent.SERVER_LEVEL_POST.unregister(observer);for(var item:items)item.discard();
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(oldRandomTicks,level.getServer());
            for(var entry:originals.entrySet())level.setBlock(entry.getKey(),entry.getValue(),QUIET);for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
            try{if(player!=null&&level.getServer().getPlayerList().getPlayer(player.getUUID())==player)level.getServer().getPlayerList().remove(player);}finally{if(player!=null)player.discard();channel.finishAndReleaseAll();}
        }
    }
}

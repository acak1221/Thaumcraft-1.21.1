package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.lib.SpaRules;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.fluid.FluidPure;
import thaumcraft.common.lib.fluid.PureFlowRules;

/** Original BlockFluidPure and its actual Forge1.7 Classic8-quanta consumer. */
public final class BlockFluidPure extends LiquidBlock {
    private static final Codec<FlowingFluid> PURE_CODEC=BuiltInRegistries.FLUID.byNameCodec().comapFlatMap(
            fluid->fluid instanceof FluidPure pure?DataResult.success(pure):DataResult.error(()->"Purifying fluid required"),fluid->fluid);
    public static final MapCodec<LiquidBlock> CODEC=RecordCodecBuilder.<LiquidBlock>mapCodec(instance->instance.group(
            PURE_CODEC.fieldOf("fluid").forGetter(block->((BlockFluidPure)block).fluid),propertiesCodec()).apply(instance,BlockFluidPure::new));
    private static final Direction[] SIDES={Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH};
    private static final Map<ServerLevel,Set<BlockPos>> FLOWING=new IdentityHashMap<>(),CLAIMING=new IdentityHashMap<>();
    @FunctionalInterface public interface ClientEffects {void bubble(Level level,double x,double y,double z);}
    private static ClientEffects clientEffects=(level,x,y,z)->{};
    public BlockFluidPure(FlowingFluid fluid,Properties properties){super(fluid,properties.randomTicks().lightLevel(state->SpaRules.pureLight(state.getValue(LEVEL))));}
    public static void setClientEffects(ClientEffects effects){clientEffects=java.util.Objects.requireNonNull(effects);}
    @Override public MapCodec<LiquidBlock> codec(){return CODEC;}
    public static boolean readable(BlockGetter level,BlockPos pos){
        if(level instanceof ServerLevel server)return FluxFiniteBlock.loaded(server,pos);
        return !(level instanceof LevelReader reader)||!reader.isOutsideBuildHeight(pos)&&reader.hasChunk(pos.getX()>>4,pos.getZ()>>4);
    }
    private static boolean canonical(ServerLevel level){return level.getServer().isSameThread()&&level.getServer().getLevel(level.dimension())==level;}
    private static boolean pure(BlockState state){return state.getBlock() instanceof BlockFluidPure;}
    public static int quanta(BlockGetter level,BlockPos pos){
        if(!readable(level,pos))return -1;var state=level.getBlockState(pos);return state.isAir()?0:pure(state)?PureFlowRules.quanta(state.getValue(LEVEL)):-1;
    }
    private static boolean source(BlockGetter level,BlockPos pos){return readable(level,pos)&&pure(level.getBlockState(pos))&&level.getBlockState(pos).getValue(LEVEL)==0;}
    private static boolean same(BlockGetter level,BlockPos pos){return readable(level,pos)&&pure(level.getBlockState(pos));}
    private static boolean veto(BlockState state){return state.is(BlockTags.DOORS)||state.is(BlockTags.SIGNS)||state.is(Blocks.SUGAR_CANE);}
    private static boolean portal(BlockState state){return state.is(Blocks.NETHER_PORTAL)||state.is(Blocks.END_PORTAL)||state.is(Blocks.END_GATEWAY);}
    private static boolean oldThinSolid(BlockState state){return state.getBlock() instanceof net.minecraft.world.level.block.BasePressurePlateBlock||state.getBlock() instanceof net.minecraft.world.level.block.CakeBlock||state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock;}
    private static boolean oldThinNonSolid(BlockState state){return state.is(Blocks.SNOW)||state.getBlock() instanceof net.minecraft.world.level.block.LadderBlock;}
    /** Removed1.7 Material.isSolid, including material facts independent of modern collision volume. */
    public static boolean legacySolid(BlockState state){
        if(state.getBlock() instanceof FluxFiniteBlock||BlockTaintFibres.family(state)||oldThinNonSolid(state))return false;
        return oldThinSolid(state)||state.isSolid();
    }
    private static boolean blocksMovement(BlockState state){
        // MaterialTaint is non-solid but DOES block movement. Likewise original
        // plates/cake/open gates stay wood/rock/cake; ladders/snow stay circuits/snow.
        if(state.getBlock() instanceof FluxFiniteBlock||BlockTaintFibres.family(state))return true;
        if(oldThinNonSolid(state))return false;
        return oldThinSolid(state)||state.blocksMotion();
    }
    public static int legacyDensity(BlockState state){return state.getBlock() instanceof FluxFiniteBlock flux?flux.density():pure(state)?1000:Integer.MAX_VALUE;}
    public static boolean canDisplace(BlockGetter level,BlockPos pos){
        if(!readable(level,pos))return false;var state=level.getBlockState(pos);
        return PureFlowRules.canDisplace(state.isAir(),pure(state),veto(state),blocksMovement(state),portal(state),legacyDensity(state));
    }
    public static boolean canFlowInto(BlockGetter level,BlockPos pos){
        if(!readable(level,pos))return false;var state=level.getBlockState(pos);
        boolean liquid=state.getFluidState().is(FluidTags.WATER)||state.getFluidState().is(FluidTags.LAVA);
        return PureFlowRules.canFlowInto(state.isAir(),pure(state),veto(state),blocksMovement(state),portal(state),liquid,legacyDensity(state));
    }
    private static void schedule(LevelAccessor level,BlockPos pos,BlockState state){
        // Classic schedules its BLOCK, so a pending flow1 tick still runs if
        // a real Spa changes this same cell to source0 before its deadline.
        if(pure(state))level.scheduleTick(pos,state.getBlock(),PureFlowRules.TICK_DELAY);
    }
    @Override protected FluidState getFluidState(BlockState state){
        int metadata=state.getValue(LEVEL);
        return metadata==0?fluid.getSource(false):metadata<8?fluid.getFlowing(8-metadata,false):Fluids.EMPTY.defaultFluidState();
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.empty();}
    @Override protected boolean isRandomlyTicking(BlockState state){return true;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){flowTick(level,pos,state);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){flowTick(level,pos,state);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){if(!old.is(this))schedule(level,pos,state);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moved){schedule(level,pos,state);}
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState other,LevelAccessor level,BlockPos pos,BlockPos from){schedule(level,pos,state);return state;}
    /** Original FillBucketEvent captures only metadata0; flowing cells remain intact. */
    @Override public ItemStack pickupBlock(Player player,LevelAccessor level,BlockPos pos,BlockState state){return thaumcraft.common.lib.fluid.PureBucketActions.capture(player,level,pos,state);}
    @Override public java.util.Optional<net.minecraft.sounds.SoundEvent> getPickupSound(){return java.util.Optional.empty();}
    private static final class Step {
        final ServerLevel level;final BlockPos pos;BlockState expected;
        Step(ServerLevel level,BlockPos pos,BlockState state){this.level=level;this.pos=pos.immutable();expected=state;}
        boolean current(){return canonical(level)&&readable(level,pos)&&level.getBlockState(pos)==expected;}
        boolean setSource(BlockState state,int flags){if(!current())return false;expected=state;level.setBlock(pos,state,flags);return current();}
        void flow(BlockPos target,int metadata){
            if(metadata<0||!current()||!canDisplace(level,target))return;BlockState old=level.getBlockState(target);
            // The removed material enum must not silently destroy an addon's physical stock.
            var owner=level.getBlockEntity(target);
            if(owner instanceof Container inventory&&!inventory.isEmpty())return;
            if(!current()||level.getBlockState(target)!=old||level.getBlockEntity(target)!=owner)return;
            if(!old.isAir()&&legacyDensity(old)==Integer.MAX_VALUE)Block.dropResources(old,level,target,owner);
            if(current()&&readable(level,target)&&level.getBlockState(target)==old&&level.getBlockEntity(target)==owner)level.setBlock(target,ThaumcraftSpa.PURE_WATER.get().defaultBlockState().setValue(LEVEL,metadata),Block.UPDATE_ALL);
        }
    }
    public void flowTick(Level world,BlockPos pos,BlockState state){
        if(!(world instanceof ServerLevel level)||!canonical(level)||!readable(level,pos)||level.getBlockState(pos)!=state||!state.is(this))return;
        var active=FLOWING.computeIfAbsent(level,ignored->new HashSet<>());
        var footprint=new HashSet<BlockPos>();footprint.add(pos.immutable());for(Direction side:Direction.values())footprint.add(pos.relative(side));
        // Two source callbacks cannot both publish the same displaced plant.
        // This is only the seven actual immediately writable Classic cells.
        if(footprint.stream().anyMatch(active::contains))return;active.addAll(footprint);
        try{
            var step=new Step(level,pos,state);int remaining=PureFlowRules.quanta(state.getValue(LEVEL));
            if(remaining<8){
                boolean upper=same(level,pos.above());for(Direction side:SIDES)upper|=same(level,pos.above().relative(side));
                int expected=upper?7:PureFlowRules.expectedQuanta(false,quanta(level,pos.west()),quanta(level,pos.east()),quanta(level,pos.north()),quanta(level,pos.south()));
                if(expected!=remaining){
                    remaining=expected;
                    if(expected<=0){if(!step.setSource(Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return;}
                    else{if(!step.setSource(state.setValue(LEVEL,8-expected),Block.UPDATE_ALL))return;schedule(level,pos,step.expected);level.updateNeighborsAt(pos,this);if(!step.current())return;}
                }
            }else if(!step.setSource(state.setValue(LEVEL,0),Block.UPDATE_CLIENTS))return;
            // Intentionally continue after a starved cell removed itself: the
            // original Classic still attempts its downward metadata1 here.
            if(canDisplace(level,pos.below())){step.flow(pos.below(),1);return;}
            int metadata=8-remaining+1;if(metadata>=8||!step.current())return;
            boolean vertical=same(level,pos.below())||same(level,pos)&&canFlowInto(level,pos.below());
            if(source(level,pos)||!vertical){
                if(same(level,pos.above()))metadata=1;
                boolean[] directions=PureFlowRules.optimal(PureFlowRules.costs(new PureFlowRules.Grid(){
                    private BlockPos at(int x,int z){return new BlockPos(x,pos.getY(),z);}
                    public boolean canFlowInto(int x,int z){return BlockFluidPure.canFlowInto(level,at(x,z));}
                    public boolean source(int x,int z){return BlockFluidPure.source(level,at(x,z));}
                    public boolean canFlowDown(int x,int z){return BlockFluidPure.canFlowInto(level,at(x,z).below());}
                },pos.getX(),pos.getZ()));
                for(int side=0;side<4;side++)if(directions[side])step.flow(pos.relative(SIDES[side]),metadata);
            }
        }finally{active.removeAll(footprint);if(active.isEmpty())FLOWING.remove(level);}
    }
    /** Original normalized Classic flow; native water-tag physics owns swimming/drowning/pushing. */
    public static Vec3 flowVector(BlockGetter level,BlockPos pos){
        if(!readable(level,pos)||!pure(level.getBlockState(pos)))return Vec3.ZERO;
        int decay=8-quanta(level,pos);Vec3 flow=Vec3.ZERO;
        for(Direction direction:new Direction[]{Direction.WEST,Direction.NORTH,Direction.EAST,Direction.SOUTH}){
            BlockPos next=pos.relative(direction);if(!readable(level,next))continue;int otherDecay=8-quanta(level,next),power;
            if(otherDecay>=8){
                if(blocksMovement(level.getBlockState(next))||!readable(level,next.below()))continue;
                otherDecay=8-quanta(level,next.below());if(otherDecay<0)continue;power=otherDecay-(decay-8);
            }else{if(otherDecay<0)continue;power=otherDecay-decay;}
            flow=flow.add(direction.getStepX()*power,0,direction.getStepZ()*power);
        }
        if(same(level,pos.above())){
            boolean wall=false;for(Direction side:SIDES)for(int dy=0;dy<=1;dy++){var next=pos.relative(side).above(dy);if(readable(level,next)&&legacySolid(level.getBlockState(next)))wall=true;}
            if(wall)flow=flow.normalize().add(0,-6,0);
        }
        return flow.normalize();
    }
    @Override protected void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){
        if(!(world instanceof ServerLevel level)||!canonical(level)||!source(level,pos)||!(entity instanceof Player player)||player.level()!=level
                ||player.isRemoved()||level.getEntity(player.getUUID())!=player)return;
        var ward=ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD);if(player.hasEffect(ward))return;
        var active=CLAIMING.computeIfAbsent(level,ignored->new HashSet<>());if(!active.add(pos.immutable()))return;
        try{
            int duration=SpaRules.warpWardDuration(KnowledgeAccess.get(player).getWarpPerm());
            if(!canonical(level)||!readable(level,pos)||level.getBlockState(pos)!=state||!source(level,pos)||player.hasEffect(ward)||player.level()!=level||player.isRemoved()||level.getEntity(player.getUUID())!=player)return;
            player.addEffect(new MobEffectInstance(ward,duration,0,true,true));
            if(canonical(level)&&readable(level,pos)&&level.getBlockState(pos)==state)level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        }finally{active.remove(pos);if(active.isEmpty())CLAIMING.remove(level);}
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        int metadata=state.getValue(LEVEL);
        clientEffects.bubble(level,(float)pos.getX()+random.nextFloat(),(float)pos.getY()+.125F*(8-metadata),(float)pos.getZ()+random.nextFloat());
        if(random.nextInt(25)==0){double x=(float)pos.getX()+random.nextFloat(),y=pos.getY()+1D,z=(float)pos.getZ()+random.nextFloat();
            level.playLocalSound(x,y,z,SoundEvents.LAVA_POP,SoundSource.BLOCKS,.1F+random.nextFloat()*.1F,.9F+random.nextFloat()*.15F,false);}
    }
}

package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintSettings;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.monster.EntityTaintSporeSwarmer;
import thaumcraft.common.lib.world.TaintBlockRules;
import thaumcraft.common.lib.world.TaintMaterialRules;

/** Original crust0/soil1 active ecology. Existing flesh_block remains the canonical inert metadata2. */
public final class BlockTaint extends Block {
    public static final MapCodec<BlockTaint> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            propertiesCodec(),Codec.intRange(0,2).fieldOf("metadata").forGetter(BlockTaint::metadata)).apply(instance,BlockTaint::new));
    private static final Direction[] HORIZONTAL={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final SoundEvent ROOTS=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","roots"));
    private static final Map<ServerLevel,Set<BlockPos>> ACTIVE=new IdentityHashMap<>();
    @FunctionalInterface public interface ClientEffects {void droplet(Level level,Vec3 position);}
    private static ClientEffects clientEffects=(level,position)->{};
    private final int metadata;
    public BlockTaint(Properties properties,int metadata){super(properties.strength(TaintBlockRules.taintHardness(metadata),TaintBlockRules.TAINT_RESISTANCE));if(metadata<0||metadata>2)throw new IllegalArgumentException("Invalid original taint metadata");this.metadata=metadata;}
    public int metadata(){return metadata;}
    public static int metadata(BlockState state){return state.getBlock() instanceof BlockTaint taint?taint.metadata:state.is(ThaumcraftTaint.FLESH.get())?2:-1;}
    public static boolean solidFamily(BlockState state){return metadata(state)>=0;}
    public static void setClientEffects(ClientEffects effects){clientEffects=java.util.Objects.requireNonNull(effects);}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){work(state,level,pos,random);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){work(state,level,pos,random);}
    private void work(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(metadata==2||!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level||!BlockTaintFibres.loaded(level,pos)||level.getBlockState(pos)!=state)return;
        Set<BlockPos> active=ACTIVE.computeIfAbsent(level,ignored->new HashSet<>());if(!active.add(pos.immutable()))return;
        try {
            BlockTaintFibres.taintBiomeSpread(level,pos.getX(),pos.getY(),pos.getZ(),random,this);
            if(level.getBlockState(pos)!=state)return;
            if(metadata==0) {
                if(tryToFall(level,pos,pos,state))return;
                if(BlockTaintFibres.loaded(level,pos.above())&&level.isEmptyBlock(pos.above())) {
                    Direction direction=Direction.from3DDataValue(2+random.nextInt(4));boolean open=true;
                    for(int depth=0;depth<4;depth++) {
                        BlockPos column=pos.below(depth),side=column.relative(direction);
                        if(!BlockTaintFibres.loaded(level,column)||!BlockTaintFibres.loaded(level,side)||!level.isEmptyBlock(side)||!solidFamily(level.getBlockState(column))){open=false;break;}
                    }
                    if(open&&tryToFall(level,pos,pos.relative(direction),state))return;
                }
            }
            BlockPos target=pos.offset(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
            if(!BlockTaintFibres.loaded(level,target))return;
            if(BlockTaintFibres.isTaintBiome(level,target)) {
                BlockTaintFibres.spreadFibres(level,target.getX(),target.getY(),target.getZ());
                if(metadata==0) {
                    if(ThaumcraftTaintSettings.spawnTaintSpore()&&BlockTaintFibres.loaded(level,pos.above())&&level.isEmptyBlock(pos.above())&&random.nextInt(200)==0) {
                        if(level.getEntitiesOfClass(EntityTaintSporeSwarmer.class,new AABB(pos).inflate(16D)).isEmpty()&&level.getBlockState(pos)==state) {
                            level.removeBlock(pos,false);var spore=ThaumcraftTaint.TAINT_SWARMER.get().create(level);if(spore==null)throw new IllegalStateException("Missing real taint swarmer type");
                            spore.moveTo((double)((float)pos.getX()+.5F),pos.getY(),(double)((float)pos.getZ()+.5F),0,0);level.addFreshEntity(spore);level.playSound(null,spore,ROOTS,SoundSource.BLOCKS,.1F,.9F+level.random.nextFloat()*.2F);
                        }
                    } else if(sealed(level,pos)&&level.getBlockState(pos)==state)makeGoo(level,pos);
                }
            } else if(metadata==0&&random.nextInt(20)==0&&level.getBlockState(pos)==state)makeGoo(level,pos);
            else if(metadata==1&&random.nextInt(10)==0&&level.getBlockState(pos)==state)level.setBlock(pos,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
        } finally {active.remove(pos);if(active.isEmpty())ACTIVE.remove(level);}
    }
    private static boolean sealed(ServerLevel level,BlockPos pos) {
        if(!BlockTaintFibres.loaded(level,pos.above())||!solidFamily(level.getBlockState(pos.above())))return false;
        for(Direction side:HORIZONTAL){BlockPos next=pos.relative(side);if(!BlockTaintFibres.loaded(level,next)||!solidFamily(level.getBlockState(next)))return false;}return true;
    }
    private static void makeGoo(ServerLevel level,BlockPos pos) {
        var goo=ThaumcraftFlux.FLUX_GOO.get();var state=goo.defaultBlockState().setValue(FluxFiniteBlock.LEVEL,FluxFiniteBlock.QUANTA);
        level.setBlock(pos,goo.withGeometry(state,level,pos),Block.UPDATE_ALL); // Original getQuanta()==8 is exceptional NINE-unit metadata8.
    }
    public static boolean canFallBelow(ServerLevel level,BlockPos pos) {
        if(!BlockTaintFibres.loaded(level,pos))return false;
        for(BlockPos around:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1))) {
            if(!BlockTaintFibres.loaded(level,around)||TaintMaterialRules.log(level.getBlockState(around)))return false;
        }
        BlockState state=level.getBlockState(pos);
        if(state.isAir())return true;
        if(state.is(ThaumcraftFlux.FLUX_GOO.get())&&state.getValue(FluxFiniteBlock.LEVEL)>=4)return false;
        return state.is(Blocks.FIRE)||state.getBlock() instanceof BlockTaintFibres||state.canBeReplaced()||state.liquid();
    }
    private static boolean tryToFall(ServerLevel level,BlockPos source,BlockPos start,BlockState expected) {
        if(start.getY()<level.getMinBuildHeight()||!canFallBelow(level,start.below())||level.getBlockState(source)!=expected)return false;
        if(level.hasChunksAt(start.offset(-32,-32,-32),start.offset(32,32,32))){EntityFallingTaint.start(level,source,start);return true;}
        // Preserve the original dry-fallback coordinate quirk: it writes SOURCE, not computed landing.
        if(level.getBlockState(source)!=expected)return false;
        level.removeBlock(source,false);
        BlockPos landing=start;
        while(landing.getY()>level.getMinBuildHeight()&&canFallBelow(level,landing.below()))landing=landing.below();
        if(landing.getY()>level.getMinBuildHeight()&&level.getBlockState(source).isAir())level.setBlock(source,expected,Block.UPDATE_ALL);
        return false;
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){if(metadata!=2)BlockTaintFibres.poison(level,entity,false);super.stepOn(level,pos,state,entity);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(metadata==0&&BlockTaintFibres.loaded(level,pos.below())&&level.isEmptyBlock(pos.below())&&random.nextInt(10)==0)
            clientEffects.droplet(level,new Vec3((float)pos.getX()+.1F+level.random.nextFloat()*.8F,pos.getY(),(float)pos.getZ()+.1F+level.random.nextFloat()*.8F));
    }
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int event,int value) {
        if(event==1){if(level.isClientSide)level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),ROOTS,SoundSource.BLOCKS,.1F,.9F+level.random.nextFloat()*.2F,false);return true;}
        return super.triggerEvent(state,level,pos,event,value);
    }
}

package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintSettings;
import thaumcraft.common.entities.monster.EntityTaintSpore;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.TaintSpreadRules;
import thaumcraft.common.lib.world.TaintMaterialRules;

/** Original five fibre states, physical support, free taint ecology and native spore-stalk lifecycle. */
public final class BlockTaintFibres extends Block {
    public static final MapCodec<BlockTaintFibres> CODEC=simpleCodec(BlockTaintFibres::new);
    public static final IntegerProperty TYPE=IntegerProperty.create("type",0,4);
    private static final Direction[] ORDER={Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final SoundEvent ROOTS=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","roots"));
    private static final Map<ServerLevel,Set<BlockPos>> ACTIVE=new IdentityHashMap<>();
    public BlockTaintFibres(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(TYPE,0));}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(TYPE);}
    public static boolean loaded(BlockGetter world,BlockPos pos) {
        if(world instanceof ServerLevel server)return FluxFiniteBlock.loaded(server,pos);
        return !(world instanceof LevelReader reader)||!reader.isOutsideBuildHeight(pos)&&reader.hasChunk(pos.getX()>>4,pos.getZ()>>4);
    }
    public static boolean family(BlockState state){return BlockTaint.solidFamily(state)||state.getBlock() instanceof BlockTaintFibres;}
    public static boolean isTaintBiome(Level world,BlockPos pos) {
        if(!loaded(world,pos))return false;
        if(world instanceof ServerLevel server){var biome=BiomeColumns.surfaceHolder(server,pos.getX(),pos.getZ());return biome!=null&&biome.is(ThaumcraftBiomes.TAINT);}
        var chunk=world.getChunk(pos);int y=Mth.clamp(chunk.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX()&15,pos.getZ()&15),world.getMinBuildHeight(),world.getMaxBuildHeight()-1);
        return chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(y),QuartPos.fromBlock(pos.getZ())).is(ThaumcraftBiomes.TAINT);
    }
    public static int getAdjacentTaint(BlockGetter world,int x,int y,int z) {
        int count=0;BlockPos pos=new BlockPos(x,y,z);for(Direction side:ORDER){var other=pos.relative(side);if(loaded(world,other)&&family(world.getBlockState(other)))count++;}return count;
    }
    public static boolean isOnlyAdjacentToTaint(Level world,int x,int y,int z) {
        BlockPos pos=new BlockPos(x,y,z);for(Direction side:ORDER){var other=pos.relative(side);if(!loaded(world,other))return false;var state=world.getBlockState(other);if(!state.isAir()&&!family(state))return false;}return true;
    }
    public static boolean sturdy(BlockGetter world,BlockPos pos,Direction face){return loaded(world,pos)&&world.getBlockState(pos).isFaceSturdy(world,pos,face);}
    private static boolean allNeighboursLoaded(Level world,BlockPos pos){for(Direction side:ORDER)if(!loaded(world,pos.relative(side)))return false;return true;}
    private static boolean anySupport(Level world,BlockPos pos){for(Direction side:ORDER)if(sturdy(world,pos.relative(side),side.getOpposite()))return true;return false;}
    private static boolean mutable(ServerLevel world,BlockPos pos,BlockState expected) {
        // Native containers with physical stock must not lose it merely because a modern tag calls them replaceable/logs.
        if(world.getBlockEntity(pos) instanceof Container container&&!container.isEmpty())return false;
        return loaded(world,pos)&&world.getBlockState(pos)==expected;
    }
    public static boolean spreadFibres(ServerLevel world,int x,int y,int z) {
        if(!world.getServer().isSameThread()||world.getServer().getLevel(world.dimension())!=world)return false;
        BlockPos pos=new BlockPos(x,y,z);if(!loaded(world,pos))return false;BlockState old=world.getBlockState(pos);
        if(!allNeighboursLoaded(world,pos)||!anySupport(world,pos)||isOnlyAdjacentToTaint(world,x,y,z)||old.liquid()
                ||!(old.isAir()||old.canBeReplaced()||old.getBlock() instanceof FlowerBlock||old.is(BlockTags.LEAVES))||!mutable(world,pos,old))return false;
        int type=TaintSpreadRules.fibreType(world.random::nextInt,()->loaded(world,pos.above())&&world.isEmptyBlock(pos.above()),()->sturdy(world,pos.below(),Direction.UP));
        if(!mutable(world,pos,old))return false;
        world.setBlock(pos,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(TYPE,type),Block.UPDATE_ALL);
        world.blockEvent(pos,ThaumcraftTaint.FIBRES.get(),1,0);return true;
    }
    public static void taintBiomeSpread(ServerLevel world,int x,int y,int z,RandomSource random,Block block) {
        int rate=ThaumcraftTaintSettings.taintSpreadRate();if(rate<=0)return;
        int dx=random.nextInt(3)-1,dz=random.nextInt(3)-1;var candidate=new BlockPos(x+dx,y,z+dz);
        if(!loaded(world,candidate))return;var biome=BiomeColumns.surfaceHolder(world,candidate.getX(),candidate.getZ());if(biome==null)return;
        if(!biome.is(ThaumcraftBiomes.TAINT)&&TaintSpreadRules.biomeChance(rate,random::nextInt,random::nextLong)&&getAdjacentTaint(world,x,y,z)>=2) {
            BiomeColumns.set(world,candidate.getX(),candidate.getZ(),ThaumcraftBiomes.TAINT);world.blockEvent(new BlockPos(x,y,z),block,1,0);
        }
    }
    @Override protected void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random){work(state,world,pos,random);}
    @Override protected void tick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random){work(state,world,pos,random);}
    private void work(BlockState state,ServerLevel world,BlockPos pos,RandomSource random) {
        if(!world.getServer().isSameThread()||world.getServer().getLevel(world.dimension())!=world||!loaded(world,pos)||world.getBlockState(pos)!=state)return;
        Set<BlockPos> active=ACTIVE.computeIfAbsent(world,ignored->new HashSet<>());if(!active.add(pos.immutable()))return;
        try {
            int type=state.getValue(TYPE);taintBiomeSpread(world,pos.getX(),pos.getY(),pos.getZ(),random,this);
            if(world.getBlockState(pos)!=state)return;
            if(type==0&&isOnlyAdjacentToTaint(world,pos.getX(),pos.getY(),pos.getZ())||!isTaintBiome(world,pos)){world.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);return;}
            BlockPos target=pos.offset(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
            if(!loaded(world,target)||!isTaintBiome(world,target))return;
            BlockState original=world.getBlockState(target);
            if(spreadFibres(world,target.getX(),target.getY(),target.getZ()))return;
            int adjacent=getAdjacentTaint(world,target.getX(),target.getY(),target.getZ());
            if(adjacent>=2&&TaintMaterialRules.crust(original)&&mutable(world,target,original)) {
                world.setBlock(target,ThaumcraftTaint.CRUST.get().defaultBlockState(),Block.UPDATE_ALL);world.blockEvent(target,ThaumcraftTaint.CRUST.get(),1,0);
            }
            if(adjacent>=3&&!original.isAir()&&TaintMaterialRules.soil(original)&&loaded(world,target)) {
                var now=world.getBlockState(target);if((now==original||now.is(ThaumcraftTaint.CRUST.get()))&&mutable(world,target,now)) {
                    world.setBlock(target,ThaumcraftTaint.SOIL.get().defaultBlockState(),Block.UPDATE_ALL);world.blockEvent(target,ThaumcraftTaint.SOIL.get(),1,0);
                }
            }
            if(type==3&&ThaumcraftTaintSettings.spawnTaintSpore()&&random.nextInt(10)==0&&loaded(world,pos.above())&&world.isEmptyBlock(pos.above())) {
                changeType(world,pos,4);var spore=ThaumcraftTaint.TAINT_SPORE.get().create(world);if(spore==null)throw new IllegalStateException("Missing real taint spore type");
                spore.moveTo((double)((float)pos.getX()+.5F),pos.getY()+1D,(double)((float)pos.getZ()+.5F),0,0);world.addFreshEntity(spore);
            } else if(type==4&&world.getEntitiesOfClass(EntityTaintSpore.class,new AABB(pos.above())).isEmpty())changeType(world,pos,3);
        } finally {active.remove(pos);if(active.isEmpty())ACTIVE.remove(world);}
    }
    public static void changeType(ServerLevel world,BlockPos pos,int type){var state=world.getBlockState(pos);if(state.getBlock() instanceof BlockTaintFibres)world.setBlock(pos,state.setValue(TYPE,type),Block.UPDATE_ALL);}
    @Override protected void neighborChanged(BlockState state,Level world,BlockPos pos,Block changed,BlockPos from,boolean moved) {
        if(!world.isClientSide&&world.getBlockState(pos).is(this)&&isOnlyAdjacentToTaint(world,pos.getX(),pos.getY(),pos.getZ()))world.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
        super.neighborChanged(state,world,pos,changed,from,moved);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return isTaintBiome(context.getLevel(),context.getClickedPos())?defaultBlockState():null;}
    @Override public ItemStack getCloneItemStack(LevelReader world,BlockPos pos,BlockState state){return new ItemStack(ThaumcraftTaint.fibreItem(state.getValue(TYPE)));}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter world,BlockPos pos){return Shapes.empty();}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context) {
        if(state.getValue(TYPE)!=0)return Shapes.box((double).2F,0,(double).2F,(double).8F,(double).8F,(double).8F);
        for(Direction side:ORDER)if(sturdy(world,pos.relative(side),side.getOpposite()))return switch(side) {
            case DOWN->Shapes.box(0,0,0,1,.0625D,1);case UP->Shapes.box(0,.9375D,0,1,1,1);case NORTH->Shapes.box(0,0,0,1,1,.0625D);case SOUTH->Shapes.box(0,0,.9375D,1,1,1);case WEST->Shapes.box(0,0,0,.0625D,1,1);case EAST->Shapes.box(.9375D,0,0,1,1,1);};
        return Shapes.box(0,0,0,1,.0625D,1);
    }
    public static void poison(Level world,Entity entity,boolean fibres) {
        if(world.isClientSide||!(entity instanceof LivingEntity living)||living.getType().is(EntityTypeTags.UNDEAD))return;
        boolean player=entity instanceof Player;
        if(world.random.nextInt(TaintSpreadRules.poisonBound(fibres,player))==0)living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()),TaintSpreadRules.poisonDuration(player),0,false,true));
    }
    @Override protected void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){poison(world,entity,true);}
    @Override protected boolean triggerEvent(BlockState state,Level world,BlockPos pos,int event,int value) {
        if(event==1){if(world.isClientSide)world.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),ROOTS,SoundSource.BLOCKS,.1F,.9F+world.random.nextFloat()*.2F,false);return true;}
        return super.triggerEvent(state,world,pos,event,value);
    }
}

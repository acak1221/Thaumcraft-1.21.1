package thaumcraft.common.lib.world;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.BlockShockTrap;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNode.NodeModifier;
import thaumcraft.common.tiles.TileNode.NodeType;

/** Original createRandomNodeAt's shared all-type plan, used by natural nodes, transactional Silverwood and primal impacts. */
public final class NodeGeneration {
    public record Plan(AspectList aspects,NodeType type,NodeModifier modifier){
        public Plan{aspects=Objects.requireNonNull(aspects).copy();Objects.requireNonNull(type);}
        @Override public AspectList aspects(){return aspects.copy();}
    }
    private NodeGeneration(){}
    public static Plan plan(Holder<Biome> surfaceBiome,BlockPos origin,RandomSource random,boolean silverwood,boolean eerie,boolean small,Function<BlockPos,BlockState> environment){
        var biome=NormalNodeBiomeAdapter.describe(surfaceBiome);
        return plan(biome.aura(),biome::randomTag,surfaceBiome.is(ThaumcraftBiomes.TAINT),origin,random,silverwood,eerie,small,environment);
    }
    /** Registry-independent input reducer also used by exact RNG/quarter-aura checks. Null environment samples mean unavailable. */
    static Plan plan(int biomeAura,Function<RandomSource,Aspect> biomeTag,boolean taintedBiome,BlockPos origin,RandomSource random,
                     boolean silverwood,boolean eerie,boolean small,Function<BlockPos,BlockState> environment){
        NodeType type=NodeType.NORMAL;
        if(silverwood)type=NodeType.PURE;
        else if(eerie)type=NodeType.DARK;
        else if(random.nextInt(18)==0){
            int choice=random.nextInt(10);type=choice<3?NodeType.DARK:choice<6?NodeType.UNSTABLE:choice<9?NodeType.PURE:NodeType.HUNGRY;
        }
        NodeModifier modifier=null;if(random.nextInt(9)==0)modifier=NodeModifier.values()[random.nextInt(3)];
        int aura=biomeAura;
        if(type!=NodeType.PURE&&taintedBiome){aura=(int)(aura*1.5F);if(random.nextBoolean()){type=NodeType.TAINTED;aura=(int)(aura*1.5F);}}
        if(silverwood||small)aura/=4;
        int value=random.nextInt(aura/2)+aura/2;
        AspectList aspects=new AspectList();Aspect tag=biomeTag.apply(random);
        var primal=Aspect.getPrimalAspects();var compound=Aspect.getCompoundAspects();
        if(tag!=null)aspects.add(tag,2);
        else{aspects.add(compound.get(random.nextInt(compound.size())),1);aspects.add(primal.get(random.nextInt(primal.size())),1);}
        for(int i=0;i<3;i++)if(random.nextBoolean()){
            var pool=random.nextInt(18)==0?compound:primal;aspects.merge(pool.get(random.nextInt(pool.size())),1);
        }
        if(type==NodeType.HUNGRY){aspects.merge(Aspect.HUNGER,2);if(random.nextBoolean())aspects.merge(Aspect.GREED,1);}
        else if(type==NodeType.PURE)aspects.merge(random.nextBoolean()?Aspect.LIFE:Aspect.ORDER,2);
        else if(type==NodeType.DARK){
            if(random.nextBoolean())aspects.merge(Aspect.DEATH,1);if(random.nextBoolean())aspects.merge(Aspect.UNDEAD,1);
            if(random.nextBoolean())aspects.merge(Aspect.ENTROPY,1);if(random.nextBoolean())aspects.merge(Aspect.DARKNESS,1);
        }
        int water=0,lava=0,stone=0,foliage=0;
        for(int x=-5;x<=5;x++)for(int y=-5;y<=5;y++)for(int z=-5;z<=5;z++){
            BlockState state=environment.apply(origin.offset(x,y,z));if(state==null)continue;
            if(state.getFluidState().is(FluidTags.WATER))++water;
            else if(state.getFluidState().is(FluidTags.LAVA))++lava;
            else if(state.is(Blocks.STONE))++stone;
            if(state.is(BlockTags.LEAVES))++foliage;
        }
        if(water>100)aspects.merge(Aspect.WATER,1);if(lava>100){aspects.merge(Aspect.FIRE,1);aspects.merge(Aspect.EARTH,1);}
        if(stone>500)aspects.merge(Aspect.EARTH,1);if(foliage>100)aspects.merge(Aspect.PLANT,1);
        var sorted=aspects.getAspectsSorted();int[] spread=new int[sorted.length];float total=0;
        for(int i=0;i<spread.length;i++){spread[i]=aspects.getAmount(sorted[i])==2?50+random.nextInt(25):25+random.nextInt(50);total+=spread[i];}
        for(int i=0;i<spread.length;i++)aspects.merge(sorted[i],(int)(spread[i]/total*value));
        return new Plan(aspects,type,modifier);
    }
    public static boolean createRandomNodeAt(WorldGenLevel level,BlockPos origin,RandomSource random,boolean silverwood,boolean eerie,boolean small){
        if(level instanceof ServerLevel server&&!server.getServer().isSameThread())throw new IllegalStateException("Runtime node generation requires the server thread");
        if(!available(level,origin)||!level.ensureCanWrite(origin))return false;
        var biome=BiomeColumns.surfaceHolder(level,origin.getX(),origin.getZ());if(biome==null)return false;
        // Original consumes the COMPLETE generation roll even when the final position is occupied.
        Plan plan=plan(biome,origin,random,silverwood,eerie,small,pos->available(level,pos)?level.getBlockState(pos):null);
        BlockState current=level.getBlockState(origin);
        if(current.isAir()||current.getBlock() instanceof BlockShockTrap){
            if(!level.setBlock(origin,ThaumcraftContent.AURA_NODE.get().defaultBlockState(),0))return false;
        }
        if(!(level.getBlockEntity(origin) instanceof TileNode node))return false;
        if(!node.tryInitializeGenerated(plan.aspects(),plan.type(),plan.modifier(),level))return false;
        if(level instanceof ServerLevel server){BlockState state=server.getBlockState(origin);server.sendBlockUpdated(origin,state,state,Block.UPDATE_CLIENTS);}
        return true; // Generation regions publish their actual chunk state through the eventual native chunk packet.
    }
    private static boolean available(WorldGenLevel level,BlockPos pos){
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return false;
        return !(level instanceof ServerLevel server)||server.getWorldBorder().isWithinBounds(pos)&&server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
}

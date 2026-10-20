package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftTaint;

/** Original ordered biome decoration. Native base terrain/ore/cave/spring
 * features keep1.21's terrain palette/heights; their separate feature seeds
 * replace the removed1.7 all-in-one ore+vegetation seed schedule. All TC4
 * prefix/tree/pod/fibre counters and own branch draws remain ordered here. */
public final class ThaumcraftBiomeDecorationFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceLocation POD_GROWTH=ResourceLocation.fromNamespaceAndPath("thaumcraft","mana_pod_growth");
    public ThaumcraftBiomeDecorationFeature() {super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level=context.level();int x=context.origin().getX()&~15,z=context.origin().getZ()&~15;
        var biome=BiomeColumns.surfaceHolder(level,x+8,z+8);if(biome==null)return false;
        int kind=biome.is(ThaumcraftBiomes.MAGICAL_FOREST)?0:biome.is(ThaumcraftBiomes.EERIE)?1:biome.is(ThaumcraftBiomes.TAINT)?2:-1;if(kind<0)return false;
        RandomSource random=context.random();
        if(kind==0){
            int blobs=random.nextInt(3);for(int i=0;i<blobs;i++){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;OriginalBiomeVegetation.blob(level,random,top(level,xx,zz),Blocks.MOSSY_COBBLESTONE.defaultBlockState());}
            for(int gx=0;gx<4;gx++)for(int gz=0;gz<4;gz++){
                int xx=x+gx*4+1+8+random.nextInt(3),zz=z+gz*4+1+8+random.nextInt(3);BlockPos pos=top(level,xx,zz);
                if(random.nextInt(40)==0)OriginalBiomeVegetation.hugeMushroom(level,random,pos);
            }
        }
        decorateBase(context,x,z,kind);
        if(kind==0){
            // OriginalTileManaPod.checkGrowth used a distinct WORLD stream.
            // Native worker generation has a separate positional stream; it
            // never touches the live ServerLevel RNG or thread-confined BEs.
            RandomSource growth=level.getLevel().getChunkSource().randomState().getOrCreateRandomFactory(POD_GROWTH).at(x,0,z);
            var pods=new WorldGenManaPods();for(int i=0;i<10;i++){
                int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;pods.generate(level,random,growth,new BlockPos(xx,64,zz));
            }
            for(int i=0;i<8;i++){
                int xx=x+random.nextInt(16),zz=z+random.nextInt(16);BlockPos pos=top(level,xx,zz);
                while(pos.getY()>50&&OriginalBiomeVegetation.available(level,pos)&&!level.getBlockState(pos).is(Blocks.GRASS_BLOCK))pos=pos.below();
                if(!OriginalBiomeVegetation.available(level,pos.above())||!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)||!level.getBlockState(pos.above()).canBeReplaced())continue;
                if(adjacentWood(level,pos.above()))level.setBlock(pos.above(),ThaumcraftBiomes.VISHROOM.get().defaultBlockState(),2);
            }
        } else if(kind==2)decorateTaint(level,random,x,z);
        return true;
    }
    private static void decorateBase(FeaturePlaceContext<NoneFeatureConfiguration> context,int x,int z,int kind) {
        var level=context.level();var random=context.random();
        // Original sediment pass: sand3,clay1,gravel1, before tree counts.
        for(int i=0;i<3;i++){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;OriginalBiomeVegetation.sediment(level,random,ground(level,xx,zz),Blocks.SAND.defaultBlockState(),7,false);}
        {int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;OriginalBiomeVegetation.sediment(level,random,ground(level,xx,zz),Blocks.CLAY.defaultBlockState(),4,true);}
        {int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;OriginalBiomeVegetation.sediment(level,random,ground(level,xx,zz),Blocks.GRAVEL.defaultBlockState(),6,false);}
        int trees=2;if(random.nextInt(10)==0)++trees;
        for(int i=0;i<trees;i++){
            int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;BlockPos pos=top(level,xx,zz);
            if(kind==0){
                if(random.nextInt(14)==0)new WorldGenSilverwoodTrees(false,8,5).generate(level,random,pos);
                else if(random.nextInt(10)==0)new WorldGenGreatwoodTrees(false).generate(level,random,pos,random.nextInt(8)==0);
                else new WorldGenBigMagicTree(false).generate(level,random,pos);
            } else if(kind==2&&random.nextInt(8)==0)new WorldGenBigMagicTree(false,true).generate(level,random,pos);
            else {
                // Retain current vanilla oak/fancy-oak geometry for original
                // base-biome vanilla trees; no substitute for TC4 BigMagic.
                var key=random.nextInt(10)==0?TreeFeatures.FANCY_OAK:TreeFeatures.OAK;
                level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolderOrThrow(key).value().place(level,context.chunkGenerator(),random,pos);
            }
        }
        int flowers=kind==0?10:kind==1?1:0;
        for(int i=0;i<flowers;i++){
            int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)+32);
            // Original pickRandomFlower is inherited:2/3 dandelion,1/3 poppy.
            // MagicalForest's red nine-flower list is BONEMEAL-only in Forge.
            var flower=random.nextInt(3)>0?Blocks.DANDELION:Blocks.POPPY;
            OriginalBiomeVegetation.patch(level,random,new BlockPos(xx,yy,zz),flower.defaultBlockState(),64,false);
        }
        int grasses=kind==0?12:2;
        for(int i=0;i<grasses;i++){
            int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);
            var grass=kind==0&&random.nextInt(4)==0?Blocks.FERN:Blocks.SHORT_GRASS;
            OriginalBiomeVegetation.patch(level,random,new BlockPos(xx,yy,zz),grass.defaultBlockState(),128,true);
        }
        int lilies=kind==0?6:0;
        for(int i=0;i<lilies;i++){
            int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);
            while(yy>level.getMinBuildHeight()&&OriginalBiomeVegetation.available(level,new BlockPos(xx,yy-1,zz))&&level.isEmptyBlock(new BlockPos(xx,yy-1,zz)))--yy;
            OriginalBiomeVegetation.lily(level,random,new BlockPos(xx,yy,zz));
        }
        int mushrooms=kind==0?6:0;
        for(int i=0;i<mushrooms;i++){
            if(random.nextInt(4)==0){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;OriginalBiomeVegetation.patch(level,random,top(level,xx,zz),Blocks.BROWN_MUSHROOM.defaultBlockState(),64,false);}
            if(random.nextInt(8)==0){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);OriginalBiomeVegetation.patch(level,random,new BlockPos(xx,yy,zz),Blocks.RED_MUSHROOM.defaultBlockState(),64,false);}
        }
        if(random.nextInt(4)==0){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);OriginalBiomeVegetation.patch(level,random,new BlockPos(xx,yy,zz),Blocks.BROWN_MUSHROOM.defaultBlockState(),64,false);}
        if(random.nextInt(8)==0){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);OriginalBiomeVegetation.patch(level,random,new BlockPos(xx,yy,zz),Blocks.RED_MUSHROOM.defaultBlockState(),64,false);}
        // Taint's negative configured reeds count does NOT suppress the ten
        // additional inherited attempts. Preserve that original quirk.
        for(int i=0;i<10;i++){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);OriginalBiomeVegetation.reeds(level,random,new BlockPos(xx,yy,zz));}
        if(random.nextInt(32)==0){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8,yy=nextInt(random,height(level,xx,zz)*2);OriginalBiomeVegetation.pumpkin(level,random,new BlockPos(xx,yy,zz));}
    }
    private static void decorateTaint(WorldGenLevel level,RandomSource random,int x,int z) {
        int blobs=random.nextInt(3);for(int i=0;i<blobs;i++){int xx=x+random.nextInt(16)+8,zz=z+random.nextInt(16)+8;OriginalBiomeVegetation.blob(level,random,top(level,xx,zz),ThaumcraftTaint.CRUST.get().defaultBlockState());}
        for(int i=0;i<10;i++){
            int xx=x+random.nextInt(16),zz=z+random.nextInt(16);BlockPos pos=top(level,xx,zz).below();if(!OriginalBiomeVegetation.available(level,pos))continue;var state=level.getBlockState(pos);
            if(state.isAir())continue;
            if(state.is(Blocks.GRASS_BLOCK)&&OriginalBiomeVegetation.available(level,pos.above()))level.setBlock(pos.above(),ThaumcraftTaint.FIBRES.get().defaultBlockState(),2);
            else if(state.canBeReplaced()&&level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK))level.setBlock(pos,ThaumcraftTaint.FIBRES.get().defaultBlockState(),2);
        }
        for(int i=0;i<8;i++){
            int xx=x+random.nextInt(16),zz=z+random.nextInt(16);BlockPos pos=null;
            // Original Utils startsY10 and accidentally loops forever above250.
            // Refuse an exhausted bounded scan rather than stalling generation.
            for(int yy=10;yy<=250&&yy+1<level.getMaxBuildHeight();yy++){
                var candidate=new BlockPos(xx,yy+1,zz);if(!OriginalBiomeVegetation.available(level,candidate))break;
                if(level.isEmptyBlock(candidate)){pos=candidate;break;}
            }
            if(pos==null)continue;
            var biome=BiomeColumns.surfaceHolder(level,xx,zz);if(biome!=null&&!biome.is(ThaumcraftBiomes.TAINT))BiomeColumns.set(level,xx,zz,ThaumcraftBiomes.TAINT);
            if(!level.isEmptyBlock(pos))continue;
            boolean solid=false;for(Direction direction:Direction.values()){
                var neighbor=pos.relative(direction);if(OriginalBiomeVegetation.available(level,neighbor)&&level.getBlockState(neighbor).isFaceSturdy(level,neighbor,direction.getOpposite())){solid=true;break;}
            }
            if(solid)level.setBlock(pos,ThaumcraftTaint.FIBRES.get().defaultBlockState(),2);
        }
    }
    private static boolean adjacentWood(WorldGenLevel level,BlockPos pos) {
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            if(x==0&&y==0&&z==0)continue;var sample=pos.offset(x,y,z);
            if(OriginalBiomeVegetation.available(level,sample)&&level.getBlockState(sample).is(BlockTags.LOGS))return true;
        }
        return false;
    }
    private static int nextInt(RandomSource random,int bound) {return bound<=0?0:random.nextInt(bound);}
    private static int height(WorldGenLevel level,int x,int z) {return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);}
    private static BlockPos top(WorldGenLevel level,int x,int z) {return new BlockPos(x,height(level,x,z),z);}
    private static BlockPos ground(WorldGenLevel level,int x,int z) {return new BlockPos(x,level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z),z);}
}

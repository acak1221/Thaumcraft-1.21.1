package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded native-state translations of the1.7 biome decorator's small vanilla
 * generators. TC4 decorator draw/count order is explicit; native build limits,
 * lighting, voxel support and block-state texture faces are platform adapters. */
final class OriginalBiomeVegetation {
    private OriginalBiomeVegetation() {}
    static boolean available(WorldGenLevel level,BlockPos pos) {return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&WorldGenWriteArea.canWrite(level, pos);}
    private static boolean dirt(BlockState state) {return state.is(Blocks.DIRT)||state.is(Blocks.COARSE_DIRT)||state.is(Blocks.PODZOL);}
    static void blob(WorldGenLevel level,RandomSource random,BlockPos origin,BlockState block) {
        int x=origin.getX(),y=origin.getY(),z=origin.getZ();
        while(y>3){var below=new BlockPos(x,y-1,z);if(!available(level,below))return;var state=level.getBlockState(below);if(state.is(Blocks.GRASS_BLOCK)||dirt(state)||state.is(Blocks.STONE))break;--y;}
        if(y<=3)return;
        for(int part=0;part<3;part++){
            int rx=random.nextInt(2),ry=random.nextInt(2),rz=random.nextInt(2);float radius=(rx+ry+rz)*.333F+.5F;
            for(int xx=x-rx;xx<=x+rx;xx++)for(int zz=z-rz;zz<=z+rz;zz++)for(int yy=y-ry;yy<=y+ry;yy++){
                float dx=xx-x,dz=zz-z,dy=yy-y;var pos=new BlockPos(xx,yy,zz);
                if(dx*dx+dz*dz+dy*dy<=radius*radius&&available(level,pos))level.setBlock(pos,block,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            }
            x+=-1+random.nextInt(2);z+=-1+random.nextInt(2);y-=random.nextInt(2);
        }
    }
    static void sediment(WorldGenLevel level,RandomSource random,BlockPos origin,BlockState block,int radiusBound,boolean clay) {
        if(!available(level,origin)||!level.getFluidState(origin).is(FluidTags.WATER))return;
        int radius=random.nextInt(radiusBound-2)+2,vertical=clay?1:2;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x*x+z*z>radius*radius)continue;
            for(int y=-vertical;y<=vertical;y++){
                var pos=origin.offset(x,y,z);if(!available(level,pos))continue;var state=level.getBlockState(pos);
                if(dirt(state)||(clay?state.is(Blocks.CLAY):state.is(Blocks.GRASS_BLOCK)))level.setBlock(pos,block,2);
            }
        }
    }
    static void patch(WorldGenLevel level,RandomSource random,BlockPos origin,BlockState block,int attempts,boolean descend) {
        BlockPos base=origin;
        if(descend)while(base.getY()>level.getMinBuildHeight()&&available(level,base)&&(level.isEmptyBlock(base)||level.getBlockState(base).is(BlockTags.LEAVES)))base=base.below();
        for(int attempt=0;attempt<attempts;attempt++){
            var pos=base.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if(available(level,pos)&&level.isEmptyBlock(pos)&&block.canSurvive(level,pos))level.setBlock(pos,block,2);
        }
    }
    static void lily(WorldGenLevel level,RandomSource random,BlockPos origin) {patch(level,random,origin,Blocks.LILY_PAD.defaultBlockState(),10,false);}
    static void reeds(WorldGenLevel level,RandomSource random,BlockPos origin) {
        for(int attempt=0;attempt<20;attempt++){
            var pos=origin.offset(random.nextInt(4)-random.nextInt(4),0,random.nextInt(4)-random.nextInt(4));
            if(!available(level,pos)||!level.isEmptyBlock(pos))continue;
            boolean water=false;for(var direction:Direction.Plane.HORIZONTAL)if(available(level,pos.below().relative(direction))&&level.getFluidState(pos.below().relative(direction)).is(FluidTags.WATER)){water=true;break;}
            if(!water)continue;int height=2+random.nextInt(random.nextInt(3)+1);
            for(int y=0;y<height;y++){var cell=pos.above(y);var cane=Blocks.SUGAR_CANE.defaultBlockState();if(available(level,cell)&&cane.canSurvive(level,cell))level.setBlock(cell,cane,2);}
        }
    }
    static void pumpkin(WorldGenLevel level,RandomSource random,BlockPos origin) {
        for(int attempt=0;attempt<64;attempt++){
            var pos=origin.offset(random.nextInt(8)-random.nextInt(8),random.nextInt(4)-random.nextInt(4),random.nextInt(8)-random.nextInt(8));
            if(!available(level,pos)||!level.isEmptyBlock(pos)||!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK))continue;
            if(Blocks.PUMPKIN.defaultBlockState().canSurvive(level,pos)){
                // Old pumpkin metadata carried four directions even for its
                // uncarved appearance. Consume the draw; native uncarved has no facing.
                random.nextInt(4);level.setBlock(pos,Blocks.PUMPKIN.defaultBlockState(),2);
            }
        }
    }
    static boolean hugeMushroom(WorldGenLevel level,RandomSource random,BlockPos origin) {
        int type=random.nextInt(2),height=4+random.nextInt(3),baseY=origin.getY();
        if(baseY<level.getMinBuildHeight()+1||baseY+height+1>=level.getMaxBuildHeight())return false;
        for(int y=baseY;y<=baseY+1+height;y++){
            int spread=y<=baseY+3?0:3;
            for(int x=origin.getX()-spread;x<=origin.getX()+spread;x++)for(int z=origin.getZ()-spread;z<=origin.getZ()+spread;z++){
                var pos=new BlockPos(x,y,z);if(!available(level,pos))return false;var state=level.getBlockState(pos);
                if(!state.isAir()&&!state.is(BlockTags.LEAVES))return false;
            }
        }
        var soil=level.getBlockState(origin.below());if(!dirt(soil)&&!soil.is(Blocks.GRASS_BLOCK)&&!soil.is(Blocks.MYCELIUM))return false;
        int start=baseY+height-(type==1?3:0);
        for(int y=start;y<=baseY+height;y++){
            int radius=type==0?3:y<baseY+height?2:1;
            for(int x=origin.getX()-radius;x<=origin.getX()+radius;x++)for(int z=origin.getZ()-radius;z<=origin.getZ()+radius;z++){
                int metadata=5;if(x==origin.getX()-radius)metadata--;if(x==origin.getX()+radius)metadata++;
                if(z==origin.getZ()-radius)metadata-=3;if(z==origin.getZ()+radius)metadata+=3;
                if(type==0||y<baseY+height){
                    if((x==origin.getX()-radius||x==origin.getX()+radius)&&(z==origin.getZ()-radius||z==origin.getZ()+radius))continue;
                    if(x==origin.getX()-(radius-1)&&z==origin.getZ()-radius||x==origin.getX()-radius&&z==origin.getZ()-(radius-1))metadata=1;
                    if(x==origin.getX()+(radius-1)&&z==origin.getZ()-radius||x==origin.getX()+radius&&z==origin.getZ()-(radius-1))metadata=3;
                    if(x==origin.getX()-(radius-1)&&z==origin.getZ()+radius||x==origin.getX()-radius&&z==origin.getZ()+(radius-1))metadata=7;
                    if(x==origin.getX()+(radius-1)&&z==origin.getZ()+radius||x==origin.getX()+radius&&z==origin.getZ()+(radius-1))metadata=9;
                }
                if(metadata==5&&y<baseY+height)metadata=0;
                var pos=new BlockPos(x,y,z);if(metadata==0||level.getBlockState(pos).isSolidRender(level,pos))continue;
                level.setBlock(pos,mushroomState(type==0?Blocks.BROWN_MUSHROOM_BLOCK:Blocks.RED_MUSHROOM_BLOCK,metadata),2|Block.UPDATE_KNOWN_SHAPE);
            }
        }
        for(int y=0;y<height;y++){var pos=origin.above(y);if(!level.getBlockState(pos).isSolidRender(level,pos))level.setBlock(pos,mushroomState(Blocks.MUSHROOM_STEM,10),2|Block.UPDATE_KNOWN_SHAPE);}
        return true;
    }
    private static BlockState mushroomState(Block block,int metadata) {
        return block.defaultBlockState().setValue(HugeMushroomBlock.UP,metadata!=10).setValue(HugeMushroomBlock.DOWN,false)
            .setValue(HugeMushroomBlock.NORTH,metadata==1||metadata==2||metadata==3||metadata==10)
            .setValue(HugeMushroomBlock.SOUTH,metadata==7||metadata==8||metadata==9||metadata==10)
            .setValue(HugeMushroomBlock.WEST,metadata==1||metadata==4||metadata==7||metadata==10)
            .setValue(HugeMushroomBlock.EAST,metadata==3||metadata==6||metadata==9||metadata==10);
    }
}

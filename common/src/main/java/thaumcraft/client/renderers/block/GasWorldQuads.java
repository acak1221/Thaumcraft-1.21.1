package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.FluxFiniteBlock;

/** BlockGasRenderer:71-268, including the solid-support gate before corner sampling. */
public final class GasWorldQuads {
    private GasWorldQuads() {}
    public static List<BakedQuad> quads(BlockAndTintGetter world,BlockPos pos,BlockState state,BakedModel base) {
        var fluid=(FluxFiniteBlock)state.getBlock();
        Direction toward=fluid.verticalDirection(),away=toward.getOpposite();
        List<BakedQuad> out=new ArrayList<>();
        BlockPos support=pos.relative(toward);
        if(!world.getBlockState(support).isFaceSturdy(world,support,Direction.DOWN)) {
            // Oracle :87-95 calls renderStandardBlock, regardless of quanta.
            List<BakedQuad> cube=new ArrayList<>();
            PositionBlockModels.box(cube,new float[]{0,0,0,16,16,16},base.getParticleIcon(),-1,true);
            for(BakedQuad quad:cube)if(visible(world,pos,state,quad.getDirection()))out.add(quad);
            return out;
        }
        float center=height(world,pos,fluid);
        GasSurfaceRules.Corners corners;
        if(center==1F)corners=new GasSurfaceRules.Corners(1,1,1,1);
        else {
            float[] samples=new float[9];
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)samples[(x+1)*3+z+1]=height(world,pos.offset(x,0,z),fluid);
            corners=GasSurfaceRules.corners(samples);
        }
        boolean top=!world.getBlockState(pos.relative(away)).is(fluid);
        boolean rises=toward==Direction.UP;
        double d=GasSurfaceRules.RENDER_OFFSET;
        double nw=corners.northWest(),sw=corners.southWest(),se=corners.southEast(),ne=corners.northEast();
        if(top){nw-=d;sw-=d;se-=d;ne-=d;}
        var sprite=base.getParticleIcon();
        int light=LevelRenderer.getLightColor(world,state,pos);
        if(top) {
            if(!rises)add(out,sprite,Direction.UP,light,1,new double[][]{{0,nw,0,0,0},{0,sw,1,0,1},{1,se,1,1,1},{1,ne,0,1,0}});
            else add(out,sprite,Direction.DOWN,light,1,new double[][]{{1,1-ne,0,1,0},{1,1-se,1,1,1},{0,1-sw,1,0,1},{0,1-nw,0,0,0}});
        }
        if(visible(world,pos,state,toward)&&!world.getBlockState(support).is(fluid)) {
            // Original rising bottom is y+1+.001, not y+1-.001.
            double y=rises?1+d:d;
            int bottomLight=LevelRenderer.getLightColor(world,state,pos.below());
            if(rises)add(out,sprite,Direction.UP,bottomLight,GasSurfaceRules.bottomLight(true),new double[][]{{0,y,0,0,0},{0,y,1,0,1},{1,y,1,1,1},{1,y,0,1,0}});
            else add(out,sprite,Direction.DOWN,bottomLight,GasSurfaceRules.bottomLight(false),new double[][]{{0,y,1,0,1},{0,y,0,0,0},{1,y,0,1,0},{1,y,1,1,1}});
        }
        Direction[] sides={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
        double[][] ends={{0,d,1,d,nw,ne},{1,1-d,0,1-d,se,sw},{d,1,d,0,sw,nw},{1-d,0,1-d,1,ne,se}};
        for(int side=0;side<4;side++) {
            var face=sides[side];if(!visible(world,pos,state,face))continue;
            var e=ends[side];double x1=e[0],z1=e[1],x2=e[2],z2=e[3],h1=e[4],h2=e[5];
            int sideLight=LevelRenderer.getLightColor(world,state,pos.relative(face));
            double v1=(1-h1)*.5,v2=(1-h2)*.5;
            if(!rises)add(out,sprite,face,sideLight,GasSurfaceRules.sideLight(side),new double[][]{{x1,h1,z1,0,v1},{x2,h2,z2,.5,v2},{x2,0,z2,.5,.5},{x1,0,z1,0,.5}});
            else add(out,sprite,face,sideLight,GasSurfaceRules.sideLight(side),new double[][]{{x1,1,z1,0,.5},{x2,1,z2,.5,.5},{x2,1-h2,z2,.5,v2},{x1,1-h1,z1,0,v1}});
        }
        return out;
    }
    private static boolean visible(BlockAndTintGetter world,BlockPos pos,BlockState state,Direction face) {
        // Both native flux blocks have the original shared MaterialTaint.
        return !(world.getBlockState(pos.relative(face)).getBlock() instanceof FluxFiniteBlock)
                && Block.shouldRenderFace(state,world,pos,face,pos.relative(face));
    }
    private static float height(BlockAndTintGetter world,BlockPos pos,FluxFiniteBlock fluid) {
        var cell=world.getBlockState(pos);var below=world.getBlockState(pos.relative(fluid.verticalDirection().getOpposite()));
        boolean same=cell.is(fluid);
        return GasSurfaceRules.heightForRender(same,below.liquid(),same?cell.getValue(FluxFiniteBlock.LEVEL):0,
                cell.isSolid(),below.is(fluid),same?cell.getValue(FluxFiniteBlock.LEVEL)+1:cell.isAir()?0:-1);
    }
    private static void add(List<BakedQuad> out,TextureAtlasSprite sprite,Direction face,int light,float shade,double[][] points) {
        int[] data=new int[32];int c=(int)(shade*255);int color=0xff000000|c<<16|c<<8|c;
        for(int v=0;v<4;v++) {
            int i=v*8;var p=points[v];
            data[i]=Float.floatToRawIntBits((float)p[0]);data[i+1]=Float.floatToRawIntBits((float)p[1]);data[i+2]=Float.floatToRawIntBits((float)p[2]);
            data[i+3]=color;data[i+4]=Float.floatToRawIntBits(sprite.getU((float)p[3]));data[i+5]=Float.floatToRawIntBits(sprite.getV((float)p[4]));
        }
        out.add(new LegacyBlockQuads.LitQuad(new BakedQuad(data,-1,face,sprite,false),light));
    }
}

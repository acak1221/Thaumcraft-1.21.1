package thaumcraft.fabric.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.lib.fluid.PureFlowRules;

/** Original Forge Classic corner heights/UV/face visibility, scoped to TC4 purifying fluid. */
public final class FabricFluidPureRenderHandler implements FluidRenderHandler {
    private static final double OFFSET=(double).001F;
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","blocks/fluidpure");
    private TextureAtlasSprite[] sprites;
    public static void init(){
        var handler=new FabricFluidPureRenderHandler();
        FluidRenderHandlerRegistry.INSTANCE.register(ThaumcraftSpa.PURE_FLUID_SOURCE.get(),ThaumcraftSpa.PURE_FLUID_FLOWING.get(),handler);
    }
    @Override public void reloadTextures(TextureAtlas atlas){var sprite=atlas.getSprite(TEXTURE);sprites=new TextureAtlasSprite[]{sprite,sprite};}
    @Override public TextureAtlasSprite[] getFluidSprites(BlockAndTintGetter level,BlockPos pos,FluidState state){return sprites;}
    @Override public int getFluidColor(BlockAndTintGetter level,BlockPos pos,FluidState state){return 0xFFFFFFFF;}
    private static boolean pure(BlockState state){return state.getBlock() instanceof BlockFluidPure;}
    public static float height(BlockAndTintGetter level,BlockPos pos){
        var state=level.getBlockState(pos);var above=level.getBlockState(pos.above());
        if(pure(state)){
            if(!above.getFluidState().isEmpty()||above.getBlock() instanceof FluxFiniteBlock)return 1F;
            if(state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL)==0)return .875F;
        }
        if(!BlockFluidPure.legacySolid(state)&&pure(above))return 1F;
        return BlockFluidPure.quanta(level,pos)/8F*.875F;
    }
    /** NW, SW, SE, NE, in precisely the original renderer's sampling order. */
    public static float[] corners(BlockAndTintGetter level,BlockPos pos){
        float center=height(level,pos);if(center==1F)return new float[]{1,1,1,1};
        float nw=height(level,pos.offset(-1,0,-1)),w=height(level,pos.west()),sw=height(level,pos.offset(-1,0,1));
        float n=height(level,pos.north()),s=height(level,pos.south()),ne=height(level,pos.offset(1,0,-1)),e=height(level,pos.east()),se=height(level,pos.offset(1,0,1));
        return new float[]{PureFlowRules.renderAverage(nw,w,n,center),PureFlowRules.renderAverage(w,sw,s,center),PureFlowRules.renderAverage(s,e,se,center),PureFlowRules.renderAverage(n,ne,e,center)};
    }
    private static boolean visible(BlockAndTintGetter level,BlockPos pos){var state=level.getBlockState(pos);return !pure(state)&&!state.isSolidRender(level,pos);}
    private static int light(BlockAndTintGetter level,BlockPos pos){
        int here=LevelRenderer.getLightColor(level,pos),above=LevelRenderer.getLightColor(level,pos.above());
        return Math.max(here&255,above&255)|Math.max(here>>16&255,above>>16&255)<<16;
    }
    @Override public void renderFluid(BlockPos pos,BlockAndTintGetter level,VertexConsumer out,BlockState state,FluidState fluid){
        if(!pure(state)||sprites==null||sprites[0]==null)return;
        var sprite=sprites[0];float[] sampled=corners(level,pos);double[] corners={sampled[0],sampled[1],sampled[2],sampled[3]};
        boolean top=!pure(level.getBlockState(pos.above()));
        // Native chunk meshes expect section-relative coordinates, including Y.
        double x=pos.getX()&15,y=pos.getY()&15,z=pos.getZ()&15;
        if(top){
            for(int i=0;i<4;i++)corners[i]-=OFFSET;
            var vector=fluid.getFlow(level,pos);float[][] uv=new float[4][2];
            if(vector.x==0D&&vector.z==0D){uv[0]=new float[]{0,0};uv[1]=new float[]{0,1};uv[2]=new float[]{1,1};uv[3]=new float[]{1,0};}
            else{
                float direction=(float)(Math.atan2(vector.z,vector.x)-Math.PI/2),sx=Mth.sin(direction)*.25F,cz=Mth.cos(direction)*.25F;
                uv[0]=new float[]{(8+(-cz-sx)*16)/16,(8+(-cz+sx)*16)/16};
                uv[1]=new float[]{(8+(-cz+sx)*16)/16,(8+(cz+sx)*16)/16};
                uv[2]=new float[]{(8+(cz+sx)*16)/16,(8+(cz-sx)*16)/16};
                uv[3]=new float[]{(8+(cz-sx)*16)/16,(8+(-cz-sx)*16)/16};
            }
            quad(out,sprite,new double[][]{{x,y+corners[0],z},{x,y+corners[1],z+1},{x+1,y+corners[2],z+1},{x+1,y+corners[3],z}},uv,1F,light(level,pos),true);
        }
        if(visible(level,pos.below()))quad(out,sprite,new double[][]{{x,y+OFFSET,z+1},{x,y+OFFSET,z},{x+1,y+OFFSET,z},{x+1,y+OFFSET,z+1}},new float[][]{{0,1},{0,0},{1,0},{1,1}},.5F,light(level,pos.below()),false);
        Direction[] sides={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
        for(int side=0;side<4;side++){
            BlockPos neighbour=pos.relative(sides[side]);if(!visible(level,neighbour))continue;
            double ax,bx,az,bz,h1,h2;
            switch(side){
                case 0->{ax=x;bx=x+1;az=z+OFFSET;bz=az;h1=corners[0];h2=corners[3];}
                case 1->{ax=x+1;bx=x;az=z+1-OFFSET;bz=az;h1=corners[2];h2=corners[1];}
                case 2->{ax=x+OFFSET;bx=ax;az=z+1;bz=z;h1=corners[1];h2=corners[0];}
                default->{ax=x+1-OFFSET;bx=ax;az=z;bz=z+1;h1=corners[3];h2=corners[2];}
            }
            quad(out,sprite,new double[][]{{ax,y+h1,az},{bx,y+h2,bz},{bx,y,bz},{ax,y,az}},new float[][]{{0,(float)((1-h1)*.5)},{.5F,(float)((1-h2)*.5)},{.5F,.5F},{0,.5F}},side<2?.8F:.6F,light(level,neighbour),true);
        }
    }
    private static void quad(VertexConsumer out,TextureAtlasSprite sprite,double[][] vertices,float[][] uv,float shade,int light,boolean doubleSided){
        for(int index:new int[]{0,1,2,3})vertex(out,sprite,vertices[index],uv[index],shade,light);
        if(doubleSided)for(int index:new int[]{0,3,2,1})vertex(out,sprite,vertices[index],uv[index],shade,light);
    }
    private static void vertex(VertexConsumer out,TextureAtlasSprite sprite,double[] position,float[] uv,float shade,int light){
        out.addVertex((float)position[0],(float)position[1],(float)position[2]).setColor(shade,shade,shade,1F).setUv(sprite.getU(uv[0]),sprite.getV(uv[1])).setLight(light).setNormal(0,1,0);
    }
}

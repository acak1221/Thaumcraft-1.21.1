package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftTaintSettings;
import thaumcraft.common.lib.world.TaintSpreadRules;

/** Shared replacement for the former Fabric-only neighbour-aware fibre model. */
public final class TaintFibreWorldQuads {
    private TaintFibreWorldQuads() {}
    public static List<BakedQuad> quads(BlockAndTintGetter world,BlockPos pos,BlockState state) {
        List<BakedQuad> out=new ArrayList<>();
        int type=state.getValue(BlockTaintFibres.TYPE),light=LevelRenderer.getLightColor(world,state,pos);
        for(int pass=0;pass<2;pass++)for(Direction side:Direction.values()) {
            var neighbour=pos.relative(side);var support=world.getBlockState(neighbour);
            if(BlockTaint.solidFamily(support)||!support.isFaceSturdy(world,neighbour,side.getOpposite()))continue;
            if(pass==1&&(type!=0||!ThaumcraftTaintSettings.glowingTaint()))continue;
            int index=pass==0?0:TaintSpreadRules.overlay(side.get3DDataValue(),pos.getX(),pos.getY(),pos.getZ());
            if(pass==1&&index==0)continue;
            List<BakedQuad> cube=new ArrayList<>();
            PositionBlockModels.box(cube,new float[]{0,0,0,16,16,16},LegacyBlockQuads.sprite(pass==0?"taint_fibres":"taint_over_"+index),pass==0?0:-1,false);
            for(BakedQuad quad:cube)if(quad.getDirection()==side.getOpposite()) {
                int[] data=quad.getVertices().clone();int axis=side.getAxis()==Direction.Axis.X?0:side.getAxis()==Direction.Axis.Y?1:2;
                float depth=pass==0?.995F:.98F;
                float coordinate=side.getAxisDirection()==Direction.AxisDirection.POSITIVE?depth:1F-depth;
                for(int v=0;v<4;v++)data[v*8+axis]=Float.floatToRawIntBits(coordinate);
                out.add(new LegacyBlockQuads.LitQuad(new BakedQuad(data,quad.getTintIndex(),quad.getDirection(),quad.getSprite(),false),pass==0?light:200));
            }
        }
        if((type==1||type==2)&&BlockTaintFibres.sturdy(world,pos.below(),Direction.UP)) {
            var offset=TaintSpreadRules.grassOffset(pos.getX(),pos.getY(),pos.getZ());
            cross(out,type==1?"taintgrass1":"taintgrass2",(float)offset[0],(float)offset[1],light);
        } else if(type==3||type==4)cross(out,type==3?"taint_spore_stalk_1":"taint_spore_stalk_2",0,0,light);
        return out;
    }
    private static void cross(List<BakedQuad> out,String texture,float dx,float dz,int light) {
        var sprite=LegacyBlockQuads.sprite(texture);
        for(int diagonal=0;diagonal<2;diagonal++)for(int reverse=0;reverse<2;reverse++) {
            float ax=.05F+dx,bx=.95F+dx,az=(diagonal==0?.05F:.95F)+dz,bz=(diagonal==0?.95F:.05F)+dz;
            if(reverse==1){float swap=ax;ax=bx;bx=swap;swap=az;az=bz;bz=swap;}
            float[][] points={{ax,1,az,0,0},{ax,0,az,0,1},{bx,0,bz,1,1},{bx,1,bz,1,0}};
            int[] data=new int[32];
            for(int v=0;v<4;v++) {
                int i=v*8;float[] p=points[v];
                data[i]=Float.floatToRawIntBits(p[0]);data[i+1]=Float.floatToRawIntBits(p[1]);data[i+2]=Float.floatToRawIntBits(p[2]);
                data[i+3]=-1;data[i+4]=Float.floatToRawIntBits(sprite.getU(p[3]));data[i+5]=Float.floatToRawIntBits(sprite.getV(p[4]));
            }
            out.add(new LegacyBlockQuads.LitQuad(new BakedQuad(data,0,Direction.UP,sprite,false),light));
        }
    }
}

package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.tiles.TileManaPod;

/** Original stem is the block model; this is the age2..7 translucent fruit,
 * age3..7 pulsing inner core, aspect maturation tint and original model UVs. */
public final class TileManaPodRenderer implements BlockEntityRenderer<TileManaPod> {
    private static final ResourceLocation INNER=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/manapod_0.png"),OUTER=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/manapod_2.png");
    private final ModelPart inner=part(4,5),outer=part(7,9);
    public TileManaPodRenderer(BlockEntityRendererProvider.Context context){}
    private static ModelPart part(int width,int height){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("pod",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-width/2F,0,-width/2F,width,height,width),PartPose.ZERO);return LayerDefinition.create(mesh,32,32).bakeRoot().getChild("pod");}
    @Override public void render(TileManaPod tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        int age=tile.getLevel()==null?5:tile.getBlockState().getValue(BlockManaPod.AGE);if(age<=1)return;
        float red=.14509805F,green=.6156863F,blue=.45882353F;
        if(tile.getAspect()!=null){int rgb=tile.getAspect().getColor();float r=(rgb>>16&255)/255F,g=(rgb>>8&255)/255F,b=(rgb&255)/255F;
            if(age==7){red=r;green=g;blue=b;}else{float maturity=age-2;red=(red+r*maturity)/(maturity+1);green=(green+g*maturity)/(maturity+1);blue=(blue+b*maturity)/(maturity+1);}}
        pose.pushPose();try{
            pose.translate(.5,.75,.5);pose.mulPose(Axis.XP.rotationDegrees(180));
            if(age>2){
                var player=Minecraft.getInstance().player;int ticks=player==null?0:player.tickCount;
                float pulse=Mth.sin((ticks+tile.hashCode()%100)/8F)*.1F+.9F;
                light=age*10+(int)(150*pulse);pose.pushPose();try{pose.translate(0,.1,0);float scale=(float)(.125*age*pulse);pose.scale(scale,scale,scale);inner.render(pose,buffers.getBuffer(RenderType.entityTranslucent(INNER)),light,overlay);}finally{pose.popPose();}
            }
            float scale=(float)(.15*age);pose.scale(scale,scale,scale);
            int color=(int)(.9F*255)<<24|(int)(red*255)<<16|(int)(green*255)<<8|(int)(blue*255);
            outer.render(pose,buffers.getBuffer(RenderType.entityTranslucent(OUTER)),light,overlay,color);
        }finally{pose.popPose();}
    }
}

package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.client.renderers.models.ModelNodeStabilizer;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.tiles.TileNodeConverter;

/** Original converter41-103: inverted stabilizer mesh, min(50,count)/137,
 * original three status tints and per-piston oscillation. No renderer counter. */
public final class TileNodeConverterRenderer implements BlockEntityRenderer<TileNodeConverter> {
    private static final RenderType BASE=VisNetRender.modelType("converter_base","node_converter",false),OVER=VisNetRender.modelType("converter_over","node_converter_over",false);
    private static volatile ModelNodeStabilizer itemModel;private final ModelNodeStabilizer model;
    public TileNodeConverterRenderer(BlockEntityRendererProvider.Context context){model=new ModelNodeStabilizer(Minecraft.getInstance().getResourceManager());itemModel=model;}
    @Override public void render(TileNodeConverter tile,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){renderModel(model,tile.getCount(),tile.getStatus(),VisNetRender.cameraTicks(),true,pose,buffers,light,overlay);}
    public static void renderItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!stack.is(ThaumcraftNodeDevices.NODE_TRANSDUCER_ITEM.get()))return;ModelNodeStabilizer mesh=itemModel;if(mesh==null){mesh=new ModelNodeStabilizer(Minecraft.getInstance().getResourceManager());itemModel=mesh;}renderModel(mesh,-1,0,0,false,pose,buffers,light,overlay);}
    private static void renderModel(ModelNodeStabilizer model,int count,int status,int ticks,boolean world,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        float v=Math.min(50,count)/137F,r=status==2?1:status==1?1:.5F,g=status==2?0:status==1?.6F:1,b=status==2?.3F:status==1?.1F:.5F;
        pose.pushPose();try{pose.translate(.5,1,.5);pose.mulPose(Axis.XP.rotationDegrees(90));model.render("lock",pose,buffers.getBuffer(BASE),light,overlay);int glow=world?50+(int)(170F*v*2.5F*(Mth.sin(ticks/3F)*.1F+.9F)):light;model.render("lock",pose,VisNetRender.tint(buffers.getBuffer(OVER),r,g,b),glow,overlay);
            for(int i=0;i<4;i++){pose.pushPose();try{pose.mulPose(Axis.ZP.rotationDegrees(i*90));pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(0,0,v);model.render("piston",pose,buffers.getBuffer(BASE),light,overlay);glow=world?50+(int)(170F*v*2.5F*(Mth.sin((ticks+i*5)/3F)*.1F+.9F)):light;model.render("piston",pose,VisNetRender.tint(buffers.getBuffer(OVER),r,g,b),glow,overlay);}finally{pose.popPose();}}
        }finally{pose.popPose();}
    }
}

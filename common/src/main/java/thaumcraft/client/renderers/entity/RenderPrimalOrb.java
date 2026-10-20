package thaumcraft.client.renderers.entity;

import java.util.Random;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;

/** Twelve seeded cumulative primal-colour fans and original row-two thirteen-frame additive billboard. */
public final class RenderPrimalOrb extends EntityRenderer<EntityPrimalOrb> {
    private static final RenderType RAYS=new Rays();
    private static final int[] COLORS={0xffff7e,0xff3c01,0x0090ff,0x00a000,0xeeccff,0x555577};
    public RenderPrimalOrb(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public ResourceLocation getTextureLocation(EntityPrimalOrb orb){return VisNetRender.PARTICLES;}
    @Override public void render(EntityPrimalOrb orb,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();
        try{
            var random=new Random(orb.getId());var ray=buffers.getBuffer(RAYS);
            for(int i=0;i<12;i++){
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360F));pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360F));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360F));pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360F));pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360F+orb.tickCount/80F*360F));
                float length=random.nextFloat()*20F+5F,width=random.nextFloat()*2F+1F;
                float divisor=30F/((float)Math.min(orb.tickCount,10)/10F);width/=divisor;length/=divisor;
                float[][] ends={{(float)(-.866D*width),length,-.5F*width},{(float)(.866D*width),length,-.5F*width},{0,length,width}};
                int color=COLORS[i/2];
                // Exact triangle fan, expanded into three actual native TRIANGLES, not degenerate quad buffers.
                for(int side=0;side<3;side++){
                    vertex(ray,pose,new float[]{0,0,0},0xffffff,255);
                    vertex(ray,pose,ends[side],color,0);vertex(ray,pose,ends[(side+1)%3],color,0);
                }
            }
        }finally{pose.popPose();}
        if(!VisNetRender.flush(buffers))return;
        pose.pushPose();
        try{
            pose.mulPose(entityRenderDispatcher.cameraOrientation());float u=(orb.tickCount%13)/16F;
            VisNetRender.draw(VisNetRender.PARTICLES,GL11.GL_ONE,true,GameRenderer.getPositionTexColorShader(),
                    out->{
                        VisNetRender.vertex(out,pose,new net.minecraft.world.phys.Vec3(-.25,-.25,0),u,.1874375F,0xffffff,.8F);
                        VisNetRender.vertex(out,pose,new net.minecraft.world.phys.Vec3(.25,-.25,0),u+.0624375F,.1874375F,0xffffff,.8F);
                        VisNetRender.vertex(out,pose,new net.minecraft.world.phys.Vec3(.25,.25,0),u+.0624375F,.125F,0xffffff,.8F);
                        VisNetRender.vertex(out,pose,new net.minecraft.world.phys.Vec3(-.25,.25,0),u,.125F,0xffffff,.8F);
                    });
        }finally{pose.popPose();}
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float[] point,int color,int alpha){
        out.addVertex(pose.last(),point[0],point[1],point[2]).setColor(color>>16&255,color>>8&255,color&255,alpha);
    }
    private static final class Rays extends RenderType {
        private Rays(){super("thaumcraft_primal_rays",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.TRIANGLES,4096,false,false,Rays::begin,Rays::end);}
        private static void begin(){RENDERTYPE_LIGHTNING_SHADER.setupRenderState();NO_TEXTURE.setupRenderState();LIGHTNING_TRANSPARENCY.setupRenderState();CULL.setupRenderState();LEQUAL_DEPTH_TEST.setupRenderState();COLOR_WRITE.setupRenderState();}
        private static void end(){COLOR_WRITE.clearRenderState();LEQUAL_DEPTH_TEST.clearRenderState();CULL.clearRenderState();LIGHTNING_TRANSPARENCY.clearRenderState();NO_TEXTURE.clearRenderState();RENDERTYPE_LIGHTNING_SHADER.clearRenderState();}
    }
}

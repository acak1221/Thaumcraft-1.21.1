package thaumcraft.client.renderers.entity;

import java.util.Random;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;

/** Original twelve seeded, rotating pale-purple light fans plus the thirteen-frame particle-atlas billboard. */
public final class RenderEldritchOrb extends EntityRenderer<EntityEldritchOrb> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png");
    public RenderEldritchOrb(EntityRendererProvider.Context context) { super(context);shadowRadius=0; }
    @Override public ResourceLocation getTextureLocation(EntityEldritchOrb entity) { return TEXTURE; }
    @Override public void render(EntityEldritchOrb entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();
        try {
            Random random=new Random(entity.getId());
            VertexConsumer ray=buffers.getBuffer(RenderType.lightning());
            for (int i=0;i<12;i++) {
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360+entity.tickCount/80F*360));
                float growth=Math.min(entity.tickCount,10)/10F/30;
                float length=(random.nextFloat()*20+5)*growth, width=(random.nextFloat()*2+1)*growth;
                float[][] ends={{-.866F*width,length,-.5F*width},{.866F*width,length,-.5F*width},{0,length,width}};
                for (int side=0;side<3;side++) {
                    ray.addVertex(pose.last(),0,0,0).setColor(255,255,255,255);
                    float[] a=ends[side],b=ends[(side+1)%3];
                    ray.addVertex(pose.last(),a[0],a[1],a[2]).setColor(238,204,255,0);
                    ray.addVertex(pose.last(),b[0],b[1],b[2]).setColor(238,204,255,0);
                    ray.addVertex(pose.last(),0,0,0).setColor(255,255,255,255);
                }
            }
        } finally { pose.popPose(); }
        pose.pushPose();
        try {
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            float u=(entity.tickCount%13)/16F, end=u+.0624375F, v=.1875F, bottom=v+.0624375F;
            var out=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
            vertex(out,pose,-.5F,-.5F,u,bottom);vertex(out,pose,.5F,-.5F,end,bottom);
            vertex(out,pose,.5F,.5F,end,v);vertex(out,pose,-.5F,.5F,u,v);
        } finally { pose.popPose(); }
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float u,float v) {
        out.addVertex(pose.last(),x,y,0).setColor(255,255,255,255).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose.last(),0,1,0);
    }
}

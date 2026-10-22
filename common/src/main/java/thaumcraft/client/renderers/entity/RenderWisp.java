package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.monster.EntityWisp;

/** Original additive two-layer billboard, including its unusual texture-width/64 UV nudge. */
public final class RenderWisp extends EntityRenderer<EntityWisp> {
    private static final ResourceLocation WISP=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/wisp.png");
    public RenderWisp(EntityRendererProvider.Context context) { super(context);shadowRadius=0; }
    @Override public ResourceLocation getTextureLocation(EntityWisp entity) { return WISP; }
    @Override public void render(EntityWisp wisp,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        if(wisp.getHealth()<=0 || !VisNetRender.flush(buffers)) return;
        var aspect=Aspect.getAspect(wisp.getAspectTag());int color=aspect==null?0:aspect.getColor();
        if(wisp.hurtTime>0) color=0xff0000|((int)((color>>8&255)*255/300F)<<8)|(int)((color&255)*255/300F);
        pose.pushPose();
        try {
            pose.translate(0,.45F,0);pose.mulPose(entityRenderDispatcher.cameraOrientation());
            int frame=wisp.tickCount%16;float u=(frame%4)/4F,v=(frame/4)/4F;
            float span=thaumcraft.client.lib.UtilsFX.wispUvSpan(thaumcraft.client.lib.UtilsFX.getTextureSize("textures/misc/wisp.png",64));
            final int tint=color;
            VisNetRender.draw(WISP,GL11.GL_ONE,false,GameRenderer.getPositionTexColorShader(),out -> VisNetRender.quad(out,pose,1,1,tint,u,u+span,v,v+span));
            VisNetRender.draw(VisNetRender.PARTICLES,GL11.GL_ONE,false,out -> VisNetRender.quad(out,pose,.4F+Mth.sin((wisp.tickCount+partial)/10)*.1F,1,0xffffff,frame/16F,(frame+1)/16F,5/16F,6/16F));
        } finally { pose.popPose(); }
    }
}

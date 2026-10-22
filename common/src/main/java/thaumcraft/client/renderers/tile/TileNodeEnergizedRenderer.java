package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

/** Always-visible original auraBase/type/modifier core + LIGHTNINGRINGV.
 * Reads the ACTUAL energized BE; no fake TileNode, item, cube or pulse/base alias.
 * Special type appearance is not a claim of special-node gameplay closure. */
public final class TileNodeEnergizedRenderer implements BlockEntityRenderer<TileNodeEnergized> {
    public TileNodeEnergizedRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TileNodeEnergized node,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var viewer=Minecraft.getInstance().getCameraEntity();if(viewer==null || node.isQuarantined())return;double distance=Math.sqrt(viewer.distanceToSqr(Vec3.atCenterOf(node.getBlockPos())));if(distance>64 || !VisNetRender.flush(buffers))return;
        var aspects=node.getAuraBase();long now=System.nanoTime();int frame=(int)Math.floorMod(now/40000000L+node.getBlockPos().getX(),32L),index=0;float alpha=(float)((64-distance)/64),average=0,angle=0;var modifier=node.getNodeModifier();
        if(modifier==NodeModifier.BRIGHT)alpha*=1.5F;else if(modifier==NodeModifier.PALE)alpha*=.66F;else if(modifier==NodeModifier.FADING)alpha*=Mth.sin(viewer.tickCount/3F)*.25F+.33F;
        pose.pushPose();try{pose.translate(.5,.5,.5);
            for(var aspect:aspects.getAspects()){int amount=aspects.getAmount(aspect);average+=amount;float scale=.2F+(Mth.sin(viewer.tickCount/(14F-index))*.25F+.5F)*(amount/50F);angle=(float)((now/5000000L)%(5000L+500L*index))/(5000F+500F*index)*Mth.TWO_PI;float opacity=alpha/Math.max(1F,aspects.size()/2F);if(aspect.getBlend()==GL11.GL_ONE_MINUS_SRC_ALPHA)opacity*=1.5F;VisNetRender.billboard(pose,VisNetRender.NODES,angle,scale,opacity,32,0,frame,aspect.getColor(),aspect.getBlend());++index;}
            if(aspects.size()>0){var type=node.getNodeType();int strip=switch(type){case NORMAL->1;case UNSTABLE->6;case DARK->2;case TAINTED->5;case PURE->4;case HUNGRY->3;};float scale=.1F+average/aspects.size()/150F;if(type==NodeType.HUNGRY)scale*=.75F;if(type==NodeType.UNSTABLE)angle=0;VisNetRender.billboard(pose,VisNetRender.NODES,angle,scale,alpha,32,strip,frame,0xffffff,type==NodeType.DARK || type==NodeType.TAINTED?GL11.GL_ONE_MINUS_SRC_ALPHA:GL11.GL_ONE);}
            // Original Energized38-42: retained4096x256 =>16 horizontal frames,
            // 40ms + TESR camera-relative X phase; fixed scale.33 / alpha.9.
            int frames=thaumcraft.client.lib.UtilsFX.getTextureAnimationSize("textures/items/lightningringv.png");
            double relativeX=node.getBlockPos().getX()-Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x;int ringFrame=(int)(((double)(now/40000000L)+relativeX)%frames);VisNetRender.facingQuad(pose,VisNetRender.RING,.33F,.9F,frames,ringFrame,true);
        }finally{pose.popPose();}
    }
    @Override public int getViewDistance(){return 64;}
}

package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import thaumcraft.client.renderers.models.ModelNodeStabilizer;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.tiles.TileNodeStabilizer;

/** Basic lock1 ONLY. Original renderer41-99 + UtilsFX153-191, independently
 * adapted after actively reading pinned OpenTC4. No advanced red/power facade. */
public final class TileNodeStabilizerRenderer implements BlockEntityRenderer<TileNodeStabilizer> {
    private static final RenderType BASE=new StabilizerType("base","node_stabilizer"),OVER=new StabilizerType("over","node_stabilizer_over");
    private static final ResourceLocation BUBBLE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/node_bubble.png");
    private static final Tesselator QUAD=new Tesselator(256);
    private static volatile ModelNodeStabilizer itemModel;
    private static volatile ShaderInstance bubbleShader;
    private final ModelNodeStabilizer model;
    public TileNodeStabilizerRenderer(BlockEntityRendererProvider.Context context){model=new ModelNodeStabilizer(Minecraft.getInstance().getResourceManager());itemModel=model;}
    /** Root-owned core-shader registration: POSITION_TEX_COLOR, vanilla vertex
     * position_tex_color; fragment discards color.a <= 0.003921569, NOT vanilla .1.
     * Sampler0 / ModelViewMat / ProjMat / ColorModulator; no new vertex shader. */
    public static void setBubbleShader(ShaderInstance shader){bubbleShader=Objects.requireNonNull(shader,"native stabilizer bubble shader");}
    @Override public void render(TileNodeStabilizer tile,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay){int ticks=cameraTicks();renderModel(model,tile.getAnimationCount(),ticks,true,pose,buffers,light,overlay);if(tile.getAnimationCount()>0){if(buffers instanceof MultiBufferSource.BufferSource nativeBuffers){nativeBuffers.endBatch(BASE);nativeBuffers.endBatch(OVER);}bubble(tile.getAnimationCount(),ticks,pose);}}
    public static void renderItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!stack.is(ThaumcraftNodeDevices.NODE_STABILIZER_ITEM.get()))return;ModelNodeStabilizer mesh=itemModel;if(mesh==null){mesh=new ModelNodeStabilizer(Minecraft.getInstance().getResourceManager());itemModel=mesh;}renderModel(mesh,0,0,false,pose,buffers,light,overlay);}
    private static int cameraTicks(){var camera=Minecraft.getInstance().getCameraEntity();return camera==null?0:camera.tickCount;}
    private static void renderModel(ModelNodeStabilizer model,int count,int ticks,boolean world,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();try{pose.translate(.5,0,.5);pose.mulPose(Axis.XN.rotationDegrees(90));model.render("lock",pose,buffers.getBuffer(BASE),light,overlay);
            for(int a=0;a<4;a++){pose.pushPose();try{pose.mulPose(Axis.ZP.rotationDegrees(90*a));pose.mulPose(Axis.YP.rotationDegrees(45));pose.translate(0,0,count/100F);model.render("piston",pose,buffers.getBuffer(BASE),light,overlay);float pulse=Mth.sin((ticks+a*5)/3F)*.1F+.9F;int glow=world?50+(int)(170F*(count/37F*pulse)):light;model.render("piston",pose,buffers.getBuffer(OVER),glow,overlay);}finally{pose.popPose();}}
        }finally{pose.popPose();}
    }
    private static void bubble(int count,int ticks,PoseStack pose){
        // A separate immediate native quad preserves SRC_ALPHA/ONE and the
        // original very low alpha test without entity-shader lighting/cutout.
        ShaderInstance shader=Objects.requireNonNull(bubbleShader,"Root must register thaumcraft:node_stabilizer_bubble before rendering an open stabilizer");
        var oldShader=RenderSystem.getShader();int texture=RenderSystem.getShaderTexture(0);float[] colour=RenderSystem.getShaderColor().clone();boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),depthTest=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);int depthFunction=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        pose.pushPose();try{pose.translate(.5,1.5,.5);pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,BUBBLE);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(false);RenderSystem.disableCull();float opacity=count/37F*(Mth.sin(ticks/8F)*.1F+.5F);int alpha=(int)(opacity*255F);var mesh=QUAD.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            // Literal UtilsFX153-191 facing-quad corner/UV order, not the
            // differently oriented strip/particle atlas helper.
            vertex(mesh,pose,-.9F,-.9F,0,1,alpha);vertex(mesh,pose,-.9F,.9F,1,1,alpha);vertex(mesh,pose,.9F,.9F,1,0,alpha);vertex(mesh,pose,.9F,-.9F,0,0,alpha);BufferUploader.drawWithShader(mesh.buildOrThrow());}
        finally{pose.popPose();RenderSystem.setShader(()->oldShader);RenderSystem.setShaderTexture(0,texture);RenderSystem.setShaderColor(colour[0],colour[1],colour[2],colour[3]);RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();RenderSystem.depthMask(depthWrite);RenderSystem.depthFunc(depthFunction);if(depthTest)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();}
    }
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float u,float v,int alpha){out.addVertex(pose.last(),x,y,0).setUv(u,v).setColor(255,255,255,alpha);}
    /** Native Fabric has no Forge getRenderBoundingBox BE hook. Route through
     * the native offscreen-BER set so the upper bubble is not one-cell culled,
     * then retain a bounded distance from the original TWO-high render volume. */
    public static AABB renderBounds(TileNodeStabilizer tile){return new AABB(tile.getBlockPos()).expandTowards(0,1,0);}
    @Override public boolean shouldRenderOffScreen(TileNodeStabilizer tile){return true;}
    @Override public int getViewDistance(){return 64;}
    @Override public boolean shouldRender(TileNodeStabilizer tile,Vec3 camera){return renderBounds(tile).distanceToSqr(camera)<=getViewDistance()*getViewDistance();}
    private static final class StabilizerType extends RenderType {
        private StabilizerType(String name,String texture){super("thaumcraft_stabilizer_"+name,DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,8192,false,false,()->begin(texture),()->end(texture));}
        private static TextureStateShard texture(String name){return new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/"+name+".png"),false,false);}
        private static void begin(String name){RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState();texture(name).setupRenderState();NO_TRANSPARENCY.setupRenderState();NO_CULL.setupRenderState();LIGHTMAP.setupRenderState();OVERLAY.setupRenderState();LEQUAL_DEPTH_TEST.setupRenderState();COLOR_DEPTH_WRITE.setupRenderState();}
        private static void end(String name){COLOR_DEPTH_WRITE.clearRenderState();LEQUAL_DEPTH_TEST.clearRenderState();OVERLAY.clearRenderState();LIGHTMAP.clearRenderState();NO_CULL.clearRenderState();NO_TRANSPARENCY.clearRenderState();texture(name).clearRenderState();RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();}
    }
}

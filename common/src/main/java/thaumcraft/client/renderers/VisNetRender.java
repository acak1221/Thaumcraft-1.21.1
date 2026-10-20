package thaumcraft.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** New M09b-only native drawing. No accepted helper/model/renderer is edited.
 * Immediate effects run ONLY after the native buffered geometry is flushed.
 * Every entry owns exact blend/depth/write/cull/shader/texture/colour restoration. */
public final class VisNetRender {
    public static final ResourceLocation NODES=texture("misc/nodes"),RING=texture("items/lightningringv"),BEAM=texture("misc/beam1"),BOLT_LARGE=texture("misc/p_large"),BOLT_SMALL=texture("misc/p_small"),PARTICLES=texture("misc/particles");
    private static final Tesselator MESH=new Tesselator(65536);
    private static volatile ShaderInstance shader;
    private VisNetRender() {}
    public static ResourceLocation texture(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/"+path+".png");}
    public static void setShader(ShaderInstance value){shader=Objects.requireNonNull(value,"Root-owned low-alpha POSITION_TEX_COLOR shader");}
    public static int cameraTicks(){var camera=Minecraft.getInstance().getCameraEntity();return camera==null?0:camera.tickCount;}
    public static boolean flush(MultiBufferSource buffers){if(buffers instanceof MultiBufferSource.BufferSource nativeBuffers){nativeBuffers.endBatch();return true;}return false;}
    public static RenderType modelType(String name,String texture,boolean translucent){return new ModelType(name,texture,translucent);}
    public static VertexConsumer tint(VertexConsumer target,float red,float green,float blue){return new Tint(target,red,green,blue);}

    public static void draw(ResourceLocation texture,int destination,boolean depthWrite,Consumer<VertexConsumer> vertices){
        ShaderInstance selected=Objects.requireNonNull(shader,"Root must pass node_stabilizer_bubble to VisNetClientEffects.setShader before VisNet rendering");
        draw(texture,destination,depthWrite,selected,vertices);
    }
    /** Additive caller with the ordinary alpha threshold, e.g. original Wisp's first billboard layer. */
    public static void draw(ResourceLocation texture,int destination,boolean depthWrite,ShaderInstance selected,Consumer<VertexConsumer> vertices){
        drawFormat(texture,destination,depthWrite,selected,DefaultVertexFormat.POSITION_TEX_COLOR,vertices);
    }
    /** Same owned state transaction, native lightmap particle format and the original low-alpha threshold. */
    public static void drawParticle(ResourceLocation texture,ShaderInstance selected,Consumer<VertexConsumer> vertices){
        drawFormat(texture,GL11.GL_ONE,false,selected,DefaultVertexFormat.PARTICLE,vertices);
    }
    private static void drawFormat(ResourceLocation texture,int destination,boolean depthWrite,ShaderInstance selected,VertexFormat format,Consumer<VertexConsumer> vertices){
        Objects.requireNonNull(selected,"Native effect shader must be registered before drawing");
        try(NativeState ignored=new NativeState()){
            RenderSystem.setShader(()->selected);RenderSystem.setShaderTexture(0,texture);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,destination);RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(depthWrite);RenderSystem.disableCull();
            var mesh=MESH.begin(VertexFormat.Mode.QUADS,format);vertices.accept(mesh);BufferUploader.drawWithShader(mesh.buildOrThrow());
        }
    }
    /** UtilsFX facing-strip UVs. Positions supplied by a caller-owned pose. */
    public static void billboard(PoseStack pose,ResourceLocation texture,float angle,float scale,float alpha,int frames,int strip,int frame,int colour,int destination){
        pose.pushPose();try{pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());pose.mulPose(Axis.ZP.rotation(angle));draw(texture,destination,false,out->quad(out,pose,scale,alpha,colour,frame/(float)frames,(frame+1F)/frames,strip/(float)frames,(strip+1F)/frames));}finally{pose.popPose();}
    }
    /** Distinct original UtilsFX153-191 facing-QUAD UV order, NOT strip order.
     * Energized ring retains its original depth write after renderNode returns. */
    public static void facingQuad(PoseStack pose,ResourceLocation texture,float scale,float alpha,int frames,int frame,boolean depthWrite){
        pose.pushPose();try{pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());float u0=frame/(float)frames,u1=(frame+1F)/frames;draw(texture,GL11.GL_ONE,depthWrite,out->{vertex(out,pose,new Vec3(-scale,-scale,0),u0,1,0xffffff,alpha);vertex(out,pose,new Vec3(-scale,scale,0),u1,1,0xffffff,alpha);vertex(out,pose,new Vec3(scale,scale,0),u1,0,0xffffff,alpha);vertex(out,pose,new Vec3(scale,-scale,0),u0,0,0xffffff,alpha);});}finally{pose.popPose();}
    }
    public static void quad(VertexConsumer out,PoseStack pose,float scale,float alpha,int colour,float u0,float u1,float v0,float v1){
        vertex(out,pose,new Vec3(-scale,-scale,0),u1,v1,colour,alpha);vertex(out,pose,new Vec3(-scale,scale,0),u1,v0,colour,alpha);vertex(out,pose,new Vec3(scale,scale,0),u0,v0,colour,alpha);vertex(out,pose,new Vec3(scale,-scale,0),u0,v1,colour,alpha);
    }
    public static void vertex(VertexConsumer out,PoseStack pose,Vec3 point,float u,float v,int colour,float alpha){out.addVertex(pose.last(),(float)point.x,(float)point.y,(float)point.z).setUv(u,v).setColor(colour>>16&255,colour>>8&255,colour&255,Mth.clamp((int)(alpha*255F),0,255));}
    /** Two original crossed scrolling beam planes, size.7 / halfwidth.105.
     * The caller supplies ACTUAL parent and relay endpoints/pulse/RGB, not stock. */
    public static void beam(PoseStack pose,Vec3 start,Vec3 end,float age,float opacity,int colour){
        Vec3 delta=end.subtract(start);double length=delta.length();if(length<1.0e-6 || length>8.2)return;Vec3 direction=delta.scale(1/length);Vec3 side=direction.cross(Math.abs(direction.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize().scale(.105),other=direction.cross(side).normalize().scale(.105);float slide=-age*.2F-Mth.floor(-age*.1F);
        draw(BEAM,GL11.GL_ONE,false,out->{for(int i=0;i<2;i++){Vec3 across=i==0?side:other;float v=-1+slide+i/3F;vertex(out,pose,end.subtract(across),1,v+(float)length,colour,opacity);vertex(out,pose,start.subtract(across),1,v,colour,opacity);vertex(out,pose,start.add(across),0,v,colour,opacity);vertex(out,pose,end.add(across),0,v+(float)length,colour,opacity);}});
    }
    /** State is captured at DRAW time, never while merely queuing vertices. */
    private static final class NativeState implements AutoCloseable {
        final ShaderInstance shader=RenderSystem.getShader();final int[] textures={RenderSystem.getShaderTexture(0),RenderSystem.getShaderTexture(1),RenderSystem.getShaderTexture(2)};final float[] colour=RenderSystem.getShaderColor().clone();
        final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        final int depthFunction=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        @Override public void close(){RenderSystem.setShader(()->shader);for(int i=0;i<textures.length;i++)RenderSystem.setShaderTexture(i,textures[i]);RenderSystem.setShaderColor(colour[0],colour[1],colour[2],colour[3]);RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha);if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();RenderSystem.depthMask(depthWrite);RenderSystem.depthFunc(depthFunction);if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();}
    }
    private static final class ModelType extends RenderType {
        private NativeState state;private final TextureStateShard sheet;private final boolean translucent;
        ModelType(String name,String texture,boolean translucent){super("thaumcraft_visnet_"+name,DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES,8192,false,false,()->{},()->{});sheet=new TextureStateShard(VisNetRender.texture("models/"+texture),false,false);this.translucent=translucent;}
        @Override public void draw(MeshData mesh){RenderSystem.assertOnRenderThread();setupRenderState();try{BufferUploader.drawWithShader(mesh);}finally{clearRenderState();}}
        @Override public void setupRenderState(){if(state!=null)throw new IllegalStateException("Nested VisNet model draw");state=new NativeState();try{RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState();sheet.setupRenderState();LIGHTMAP.setupRenderState();OVERLAY.setupRenderState();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableDepthTest();RenderSystem.depthFunc(GL11.GL_LEQUAL);RenderSystem.depthMask(true);RenderSystem.disableCull();if(translucent){RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();}else RenderSystem.disableBlend();}catch(RuntimeException|Error error){state.close();state=null;throw error;}}
        @Override public void clearRenderState(){try{OVERLAY.clearRenderState();LIGHTMAP.clearRenderState();sheet.clearRenderState();RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();}finally{NativeState old=state;state=null;if(old!=null)old.close();}}
    }
    private record Tint(VertexConsumer target,float red,float green,float blue) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x,float y,float z){target.addVertex(x,y,z);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(Mth.clamp((int)(r*red),0,255),Mth.clamp((int)(g*green),0,255),Mth.clamp((int)(b*blue),0,255),a);return this;}
        @Override public VertexConsumer setUv(float u,float v){target.setUv(u,v);return this;}
        @Override public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
        @Override public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){target.setNormal(x,y,z);return this;}
    }
}

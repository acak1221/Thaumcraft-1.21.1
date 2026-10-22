package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Original FXSlimyBubble144..150, lifetime15..19, rise0.005, lift at age5 and final half-speed tail. */
public final class SlurryBubbleParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return 1; }
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/misc/particles.png");
    private static final Tesselator MESH=new Tesselator(2048);
    private int frame=144;
    public SlurryBubbleParticle(ClientLevel level,double x,double y,double z,float size) {
        super(level,x,y,z); xd=yd=zd=0; hasPhysics=false; quadSize=size; lifetime=15+level.random.nextInt(5); alpha=.8F;
        setColor(1,1,1); setSize(.01F,.01F);
    }
    private boolean valid() { return Minecraft.getInstance().level==level && level.hasChunkAt(BlockPos.containing(x,y,z)); }
    @Override public void tick() {
        xo=x; yo=y; zo=z; if(!valid() || age++>=lifetime) { remove(); return; }
        if(age-1<6) { frame=144+age/2; if(age==5) y+=.1D; }
        else if(age<lifetime-4) { yd+=.005D; frame=147+age%4/2; }
        else { yd/=2D; frame=150-(lifetime-age)/2; }
        y+=yd;
    }
    @Override public float getQuadSize(float partial) { return quadSize; }
    @Override protected float getU0() { return frame%16/16F; }
    @Override protected float getU1() { return getU0()+.0625F; }
    @Override protected float getV0() { return frame/16/16F; }
    @Override protected float getV1() { return getV0()+.0625F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer ignored,Camera camera,float partial) {
        if(!valid() || !isAlive()) return;
        var shader=RenderSystem.getShader(); int[] texture={RenderSystem.getShaderTexture(0),RenderSystem.getShaderTexture(1),RenderSystem.getShaderTexture(2)};
        float[] color=RenderSystem.getShaderColor().clone(); boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),write=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader); RenderSystem.setShaderTexture(0,TEXTURE); RenderSystem.setShaderColor(1,1,1,1);
            RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(false); RenderSystem.disableCull();
            var buffer=MESH.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE); super.render(buffer,camera,partial); BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(()->shader); for(int i=0;i<texture.length;i++) RenderSystem.setShaderTexture(i,texture[i]); RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
            RenderSystem.blendFuncSeparate(src,dst,srcAlpha,dstAlpha); if(blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if(depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest(); RenderSystem.depthFunc(depthFunc); RenderSystem.depthMask(write); if(cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        }
    }
}

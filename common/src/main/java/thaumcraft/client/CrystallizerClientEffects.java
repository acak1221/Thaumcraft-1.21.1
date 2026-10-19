package thaumcraft.client;

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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import thaumcraft.client.fx.particles.ResourceParticle;

/** Root wires TileEssentiaCrystalizer.setClientEffects(CrystallizerClientEffects::vent).
 * Engine owns the seven native vent ticks; this owns ONLY the original client FX.
 * Accepted FXVent is untouched; its tube-specific direction must not be reused. */
public final class CrystallizerClientEffects {
    private CrystallizerClientEffects() {}
    public static void vent(Level world,BlockPos pos,Direction outward){
        if(!(world instanceof ClientLevel level) || outward==null)return;
        // Literal TileEssentiaCrystalizer:273-280 order, six CLIENT world draws.
        // Sample before proxy-distance rejection, as in the original caller.
        float x=.1F-level.random.nextFloat()*.2F,z=.1F-level.random.nextFloat()*.2F,y=.1F-level.random.nextFloat()*.2F;
        float dx=.1F-level.random.nextFloat()*.2F,dz=.1F-level.random.nextFloat()*.2F,dy=.1F-level.random.nextFloat()*.2F;
        var camera=Minecraft.getInstance().getCameraEntity();int range=Minecraft.useFancyGraphics()?50:25;if(camera==null || camera.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>range*range)return;
        Minecraft.getInstance().particleEngine.add(new Vent(level,pos.getX()+.5F+x+outward.getStepX()/2.1F,pos.getY()+.5F+y+outward.getStepY()/2.1F,pos.getZ()+.5F+z+outward.getStepZ()/2.1F,outward.getStepX()/4F+dx,outward.getStepY()/4F+dy,outward.getStepZ()/4F+dz));
    }
    /** Native ParticleEngine form of the same original white FXVent behaviour.
     * Particle-local heading randomness is distinct from the six caller draws. */
    private static final class Vent extends SingleQuadParticle {
        private static final Tesselator BUFFER=new Tesselator(256);private float scale;
        private Vent(ClientLevel level,double x,double y,double z,double dx,double dy,double dz){super(level,x,y,z);setSize(.02F,.02F);scale=random.nextFloat()*.1F+.05F;hasPhysics=false;setColor(1,1,1);float length=Mth.sqrt((float)(dx*dx+dy*dy+dz*dz));xd=heading(dx/length);yd=heading(dy/length);zd=heading(dz/length);}
        private double heading(double unit){return(unit+random.nextGaussian()*(random.nextBoolean()?-1:1)*.0075F*5)*.125F;}
        @Override public void tick(){xo=x;yo=y;zo=z;++age;if(scale>1)remove();yd+=.0025;move(xd,yd,zd);xd*=.8500000190734863;yd*=.8500000190734863;zd*=.8500000190734863;if(scale<1)scale*=1.15F;if(onGround){xd*=.7F;zd*=.7F;}}
        @Override public float getQuadSize(float partialTick){return .3F*scale;}
        @Override protected float getU0(){return(int)(1+scale*4)%16/16F;}
        @Override protected float getU1(){return getU0()+.0624375F;}
        @Override protected float getV0(){return(int)(1+scale*4)/16/16F;}
        @Override protected float getV1(){return getV0()+.0624375F;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        @Override public void render(VertexConsumer unused,Camera camera,float partialTick){var shader=RenderSystem.getShader();int texture=RenderSystem.getShaderTexture(0);try{RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,ResourceParticle.TEXTURE);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);alpha=.4F*Math.max(0,1-scale);var vertices=BUFFER.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);super.render(vertices,camera,partialTick);BufferUploader.drawWithShader(vertices.buildOrThrow());}finally{RenderSystem.setShader(()->shader);RenderSystem.setShaderTexture(0,texture);RenderSystem.defaultBlendFunc();RenderSystem.disableBlend();RenderSystem.depthMask(true);}}
    }
}

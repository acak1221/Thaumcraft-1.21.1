package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import thaumcraft.client.renderers.VisNetRender;

/** Original FXBubble(age1), additive particles row1, block-light240, noclip .002 lift and final three pop cells. */
public final class SanityBubbleParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    private int remaining,frame=16;
    public SanityBubbleParticle(ClientLevel level,double x,double y,double z,float r,float g,float b){this(level,x,y,z,r,g,b,1);}
    public SanityBubbleParticle(ClientLevel level,double x,double y,double z,float r,float g,float b,int initialAge){
        super(level,x,y,z);setSize(.02F,.02F);hasPhysics=false;quadSize*=random.nextFloat()*.3F+.2F;setColor(r,g,b);
        xd=(double)((float)(Math.random()*2D-1D)*.02F);yd=(double)((float)Math.random()*.02F);zd=(double)((float)(Math.random()*2D-1D)*.02F);
        lifetime=remaining=(int)((double)(initialAge+2)+8D/(Math.random()*.8D+.2D));
        var client=Minecraft.getInstance();var camera=client.getCameraEntity();int visible=client.options.graphicsMode().get()==GraphicsStatus.FAST?25:50;
        if(camera==null||camera.distanceToSqr(x,y,z)>(double)(visible*visible))lifetime=remaining=0;
        xo=x;yo=y;zo=z;
    }
    @Override public void setAlpha(float value){super.setAlpha(value);}
    private boolean valid(){return Minecraft.getInstance().level==level&&level.hasChunkAt(BlockPos.containing(x,y,z));}
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(!valid()){remove();return;}
        yd+=.002D;xd+=(double)((level.random.nextFloat()-level.random.nextFloat())*.01F);zd+=(double)((level.random.nextFloat()-level.random.nextFloat())*.01F);
        x+=xd;y+=yd;z+=zd;xd*=(double).85F;yd*=(double).85F;zd*=(double).85F;age++;
        if(remaining--<=0)remove();else if(remaining<=2)frame++;
    }
    @Override public float getQuadSize(float partial){return quadSize;}
    @Override protected int getLightColor(float partial){return 240;}
    @Override protected float getU0(){return frame%16/16F;}
    @Override protected float getU1(){return getU0()+.0624375F;}
    @Override protected float getV0(){return frame/16/16F;}
    @Override protected float getV1(){return getV0()+.0624375F;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    @Override public void render(VertexConsumer ignored,Camera camera,float partial){
        if(valid()&&isAlive())VisNetRender.drawParticle(VisNetRender.PARTICLES,MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial));
    }
}

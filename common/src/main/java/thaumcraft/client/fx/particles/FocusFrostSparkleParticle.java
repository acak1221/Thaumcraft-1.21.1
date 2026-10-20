package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.projectile.EntityFrostShard;

/** Original frosty shard's white type6 sparkles,18ticks,gravity.005,shrink/drag. */
public final class FocusFrostSparkleParticle extends Particle {
    private final float size;
    private FocusFrostSparkleParticle(ClientLevel level,double x,double y,double z) {
        super(level,x,y,z);lifetime=18;hasPhysics=false;xd=yd=zd=0;xo=x;yo=y;zo=z;
        size=.04F*(random.nextFloat()*.5F+.5F)*2;
        setColor(.8F+level.random.nextFloat()*.2F,.8F+level.random.nextFloat()*.2F,.8F+level.random.nextFloat()*.2F);
        setSize(.01F,.01F);
    }
    public static void trail(EntityFrostShard shard) {
        if(!(shard.level() instanceof ClientLevel level) || Minecraft.getInstance().level!=level)return;
        var client=Minecraft.getInstance();var setting=client.options.particles().get();
        int count=setting==ParticleStatus.MINIMAL?0:setting==ParticleStatus.DECREASED?2:4;
        float size=shard.getDamage()/10F;
        for(int i=0;i<shard.getFrosty();i++) {
            double x=(float)shard.getX()-size+shard.getRandom().nextFloat()*size*2;
            double y=(float)shard.getY()-size+shard.getRandom().nextFloat()*size*2;
            double z=(float)shard.getZ()-size+shard.getRandom().nextFloat()*size*2;
            if(level.random.nextInt(6)<count)client.particleEngine.add(new FocusFrostSparkleParticle(level,x,y,z));
        }
    }
    @Override public void tick() {
        xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}
        yd-=.04*.005F;move(xd,yd,zd);xd*=.9080000019073486;yd*=.9080000019073486;zd*=.9080000019073486;
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    @Override public void render(VertexConsumer unused,Camera camera,float partial) {
        var pose=new PoseStack();var eye=camera.getPosition();
        pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(camera.rotation());
        float u=((16+age/6)%4)/16F;
        int rgb=(int)(rCol*255)<<16|(int)(gCol*255)<<8|(int)(bCol*255);
        VisNetRender.draw(VisNetRender.PARTICLES,GL11.GL_ONE,false,out->
                VisNetRender.quad(out,pose,size*(lifetime-age+1)/lifetime,1,rgb,u,u+.0624375F,.25F,.3124375F));
    }
}

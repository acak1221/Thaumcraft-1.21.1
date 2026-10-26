package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.bolt.WispLightningShape;
import thaumcraft.client.renderers.VisNetRender;

/** Original Harness type6, duration1,multiplier2,speed3,width.015;
 * shared fractal math retains the independent Wisp default constructor. */
public final class HarnessBoltParticle extends Particle {
    private final WispLightningShape bolt;
    private int boltAge;
    private HarnessBoltParticle(ClientLevel level,Vec3 from,Vec3 to,long seed) {
        super(level,from.x,from.y,from.z);
        // The original ModelHoverHarness: FXLightningBolt(world, x, y, z, px, py, pz, seed, 1, 2.0f, 3).
        bolt=new WispLightningShape(from,to,seed,1,2F,3);boltAge=-(int)(bolt.length*3);
        xd=yd=zd=0;hasPhysics=false;setSize(.01F,.01F);
    }
    public static void emit(ClientLevel level,Vec3 from,Vec3 to,long seed) {
        if(from.distanceToSqr(to)<1e-10)return;Minecraft.getInstance().particleEngine.add(new HarnessBoltParticle(level,from,to,seed));
    }
    @Override public void tick() {boltAge=Math.min(bolt.maxAge,boltAge+bolt.increment);if(boltAge>=bolt.maxAge)remove();}
    @Override public ParticleRenderType getRenderType() {return ParticleRenderType.CUSTOM;}
    @Override public void render(VertexConsumer unused,Camera camera,float partial) {
        var player=Minecraft.getInstance().player;if(player==null)return;int range=Minecraft.useFancyGraphics()?100:50;if(player.distanceToSqr(x,y,z)>range*range)return;
        float fraction=boltAge>=0?boltAge/(float)bolt.maxAge:0;int travel=(int)(bolt.length*3);
        int reach=travel==0?bolt.mainSegments:(int)((boltAge+partial+travel)/(float)travel*bolt.mainSegments);
        PoseStack pose=new PoseStack();var eye=camera.getPosition();pose.translate(-eye.x,-eye.y,-eye.z);var look=camera.getLookVector();Vec3 view=new Vec3(look.x,look.y,look.z);
        for(int pass=0;pass<2;pass++) {
            float alpha=pass==0?(1-fraction)*.4F:1-fraction*.5F;
            VisNetRender.draw(pass==0?VisNetRender.BOLT_LARGE:VisNetRender.BOLT_SMALL,GL11.GL_ONE_MINUS_SRC_ALPHA,false,VisNetRender.lowAlphaShader(),out->{
                for(var segment:bolt.segments) {
                    if(segment.number>reach)continue;
                    // The original divides the ribbon width by the joint sine; a sine the normalised dot product
                    // pushed out of the unit interval made the original vertex coordinates not a number and the
                    // original rasteriser dropped the quad. The port drops the same segment.
                    if(!segment.renderable())continue;Vec3 start=segment.start(),end=segment.end();
                    float dx=(float)player.getX()-(float)start.x,dy=(float)player.getY()-(float)start.y,dz=(float)player.getZ()-(float)start.z;
                    float distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz),width=.015F*(distance/5+1)*(1+segment.light)*.5F;
                    Vec3 before=view.cross(segment.previousDirection()).scale(width/segment.sinPrevious()),after=view.cross(segment.nextDirection()).scale(width/segment.sinNext());float opacity=alpha*segment.light;
                    vertex(out,pose,end.subtract(after),.5F,0,opacity);vertex(out,pose,start.subtract(before),.5F,0,opacity);vertex(out,pose,start.add(before),.5F,1,opacity);vertex(out,pose,end.add(after),.5F,1,opacity);
                    if(segment.last()) {Vec3 rounded=end.add(segment.direction().normalize().scale(width));vertex(out,pose,rounded.subtract(after),0,0,opacity);vertex(out,pose,end.subtract(after),.5F,0,opacity);vertex(out,pose,end.add(after),.5F,1,opacity);vertex(out,pose,rounded.add(after),0,1,opacity);}
                    if(segment.first()) {Vec3 rounded=start.subtract(segment.direction().normalize().scale(width));vertex(out,pose,start.subtract(before),.5F,0,opacity);vertex(out,pose,rounded.subtract(before),0,0,opacity);vertex(out,pose,rounded.add(before),0,1,opacity);vertex(out,pose,start.add(before),.5F,1,opacity);}
                }
            });
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 point,float u,float v,float alpha) {VisNetRender.vertex(out,pose,point,u,v,0xbfffff,alpha);}
}

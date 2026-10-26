package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.bolt.WispLightningShape;
import thaumcraft.client.renderers.VisNetRender;

/** Original type0, speed4 purple wisp bolt: seven fractals, joined ribbon ends, distance width, large/small passes. */
public final class WispBoltParticle extends Particle {
    private final WispLightningShape bolt;
    private int boltAge;
    private WispBoltParticle(ClientLevel level,Entity source,Entity target) {
        super(level,source.getX(),source.getY(),source.getZ());
        // The original ClientProxy.bolt: FXLightningBolt(world, source, target, seed, 4), that is the entity to
        // entity flavour of FXLightningBoltCommon with the age step four and the multiplier 0.4f.
        bolt=new WispLightningShape(source.position(),new Vec3(target.getX(),target.getEyeY()-.7F,target.getZ()),level.random.nextLong(),4);
        boltAge=-(int)(bolt.length*3);xd=yd=zd=0;hasPhysics=false;setSize(.01F,.01F);
    }
    public static void accept(int sourceId,int targetId) {
        var client=Minecraft.getInstance();var level=client.level;if(level==null || client.player==null)return;
        Entity source=level.getEntity(sourceId),target=level.getEntity(targetId);
        if(source==null || target==null)return;
        Vec3 end=new Vec3(target.getX(),target.getEyeY()-.7F,target.getZ());
        // Zero-length geometry made the original float fractal normalize(0), producing NaN vertices.
        if(source.position().distanceToSqr(end)<1.0e-10)return;
        client.particleEngine.add(new WispBoltParticle(level,source,target));
    }
    @Override public void tick() { boltAge=Math.min(bolt.maxAge,boltAge+bolt.increment);if(boltAge>=bolt.maxAge)remove(); }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused,Camera camera,float partial) {
        var client=Minecraft.getInstance();var player=client.player;if(player==null)return;
        int range=Minecraft.useFancyGraphics()?100:50;if(player.distanceToSqr(x,y,z)>range*range)return;
        float fraction=boltAge>=0?boltAge/(float)bolt.maxAge:0;
        int travel=(int)(bolt.length*3);
        int reach=travel==0?bolt.mainSegments:(int)((boltAge+partial+travel)/(float)travel*bolt.mainSegments);
        PoseStack pose=new PoseStack();var eye=camera.getPosition();pose.translate(-eye.x,-eye.y,-eye.z);
        var look=camera.getLookVector();Vec3 view=new Vec3(look.x,look.y,look.z);
        for(int pass=0;pass<2;pass++) {
            final int layer=pass;final float alpha=pass==0?(1-fraction)*.4F:1-fraction*.5F;
            int rgb=pass==0?0x994c99:0xff99ff;
            VisNetRender.draw(pass==0?VisNetRender.BOLT_LARGE:VisNetRender.BOLT_SMALL,GL11.GL_ONE,false,VisNetRender.lowAlphaShader(),out -> {
                for(var segment:bolt.segments) {
                    if(segment.number>reach)continue;
                    // The original divides the ribbon width by the joint sine; a sine the normalised dot product
                    // pushed out of the unit interval made the original vertex coordinates not a number and the
                    // original rasteriser dropped the quad. The port drops the same segment.
                    if(!segment.renderable())continue;
                    Vec3 start=segment.start(),end=segment.end();
                    float dx=(float)player.getX()-(float)start.x,dy=(float)player.getY()-(float)start.y,dz=(float)player.getZ()-(float)start.z;
                    float distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
                    float width=.03F*(distance/5+1)*(1+segment.light)*.5F;
                    Vec3 before=view.cross(segment.previousDirection()).scale(width/segment.sinPrevious()),after=view.cross(segment.nextDirection()).scale(width/segment.sinNext());
                    float opacity=alpha*segment.light;
                    vertex(out,pose,end.subtract(after),.5F,0,rgb,opacity);vertex(out,pose,start.subtract(before),.5F,0,rgb,opacity);
                    vertex(out,pose,start.add(before),.5F,1,rgb,opacity);vertex(out,pose,end.add(after),.5F,1,rgb,opacity);
                    if(segment.last()) {
                        Vec3 rounded=end.add(segment.direction().normalize().scale(width));
                        vertex(out,pose,rounded.subtract(after),0,0,rgb,opacity);vertex(out,pose,end.subtract(after),.5F,0,rgb,opacity);
                        vertex(out,pose,end.add(after),.5F,1,rgb,opacity);vertex(out,pose,rounded.add(after),0,1,rgb,opacity);
                    }
                    if(segment.first()) {
                        Vec3 rounded=start.subtract(segment.direction().normalize().scale(width));
                        vertex(out,pose,start.subtract(before),.5F,0,rgb,opacity);vertex(out,pose,rounded.subtract(before),0,0,rgb,opacity);
                        vertex(out,pose,rounded.add(before),0,1,rgb,opacity);vertex(out,pose,start.add(before),.5F,1,rgb,opacity);
                    }
                }
            });
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 point,float u,float v,int rgb,float alpha) { VisNetRender.vertex(out,pose,point,u,v,rgb,alpha); }
}

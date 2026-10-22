package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.tiles.TileNodeConverter;

/** Real native client callbacks only: validated committed-drain payloads and
 * converter's original client tick / event10,10. No world stock or gameplay API.
 * ParticleEngine owns lifetime/world cleanup. No retained Level in static maps. */
public final class VisNetClientEffects {
    private static final SoundEvent CRAFT_FAIL=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","craftfail"));
    private static long drains,bolts,bursts;private static BlockPos lastSupplier,lastConsumer;private static ResourceLocation lastDimension;
    public record Diagnostics(long drains,long bolts,long bursts,BlockPos lastSupplier,BlockPos lastConsumer,ResourceLocation lastDimension) {}
    public static Diagnostics diagnostics(){return new Diagnostics(drains,bolts,bursts,lastSupplier,lastConsumer,lastDimension);}
    public static final TileNodeConverter.ClientEffects CONVERTER_EFFECTS=new TileNodeConverter.ClientEffects(){
        @Override public void bolt(Level world,double x,double y,double z,double tx,double ty,double tz){if(!(world instanceof ClientLevel level) || Minecraft.getInstance().level!=level || !finite(x,y,z,tx,ty,tz))return;Vec3 start=new Vec3(x,y,z),end=new Vec3(tx,ty,tz);if(start.distanceToSqr(end)>64)return;Minecraft.getInstance().particleEngine.add(new ConverterBolt(level,start,end));++bolts;}
        @Override public void burst(Level world,double x,double y,double z,float size){if(!(world instanceof ClientLevel level) || Minecraft.getInstance().level!=level || !finite(x,y,z,size) || size<=0 || size>4)return;Minecraft.getInstance().particleEngine.add(new ConverterBurst(level,x,y,z,size));level.playLocalSound(x,y,z,CRAFT_FAIL,SoundSource.BLOCKS,.5F,1,false);++bursts;}
    };
    private VisNetClientEffects() {}
    public static void setShader(ShaderInstance shader){VisNetRender.setShader(shader);}
    /** Supplier FIRST, consumer SECOND. Root's consumer-first payload wires
     * drain(level,payload.target(),payload.origin(),payload.rgb()). The supplier
     * is the real selected relay/source; the separate parent beam reaches root. */
    public static void drain(Level world,BlockPos supplier,BlockPos consumer,Aspect aspect){if(aspect!=null && Aspect.getAspect(aspect.getTag())==aspect && aspect.isPrimal())drain(world,supplier,consumer,aspect.getColor()&0xffffff);}
    public static void drain(Level world,BlockPos supplier,BlockPos consumer,int colour){
        if(!(world instanceof ClientLevel level) || Minecraft.getInstance().level!=level || supplier==null || consumer==null || !level.hasChunkAt(supplier) || !level.hasChunkAt(consumer) || level.isOutsideBuildHeight(supplier) || level.isOutsideBuildHeight(consumer) || supplier.distSqr(consumer)<=0 || supplier.distSqr(consumer)>64*64)return;
        boolean primal=false;for(Aspect aspect:Aspect.getPrimalAspects())if((aspect.getColor()&0xffffff)==colour){primal=true;break;}if(!primal)return;
        // Original PacketFXVisDrain75: six CLIENT world jitter samples. Particle
        // steering randomness is private to its own native Particle instance.
        Vec3 start=new Vec3(supplier.getX()+.4+level.random.nextFloat()*.2F,supplier.getY()+.4+level.random.nextFloat()*.2F,supplier.getZ()+.4+level.random.nextFloat()*.2F),end=new Vec3(consumer.getX()+(double)level.random.nextFloat(),consumer.getY()+(double)level.random.nextFloat(),consumer.getZ()+(double)level.random.nextFloat());
        Minecraft.getInstance().particleEngine.add(new DrainSparkle(level,start,end,colour));++drains;lastSupplier=supplier.immutable();lastConsumer=consumer.immutable();lastDimension=level.dimension().location();
    }
    private static boolean finite(double... values){for(double value:values)if(!Double.isFinite(value))return false;return true;}
    private abstract static class NativeEffect extends Particle {
        NativeEffect(ClientLevel level,double x,double y,double z){super(level,x,y,z,0,0,0);xd=yd=zd=0;hasPhysics=false;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
        boolean expired(){if(Minecraft.getInstance().level!=level || age++>=lifetime){remove();return true;}return false;}
        PoseStack pose(Camera camera,float partial){PoseStack pose=new PoseStack();pose.translate(Mth.lerp(partial,xo,x)-camera.getPosition().x,Mth.lerp(partial,yo,y)-camera.getPosition().y,Mth.lerp(partial,zo,z)-camera.getPosition().z);return pose;}
    }
    /** Original FXVisSparkle: 1000 limit, .985 drag, .1 steering/per-axis cap,
     * age/45..59 growth, near-target shrink, particles row8/16 and alpha.5. */
    private static final class DrainSparkle extends NativeEffect implements thaumcraft.client.fx.Tc4Particle {
        final Vec3 target;final int colour;final float sizeMod;float scale;
        DrainSparkle(ClientLevel level,Vec3 start,Vec3 target,int colour){super(level,start.x,start.y,start.z);this.target=target;this.colour=colour;lifetime=1000;xd=random.nextGaussian()*.01;yd=random.nextGaussian()*.01;zd=random.nextGaussian()*.01;sizeMod=45+random.nextInt(15);}
        @Override public void tick(){xo=x;yo=y;zo=z;if(expired())return;move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;Vec3 delta=target.subtract(x,y,z);double distance=delta.length();if(distance<2)scale*=.95F;if(distance<.2){lifetime=age;return;}if(age<10)scale=age/sizeMod;Vec3 direction=delta.scale(.1F/distance);xd=Mth.clamp((float)(xd+direction.x),-.1F,.1F);yd=Mth.clamp((float)(yd+direction.y),-.1F,.1F);zd=Mth.clamp((float)(zd+direction.z),-.1F,.1F);}
        @Override public void render(VertexConsumer ignored,Camera camera,float partial){PoseStack pose=pose(camera,partial);pose.mulPose(camera.rotation());float half=.1F*scale*(Mth.sin(age/3F)*.3F+6);float u=(age%16)/16F;VisNetRender.draw(VisNetRender.PARTICLES,GL11.GL_ONE,false,out->VisNetRender.quad(out,pose,half,.5F,colour,u,u+.0624375F,.5F,.5624375F));}
    }
    /** Original FXBurst: frame0..31 of nodes row31; fixed position/lifetime31. */
    private static final class ConverterBurst extends NativeEffect {
        final float scale;
        ConverterBurst(ClientLevel level,double x,double y,double z,float size){super(level,x,y,z);lifetime=31;scale=(random.nextFloat()*.5F+.5F)*2*size;}
        @Override public void tick(){xo=x;yo=y;zo=z;expired();}
        @Override public void render(VertexConsumer ignored,Camera camera,float partial){PoseStack pose=pose(camera,partial);VisNetRender.billboard(pose,VisNetRender.NODES,0,scale,1,32,31,age%32,0xffffff,GL11.GL_ONE);}
    }
    /** Native bounded visual adaptation of the original duration10/speed5,
     * multiplier4 seven midpoint subdivisions and type0 two-pass lightning.
     * Branch fan/collision-light joins are NOT claimed as full fractal parity. */
    private static final class ConverterBolt extends NativeEffect {
        final List<Vec3> points;final int renderTravel;
        ConverterBolt(ClientLevel level,Vec3 start,Vec3 end){super(level,start.x,start.y,start.z);Random local=new Random(level.random.nextLong());lifetime=10+local.nextInt(10)-5;age=-(int)(start.distanceTo(end)*5);renderTravel=Math.max(1,(int)(start.distanceTo(end)*3));var path=new ArrayList<Vec3>();path.add(start);path.add(end);float[] divisors={8,12,17,23,30,34,40};double length=start.distanceTo(end);for(float divisor:divisors){var next=new ArrayList<Vec3>();next.add(path.getFirst());for(int i=1;i<path.size();i++){Vec3 a=path.get(i-1),b=path.get(i),mid=a.add(b).scale(.5),direction=b.subtract(a).normalize(),side=direction.cross(Math.abs(direction.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),other=direction.cross(side);double amount=length*4/divisor;next.add(mid.add(side.scale((local.nextFloat()-.5F)*amount)).add(other.scale((local.nextFloat()-.5F)*amount)));next.add(b);}path=next;}points=List.copyOf(path);}
        @Override public void tick(){xo=x;yo=y;zo=z;if(Minecraft.getInstance().level!=level || ++age>=lifetime)remove();}
        @Override public void render(VertexConsumer ignored,Camera camera,float partial){float progress=Mth.clamp((age+renderTravel+partial)/renderTravel,0,1),boltAge=age>=0?(age+partial)/lifetime:0;if(progress<=0)return;int visible=Math.min(points.size()-1,(int)((points.size()-1)*progress));if(visible==0)return;PoseStack pose=new PoseStack();Vec3 eye=camera.getPosition();
            for(int pass=0;pass<2;pass++){int colour=pass==0?0x994c99:0xff99ff;float opacity=pass==0?(1-boltAge)*.4F:1-boltAge*.5F;VisNetRender.draw(pass==0?VisNetRender.BOLT_LARGE:VisNetRender.BOLT_SMALL,GL11.GL_ONE,false,out->{for(int i=1;i<=visible;i++){Vec3 a=points.get(i-1).subtract(eye),b=points.get(i).subtract(eye),cross=a.cross(b.subtract(a)).normalize().scale(.025*(a.length()/5+1));VisNetRender.vertex(out,pose,a.subtract(cross),.5F,0,colour,opacity);VisNetRender.vertex(out,pose,b.subtract(cross),.5F,0,colour,opacity);VisNetRender.vertex(out,pose,b.add(cross),.5F,1,colour,opacity);VisNetRender.vertex(out,pose,a.add(cross),.5F,1,colour,opacity);}});}
        }
    }
}

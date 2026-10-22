package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.config.ThaumcraftTaint;

/** Original FXSwarm's eight animated cells, curved noisy pursuit/repulsion,
 * liquid-aware push-out, target-death50 fade and shared three-buzz timer. */
public final class TaintSwarmParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return 1; }
    private static final ArrayList<Long> BUZZ=new ArrayList<>();
    private final Entity target;
    private final float size,speed,turn;
    private int deathTimer;
    private float yaw,pitch;
    private TaintSwarmParticle(ClientLevel level,double x,double y,double z,Entity target,float r,float g,float b,float speed,float turn,float gravity){
        super(level,x,y,z);this.target=target;this.speed=speed;this.turn=turn;this.gravity=gravity;
        setColor(r,g,b);size=random.nextFloat()*.5F+1;
        xd=(random.nextFloat()-random.nextFloat())*.2F;yd=(random.nextFloat()-random.nextFloat())*.2F;zd=(random.nextFloat()-random.nextFloat())*.2F;
        hasPhysics=true;setSize(.2F,.2F);
        Entity camera=Minecraft.getInstance().getCameraEntity();int range=Minecraft.useFancyGraphics()?64:32;
        if(camera==null||camera.distanceToSqr(x,y,z)>range*range)remove();
    }
    public static TaintSwarmParticle spawn(Entity target,float speed,float turn,float gravity){
        if(!(target.level() instanceof ClientLevel level)||Minecraft.getInstance().level!=level)return null;
        double x=target.getX()+(double)((level.random.nextFloat()-level.random.nextFloat())*2F);
        double y=target.getY()+(double)((level.random.nextFloat()-level.random.nextFloat())*2F);
        double z=target.getZ()+(double)((level.random.nextFloat()-level.random.nextFloat())*2F);
        var fx=new TaintSwarmParticle(level,x,y,z,target,.8F+level.random.nextFloat()*.2F,level.random.nextFloat()*.4F,1-level.random.nextFloat()*.2F,speed,turn,gravity);
        Minecraft.getInstance().particleEngine.add(fx);return fx;
    }
    public static void resetBuzz(){BUZZ.clear();}
    private boolean targetGone(){return target.isRemoved()||target instanceof LivingEntity living&&living.deathTime>0;}
    @Override public void tick(){
        xo=x;yo=y;zo=z;++age;boolean gone=targetGone();
        if(gone){++deathTimer;yd-=(double)(gravity/2F);if(deathTimer>50){remove();return;}}else yd+=(double)gravity;
        pushOut();move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;
        if(!gone){
            boolean hurt=target instanceof LivingEntity living&&living.hurtTime>0;
            int bound=(int)(turn/2F);float turnYaw=turn/2F+random.nextInt(bound),turnPitch=turn/2F+random.nextInt(bound);
            if(distanceToTargetSquared()<=target.getBbWidth()||hurt){turnYaw=-turnYaw;turnPitch=-turnPitch;}
            face(turnYaw,turnPitch);
            double mx=-Mth.sin(yaw/180F*(float)Math.PI)*Mth.cos(pitch/180F*(float)Math.PI),mz=Mth.cos(yaw/180F*(float)Math.PI)*Mth.cos(pitch/180F*(float)Math.PI),my=-Mth.sin(pitch/180F*(float)Math.PI);
            float length=(float)Math.sqrt(mx*mx+my*my+mz*mz);mx/=length;my/=length;mz/=length;
            mx+=random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075F*15F;
            my+=random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075F*15F;
            mz+=random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075F*15F;
            xd=mx*(double)speed;yd=my*(double)speed;zd=mz*(double)speed;
        }
        if(BUZZ.size()<3&&random.nextInt(50)==0&&level.getNearestPlayer(x,y,z,8,false)!=null){
            level.playLocalSound(x,y,z,SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","fly")),SoundSource.AMBIENT,.03F,.5F+random.nextFloat()*.4F,false);
            BUZZ.add(System.nanoTime()+1500000L);
        }
        if(BUZZ.size()>=3&&BUZZ.getFirst()<System.nanoTime())BUZZ.removeFirst();
    }
    private double distanceToTargetSquared(){double a=x-target.getX(),b=y-target.getY(),c=z-target.getZ();return a*a+b*b+c*c;}
    private void face(float maxYaw,float maxPitch){
        double dx=target.getX()-x,dz=target.getZ()-z,dy=(target.getBoundingBox().minY+target.getBoundingBox().maxY)/2-(getBoundingBox().minY+getBoundingBox().maxY)/2;
        double flat=Math.sqrt(dx*dx+dz*dz);float desiredYaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90,desiredPitch=(float)-(Math.atan2(dy,flat)*180/Math.PI);
        pitch=rotate(pitch,desiredPitch,maxPitch);yaw=rotate(yaw,desiredYaw,maxYaw);
    }
    private static float rotate(float old,float desired,float maximum){float delta=Mth.wrapDegrees(desired-old);if(delta>maximum)delta=maximum;if(delta< -maximum)delta=-maximum;return old+delta;}
    private void pushOut(){
        BlockPos at=BlockPos.containing(x,y,z);var state=level.getBlockState(at);
        if(state.is(ThaumcraftTaint.FIBRES.get())||state.isAir()||level.containsAnyLiquid(getBoundingBox()))return;
        double px=x-at.getX(),py=y-at.getY(),pz=z-at.getZ(),nearest=9999;int direction=-1;
        BlockPos[] cells={at.west(),at.east(),at.below(),at.above(),at.north(),at.south()};double[] gaps={px,1-px,py,1-py,pz,1-pz};
        for(int i=0;i<6;i++)if(!level.getBlockState(cells[i]).isSolidRender(level,cells[i])&&gaps[i]<nearest){nearest=gaps[i];direction=i;}
        float impulse=random.nextFloat()*.05F+.025F,lateral=(random.nextFloat()-random.nextFloat())*.1F;
        switch(direction){case 0->{xd=-impulse;yd=zd=lateral;}case 1->{xd=impulse;yd=zd=lateral;}case 2->{yd=-impulse;xd=zd=lateral;}case 3->{yd=impulse;xd=zd=lateral;}case 4->{zd=-impulse;xd=yd=lateral;}case 5->{zd=impulse;xd=yd=lateral;}default->{}}
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    @Override protected float getU0(){return (7+age%8)/16F;}
    @Override protected float getU1(){return getU0()+.0624375F;}
    @Override protected float getV0(){return .25F;}
    @Override protected float getV1(){return .3124375F;}
    @Override protected int getLightColor(float partial){return 240;}
    @Override public float getQuadSize(float partial){return .1F*size*(Mth.sin(age/3F)*.25F+1);}
    @Override public void render(VertexConsumer unused,Camera camera,float partial){
        boolean hurt=!(target instanceof LivingEntity living)||living.hurtTime>0;
        float beforeG=gCol,beforeB=bCol,beforeAlpha=alpha;if(hurt){gCol*=.5F;bCol*=.5F;}alpha=(50F-deathTimer)/50F;
        try{VisNetRender.drawParticle(VisNetRender.PARTICLES,GL11.GL_ONE_MINUS_SRC_ALPHA,MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial));}
        finally{gCol=beforeG;bCol=beforeB;alpha=beforeAlpha;}
    }
}

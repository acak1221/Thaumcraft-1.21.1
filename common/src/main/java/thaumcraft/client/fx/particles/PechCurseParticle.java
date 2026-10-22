package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.projectile.EntityPechBlast;

/** The Pech's original FXWisp2/3 and FXSparkle(type5,size1.5,multiplier6).
 * Native particle lifetime/removal and voxel collision replace EntityFX. Both
 * additive motes and standard-alpha dark sparkle retain the original1/255
 * shader threshold, full light, exact atlas cells, gravity and drag. */
public final class PechCurseParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return sparkle ? 1 : 0; }
    private final boolean sparkle;
    private final float originalSize;
    private PechCurseParticle(ClientLevel level,double x,double y,double z,int type,boolean impact){
        super(level,x,y,z);sparkle=type==5;xd=yd=zd=0;xo=x;yo=y;zo=z;
        float scale=(random.nextFloat()*.5F+.5F)*2;
        originalSize=(sparkle?.1F*1.5F:.5F*.3F)*scale;quadSize=originalSize;
        lifetime=sparkle?18:(int)(36/(Math.random()*.3+.7));alpha=sparkle?1:.5F;
        gravity=sparkle?0:.02F;hasPhysics=impact;setSize(sparkle?.01F:.1F,sparkle?.01F:.1F);
        switch(type){
            case 0->setColor(.75F+level.random.nextFloat()*.25F,.25F+level.random.nextFloat()*.25F,.75F+level.random.nextFloat()*.25F);
            case 2->setColor(.2F,.2F,.7F+level.random.nextFloat()*.3F);
            case 3->setColor(.2F,.7F+level.random.nextFloat()*.3F,.2F);
            case 5->setColor(level.random.nextFloat()*.1F,level.random.nextFloat()*.1F,level.random.nextFloat()*.1F);
            default->throw new IllegalArgumentException("Unknown Pech particle type");
        }
        // Only FXWisp has the original constructor's graphics-distance cull.
        if(!sparkle){var camera=Minecraft.getInstance().getCameraEntity();int range=Minecraft.useFancyGraphics()?50:25;if(camera!=null&&camera.distanceToSqr(x,y,z)>range*range)lifetime=0;}
    }
    private static void add(PechCurseParticle particle){Minecraft.getInstance().particleEngine.add(particle);}
    public static void trail(EntityPechBlast blast){
        if(!(blast.level() instanceof ClientLevel level))return;var before=blast.previousTrailPosition();
        for(int i=0;i<3;i++){
            add(new PechCurseParticle(level,blast.getX()+spread(level,.2F),blast.getY()+spread(level,.2F),blast.getZ()+spread(level,.2F),3,false));
            add(new PechCurseParticle(level,(blast.getX()+before.x)/2+spread(level,.2F),(blast.getY()+before.y)/2+spread(level,.2F),(blast.getZ()+before.z)/2+spread(level,.2F),2,false));
            float x=(float)blast.getX()+spread(level,.1F),y=(float)blast.getY()+spread(level,.1F),z=(float)blast.getZ()+spread(level,.1F);
            var setting=Minecraft.getInstance().options.particles().get();int count=setting==ParticleStatus.MINIMAL?0:setting==ParticleStatus.DECREASED?2:4;
            if(level.random.nextInt(6)<count)add(new PechCurseParticle(level,x,y,z,5,false));
        }
    }
    public static void impact(EntityPechBlast blast){
        if(!(blast.level() instanceof ClientLevel level))return;
        for(int i=0;i<9;i++)for(int type:new int[]{3,2,0}){
            float x=spread(level,.3F),y=spread(level,.3F),z=spread(level,.3F);
            var particle=new PechCurseParticle(level,blast.getX()+x,blast.getY()+y,blast.getZ()+z,type,true);
            if(particle.lifetime>0){particle.xd=(blast.getX()+(double)(x*8F)-particle.x)/particle.lifetime;particle.yd=(blast.getY()+(double)(y*8F)-particle.y)/particle.lifetime;particle.zd=(blast.getZ()+(double)(z*8F)-particle.z)/particle.lifetime;}
            add(particle);
        }
    }
    private static float spread(ClientLevel level,float scale){return(level.random.nextFloat()-level.random.nextFloat())*scale;}
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}yd-=.04*gravity;
        if(hasPhysics)pushOut();move(xd,yd,zd);
        double drag=sparkle?.9080000019073486:(double).98F;xd*=drag;yd*=drag;zd*=drag;if(onGround){xd*=.7F;zd*=.7F;}
    }
    private void pushOut(){
        BlockPos pos=BlockPos.containing(x,y,z);
        if(level.isEmptyBlock(pos)||!level.getBlockState(pos).isCollisionShapeFullBlock(level,pos)||level.containsAnyLiquid(getBoundingBox()))return;
        double[] distances={x-pos.getX(),1-(x-pos.getX()),y-pos.getY(),1-(y-pos.getY()),z-pos.getZ(),1-(z-pos.getZ())};
        Direction[] directions={Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH};
        int side=-1;double nearest=9999;
        for(int i=0;i<6;i++){BlockPos neighbor=pos.relative(directions[i]);if(!level.getBlockState(neighbor).isCollisionShapeFullBlock(level,neighbor)&&distances[i]<nearest){nearest=distances[i];side=i;}}
        float speed=random.nextFloat()*.05F+.025F,other=(random.nextFloat()-random.nextFloat())*.1F;
        if(side>=0){Direction direction=directions[side];xd=direction.getAxis()==Direction.Axis.X?direction.getStepX()*speed:other;yd=direction.getAxis()==Direction.Axis.Y?direction.getStepY()*speed:other;zd=direction.getAxis()==Direction.Axis.Z?direction.getStepZ()*speed:other;}
    }
    @Override public float getQuadSize(float partial){return originalSize*(sparkle?(float)(lifetime-age+1)/lifetime:(float)(lifetime-age)/lifetime);}
    @Override public int getLightColor(float partial){return 240;}
    @Override protected float getU0(){return sparkle?(16+age/6)%4/16F:0;}
    @Override protected float getU1(){return getU0()+(sparkle?.0624375F:.125F);}
    @Override protected float getV0(){return sparkle?.25F:.875F;}
    @Override protected float getV1(){return getV0()+(sparkle?.0624375F:.125F);}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    @Override public void render(VertexConsumer unused,Camera camera,float partial){VisNetRender.drawParticle(VisNetRender.PARTICLES,sparkle?771:1,MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial));}
}

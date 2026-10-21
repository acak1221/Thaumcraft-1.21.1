package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;

/** Original FXWisp2/3/4 for all six primal colours, including type-five standard-alpha wisps. */
public final class PrimalWispParticle extends SingleQuadParticle {
    private final float initialSize;
    private final int destination;
    private final Entity target;
    private PrimalWispParticle(ClientLevel level,double x,double y,double z,float size,int type,Entity target,boolean physics,float gravity){
        super(level,x,y,z);this.target=target;xd=yd=zd=0;xo=x;yo=y;zo=z;
        // The original FXWisp:44 keeps the particle scale of the caller and draws the mote with the half size
        // 0.5f * scale * ageScale. The vanilla particle engine halves the quad size, so the quad size is the
        // scale itself. The original never randomises that size.
        initialSize=size;quadSize=initialSize;
        lifetime=(int)(36/(Math.random()*.3+.7));alpha=.5F;this.gravity=gravity;hasPhysics=physics;setSize(.1F,.1F);
        destination=type==5?771:1;
        switch(type){
            case 0->setColor(.75F+level.random.nextFloat()*.25F,.25F+level.random.nextFloat()*.25F,.75F+level.random.nextFloat()*.25F);
            case 1->setColor(.5F+level.random.nextFloat()*.3F,.5F+level.random.nextFloat()*.3F,.2F);
            case 2->setColor(.2F,.2F,.7F+level.random.nextFloat()*.3F);
            case 3->setColor(.2F,.7F+level.random.nextFloat()*.3F,.2F);
            case 4->setColor(.7F+level.random.nextFloat()*.3F,.2F,.2F);
            case 5->setColor(level.random.nextFloat()*.1F,level.random.nextFloat()*.1F,level.random.nextFloat()*.1F);
            default->throw new IllegalArgumentException("Unknown original primal wisp type");
        }
        var camera=Minecraft.getInstance().getCameraEntity();int range=Minecraft.useFancyGraphics()?50:25;
        if(camera!=null&&camera.distanceToSqr(x,y,z)>range*range)lifetime=0;
    }
    private static float spread(ClientLevel level,float scale){return(level.random.nextFloat()-level.random.nextFloat())*scale;}
    private static void add(PrimalWispParticle particle){if(particle.lifetime>0)Minecraft.getInstance().particleEngine.add(particle);}
    public static void trail(EntityPrimalOrb orb){
        if(!(orb.level() instanceof ClientLevel level))return;
        // The original FX4 call passes these offsets as ABSOLUTE coordinates near origin, not orb-relative positions.
        for(int type=0;type<6;type++)add(new PrimalWispParticle(level,spread(level,.2F),spread(level,.2F),spread(level,.2F),.4F,type,orb,true,0));
        add(new PrimalWispParticle(level,orb.getX()+spread(level,.2F),orb.getY()+spread(level,.2F),orb.getZ()+spread(level,.2F),.1F,orb.getRandom().nextInt(6),null,false,0));
    }
    public static void impact(EntityPrimalOrb orb){
        if(!(orb.level() instanceof ClientLevel level))return;
        for(int a=0;a<6;a++)for(int type=0;type<6;type++){
            float x=spread(level,.5F),y=spread(level,.5F),z=spread(level,.5F);
            var mote=new PrimalWispParticle(level,orb.getX()+x,orb.getY()+y,orb.getZ()+z,.4F,type,null,true,.05F);
            if(mote.lifetime>0){mote.xd=(orb.getX()+(double)(x*10F)-mote.x)/mote.lifetime;mote.yd=(orb.getY()+(double)(y*10F)-mote.y)/mote.lifetime;mote.zd=(orb.getZ()+(double)(z*10F)-mote.z)/mote.lifetime;}
            add(mote);
        }
    }
    @Override public void tick(){
        xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}yd-=.04D*gravity;move(xd,yd,zd);
        if(target!=null){
            xd*=.985D;yd*=.985D;zd*=.985D;
            double dx=target.getX()-x,dy=target.getY()+target.getBbHeight()/2F-y,dz=target.getZ()-z;
            double distance=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            xd=Mth.clamp((float)(xd+dx/distance*.2D),-.2F,.2F);yd=Mth.clamp((float)(yd+dy/distance*.2D),-.2F,.2F);zd=Mth.clamp((float)(zd+dz/distance*.2D),-.2F,.2F);
            if(!Double.isFinite(xd+yd+zd))remove();
        }else{xd*=.98F;yd*=.98F;zd*=.98F;if(onGround){xd*=.7F;zd*=.7F;}}
    }
    @Override public float getQuadSize(float partial){return initialSize*(lifetime-age)/(float)lifetime;}
    @Override protected float getU0(){return 0;}
    @Override protected float getU1(){return .125F;}
    @Override protected float getV0(){return .875F;}
    @Override protected float getV1(){return 1;}
    @Override protected int getLightColor(float partial){return 240;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    @Override public void render(VertexConsumer ignored,Camera camera,float partial){VisNetRender.drawParticle(VisNetRender.PARTICLES,destination,MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial));}
}

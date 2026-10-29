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
public final class PrimalWispParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return destination == 1 ? 0 : 1; }
    private final float initialSize;
    private final int destination;
    private final Entity target;
    /** Original FXWisp.shrink: true fades the mote out linearly over its life, false grows it for half the life and shrinks it for the rest. */
    private final boolean shrink;
    /** Original FXWisp:44 - the quad size of the mote is the particle scale, the half size is 0.5f * scale. */
    public static float moteQuadSize(float scale) { return scale; }
    private PrimalWispParticle(ClientLevel level,double x,double y,double z,float size,int type,Entity target,boolean physics,float gravity){
        this(level,x,y,z,size,type,target,physics,gravity,true);
    }
    private PrimalWispParticle(ClientLevel level,double x,double y,double z,float size,int type,Entity target,boolean physics,float gravity,boolean shrink){
        super(level,x,y,z);this.target=target;this.shrink=shrink;xd=yd=zd=0;xo=x;yo=y;zo=z;
        // The original FXWisp:44 keeps the particle scale of the caller and draws the mote with the half size
        // 0.5f * scale * ageScale. The vanilla particle engine halves the quad size, so the quad size is the
        // scale itself. The original never randomises that size.
        initialSize=moteQuadSize(size);quadSize=initialSize;
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
    /**
     * Original {@code ClientProxy.wispFX2}: one wisp of a primal colour that drifts with the given gravity.
     *
     * @param type   0 to 5, the colour of the original types
     * @param noClip whether the wisp passes through blocks (the original {@code clip} argument sets {@code noClip})
     */
    public static void spawn(ClientLevel level,double x,double y,double z,float size,int type,boolean shrink,boolean noClip,float gravity){
        Spec spec=spec(x,y,z,size,type,shrink,noClip,gravity);
        add(new PrimalWispParticle(level,spec.x(),spec.y(),spec.z(),spec.size(),spec.type(),null,spec.physics(),spec.gravity(),spec.shrink()));
    }
    /**
     * What {@link #spawn} gives the constructor, as plain values: the facade's two adjacent booleans {@code shrink} and
     * {@code noClip} become the constructor's {@code shrink} and {@code physics} ({@code physics} is the opposite of
     * {@code noClip}). A particle needs a client level, so this pure step is where a unit test pins the mapping.
     */
    public record Spec(double x,double y,double z,float size,int type,boolean shrink,boolean physics,float gravity){}
    /** The constructor arguments of {@code wispFX2}: {@code noClip} (the original {@code clip} argument) turns the physics off. */
    public static Spec spec(double x,double y,double z,float size,int type,boolean shrink,boolean noClip,float gravity){
        return new Spec(x,y,z,size,type,shrink,!noClip,gravity);
    }
    /** The speed per tick that covers {@code from} to {@code to} in {@code lifetime} ticks (the velocity of {@code wispFX3}). */
    public static double velocityToward(double from,double to,int lifetime){return(to-from)/lifetime;}
    /**
     * Original {@code ClientProxy.wispFX3}: one wisp of a primal colour that flies from a point to another and arrives when it dies
     * (its velocity is the distance over its lifetime, as in the original constructor).
     */
    public static void spawnToward(ClientLevel level,double x,double y,double z,double tx,double ty,double tz,float size,int type,boolean shrink,float gravity){
        var mote=new PrimalWispParticle(level,x,y,z,size,type,null,true,gravity,shrink);
        if(mote.lifetime>0){mote.xd=velocityToward(mote.x,tx,mote.lifetime);mote.yd=velocityToward(mote.y,ty,mote.lifetime);mote.zd=velocityToward(mote.z,tz,mote.lifetime);}
        add(mote);
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
    @Override public float getQuadSize(float partial){return initialSize*sizeFactor(shrink,age,lifetime);}
    /**
     * The share of its full size a typed wisp has at an age (original FXWisp render: {@code shrink} fades it out over the whole life; otherwise it
     * grows to full size at half the life and shrinks again, {@code age / (lifetime / 2)} folded at one).
     */
    public static float sizeFactor(boolean shrink,int age,int lifetime){
        if(shrink)return(lifetime-age)/(float)lifetime;
        float scale=age/(float)Math.max(1,lifetime/2);return scale>1?2-scale:scale;
    }
    @Override protected float getU0(){return 0;}
    @Override protected float getU1(){return .125F;}
    @Override protected float getV0(){return .875F;}
    @Override protected float getV1(){return 1;}
    @Override protected int getLightColor(float partial){return 240;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
    @Override public void render(VertexConsumer ignored,Camera camera,float partial){VisNetRender.drawParticle(VisNetRender.PARTICLES,destination,MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial));}
}

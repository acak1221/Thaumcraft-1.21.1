package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityWisp;

/** Original FXWisp mote, FXBurst's node-strip and firebat FXGeneric's nine atlas cells. */
public final class MonsterSpriteParticle extends SingleQuadParticle {
    private enum Kind { MOTE,BURST,EXPLOSIVE }
    private static ShaderInstance litShader;
    private final Kind kind;
    private final float originalSize;
    private MonsterSpriteParticle(ClientLevel level,double x,double y,double z,Kind kind,float size,int rgb,int duration) {
        super(level,x,y,z);this.kind=kind;xd=yd=zd=0;xo=x;yo=y;zo=z;
        // The original FXBurst:32 keeps the particle scale of the caller and draws it with that half size, and
        // the original FXWisp:44 keeps the same scale for the mote. The original never randomises the size.
        originalSize=kind==Kind.BURST?size:kind==Kind.MOTE?.5F*size:.1F*size;
        quadSize=originalSize;
        lifetime=kind==Kind.BURST?31:kind==Kind.EXPLOSIVE?duration:(int)(36/(random.nextDouble()*.3+.7));
        // Original FXBurst:45 uses glColor4f(1,1,1,0.75): the burst is drawn at three quarters opacity.
        alpha=kind==Kind.MOTE?.5F:kind==Kind.EXPLOSIVE?.8F:.75F;
        setColor((rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F);
        if(kind==Kind.MOTE && rCol==0)rCol=1; // Original FXWisp's red==0 constructor quirk.
        gravity=kind==Kind.MOTE?.02F:0;hasPhysics=kind==Kind.MOTE;setSize(kind==Kind.BURST?.01F:.1F,kind==Kind.BURST?.01F:.1F);
    }
    public static void setLitShader(ShaderInstance shader) { litShader=java.util.Objects.requireNonNull(shader); }
    public static ShaderInstance litShader() { return java.util.Objects.requireNonNull(litShader,"Monster lit particle shader not registered"); }
    /** Exact FXBurst constructor entry, shared by the original Wisp/ShockOrb clients. */
    public static void burst(ClientLevel level,double x,double y,double z,float size) { Minecraft.getInstance().particleEngine.add(new MonsterSpriteParticle(level,x,y,z,Kind.BURST,size,0xffffff,0)); }
    public static void fireBat(EntityFireBat bat) {
        if(!(bat.level() instanceof ClientLevel level))return;
        if(bat.getIsExplosive()) {
            var random=bat.getRandom();
            double x=bat.xo+(random.nextFloat()-random.nextFloat())*.1F;
            double y=bat.yo+bat.getBbHeight()/2+(random.nextFloat()-random.nextFloat())*.1F;
            double z=bat.zo+(random.nextFloat()-random.nextFloat())*.1F;
            int age=7+random.nextInt(5);float scale=1+random.nextFloat()*.5F;
            Minecraft.getInstance().particleEngine.add(new MonsterSpriteParticle(level,x,y,z,Kind.EXPLOSIVE,scale,0xffffff,age));
        }
        if(!bat.getIsVampire()) for(var type:new net.minecraft.core.particles.SimpleParticleType[]{ParticleTypes.SMOKE,ParticleTypes.FLAME})
            level.addParticle(type,bat.xo+spread(level,.2F),bat.yo+bat.getBbHeight()/2+spread(level,.2F),bat.zo+spread(level,.2F),0,0,0);
    }
    public static final EntityWisp.ClientEffects WISP_EFFECTS=new EntityWisp.ClientEffects() {
        @Override public void tick(EntityWisp wisp) {
            if(!(wisp.level() instanceof ClientLevel level) || !level.random.nextBoolean())return;
            var aspect=Aspect.getAspect(wisp.getAspectTag());if(aspect==null)return;
            double x=wisp.getX()+spread(level,.7F),y=wisp.getY()+.45F+spread(level,.7F),z=wisp.getZ()+spread(level,.7F);
            var viewer=Minecraft.getInstance().getCameraEntity();int range=Minecraft.useFancyGraphics()?50:25;
            if(viewer!=null && viewer.distanceToSqr(x,y,z)<=range*range)Minecraft.getInstance().particleEngine.add(new MonsterSpriteParticle(level,x,y,z,Kind.MOTE,.1F,aspect.getColor(),0));
        }
        @Override public void burst(EntityWisp wisp) { if(wisp.level() instanceof ClientLevel level)MonsterSpriteParticle.burst(level,wisp.getX(),wisp.getY()+.45F,wisp.getZ(),1); }
    };
    private static float spread(ClientLevel level,float scale) { return(level.random.nextFloat()-level.random.nextFloat())*scale; }
    @Override public void tick() {
        xo=x;yo=y;zo=z;if(age++>=lifetime){remove();return;}
        if(kind!=Kind.BURST) {
            yd-=.04*gravity;
            // The original FXWisp:194-196 pushes the mote out of the solid blocks it ends up inside.
            ParticleDeflection.pushOutOfBlocks(this,level,x,y,z);
            move(xd,yd,zd);xd*=.98F;yd*=.98F;zd*=.98F;if(onGround){xd*=.7F;zd*=.7F;}
        }
    }
    @Override public float getQuadSize(float partial) {
        if(kind!=Kind.MOTE)return originalSize;
        float scale=age/(float)(lifetime/2);return originalSize*(scale>1?2-scale:scale);
    }
    @Override protected float getU0() { int frame=151+(int)Math.min(9F*age/lifetime,8);return frame%16/16F; }
    @Override protected float getU1() { return getU0()+.0624375F; }
    @Override protected float getV0() { int frame=151+(int)Math.min(9F*age/lifetime,8);return frame/16/16F; }
    @Override protected float getV1() { return getV0()+.0624375F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused,Camera camera,float partial) {
        if(kind==Kind.EXPLOSIVE) {
            float oldAlpha=alpha;if(age<=1 || age>=lifetime-1)alpha=oldAlpha/2;
            try { VisNetRender.drawParticle(VisNetRender.PARTICLES,java.util.Objects.requireNonNull(litShader,"Monster lit particle shader not registered"),out -> super.render(out,camera,partial)); }
            finally { alpha=oldAlpha; }
            return;
        }
        PoseStack pose=new PoseStack();var eye=camera.getPosition();
        pose.translate(Mth.lerp(partial,xo,x)-eye.x,Mth.lerp(partial,yo,y)-eye.y,Mth.lerp(partial,zo,z)-eye.z);pose.mulPose(camera.rotation());
        int rgb=(int)(rCol*255)<<16|(int)(gCol*255)<<8|(int)(bCol*255);
        float u0=kind==Kind.BURST?(age%32)/32F:0,u1=kind==Kind.BURST?u0+1/32F:.125F;
        float v0=kind==Kind.BURST?.96875F:.875F;
        if(kind==Kind.BURST)VisNetRender.draw(VisNetRender.NODES,GL11.GL_ONE,false,GameRenderer.getPositionTexColorShader(),out -> VisNetRender.quad(out,pose,getQuadSize(partial),alpha,rgb,u0,u1,v0,1));
        else VisNetRender.draw(VisNetRender.PARTICLES,GL11.GL_ONE,false,out -> VisNetRender.quad(out,pose,getQuadSize(partial),alpha,rgb,u0,u1,v0,1));
    }
}

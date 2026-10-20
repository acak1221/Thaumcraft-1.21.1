package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import thaumcraft.client.renderers.VisNetRender;

/** Original FXSpark, particles2 first three8x8 rows,5..9ticks, optional horizontal flip and no movement. */
public final class ShockSparkParticle extends SingleQuadParticle {
    private final int row;
    private final boolean flip;
    public ShockSparkParticle(ClientLevel level,double x,double y,double z,float size) {
        super(level,x,y,z); xd=yd=zd=0;gravity=0;hasPhysics=false;
        quadSize=size;lifetime=5+level.random.nextInt(5);row=level.random.nextInt(3);flip=level.random.nextBoolean();
        alpha=.8F;setSize(.01F,.01F);xo=x;yo=y;zo=z;
    }
    @Override public void tick() { xo=x;yo=y;zo=z;if(age++>=lifetime)remove(); }
    private float u() { return(int)(age/(float)lifetime*7F)/8F; }
    @Override protected float getU0() { return u()+(flip?.125F:0); }
    @Override protected float getU1() { return u()+(flip?0:.125F); }
    @Override protected float getV0() { return row/8F; }
    @Override protected float getV1() { return getV0()+.125F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer ignored,Camera camera,float partial) {
        VisNetRender.drawParticle(VisNetRender.texture("misc/particles2"),MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial));
    }
}

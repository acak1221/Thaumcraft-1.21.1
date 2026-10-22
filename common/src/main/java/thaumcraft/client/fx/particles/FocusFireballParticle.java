package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import thaumcraft.client.renderers.VisNetRender;
import thaumcraft.common.entities.projectile.EntityExplosiveOrb;

/** Original ExplosiveOrb's stationary FXGeneric trail: cells151..159,7–11
 * ticks, scale2..3, alpha.8 with half-alpha at first/last animation frames. */
public final class FocusFireballParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    private FocusFireballParticle(ClientLevel level, double x, double y, double z, int lifetime, float scale) {
        super(level,x,y,z); this.lifetime=lifetime; quadSize=.1F*scale; alpha=.8F;
        hasPhysics=false; gravity=0; xd=yd=zd=0; xo=x;yo=y;zo=z;
    }
    public static void trail(EntityExplosiveOrb orb) {
        if (!(orb.level() instanceof ClientLevel level) || Minecraft.getInstance().level != level) return;
        var random=orb.getRandom();
        double x=orb.xo+(random.nextFloat()-random.nextFloat())*.3F;
        double y=orb.yo+(random.nextFloat()-random.nextFloat())*.3F;
        double z=orb.zo+(random.nextFloat()-random.nextFloat())*.3F;
        Minecraft.getInstance().particleEngine.add(new FocusFireballParticle(level,x,y,z,7+random.nextInt(5),2+random.nextFloat()));
    }
    @Override public void tick() { xo=x;yo=y;zo=z;if(age++>=lifetime)remove(); }
    @Override protected float getU0() { return (151+Math.min(8,9*age/lifetime))%16/16F; }
    @Override protected float getU1() { return getU0()+.0624375F; }
    @Override protected float getV0() { return (151+Math.min(8,9*age/lifetime))/16/16F; }
    @Override protected float getV1() { return getV0()+.0624375F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        float original=alpha;if(age<=1 || age>=lifetime-1)alpha=original/2;
        try { VisNetRender.drawParticle(VisNetRender.PARTICLES,MonsterSpriteParticle.litShader(),out->super.render(out,camera,partial)); }
        finally { alpha=original; }
    }
}

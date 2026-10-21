package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;

/** Original FXWispEG body haze: thirteen frames, downward drift, horizontal source following, age/distance fade and particle setting count. */
public final class WarpGuardianParticle extends SingleQuadParticle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    /**
     * Original FXWispEG:25 and :53 - the particle scale is the fixed 0.4f and the half size of the quad is
     * {@code 0.5f * 0.4f}, so the quad size of the 1.21.1 particle is the scale itself.
     */
    public static final float QUAD_SIZE = .4F;
    private static final Tesselator BUFFER = new Tesselator(256);
    private final EntityEldritchGuardian target;
    private WarpGuardianParticle(ClientLevel level, EntityEldritchGuardian target, double x, double y, double z) {
        super(level, x, y, z); this.target = target;
        xd = random.nextGaussian() * .03; yd = -.05; zd = random.nextGaussian() * .03;
        // The original FXWispEG:25 scales the particle by 0.4f and draws it with the half size 0.5f * 0.4f.
        // The original never randomises that size.
        lifetime = (int)(40 / (random.nextDouble() * .3 + .7)); quadSize = QUAD_SIZE;
        setColor(random.nextFloat() * .05F, random.nextFloat() * .05F, random.nextFloat() * .05F);
        hasPhysics = true; setSize(.01F, .01F); xo = x; yo = y; zo = z;
    }
    public static void ambient(EntityEldritchGuardian guardian, double x, double y, double z) {
        if (!(guardian.level() instanceof ClientLevel level)) return;
        var client = Minecraft.getInstance(); var viewer = client.getCameraEntity();
        int range = Minecraft.useFancyGraphics() ? 50 : 25;
        if (viewer == null || viewer.distanceToSqr(x, y, z) > range * range) return;
        int count = client.options.particles().get() == ParticleStatus.MINIMAL ? 0 : client.options.particles().get() == ParticleStatus.DECREASED ? 1 : 2;
        for (int i = 0; i < count; i++) client.particleEngine.add(new WarpGuardianParticle(level, guardian, x, y, z));
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (!onGround) { x += target.getDeltaMovement().x; z += target.getDeltaMovement().z; }
        // The original FXWispEG:97 pushes the mote out of the solid blocks it ends up inside.
        var motion=ParticleDeflection.pushOutOfBlocks(level,random,x,y,z,xd,yd,zd);
        if(motion!=null){xd=motion.x();yd=motion.y();zd=motion.z();}
        move(xd, yd, zd); xd *= .98F; yd *= .98F; zd *= .98F;
        if (onGround) { xd *= .8500000190734863; zd *= .8500000190734863; }
        if (age++ >= lifetime) remove();
    }
    @Override protected int getLightColor(float partial) { return 240; }
    @Override protected float getU0() { return (age % 13) / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return .1875F; }
    @Override protected float getV1() { return .2499375F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        double distance = camera.getPosition().distanceToSqr(x, y, z);
        alpha = .2F * (1 - age / (float)lifetime) * (float)(1 - Math.min(1024, distance) / 1024);
        var shader = RenderSystem.getShader(); int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader); RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            super.render(buffer, camera, partial); BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> shader); RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend(); RenderSystem.depthMask(true);
        }
    }
}

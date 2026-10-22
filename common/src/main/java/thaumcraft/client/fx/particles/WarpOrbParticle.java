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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;

/** TC4 orb's thirty shrinking type5 wisps, original atlas cell, target-based velocity, .02 gravity and smoke blend. */
public final class WarpOrbParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return 1; }
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Tesselator BUFFER = new Tesselator(256);
    private WarpOrbParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x + dx, y + dy, z + dz);
        lifetime = (int)(36 / (random.nextDouble() * .3 + .7));
        quadSize = .3F * (random.nextFloat() * .5F + .5F); alpha = .5F;
        setColor(level.random.nextFloat() * .1F, level.random.nextFloat() * .1F, level.random.nextFloat() * .1F);
        gravity = .02F; hasPhysics = true; setSize(.1F, .1F);
        xd = 7 * dx / lifetime; yd = 7 * dy / lifetime; zd = 7 * dz / lifetime;
        xo = this.x; yo = this.y; zo = this.z;
    }
    public static void explode(EntityEldritchOrb orb) {
        if (!(orb.level() instanceof ClientLevel level)) return;
        var client = Minecraft.getInstance(); var viewer = client.getCameraEntity();
        int range = Minecraft.useFancyGraphics() ? 50 : 25;
        for (int i = 0; i < 30; i++) {
            double dx = (level.random.nextFloat() - level.random.nextFloat()) * .3F;
            double dy = (level.random.nextFloat() - level.random.nextFloat()) * .3F;
            double dz = (level.random.nextFloat() - level.random.nextFloat()) * .3F;
            if (viewer != null && viewer.distanceToSqr(orb.getX() + dx, orb.getY() + dy, orb.getZ() + dz) <= range * range)
                client.particleEngine.add(new WarpOrbParticle(level, orb.getX(), orb.getY(), orb.getZ(), dx, dy, dz));
        }
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        yd -= .04 * gravity; move(xd, yd, zd);
        xd *= .98F; yd *= .98F; zd *= .98F;
        if (onGround) { xd *= .7F; zd *= .7F; }
    }
    @Override public float getQuadSize(float partial) { return quadSize * (lifetime - age) / lifetime; }
    @Override protected int getLightColor(float partial) { return 240; }
    @Override protected float getU0() { return 0; }
    @Override protected float getU1() { return .125F; }
    @Override protected float getV0() { return .875F; }
    @Override protected float getV1() { return 1; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        var shader = RenderSystem.getShader(); int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader); RenderSystem.setShaderTexture(0, TEXTURE);
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

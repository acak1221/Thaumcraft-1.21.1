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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** ClientProxy.blockSparkle/FXGeneric: green 0x40ff40, original112..120 atlas frames, delay/size/drag/fullbright. */
public final class GrowthLampParticle extends SingleQuadParticle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Tesselator BUFFER = new Tesselator(4096);
    private final int delay;
    public GrowthLampParticle(ClientLevel level, BlockPos pos) {
        super(level, pos.getX() - .1 + level.random.nextFloat() * 1.2, pos.getY() - .1 + level.random.nextFloat() * 1.2, pos.getZ() - .1 + level.random.nextFloat() * 1.2);
        hasPhysics = false; xd = zd = 0; yd = level.random.nextFloat() * .02;
        setColor(Mth.clamp(64F/255 - .2F + level.random.nextFloat() * .4F, 0, 1), Mth.clamp(.8F + level.random.nextFloat() * .4F, 0, 1), Mth.clamp(64F/255 - .2F + level.random.nextFloat() * .4F, 0, 1));
        alpha = .9F;
        int active = 5 + level.random.nextInt(8); delay = level.random.nextInt(10); lifetime = active + delay;
        quadSize = .1F * (.7F + level.random.nextFloat() * .4F); setSize(.01F, .01F);
    }
    private boolean valid() { return Minecraft.getInstance().level == level && level.hasChunkAt(BlockPos.containing(x, y, z)); }
    @Override public void tick() {
        xo=x; yo=y; zo=z;
        if (!valid() || age++ >= lifetime) { remove(); return; }
        if (!level.hasChunkAt(BlockPos.containing(x + xd, y + yd, z + zd))) { remove(); return; }
        move(xd, yd, zd); xd *= .98; yd *= .98; zd *= .98;
    }
    private int frame() { return 112 + Math.min(8, age * 9 / Math.max(1, lifetime)); }
    @Override protected float getU0() { return frame() % 16 / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return frame() / 16 / 16F; }
    @Override protected float getV1() { return getV0() + .0624375F; }
    @Override protected int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        if (!valid() || !isAlive() || age < delay) return;
        var shader = RenderSystem.getShader(); int texture = RenderSystem.getShaderTexture(0); float savedAlpha = alpha;
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader); RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull(); RenderSystem.depthMask(false);
            if (age <= 1 || age >= lifetime - 1) alpha /= 2;
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE); super.render(buffer, camera, partialTick); BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            alpha = savedAlpha; RenderSystem.setShader(() -> shader); RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend(); RenderSystem.enableCull(); RenderSystem.depthMask(true);
        }
    }
}

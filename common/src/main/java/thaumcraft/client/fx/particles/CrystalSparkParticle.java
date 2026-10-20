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
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.crystals.CrystalClusterGeometry;

/** Original FXSpark: stationary5..9-tick,8x8 particles2 atlas, additive layer2 and alpha.8. */
public final class CrystalSparkParticle extends SingleQuadParticle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles2.png");
    private static final Tesselator BUFFER = new Tesselator(256);
    private final int first;
    private final boolean flip;
    private CrystalSparkParticle(ClientLevel level, double x, double y, double z, float size) {
        super(level, x, y, z); xd = yd = zd = 0; gravity = 0; quadSize = size; alpha = .8F; setSize(.01F, .01F);
        lifetime = 5 + level.random.nextInt(5); first = level.random.nextInt(3) * 8; flip = level.random.nextBoolean();
    }
    public static void sparkle(Level world, BlockPos position, int metadata, RandomSource random) {
        if (!(world instanceof ClientLevel level) || metadata < 0 || metadata > 6 || random.nextInt(17) != 0) return;
        var particle = new CrystalSparkParticle(level, position.getX() + .3 + level.random.nextFloat() * .4F,
                position.getY() + .3 + level.random.nextFloat() * .4F, position.getZ() + .3 + level.random.nextFloat() * .4F, .2F + random.nextFloat() * .1F);
        int color = CrystalClusterGeometry.color(metadata == 6 ? random.nextInt(6) : metadata);
        particle.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
        Minecraft.getInstance().particleEngine.add(particle);
    }
    @Override public void tick() { xo = x; yo = y; zo = z; if (age++ >= lifetime) remove(); }
    private int cell() { return first + (int) (age / (float) lifetime * 7); }
    @Override protected float getU0() { return (cell() % 8 + (flip ? 1 : 0)) / 8F; }
    @Override protected float getU1() { return (cell() % 8 + (flip ? 0 : 1)) / 8F; }
    @Override protected float getV0() { return cell() / 8 / 8F; }
    @Override protected float getV1() { return getV0() + .125F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        var shader = RenderSystem.getShader(); int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader); RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE); RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE); super.render(buffer, camera, partialTick); BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally { RenderSystem.setShader(() -> shader); RenderSystem.setShaderTexture(0, texture); RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend(); RenderSystem.depthMask(true); }
    }
}

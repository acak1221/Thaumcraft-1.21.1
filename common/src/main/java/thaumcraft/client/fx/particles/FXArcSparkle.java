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
import net.minecraft.util.Mth;

/**
 * Original TC4 {@code FXSparkle} (oracle {@code thaumcraft/client/fx/particles/FXSparkle.java:120-135}),
 * the sparkle the eldritch golem's lightning arc strikes at its target.
 * <p>
 * {@code ClientProxy.arcLightning} creates it as {@code FXSparkle(world, tx, ty, tz, tx, ty, tz,
 * 3.0f, 6, 2)}: a size factor of three, the colour type six and a multiplier of two, so the
 * particle lives {@code 3 * 2} ticks, its half extent is {@code 0.1 * 0.2 * 3} and it shrinks over
 * its life. Its colour is overwritten with the arc colour and its gravity is zero.
 */
public final class FXArcSparkle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    /** Original FXSparkle:123: {@code 0.1 * particleSize} with the original particle size of 0.2. */
    private static final float EXTENT = .1F * .2F * 3F;
    /** Original FXSparkle:128: the half opacity of the sparkle. */
    private static final float ALPHA = .75F;
    /** Original FXSparkle:126-128. */
    private static final int MULTIPLIER = 2;
    // A separate small native buffer: ParticleEngine already has its CUSTOM
    // buffer open while invoking render, so do not nest its singleton builder.
    private static final Tesselator BUFFER = new Tesselator(256);

    private FXArcSparkle(ClientLevel level, double x, double y, double z, float red, float green, float blue) {
        super(level, x, y, z);
        xd = yd = zd = 0;
        gravity = 0;
        hasPhysics = false;
        setSize(.01F, .01F);
        quadSize = EXTENT;
        lifetime = 3 * MULTIPLIER;
        alpha = ALPHA;
        setColor(red, green, blue);
        xo = x;
        yo = y;
        zo = z;
    }

    public static void emit(ClientLevel level, double x, double y, double z, float red, float green, float blue) {
        Minecraft.getInstance().particleEngine.add(new FXArcSparkle(level, x, y, z, red, green, blue));
    }

    /** Original FXSparkle:130-134: the sparkle shrinks over its lifetime. */
    @Override public float getQuadSize(float partialTick) {
        return quadSize * Mth.clamp((lifetime - age + 1) / (float) lifetime, 0, 1);
    }

    @Override protected int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }
    /** Original FXSparkle:122-125: the cell walks the first four cells of the fourth row. */
    @Override protected float getU0() { return ((16 + age / MULTIPLIER) % 4) / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return .25F; }
    @Override protected float getV1() { return .3124375F; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader);
            RenderSystem.setShaderTexture(0, ResourceParticle.TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            super.render(buffer, camera, partialTick);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }
}

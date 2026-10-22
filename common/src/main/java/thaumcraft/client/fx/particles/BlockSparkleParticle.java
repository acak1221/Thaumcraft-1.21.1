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
import net.minecraft.util.RandomSource;
import thaumcraft.common.lib.network.BlockSparkleFxPayload;

/**
 * The native {@code FXGeneric} of the original block sparkle.
 *
 * <p>Oracle {@code thaumcraft/client/ClientProxy.java:632-644} ({@code blockSparkle}) and
 * {@code thaumcraft/client/fx/particles/FXGeneric.java:1-59}: the burst is drawn at the block with the
 * position {@code x - 0.1 + random * 1.2}, the velocity {@code random * 0.02} upwards, the alpha
 * {@code 0.9f}, the atlas frames {@code 112..120} of {@code textures/misc/particles.png}, the life
 * {@code 5 + random(8)} plus the delay {@code random(10)} and the scale {@code 0.7 + random * 0.4} of
 * the quarter of the particle.</p>
 *
 * <p>The colour rule of the original: the three channels are the bytes of the colour, each jittered by
 * {@code -0.2 + random * 0.4}, and the special colour {@code -9999} is not a colour at all: it is
 * replaced by a random grey {@code 0.33 + random * 0.67} per channel and per particle
 * (oracle {@code ClientProxy.java:637-643}).</p>
 */
public class BlockSparkleParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Tesselator BUFFER = new Tesselator(4096);
    /** The original FXGeneric start particle of the block sparkle. */
    public static final int FIRST_FRAME = 112;
    /** The original FXGeneric number of particles of the block sparkle. */
    public static final int FRAME_COUNT = 9;
    /** The original alpha of the block sparkle. */
    public static final float ALPHA = .9F;
    /** The original quarter of the particle. */
    public static final float QUAD = .1F;
    /** The original low end of the random grey. */
    public static final float GREY_LOW = .33F;
    /** The original span of the random grey. */
    public static final float GREY_SPAN = .67F;
    /** The original jitter of every channel. */
    public static final float JITTER = .4F;
    /** The original offset of every channel. */
    public static final float JITTER_OFFSET = -.2F;

    private final int delay;

    public BlockSparkleParticle(ClientLevel level, BlockPos pos, int rgb) {
        // The original draws the base colour of the burst before the position of the particle.
        this(level, pos, baseColour(rgb, level.random));
    }

    private BlockSparkleParticle(ClientLevel level, BlockPos pos, float[] base) {
        super(level, pos.getX() - .1F + level.random.nextFloat() * 1.2F,
                pos.getY() - .1F + level.random.nextFloat() * 1.2F,
                pos.getZ() - .1F + level.random.nextFloat() * 1.2F);
        hasPhysics = false;
        xd = 0; yd = level.random.nextFloat() * .02F; zd = 0;
        float[] colour = jitter(base, level.random);
        setColor(Mth.clamp(colour[0], 0, 1), Mth.clamp(colour[1], 0, 1), Mth.clamp(colour[2], 0, 1));
        alpha = ALPHA;
        int active = 5 + level.random.nextInt(8);
        delay = level.random.nextInt(10);
        lifetime = active + delay;
        quadSize = QUAD * (.7F + level.random.nextFloat() * .4F);
        setSize(.01F, .01F);
    }

    /**
     * The base colour of the original {@code blockSparkle} (oracle {@code ClientProxy.java:633-643}).
     *
     * <p>For an ordinary colour the three channels are the bytes of the colour divided by 255 and the
     * random is not touched. For the special colour {@code -9999} every channel is a fresh random
     * grey {@code 0.33 + random * 0.67}.</p>
     */
    public static float[] baseColour(int rgb, RandomSource random) {
        if (rgb == BlockSparkleFxPayload.RANDOM_GREY) {
            return new float[]{GREY_LOW + random.nextFloat() * GREY_SPAN,
                    GREY_LOW + random.nextFloat() * GREY_SPAN,
                    GREY_LOW + random.nextFloat() * GREY_SPAN};
        }
        return new float[]{(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F};
    }

    /** The original per particle jitter of the three channels: {@code -0.2 + random * 0.4}. */
    public static float[] jitter(float[] base, RandomSource random) {
        float[] out = new float[3];
        for (int i = 0; i < 3; i++) out[i] = base[i] + JITTER_OFFSET + random.nextFloat() * JITTER;
        return out;
    }

    /** The whole colour of one particle: the base colour and its jitter, in the original order. */
    public static float[] colour(int rgb, RandomSource random) {
        return jitter(baseColour(rgb, random), random);
    }

    private boolean valid() { return Minecraft.getInstance().level == level && level.hasChunkAt(BlockPos.containing(x, y, z)); }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (!valid() || age++ >= lifetime) { remove(); return; }
        if (!level.hasChunkAt(BlockPos.containing(x + xd, y + yd, z + zd))) { remove(); return; }
        move(xd, yd, zd);
        xd *= .98; yd *= .98; zd *= .98;
    }

    /** The original FXGeneric frame: the start frame plus the progress over the nine frames. */
    private int frame() { return FIRST_FRAME + Math.min(FRAME_COUNT - 1, age * FRAME_COUNT / Math.max(1, lifetime)); }

    @Override protected float getU0() { return frame() % 16 / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return frame() / 16 / 16F; }
    @Override protected float getV1() { return getV0() + .0624375F; }
    @Override protected int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        if (!valid() || !isAlive() || age < delay) return;
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        float savedAlpha = alpha;
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            // Oracle ClientProxy:1102-1110: FXGeneric uses the default layer zero.
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            // The original FXGeneric halves the alpha of the first and of the last frame.
            if (age <= 1 || age >= lifetime - 1) alpha /= 2;
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            super.render(buffer, camera, partialTick);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            alpha = savedAlpha;
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }
}

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
import net.minecraft.world.phys.Vec3;

/**
 * The original essence trail of the public essentia source API.
 *
 * <p>Oracle {@code thaumcraft/client/fx/particles/FXEssentiaTrail.java:24-212}: the mote starts at the
 * source, flies to the consumer and dies when it arrives. The numbers are the original ones:</p>
 *
 * <ul>
 *   <li>the size of the mote is {@code 0.1 * (sin(count / 2) * 0.1 + 1) * scale} (oracle {@code :37}),</li>
 *   <li>the lifetime is {@code base / 2 + rand(base)} with {@code base = (int)(distance * 30)} and a
 *       minimum of one (oracle {@code :42-49}),</li>
 *   <li>the three speeds are {@code sin(count / 4) * 0.015 + gauss * 0.002},
 *       {@code 0.1 + sin(count / 3) * 0.01} and {@code sin(count / 2) * 0.015 + gauss * 0.002}
 *       (oracle {@code :50-52}),</li>
 *   <li>the colour is the colour of the packet, darkened by twenty per cent and jittered by the same
 *       twenty per cent (oracle {@code :53-59}),</li>
 *   <li>the gravity of the mote is {@code 0.01 * 0.2} per tick (oracle {@code :110}),</li>
 *   <li>the mote is pushed out of the solid blocks it ends up inside (oracle {@code :111-113}),</li>
 *   <li>the three speeds are damped by {@code 0.985} and clamped to {@code 0.05} (oracle
 *       {@code :115-120}),</li>
 *   <li>the homing adds {@code 0.01 / min(1, distance)} and the mote shrinks by two per cent inside two
 *       blocks (oracle {@code :121-135}).</li>
 * </ul>
 *
 * <p>The atlas cell of the original is the fixed {@code (9, 1)} cell of the particle sheet (oracle
 * {@code :92-95}), because the original computed a per age cell at {@code :83} and never used it.</p>
 */
public final class EssentiaTrailParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    @Override public int tc4Layer() { return 1; }
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Tesselator BUFFER = new Tesselator(4096);
    private final BlockPos source;
    private final Vec3 destination;
    private final int phase;

    public EssentiaTrailParticle(ClientLevel level, BlockPos source, Vec3 from, Vec3 destination,
                                 int rgb, int phase, float scale) {
        super(level, from.x, from.y, from.z);
        this.source = source.immutable();
        this.destination = destination;
        this.phase = phase;
        hasPhysics = false;
        xd = yd = zd = 0;
        setSize(.01F, .01F);
        setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F);
        quadSize = .1F * (Mth.sin(phase / 2F) * .1F + 1) * scale;
        alpha = .5F;
        int base = Math.max(1, (int) (from.distanceTo(destination) * 30));
        // The original FXEssentiaTrail:60 has no upper bound on the lifetime.
        lifetime = Math.max(1, base / 2 + random.nextInt(base));
        xd = Mth.sin(phase / 4F) * .015F + random.nextGaussian() * .002F;
        yd = .1F + Mth.sin(phase / 3F) * .01F;
        zd = Mth.sin(phase / 2F) * .015F + random.nextGaussian() * .002F;
        rCol *= .8F + random.nextFloat() * .2F;
        gCol *= .8F + random.nextFloat() * .2F;
        bCol *= .8F + random.nextFloat() * .2F;
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (!loaded(BlockPos.containing(x, y, z)) || age++ >= lifetime) {
            remove();
            return;
        }
        yd += .002F;
        // The original FXEssentiaTrail:115 pushes the mote out of the solid blocks it ends up inside.
        var motion = ParticleDeflection.pushOutOfBlocks(level, random, getBoundingBox(), x, y, z);
        if (motion != null) { xd = motion.x(); yd = motion.y(); zd = motion.z(); }
        if (!loaded(BlockPos.containing(x + xd, y + yd, z + zd))) { remove(); return; }
        move(xd, yd, zd);
        xd *= .985F; yd *= .985F; zd *= .985F;
        xd = Mth.clamp(xd, -.05F, .05F);
        yd = Mth.clamp(yd, -.05F, .05F);
        zd = Mth.clamp(zd, -.05F, .05F);
        Vec3 offset = destination.subtract(x, y, z);
        double distance = offset.length();
        if (distance < .01D) { remove(); return; }
        if (distance < 2.0D) quadSize *= .98F;
        if (quadSize < .02F) { remove(); return; }
        double acceleration = .01D / Math.min(1.0D, distance);
        xd += offset.x / distance * acceleration;
        yd += offset.y / distance * acceleration;
        zd += offset.z / distance * acceleration;
    }

    private boolean loaded(BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
    }

    @Override public float getQuadSize(float partialTick) {
        return quadSize * (Mth.sin((age + partialTick - phase) / 5F) * .25F + 1);
    }

    @Override protected int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }

    @Override protected float getU0() { return .5625F; }
    @Override protected float getU1() { return .625F; }
    @Override protected float getV0() { return .0625F; }
    @Override protected float getV1() { return .125F; }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        if (!isAlive() || !loaded(BlockPos.containing(x, y, z))) return;
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        float opacity = alpha;
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            super.render(buffer, camera, partialTick);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            alpha = opacity;
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
        }
    }
}

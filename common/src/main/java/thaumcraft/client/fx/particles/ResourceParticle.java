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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.tiles.TileNitor;

/**
 * Small native ParticleEngine effects, scoped to Nitor, Alumentum and shimmerleaf. TC4
 * FXWisp/FXSparkle supply the atlas cells, colours, scale, life, drag and gravity.
 * CUSTOM is a vanilla particle render pass, not a replacement particle engine.
 * It permits the original SRC_ALPHA/ONE glow and SRC_ALPHA/ONE_MINUS_SRC_ALPHA
 * smoke without editing the shared block atlas or registering vanilla stand-ins.
 * Native camera billboarding/shader/collision still require inspected client evidence.
 */
public final class ResourceParticle extends SingleQuadParticle {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    // A separate small native buffer: ParticleEngine already has its CUSTOM
    // buffer open while invoking render, so do not nest its singleton builder.
    private static final Tesselator BUFFER = new Tesselator(256);
    private final boolean sparkle;
    private final boolean smoke;
    private final int multiplier;
    private boolean shimmerleaf;

    private ResourceParticle(ClientLevel level, double x, double y, double z, float scale, int type,
                             boolean sparkle, int multiplier, float gravity, boolean physics) {
        super(level, x, y, z);
        this.sparkle = sparkle;
        this.smoke = !sparkle && type == 5;
        this.multiplier = multiplier;
        this.gravity = gravity;
        hasPhysics = physics;
        xd = yd = zd = 0;
        setSize(sparkle ? .01F : .1F, sparkle ? .01F : .1F);
        lifetime = sparkle ? 3 * multiplier : (int)(36 / (random.nextDouble() * .3 + .7));
        quadSize = (sparkle ? .1F : .5F) * (random.nextFloat() * .5F + .5F) * 2 * scale;
        alpha = sparkle ? 1 : .5F;
        if (sparkle) {
            // Source FXSparkle:56-109 has NO case 6. Both callers request 6,
            // leaving its constructor's (1,0,0), not FXWisp's white case 6.
            setColor(1, 0, 0);
        } else if (type == 4) setColor(.7F + random.nextFloat() * .3F, .2F, .2F);
        else if (type == 1) setColor(.5F + random.nextFloat() * .3F, .5F + random.nextFloat() * .3F, .2F);
        else if (type >= 0) setColor(random.nextFloat() * .1F, random.nextFloat() * .1F, random.nextFloat() * .1F);
    }

    private static ResourceParticle wisp(ClientLevel level, double x, double y, double z, float size, int type,
                                         float gravity, boolean physics) {
        return new ResourceParticle(level, x, y, z, size, type, false, 1, gravity, physics);
    }
    private static void add(ResourceParticle particle) {
        var camera = Minecraft.getInstance().getCameraEntity();
        int range = Minecraft.useFancyGraphics() ? 50 : 25;
        if (camera != null && camera.distanceToSqr(particle.x, particle.y, particle.z) <= range * range) {
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }
    private static int count(int base) {
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        return setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? base : base * 2;
    }
    private static double spread(RandomSource random, double amount) { return (random.nextFloat() - random.nextFloat()) * amount; }

    public static void shimmerleaf(Level world, BlockPos pos) {
        if (!(world instanceof ClientLevel level)) return;
        var random = level.random;
        float red = .3F + random.nextFloat() * .3F, green = .7F + random.nextFloat() * .3F, blue = .7F + random.nextFloat() * .3F;
        double x = pos.getX() + .5F + spread(random, .1F), y = pos.getY() + .5F + spread(random, .15F), z = pos.getZ() + .5F + spread(random, .1F);
        var particle = wisp(level, x, y, z, .2F, -1, 0, true);
        particle.shimmerleaf = true;
        particle.setColor(red, green, blue);
        add(particle);
    }

    public static final class NitorEffects implements TileNitor.ClientEffects {
        @Override public void tick(Level world, BlockPos pos) {
            if (!(world instanceof ClientLevel level)) return;
            RandomSource random = level.random;
            double x = pos.getX() + .5, y = pos.getY() + .5, z = pos.getZ() + .5;
            // TC4 TileNitor:18-27, including its particle-setting probabilities.
            if (random.nextInt(9 - count(2)) == 0) {
                var particle = wisp(level, x, y, z, .5F, 4, -.025F, true);
                particle.xd = (-.2 + random.nextFloat() * .4) / particle.lifetime;
                particle.zd = (-.2 + random.nextFloat() * .4) / particle.lifetime;
                add(particle);
            }
            if (random.nextInt(15 - count(4)) == 0) {
                var particle = wisp(level, x, y, z, .25F, 1, -.02F, true);
                particle.xd = (-.1 + random.nextFloat() * .2) / particle.lifetime;
                particle.zd = (-.1 + random.nextFloat() * .2) / particle.lifetime;
                add(particle);
            }
        }
        @Override public void sparkle(Level world, BlockPos pos, RandomSource random) {
            if (!(world instanceof ClientLevel level)) return;
            // TC4 BlockAiry:282-288, separate from the tile's two wisp streams.
            var particle = new ResourceParticle(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                    1, 6, true, 3, .05F, false);
            particle.xd = spread(random, 1.0 / 3) / particle.lifetime;
            particle.yd = spread(random, 1.0 / 3) / particle.lifetime;
            particle.zd = spread(random, 1.0 / 3) / particle.lifetime;
            add(particle);
        }
    }

    public static void alumentumTrail(EntityAlumentum projectile) {
        if (!(projectile.level() instanceof ClientLevel level)) return;
        RandomSource random = level.random;
        // TC4 EntityAlumentum:36-47: six dark wisps and up to three sparkles.
        for (int i = 0; i < 3; ++i) {
            add(wisp(level, projectile.getX() + spread(random, .3), projectile.getY() + spread(random, .3),
                    projectile.getZ() + spread(random, .3), .3F, 5, .02F, false));
            add(wisp(level, (projectile.getX() + projectile.xo) / 2 + spread(random, .3),
                    (projectile.getY() + projectile.yo) / 2 + spread(random, .3),
                    (projectile.getZ() + projectile.zo) / 2 + spread(random, .3), .3F, 5, .02F, false));
            if (random.nextInt(6) < count(2)) {
                add(new ResourceParticle(level, projectile.getX() + spread(random, .1), projectile.getY() + spread(random, .1),
                        projectile.getZ() + spread(random, .1), 1.5F, 6, true, 6, 0, false));
            }
        }
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        yd -= .04 * gravity;
        move(xd, yd, zd);
        double drag = sparkle ? .9080000019073486 : .98F;
        xd *= drag; yd *= drag; zd *= drag;
        if (onGround) { xd *= .7F; zd *= .7F; }
    }
    @Override public float getQuadSize(float partialTick) {
        if (shimmerleaf) {
            float scale = age / (float) (lifetime / 2);
            return quadSize * Math.max(0, scale > 1 ? 2 - scale : scale);
        }
        return quadSize * Mth.clamp((lifetime - age + (sparkle ? 1 : 0)) / (float)lifetime, 0, 1);
    }
    @Override protected int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }
    @Override protected float getU0() { return sparkle ? ((16 + age / multiplier) % 4) / 16F : 0; }
    @Override protected float getU1() { return getU0() + (sparkle ? .0624375F : .125F); }
    @Override protected float getV0() { return sparkle ? .25F : .875F; }
    @Override protected float getV1() { return sparkle ? .3124375F : 1; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    smoke ? GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA : GlStateManager.DestFactor.ONE);
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

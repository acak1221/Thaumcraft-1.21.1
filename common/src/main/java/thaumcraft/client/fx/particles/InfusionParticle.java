package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.lang.ref.WeakReference;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.mixin.client.MinecraftItemColorsAccess;

/**
 * Native ParticleEngine particles, not a second particle manager. Original TC4
 * FXEssentiaTrail, FXBoreSparkle/Particles, FXBlockRunes and blockSparkle provide
 * the atlas cells, homing, colour, fade and timing. Native no-collision movement
 * replaces their old push-out-of-solid-block routine; it never moves game items.
 */
public final class InfusionParticle extends SingleQuadParticle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final Tesselator BUFFER = new Tesselator(4096);
    private enum Kind { ESSENTIA, SPARKLE, FRAGMENT, RUNE, BURST }

    /** Cancellation and BE identity without keeping a chunk, world or tile alive. */
    public static final class Scope {
        private final WeakReference<TileInfusionMatrix> origin;
        private boolean live = true;

        public Scope(TileInfusionMatrix matrix) { origin = new WeakReference<>(matrix); }
        public void cancel() { live = false; }

        public boolean valid(ClientLevel level) {
            TileInfusionMatrix matrix = origin.get();
            return live && Minecraft.getInstance().level == level && matrix != null && !matrix.isRemoved()
                    && matrix.getLevel() == level && loaded(level, matrix.getBlockPos())
                    && level.getBlockEntity(matrix.getBlockPos()) == matrix;
        }
    }

    private final Kind kind;
    private final Scope scope;
    private final BlockPos source;
    private final Vec3 destination;
    private final int phase;
    private final TextureAtlasSprite sprite;
    private final float fragmentU, fragmentV;
    private int delay;
    private int rune;
    private float runeRotation, offsetX, offsetY;

    private InfusionParticle(ClientLevel level, Scope scope, BlockPos source, Vec3 from, Vec3 destination,
                             Kind kind, int color, int phase, float scale, TextureAtlasSprite sprite) {
        super(level, from.x, from.y, from.z);
        this.scope = scope;
        this.source = source.immutable();
        this.destination = destination;
        this.kind = kind;
        this.phase = phase;
        this.sprite = sprite;
        fragmentU = random.nextFloat() * .75F;
        fragmentV = random.nextFloat() * .75F;
        hasPhysics = false;
        xd = yd = zd = 0;
        setSize(.01F, .01F);
        setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
        if (kind == Kind.ESSENTIA) {
            quadSize = .1F * (Mth.sin(phase / 2F) * .1F + 1) * scale;
            alpha = .5F;
            int base = Math.max(1, (int)(from.distanceTo(destination) * 30));
            // The original FXEssentiaTrail:60 has no upper bound on the lifetime.
            lifetime = Math.max(1, base / 2 + random.nextInt(base));
            xd = Mth.sin(phase / 4F) * .015F + random.nextGaussian() * .002F;
            yd = .1F + Mth.sin(phase / 3F) * .01F;
            zd = Mth.sin(phase / 2F) * .015F + random.nextGaussian() * .002F;
            rCol *= .8F + random.nextFloat() * .2F;
            gCol *= .8F + random.nextFloat() * .2F;
            bCol *= .8F + random.nextFloat() * .2F;
        } else if (kind == Kind.RUNE) {
            // FXBlockRunes:24-48,51-92; the source actually renders row .375,
            // regardless of its runeIndex starting at 224. Preserve that UV.
            lifetime = 75; // 3 * the matrix caller's duration 25.
            rune = random.nextInt(16);
            runeRotation = random.nextInt(4) * 90;
            offsetX = random.nextFloat() * .2F;
            offsetY = -.3F + random.nextFloat() * .6F;
            quadSize = .15F * (float)(1 + random.nextGaussian() * .1);
            gravity = -.03F;
            alpha = 0;
        } else if (kind == Kind.BURST) {
            // ClientProxy.blockSparkle:632-644 / FXGeneric:44-75.
            delay = random.nextInt(10);
            lifetime = 5 + random.nextInt(8) + delay;
            quadSize = .1F * (.7F + random.nextFloat() * .4F);
            alpha = .9F;
            yd = random.nextFloat() * .02;
            setColor(Mth.clamp(rCol - .2F + random.nextFloat() * .4F, 0, 1),
                    Mth.clamp(gCol - .2F + random.nextFloat() * .4F, 0, 1),
                    Mth.clamp(bCol - .2F + random.nextFloat() * .4F, 0, 1));
        } else {
            int base = Math.max(1, (int)(from.distanceTo(destination) * 3));
            lifetime = Math.max(1, Math.min(100, base / 2 + random.nextInt(base)));
            quadSize = kind == Kind.FRAGMENT ? .1F * (random.nextFloat() * .3F + .4F) : .1F * (random.nextFloat() * .5F + .5F);
            alpha = kind == Kind.FRAGMENT ? .3F : 1;
            double speed = kind == Kind.FRAGMENT ? .03 : .01;
            xd = random.nextGaussian() * speed;
            yd = random.nextGaussian() * speed;
            zd = random.nextGaussian() * speed;
        }
    }

    public static InfusionParticle essentia(ClientLevel level, Scope scope, BlockPos source, BlockPos matrix, int rgb, int count, float scale) {
        // ClientTickEventsFML:257-266 offsets the matrix destination down ONE,
        // giving matrixY-.5, not the centre pedestal and not the cube's centre.
        return new InfusionParticle(level, scope, source, Vec3.atCenterOf(source), Vec3.atCenterOf(matrix).add(0, -1, 0),
                Kind.ESSENTIA, rgb, count, scale, null);
    }

    public static InfusionParticle sparkle(ClientLevel level, Scope scope, BlockPos source, Vec3 from, BlockPos matrix) {
        int red = (int)((.4F + level.random.nextFloat() * .2F) * 255);
        int blue = (int)((.6F + level.random.nextFloat() * .3F) * 255);
        return new InfusionParticle(level, scope, source, from, Vec3.atCenterOf(matrix).add(0, -1, 0),
                Kind.SPARKLE, red << 16 | 0x003300 | blue, 0, 1, null);
    }

    public static InfusionParticle fragment(ClientLevel level, Scope scope, BlockPos source, Vec3 from, BlockPos matrix, ItemStack actual) {
        ItemStack display = actual.copy();
        display.setCount(1);
        Minecraft client = Minecraft.getInstance();
        TextureAtlasSprite sprite = client.getItemRenderer().getModel(display, level, null, 0).getParticleIcon();
        // The native model and colour lookup see the actual item's components.
        // Neither a synthetic inventory snapshot nor an EntityItem is created.
        int color = ((MinecraftItemColorsAccess) client).thaumcraft$itemColors().getColor(display, 0);
        int red = (int)((color >> 16 & 255) * .6F), green = (int)((color >> 8 & 255) * .6F), blue = (int)((color & 255) * .6F);
        return new InfusionParticle(level, scope, source, from, Vec3.atCenterOf(matrix).add(0, -1, 0),
                Kind.FRAGMENT, red << 16 | green << 8 | blue, 0, 1, sprite);
    }

    public static InfusionParticle rune(ClientLevel level, Scope scope, BlockPos pedestal) {
        int red = (int)((.5F + level.random.nextFloat() * .2F) * 255);
        int blue = (int)((.7F + level.random.nextFloat() * .3F) * 255);
        return new InfusionParticle(level, scope, pedestal, Vec3.atCenterOf(pedestal), Vec3.ZERO,
                Kind.RUNE, red << 16 | 0x001900 | blue, 0, 1, null);
    }

    public static InfusionParticle burst(ClientLevel level, Scope scope, BlockPos pos, int rgb) {
        Vec3 from = new Vec3(pos.getX() - .1 + level.random.nextFloat() * 1.2,
                pos.getY() - .1 + level.random.nextFloat() * 1.2, pos.getZ() - .1 + level.random.nextFloat() * 1.2);
        return new InfusionParticle(level, scope, pos, from, Vec3.ZERO, Kind.BURST, rgb, 0, 1, null);
    }

    public static boolean loaded(ClientLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (!scope.valid(level) || !loaded(level, source) || !loaded(level, BlockPos.containing(x, y, z)) || age++ >= lifetime) {
            remove();
            return;
        }
        if (kind == Kind.RUNE) {
            float threshold = lifetime / 5F;
            int tick = age - 1;
            alpha = (tick <= threshold ? tick / threshold : (lifetime - tick) / (float)lifetime) / 2;
            yd -= .04 * gravity;
            moveLoaded();
            return;
        }
        if (kind == Kind.BURST) {
            moveLoaded();
            xd *= .98; yd *= .98; zd *= .98;
            return;
        }
        if (kind != Kind.ESSENTIA && BlockPos.containing(x, y, z).equals(BlockPos.containing(destination))) {
            remove();
            return;
        }
        if (kind == Kind.ESSENTIA) yd += .002;
        // The original FXEssentiaTrail:115 pushes the mote out of the solid blocks it ends up inside.
        if (kind == Kind.ESSENTIA) {
            var motion = ParticleDeflection.pushOutOfBlocks(level, random, getBoundingBox(), x, y, z);
            if (motion != null) { xd = motion.x(); yd = motion.y(); zd = motion.z(); }
        }
        moveLoaded();
        xd *= .985; yd *= .985; zd *= .985;
        Vec3 offset = destination.subtract(x, y, z);
        double distance = offset.length();
        // Native zero-distance guard replaces the original NaN-producing
        // divide-by-zero; no gameplay or source/demand state is touched.
        if (distance < .01 || !isAlive()) { remove(); return; }
        double acceleration;
        if (kind == Kind.ESSENTIA) {
            xd = Mth.clamp(xd, -.05, .05); yd = Mth.clamp(yd, -.05, .05); zd = Mth.clamp(zd, -.05, .05);
            if (distance < 2) quadSize *= .98F;
            if (quadSize < .02F) { remove(); return; }
            acceleration = .01 / Math.min(1, distance);
        } else {
            acceleration = distance < 4 ? .6 : .3;
            if (distance < 4) quadSize *= .9F;
        }
        xd += offset.x / distance * acceleration;
        yd += offset.y / distance * acceleration;
        zd += offset.z / distance * acceleration;
        if (kind != Kind.ESSENTIA) {
            xd = Mth.clamp(xd, -.35, .35); yd = Mth.clamp(yd, -.35, .35); zd = Mth.clamp(zd, -.35, .35);
        }
    }

    private void moveLoaded() {
        if (!loaded(level, BlockPos.containing(x + xd, y + yd, z + zd))) remove();
        else move(xd, yd, zd);
    }

    @Override public float getQuadSize(float partialTick) {
        if (kind == Kind.SPARKLE) return quadSize * (Mth.sin((age + partialTick) / 3) * .5F + 1);
        if (kind == Kind.ESSENTIA) return quadSize * (Mth.sin((age + partialTick - phase) / 5) * .25F + 1);
        return quadSize;
    }

    @Override protected int getLightColor(float partialTick) {
        return kind == Kind.FRAGMENT ? super.getLightColor(partialTick) : LightTexture.FULL_BRIGHT;
    }

    private int burstFrame() { return 112 + Math.min(8, age * 9 / Math.max(1, lifetime)); }
    @Override protected float getU0() {
        if (sprite != null) return Mth.lerp(fragmentU, sprite.getU0(), sprite.getU1());
        return switch (kind) {
            case ESSENTIA -> .5625F;
            case RUNE -> rune / 16F;
            case BURST -> burstFrame() % 16 / 16F;
            default -> age % 4 / 16F;
        };
    }
    @Override protected float getU1() {
        return getU0() + (sprite == null ? (kind == Kind.ESSENTIA ? .0625F : .0624375F) : (sprite.getU1() - sprite.getU0()) / 4);
    }
    @Override protected float getV0() {
        if (sprite != null) return Mth.lerp(fragmentV, sprite.getV0(), sprite.getV1());
        return switch (kind) {
            case ESSENTIA -> .0625F;
            case RUNE -> .375F;
            case BURST -> burstFrame() / 16 / 16F;
            default -> .25F;
        };
    }
    @Override protected float getV1() {
        return getV0() + (sprite == null ? (kind == Kind.ESSENTIA ? .0625F : .0624375F) : (sprite.getV1() - sprite.getV0()) / 4);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        if (!scope.valid(level) || !isAlive() || !loaded(level, source) || !loaded(level, BlockPos.containing(x, y, z))
                || kind == Kind.BURST && age < delay) return;
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        float opacity = alpha;
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, sprite == null ? TEXTURE : sprite.atlasLocation());
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    kind == Kind.FRAGMENT ? GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA : GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            if (kind == Kind.BURST && (age <= 1 || age >= lifetime - 1)) alpha /= 2;
            // A separate native builder avoids nesting ParticleEngine's open
            // CUSTOM builder. No framebuffer, replacement engine or atlas edit.
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            if (kind == Kind.RUNE) renderRune(buffer, camera, partialTick);
            else super.render(buffer, camera, partialTick);
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

    private void renderRune(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 eye = camera.getPosition();
        PoseStack pose = new PoseStack();
        pose.translate(Mth.lerp(partialTick, xo, x) - eye.x, Mth.lerp(partialTick, yo, y) - eye.y, Mth.lerp(partialTick, zo, z) - eye.z);
        pose.mulPose(Axis.YP.rotationDegrees(runeRotation));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(offsetX, offsetY, -.51);
        runeVertex(buffer, pose, -quadSize, quadSize, getU1(), getV1());
        runeVertex(buffer, pose, quadSize, quadSize, getU1(), getV0());
        runeVertex(buffer, pose, quadSize, -quadSize, getU0(), getV0());
        runeVertex(buffer, pose, -quadSize, -quadSize, getU0(), getV1());
    }

    private void runeVertex(VertexConsumer buffer, PoseStack pose, float x, float y, float u, float v) {
        buffer.addVertex(pose.last(), x, y, 0).setUv(u, v).setColor(rCol, gCol, bCol, alpha).setLight(LightTexture.FULL_BRIGHT);
    }
}

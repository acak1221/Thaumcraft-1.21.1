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
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The original rune particle of the TC4 1.7.10 client.
 *
 * <p>Oracle {@code thaumcraft/client/fx/particles/FXBlockRunes.java:17-98}. The particle is a single
 * quad of the {@code particles.png} sheet: the cell of the row {@code .375} and of the column
 * {@code runeIndex % 16}, drawn with the half size {@code 0.3f * (1 + rand.nextGaussian() * 0.1f)} (the
 * quad spans the half of that), turned by {@code rand.nextInt(4) * 90} degrees about Y and by ninety
 * degrees about X, and moved by the offsets {@code rand.nextFloat() * 0.2} and
 * {@code -0.3 + rand.nextFloat() * 0.6}.</p>
 *
 * <p>The alpha of the original (the oracle field {@code 82339_as}) is
 * {@code age <= maxAge / 5 ? age / (maxAge / 5) : (maxAge - age) / maxAge} and the original draws it
 * halved. The lifetime is three times the duration of the caller, the gravity of the original is
 * applied as {@code motionY -= 0.04 * gravity} and the particle has no collision.</p>
 */
public final class BlockRuneParticle extends SingleQuadParticle implements thaumcraft.client.fx.Tc4Particle {
    /** The original texture of the rune. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    /** The original row of the sheet the rune is drawn from. */
    public static final float ROW = .375F;
    private static final Tesselator BUFFER = new Tesselator(4096);

    private final float rotation;
    private final float offsetX;
    private final float offsetY;
    private final int cell;
    private final float gravity;

    /**
     * The original constructor.
     *
     * @param level the level of the client
     * @param x the position of the rune
     * @param y the position of the rune
     * @param z the position of the rune
     * @param red the red of the original
     * @param green the green of the original
     * @param blue the blue of the original
     * @param duration the duration of the caller, the lifetime is three times that
     * @param gravity the gravity of the caller
     */
    public BlockRuneParticle(ClientLevel level, double x, double y, double z,
                              float red, float green, float blue, int duration, float gravity) {
        super(level, x, y, z);
        // The original FXBlockRunes:29 draws the rotation before the size, the offsets and the scale.
        this.rotation = random.nextInt(4) * 90;
        setColor(red, green, blue);
        hasPhysics = false;
        xd = yd = zd = 0;
        lifetime = 3 * duration;
        setSize(.01F, .01F);
        xo = x;
        yo = y;
        zo = z;
        // The original FXBlockRunes:44 picks the cell with the global random, not with the own one.
        this.cell = (int) (Math.random() * 16.0 + 224.0) % 16;
        this.offsetX = random.nextFloat() * .2F;
        this.offsetY = -.3F + random.nextFloat() * .6F;
        // The original FXBlockRunes:47 - the size of the quad is 0.3f of that factor, and the quad spans
        // the half of it.
        quadSize = .15F * (float) (1.0 + random.nextGaussian() * .1);
        this.gravity = gravity;
        alpha = 0;
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        // The original FXBlockRunes:84-85 computes the alpha before the age grows.
        float threshold = lifetime / 5F;
        float age = this.age;
        alpha = (age <= threshold ? age / threshold : (lifetime - age) / (float) lifetime) / 2;
        if (this.age++ >= lifetime) {
            remove();
            return;
        }
        // The original FXBlockRunes:89-92.
        yd -= .04 * gravity;
        x += xd;
        y += yd;
        z += zd;
    }

    @Override protected float getU0() { return cell / 16F; }
    @Override protected float getU1() { return getU0() + .0624375F; }
    @Override protected float getV0() { return ROW; }
    @Override protected float getV1() { return ROW + .0624375F; }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        if (!isAlive()) return;
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        float opacity = alpha;
        try {
            RenderSystem.setShader(MonsterSpriteParticle::litShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            draw(buffer, camera, partialTick);
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

    /** The original FXBlockRunes:51-78: the quad of the rune with its two rotations and its offsets. */
    private void draw(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 eye = camera.getPosition();
        PoseStack pose = new PoseStack();
        pose.translate(Mth.lerp(partialTick, xo, x) - eye.x, Mth.lerp(partialTick, yo, y) - eye.y,
                Mth.lerp(partialTick, zo, z) - eye.z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.translate(offsetX, offsetY, -.51);
        float u0 = cell / 16F;
        float u1 = u0 + .0624375F;
        float v0 = ROW;
        float v1 = ROW + .0624375F;
        vertex(buffer, pose, -quadSize, quadSize, u1, v1);
        vertex(buffer, pose, quadSize, quadSize, u1, v0);
        vertex(buffer, pose, quadSize, -quadSize, u0, v0);
        vertex(buffer, pose, -quadSize, -quadSize, u0, v1);
    }

    private void vertex(VertexConsumer buffer, PoseStack pose, float x, float y, float u, float v) {
        buffer.addVertex(pose.last(), x, y, 0).setUv(u, v)
                .setColor(rCol, gCol, bCol, alpha).setLight(LightTexture.FULL_BRIGHT);
    }
}

package thaumcraft.client.fx.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Original TC4 {@code FXSmokeSpiral} (oracle {@code thaumcraft/client/fx/particles/FXSmokeSpiral.java:19-152}),
 * the smoke spiral of the Sword of the Zephyr and of the spawning eldritch warden.
 *
 * <p>The original particle never moves: its own {@code onUpdate} only fades the alpha
 * ({@code (maxAge - age) / maxAge}) and kills the particle, so the visible spiral is a pure
 * function of the age. The spiral turns a full {@code 720} degrees over its life, its polar angle
 * falls from {@code 90} to {@code -90} degrees and the radius scales the whole circle. The drawn
 * quad is the camera facing billboard of {@code 0.15 * particleScale} with the alpha
 * {@code 0.66 * fade} and it walks the first five cells of the particle sheet
 * ({@code 1 + age / maxAge * 4}).
 *
 * <p>The original's UV order is kept: the cell is mirrored horizontally, its left edge sits on
 * the right hand side of the quad. The original's own quirk of the grey pixels of the
 * {@code bloomColor} program is a different program and is not repeated here.
 */
public final class FXSmokeSpiral extends Particle {
    /** Original FXSmokeSpiral:44 and :64, the alpha of the smoke cell. */
    private static final float ALPHA = .66F;
    /** Original FXSmokeSpiral:58, the half size of the billboard is 0.15 of the particle scale. */
    private static final float QUAD_FACTOR = .15F;
    /** Original FXSmokeSpiral:32, the life of the spiral. */
    private static final int LIFE = 20, LIFE_SPREAD = 10;
    /** Original EntityFX:52, the default particle scale of 1..2. */
    private static final float SCALE = 2F;
    // A separate small native buffer: ParticleEngine already has its CUSTOM
    // buffer open while invoking render, so do not nest its singleton builder.
    private static final Tesselator BUFFER = new Tesselator(256);

    private final float radius;
    private final int start;
    private final int miny;
    private final float quadSize;

    private FXSmokeSpiral(ClientLevel level, double x, double y, double z, float radius, int start, int miny,
                           int red, int green, int blue) {
        super(level, x, y, z);
        this.radius = radius;
        this.start = start;
        this.miny = miny;
        // Original EntityFX:52: the particle scale is (random * 0.5 + 0.5) * 2.
        this.quadSize = QUAD_FACTOR * (random.nextFloat() * .5F + .5F) * SCALE;
        // Original FXSmokeSpiral:32.
        lifetime = LIFE + random.nextInt(LIFE_SPREAD);
        xd = yd = zd = 0;
        gravity = 0;
        hasPhysics = false;
        setSize(.01F, .01F);
        setColor(red / 255F, green / 255F, blue / 255F);
        alpha = ALPHA;
    }

    /**
     * Original {@code ClientProxy.smokeSpiral} (oracle
     * {@code thaumcraft/client/ClientProxy.java:677-683}): the spiral takes the colour of the
     * caller as a packed RGB value.
     */
    public static void spiral(ClientLevel level, double x, double y, double z, float radius, int start, int miny, int color) {
        Minecraft.getInstance().particleEngine.add(new FXSmokeSpiral(level, x, y, z, radius, start, miny,
                (color >> 16) & 255, (color >> 8) & 255, color & 255));
    }

    /** Original FXSmokeSpiral:75-80: the alpha fades over the life and the particle then dies. */
    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        alpha = ALPHA * (lifetime - age) / (float) lifetime;
        if (age++ >= lifetime) remove();
    }

    /** Original FXSmokeSpiral:45-53: the spiral of the cell and of the position. */
    private void spiral(float partialTick, Vec3 eye, PoseStack pose) {
        float t = (age + partialTick) / (float) lifetime;
        float r1 = start + 720F * t;
        float r2 = 90F - 180F * t;
        float mx = -Mth.sin(r1 * (float) Math.PI / 180F) * Mth.cos(r2 * (float) Math.PI / 180F);
        float mz = Mth.cos(r1 * (float) Math.PI / 180F) * Mth.cos(r2 * (float) Math.PI / 180F);
        float my = -Mth.sin(r2 * (float) Math.PI / 180F);
        double px = x + mx * radius;
        // Original FXSmokeSpiral:60: the spiral never sinks below the floor of its owner.
        double py = Math.max(y + my * radius, miny + .1);
        double pz = z + mz * radius;
        pose.translate(px - eye.x, py - eye.y, pz - eye.z);
    }

    @Override public void render(VertexConsumer unused, Camera camera, float partialTick) {
        // Original FXSmokeSpiral:54-57: the cell of the particle sheet.
        int cell = (int) (1F + age / (float) lifetime * 4F);
        float u0 = (cell % 16) / 16F, u1 = u0 + .0624375F;
        float v0 = (cell / 16) / 16F, v1 = v0 + .0624375F;
        var pose = new PoseStack();
        spiral(partialTick, camera.getPosition(), pose);
        pose.mulPose(camera.rotation());
        int light = getLightColor(partialTick);
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, ResourceParticle.TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            // Original FXSmokeSpiral:65-68: the cell is mirrored horizontally.
            vertex(buffer, pose, -quadSize, -quadSize, u1, v1, light);
            vertex(buffer, pose, -quadSize, quadSize, u1, v0, light);
            vertex(buffer, pose, quadSize, quadSize, u0, v0, light);
            vertex(buffer, pose, quadSize, -quadSize, u0, v1, light);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }

    private void vertex(VertexConsumer out, PoseStack pose, float px, float py, float u, float v, int light) {
        out.addVertex(pose.last(), px, py, 0).setUv(u, v)
                .setColor(rCol, gCol, bCol, alpha).setLight(light);
    }

    /** Original FXSmokeSpiral:63: the brightness of the block the spiral stands in. */
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
}

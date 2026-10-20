package thaumcraft.client.fx.particles;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Original FXSonic: retained hemisphere OBJ, fifteen ripple textures, source-following eye position, fixed attack yaw/pitch, age10..14. */
public final class WarpSonicParticle extends Particle {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/hemis.obj");
    private static final Tesselator BUFFER = new Tesselator(16384);
    private static List<Vertex[]> mesh;
    private final Entity target;
    private final float yaw, pitch;
    private record Vertex(float x, float y, float z, float u, float v) {}

    public WarpSonicParticle(ClientLevel level, Entity target) {
        super(level, target.getX(), target.getEyeY(), target.getZ());
        this.target = target; yaw = target.getYHeadRot(); pitch = target.getXRot();
        hasPhysics = false; lifetime = 10 + random.nextInt(5); setSize(.01F, .01F);
        xo = x; yo = y; zo = z;
    }
    public static void resetMesh() { mesh = null; }
    private static List<Vertex[]> mesh() {
        if (mesh != null) return mesh;
        var points = new ArrayList<float[]>(); var uvs = new ArrayList<float[]>(); var faces = new ArrayList<Vertex[]>();
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(MODEL)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                if (tokens[0].equals("v")) points.add(new float[]{Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])});
                else if (tokens[0].equals("vt")) uvs.add(new float[]{Float.parseFloat(tokens[1]), 1 - Float.parseFloat(tokens[2])});
                else if (tokens[0].equals("f")) {
                    Vertex[] polygon = new Vertex[tokens.length - 1];
                    for (int i = 1; i < tokens.length; i++) {
                        String[] indices = tokens[i].split("/"); float[] p = points.get(Integer.parseInt(indices[0]) - 1), uv = uvs.get(Integer.parseInt(indices[1]) - 1);
                        polygon[i - 1] = new Vertex(p[0], p[1], p[2], uv[0], uv[1]);
                    }
                    for (int i = 1; i < polygon.length - 1; i++) faces.add(new Vertex[]{polygon[0], polygon[i], polygon[i + 1]});
                }
            }
        } catch (IOException | RuntimeException failure) { throw new IllegalStateException("Cannot read retained sonic hemisphere " + MODEL, failure); }
        if (faces.isEmpty()) throw new IllegalStateException("Empty retained sonic hemisphere " + MODEL);
        mesh = List.copyOf(faces); return mesh;
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime || target.isRemoved()) { remove(); return; }
        x = target.getX(); y = target.getEyeY(); z = target.getZ();
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
    @Override public void render(VertexConsumer unused, Camera camera, float partial) {
        var oldShader = RenderSystem.getShader(); int oldTexture = RenderSystem.getShaderTexture(0);
        int frame = Math.min(15, (int)(14 * (age + partial) / lifetime) + 1);
        PoseStack pose = new PoseStack(); var eye = camera.getPosition();
        pose.translate(Mth.lerp(partial, xo, x) - eye.x, Mth.lerp(partial, yo, y) - eye.y, Mth.lerp(partial, zo, z) - eye.z);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw)); pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.translate(0, 0, 2 * target.getBbHeight() + target.getBbWidth() / 2);
        pose.scale(.25F * target.getBbHeight(), .25F * target.getBbHeight(), -target.getBbHeight());
        try {
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/ripple" + frame + ".png"));
            RenderSystem.enableBlend(); RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull(); RenderSystem.depthMask(false);
            var buffer = BUFFER.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.PARTICLE);
            for (Vertex[] face : mesh()) for (Vertex vertex : face)
                buffer.addVertex(pose.last(), vertex.x, vertex.y, vertex.z).setUv(vertex.u, vertex.v).setColor(.5F, .5F, .5F, 1F).setLight(220);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> oldShader); RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.defaultBlendFunc(); RenderSystem.disableBlend(); RenderSystem.enableCull(); RenderSystem.depthMask(true);
        }
    }
}

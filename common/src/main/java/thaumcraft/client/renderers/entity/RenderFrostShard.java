package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.projectile.EntityFrostShard;

/** TC4 RenderFrostShard:31-47. Reads the retained ORIGINAL 42-vertex/80-triangle
 * orb.obj into native vertices; no Forge OBJ loader, display list, placeholder
 * item/cube, donor model, entity/world cache or gameplay RNG consumption.
 * Renderer instances are recreated on native resource reload. Current entity
 * translucent shader/light/fog replace fixed-function GL, not visual parity. */
public final class RenderFrostShard extends EntityRenderer<EntityFrostShard> {
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/orb.obj");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/frostshard.png");
    private final List<Vertex> triangles;

    public RenderFrostShard(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
        triangles = loadMesh();
    }

    @Override public void render(EntityFrostShard entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.isRemoved()) return;
        Random scale = new Random(entity.getId()); // The original java.util.Random(entityID), not a test seed.
        float base = entity.getDamage() * .1F;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.scale(base + scale.nextFloat() * .1F, base + scale.nextFloat() * .1F, base + scale.nextFloat() * .1F);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (int triangle = 0; triangle < triangles.size(); triangle += 3) {
            emit(triangles.get(triangle), pose, buffer, light);
            emit(triangles.get(triangle + 1), pose, buffer, light);
            emit(triangles.get(triangle + 2), pose, buffer, light);
            // Native entity buffers are QUADS. A duplicated final corner leaves
            // the original triangle plus one degenerate triangle, not a new face.
            emit(triangles.get(triangle + 2), pose, buffer, light);
        }
        pose.popPose();
    }

    private static void emit(Vertex vertex, PoseStack pose, VertexConsumer buffer, int light) {
        buffer.addVertex(pose.last(), vertex.x, vertex.y, vertex.z).setColor(1F, 1F, 1F, 1F)
                .setUv(vertex.u, vertex.v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose.last(), vertex.nx, vertex.ny, vertex.nz);
    }

    @Override public ResourceLocation getTextureLocation(EntityFrostShard entity) { return TEXTURE; }

    /** Deliberately bounded to a small, triangulated, positive-index OBJ. Native
     * resource packs may replace the mesh, but malformed/oversize data is never
     * silently substituted with an invented visible placeholder. */
    private static List<Vertex> loadMesh() {
        try (var stream = Minecraft.getInstance().getResourceManager().open(MODEL)) {
            byte[] bytes = stream.readNBytes(65537);
            if (bytes.length > 65536) throw new IOException("Frost orb exceeds64KiB");
            List<float[]> positions = new ArrayList<>(), normals = new ArrayList<>(), uv = new ArrayList<>();
            List<Vertex> mesh = new ArrayList<>();
            String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\\R");
            if (lines.length > 1024) throw new IOException("Frost orb exceeds1024lines");
            for (String line : lines) {
                if (line.length() > 256) throw new IOException("Frost orb line exceeds256characters");
                String[] fields = line.strip().split("\\s+");
                switch (fields[0]) {
                    case "v" -> positions.add(vector(fields, false));
                    case "vn" -> normals.add(vector(fields, false));
                    case "vt" -> uv.add(vector(fields, true));
                    case "f" -> {
                        if (fields.length != 4 || mesh.size() >= 768) throw new IOException("Frost orb requires at most256triangles");
                        for (int corner = 1; corner <= 3; ++corner) {
                            String[] index = fields[corner].split("/");
                            if (index.length != 3) throw new IOException("Frost orb requires position/UV/normal");
                            float[] p = positions.get(Integer.parseInt(index[0]) - 1);
                            float[] t = uv.get(Integer.parseInt(index[1]) - 1);
                            float[] n = normals.get(Integer.parseInt(index[2]) - 1);
                            mesh.add(new Vertex(p[0], p[1], p[2], t[0], 1 - t[1], n[0], n[1], n[2]));
                        }
                    }
                    default -> { /* Original comments, group and smoothing declarations. */ }
                }
                if (positions.size() > 256 || normals.size() > 256 || uv.size() > 256) throw new IOException("Frost orb exceeds256entries");
            }
            if (mesh.isEmpty()) throw new IOException("Frost orb contains no triangles");
            return List.copyOf(mesh);
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new IllegalStateException("Cannot load retained native Frost mesh " + MODEL, failure);
        }
    }

    private static float[] vector(String[] fields, boolean texture) throws IOException {
        if (fields.length != 4 && !(texture && fields.length == 3)) throw new IOException("Invalid Frost OBJ vector");
        float[] value = new float[3];
        for (int coordinate = 1; coordinate < fields.length; ++coordinate) {
            value[coordinate - 1] = Float.parseFloat(fields[coordinate]);
            if (!Float.isFinite(value[coordinate - 1]) || Math.abs(value[coordinate - 1]) > 2) throw new IOException("Invalid Frost OBJ coordinate");
        }
        return value;
    }

    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
}

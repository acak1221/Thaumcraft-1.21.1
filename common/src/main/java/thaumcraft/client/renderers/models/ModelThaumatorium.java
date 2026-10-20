package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.geometry.HarnessObjMesh;

/**
 * Original TileThaumatoriumRenderer25-40 (oracle
 * {@code thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java}) draws the
 * thaumatorium from the retained Wavefront asset, not from boxes. The original
 * 1.7.10 has no {@code ModelThaumatorium} class: the model IS
 * {@code textures/models/thaumatorium.obj} on {@code textures/models/thaumatorium.png}.
 *
 * <p>The retained OBJ is a single triangulated group with v/vt/vn and an MTL
 * reference. The loader reads the same vertices, UVs and normals and reproduces
 * the original per-face UV inset, which keeps the seams from bleeding.
 */
public final class ModelThaumatorium {
    /** The original Wavefront loader flips V and insets each face around its centroid. */
    private static final float OBJ_UV_INSET = .0005F;
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/thaumatorium.obj");
    private final HarnessObjMesh mesh;

    public ModelThaumatorium() {
        try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MESH).open()) {
            mesh = HarnessObjMesh.read(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException invalid) {
            throw new IllegalStateException("Cannot load the retained thaumatorium mesh", invalid);
        }
    }

    /** The retained asset, for callers that need the original triangle data. */
    public HarnessObjMesh mesh() {
        return mesh;
    }

    /** Draws the whole original model; the caller owns the pose and the buffer. */
    public void render(PoseStack pose, VertexConsumer vertices, int light, int overlay) {
        for (var triangle : mesh.triangles()) {
            float averageU = (triangle.a().u() + triangle.b().u() + triangle.c().u()) / 3F;
            float averageV = (triangle.a().v() + triangle.b().v() + triangle.c().v()) / 3F;
            vertex(pose, vertices, triangle.a(), averageU, averageV, light);
            vertex(pose, vertices, triangle.b(), averageU, averageV, light);
            vertex(pose, vertices, triangle.c(), averageU, averageV, light);
            // Native entity consumers are quad batches: a repeated final
            // vertex preserves exactly one triangle and one degenerate.
            vertex(pose, vertices, triangle.c(), averageU, averageV, light);
        }
    }

    private static void vertex(PoseStack pose, VertexConsumer out, HarnessObjMesh.Vertex vertex, float averageU, float averageV, int light) {
        float u = vertex.u() + (vertex.u() > averageU ? -OBJ_UV_INSET : OBJ_UV_INSET);
        float v = vertex.v() + (vertex.v() > averageV ? -OBJ_UV_INSET : OBJ_UV_INSET);
        out.addVertex(pose.last(), vertex.x(), vertex.y(), vertex.z()).setColor(0xffffffff).setUv(u, v).setLight(light)
                .setNormal(pose.last(), vertex.nx(), vertex.ny(), vertex.nz());
    }
}

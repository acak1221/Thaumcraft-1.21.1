package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Small, bounded bridge for the retained Z-up pillar OBJ, not a general OBJ loader. */
public final class ModelInfusionPillar {
    private static final ResourceLocation OBJ = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/pillar.obj");
    private final List<Vertex> triangles;

    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    public ModelInfusionPillar(ResourceManager resources) {
        List<float[]> positions = new ArrayList<>(), coordinates = new ArrayList<>(), normals = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>();
        try (BufferedReader reader = resources.getResourceOrThrow(OBJ).openAsReader()) {
            String line;
            int lines = 0, groups = 0;
            while ((line = reader.readLine()) != null) {
                if (++lines > 4096 || line.length() > 1024) throw new IOException("Pillar OBJ exceeds bounded format");
                String[] fields = line.trim().split("\\s+");
                switch (fields[0]) {
                    case "v" -> positions.add(vector(fields, 3));
                    case "vt" -> coordinates.add(vector(fields, 2));
                    case "vn" -> normals.add(vector(fields, 3));
                    case "g" -> {
                        if (fields.length < 2 || ++groups > 32) throw new IOException("Invalid pillar group");
                    }
                    case "f" -> {
                        if (groups == 0 || fields.length != 4 || vertices.size() >= 8190) {
                            throw new IOException("Pillar faces must be grouped, bounded triangles");
                        }
                        for (int i = 1; i <= 3; i++) {
                            String[] indices = fields[i].split("/", -1);
                            if (indices.length != 3) throw new IOException("Pillar requires position/UV/normal indices");
                            float[] p = entry(positions, indices[0]);
                            float[] t = entry(coordinates, indices[1]);
                            float[] n = entry(normals, indices[2]);
                            vertices.add(new Vertex(p[0], p[1], p[2], t[0], 1 - t[1], n[0], n[1], n[2]));
                        }
                    }
                    // Original starts at g Box001, has 24 Z-up vertices and 44
                    // triangle faces. Every group uses caller-bound pillar.png;
                    // editor material colours must NOT tint that original texture.
                    default -> { /* Blank/comment/object/smoothing/material declarations. */ }
                }
                if (positions.size() > 1024 || coordinates.size() > 1024 || normals.size() > 1024) {
                    throw new IOException("Pillar vertex tables exceed bounded format");
                }
            }
            if (vertices.isEmpty()) throw new IOException("Empty pillar OBJ");
            triangles = List.copyOf(vertices);
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to load original thaumcraft pillar OBJ", exception);
        }
    }

    private static float[] vector(String[] fields, int size) throws IOException {
        if (fields.length < size + 1) throw new IOException("Incomplete pillar vector");
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = Float.parseFloat(fields[i + 1]);
            if (!Float.isFinite(result[i])) throw new IOException("Non-finite pillar coordinate");
        }
        return result;
    }

    private static float[] entry(List<float[]> table, String text) throws IOException {
        int index = Integer.parseInt(text);
        if (index < 1 || index > table.size()) throw new IOException("Invalid pillar OBJ index");
        return table.get(index - 1);
    }

    /** Consumer MUST use TRIANGLES/NEW_ENTITY, not a quad RenderType. */
    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        for (Vertex vertex : triangles) {
            buffer.addVertex(pose.last(), vertex.x, vertex.y, vertex.z).setColor(255, 255, 255, 255)
                    .setUv(vertex.u, vertex.v).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), vertex.nx, vertex.ny, vertex.nz);
        }
    }
}

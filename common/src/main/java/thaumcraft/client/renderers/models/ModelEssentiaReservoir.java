package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** The original reservoir.obj's single Cylinder001 group and 236 triangles. */
public final class ModelEssentiaReservoir {
    private static final ResourceLocation OBJ = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/reservoir.obj");
    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
    private final List<Vertex> triangles;

    public ModelEssentiaReservoir(ResourceManager resources) {
        var positions = new ArrayList<float[]>();
        var uv = new ArrayList<float[]>();
        var normals = new ArrayList<float[]>();
        var vertices = new ArrayList<Vertex>();
        try (BufferedReader reader = resources.getResourceOrThrow(OBJ).openAsReader()) {
            String line; int count = 0;
            while ((line = reader.readLine()) != null) {
                if (++count > 4096) throw new IOException("Reservoir OBJ exceeds its bounded format");
                String[] words = line.trim().split("\\s+");
                switch (words[0]) {
                    case "v" -> positions.add(vector(words, 3));
                    case "vt" -> uv.add(vector(words, 2));
                    case "vn" -> normals.add(vector(words, 3));
                    case "g" -> {
                        if (words.length != 2 || !words[1].equals("Cylinder001")) throw new IOException("Unknown reservoir group");
                    }
                    case "f" -> {
                        if (words.length != 4 || vertices.size() >= 4096) throw new IOException("Reservoir requires bounded triangle faces");
                        for (int i = 1; i <= 3; i++) {
                            String[] indices = words[i].split("/");
                            if (indices.length != 3) throw new IOException("Reservoir face requires position/UV/normal");
                            float[] p = positions.get(Integer.parseInt(indices[0]) - 1);
                            float[] t = uv.get(Integer.parseInt(indices[1]) - 1);
                            float[] n = normals.get(Integer.parseInt(indices[2]) - 1);
                            vertices.add(new Vertex(p[0], p[1], p[2], t[0], 1 - t[1], n[0], n[1], n[2]));
                        }
                    }
                    default -> { }
                }
                if (positions.size() > 1024 || uv.size() > 1024 || normals.size() > 1024) throw new IOException("Reservoir vertex table exceeds its bounded format");
            }
            if (vertices.isEmpty()) throw new IOException("Missing reservoir mesh");
            triangles = List.copyOf(vertices);
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new IllegalStateException("Cannot read original reservoir OBJ", failure);
        }
    }

    private static float[] vector(String[] words, int size) throws IOException {
        if (words.length < size + 1) throw new IOException("Incomplete reservoir vector");
        float[] out = new float[size];
        for (int i = 0; i < size; i++) {
            out[i] = Float.parseFloat(words[i + 1]);
            if (!Float.isFinite(out[i])) throw new IOException("Non-finite reservoir vector");
        }
        return out;
    }

    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        for (Vertex v : triangles) buffer.addVertex(pose.last(), v.x, v.y, v.z).setColor(-1)
                .setUv(v.u, v.v).setOverlay(overlay).setLight(light).setNormal(pose.last(), v.nx, v.ny, v.nz);
    }
}

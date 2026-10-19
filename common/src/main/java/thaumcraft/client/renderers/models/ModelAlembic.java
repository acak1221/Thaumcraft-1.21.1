package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Only the original alembic's five named, triangulated OBJ groups; not an OBJ framework. */
public final class ModelAlembic {
    private static final ResourceLocation OBJ = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/alembic.obj");
    private static final ResourceLocation MTL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/alembic.mtl");
    private static final Set<String> GROUPS = Set.of("Pot", "Legs", "TubeMain", "TubeSmall", "Panel");
    private final Map<String, List<Vertex>> groups;

    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    public ModelAlembic(ResourceManager resources) {
        try {
            Set<String> materials = new HashSet<>();
            try (BufferedReader reader = resources.getResourceOrThrow(MTL).openAsReader()) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (++count > 256) throw new IOException("Alembic MTL exceeds its bounded format");
                    String[] fields = line.trim().split("\\s+");
                    if (fields[0].equals("newmtl") && fields.length == 2) materials.add(fields[1]);
                }
            }
            // The original renderer binds alembic.png for EVERY group. Its
            // .mtl has only editor wire colours, no map_Kd. Multiplying by those
            // Kd values would incorrectly turn the textured brass pot purple.
            Map<String, List<Vertex>> parsed = new HashMap<>();
            List<float[]> positions = new ArrayList<>(), uv = new ArrayList<>(), normals = new ArrayList<>();
            List<Vertex> group = null;
            try (BufferedReader reader = resources.getResourceOrThrow(OBJ).openAsReader()) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    if (++count > 4096) throw new IOException("Alembic OBJ exceeds its bounded format");
                    String[] fields = line.trim().split("\\s+");
                    switch (fields[0]) {
                        case "v" -> positions.add(vector(fields, 3));
                        case "vt" -> uv.add(vector(fields, 2));
                        case "vn" -> normals.add(vector(fields, 3));
                        case "g" -> {
                            if (fields.length != 2 || !GROUPS.contains(fields[1])) throw new IOException("Unknown alembic group");
                            group = parsed.computeIfAbsent(fields[1], ignored -> new ArrayList<>());
                        }
                        case "mtllib" -> {
                            if (fields.length != 2 || !fields[1].equals("alembic.mtl")) throw new IOException("Unknown alembic material library");
                        }
                        case "usemtl" -> {
                            if (fields.length != 2 || !materials.contains(fields[1])) throw new IOException("Unknown alembic material");
                        }
                        case "f" -> {
                            if (group == null || fields.length != 4 || group.size() > 4096) throw new IOException("Alembic faces must be bounded triangles");
                            for (int i = 1; i <= 3; ++i) {
                                String[] indices = fields[i].split("/");
                                if (indices.length != 3) throw new IOException("Alembic face requires position/UV/normal");
                                float[] p = positions.get(Integer.parseInt(indices[0]) - 1);
                                float[] t = uv.get(Integer.parseInt(indices[1]) - 1);
                                float[] n = normals.get(Integer.parseInt(indices[2]) - 1);
                                group.add(new Vertex(p[0], p[1], p[2], t[0], 1 - t[1], n[0], n[1], n[2]));
                            }
                        }
                        default -> { /* Comments, blank lines and original smoothing groups. */ }
                    }
                    if (positions.size() > 1024 || uv.size() > 1024 || normals.size() > 1024) throw new IOException("Alembic vertex table exceeds its bounded format");
                }
            }
            if (!parsed.keySet().equals(GROUPS) || parsed.values().stream().anyMatch(List::isEmpty)) throw new IOException("Missing original alembic group");
            parsed.replaceAll((name, vertices) -> List.copyOf(vertices));
            groups = Map.copyOf(parsed);
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException exception) {
            throw new IllegalStateException("Unable to load original thaumcraft alembic OBJ/MTL", exception);
        }
    }

    private static float[] vector(String[] fields, int size) throws IOException {
        if (fields.length < size + 1) throw new IOException("Incomplete alembic vector");
        float[] result = new float[size];
        for (int i = 0; i < size; ++i) {
            result[i] = Float.parseFloat(fields[i + 1]);
            if (!Float.isFinite(result[i])) throw new IOException("Non-finite alembic vector");
        }
        return result;
    }

    /** Buffer must use TRIANGLES/NEW_ENTITY; UVs, normals, light and overlay are native. */
    public void render(String group, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        for (Vertex vertex : groups.get(group)) {
            buffer.addVertex(pose.last(), vertex.x, vertex.y, vertex.z).setColor(255, 255, 255, 255)
                    .setUv(vertex.u, vertex.v).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), vertex.nx, vertex.ny, vertex.nz);
        }
    }
}

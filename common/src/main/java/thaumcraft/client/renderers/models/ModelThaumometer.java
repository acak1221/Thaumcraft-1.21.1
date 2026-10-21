package thaumcraft.client.renderers.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The original thaumometer body of {@code textures/models/scanner.obj}.
 *
 * <p>The original ItemThaumometerRenderer:74 loads the OBJ with the Forge
 * {@code AdvancedModelLoader} and draws it with {@code textures/models/scanner.png}.
 * The native tree has no OBJ loader, so the twenty four original quads of the
 * single {@code scanner} group are parsed here into native vertices; the
 * geometry, the texture coordinates and the normals are the original ones.
 */
public final class ModelThaumometer {
    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}

    /** The original sheet of the scanner body. */
    public static final net.minecraft.resources.ResourceLocation TEXTURE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/scanner.png");
    /** The original sheet of the scan screen. */
    public static final net.minecraft.resources.ResourceLocation SCREEN =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/scanscreen.png");

    private static final List<Vertex> QUADS = load();

    public ModelThaumometer() {}

    private static List<Vertex> load() {
        var positions = new ArrayList<float[]>();
        var uv = new ArrayList<float[]>();
        var normals = new ArrayList<float[]>();
        var quads = new ArrayList<Vertex>();
        var stream = ModelThaumometer.class.getClassLoader().getResourceAsStream("assets/thaumcraft/textures/models/scanner.obj");
        if (stream == null) throw new IllegalStateException("Missing original thaumometer OBJ");
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String row;
            while ((row = reader.readLine()) != null) {
                String[] words = row.trim().split("\\s+");
                if (words.length == 0 || words[0].isEmpty() || words[0].startsWith("#")) continue;
                switch (words[0]) {
                    case "v" -> positions.add(new float[]{Float.parseFloat(words[1]), Float.parseFloat(words[2]), Float.parseFloat(words[3])});
                    case "vt" -> uv.add(new float[]{Float.parseFloat(words[1]), 1F - Float.parseFloat(words[2])});
                    case "vn" -> normals.add(new float[]{Float.parseFloat(words[1]), Float.parseFloat(words[2]), Float.parseFloat(words[3])});
                    case "f" -> {
                        if (words.length != 5) throw new IllegalStateException("Original scanner requires grouped quads");
                        for (int i = 1; i <= 4; i++) {
                            String[] index = words[i].split("/");
                            float[] p = positions.get(Integer.parseInt(index[0]) - 1);
                            float[] t = uv.get(Integer.parseInt(index[1]) - 1);
                            float[] n = normals.get(Integer.parseInt(index[2]) - 1);
                            quads.add(new Vertex(p[0], p[1], p[2], t[0], t[1], n[0], n[1], n[2]));
                        }
                    }
                    default -> {}
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot read original thaumometer OBJ", failure);
        }
        if (quads.size() != 24 * 4) throw new IllegalStateException("Original scanner geometry count changed");
        return List.copyOf(quads);
    }

    /** Original ItemThaumometerRenderer:149 - the whole scanner body. */
    public void render(PoseStack pose, VertexConsumer output, int light, int overlay) {
        for (Vertex vertex : QUADS) {
            output.addVertex(pose.last(), vertex.x, vertex.y, vertex.z).setColor(-1)
                    .setUv(vertex.u, vertex.v).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), vertex.nx, vertex.ny, vertex.nz);
        }
    }
}

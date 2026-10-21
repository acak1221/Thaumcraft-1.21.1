package thaumcraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The four warp post chains of the original TC4 4.2.3.5 and the way 1.21.1 resolves their
 * post programs.
 *
 * <p>Original chains (the tracked original JAR, {@code assets/minecraft/shaders/post}):
 * {@code blurtc.json} (two {@code blur} passes, Radius 10, BlurDir 1,0 and 0,1),
 * {@code desaturatetc.json} ({@code color_convolve} with Saturation 0.8 and {@code blit}),
 * {@code hunger.json} (TC4's own {@code color_convolve2} with ColorScale 1,0.8,0.8 and
 * Saturation 1.1, then {@code blit}) and {@code sunscorned.json} (TC4's own {@code bloomColor}
 * and {@code bloom2}).
 *
 * <p>1.21.1 resolves a post program in the DEFAULT namespace only:
 * {@code net.minecraft.client.renderer.EffectInstance} looks the program description up as
 * {@code minecraft:shaders/program/<name>.json} and the vertex/fragment sources as
 * {@code minecraft:shaders/program/<name>.vsh} and {@code .fsh}. A pass named
 * {@code minecraft:box_blur} or {@code thaumcraft:bloom2} therefore resolves to
 * {@code minecraft:shaders/program/minecraft:box_blur.json} and can never be loaded, which is
 * how the previous wave of this port lost all four warp effects.
 */
class WarpPostChainTest {
    /** The vanilla 1.21.1 post programs the chains may use without shipping them. */
    private static final Set<String> VANILLA_PROGRAMS = Set.of(
            "blit", "blur", "bits", "box_blur", "color_convolve", "entity_outline",
            "entity_outline_box_blur", "entity_sobel", "invert", "spiderclip", "transparency");
    /** The vanilla 1.21.1 vertex programs the chains may use without shipping them. */
    private static final Set<String> VANILLA_VERTEX = Set.of(
            "blit", "blur", "rotscale", "screenquad", "sobel");

    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private boolean exists(String path) { return getClass().getClassLoader().getResource(path) != null; }

    private static List<String> passNames(JsonObject chain) {
        List<String> names = new ArrayList<>();
        for (JsonElement element : chain.getAsJsonArray("passes")) {
            names.add(element.getAsJsonObject().get("name").getAsString());
        }
        return names;
    }

    @Test void everyWarpChainPassNamesAProgramThatOneTwentyOneCanResolve() throws Exception {
        for (String chain : List.of("warp_blurred", "warp_death_gaze", "warp_hunger", "warp_sun_scorned")) {
            String path = "assets/thaumcraft/shaders/post/" + chain + ".json";
            JsonObject description = json(path);
            JsonArray passes = description.getAsJsonArray("passes");
            assertFalse(passes.isEmpty(), chain + " has no pass");
            for (JsonElement element : passes) {
                JsonObject pass = element.getAsJsonObject();
                String name = pass.get("name").getAsString();
                // A namespace in the pass name is the defect this test guards against: the
                // 1.21.1 loader would look for minecraft:shaders/program/<name>:<name>.json.
                assertFalse(name.contains(":"), chain + " names the program " + name + " with a namespace");
                boolean native_ = exists("assets/minecraft/shaders/program/" + name + ".json");
                assertTrue(native_ || VANILLA_PROGRAMS.contains(name),
                        chain + " names the post program " + name + ", which neither the port nor 1.21.1 ships");
                assertNotNull(pass.get("intarget"), chain);
                assertNotNull(pass.get("outtarget"), chain);
            }
            // The first pass reads the world and the last one writes it back.
            assertEquals("minecraft:main", passes.get(0).getAsJsonObject().get("intarget").getAsString(), chain);
            assertEquals("minecraft:main", passes.get(passes.size() - 1).getAsJsonObject().get("outtarget").getAsString(), chain);
        }
    }

    @Test void thePortedOriginalProgramsKeepTheirOriginalPassOrderAndUniforms() throws Exception {
        // Original blurtc.json: two blur passes, Radius 10, the x axis and then the y axis.
        JsonObject blur = json("assets/thaumcraft/shaders/post/warp_blurred.json");
        List<String> blurPasses = passNames(blur);
        assertEquals(List.of("box_blur", "box_blur"), blurPasses);
        JsonArray blurPass = blur.getAsJsonArray("passes").get(0).getAsJsonObject().getAsJsonArray("uniforms");
        assertEquals("BlurDir", blurPass.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(1.0D, blurPass.get(0).getAsJsonObject().getAsJsonArray("values").get(0).getAsDouble());
        assertEquals(0.0D, blurPass.get(0).getAsJsonObject().getAsJsonArray("values").get(1).getAsDouble());
        assertEquals("Radius", blurPass.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals(10.0D, blurPass.get(1).getAsJsonObject().getAsJsonArray("values").get(0).getAsDouble());
        JsonArray blurSecond = blur.getAsJsonArray("passes").get(1).getAsJsonObject().getAsJsonArray("uniforms");
        assertEquals(0.0D, blurSecond.get(0).getAsJsonObject().getAsJsonArray("values").get(0).getAsDouble());
        assertEquals(1.0D, blurSecond.get(0).getAsJsonObject().getAsJsonArray("values").get(1).getAsDouble());

        // Original desaturatetc.json: color_convolve with Saturation 0.8, then blit.
        JsonObject gaze = json("assets/thaumcraft/shaders/post/warp_death_gaze.json");
        assertEquals(List.of("color_convolve", "blit"), passNames(gaze));
        JsonArray gazeUniforms = gaze.getAsJsonArray("passes").get(0).getAsJsonObject().getAsJsonArray("uniforms");
        assertEquals("Saturation", gazeUniforms.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals(0.8D, gazeUniforms.get(0).getAsJsonObject().getAsJsonArray("values").get(0).getAsDouble());

        // Original hunger.json: TC4's own color_convolve2 with ColorScale 1,0.8,0.8 and
        // Saturation 1.1, then blit.
        JsonObject hunger = json("assets/thaumcraft/shaders/post/warp_hunger.json");
        assertEquals(List.of("thaumcraft_color_convolve2", "blit"), passNames(hunger));
        JsonArray hungerUniforms = hunger.getAsJsonArray("passes").get(0).getAsJsonObject().getAsJsonArray("uniforms");
        assertEquals("ColorScale", hungerUniforms.get(0).getAsJsonObject().get("name").getAsString());
        JsonArray scale = hungerUniforms.get(0).getAsJsonObject().getAsJsonArray("values");
        assertEquals(1.0D, scale.get(0).getAsDouble());
        assertEquals(0.8D, scale.get(1).getAsDouble());
        assertEquals(0.8D, scale.get(2).getAsDouble());
        assertEquals("Saturation", hungerUniforms.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals(1.1D, hungerUniforms.get(1).getAsJsonObject().getAsJsonArray("values").get(0).getAsDouble());

        // Original sunscorned.json: TC4's own bloomColor, then its bloom2.
        JsonObject sun = json("assets/thaumcraft/shaders/post/warp_sun_scorned.json");
        assertEquals(List.of("thaumcraft_bloom_color", "thaumcraft_bloom2"), passNames(sun));
    }

    @Test void thePortedOriginalProgramsShipTheirVertexAndFragmentSources() throws Exception {
        for (String program : List.of("thaumcraft_color_convolve2", "thaumcraft_bloom_color", "thaumcraft_bloom2")) {
            JsonObject description = json("assets/minecraft/shaders/program/" + program + ".json");
            String vertex = description.get("vertex").getAsString();
            String fragment = description.get("fragment").getAsString();
            assertFalse(vertex.contains(":"), program + " names its vertex program with a namespace");
            assertFalse(fragment.contains(":"), program + " names its fragment program with a namespace");
            assertTrue(exists("assets/minecraft/shaders/program/" + vertex + ".vsh") || VANILLA_VERTEX.contains(vertex),
                    program + " names the vertex program " + vertex + ", which neither the port nor 1.21.1 ships");
            assertTrue(exists("assets/minecraft/shaders/program/" + fragment + ".fsh") || VANILLA_PROGRAMS.contains(fragment),
                    program + " names the fragment program " + fragment + ", which neither the port nor 1.21.1 ships");
            // The 1.21.1 post quad is drawn with the Position attribute only.
            assertEquals("Position", description.getAsJsonArray("attributes").get(0).getAsString(), program);
        }
        // The shared vertex program of the two bloom programs carries the original flip.
        assertTrue(exists("assets/minecraft/shaders/program/thaumcraft_bloom_vertex_base.vsh"));
    }
}

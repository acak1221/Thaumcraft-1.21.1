package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The Traveling Trunk item (original ItemTrunkSpawnerRenderer draws the vanilla chest model with textures/models/trunk.png). The port's
 * model is the same three boxes - body, lid and latch of ModelChest - whose faces read the 64x64 skin the way the box unwrap of a
 * ModelPart does; the expected rectangles are computed here from the box sizes and texture offsets, not copied from the file.
 */
class TrunkSpawnerModelTest {
    private JsonObject model() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/thaumcraft/models/item/trunk_spawner.json");
        assertNotNull(stream, "missing trunk_spawner model");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void box(JsonObject element, int[] from, int[] to, int u, int v, int w, int h, int d) {
        assertEquals(3, element.getAsJsonArray("from").size());
        for (int axis = 0; axis < 3; axis++) {
            assertEquals(from[axis], element.getAsJsonArray("from").get(axis).getAsFloat(), 0F, "from " + axis);
            assertEquals(to[axis], element.getAsJsonArray("to").get(axis).getAsFloat(), 0F, "to " + axis);
        }
        double s = 16.0 / 64.0;
        double[][] expected = {
                {u + d, v, u + d + w, v + d},                         // up
                {u + d + w, v, u + d + 2 * w, v + d},                 // down
                {u, v + d, u + d, v + d + h},                         // west
                {u + d, v + d, u + d + w, v + d + h},                 // north (the front)
                {u + d + w, v + d, u + 2 * d + w, v + d + h},         // east
                {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h}  // south
        };
        String[] names = {"up", "down", "west", "north", "east", "south"};
        for (int face = 0; face < 6; face++) {
            JsonObject json = element.getAsJsonObject("faces").getAsJsonObject(names[face]);
            assertEquals("#trunk", json.get("texture").getAsString(), names[face]);
            JsonArray uv = json.getAsJsonArray("uv");
            for (int corner = 0; corner < 4; corner++) assertEquals(expected[face][corner] * s, uv.get(corner).getAsDouble(), 1e-3, names[face] + " uv " + corner);
        }
    }

    @Test void itIsTheVanillaChestBoxesOnTheTrunkSkin() throws Exception {
        JsonObject model = model();
        assertEquals("minecraft:block/block", model.get("parent").getAsString());
        assertEquals("thaumcraft:models/trunk", model.getAsJsonObject("textures").get("trunk").getAsString());
        assertEquals("thaumcraft:models/trunk", model.getAsJsonObject("textures").get("particle").getAsString());
        assertNotNull(getClass().getClassLoader().getResource("assets/thaumcraft/textures/models/trunk.png"), "the skin");
        JsonArray elements = model.getAsJsonArray("elements");
        assertEquals(3, elements.size());
        // ModelChest: body 14x10x14 from (1, 0, 1); lid 14x5x14 from (1, 9, 1) overlapping the body by a pixel; latch 2x4x1 at the front.
        box(elements.get(0).getAsJsonObject(), new int[] {1, 0, 1}, new int[] {15, 10, 15}, 0, 19, 14, 10, 14);
        box(elements.get(1).getAsJsonObject(), new int[] {1, 9, 1}, new int[] {15, 14, 15}, 0, 0, 14, 5, 14);
        box(elements.get(2).getAsJsonObject(), new int[] {7, 7, 0}, new int[] {9, 11, 1}, 0, 0, 2, 4, 1);
        assertTrue(model.getAsJsonObject("textures").size() == 2);
    }
}

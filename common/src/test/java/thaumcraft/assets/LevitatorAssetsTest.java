package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Resource wiring of the Arcane Levitator (original ConfigBlocks, ConfigRecipes:194, ConfigResearch LEVITATOR). */
class LevitatorAssetsTest {
    private JsonObject json(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void theBlockHasItsStateModelsTexturesAndNames() throws Exception {
        assertEquals("thaumcraft:block/levitator", json("assets/thaumcraft/blockstates/levitator.json").getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        var model = json("assets/thaumcraft/models/block/levitator.json");
        assertEquals("minecraft:block/cube_bottom_top", model.get("parent").getAsString());
        var textures = model.getAsJsonObject("textures");
        assertEquals("thaumcraft:blocks/liftertop", textures.get("top").getAsString());
        assertEquals("thaumcraft:blocks/lifterside", textures.get("side").getAsString());
        assertEquals("thaumcraft:blocks/arcaneearbottom", textures.get("bottom").getAsString());
        for (String texture : new String[] {"liftertop", "lifterside", "arcaneearbottom"}) {
            assertTrue(getClass().getClassLoader().getResource("assets/thaumcraft/textures/blocks/" + texture + ".png") != null, texture);
        }
        assertEquals("Arcane Levitator", json("assets/thaumcraft/lang/en_us.json").get("block.thaumcraft.levitator").getAsString());
        assertEquals("Магический подъёмник", json("assets/thaumcraft/lang/ru_ru.json").get("block.thaumcraft.levitator").getAsString());
    }

    /**
     * The item of the levitator is the inventory model of the original, not the plain block.
     *
     * <p>Original {@code BlockLifterRenderer.renderInventoryBlock} (oracle
     * {@code thaumcraft/client/renderers/block/BlockLifterRenderer.java:32-53}): the item in the
     * inventory slot was drawn by the render handler of the block itself - the cube of the block
     * plus the two emissive inner slabs of the {@code animatedglow} sheet, the upper one of the
     * bounds 0.01..0.99 by 0.9..0.99 in the colour
     * {@code BlockCustomOreItem.colors[4]} and the lower one of the bounds 0.01..0.99 by
     * 0.1..0.9 in the colour {@code BlockCustomOreItem.colors[5]}. Forge 1.7.10 routes such a
     * block to that method: {@code RenderingRegistry.renderItemAsFull3DBlock} answers the
     * {@code shouldRender3DInInventory} of the handler and
     * {@code RenderingRegistry.renderInventoryBlock} draws the inventory art of the block.
     * The 1.12 successor of that very code is the item model with the same two slabs and the
     * tint indices 0 and 1, which is what the port has.
     */
    @Test void theItemIsTheInventoryModelOfTheOriginal() throws Exception {
        var item = json("assets/thaumcraft/models/item/levitator.json");
        assertEquals("minecraft:block/block", item.get("parent").getAsString());
        // The inventory render of 1.7.10 shaded the faces of the cube, it had no ambient
        // occlusion; the 1.12 model of the same art says so as well.
        assertFalse(item.get("ambientocclusion").getAsBoolean(), "the original item had no ambient occlusion");
        var textures = item.getAsJsonObject("textures");
        assertEquals("thaumcraft:blocks/liftertop", textures.get("top").getAsString());
        assertEquals("thaumcraft:blocks/lifterside", textures.get("side").getAsString());
        assertEquals("thaumcraft:blocks/arcaneearbottom", textures.get("bottom").getAsString());
        assertEquals("thaumcraft:blocks/animatedglow", textures.get("glow").getAsString());
        var elements = item.getAsJsonArray("elements");
        assertEquals(3, elements.size(), "the cube of the original and its two emissive slabs");
        // The cube: the icons of the original, bottom arcaneearbottom, top liftertop, sides lifterside.
        var cube = elements.get(0).getAsJsonObject();
        assertEquals(JsonParser.parseString("[0,0,0]"), cube.get("from"));
        assertEquals(JsonParser.parseString("[16,16,16]"), cube.get("to"));
        var cubeFaces = cube.getAsJsonObject("faces");
        assertEquals("#bottom", cubeFaces.getAsJsonObject("down").get("texture").getAsString());
        assertEquals("#top", cubeFaces.getAsJsonObject("up").get("texture").getAsString());
        for (String face : new String[] {"north", "south", "west", "east"}) {
            assertEquals("#side", cubeFaces.getAsJsonObject(face).get("texture").getAsString(), face);
        }
        // The upper emissive slab of the original: 0.01..0.99 by 0.9..0.99, the tint 0.
        assertSlab(elements.get(1).getAsJsonObject(), 0, "[0.16,14.4,0.16]", "[15.84,15.84,15.84]");
        // The lower emissive slab of the original: 0.01..0.99 by 0.1..0.9, the tint 1.
        assertSlab(elements.get(2).getAsJsonObject(), 1, "[0.16,1.6,0.16]", "[15.84,14.4,15.84]");
    }

    /**
     * The two emissive slabs of the item carry the ore colours of the original.
     *
     * <p>Original {@code BlockLifterRenderer.renderInventoryBlock} (oracle
     * {@code thaumcraft/client/renderers/block/BlockLifterRenderer.java:36-51}): the upper slab is
     * tinted with {@code BlockCustomOreItem.colors[4]} and the lower one with
     * {@code BlockCustomOreItem.colors[5]}. The colour table of the original is
     * {@code {0xFFFFFF, 0xFFFF7E, 16727041, 37119, 40960, 0xEECCFF, 0x555577}} (oracle
     * {@code thaumcraft/common/blocks/BlockCustomOreItem.java:17}), so the upper slab is
     * 40960 = 0x00A000 and the lower one is 15650047 = 0xEECCFF.
     */
    @Test void theEmissiveSlabsOfTheItemKeepTheOriginalOreColours() throws Exception {
        Path file = Path.of("src/main/java", "thaumcraft/client/ClientSetup.java");
        assertTrue(Files.exists(file), "the source " + file + " is missing");
        String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(source.contains("registerItemColors"), "the port registers no item colour");
        assertTrue(source.contains("LEVITATOR_ITEM"), "the port tints no levitator item");
        assertTrue(source.contains("0x00A000"), "the upper slab keeps the colour 4 of the original");
        assertTrue(source.contains("0xEECCFF"), "the lower slab keeps the colour 5 of the original");
    }

    /** One emissive slab of the item model: its bounds and the tint of all its six faces. */
    private static void assertSlab(JsonObject element, int tint, String from, String to) {
        assertEquals(JsonParser.parseString(from), element.get("from"));
        assertEquals(JsonParser.parseString(to), element.get("to"));
        var faces = element.getAsJsonObject("faces");
        for (String face : new String[] {"down", "up", "north", "south", "west", "east"}) {
            var entry = faces.getAsJsonObject(face);
            assertEquals("#glow", entry.get("texture").getAsString(), face);
            assertEquals(tint, entry.get("tintindex").getAsInt(), face);
        }
    }

    @Test void itDropsItselfAndIsChoppedWithAnAxe() throws Exception {
        var loot = json("data/thaumcraft/loot_table/blocks/levitator.json");
        assertEquals("thaumcraft:levitator", loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        assertTrue(json("data/minecraft/tags/block/mineable/axe.json").getAsJsonArray("values").asList().stream().anyMatch(v -> v.getAsString().equals("thaumcraft:levitator")));
    }

    @Test void theArcaneRecipeKeepsTheOriginalShapeIngredientsAndVis() throws Exception {
        var recipe = json("data/thaumcraft/recipe/arcane/levitator.json");
        assertEquals("thaumcraft:arcane_shaped", recipe.get("type").getAsString());
        assertEquals("LEVITATOR", recipe.get("research").getAsString());
        assertEquals(JsonParser.parseString("[\"WEW\",\"BNB\",\"WAW\"]"), recipe.get("pattern"));
        var key = recipe.getAsJsonObject("key");
        assertEquals("thaumcraft:greatwood_planks", key.getAsJsonObject("W").get("item").getAsString());
        assertEquals("thaumcraft:earth_shard", key.getAsJsonObject("E").get("item").getAsString());
        assertEquals("thaumcraft:air_shard", key.getAsJsonObject("A").get("item").getAsString());
        assertEquals("thaumcraft:nitor", key.getAsJsonObject("N").get("item").getAsString());
        assertEquals("minecraft:iron_ingot", key.getAsJsonObject("B").get("item").getAsString());
        assertEquals(JsonParser.parseString("{\"aer\":10,\"terra\":5}"), recipe.get("vis"));
        assertEquals("thaumcraft:levitator", recipe.getAsJsonObject("result").get("id").getAsString());
    }
}

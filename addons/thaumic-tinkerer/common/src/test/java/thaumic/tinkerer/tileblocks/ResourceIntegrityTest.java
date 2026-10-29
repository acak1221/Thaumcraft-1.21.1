package thaumic.tinkerer.tileblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the group is complete and consistent: every block has a blockstate, a model whose textures exist, an
 * item model, a loot table and a place in the tool tags, and every text the group shows exists in English and Russian.</p>
 */
class ResourceIntegrityTest {
    private static final String[] BLOCKS = { LibBlockNames.ANIMATION_TABLET, LibBlockNames.REMOTE_PLACER, LibBlockNames.MOBILIZER,
            LibBlockNames.MOBILIZER_RELAY, LibBlockNames.GOLEMCONNECTOR, LibBlockNames.INTERFACE, LibBlockNames.DISLOCATOR };

    /** The research of the group and the number of text pages of each. */
    private static final Map<String, Integer> RESEARCH_PAGES = Map.of(
            LibResearch.KEY_ANIMATION_TABLET, 1,
            LibResearch.KEY_REMOTE_PLACER, 2,
            LibResearch.KEY_MOBILIZER, 1,
            LibResearch.KEY_INTERFACE, 3,
            LibResearch.KEY_DISLOCATOR, 1,
            LibResearch.KEY_GOLEMCONNECTOR, 4);

    private static final String[] MESSAGES = { "ttmisc.animationTablet.notRotatable", "ttmisc.connector.set",
            "ttmisc.connector.complete", "ttmisc.connector.interffail", "ttmisc.connector.notinterf",
            "ttmisc.connector.notpresent", "ttmisc.connector.toofar", "ttmisc.golemconnector.set",
            "ttmisc.golemconnector.complete", "ttmisc.golemconnector.notinterf", "ttmisc.golemconnector.notpresent",
            "ttmisc.leftClick", "ttmisc.rightClick", "ttmisc.redstoneControl", "tooltip.thaumictinkerer.golem_connector" };

    private static JsonObject json(String resource) throws IOException {
        try (InputStream in = ResourceIntegrityTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return parsed.getAsJsonObject();
        }
    }

    private static boolean exists(String resource) {
        return ResourceIntegrityTest.class.getClassLoader().getResource(resource) != null;
    }

    private static String path(String id) {
        return id.startsWith("thaumictinkerer:") ? id.substring("thaumictinkerer:".length()) : id;
    }

    @Test
    void everyBlockHasItsBlockstateModelsAndLoot() throws IOException {
        for (String block : BLOCKS) {
            JsonObject state = json("assets/thaumictinkerer/blockstates/" + block + ".json");
            assertTrue(state.has("variants"), block + " has variants");
            for (Map.Entry<String, JsonElement> variant : state.getAsJsonObject("variants").entrySet()) {
                String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                assertTrue(exists("assets/thaumictinkerer/models/" + path(model) + ".json"), block + " model " + model);
            }
            JsonObject model = json("assets/thaumictinkerer/models/block/" + block + ".json");
            for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
                String id = texture.getValue().getAsString();
                if (id.startsWith("thaumictinkerer:")) {
                    assertTrue(exists("assets/thaumictinkerer/textures/" + path(id) + ".png"), block + " texture " + id);
                }
            }
            assertTrue(exists("assets/thaumictinkerer/models/item/" + block + ".json"), block + " item model");
            JsonObject loot = json("data/thaumictinkerer/loot_table/blocks/" + block + ".json");
            String drop = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                    .getAsJsonObject().get("name").getAsString();
            assertEquals("thaumictinkerer:" + block, drop);
        }
    }

    @Test
    void theBinderHasItsItemModelAndTexture() throws IOException {
        JsonObject model = json("assets/thaumictinkerer/models/item/" + LibItemNames.CONNECTOR + ".json");
        String texture = model.getAsJsonObject("textures").get("layer0").getAsString();
        assertTrue(exists("assets/thaumictinkerer/textures/" + path(texture) + ".png"), texture);
    }

    @Test
    void theFacingDevicesTurnTheirModelWithTheFacing() throws IOException {
        for (String block : new String[] { LibBlockNames.REMOTE_PLACER, LibBlockNames.DISLOCATOR }) {
            JsonObject variants = json("assets/thaumictinkerer/blockstates/" + block + ".json").getAsJsonObject("variants");
            for (String facing : new String[] { "north", "east", "south", "west", "up", "down" }) {
                assertTrue(variants.has("facing=" + facing), block + " " + facing);
            }
            assertEquals(90, variants.getAsJsonObject("facing=east").get("y").getAsInt());
            assertEquals(270, variants.getAsJsonObject("facing=up").get("x").getAsInt());
        }
    }

    @Test
    void everyBlockIsMineableWithItsTool() throws IOException {
        List<String> pickaxe = new ArrayList<>();
        json("data/thaumictinkerer/tags/block/mineable/tile_blocks/pickaxe.json").getAsJsonArray("values")
                .forEach(e -> pickaxe.add(e.getAsString()));
        List<String> axe = new ArrayList<>();
        json("data/thaumictinkerer/tags/block/mineable/tile_blocks/axe.json").getAsJsonArray("values")
                .forEach(e -> axe.add(e.getAsString()));
        for (String block : BLOCKS) {
            String id = "thaumictinkerer:" + block;
            // 1.7.10: the Golem Connector is wood, the others iron or stone.
            assertTrue(block.equals(LibBlockNames.GOLEMCONNECTOR) ? axe.contains(id) : pickaxe.contains(id), id);
        }
    }

    @Test
    void everyTextOfTheGroupExistsInEnglishAndRussian() throws IOException {
        for (String lang : new String[] { "en_us", "ru_ru" }) {
            JsonObject texts = json("assets/thaumictinkerer/lang/" + lang + ".json");
            List<String> missing = new ArrayList<>();
            for (String block : BLOCKS) require(texts, "block.thaumictinkerer." + block, missing);
            require(texts, "item.thaumictinkerer." + LibItemNames.CONNECTOR, missing);
            for (String key : MESSAGES) require(texts, key, missing);
            for (Map.Entry<String, Integer> research : RESEARCH_PAGES.entrySet()) {
                require(texts, "tc.research_name." + research.getKey(), missing);
                require(texts, "tc.research_text." + research.getKey(), missing);
                for (int page = 0; page < research.getValue(); page++) {
                    require(texts, "ttresearch.page." + research.getKey() + "." + page, missing);
                }
            }
            assertEquals(List.of(), missing, lang + " lacks texts");
        }
    }

    private static void require(JsonObject texts, String key, List<String> missing) {
        if (!texts.has(key) || texts.get(key).getAsString().isEmpty()) missing.add(key);
    }

    @Test
    void theLangFragmentsAgreeAcrossLanguages() throws IOException {
        Path en = Path.of("lang-extra", "tile_blocks.en_us.json");
        Path ru = Path.of("lang-extra", "tile_blocks.ru_ru.json");
        assertTrue(Files.isRegularFile(en) && Files.isRegularFile(ru), "the fragments exist (tests run in addons/thaumic-tinkerer)");
        JsonObject english = JsonParser.parseString(Files.readString(en)).getAsJsonObject();
        JsonObject russian = JsonParser.parseString(Files.readString(ru)).getAsJsonObject();
        assertEquals(english.keySet(), russian.keySet());
    }
}

/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): JUnit tests of the data files and registry contracts of the KamiWorld group. Distributed under the same licence as the add-on
 * (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.kamiworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.dim.TeleporterBedrock;
import thaumic.tinkerer.common.dim.WorldProviderBedrock;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the KamiWorld group: the models, blockstates, loot table and tags of the two blocks, the files that make
 * the Bedrock dimension (dimension type, dimension, biome) and agree with the constants of the code, the text of the
 * Dimensional Shards page with the new texture paths in every language, and the language keys of the screens.</p>
 */
class KamiWorldResourcesTest {
    private static final String NS = "thaumictinkerer";
    private static final List<String> LANGUAGES = List.of("en_us", "ru_ru", "de_de", "fr_fr", "ko_kr", "zh_cn");

    private static JsonObject json(String path) {
        try (InputStream in = KamiWorldResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean exists(String path) {
        return KamiWorldResourcesTest.class.getResource(path) != null;
    }

    private static String own(String id) {
        return id.startsWith(NS + ":") ? id.substring(NS.length() + 1) : null;
    }

    // ---- blocks --------------------------------------------------------------------------------------------------

    @Test
    void bothBlocksHaveABlockstateThatPointsAtAnExistingModel() {
        for (String block : List.of(LibBlockNames.WARP_GATE, LibBlockNames.PORTAL)) {
            JsonObject variants = json("/assets/" + NS + "/blockstates/" + block + ".json").getAsJsonObject("variants");
            assertEquals(1, variants.size(), block);
            String model = variants.getAsJsonObject("").get("model").getAsString();
            assertTrue(exists("/assets/" + NS + "/models/" + own(model) + ".json"), block + ": " + model);
        }
    }

    @Test
    void theModelsUseTexturesThatExist() {
        for (String model : List.of("models/block/warp_gate", "models/item/warp_gate", "models/block/bedrock_portal")) {
            JsonObject textures = json("/assets/" + NS + "/" + model + ".json").getAsJsonObject("textures");
            assertTrue(textures.has("particle"), model + " has a particle texture");
            for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
                String path = own(texture.getValue().getAsString());
                assertNotNull(path, model + " uses a texture of the add-on");
                assertTrue(exists("/assets/" + NS + "/textures/" + path + ".png"), model + ": " + path);
            }
        }
    }

    @Test
    void theGateIsACubeWhoseTopIsTheOpenFrameAndWhoseItemHasTheGlowingFrameToo() {
        JsonObject block = json("/assets/" + NS + "/models/block/warp_gate.json");
        assertEquals("minecraft:block/cube", block.get("parent").getAsString());
        JsonObject textures = block.getAsJsonObject("textures");
        assertEquals(NS + ":block/warp_gate_0", textures.get("up").getAsString(), "1.7.10: icon 0 on the top");
        for (String side : List.of("down", "north", "south", "east", "west")) {
            assertEquals(NS + ":block/warp_gate_1", textures.get(side).getAsString(), "1.7.10: icon 1 on every other side");
        }
        JsonObject item = json("/assets/" + NS + "/models/item/warp_gate.json");
        JsonArray elements = item.getAsJsonArray("elements");
        assertEquals(2, elements.size(), "the cube and the layer of the glow");
        assertEquals(NS + ":block/warp_gate_2", item.getAsJsonObject("textures").get("glow").getAsString());
        // The block's glow is drawn by the block entity renderer (RenderTileWarpGate), from the same sprite.
    }

    @Test
    void thePortalShowsItsAnimatedTextureOnEverySideAndOnlyTheSidesAreCulled() {
        JsonObject model = json("/assets/" + NS + "/models/block/bedrock_portal.json");
        JsonObject faces = model.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(Set.of("down", "up", "north", "south", "west", "east"), faces.keySet());
        // 1.7.10 shouldSideBeRendered: the top and the bottom are always drawn, a side only when the neighbour is not opaque.
        for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
            boolean vertical = face.getKey().equals("up") || face.getKey().equals("down");
            assertEquals(!vertical, face.getValue().getAsJsonObject().has("cullface"), face.getKey());
        }
        JsonObject animation = json("/assets/" + NS + "/textures/block/portal.png.mcmeta").getAsJsonObject("animation");
        assertEquals(3, animation.get("frametime").getAsInt(), "1.7.10: the portal texture changes frame every 3 ticks");
    }

    @Test
    void thePortalHasAnItemModelThatShowsTheBlockLikeAnyBlockItem() {
        // 1.7.10 registered the default item block for the portal (getItemBlock() returned null) and listed it in the tab.
        JsonObject item = json("/assets/" + NS + "/models/item/" + LibBlockNames.PORTAL + ".json");
        assertEquals(NS + ":block/bedrock_portal", item.get("parent").getAsString());
        // The block model inherits minecraft:block/block so that the item gets the display transforms of a block.
        assertEquals("minecraft:block/block", json("/assets/" + NS + "/models/block/bedrock_portal.json").get("parent").getAsString());
    }

    @Test
    void theGateDropsItselfAndTheTagsKnowTheBlocks() {
        JsonObject loot = json("/data/" + NS + "/loot_table/blocks/" + LibBlockNames.WARP_GATE + ".json");
        assertEquals("minecraft:block", loot.get("type").getAsString());
        String name = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                .get("name").getAsString();
        assertEquals(NS + ":warp_gate", name);
        assertFalse(exists("/data/" + NS + "/loot_table/blocks/" + LibBlockNames.PORTAL + ".json"), "the portal drops nothing (no loot table)");

        assertTrue(tagValues("/data/" + NS + "/tags/block/mineable/kami/pickaxe.json").contains(NS + ":warp_gate"));
        assertTrue(json("/data/minecraft/tags/block/mineable/pickaxe.json").getAsJsonArray("values").toString()
                .contains("#thaumictinkerer:mineable/kami/pickaxe"), "the vanilla pickaxe tag includes the group's tag");
        assertTrue(tagValues("/data/minecraft/tags/block/wither_immune.json").contains(NS + ":bedrock_portal"));
        assertTrue(tagValues("/data/minecraft/tags/block/dragon_immune.json").contains(NS + ":bedrock_portal"));
    }

    /** The ids of a tag file, whether its entries are strings or {id, required} objects. */
    private static Set<String> tagValues(String path) {
        Set<String> ids = new HashSet<>();
        for (JsonElement value : json(path).getAsJsonArray("values")) {
            ids.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        }
        return ids;
    }

    // ---- the dimension -------------------------------------------------------------------------------------------

    @Test
    void theDimensionTypeIs256BlocksFromZeroWithSkyAndFullBrightness() {
        JsonObject type = json("/data/" + NS + "/dimension_type/bedrock.json");
        assertEquals(0, type.get("min_y").getAsInt());
        assertEquals(256, type.get("height").getAsInt(), "1.7.10 world height");
        assertEquals(256, type.get("logical_height").getAsInt());
        assertEquals(0, type.get("min_y").getAsInt() % 16);
        assertEquals(0, type.get("height").getAsInt() % 16);
        assertTrue(type.get("has_skylight").getAsBoolean(), "hasNoSky was false");
        assertFalse(type.get("has_ceiling").getAsBoolean());
        assertEquals(1.0F, type.get("ambient_light").getAsFloat(), 0F, "the 1.7.10 brightness table gave every light level full brightness");
        assertFalse(type.get("bed_works").getAsBoolean(), "canRespawnHere was false");
        assertFalse(type.get("respawn_anchor_works").getAsBoolean());
        assertFalse(type.get("natural").getAsBoolean(), "isSurfaceWorld was false");
        assertEquals("minecraft:overworld", type.get("effects").getAsString(), "the sky of the beach biome and the overworld");
    }

    @Test
    void theDimensionUsesTheGeneratorTypeTheRegistrarRegisters() {
        JsonObject dimension = json("/data/" + NS + "/dimension/bedrock.json");
        assertEquals(NS + ":bedrock", dimension.get("type").getAsString());
        JsonObject generator = dimension.getAsJsonObject("generator");
        // KamiWorld.register(): CHUNK_GENERATORS.register("bedrock", ...) in the namespace of the add-on.
        assertEquals(NS + ":bedrock", generator.get("type").getAsString());
        JsonObject source = generator.getAsJsonObject("biome_source");
        assertEquals("minecraft:fixed", source.get("type").getAsString());
        String biome = own(source.get("biome").getAsString());
        assertNotNull(biome);
        assertTrue(exists("/data/" + NS + "/worldgen/biome/" + biome + ".json"));
    }

    @Test
    void theDimensionKeyAndTheLayersAgreeWithTheCode() {
        assertEquals(NS + ":bedrock", WorldProviderBedrock.KEY.location().toString());
        assertEquals(KamiHooks.BEDROCK_DIMENSION, WorldProviderBedrock.KEY, "KamiGear and KamiWorld name the same dimension");
        assertEquals(json("/data/" + NS + "/dimension_type/bedrock.json").get("height").getAsInt(), WorldProviderBedrock.HEIGHT);
        // The Awakened Pickaxe makes the exit portal in the top two layers (above 253); the entrance column puts it at 254.
        assertEquals(WorldProviderBedrock.HEIGHT - 3, WorldProviderBedrock.PORTAL_LAYER_ABOVE);
        assertTrue(TeleporterBedrock.EXIT_Y > WorldProviderBedrock.PORTAL_LAYER_ABOVE);
        assertTrue(TeleporterBedrock.EXIT_Y < WorldProviderBedrock.HEIGHT);
        assertEquals("bedrock_portal", LibBlockNames.PORTAL);
    }

    @Test
    void theBiomeHasTheMonstersOfTheBeachAndNoFeaturesOrCarvers() {
        JsonObject biome = json("/data/" + NS + "/worldgen/biome/bedrock.json");
        assertEquals(0, biome.getAsJsonArray("features").size());
        assertEquals(0, biome.getAsJsonObject("carvers").size());
        JsonObject spawners = biome.getAsJsonObject("spawners");
        assertEquals(0, spawners.getAsJsonArray("creature").size(), "the beach spawns no animals");
        Set<String> monsters = new HashSet<>();
        for (JsonElement entry : spawners.getAsJsonArray("monster")) monsters.add(entry.getAsJsonObject().get("type").getAsString());
        assertEquals(Set.of("minecraft:spider", "minecraft:zombie", "minecraft:skeleton", "minecraft:creeper", "minecraft:slime",
                "minecraft:enderman", "minecraft:witch"), monsters);
        // The End's fog colour dimmed to 15 percent: 0xA080A0 * 0.15.
        assertEquals(0x181318, biome.getAsJsonObject("effects").get("fog_color").getAsInt());
    }

    // ---- text and textures ---------------------------------------------------------------------------------------

    @Test
    void theShardsPageNamesTheNewTexturePathsInEveryLanguage() {
        assertTrue(exists("/assets/" + NS + "/textures/item/nether_shard.png"));
        assertTrue(exists("/assets/" + NS + "/textures/item/ender_shard.png"));
        assertEquals(LibItemNames.KAMI_RESOURCE_NAMES[6], "nether_shard");
        assertEquals(LibItemNames.KAMI_RESOURCE_NAMES[7], "ender_shard");
        for (String language : LANGUAGES) {
            JsonObject lang = json("/assets/" + NS + "/lang/" + language + ".json");
            String page = lang.get("ttresearch.page.DIMENSION_SHARDS.0").getAsString();
            assertTrue(page.contains("<IMG>thaumictinkerer:textures/item/nether_shard.png:0:0:255:255:0.0625</IMG>"), language);
            assertTrue(page.contains("<IMG>thaumictinkerer:textures/item/ender_shard.png:0:0:255:255:0.0625</IMG>"), language);
            assertFalse(page.contains("ttinkerer:"), language + ": no 1.7.10 namespace left");
            assertFalse(page.contains("textures/items/"), language + ": no 1.7.10 directory left");
        }
    }

    @Test
    void theScreensKeysExistInEnglishAndRussian() {
        List<String> keys = List.of("block.thaumictinkerer.warp_gate", "block.thaumictinkerer.bedrock_portal", "ttmisc.lockedGate",
                "ttmisc.destinations", "ttmisc.destinationInd", "ttmisc.entrancePoint", "ttmisc.destination", "ttmisc.numberKeys",
                "ttmisc.spaceToReset", "ttmisc.clickToTeleport", "ttmisc.noTeleport", "ttmisc.noDest", "ttmisc.differentDim",
                "ttmisc.distance", "tc.research_name.WARP_GATE", "tc.research_text.WARP_GATE", "ttresearch.page.WARP_GATE.0",
                "ttresearch.page.WARP_GATE.1");
        for (String language : List.of("en_us", "ru_ru")) {
            JsonObject lang = json("/assets/" + NS + "/lang/" + language + ".json");
            for (String key : keys) assertTrue(lang.has(key), language + " lacks " + key);
        }
    }

    @Test
    void theHudAndScreenTexturesHaveTheSizesTheCodeAssumes() throws IOException {
        try (InputStream in = KamiWorldResourcesTest.class.getResourceAsStream("/assets/" + NS + "/textures/gui/soul_hearts.png")) {
            var hearts = ImageIO.read(in);
            assertEquals(16, hearts.getWidth(), "a 16 by 16 sheet: the full heart is 9 wide, the half heart the 7 to its right");
            assertEquals(16, hearts.getHeight());
        }
        try (InputStream in = KamiWorldResourcesTest.class.getResourceAsStream("/assets/" + NS + "/textures/gui/warp_gate.png")) {
            var gui = ImageIO.read(in);
            assertEquals(256, gui.getWidth());
            assertEquals(256, gui.getHeight());
            // The two states of the button are the 13 by 13 squares at (176, 0) and (176, 13).
            assertEquals(255, (gui.getRGB(176, 0) >>> 24), "the first state is drawn");
            assertEquals(255, (gui.getRGB(176, 13) >>> 24), "the second state is drawn");
        }
    }
}

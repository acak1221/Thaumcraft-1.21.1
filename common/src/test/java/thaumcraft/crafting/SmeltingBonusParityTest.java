package thaumcraft.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The original TC4 4.2.3.5 smelting bonus catalogue, pinned against the oracle.
 *
 * <p>The original registers eighteen bonus entries in {@code ConfigRecipes.initializeSmelting} (oracle
 * {@code thaumcraft/common/config/ConfigRecipes.java:493-510}) and the ARCANE FURNACE pays them when it
 * ejects a smelted item (oracle {@code thaumcraft/common/tiles/TileArcaneFurnace.java:237-259}). The port
 * had neither the catalogue nor the lookup: {@code ThaumcraftApi.addSmeltingBonus} was never called and
 * {@code TileArcaneFurnace.setSmeltingBonus} was never installed, so the arcane furnace never paid a
 * bonus.</p>
 *
 * <p>Six entries need a third-party ore mod (tin, silver, lead) and 1.21.1 has none of those metals, so
 * they are carried as documented skips instead of live registrations. The remaining twelve are pinned
 * here with the original line, the original registration and the flattened native identity.</p>
 */
class SmeltingBonusParityTest {
    private static final String RESOURCE = "data/thaumcraft/tc4/smelting_bonus.json";

    private static JsonObject corpus() throws Exception {
        try (InputStream stream = SmeltingBonusParityTest.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new AssertionError("missing resource " + RESOURCE);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static Map<Integer, JsonObject> rows(JsonObject corpus, String array) {
        Map<Integer, JsonObject> rows = new LinkedHashMap<>();
        for (JsonElement element : corpus.getAsJsonArray(array)) {
            JsonObject row = element.getAsJsonObject();
            rows.put(row.get("source_line").getAsInt(), row);
        }
        return rows;
    }

    @Test void theCatalogueCarriesTheTwelveOriginalEntriesThePortCanFire() throws Exception {
        JsonObject corpus = corpus();
        assertEquals(1, corpus.get("schema_version").getAsInt());
        Map<Integer, JsonObject> carried = rows(corpus, "bonus");
        // Oracle ConfigRecipes:493-510 registers eighteen entries; six of them need
        // a third-party tin, silver or lead mod and 1.21.1 has none of those metals.
        assertEquals(List.of(493, 494, 495, 496, 500, 501, 502, 503, 507, 508, 509, 510),
                new ArrayList<>(carried.keySet()), "the original registration order and the original lines");
        assertEquals("oreGold", carried.get(493).get("ore").getAsString());
        assertEquals("minecraft:gold_nugget", carried.get(493).get("output").getAsString());
        assertEquals("oreIron", carried.get(494).get("ore").getAsString());
        assertEquals("minecraft:iron_nugget", carried.get(494).get("output").getAsString());
        assertEquals("oreCinnabar", carried.get(495).get("ore").getAsString());
        assertEquals("thaumcraft:quicksilver_drop", carried.get(495).get("output").getAsString());
        assertEquals("oreCopper", carried.get(496).get("ore").getAsString());
        assertEquals("thaumcraft:copper_nugget", carried.get(496).get("output").getAsString());
        assertEquals("thaumcraft:cluster_gold", carried.get(500).get("item").getAsString());
        assertEquals("minecraft:gold_nugget", carried.get(500).get("output").getAsString());
        assertEquals("thaumcraft:cluster_iron", carried.get(501).get("item").getAsString());
        assertEquals("minecraft:iron_nugget", carried.get(501).get("output").getAsString());
        assertEquals("thaumcraft:cluster_cinnabar", carried.get(502).get("item").getAsString());
        assertEquals("thaumcraft:quicksilver_drop", carried.get(502).get("output").getAsString());
        assertEquals("thaumcraft:cluster_copper", carried.get(503).get("item").getAsString());
        assertEquals("thaumcraft:copper_nugget", carried.get(503).get("output").getAsString());
        assertEquals("minecraft:chicken", carried.get(507).get("item").getAsString());
        assertEquals("thaumcraft:nugget_chicken", carried.get(507).get("output").getAsString());
        assertEquals("minecraft:beef", carried.get(508).get("item").getAsString());
        assertEquals("thaumcraft:nugget_beef", carried.get(508).get("output").getAsString());
        assertEquals("minecraft:porkchop", carried.get(509).get("item").getAsString());
        assertEquals("thaumcraft:nugget_pork", carried.get(509).get("output").getAsString());
        // The original raw fish entry carries the wildcard metadata (Short.MAX_VALUE) of
        // Items.fish, so the native tag holds every raw fish of 1.21.1.
        assertEquals("thaumcraft:original_raw_fish", carried.get(510).get("item_tag").getAsString());
        assertEquals("thaumcraft:nugget_fish", carried.get(510).get("output").getAsString());
    }

    @Test void everyCarriedEntryNamesExactlyOneInputAndTheOriginalRegistration() throws Exception {
        for (JsonObject row : rows(corpus(), "bonus").values()) {
            int inputs = 0;
            if (row.has("ore")) ++inputs;
            if (row.has("item")) ++inputs;
            if (row.has("item_tag")) ++inputs;
            assertEquals(1, inputs, "entry at line " + row.get("source_line").getAsInt() + " has one input");
            assertTrue(row.has("output"), "entry at line " + row.get("source_line").getAsInt() + " has an output");
            assertTrue(row.has("original"), "entry at line " + row.get("source_line").getAsInt() + " names the original registration");
            assertFalse(row.get("original").getAsString().isBlank());
        }
    }

    @Test void theSixThirdPartyOreEntriesAreDocumentedSkips() throws Exception {
        Map<Integer, JsonObject> skipped = rows(corpus(), "skipped");
        assertEquals(List.of(497, 498, 499, 504, 505, 506), new ArrayList<>(skipped.keySet()),
                "the original tin, silver and lead registrations");
        assertEquals("oreTin", skipped.get(497).get("ore").getAsString());
        assertEquals("oreSilver", skipped.get(498).get("ore").getAsString());
        assertEquals("oreLead", skipped.get(499).get("ore").getAsString());
        assertEquals("thaumcraft:cluster_tin", skipped.get(504).get("item").getAsString());
        assertEquals("thaumcraft:cluster_silver", skipped.get(505).get("item").getAsString());
        assertEquals("thaumcraft:cluster_lead", skipped.get(506).get("item").getAsString());
        for (JsonObject row : skipped.values()) {
            assertTrue(row.has("reason") && !row.get("reason").getAsString().isBlank(),
                    "every skipped entry states why it cannot fire");
        }
    }

    @Test void theEighteenOriginalRegistrationsAreAllAccountedFor() throws Exception {
        JsonObject corpus = corpus();
        List<Integer> lines = new ArrayList<>(rows(corpus(), "bonus").keySet());
        lines.addAll(rows(corpus(), "skipped").keySet());
        java.util.Collections.sort(lines);
        assertEquals(18, lines.size(), "the original registers eighteen bonus entries");
        List<Integer> expected = new ArrayList<>();
        for (int line = 493; line <= 510; ++line) expected.add(line);
        assertEquals(expected, lines, "the original registration lines 493-510");
    }
}

package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;

class ResearchItemIconTest {
    @BeforeAll static void loadResearch() { ResearchCatalog.loadBundled(); }

    @Test void allNineStaffResearchIconsResolveToTheirActualStaffRods() {
        for (String material : List.of("blaze", "bone", "greatwood", "ice", "obsidian",
                "primal", "quartz", "reed", "silverwood")) {
            assertIcon("ROD_" + material + "_staff", "staff_rod_" + material);
        }
    }

    @Test void goldTravellerBootsAndHarnessKeepTheirCorpusIcons() {
        assertIcon("PUREGOLD", "cluster_gold");
        assertIcon("BOOTSTRAVELLER", "boots_traveller");
        assertIcon("HOVERHARNESS", "hover_harness");
    }

    @Test void acceptingStaffMetadataDoesNotAcceptArbitraryItemsOrUnsupportedVariants() {
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.itemWandRod, 1, 999)"));
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.itemBootsTraveller, 1, 50)"));
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.unrecognized)"));
        assertNull(ResearchIconDisplay.itemReference("new ItemStack(ConfigItems.itemWandRod, 65, 50)"));
    }

    @Test void everyOriginalItemIconMatchesTheIndependentLegacyIdentityTable() throws Exception {
        Map<String, JsonObject> legacy = new HashMap<>();
        try (var input = getClass().getClassLoader().getResourceAsStream("data/thaumcraft/tc4/legacy_ids.json")) {
            assertNotNull(input);
            var rows = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("rows");
            for (var element : rows) {
                var row = element.getAsJsonObject();
                legacy.put(row.get("field").getAsString() + ":" + row.get("meta").getAsInt(), row);
            }
        }
        Pattern literal = Pattern.compile("new ItemStack\\(Config(?:Items|Blocks)\\.([A-Za-z0-9_]+)(?:,\\s*([0-9]+)(?:,\\s*([0-9]+|Short.MAX_VALUE))?)?\\)");
        int originals = 0, textures = 0, absent = 0, itemIcons = 0;
        for (var research : ResearchCatalog.bundled()) {
            originals++;
            String expression = research.iconExpression();
            if (expression == null) {
                assertTrue(research.isVirtual(), "only the original virtual rod/cap entries have no icon: " + research.key());
                absent++;
                continue;
            }
            if (expression.startsWith("new ResourceLocation(")) { textures++; continue; }
            itemIcons++;
            var match = literal.matcher(expression);
            if (!match.matches()) {
                assertNotNull(ResearchIconDisplay.reference(expression, 0), research.key() + " literal prototype or vanilla icon");
                continue;
            }
            int metadata = match.group(3) == null ? 0 : match.group(3).equals("Short.MAX_VALUE") ? 32767 : Integer.parseInt(match.group(3));
            var row = legacy.get(match.group(1) + ":" + metadata);
            assertNotNull(row, research.key() + " has an independently extracted original field/metadata row");
            if (row.has("ids")) {
                var ids = row.getAsJsonArray("ids");
                for (int frame = 0; frame < ids.size(); frame++) {
                    assertEquals(ids.get(frame).getAsString(), qualified(ResearchIconDisplay.reference(expression, frame).path()),
                            research.key() + " wildcard frame " + frame);
                }
            } else {
                String field = row.get("kind").getAsString().equals("block") ? "item" : "id";
                if (row.get(field).isJsonNull()) {
                    assertEquals("CAP_silver", research.key(), "only optional silver has no native item");
                    assertNull(ResearchIconDisplay.reference(expression, 0));
                } else {
                    var resolved = ResearchIconDisplay.reference(expression, 0);
                    assertNotNull(resolved, research.key());
                    assertEquals(row.get(field).getAsString(), qualified(resolved.path()), research.key() + " keeps its original subtype");
                    assertEquals(match.group(2) == null ? 1 : Integer.parseInt(match.group(2)), resolved.count(), research.key());
                }
            }
        }
        assertEquals(201, originals);
        assertEquals(27, textures);
        assertEquals(2, absent);
        assertEquals(172, itemIcons);
    }

    @Test void originalPrototypeAndWildcardIconsKeepEveryFrameAndTheActiveStoneSprite() {
        assertEquals(new ResearchIconDisplay.ItemReference("wand", 1), ResearchIconDisplay.reference("sceptre3", 0));
        assertEquals(new ResearchIconDisplay.ItemReference("banner", 1), ResearchIconDisplay.reference("is", 0));
        assertEquals(new ResearchIconDisplay.ItemReference("jar_node", 1), ResearchIconDisplay.reference("jarIs", 0));
        var placers = List.of("straw", "wood", "tallow", "clay", "flesh", "stone", "iron", "thaumium");
        for (int frame = 0; frame < placers.size(); frame++) {
            assertEquals("golem_" + placers.get(frame), ResearchIconDisplay.reference("ag", frame).path());
            assertEquals(ResearchIconDisplay.reference("ag", frame), ResearchIconDisplay.reference("ag", frame + 8));
        }
        assertEquals("minecraft:gold_nugget", ResearchIconDisplay.reference("new ItemStack(Items.field_151074_bl)", 0).path());
        assertEquals("thaumcraft:textures/items/sinister_stone_active.png",
                ResearchIconDisplay.textureReference(ResearchCatalog.get("SINSTONE").iconExpression()).toString());
        assertNull(ResearchIconDisplay.textureReference("new ItemStack(ConfigItems.itemCompassStone)"));
    }

    private static String qualified(String path) { return path.indexOf(':') >= 0 ? path : "thaumcraft:" + path; }

    private static void assertIcon(String key, String path) {
        var research = ResearchCatalog.get(key);
        assertNotNull(research, key);
        assertEquals(new ResearchIconDisplay.ItemReference(path, 1),
                ResearchIconDisplay.itemReference(research.iconExpression()), key);
    }
}
